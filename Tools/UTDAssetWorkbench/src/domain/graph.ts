import { refMatchesItem } from "./relations";
import type { CanonicalItem, CanonicalRecipe, CanonicalRef, FilteredGraph, GraphEdge, GraphNode } from "./schema";
import { displayNameFromId, stableStringify } from "./stable";

/** Full catalog in, whitelist-root one-hop graph out. */
export function buildFilteredGraph(items: CanonicalItem[], allRecipes: CanonicalRecipe[]): FilteredGraph {
  const roots = items.filter((item) => item.humanSelected);
  const recipes = allRecipes.filter((recipe) =>
    roots.some((root) => recipe.outputs.some((output) => refMatchesItem(output, root)))
  );
  const itemsByRegistry = indexByRegistry(items);
  const nodes = new Map<string, GraphNode>();
  const edges = new Map<string, GraphEdge>();

  for (const root of roots) addItemNode(nodes, root);
  for (const recipe of recipes) {
    const recipeNodeId = `recipe:${recipe.id}`;
    nodes.set(recipeNodeId, recipeNode(recipe));
    for (const output of recipe.outputs) {
      for (const root of roots.filter((item) => refMatchesItem(output, item))) {
        addEdge(edges, recipeNodeId, `item:${root.itemKey}`, "produces", output.count);
      }
    }
    for (const input of recipe.inputs) {
      const inputNode = input.refKind === "item"
        ? resolveInputNode(input, itemsByRegistry)
        : refNode(`${input.refKind}:${input.ref}`, input.refKind, input.ref);
      nodes.set(inputNode.id, inputNode);
      addEdge(edges, inputNode.id, recipeNodeId, "requires", input.count);
    }
  }

  // Recycling and dismantling intentionally point from finished goods back to
  // ingredients. Including those reverse edges makes almost every healthy
  // production chain look like one giant cycle, so only forward-production
  // recipes participate in blocking cycle detection.
  const productionRecipes = recipes.filter((recipe) => recipe.stationScope !== "recycling");
  const cycles = detectRootCycles(roots, productionRecipes);
  const cycleKeys = new Set(cycles.flat());
  for (const key of cycleKeys) {
    const node = nodes.get(`item:${key}`);
    if (node) node.cycle = true;
  }
  for (const recipe of recipes) {
    if (recipe.stationScope === "recycling") continue;
    const outputs = recipe.outputs.flatMap((ref) => roots.filter((item) => refMatchesItem(ref, item)).map((item) => item.itemKey));
    const inputs = recipe.inputs.flatMap((ref) => roots.filter((item) => refMatchesItem(ref, item)).map((item) => item.itemKey));
    if (outputs.some((key) => cycleKeys.has(key)) && inputs.some((key) => cycleKeys.has(key))) {
      const node = nodes.get(`recipe:${recipe.id}`);
      if (node) node.cycle = true;
    }
  }
  return {
    nodes: [...nodes.values()],
    edges: [...edges.values()],
    cycles,
    rootItemKeys: roots.map((item) => item.itemKey)
  };
}

function indexByRegistry(items: CanonicalItem[]): Map<string, CanonicalItem[]> {
  const result = new Map<string, CanonicalItem[]>();
  for (const item of items) {
    const bucket = result.get(item.registryId) ?? [];
    bucket.push(item);
    result.set(item.registryId, bucket);
  }
  return result;
}

function resolveInputNode(ref: CanonicalRef, itemsByRegistry: Map<string, CanonicalItem[]>): GraphNode {
  const candidates = itemsByRegistry.get(ref.ref) ?? [];
  const item = candidates.find((candidate) => candidate.humanSelected && refMatchesItem(ref, candidate))
    ?? candidates.find((candidate) => refMatchesItem(ref, candidate))
    ?? candidates[0];
  return item ? itemNode(item) : refNode(`item:${ref.ref}`, "item", ref.ref);
}

function addItemNode(nodes: Map<string, GraphNode>, item: CanonicalItem): void {
  nodes.set(`item:${item.itemKey}`, itemNode(item));
}

function itemNode(item: CanonicalItem): GraphNode {
  return {
    id: `item:${item.itemKey}`,
    kind: "item",
    label: item.clientNameZhCn || displayNameFromId(item.registryId),
    subtitle: item.variantDiscriminator || item.registryId,
    ownership: item.ownership,
    managed: item.managed,
    issueCount: item.issues.length,
    cycle: false,
    ref: item.itemKey,
    iconDataUrl: item.iconDataUrl || undefined
  };
}

function recipeNode(recipe: CanonicalRecipe): GraphNode {
  return {
    id: `recipe:${recipe.id}`,
    kind: "recipe",
    label: recipe.outputName || recipe.id.split("/").at(-1) || recipe.id,
    subtitle: `${recipe.station} · ${recipe.recipeType}`,
    ownership: recipe.ownership,
    managed: recipe.editable,
    issueCount: recipe.issues.length,
    cycle: false,
    ref: recipe.id
  };
}

function refNode(id: string, kind: GraphNode["kind"], ref: string): GraphNode {
  return {
    id,
    kind,
    label: kind === "tag" ? `#${ref}` : displayNameFromId(ref),
    subtitle: kind === "tag" ? "标签终点 · 不展开" : ref,
    ownership: "external",
    managed: false,
    issueCount: 0,
    cycle: false,
    ref
  };
}

function addEdge(edges: Map<string, GraphEdge>, from: string, to: string, relation: GraphEdge["relation"], count: number): void {
  const id = `${from}->${to}:${relation}`;
  const existing = edges.get(id);
  if (existing) existing.count += count;
  else edges.set(id, { id, from, to, relation, count });
}

function detectRootCycles(roots: CanonicalItem[], recipes: CanonicalRecipe[]): string[][] {
  const adjacency = new Map<string, Set<string>>();
  for (const item of roots) adjacency.set(item.itemKey, new Set());
  for (const recipe of recipes) {
    for (const outputRef of recipe.outputs) {
      const outputs = roots.filter((item) => refMatchesItem(outputRef, item));
      for (const inputRef of recipe.inputs) {
        const inputs = roots.filter((item) => refMatchesItem(inputRef, item));
        for (const output of outputs) for (const input of inputs) {
          // Shared carrier items (notably TaCZ workbenches) can represent two
          // different blocks through components. A component-changing upgrade
          // is a forward transformation even when both registry ids match.
          if (output.itemKey === input.itemKey && refsDescribeDifferentVariants(outputRef, inputRef)) continue;
          adjacency.get(output.itemKey)?.add(input.itemKey);
        }
      }
    }
  }
  return stronglyConnectedCycles(adjacency);
}

function refsDescribeDifferentVariants(left: CanonicalRef, right: CanonicalRef): boolean {
  if (left.refKind !== "item" || right.refKind !== "item" || left.ref !== right.ref) return false;
  if (left.variantDiscriminator || right.variantDiscriminator) {
    return (left.variantDiscriminator ?? "") !== (right.variantDiscriminator ?? "");
  }
  if (left.identityKey || right.identityKey) return (left.identityKey ?? "") !== (right.identityKey ?? "");
  return stableStringify(left.components ?? {}) !== stableStringify(right.components ?? {});
}

function stronglyConnectedCycles(adjacency: Map<string, Set<string>>): string[][] {
  let index = 0;
  const indices = new Map<string, number>();
  const lowLinks = new Map<string, number>();
  const stack: string[] = [];
  const onStack = new Set<string>();
  const components: string[][] = [];
  const visit = (node: string): void => {
    indices.set(node, index);
    lowLinks.set(node, index);
    index += 1;
    stack.push(node);
    onStack.add(node);
    for (const next of adjacency.get(node) ?? []) {
      if (!indices.has(next)) {
        visit(next);
        lowLinks.set(node, Math.min(lowLinks.get(node)!, lowLinks.get(next)!));
      } else if (onStack.has(next)) lowLinks.set(node, Math.min(lowLinks.get(node)!, indices.get(next)!));
    }
    if (lowLinks.get(node) !== indices.get(node)) return;
    const component: string[] = [];
    let current: string;
    do {
      current = stack.pop()!;
      onStack.delete(current);
      component.push(current);
    } while (current !== node);
    const selfLoop = component.length === 1 && adjacency.get(component[0])?.has(component[0]);
    if (component.length > 1 || selfLoop) components.push(component.sort());
  };
  for (const node of adjacency.keys()) if (!indices.has(node)) visit(node);
  return components.sort((a, b) => a.join("|").localeCompare(b.join("|")));
}
