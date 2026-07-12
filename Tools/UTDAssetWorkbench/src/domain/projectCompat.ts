import { constrainPresentationScope, derivePresentationKeys } from "./presentation";
import { validateBlockTransforms } from "./blockTransforms";
import { refreshProject } from "./mutations";
import {
  BLOCK_TRANSFORM_SCHEMA,
  PRESENTATION_SCHEMA,
  WORKBENCH_SCHEMA,
  type BlockTransform,
  type CanonicalItem,
  type ItemPresentationOverride,
  type PresentationApplyScope,
  type WorkbenchProject
} from "./schema";
import { isRecord, numberOr, stringOr } from "./stable";

/**
 * Hydrates additive v1 fields in place so old workbench/v1 files continue to
 * open without a schema-version fork.
 */
export function hydrateWorkbenchProject(value: unknown): WorkbenchProject {
  if (!isRecord(value) || value.schemaVersion !== WORKBENCH_SCHEMA) {
    throw new Error(`Expected ${WORKBENCH_SCHEMA} project JSON.`);
  }
  const project = value as unknown as WorkbenchProject;
  project.presentations = normalizePresentations(project);
  project.blockTransforms = normalizeBlockTransforms(project);
  project.manifest.counts.presentations = project.presentations.length;
  project.manifest.counts.blockTransforms = project.blockTransforms.length;
  project.issues = project.issues
    .filter((issue) => issue.entityType !== "block_transform")
    .concat(validateBlockTransforms(project.blockTransforms));
  project.manifest.counts.issues = project.issues.length;
  return project;
}

/**
 * Replaces the project's presentation rows with a Mod-compatible
 * utd-item-presentation/v1 draft document. Runtime asset keys are preferred;
 * a unique registry + discriminator match bridges producer-specific keys.
 */
export function applyPresentationDocument(project: WorkbenchProject, value: unknown): WorkbenchProject {
  if (!isRecord(value) || value.schema_version !== PRESENTATION_SCHEMA) {
    throw new Error(`Expected ${PRESENTATION_SCHEMA} presentation JSON.`);
  }
  const rawRows = Array.isArray(value.drafts)
    ? value.drafts
    : Array.isArray(value.overrides)
      ? value.overrides
      : null;
  if (rawRows === null) {
    throw new Error(`${PRESENTATION_SCHEMA} presentation JSON must contain a drafts array.`);
  }

  const next = structuredClone(project);
  const semanticTargets = new Set<string>();
  next.presentations = rawRows.map((raw, index) => {
    if (!isRecord(raw)) throw new Error(`Presentation draft ${index + 1} must be an object.`);
    const item = resolvePresentationItem(next, raw, index);
    const applyScope = constrainPresentationScope(item, strictPresentationScope(raw, index));
    const semanticTarget = applyScope === "registry"
      ? `registry\u0000${item.registryId}`
      : `identity\u0000${item.registryId}\u0000${item.variantDiscriminator}`;
    if (semanticTargets.has(semanticTarget)) {
      throw new Error(`Presentation draft ${index + 1} duplicates target ${item.registryId} / ${item.variantDiscriminator || "<base>"}.`);
    }
    semanticTargets.add(semanticTarget);
    return normalizePresentationRow(raw, item, applyScope, next.manifest.generatedAt);
  });
  return refreshProject(next, new Set(next.presentations.map((entry) => entry.itemKey)));
}

/**
 * Replaces authored transforms with the runtime-facing
 * utd-block-transforms/v1 document. The preferred Java contract uses
 * rules[] with nested target/catalyst/activation/result/creative objects;
 * block_transforms[] remains accepted for the retired Workbench exporter.
 */
export function applyBlockTransformDocument(project: WorkbenchProject, value: unknown): WorkbenchProject {
  if (!isRecord(value)) throw new Error(`Expected ${BLOCK_TRANSFORM_SCHEMA} block transform JSON.`);
  const preferredSchema = stringOr(value.schema_version);
  const legacySchema = stringOr(value.schema);
  if (preferredSchema && legacySchema && preferredSchema !== legacySchema) {
    throw new Error("Block transform schema_version conflicts with legacy schema.");
  }
  const schema = preferredSchema || legacySchema;
  if (schema !== BLOCK_TRANSFORM_SCHEMA) {
    throw new Error(`Expected ${BLOCK_TRANSFORM_SCHEMA} block transform JSON.`);
  }
  const rows = Array.isArray(value.rules)
    ? value.rules
    : Array.isArray(value.block_transforms)
      ? value.block_transforms
      : null;
  if (rows === null) {
    throw new Error(`${BLOCK_TRANSFORM_SCHEMA} block transform JSON must contain a rules array.`);
  }
  const next = structuredClone(project);
  next.blockTransforms = normalizeBlockTransformRows(next, rows);
  const touched = new Set(next.items
    .filter((item) => next.blockTransforms.some((rule) => catalystMatchesItem(rule, item)))
    .map((item) => item.itemKey));
  return refreshProject(next, touched);
}

function normalizePresentations(project: WorkbenchProject): ItemPresentationOverride[] {
  const rows: unknown[] = Array.isArray(project.presentations) ? project.presentations : [];
  return rows.flatMap((raw) => {
    if (!isRecord(raw)) return [];
    const itemKey = stringOr(raw.itemKey ?? raw.item_key);
    const item = project.items.find((candidate) => candidate.itemKey === itemKey);
    if (!item) return [];
    const requestedScope = raw.applyScope === "registry" || raw.apply_scope === "registry" ? "registry" : "identity";
    const applyScope = constrainPresentationScope(item, requestedScope);
    return [normalizePresentationRow(raw, item, applyScope, project.manifest.generatedAt, raw.enabled === true)];
  });
}

function normalizePresentationRow(
  raw: Record<string, unknown>,
  item: CanonicalItem,
  applyScope: PresentationApplyScope,
  fallbackUpdatedAt: string,
  enabled = raw.enabled !== false
): ItemPresentationOverride {
  const keys = derivePresentationKeys(item, applyScope);
  return {
    itemKey: item.itemKey,
    registryId: item.registryId,
    variantDiscriminator: item.variantDiscriminator,
    applyScope,
    enabled,
    observedNameZhCn: stringOr(raw.observedNameZhCn ?? raw.observed_name_zh_cn, item.clientNameZhCn),
    nameKey: stringOr(raw.nameKey ?? raw.name_key, keys.nameKey),
    descriptionKey: stringOr(raw.descriptionKey ?? raw.description_key, keys.descriptionKey),
    nameZhCn: stringOr(raw.nameZhCn ?? raw.name_zh_cn, item.clientNameZhCn),
    descriptionZhCn: presentationDescriptionText(raw.descriptionZhCn ?? raw.description_zh_cn),
    baseCatalogHash: stringOr(raw.baseCatalogHash ?? raw.base_catalog_hash, item.catalogHash),
    updatedAt: stringOr(raw.updatedAt ?? raw.updated_at, fallbackUpdatedAt)
  };
}

function resolvePresentationItem(
  project: WorkbenchProject,
  raw: Record<string, unknown>,
  index: number
): CanonicalItem {
  const assetKey = stringOr(raw.assetKey ?? raw.asset_key ?? raw.itemKey ?? raw.item_key);
  if (assetKey) {
    const exact = project.items.filter((candidate) => candidate.itemKey === assetKey);
    if (exact.length === 1) return exact[0];
    if (exact.length > 1) {
      throw new Error(`Presentation draft ${index + 1} asset_key ${assetKey} is ambiguous in the canonical catalog.`);
    }
  }

  const registryId = stringOr(raw.registryId ?? raw.registry_id);
  const discriminator = stringOr(raw.variantDiscriminator ?? raw.variant_discriminator);
  if (!registryId) throw new Error(`Presentation draft ${index + 1} has no usable asset_key or registry_id.`);
  const semantic = project.items.filter((candidate) =>
    candidate.registryId === registryId && candidate.variantDiscriminator === discriminator
  );
  if (semantic.length === 1) return semantic[0];
  if (!semantic.length) {
    throw new Error(`Presentation draft ${index + 1} does not match a canonical item: ${registryId} / ${discriminator || "<base>"}.`);
  }
  throw new Error(
    `Presentation draft ${index + 1} is ambiguous for ${registryId} / ${discriminator || "<base>"}: `
      + semantic.map((candidate) => candidate.itemKey).join(", ")
  );
}

function strictPresentationScope(raw: Record<string, unknown>, index: number): PresentationApplyScope {
  const scope = raw.applyScope ?? raw.apply_scope;
  if (scope === "registry" || scope === "identity") return scope;
  throw new Error(`Presentation draft ${index + 1} apply_scope must be registry or identity.`);
}

function presentationDescriptionText(value: unknown): string {
  if (typeof value === "string") return value;
  if (Array.isArray(value)) return value.filter((line): line is string => typeof line === "string").join("\n");
  return "";
}

function normalizeBlockTransforms(project: WorkbenchProject): BlockTransform[] {
  const rows: unknown[] = Array.isArray(project.blockTransforms) ? project.blockTransforms : [];
  return normalizeBlockTransformRows(project, rows);
}

function normalizeBlockTransformRows(project: WorkbenchProject, rows: unknown[]): BlockTransform[] {
  return rows.flatMap((raw, index) => {
    if (!isRecord(raw)) return [];
    const targetRaw = isRecord(raw.target) ? raw.target : {};
    const catalystRaw = isRecord(raw.catalyst) ? raw.catalyst : {};
    const activationRaw = isRecord(raw.activation) ? raw.activation : {};
    const resultRaw = isRecord(raw.result) ? raw.result : {};
    const creativeRaw = isRecord(raw.creative) ? raw.creative : {};
    const ref = stringOr(
      catalystRaw.registryId ?? catalystRaw.registry_id ?? catalystRaw.ref ?? catalystRaw.item
        ?? raw.catalystItem ?? raw.catalyst_item
    );
    const discriminator = stringOr(catalystRaw.variantDiscriminator ?? catalystRaw.variant_discriminator);
    const suppliedIdentityKey = stringOr(catalystRaw.identityKey ?? catalystRaw.identity_key);
    const matchingItems = project.items.filter((item) =>
      item.registryId === ref && item.variantDiscriminator === discriminator
    );
    const identityKey = suppliedIdentityKey || (matchingItems.length === 1 ? matchingItems[0].itemKey : "");
    const itemComponentsSnbt = matchingItems.length === 1 ? matchingItems[0].componentsSnbt : "";
    const source = stringOr(catalystRaw.source ?? raw.inputSource ?? raw.input_source);
    const rawHand = stringOr(activationRaw.hand ?? raw.hand).toLowerCase();
    const creativeRequireInput = booleanOr(
      creativeRaw.requireInput ?? creativeRaw.require_input ?? raw.creativeRequireInput ?? raw.creative_require_input,
      true
    );
    return [{
      id: stringOr(raw.id, `utd:block_transform/imported_${index + 1}`),
      enabled: raw.enabled === true,
      priority: Math.trunc(numberOr(raw.priority, 0)),
      clickedBlock: stringOr(targetRaw.block ?? raw.clickedBlock ?? raw.clicked_block),
      targetState: stringRecord(targetRaw.state ?? raw.targetState ?? raw.target_state),
      resultBlock: stringOr(resultRaw.block ?? raw.resultBlock ?? raw.result_block),
      resultState: stringRecord(resultRaw.state ?? raw.resultState ?? raw.result_state),
      copyProperties: stringList(resultRaw.copyProperties ?? resultRaw.copy_properties ?? raw.copyProperties ?? raw.copy_properties),
      catalyst: {
        refKind: "item",
        ref,
        identityKey: identityKey || undefined,
        variantDiscriminator: discriminator || undefined,
        count: Math.trunc(numberOr(catalystRaw.count, 1))
      },
      catalystComponentsSnbt: stringOr(
        catalystRaw.componentsSnbt ?? catalystRaw.components_snbt
          ?? raw.catalystComponentsSnbt ?? raw.catalyst_components_snbt,
        itemComponentsSnbt || "{}"
      ) || "{}",
      inputSource: source === "inventory" ? "inventory" : "clicked_hand",
      hand: rawHand === "off" || rawHand === "offhand" ? "off" : rawHand === "any" || rawHand === "either" ? "any" : "main",
      requireSneaking: activationRaw.requireSneak === true || activationRaw.require_sneak === true
        || raw.requireSneaking === true || raw.require_sneaking === true,
      allowFakePlayer: activationRaw.allowFakePlayer === true || activationRaw.allow_fake_player === true
        || raw.allowFakePlayer === true || raw.allow_fake_player === true,
      consumeInput: booleanOr(catalystRaw.consume ?? raw.consumeInput ?? raw.consume_input, true),
      cancelInteraction: raw.cancelInteraction !== false && raw.cancel_interaction !== false,
      blockEntityPolicy: "reject",
      creativeRequireInput,
      creativeConsume: creativeRequireInput && booleanOr(
        creativeRaw.consume ?? raw.creativeConsume ?? raw.creative_consume,
        false
      )
    } satisfies BlockTransform];
  });
}

function catalystMatchesItem(rule: BlockTransform, item: CanonicalItem): boolean {
  if (rule.catalyst.identityKey) return rule.catalyst.identityKey === item.itemKey;
  return rule.catalyst.ref === item.registryId
    && (rule.catalyst.variantDiscriminator ?? "") === item.variantDiscriminator;
}

function stringRecord(value: unknown): Record<string, string> {
  if (!isRecord(value)) return {};
  return Object.fromEntries(Object.entries(value)
    .filter((entry): entry is [string, string] => typeof entry[1] === "string")
    .map(([key, entry]) => [key.trim().toLowerCase(), entry.trim().toLowerCase()])
    .filter(([key]) => Boolean(key)));
}

function stringList(value: unknown): string[] {
  if (!Array.isArray(value)) return [];
  return [...new Set(value.filter((entry): entry is string => typeof entry === "string")
    .map((entry) => entry.trim().toLowerCase())
    .filter(Boolean))];
}

function booleanOr(value: unknown, fallback: boolean): boolean {
  return typeof value === "boolean" ? value : fallback;
}
