import { useMemo } from "react";
import type { GraphNode, WorkbenchProject } from "../domain/schema";

export type EntitySelection =
  | { kind: "item"; id: string }
  | { kind: "recipe"; id: string }
  | { kind: "ref"; id: string };

interface Props {
  project: WorkbenchProject;
  rootItemKey: string;
  selection: EntitySelection;
  onSelect: (selection: EntitySelection) => void;
}

interface PositionedNode {
  viewId: string;
  node: GraphNode;
  x: number;
  y: number;
  width: number;
  height: number;
}

interface PositionedEdge {
  id: string;
  from: PositionedNode;
  to: PositionedNode;
  count: number;
}

export function GraphCanvas({ project, rootItemKey, selection, onSelect }: Props) {
  const layout = useMemo(() => makeLayout(project, rootItemKey), [project, rootItemKey]);
  return (
    <div className="graph-viewport">
      <div className="graph-canvas" style={{ width: layout.width, height: layout.height }}>
        <svg className="graph-lines" width={layout.width} height={layout.height} aria-hidden="true">
          <defs>
            <linearGradient id="line-fade" x1="0" x2="1">
              <stop offset="0" stopColor="#b99a57" stopOpacity=".72" />
              <stop offset="1" stopColor="#6b6b5f" stopOpacity=".38" />
            </linearGradient>
          </defs>
          {layout.edges.map((edge) => {
            const x1 = edge.from.x + edge.from.width;
            const y1 = edge.from.y + edge.from.height / 2;
            const x2 = edge.to.x;
            const y2 = edge.to.y + edge.to.height / 2;
            const bend = Math.max(48, (x2 - x1) * 0.48);
            return (
              <g key={edge.id}>
                <path
                  className="graph-path"
                  d={`M ${x1} ${y1} C ${x1 + bend} ${y1}, ${x2 - bend} ${y2}, ${x2} ${y2}`}
                />
                {edge.count > 1 && (
                  <text className="graph-count" x={(x1 + x2) / 2} y={(y1 + y2) / 2 - 6}>×{edge.count}</text>
                )}
              </g>
            );
          })}
        </svg>

        {layout.nodes.map((positioned, index) => {
          const selected = isSelected(positioned.node, selection);
          return (
            <button
              type="button"
              key={positioned.viewId}
              className={`graph-node graph-node--${positioned.node.kind} ${positioned.node.managed ? "is-managed" : "is-external"} ${positioned.node.cycle ? "has-cycle" : ""} ${selected ? "is-selected" : ""}`}
              style={{
                left: positioned.x,
                top: positioned.y,
                width: positioned.width,
                minHeight: positioned.height,
                animationDelay: `${Math.min(index * 35, 320)}ms`
              }}
              onClick={() => onSelect(toSelection(positioned.node))}
            >
              <span className="graph-node__index">{nodeMarker(positioned.node)}</span>
              <span className="graph-node__body">
                <strong>{positioned.node.label}</strong>
                <small>{positioned.node.subtitle}</small>
              </span>
              {positioned.node.issueCount > 0 && <span className="graph-node__issue">{positioned.node.issueCount}</span>}
              {positioned.node.cycle && <span className="graph-node__cycle">LOOP</span>}
            </button>
          );
        })}

        {!layout.hasRecipes && (
          <div className="graph-empty" style={{ left: 348, top: layout.height / 2 - 42 }}>
            <span>NO RECIPE LINK</span>
            <p>当前白名单根没有配方产出记录。Loot 与状态仍保留在右侧检查器。</p>
          </div>
        )}
      </div>
    </div>
  );
}

function makeLayout(project: WorkbenchProject, rootItemKey: string) {
  const nodeById = new Map(project.graph.nodes.map((node) => [node.id, node]));
  const rootId = `item:${rootItemKey}`;
  const rootNode = nodeById.get(rootId);
  const produceEdges = project.graph.edges.filter((edge) => edge.relation === "produces" && edge.to === rootId);
  const recipeIds = produceEdges.map((edge) => edge.from);
  const groups = recipeIds.map((recipeId) => {
    const recipe = nodeById.get(recipeId)!;
    const inputs = project.graph.edges
      .filter((edge) => edge.relation === "requires" && edge.to === recipeId)
      .map((edge) => ({ edge, node: nodeById.get(edge.from)! }))
      .filter((entry) => entry.node);
    return { recipe, recipeId, inputs, height: Math.max(124, inputs.length * 78 + 32) };
  });
  const contentHeight = Math.max(420, groups.reduce((sum, group) => sum + group.height + 26, 0) + 80);
  const nodes: PositionedNode[] = [];
  const edges: PositionedEdge[] = [];
  const root: PositionedNode = {
    viewId: rootId,
    node: rootNode ?? fallbackRoot(project, rootItemKey),
    x: 38,
    y: contentHeight / 2 - 46,
    width: 236,
    height: 92
  };
  nodes.push(root);
  let cursor = 54;
  for (const group of groups) {
    const recipePosition: PositionedNode = {
      viewId: group.recipeId,
      node: group.recipe,
      x: 348,
      y: cursor + group.height / 2 - 43,
      width: 264,
      height: 86
    };
    nodes.push(recipePosition);
    edges.push({ id: `${root.viewId}-${recipePosition.viewId}`, from: root, to: recipePosition, count: 1 });
    group.inputs.forEach(({ node, edge }, inputIndex) => {
      const inputPosition: PositionedNode = {
        viewId: `${node.id}@${group.recipeId}`,
        node,
        x: 692,
        y: cursor + 12 + inputIndex * 78,
        width: 254,
        height: 64
      };
      nodes.push(inputPosition);
      edges.push({ id: `${recipePosition.viewId}-${inputPosition.viewId}`, from: recipePosition, to: inputPosition, count: edge.count });
    });
    cursor += group.height + 26;
  }
  return { width: 1000, height: contentHeight, nodes, edges, hasRecipes: groups.length > 0 };
}

function fallbackRoot(project: WorkbenchProject, itemKey: string): GraphNode {
  const item = project.items.find((candidate) => candidate.itemKey === itemKey);
  return {
    id: `item:${itemKey}`,
    kind: "item",
    label: item?.clientNameZhCn ?? itemKey,
    subtitle: item?.registryId ?? itemKey,
    ownership: item?.ownership ?? "utd",
    managed: true,
    issueCount: item?.issues.length ?? 0,
    cycle: false,
    ref: itemKey
  };
}

function nodeMarker(node: GraphNode): string {
  if (node.kind === "recipe") return "R";
  if (node.kind === "tag") return "#";
  if (node.kind === "fluid") return "F";
  return node.managed ? "U" : "D";
}

function toSelection(node: GraphNode): EntitySelection {
  if (node.kind === "recipe") return { kind: "recipe", id: node.ref ?? node.id.replace(/^recipe:/, "") };
  if (node.kind === "item") return { kind: "item", id: node.ref ?? node.id.replace(/^item:/, "") };
  return { kind: "ref", id: node.id };
}

function isSelected(node: GraphNode, selection: EntitySelection): boolean {
  if (selection.kind === "recipe" && node.kind === "recipe") return node.ref === selection.id;
  if (selection.kind === "item" && node.kind === "item") return node.ref === selection.id;
  return selection.kind === "ref" && node.id === selection.id;
}
