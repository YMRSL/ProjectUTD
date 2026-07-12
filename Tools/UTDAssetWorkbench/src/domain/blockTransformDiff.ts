import { toBlockTransformDocument } from "./exporters";
import { normalizeBlockTransformRuleId } from "./blockTransforms";
import {
  BLOCK_TRANSFORM_SCHEMA,
  WORKBENCH_SCHEMA,
  type WorkbenchProject
} from "./schema";
import { hydrateWorkbenchProject } from "./projectCompat";
import { isRecord, stableStringify } from "./stable";

export const BLOCK_TRANSFORM_DIFF_SCHEMA = "utd-block-transform-diff/v1" as const;

export interface BlockTransformDiffRule {
  id: string;
  enabled: boolean;
  priority: number;
  target: {
    block: string;
    state: Record<string, string>;
    blockEntityPolicy: string;
  };
  catalyst: {
    registryId: string;
    variantDiscriminator: string;
    componentsSnbt: string;
    count: number;
    source: string;
    consume: boolean;
  };
  activation: {
    hand: string;
    requireSneak: boolean;
    allowFakePlayer: boolean;
  };
  result: {
    block: string;
    state: Record<string, string>;
    copyProperties: string[];
  };
  creative: {
    requireInput: boolean;
    consume: boolean;
  };
}

export type BlockTransformDiffField =
  | "id"
  | "enabled"
  | "priority"
  | "target"
  | "catalyst"
  | "activation"
  | "result"
  | "creative";

export interface BlockTransformModification {
  id: string;
  changed_fields: BlockTransformDiffField[];
  before: BlockTransformDiffRule;
  after: BlockTransformDiffRule;
}

export interface BlockTransformDiff {
  schema_version: typeof BLOCK_TRANSFORM_DIFF_SCHEMA;
  summary: {
    baseline_sha256: string;
    candidate_sha256: string;
    baseline_rules: number;
    candidate_rules: number;
    baseline_enabled_rules: number;
    candidate_enabled_rules: number;
    added: number;
    removed: number;
    modified: number;
    enabled: number;
    disabled: number;
    unchanged: number;
  };
  added: BlockTransformDiffRule[];
  removed: BlockTransformDiffRule[];
  modified: BlockTransformModification[];
  enabled: string[];
  disabled: string[];
}

export interface BlockTransformDiffFingerprints {
  baselineSha256: string;
  candidateSha256: string;
}

const COMPARED_FIELDS: BlockTransformDiffField[] = [
  "id",
  "enabled",
  "priority",
  "target",
  "catalyst",
  "activation",
  "result",
  "creative"
];

const REGISTRY_ID = /^[a-z0-9_.-]+:[a-z0-9_./-]+$/;
const SIMPLE_RULE_ID = /^[a-z0-9_.-]{1,128}$/;
const NAMESPACED_RULE_ID = /^[a-z0-9_.-]+:[a-z0-9_./-]+$/;
const PROPERTY_NAME = /^[a-z0-9_]+(?:[.-][a-z0-9_]+)*$/;
const JAVA_INT_MIN = -2_147_483_648;
const JAVA_INT_MAX = 2_147_483_647;

/**
 * Accepts either a canonical Workbench project or the runtime-facing
 * utd-block-transforms/v1 document and returns one deterministic rule list.
 */
export function blockTransformRulesFromSource(value: unknown): BlockTransformDiffRule[] {
  let document: unknown = value;
  if (isRecord(value) && value.schemaVersion === WORKBENCH_SCHEMA) {
    const project = hydrateWorkbenchProject(structuredClone(value)) as WorkbenchProject;
    document = toBlockTransformDocument(project);
  }
  return rulesFromDocument(document).sort(compareRules);
}

export function diffBlockTransformSources(
  baseline: unknown,
  candidate: unknown,
  fingerprints: BlockTransformDiffFingerprints
): BlockTransformDiff {
  const baselineRules = blockTransformRulesFromSource(baseline);
  const candidateRules = blockTransformRulesFromSource(candidate);
  const baselineById = indexRules(baselineRules, "baseline");
  const candidateById = indexRules(candidateRules, "candidate");
  const added: BlockTransformDiffRule[] = [];
  const removed: BlockTransformDiffRule[] = [];
  const modified: BlockTransformModification[] = [];
  const enabled: string[] = [];
  const disabled: string[] = [];
  let unchanged = 0;

  for (const rule of baselineRules) {
    const key = normalizeBlockTransformRuleId(rule.id);
    const next = candidateById.get(key);
    if (!next) {
      removed.push(rule);
      continue;
    }
    const changedFields = COMPARED_FIELDS.filter((field) =>
      stableStringify(rule[field]) !== stableStringify(next[field])
    );
    if (!changedFields.length) {
      unchanged += 1;
      continue;
    }
    modified.push({
      id: next.id,
      changed_fields: changedFields,
      before: rule,
      after: next
    });
    if (!rule.enabled && next.enabled) enabled.push(next.id);
    if (rule.enabled && !next.enabled) disabled.push(next.id);
  }

  for (const rule of candidateRules) {
    if (!baselineById.has(normalizeBlockTransformRuleId(rule.id))) added.push(rule);
  }

  return {
    schema_version: BLOCK_TRANSFORM_DIFF_SCHEMA,
    summary: {
      baseline_sha256: fingerprints.baselineSha256,
      candidate_sha256: fingerprints.candidateSha256,
      baseline_rules: baselineRules.length,
      candidate_rules: candidateRules.length,
      baseline_enabled_rules: baselineRules.filter((rule) => rule.enabled).length,
      candidate_enabled_rules: candidateRules.filter((rule) => rule.enabled).length,
      added: added.length,
      removed: removed.length,
      modified: modified.length,
      enabled: enabled.length,
      disabled: disabled.length,
      unchanged
    },
    added,
    removed,
    modified,
    enabled: enabled.sort(compareIds),
    disabled: disabled.sort(compareIds)
  };
}

export function exportBlockTransformDiffJson(diff: BlockTransformDiff): string {
  return stableStringify(diff, 2) + "\n";
}

function rulesFromDocument(value: unknown): BlockTransformDiffRule[] {
  const root = requireRecord(value, "$", `Expected ${BLOCK_TRANSFORM_SCHEMA} block transform JSON or ${WORKBENCH_SCHEMA} project JSON.`);
  const hasPreferred = hasNonNull(root, "schema_version");
  const hasLegacy = hasNonNull(root, "schema");
  if (!hasPreferred && !hasLegacy) {
    throw invalid("$.schema_version", "is required (legacy $.schema is also accepted)");
  }
  const preferredSchema = hasPreferred ? requireString(root.schema_version, "$.schema_version", false) : "";
  const legacySchema = hasLegacy ? requireString(root.schema, "$.schema", false) : "";
  if (hasPreferred && hasLegacy && preferredSchema !== legacySchema) {
    throw invalid("$.schema_version", "conflicts with legacy $.schema");
  }
  if ((hasPreferred ? preferredSchema : legacySchema) !== BLOCK_TRANSFORM_SCHEMA) {
    throw invalid("$.schema", `expected ${BLOCK_TRANSFORM_SCHEMA}`);
  }
  const rows = requireArray(required(root, "rules", "$"), "$.rules");
  const ids = new Set<string>();
  return rows.map((row, index) => {
    const rule = ruleFromDocumentRow(row, index);
    if (ids.has(rule.id)) {
      throw invalid(`$.rules[${index}].id`, `duplicate rule id ${rule.id}`);
    }
    ids.add(rule.id);
    return rule;
  });
}

function ruleFromDocumentRow(value: unknown, index: number): BlockTransformDiffRule {
  const path = `$.rules[${index}]`;
  const object = requireRecord(value, path);
  const id = requiredString(object, "id", path, true).toLowerCase();
  if (!SIMPLE_RULE_ID.test(id) && !NAMESPACED_RULE_ID.test(id)) {
    throw invalid(`${path}.id`, "must be a legacy simple id or a namespaced resource-location id");
  }
  const target = parseTarget(requireRecord(required(object, "target", path), `${path}.target`), `${path}.target`);
  const catalyst = parseCatalyst(
    requireRecord(required(object, "catalyst", path), `${path}.catalyst`),
    `${path}.catalyst`
  );
  const activation = hasOwn(object, "activation")
    ? parseActivation(requireRecord(object.activation, `${path}.activation`), `${path}.activation`)
    : { hand: "main", requireSneak: false, allowFakePlayer: false };
  const result = parseResult(requireRecord(required(object, "result", path), `${path}.result`), `${path}.result`);
  const creative = hasOwn(object, "creative")
    ? parseCreative(requireRecord(object.creative, `${path}.creative`), `${path}.creative`)
    : { requireInput: true, consume: false };
  if (creative.consume && !creative.requireInput) {
    throw invalid(`${path}.creative`, "consume=true requires requireInput=true");
  }
  return {
    id,
    enabled: optionalBoolean(object, "enabled", path, false),
    priority: optionalInt(object, "priority", path, 0),
    target,
    catalyst,
    activation,
    result,
    creative
  };
}

function parseTarget(object: Record<string, unknown>, path: string): BlockTransformDiffRule["target"] {
  const policy = optionalString(object, "blockEntityPolicy", path, "reject", true).toLowerCase();
  if (policy !== "reject") throw invalid(`${path}.blockEntityPolicy`, "v1 only supports reject");
  return {
    block: registryId(requiredString(object, "block", path, true), `${path}.block`),
    state: optionalState(object, "state", path),
    blockEntityPolicy: policy
  };
}

function parseCatalyst(object: Record<string, unknown>, path: string): BlockTransformDiffRule["catalyst"] {
  let componentsSnbt = optionalString(object, "componentsSnbt", path, "{}", false).trim();
  if (!componentsSnbt) componentsSnbt = "{}";
  const count = optionalInt(object, "count", path, 1);
  if (count < 1) throw invalid(`${path}.count`, "must be at least 1");
  const source = optionalString(object, "source", path, "clicked_hand", true).toLowerCase();
  if (source !== "clicked_hand" && source !== "inventory") {
    throw invalid(`${path}.source`, "expected clicked_hand or inventory");
  }
  return {
    registryId: registryId(requiredString(object, "registryId", path, true), `${path}.registryId`),
    variantDiscriminator: optionalString(object, "variantDiscriminator", path, "", false),
    componentsSnbt,
    count,
    source,
    consume: optionalBoolean(object, "consume", path, true)
  };
}

function parseActivation(object: Record<string, unknown>, path: string): BlockTransformDiffRule["activation"] {
  const hand = optionalString(object, "hand", path, "main", true).toLowerCase();
  if (hand !== "main" && hand !== "off" && hand !== "any") {
    throw invalid(`${path}.hand`, "expected main, off or any");
  }
  return {
    hand,
    requireSneak: optionalBoolean(object, "requireSneak", path, false),
    allowFakePlayer: optionalBoolean(object, "allowFakePlayer", path, false)
  };
}

function parseResult(object: Record<string, unknown>, path: string): BlockTransformDiffRule["result"] {
  const copyProperties: string[] = [];
  if (hasOwn(object, "copyProperties")) {
    const values = requireArray(object.copyProperties, `${path}.copyProperties`);
    const unique = new Set<string>();
    for (let index = 0; index < values.length; index += 1) {
      const property = requireString(values[index], `${path}.copyProperties[${index}]`, true).toLowerCase();
      validatePropertyName(property, `${path}.copyProperties[${index}]`);
      if (unique.has(property)) {
        throw invalid(`${path}.copyProperties[${index}]`, `duplicate property ${property}`);
      }
      unique.add(property);
      copyProperties.push(property);
    }
  }
  return {
    block: registryId(requiredString(object, "block", path, true), `${path}.block`),
    state: optionalState(object, "state", path),
    copyProperties
  };
}

function parseCreative(object: Record<string, unknown>, path: string): BlockTransformDiffRule["creative"] {
  return {
    requireInput: optionalBoolean(object, "requireInput", path, true),
    consume: optionalBoolean(object, "consume", path, false)
  };
}

function indexRules(rules: BlockTransformDiffRule[], label: string): Map<string, BlockTransformDiffRule> {
  const indexed = new Map<string, BlockTransformDiffRule>();
  for (const rule of rules) {
    const key = normalizeBlockTransformRuleId(rule.id);
    if (indexed.has(key)) throw new Error(`Duplicate Java-normalized block transform id in ${label}: ${key}.`);
    indexed.set(key, rule);
  }
  return indexed;
}

function optionalState(object: Record<string, unknown>, name: string, path: string): Record<string, string> {
  if (!hasOwn(object, name)) return {};
  const state = requireRecord(object[name], `${path}.${name}`);
  const normalized = new Map<string, string>();
  for (const [rawProperty, rawValue] of Object.entries(state)) {
    const property = rawProperty.trim().toLowerCase();
    validatePropertyName(property, `${path}.${name}.${rawProperty}`);
    const value = requireString(rawValue, `${path}.${name}.${property}`, true).toLowerCase();
    normalized.set(property, value);
  }
  return Object.fromEntries(normalized);
}

function registryId(value: string, path: string): string {
  const normalized = value.toLowerCase();
  if (!REGISTRY_ID.test(normalized)) throw invalid(path, "invalid namespaced registry id");
  return normalized;
}

function validatePropertyName(property: string, path: string): void {
  if (!PROPERTY_NAME.test(property)) throw invalid(path, "invalid block-state property name");
}

function required(object: Record<string, unknown>, name: string, path: string): unknown {
  if (!hasOwn(object, name) || object[name] === null || object[name] === undefined) {
    throw invalid(`${path}.${name}`, "is required");
  }
  return object[name];
}

function requiredString(
  object: Record<string, unknown>,
  name: string,
  path: string,
  nonBlank: boolean
): string {
  return requireString(required(object, name, path), `${path}.${name}`, nonBlank);
}

function optionalString(
  object: Record<string, unknown>,
  name: string,
  path: string,
  fallback: string,
  nonBlank: boolean
): string {
  if (!hasOwn(object, name) || object[name] === null) return fallback;
  return requireString(object[name], `${path}.${name}`, nonBlank);
}

function requireString(value: unknown, path: string, nonBlank: boolean): string {
  if (typeof value !== "string") throw invalid(path, "must be a string");
  const normalized = value.trim();
  if (nonBlank && !normalized) throw invalid(path, "must not be blank");
  return normalized;
}

function optionalBoolean(
  object: Record<string, unknown>,
  name: string,
  path: string,
  fallback: boolean
): boolean {
  if (!hasOwn(object, name) || object[name] === null) return fallback;
  if (typeof object[name] !== "boolean") throw invalid(`${path}.${name}`, "must be a boolean");
  return object[name];
}

function optionalInt(
  object: Record<string, unknown>,
  name: string,
  path: string,
  fallback: number
): number {
  if (!hasOwn(object, name) || object[name] === null) return fallback;
  const value = object[name];
  if (typeof value !== "number" || !Number.isInteger(value) || value < JAVA_INT_MIN || value > JAVA_INT_MAX) {
    throw invalid(`${path}.${name}`, "must be an integer");
  }
  return value;
}

function requireRecord(value: unknown, path: string, message = `${path}: must be an object`): Record<string, unknown> {
  if (!isRecord(value)) throw new Error(message);
  return value;
}

function requireArray(value: unknown, path: string): unknown[] {
  if (!Array.isArray(value)) throw invalid(path, "must be an array");
  return value;
}

function hasOwn(object: Record<string, unknown>, name: string): boolean {
  return Object.prototype.hasOwnProperty.call(object, name);
}

function hasNonNull(object: Record<string, unknown>, name: string): boolean {
  return hasOwn(object, name) && object[name] !== null;
}

function invalid(path: string, message: string): Error {
  return new Error(`${path}: ${message}`);
}

function compareRules(left: BlockTransformDiffRule, right: BlockTransformDiffRule): number {
  return compareIds(left.id, right.id);
}

function compareIds(left: string, right: string): number {
  return normalizeBlockTransformRuleId(left).localeCompare(normalizeBlockTransformRuleId(right), "en")
    || left.localeCompare(right, "en");
}
