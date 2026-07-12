import { refreshProject } from "./mutations";
import { lootMatchesItem, refMatchesItem } from "./relations";
import type {
  BlockTransform,
  CanonicalLootPolicy,
  CanonicalRecipe,
  ItemPresentationOverride,
  WorkbenchProject
} from "./schema";
import { isRecord, stableStringify } from "./stable";

export const LOCAL_DRAFT_SCHEMA = "utd-asset-workbench-local-draft/v2" as const;
const STORAGE_PREFIX = "utd.asset-workbench.local-draft.v2";
export const MAX_LOCAL_DRAFT_BYTES = 2_000_000;

export interface DraftIdentity {
  projectId: string;
  catalogHash: string;
}

export type RecipeDraftProjection = Pick<CanonicalRecipe,
  "id" | "station" | "stationKey" | "stationScope" | "form" | "level"
>;

export type LootDraftProjection = Pick<CanonicalLootPolicy,
  "identityKey" | "lootEnabled" | "level" | "count" | "commonTags" | "commonBaseWeight"
  | "directedWeight" | "replacePriority"
>;

export interface DraftSlices {
  presentations: ItemPresentationOverride[];
  blockTransforms: BlockTransform[];
  recipes: RecipeDraftProjection[];
  lootPolicies: LootDraftProjection[];
}

export interface LocalDraftDocument {
  schema_version: typeof LOCAL_DRAFT_SCHEMA;
  project_id: string;
  catalog_hash: string;
  saved_at: string;
  presentations: ItemPresentationOverride[];
  block_transforms: BlockTransform[];
  recipe_edits: RecipeDraftProjection[];
  loot_edits: LootDraftProjection[];
}

export interface StorageLike {
  getItem(key: string): string | null;
  setItem(key: string, value: string): void;
  removeItem(key: string): void;
}

export function draftIdentityFor(project: WorkbenchProject): DraftIdentity {
  return {
    projectId: project.manifest.projectId,
    catalogHash: project.manifest.catalogHash
  };
}

export function draftStorageKey(identity: DraftIdentity): string {
  return `${STORAGE_PREFIX}:${encodeURIComponent(identity.projectId)}:${encodeURIComponent(identity.catalogHash)}`;
}

/**
 * Browser storage receives only authored fields. Immutable identities are kept
 * solely so the projection can be replayed against exactly one canonical row.
 */
export function extractDraftSlices(project: WorkbenchProject): DraftSlices {
  return {
    presentations: structuredClone(project.presentations),
    blockTransforms: structuredClone(project.blockTransforms),
    recipes: project.recipes
      .filter((recipe) => recipe.editable)
      .map(recipeProjection)
      .sort((a, b) => a.id.localeCompare(b.id, "en")),
    lootPolicies: editableLootPolicies(project)
      .map(lootProjection)
      .sort((a, b) => a.identityKey.localeCompare(b.identityKey, "en"))
  };
}

export function draftSlicesDigest(value: WorkbenchProject | DraftSlices): string {
  const slices = "manifest" in value ? extractDraftSlices(value) : value;
  return stableStringify({
    presentations: slices.presentations,
    blockTransforms: slices.blockTransforms,
    recipes: slices.recipes,
    lootPolicies: slices.lootPolicies
  });
}

export function createLocalDraftDocument(
  identity: DraftIdentity,
  project: WorkbenchProject,
  savedAt = new Date().toISOString()
): LocalDraftDocument {
  const slices = extractDraftSlices(project);
  return {
    schema_version: LOCAL_DRAFT_SCHEMA,
    project_id: identity.projectId,
    catalog_hash: identity.catalogHash,
    saved_at: savedAt,
    presentations: slices.presentations,
    block_transforms: slices.blockTransforms,
    recipe_edits: slices.recipes,
    loot_edits: slices.lootPolicies
  };
}

export function serializeLocalDraft(document: LocalDraftDocument): string {
  const serialized = JSON.stringify(document);
  const bytes = new TextEncoder().encode(serialized).byteLength;
  if (bytes > MAX_LOCAL_DRAFT_BYTES) {
    throw new Error(`本地草稿为 ${bytes.toLocaleString("zh-CN")} 字节，超过 2 MB 安全上限；旧草稿已保留。`);
  }
  return serialized;
}

export function saveLocalDraft(
  storage: StorageLike,
  identity: DraftIdentity,
  project: WorkbenchProject,
  savedAt = new Date().toISOString()
): LocalDraftDocument {
  const document = createLocalDraftDocument(identity, project, savedAt);
  // Serialize and enforce the same limit before setItem. A failed oversized
  // save therefore cannot overwrite the last known recoverable draft.
  const serialized = serializeLocalDraft(document);
  storage.setItem(draftStorageKey(identity), serialized);
  return document;
}

export function loadLocalDraft(storage: StorageLike, identity: DraftIdentity): LocalDraftDocument | null {
  const raw = storage.getItem(draftStorageKey(identity));
  if (raw === null) return null;
  if (new TextEncoder().encode(raw).byteLength > MAX_LOCAL_DRAFT_BYTES) {
    throw new Error("本地草稿超过 2 MB 安全上限，已拒绝恢复。");
  }
  let parsed: unknown;
  try {
    parsed = JSON.parse(raw);
  } catch {
    throw new Error("本地草稿不是有效 JSON。");
  }
  if (!isLocalDraftDocument(parsed)) throw new Error("本地草稿结构无效或不属于当前项目目录。");
  if (parsed.project_id !== identity.projectId || parsed.catalog_hash !== identity.catalogHash) {
    throw new Error("本地草稿与当前 projectId / catalog hash 不匹配。");
  }
  return parsed;
}

export function removeLocalDraft(storage: StorageLike, identity: DraftIdentity): void {
  storage.removeItem(draftStorageKey(identity));
}

/**
 * Replays lightweight edits only against exact canonical identities. Missing,
 * duplicate, newly read-only, or extra identities stop restoration instead of
 * silently applying a draft to a different row.
 */
export function applyDraftSlices(project: WorkbenchProject, slices: DraftSlices): WorkbenchProject {
  const next = structuredClone(project);
  const touched = new Set<string>();

  assertExactIdentities(
    next.recipes.filter((recipe) => recipe.editable).map((recipe) => recipe.id),
    slices.recipes.map((recipe) => recipe.id),
    "配方 id"
  );
  const editableLoot = editableLootPolicies(next);
  assertExactIdentities(
    editableLoot.map((policy) => policy.identityKey),
    slices.lootPolicies.map((policy) => policy.identityKey),
    "Loot identityKey"
  );

  if (stableStringify(next.presentations) !== stableStringify(slices.presentations)) {
    const rows = next.presentations.concat(slices.presentations);
    for (const item of next.items) {
      if (rows.some((entry) => entry.applyScope === "registry"
        ? entry.registryId === item.registryId
        : entry.itemKey === item.itemKey)) touched.add(item.itemKey);
    }
  }
  if (stableStringify(next.blockTransforms) !== stableStringify(slices.blockTransforms)) {
    const rows = next.blockTransforms.concat(slices.blockTransforms);
    for (const item of next.items) {
      if (rows.some((entry) => refMatchesItem(entry.catalyst, item))) touched.add(item.itemKey);
    }
  }
  next.presentations = structuredClone(slices.presentations);
  next.blockTransforms = structuredClone(slices.blockTransforms);

  const recipesById = uniqueIndex(next.recipes, (recipe) => recipe.id, "规范项目配方 id");
  for (const edit of slices.recipes) {
    const recipe = recipesById.get(edit.id);
    if (!recipe?.editable) throw new Error(`本地草稿配方无法精确回放：${edit.id}`);
    if (stableStringify(recipeProjection(recipe)) === stableStringify(edit)) continue;
    Object.assign(recipe, structuredClone(edit));
    for (const output of recipe.outputs.filter((entry) => entry.refKind === "item")) {
      for (const item of next.items.filter((entry) => entry.managed && refMatchesItem(output, entry))) {
        touched.add(item.itemKey);
      }
    }
  }

  const lootByIdentity = uniqueIndex(next.lootPolicies, (policy) => policy.identityKey, "规范项目 Loot identityKey");
  const editableLootIds = new Set(editableLoot.map((policy) => policy.identityKey));
  for (const edit of slices.lootPolicies) {
    const policy = lootByIdentity.get(edit.identityKey);
    if (!policy || !editableLootIds.has(edit.identityKey)) {
      throw new Error(`本地草稿 Loot 无法精确回放：${edit.identityKey}`);
    }
    if (stableStringify(lootProjection(policy)) === stableStringify(edit)) continue;
    Object.assign(policy, structuredClone(edit));
    for (const item of next.items.filter((entry) => entry.managed && lootMatchesItem(policy, entry))) {
      touched.add(item.itemKey);
    }
  }

  return refreshProject(next, touched);
}

export function applyLocalDraft(project: WorkbenchProject, document: LocalDraftDocument): WorkbenchProject {
  return applyDraftSlices(project, {
    presentations: document.presentations,
    blockTransforms: document.block_transforms,
    recipes: document.recipe_edits,
    lootPolicies: document.loot_edits
  });
}

function recipeProjection(recipe: CanonicalRecipe): RecipeDraftProjection {
  return {
    id: recipe.id,
    station: recipe.station,
    stationKey: recipe.stationKey,
    stationScope: recipe.stationScope,
    form: recipe.form,
    level: recipe.level
  };
}

function lootProjection(policy: CanonicalLootPolicy): LootDraftProjection {
  return {
    identityKey: policy.identityKey,
    lootEnabled: policy.lootEnabled,
    level: policy.level,
    count: policy.count,
    commonTags: structuredClone(policy.commonTags),
    commonBaseWeight: policy.commonBaseWeight,
    directedWeight: policy.directedWeight,
    replacePriority: policy.replacePriority
  };
}

function editableLootPolicies(project: WorkbenchProject): CanonicalLootPolicy[] {
  return project.lootPolicies.filter((policy) => project.items.some((item) =>
    item.managed && item.ownership === "utd" && lootMatchesItem(policy, item)
  ));
}

function assertExactIdentities(expected: string[], actual: string[], label: string): void {
  const expectedSet = new Set(expected);
  const actualSet = new Set(actual);
  if (expectedSet.size !== expected.length) throw new Error(`规范项目存在重复${label}，不能恢复本地草稿。`);
  if (actualSet.size !== actual.length) throw new Error(`本地草稿存在重复${label}，已拒绝恢复。`);
  const missing = expected.filter((identity) => !actualSet.has(identity));
  const extra = actual.filter((identity) => !expectedSet.has(identity));
  if (!missing.length && !extra.length) return;
  const details = [
    missing.length ? `缺少 ${missing.slice(0, 3).join(", ")}` : "",
    extra.length ? `多出 ${extra.slice(0, 3).join(", ")}` : ""
  ].filter(Boolean).join("；");
  throw new Error(`本地草稿${label}集合与当前目录不一致（${details}），已拒绝模糊回放。`);
}

function uniqueIndex<T>(rows: T[], identity: (row: T) => string, label: string): Map<string, T> {
  const index = new Map<string, T>();
  for (const row of rows) {
    const key = identity(row);
    if (index.has(key)) throw new Error(`${label}重复：${key}`);
    index.set(key, row);
  }
  return index;
}

function isLocalDraftDocument(value: unknown): value is LocalDraftDocument {
  if (!isRecord(value)
    || value.schema_version !== LOCAL_DRAFT_SCHEMA
    || typeof value.project_id !== "string"
    || typeof value.catalog_hash !== "string"
    || typeof value.saved_at !== "string"
    || !Array.isArray(value.presentations)
    || !Array.isArray(value.block_transforms)
    || !Array.isArray(value.recipe_edits)
    || !Array.isArray(value.loot_edits)) return false;
  return value.presentations.every(isPresentation)
    && value.block_transforms.every(isBlockTransform)
    && value.recipe_edits.every(isRecipeProjection)
    && value.loot_edits.every(isLootProjection);
}

function isRecipeProjection(value: unknown): value is RecipeDraftProjection {
  return isRecord(value)
    && hasOnlyKeys(value, ["id", "station", "stationKey", "stationScope", "form", "level"])
    && ["id", "station", "stationKey", "stationScope", "form"].every((key) => typeof value[key] === "string")
    && (value.level === null || isFiniteNumber(value.level));
}

function isLootProjection(value: unknown): value is LootDraftProjection {
  return isRecord(value)
    && hasOnlyKeys(value, [
      "identityKey", "lootEnabled", "level", "count", "commonTags", "commonBaseWeight", "directedWeight", "replacePriority"
    ])
    && typeof value.identityKey === "string"
    && typeof value.lootEnabled === "boolean"
    && ["level", "count", "commonBaseWeight", "directedWeight", "replacePriority"].every((key) => isFiniteNumber(value[key]))
    && Array.isArray(value.commonTags)
    && value.commonTags.every((entry) => typeof entry === "string");
}

function hasOnlyKeys(value: Record<string, unknown>, allowed: string[]): boolean {
  const keys = new Set(allowed);
  return Object.keys(value).every((key) => keys.has(key));
}

function isFiniteNumber(value: unknown): value is number {
  return typeof value === "number" && Number.isFinite(value);
}

function isPresentation(value: unknown): value is ItemPresentationOverride {
  if (!isRecord(value)) return false;
  return ["itemKey", "registryId", "variantDiscriminator", "observedNameZhCn", "nameKey", "descriptionKey", "nameZhCn", "descriptionZhCn", "baseCatalogHash", "updatedAt"]
    .every((key) => typeof value[key] === "string")
    && (value.applyScope === "identity" || value.applyScope === "registry")
    && typeof value.enabled === "boolean";
}

function isBlockTransform(value: unknown): value is BlockTransform {
  if (!isRecord(value) || !isRecord(value.catalyst)) return false;
  const catalyst = value.catalyst;
  return typeof value.id === "string"
    && typeof value.enabled === "boolean"
    && isFiniteNumber(value.priority)
    && typeof value.clickedBlock === "string"
    && isStringRecord(value.targetState)
    && typeof value.resultBlock === "string"
    && isStringRecord(value.resultState)
    && Array.isArray(value.copyProperties) && value.copyProperties.every((entry) => typeof entry === "string")
    && catalyst.refKind === "item"
    && typeof catalyst.ref === "string"
    && isFiniteNumber(catalyst.count)
    && (catalyst.identityKey === undefined || typeof catalyst.identityKey === "string")
    && (catalyst.variantDiscriminator === undefined || typeof catalyst.variantDiscriminator === "string")
    && typeof value.catalystComponentsSnbt === "string"
    && (value.inputSource === "clicked_hand" || value.inputSource === "inventory")
    && (value.hand === "main" || value.hand === "off" || value.hand === "any")
    && ["requireSneaking", "allowFakePlayer", "consumeInput", "cancelInteraction", "creativeRequireInput", "creativeConsume"]
      .every((key) => typeof value[key] === "boolean")
    && value.blockEntityPolicy === "reject";
}

function isStringRecord(value: unknown): value is Record<string, string> {
  return isRecord(value) && Object.values(value).every((entry) => typeof entry === "string");
}
