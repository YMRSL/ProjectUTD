import {
  ITEM_CATEGORY_SCHEMA,
  type CanonicalItem,
  type ItemCategory,
  type ItemCategoryAssignment,
  type ItemCategoryDocument,
  type WorkbenchProject
} from "./schema";
import { isRecord, numberOr, stringOr } from "./stable";
import { refreshProject } from "./mutations";

export const UNCATEGORIZED_CATEGORY: ItemCategory = {
  key: "uncategorized",
  labelZhCn: "未分类",
  order: 9999
};

export function parseItemCategoryDocument(value: unknown): ItemCategoryDocument {
  if (!isRecord(value) || value.schema_version !== ITEM_CATEGORY_SCHEMA) {
    throw new Error(`Expected ${ITEM_CATEGORY_SCHEMA} category JSON.`);
  }
  if (!Array.isArray(value.categories) || !Array.isArray(value.assignments)) {
    throw new Error(`${ITEM_CATEGORY_SCHEMA} requires categories[] and assignments[].`);
  }
  const categoryKeys = new Set<string>();
  const categories = value.categories.map((raw, index) => {
    if (!isRecord(raw)) throw new Error(`Category #${index + 1} must be an object.`);
    const key = stringOr(raw.key).trim().toLocaleLowerCase();
    const labelZhCn = stringOr(raw.label_zh_cn ?? raw.labelZhCn).trim();
    const order = Math.trunc(numberOr(raw.order, index * 10));
    if (!/^[a-z0-9_.-]+$/.test(key)) throw new Error(`Category #${index + 1} has invalid key: ${key || "<blank>"}`);
    if (!labelZhCn) throw new Error(`Category ${key} has no Chinese label.`);
    if (categoryKeys.has(key)) throw new Error(`Duplicate category key: ${key}`);
    categoryKeys.add(key);
    return { key, labelZhCn, order };
  });
  if (!categoryKeys.has(UNCATEGORIZED_CATEGORY.key)) categories.push(UNCATEGORIZED_CATEGORY);

  const assignments = value.assignments.map((raw, index) => {
    if (!isRecord(raw)) throw new Error(`Category assignment #${index + 1} must be an object.`);
    const categoryKey = stringOr(raw.category_key ?? raw.categoryKey).trim().toLocaleLowerCase();
    if (!categoryKeys.has(categoryKey)) throw new Error(`Assignment #${index + 1} references unknown category: ${categoryKey}`);
    const itemKey = stringOr(raw.item_key ?? raw.itemKey).trim();
    const registryId = stringOr(raw.registry_id ?? raw.registryId).trim().toLocaleLowerCase();
    const variantDiscriminator = stringOr(raw.variant_discriminator ?? raw.variantDiscriminator).trim();
    const rawSourceRow = raw.source_row ?? raw.sourceRow;
    if (!itemKey && !registryId && !variantDiscriminator) {
      throw new Error(`Category assignment #${index + 1} has no identity selector.`);
    }
    const rawLevel = raw.level;
    return {
      categoryKey,
      level: rawLevel === null || rawLevel === undefined || rawLevel === ""
        ? null
        : Math.trunc(numberOr(rawLevel, 0)),
      itemKey: itemKey || undefined,
      registryId: registryId || undefined,
      variantDiscriminator: variantDiscriminator || undefined,
      sourceSheet: stringOr(raw.source_sheet ?? raw.sourceSheet) || undefined,
      sourceRow: typeof rawSourceRow === "number"
        ? Math.trunc(rawSourceRow)
        : undefined
    } satisfies ItemCategoryAssignment;
  });
  return { schema_version: ITEM_CATEGORY_SCHEMA, categories, assignments };
}

export function applyItemCategoryDocument(project: WorkbenchProject, value: unknown): WorkbenchProject {
  const document = parseItemCategoryDocument(value);
  const next = structuredClone(project);
  next.categories = [...document.categories].sort((a, b) => a.order - b.order || a.labelZhCn.localeCompare(b.labelZhCn, "zh-CN"));
  const byKey = new Map(next.categories.map((entry) => [entry.key, entry]));
  for (const item of next.items) {
    const matches = document.assignments
      .map((assignment) => ({ assignment, score: matchScore(item, assignment) }))
      .filter((entry) => entry.score > 0);
    const bestScore = Math.max(0, ...matches.map((entry) => entry.score));
    const best = matches.filter((entry) => entry.score === bestScore);
    const semantic = new Set(best.map((entry) => `${entry.assignment.categoryKey}\u0000${entry.assignment.level ?? ""}`));
    if (semantic.size > 1) {
      throw new Error(`Conflicting category assignments for ${item.itemKey}.`);
    }
    const assignment = best[0]?.assignment;
    const category = assignment ? byKey.get(assignment.categoryKey) : UNCATEGORIZED_CATEGORY;
    item.categoryKey = category?.key ?? UNCATEGORIZED_CATEGORY.key;
    item.categoryLabelZhCn = category?.labelZhCn ?? UNCATEGORIZED_CATEGORY.labelZhCn;
    item.categoryLevel = assignment?.level ?? null;
  }
  return refreshProject(next, new Set());
}

function matchScore(item: CanonicalItem, assignment: ItemCategoryAssignment): number {
  if (assignment.itemKey) return assignment.itemKey === item.itemKey ? 300 : 0;
  if (assignment.variantDiscriminator) {
    if (assignment.registryId && assignment.registryId !== item.registryId.toLocaleLowerCase()) return 0;
    return assignment.variantDiscriminator === item.variantDiscriminator ? 200 : 0;
  }
  return assignment.registryId === item.registryId.toLocaleLowerCase() ? 100 : 0;
}

export function hydrateItemCategories(project: WorkbenchProject): void {
  const categories = Array.isArray(project.categories) ? project.categories : [];
  const normalized = categories
    .filter((entry) => entry && typeof entry.key === "string" && typeof entry.labelZhCn === "string")
    .map((entry) => ({ key: entry.key, labelZhCn: entry.labelZhCn, order: numberOr(entry.order, 9999) }));
  if (!normalized.some((entry) => entry.key === UNCATEGORIZED_CATEGORY.key)) normalized.push(UNCATEGORIZED_CATEGORY);
  project.categories = normalized.sort((a, b) => a.order - b.order || a.labelZhCn.localeCompare(b.labelZhCn, "zh-CN"));
  const byKey = new Map(project.categories.map((entry) => [entry.key, entry]));
  for (const item of project.items) {
    const key = typeof item.categoryKey === "string" && byKey.has(item.categoryKey)
      ? item.categoryKey
      : UNCATEGORIZED_CATEGORY.key;
    const category = byKey.get(key) ?? UNCATEGORIZED_CATEGORY;
    item.categoryKey = category.key;
    item.categoryLabelZhCn = category.labelZhCn;
    item.categoryLevel = typeof item.categoryLevel === "number" && Number.isFinite(item.categoryLevel)
      ? Math.trunc(item.categoryLevel)
      : null;
    item.iconDataUrl = safeIconDataUrl(item.iconDataUrl);
  }
  const itemByKey = new Map(project.items.map((item) => [item.itemKey, item]));
  for (const node of project.graph?.nodes ?? []) {
    if (node.kind !== "item") continue;
    const item = itemByKey.get(node.ref ?? node.id.replace(/^item:/, ""));
    node.iconDataUrl = item?.iconDataUrl || undefined;
  }
}

export function safeIconDataUrl(value: unknown): string {
  if (typeof value !== "string" || value.length > 131_072) return "";
  return /^data:image\/png;base64,[a-z0-9+/=]+$/i.test(value) ? value : "";
}
