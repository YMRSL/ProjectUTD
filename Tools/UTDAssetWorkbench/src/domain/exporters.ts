import {
  BLOCK_TRANSFORM_SCHEMA,
  EXCEL_INTERFACE_SCHEMA,
  LANG_OVERLAY_SCHEMA,
  PRESENTATION_SCHEMA,
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
import { hydrateWorkbenchProject } from "./projectCompat";
import {
  describeBlockTransformIssueZhCn,
  normalizeBlockTransformRuleId,
  planBlockTransformExport,
  validateBlockTransforms
} from "./blockTransforms";
import { cloneJson, stableStringify } from "./stable";
export { exportItemPropertiesJson } from "./itemProperties";

export function assertWorkbenchProject(value: unknown): asserts value is WorkbenchProject {
  hydrateWorkbenchProject(value);
}

export function exportProjectJson(project: WorkbenchProject): string {
  return stableStringify(project, 2) + "\n";
}

export function toPresentationDocument(project: WorkbenchProject): JsonObject {
  return {
    schema_version: PRESENTATION_SCHEMA,
    producer: "utd_asset_workbench",
    project_id: project.manifest.projectId,
    updated_at: project.manifest.generatedAt,
    drafts: project.presentations.map((entry) => {
      const item = project.items.find((candidate) => candidate.itemKey === entry.itemKey);
      return {
        asset_key: entry.itemKey,
        registry_id: entry.registryId,
        variant_discriminator: entry.variantDiscriminator,
        apply_scope: entry.applyScope,
        observed_name_zh_cn: entry.observedNameZhCn || item?.clientNameZhCn || "",
        name_zh_cn: entry.nameZhCn,
        description_zh_cn: presentationDescriptionLines(entry.descriptionZhCn),
        enabled: entry.enabled,
        base_catalog_hash: entry.baseCatalogHash || item?.catalogHash || "",
        updated_at: entry.updatedAt || project.manifest.generatedAt,
        name_key: entry.nameKey,
        description_key: entry.descriptionKey
      };
    })
  };
}

export function exportPresentationJson(project: WorkbenchProject): string {
  return stableStringify(toPresentationDocument(project), 2) + "\n";
}

export function toLangOverlayEntries(project: WorkbenchProject): Record<string, JsonObject> {
  const namespaces: Record<string, JsonObject> = {};
  const active = [...project.presentations]
    .filter((entry) => entry.enabled)
    .sort((a, b) => a.itemKey.localeCompare(b.itemKey));
  for (const entry of active) {
    const namespace = languageNamespace(entry.nameKey, entry.registryId);
    const values = namespaces[namespace] ?? {};
    if (entry.nameZhCn.trim()) values[entry.nameKey] = entry.nameZhCn;
    if (entry.descriptionZhCn.trim()) values[entry.descriptionKey] = entry.descriptionZhCn;
    namespaces[namespace] = values;
  }
  return namespaces;
}

export function exportLangOverlaysJson(project: WorkbenchProject): string {
  return stableStringify({
    schema_version: LANG_OVERLAY_SCHEMA,
    project_id: project.manifest.projectId,
    generated_at: project.manifest.generatedAt,
    language: "zh_cn",
    namespaces: toLangOverlayEntries(project)
  }, 2) + "\n";
}

export function toBlockTransformDocument(project: WorkbenchProject): JsonObject {
  const errors = validateBlockTransforms(project.blockTransforms)
    .filter((issue) => issue.severity === "error");
  if (errors.length) {
    const details = errors.slice(0, 5)
      .map((issue) => `${issue.entityId}：${describeBlockTransformIssueZhCn(issue.code, issue.message)}`)
      .join("；");
    const remainder = errors.length > 5 ? `；另有 ${errors.length - 5} 个错误` : "";
    throw new Error(`方块替换规则存在 ${errors.length} 个阻断错误，候选包未生成：${details}${remainder}`);
  }
  const plan = planBlockTransformExport(project.blockTransforms);
  if (plan.blocking.length) {
    const ids = [...new Set(plan.blocking.map((entry) => entry.id))].join(", ");
    throw new Error(`方块替换规则仍有无法导出的启用项：${ids}。请先修复或停用。`);
  }
  return {
    schema_version: BLOCK_TRANSFORM_SCHEMA,
    rules: plan.deployable.map((entry) => ({
      id: normalizeBlockTransformRuleId(entry.id),
      enabled: entry.enabled,
      priority: entry.priority,
      target: {
        block: entry.clickedBlock,
        state: entry.targetState,
        blockEntityPolicy: entry.blockEntityPolicy
      },
      catalyst: {
        registryId: entry.catalyst.ref,
        variantDiscriminator: entry.catalyst.variantDiscriminator ?? "",
        componentsSnbt: entry.catalystComponentsSnbt || "{}",
        count: entry.catalyst.count,
        source: entry.inputSource,
        consume: entry.consumeInput
      },
      activation: {
        hand: entry.hand,
        requireSneak: entry.requireSneaking,
        allowFakePlayer: entry.allowFakePlayer
      },
      result: {
        block: entry.resultBlock,
        state: entry.resultState,
        copyProperties: entry.copyProperties
      },
      creative: {
        requireInput: entry.creativeRequireInput,
        consume: entry.creativeConsume
      }
    }))
  };
}

export function exportBlockTransformsJson(project: WorkbenchProject): string {
  return stableStringify(toBlockTransformDocument(project), 2) + "\n";
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
    category_key: item.categoryKey,
    category_label_zh_cn: item.categoryLabelZhCn,
    category_level: item.categoryLevel,
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
        name: "Presentation",
        columns: ["item_key", "registry_id", "variant_discriminator", "apply_scope", "enabled", "name_key", "description_key", "name_zh_cn", "description_zh_cn"],
        rows: project.presentations.map((entry) => ({
          item_key: entry.itemKey,
          registry_id: entry.registryId,
          variant_discriminator: entry.variantDiscriminator,
          apply_scope: entry.applyScope,
          enabled: entry.enabled,
          name_key: entry.nameKey,
          description_key: entry.descriptionKey,
          name_zh_cn: entry.nameZhCn,
          description_zh_cn: entry.descriptionZhCn
        })),
        frozenRows: 1,
        filter: true
      },
      {
        name: "Block Transforms",
        columns: ["id", "enabled", "priority", "clicked_block", "target_state", "result_block", "result_state", "copy_properties", "catalyst", "variant_discriminator", "components_snbt", "count", "input_source", "hand", "require_sneaking", "allow_fake_player", "consume_input", "block_entity_policy", "creative_require_input", "creative_consume"],
        rows: project.blockTransforms.map((entry) => ({
          id: entry.id,
          enabled: entry.enabled,
          priority: entry.priority,
          clicked_block: entry.clickedBlock,
          target_state: stableStringify(entry.targetState),
          result_block: entry.resultBlock,
          result_state: stableStringify(entry.resultState),
          copy_properties: entry.copyProperties.join(" | "),
          catalyst: entry.catalyst.ref,
          variant_discriminator: entry.catalyst.variantDiscriminator ?? "",
          components_snbt: entry.catalystComponentsSnbt,
          count: entry.catalyst.count,
          input_source: entry.inputSource,
          hand: entry.hand,
          require_sneaking: entry.requireSneaking,
          allow_fake_player: entry.allowFakePlayer,
          consume_input: entry.consumeInput,
          block_entity_policy: entry.blockEntityPolicy,
          creative_require_input: entry.creativeRequireInput,
          creative_consume: entry.creativeConsume
        })),
        frozenRows: 1,
        filter: true
      },
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

function languageNamespace(nameKey: string, registryId: string): string {
  const keyParts = nameKey.split(".");
  if ((keyParts[0] === "item" || keyParts[0] === "block" || keyParts[0] === "tooltip") && keyParts[1]) {
    return keyParts[1];
  }
  return registryId.includes(":") ? registryId.slice(0, registryId.indexOf(":")) : "unknown";
}

function presentationDescriptionLines(value: string): string[] {
  return value
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter(Boolean);
}
