export const WORKBENCH_SCHEMA = "utd-asset-workbench/v1" as const;
export const SNAPSHOT_SCHEMA = "utd-item-whitelist/v1" as const;
export const STATUS_SCHEMA = "utd-asset-status/v1" as const;
export const EXCEL_INTERFACE_SCHEMA = "utd-excel-export/v1" as const;

export type JsonPrimitive = string | number | boolean | null;
export type JsonValue = JsonPrimitive | JsonObject | JsonValue[];
export type JsonObject = { [key: string]: JsonValue };

export type Ownership = "utd" | "external";
export type ItemSource = "whitelist" | "recipe_dependency" | "recipe_output" | "loot_registry";
export type SyncState = "local_only" | "pending" | "synced" | "stale" | "error";
export type RefKind = "item" | "tag" | "fluid" | "unknown";
export type RecipeSourceKind = "shaped" | "shapeless" | "custom";
export type IssueSeverity = "error" | "warning" | "info";

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
  entityType: "project" | "item" | "recipe" | "loot";
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
  };
  counts: {
    managedItems: number;
    dependencyItems: number;
    recipes: number;
    lootPolicies: number;
    cycles: number;
    issues: number;
  };
}

export interface WorkbenchProject {
  schemaVersion: typeof WORKBENCH_SCHEMA;
  manifest: WorkbenchManifest;
  items: CanonicalItem[];
  recipes: CanonicalRecipe[];
  lootPolicies: CanonicalLootPolicy[];
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
