export const WORKBENCH_SCHEMA = "utd-asset-workbench/v1" as const;
export const SNAPSHOT_SCHEMA = "utd-item-whitelist/v1" as const;
export const STATUS_SCHEMA = "utd-asset-status/v1" as const;
export const EXCEL_INTERFACE_SCHEMA = "utd-excel-export/v1" as const;
export const PRESENTATION_SCHEMA = "utd-item-presentation/v1" as const;
export const LANG_OVERLAY_SCHEMA = "utd-lang-overlays/v1" as const;
export const BLOCK_TRANSFORM_SCHEMA = "utd-block-transforms/v1" as const;
export const ITEM_CATEGORY_SCHEMA = "utd-item-categories/v1" as const;
export const ITEM_PROPERTY_SCHEMA = "utd-item-properties/v1" as const;

export type JsonPrimitive = string | number | boolean | null;
export type JsonValue = JsonPrimitive | JsonObject | JsonValue[];
export type JsonObject = { [key: string]: JsonValue };

export type Ownership = "utd" | "external";
export type ItemSource = "whitelist" | "recipe_dependency" | "recipe_output" | "loot_registry";
export type SyncState = "local_only" | "pending" | "synced" | "stale" | "error";
export type RefKind = "item" | "tag" | "fluid" | "unknown";
export type RecipeSourceKind = "shaped" | "shapeless" | "custom";
export type IssueSeverity = "error" | "warning" | "info";
export type PresentationApplyScope = "registry" | "identity";
export type BlockTransformInputSource = "clicked_hand" | "inventory";
export type BlockTransformHand = "main" | "off" | "any";
export type BlockEntityPolicy = "reject";
export type BlockStateProperties = Record<string, string>;

export interface SourceFingerprint {
  path?: string;
  sha256?: string;
  generatedAt?: string;
}

export interface CanonicalIdentity {
  itemKey: string;
  registryId: string;
  identityKind: string;
  variantDiscriminator: string;
  componentsCanonical: string;
  componentsSnbt: string;
  identityComponentsCanonical: string;
  variantKey: string | null;
  canonicalVariant: JsonValue | null;
  canonicalComponents: JsonObject;
}

export interface CanonicalItem extends CanonicalIdentity {
  clientNameZhCn: string;
  translationKey: string;
  iconDataUrl: string;
  categoryKey: string;
  categoryLabelZhCn: string;
  categoryLevel: number | null;
  namespace: string;
  managed: boolean;
  humanSelected: boolean;
  ownership: Ownership;
  source: ItemSource;
  recipeInput: boolean;
  recipeOutput: boolean;
  recipeInputCount: number;
  recipeOutputCount: number;
  lootEnabled: boolean;
  lootLevel: number | null;
  catalogHash: string;
  deployedHash: string | null;
  sync: SyncState;
  issues: string[];
}

export interface ItemCategory {
  key: string;
  labelZhCn: string;
  order: number;
}

export interface ItemCategoryAssignment {
  categoryKey: string;
  level: number | null;
  itemKey?: string;
  registryId?: string;
  variantDiscriminator?: string;
  sourceSheet?: string;
  sourceRow?: number;
}

export interface ItemCategoryDocument {
  schema_version: typeof ITEM_CATEGORY_SCHEMA;
  categories: ItemCategory[];
  assignments: ItemCategoryAssignment[];
}

export interface CanonicalRef {
  refKind: RefKind;
  ref: string;
  identityKey?: string;
  variantDiscriminator?: string;
  count: number;
  components?: JsonObject;
  chance?: number;
  consume?: boolean;
}

export interface CanonicalRecipe {
  id: string;
  sourceKind: RecipeSourceKind;
  recipeType: string;
  ownership: Ownership;
  editable: boolean;
  station: string;
  stationKey: string;
  stationScope: string;
  form: string;
  sheet: string;
  sourceRow: number | null;
  level: number | null;
  outputName: string;
  inputs: CanonicalRef[];
  outputs: CanonicalRef[];
  pattern?: string[];
  symbolMap?: Record<string, CanonicalRef>;
  raw: JsonObject;
  issues: string[];
}

export interface CanonicalLootPolicy {
  identityKey: string;
  registryId: string;
  variantDiscriminator: string;
  lootEnabled: boolean;
  level: number;
  count: number;
  commonTags: string[];
  commonBaseWeight: number;
  allowedCommonTemplates: string[];
  directedTemplates: string[];
  directedWeight: number;
  replacePriority: number;
  legacyNbt?: string;
  raw: JsonObject;
}

/**
 * An authored language override. Observed clientNameZhCn/translationKey values
 * remain immutable evidence from the game export; edits live here instead.
 */
export interface ItemPresentationOverride {
  itemKey: string;
  registryId: string;
  variantDiscriminator: string;
  applyScope: PresentationApplyScope;
  enabled: boolean;
  observedNameZhCn: string;
  nameKey: string;
  descriptionKey: string;
  nameZhCn: string;
  descriptionZhCn: string;
  baseCatalogHash: string;
  updatedAt: string;
}

/** A data-only right-click block replacement rule. Runtime deployment is separate. */
export interface BlockTransform {
  id: string;
  enabled: boolean;
  priority: number;
  clickedBlock: string;
  targetState: BlockStateProperties;
  resultBlock: string;
  resultState: BlockStateProperties;
  copyProperties: string[];
  catalyst: CanonicalRef;
  catalystComponentsSnbt: string;
  inputSource: BlockTransformInputSource;
  hand: BlockTransformHand;
  requireSneaking: boolean;
  allowFakePlayer: boolean;
  consumeInput: boolean;
  cancelInteraction: boolean;
  blockEntityPolicy: BlockEntityPolicy;
  creativeRequireInput: boolean;
  creativeConsume: boolean;
}

export interface RarityProperty {
  value: number;
}

export interface BlockZProperty {
  width: number;
  height: number;
  capacityWidth: number | null;
  capacityHeight: number | null;
}

export interface TaczGunProperty {
  gunId: string;
  sourcePack: string;
  sourceNamespace: string;
  sourceDataId: string;
  sourceData: JsonObject;
  damage: number;
  ammoAmount: number;
  rpm: number;
  reloadTacticalFeed: number;
  reloadTacticalCooldown: number;
  reloadEmptyFeed: number;
  reloadEmptyCooldown: number;
  inaccuracyStand: number;
  inaccuracyMove: number;
  inaccuracySneak: number;
  inaccuracyLie: number;
  inaccuracyAim: number;
  armorIgnore: number;
  pierce: number;
  bulletSpeed: number;
  gravity: number;
}

export interface FoodEffectProperty {
  id: string;
  durationTicks: number;
  amplifier: number;
  chance: number;
}

export interface FoodProperty {
  foodId: string;
  nutrition: number;
  saturation: number;
  thirstDelta: number;
  waterDelta: number;
  thirstMode: "always" | "only";
  effects: FoodEffectProperty[];
}

/** Authored integration values. Original third-party files remain immutable evidence. */
export interface ItemPropertyOverride {
  itemKey: string;
  registryId: string;
  variantDiscriminator: string;
  enabled: boolean;
  rarity: RarityProperty | null;
  blockz: BlockZProperty | null;
  tacz: TaczGunProperty | null;
  food: FoodProperty | null;
  baseCatalogHash: string;
  updatedAt: string;
}

export interface GraphNode {
  id: string;
  kind: "item" | "recipe" | "tag" | "fluid" | "unknown";
  label: string;
  subtitle: string;
  ownership: Ownership;
  managed: boolean;
  issueCount: number;
  cycle: boolean;
  ref?: string;
  iconDataUrl?: string;
}

export interface GraphEdge {
  id: string;
  from: string;
  to: string;
  relation: "produces" | "requires";
  count: number;
}

export interface FilteredGraph {
  nodes: GraphNode[];
  edges: GraphEdge[];
  cycles: string[][];
  rootItemKeys: string[];
}

export interface ValidationIssue {
  code: string;
  severity: IssueSeverity;
  message: string;
  entityType: "project" | "item" | "recipe" | "loot" | "block_transform";
  entityId: string;
}

export interface SourcePolicies {
  removeTypes: string[];
  removeRecipeIds: string[];
  removeOutputs: string[];
}

export interface WorkbenchManifest {
  schemaVersion: typeof WORKBENCH_SCHEMA;
  projectId: string;
  generatedAt: string;
  contentFingerprint: string;
  catalogHash: string;
  deployedHash: string | null;
  source: {
    snapshot: SourceFingerprint;
    recipes: SourceFingerprint;
    lootRegistry?: SourceFingerprint;
    lootBalance?: SourceFingerprint;
    blockTransforms?: SourceFingerprint;
    itemProperties?: SourceFingerprint;
    categories?: SourceFingerprint;
  };
  counts: {
    managedItems: number;
    dependencyItems: number;
    recipes: number;
    lootPolicies: number;
    presentations: number;
    blockTransforms: number;
    itemProperties: number;
    cycles: number;
    issues: number;
  };
}

export interface WorkbenchProject {
  schemaVersion: typeof WORKBENCH_SCHEMA;
  manifest: WorkbenchManifest;
  items: CanonicalItem[];
  categories: ItemCategory[];
  recipes: CanonicalRecipe[];
  lootPolicies: CanonicalLootPolicy[];
  presentations: ItemPresentationOverride[];
  blockTransforms: BlockTransform[];
  itemProperties: ItemPropertyOverride[];
  lootBalance: JsonObject | null;
  graph: FilteredGraph;
  issues: ValidationIssue[];
  sourcePolicies: SourcePolicies;
}

export interface ItemStatusRecord {
  asset_key: string;
  item_key: string;
  registry_id: string;
  identity_kind: string;
  variant_discriminator: string;
  components_canonical: string;
  components_snbt: string;
  identity_components_canonical: string;
  variant_key: string | null;
  client_name_zh_cn: string;
  translation_key: string;
  category_key: string;
  category_label_zh_cn: string;
  category_level: number | null;
  canonical_components: JsonObject;
  canonical_variant: JsonValue | null;
  catalogued: boolean;
  managed: boolean;
  human_selected: boolean;
  recipe_input: boolean;
  recipe_output: boolean;
  recipe_input_count: number;
  recipe_output_count: number;
  loot_enabled: boolean;
  loot_level: number | null;
  catalog_hash: string;
  deployed_hash: string | null;
  stale: boolean;
  sync_state: SyncState;
  sync: SyncState;
  issues: string[];
}

export interface StatusManifest {
  schema_version: typeof STATUS_SCHEMA;
  project_id: string;
  generated_at: string;
  content_fingerprint: string;
  catalog_hash: string;
  deployed_hash: string | null;
  counts: WorkbenchManifest["counts"];
  items: ItemStatusRecord[];
}

export interface ExcelSheetInterface {
  name: string;
  columns: string[];
  rows: Array<Record<string, JsonValue>>;
  frozenRows?: number;
  filter?: boolean;
}

export interface ExcelExportInterface {
  schemaVersion: typeof EXCEL_INTERFACE_SCHEMA;
  generatedAt: string;
  workbookName: string;
  sheets: ExcelSheetInterface[];
}
