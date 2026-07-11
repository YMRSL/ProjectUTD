import {
  EXCEL_INTERFACE_SCHEMA,
  STATUS_SCHEMA,
  WORKBENCH_SCHEMA,
  type CanonicalLootPolicy,
  type CanonicalRecipe,
  type ExcelExportInterface,
  type ItemStatusRecord,
  type JsonObject,
  type JsonValue,
  type StatusManifest,
  type WorkbenchProject
} from "./schema";
import { cloneJson, stableStringify } from "./stable";

export function assertWorkbenchProject(value: unknown): asserts value is WorkbenchProject {
  if (!value || typeof value !== "object" || (value as { schemaVersion?: unknown }).schemaVersion !== WORKBENCH_SCHEMA) {
    throw new Error(`Expected ${WORKBENCH_SCHEMA} project JSON.`);
  }
}

export function exportProjectJson(project: WorkbenchProject): string {
  return stableStringify(project, 2) + "\n";
}

export function toStatusManifest(project: WorkbenchProject): StatusManifest {
  const items: ItemStatusRecord[] = project.items.map((item) => ({
    asset_key: item.itemKey,
    item_key: item.itemKey,
    registry_id: item.registryId,
    identity_kind: item.identityKind,
    variant_discriminator: item.variantDiscriminator,
    components_canonical: item.componentsCanonical,
    components_snbt: item.componentsSnbt,
    identity_components_canonical: item.identityComponentsCanonical,
    variant_key: item.variantKey,
    client_name_zh_cn: item.clientNameZhCn,
    translation_key: item.translationKey,
    canonical_components: item.canonicalComponents,
    canonical_variant: item.canonicalVariant,
    catalogued: item.managed,
    managed: item.managed,
    human_selected: item.humanSelected,
    recipe_input: item.recipeInput,
    recipe_output: item.recipeOutput,
    recipe_input_count: item.recipeInputCount,
    recipe_output_count: item.recipeOutputCount,
    loot_enabled: item.lootEnabled,
    loot_level: item.lootLevel,
    catalog_hash: item.catalogHash,
    deployed_hash: item.deployedHash,
    stale: item.sync === "stale",
    sync_state: item.sync,
    sync: item.sync,
    issues: item.issues
  }));
  return {
    schema_version: STATUS_SCHEMA,
    project_id: project.manifest.projectId,
    generated_at: project.manifest.generatedAt,
    content_fingerprint: project.manifest.contentFingerprint,
    catalog_hash: project.manifest.catalogHash,
    deployed_hash: project.manifest.deployedHash,
    counts: project.manifest.counts,
    items
  };
}

export function exportStatusJson(project: WorkbenchProject): string {
  return stableStringify(toStatusManifest(project), 2) + "\n";
}

export function exportStatusKjs(project: WorkbenchProject): string {
  const payload = stableStringify(toStatusManifest(project), 2);
  return `// priority: 95\n\nglobal.UTD = global.UTD || {};\n\n(function () {\n  var utd = global.UTD;\n  utd.assetStatusManifest = ${indentAfterFirst(payload, 2)};\n})();\n`;
}

export function exportRecipeKjs(project: WorkbenchProject): string {
  const payload = {
    generatedAt: project.manifest.generatedAt,
    removeTypes: project.sourcePolicies.removeTypes,
    removeRecipeIds: project.sourcePolicies.removeRecipeIds,
    removeOutputs: project.sourcePolicies.removeOutputs,
    shaped: project.recipes.filter((recipe) => recipe.sourceKind === "shaped").map(materializeRecipe),
    shapeless: project.recipes.filter((recipe) => recipe.sourceKind === "shapeless").map(materializeRecipe),
    custom: project.recipes.filter((recipe) => recipe.sourceKind === "custom").map(materializeRecipe)
  };
  const json = stableStringify(payload, 2);
  return `// priority: 60\n\nglobal.UTD = global.UTD || {};\n\n(function () {\n  var utd = global.UTD;\n  utd.recipeData = ${indentAfterFirst(json, 2)};\n})();\n`;
}

export function exportLootRegistryKjs(project: WorkbenchProject): string {
  const rows = project.lootPolicies.map(materializeLootPolicy);
  const json = stableStringify(rows, 2);
  return `// priority: 90\n\nglobal.UTD = global.UTD || {};\n\n(function () {\n  var utd = global.UTD;\n  utd.lootRegistrySeed = ${indentAfterFirst(json, 2)};\n})();\n`;
}

function materializeRecipe(recipe: CanonicalRecipe): JsonObject {
  const raw = cloneJson(recipe.raw);
  const utd = typeof raw.utd === "object" && raw.utd !== null && !Array.isArray(raw.utd)
    ? (raw.utd as JsonObject)
    : {};
  utd.station = recipe.station;
  utd.stationKey = recipe.stationKey;
  utd.stationScope = recipe.stationScope;
  utd.form = recipe.form;
  if (recipe.level !== null) utd.level = recipe.level;
  raw.utd = utd;
  return raw;
}

function materializeLootPolicy(policy: CanonicalLootPolicy): JsonObject {
  const raw = cloneJson(policy.raw);
  raw.lootEnabled = policy.lootEnabled;
  raw.level = policy.level;
  raw.count = policy.count;
  raw.commonTags = policy.commonTags;
  raw.commonBaseWeight = policy.commonBaseWeight;
  raw.allowedCommonTemplates = policy.allowedCommonTemplates;
  raw.directedTemplates = policy.directedTemplates;
  raw.directedWeight = policy.directedWeight;
  raw.replacePriority = policy.replacePriority;
  return raw;
}

export function toExcelExportInterface(project: WorkbenchProject): ExcelExportInterface {
  const status = toStatusManifest(project);
  const recipeRows = project.recipes.map((recipe) => ({
    recipe_id: recipe.id,
    ownership: recipe.ownership,
    editable: recipe.editable,
    source_kind: recipe.sourceKind,
    recipe_type: recipe.recipeType,
    station: recipe.station,
    station_key: recipe.stationKey,
    station_scope: recipe.stationScope,
    form: recipe.form,
    sheet: recipe.sheet,
    source_row: recipe.sourceRow,
    level: recipe.level,
    output_name: recipe.outputName,
    issues: recipe.issues.join(" | ")
  }));
  const inputRows = project.recipes.flatMap((recipe) => recipe.inputs.map((input, index) => ({
    recipe_id: recipe.id,
    input_index: index + 1,
    ref_kind: input.refKind,
    ref: input.ref,
    identity_key: input.identityKey ?? "",
    variant_discriminator: input.variantDiscriminator ?? "",
    count: input.count,
    components_json: stableStringify(input.components ?? {}),
    chance: input.chance ?? null,
    consume: input.consume ?? null
  })));
  const outputRows = project.recipes.flatMap((recipe) => recipe.outputs.map((output, index) => ({
    recipe_id: recipe.id,
    output_index: index + 1,
    ref_kind: output.refKind,
    ref: output.ref,
    identity_key: output.identityKey ?? "",
    variant_discriminator: output.variantDiscriminator ?? "",
    count: output.count,
    components_json: stableStringify(output.components ?? {}),
    chance: output.chance ?? null
  })));
  const lootRows = project.lootPolicies.map((loot) => ({
    identity_key: loot.identityKey,
    registry_id: loot.registryId,
    variant_discriminator: loot.variantDiscriminator,
    loot_enabled: loot.lootEnabled,
    level: loot.level,
    count: loot.count,
    common_tags: loot.commonTags.join(" | "),
    common_base_weight: loot.commonBaseWeight,
    allowed_common_templates: loot.allowedCommonTemplates.join(" | "),
    directed_templates: loot.directedTemplates.join(" | "),
    directed_weight: loot.directedWeight,
    replace_priority: loot.replacePriority,
    legacy_nbt: loot.legacyNbt ?? ""
  }));
  return {
    schemaVersion: EXCEL_INTERFACE_SCHEMA,
    generatedAt: project.manifest.generatedAt,
    workbookName: `${project.manifest.projectId}_asset_workbench.xlsx`,
    sheets: [
      {
        name: "README",
        columns: ["key", "value"],
        rows: [
          { key: "schema", value: project.schemaVersion },
          { key: "project_id", value: project.manifest.projectId },
          { key: "generated_at", value: project.manifest.generatedAt },
          { key: "content_fingerprint", value: project.manifest.contentFingerprint },
          { key: "editing_contract", value: "JSON is canonical; this workbook interface is a one-way review export." }
        ],
        frozenRows: 1
      },
      { name: "Items", columns: Object.keys(status.items[0] ?? {}), rows: status.items.map(itemStatusToExcelRow), frozenRows: 1, filter: true },
      { name: "Recipes", columns: Object.keys(recipeRows[0] ?? {}), rows: recipeRows, frozenRows: 1, filter: true },
      { name: "Recipe Inputs", columns: Object.keys(inputRows[0] ?? {}), rows: inputRows, frozenRows: 1, filter: true },
      { name: "Recipe Outputs", columns: Object.keys(outputRows[0] ?? {}), rows: outputRows, frozenRows: 1, filter: true },
      { name: "Loot", columns: Object.keys(lootRows[0] ?? {}), rows: lootRows, frozenRows: 1, filter: true },
      {
        name: "Issues",
        columns: ["severity", "code", "entity_type", "entity_id", "message"],
        rows: project.issues.map((issue) => ({ severity: issue.severity, code: issue.code, entity_type: issue.entityType, entity_id: issue.entityId, message: issue.message })),
        frozenRows: 1,
        filter: true
      }
    ]
  };
}

function itemStatusToExcelRow(item: ItemStatusRecord): Record<string, JsonValue> {
  return {
    ...item,
    canonical_components: stableStringify(item.canonical_components),
    canonical_variant: stableStringify(item.canonical_variant),
    issues: item.issues.join(" | ")
  };
}

export function exportExcelInterfaceJson(project: WorkbenchProject): string {
  return stableStringify(toExcelExportInterface(project), 2) + "\n";
}

function indentAfterFirst(text: string, spaces: number): string {
  const prefix = " ".repeat(spaces);
  return text.replaceAll("\n", `\n${prefix}`);
}
