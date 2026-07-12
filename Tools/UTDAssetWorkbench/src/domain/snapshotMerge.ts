import { normalizeSnapshot } from "./normalize";
import type { CanonicalItem, WorkbenchProject } from "./schema";

export interface SnapshotMergeResult {
  project: WorkbenchProject;
  matched: number;
  icons: number;
  unmatched: string[];
}

/**
 * Adds fresh client-rendered icons to an existing canonical directory without
 * replacing its broader historical whitelist, recipes, Loot, or authoring data.
 */
export function mergeSnapshotEvidence(project: WorkbenchProject, snapshotInput: unknown): SnapshotMergeResult {
  const snapshot = normalizeSnapshot(snapshotInput);
  const next = structuredClone(project);
  let matched = 0;
  let icons = 0;
  const unmatched: string[] = [];
  for (const observed of snapshot.items) {
    const item = resolveItem(next.items, observed);
    if (!item) {
      unmatched.push(observed.itemKey);
      continue;
    }
    matched += 1;
    if (observed.iconDataUrl) {
      item.iconDataUrl = observed.iconDataUrl;
      icons += 1;
    }
  }
  const itemByKey = new Map(next.items.map((item) => [item.itemKey, item]));
  for (const node of next.graph.nodes) {
    if (node.kind !== "item") continue;
    const item = itemByKey.get(node.ref ?? node.id.replace(/^item:/, ""));
    node.iconDataUrl = item?.iconDataUrl || undefined;
  }
  return { project: next, matched, icons, unmatched };
}

function resolveItem(items: CanonicalItem[], observed: CanonicalItem): CanonicalItem | null {
  const exact = items.filter((item) => item.itemKey === observed.itemKey);
  if (exact.length === 1) return exact[0];
  const semantic = items.filter((item) => item.registryId === observed.registryId
    && item.variantDiscriminator === observed.variantDiscriminator);
  if (semantic.length === 1) return semantic[0];
  if (!observed.variantDiscriminator) {
    const registry = items.filter((item) => item.registryId === observed.registryId && !item.variantDiscriminator);
    if (registry.length === 1) return registry[0];
  }
  return null;
}
