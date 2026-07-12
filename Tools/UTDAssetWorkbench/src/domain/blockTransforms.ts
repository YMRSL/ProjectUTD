import type { BlockTransform, ValidationIssue } from "./schema";
import { stableStringify } from "./stable";

const NAMESPACED_ID = /^[a-z0-9_.-]+:[a-z0-9_./-]+$/;
const LEGACY_RULE_ID = /^[a-z0-9_.-]{1,128}$/;
const BLOCK_STATE_PROPERTY = /^[a-z0-9_]+(?:[.-][a-z0-9_]+)*$/;
const JAVA_INT_MIN = -2_147_483_648;
const JAVA_INT_MAX = 2_147_483_647;

export interface BlockTransformProblem {
  code: string;
  message: string;
}

/** Java canonicalizes rule ids to lowercase before validating them. */
export function normalizeBlockTransformRuleId(value: string): string {
  return value.trim().toLowerCase();
}

/**
 * Mirrors every structural constraint enforced by BlockTransformConfigParser.
 * Runtime-only registry and SNBT checks deliberately remain Java's job.
 */
export function blockTransformProblems(rule: BlockTransform): BlockTransformProblem[] {
  const problems: BlockTransformProblem[] = [];
  const normalizedId = normalizeBlockTransformRuleId(rule.id);
  if (!LEGACY_RULE_ID.test(normalizedId) && !NAMESPACED_ID.test(normalizedId)) {
    problems.push({
      code: "block_transform_invalid_rule_id",
      message: "Rule id must be a Java-compatible simple id or namespaced resource-location id."
    });
  }
  if (!isJavaInt(rule.priority)) {
    problems.push({
      code: "block_transform_invalid_priority",
      message: "Priority must be a 32-bit integer."
    });
  }
  validateRegistryId(problems, "target", rule.clickedBlock);
  validateRegistryId(problems, "result", rule.resultBlock);
  validateRegistryId(problems, "catalyst", rule.catalyst.ref);
  if (!isJavaInt(rule.catalyst.count) || rule.catalyst.count < 1) {
    problems.push({
      code: "block_transform_invalid_count",
      message: "Catalyst count must be a 32-bit integer of at least 1."
    });
  }
  validateState(problems, "target", rule.targetState);
  validateState(problems, "result", rule.resultState);
  const copied = new Set<string>();
  for (const value of rule.copyProperties) {
    const property = value.trim().toLowerCase();
    if (!BLOCK_STATE_PROPERTY.test(property)) {
      problems.push({
        code: "block_transform_invalid_copy_property",
        message: `Invalid copied block-state property: ${value || "<blank>"}.`
      });
    } else if (copied.has(property)) {
      problems.push({
        code: "block_transform_duplicate_copy_property",
        message: `Copied block-state property is duplicated: ${property}.`
      });
    }
    copied.add(property);
  }
  if (rule.blockEntityPolicy !== "reject") {
    problems.push({
      code: "block_transform_invalid_block_entity_policy",
      message: "Block entity policy must be reject in v1."
    });
  }
  if (rule.creativeConsume && !rule.creativeRequireInput) {
    problems.push({
      code: "block_transform_invalid_creative_policy",
      message: "Creative consume=true requires requireInput=true."
    });
  }
  return problems;
}

/** True only when one row can be parsed by the Java v1 configuration parser. */
export function isDeployableBlockTransform(rule: BlockTransform): boolean {
  return blockTransformProblems(rule).length === 0;
}

/**
 * Disabled incomplete rows are authoring drafts and are omitted. Enabled
 * incomplete rows are blockers. Case-insensitive duplicate ids are also
 * omitted when all rows are disabled, or block when any duplicate is enabled.
 */
export function planBlockTransformExport(rules: BlockTransform[]): {
  deployable: BlockTransform[];
  blocking: BlockTransform[];
  omittedDrafts: BlockTransform[];
} {
  const structurallyValid = rules.filter(isDeployableBlockTransform);
  const invalid = rules.filter((rule) => !isDeployableBlockTransform(rule));
  const ids = countBy(structurallyValid.map((rule) => normalizeBlockTransformRuleId(rule.id)));
  const duplicates = structurallyValid.filter((rule) =>
    (ids.get(normalizeBlockTransformRuleId(rule.id)) ?? 0) > 1
  );
  const duplicateSet = new Set(duplicates);
  const blocking = [
    ...invalid.filter((rule) => rule.enabled),
    ...duplicates.filter((rule) => rule.enabled)
  ];
  const omittedDrafts = [
    ...invalid.filter((rule) => !rule.enabled),
    ...duplicates.filter((rule) => !rule.enabled)
  ];
  return {
    deployable: structurallyValid.filter((rule) => !duplicateSet.has(rule)),
    blocking,
    omittedDrafts
  };
}

/** Workbench-side checks required before a rule document is deployable. */
export function validateBlockTransforms(rules: BlockTransform[]): ValidationIssue[] {
  const issues: ValidationIssue[] = [];
  const idCounts = countBy(rules
    .map((rule) => normalizeBlockTransformRuleId(rule.id))
    .filter(isValidRuleId));

  for (const rule of rules) {
    const problems = blockTransformProblems(rule);
    if (problems.length && !rule.enabled) {
      issues.push(issue(
        "block_transform_draft_incomplete",
        "warning",
        rule,
        `Disabled draft is incomplete and will be omitted from runtime export: ${problems.map((entry) => entry.message).join(" ")}`
      ));
    } else {
      for (const problem of problems) {
        issues.push(issue(problem.code, "error", rule, problem.message));
      }
    }
    const normalizedId = normalizeBlockTransformRuleId(rule.id);
    if (isValidRuleId(normalizedId) && (idCounts.get(normalizedId) ?? 0) > 1) {
      issues.push(issue(
        "block_transform_duplicate_id",
        rule.enabled ? "error" : "warning",
        rule,
        `Java-normalized block transform id is duplicated: ${normalizeBlockTransformRuleId(rule.id)}`
      ));
    }
    if (rule.inputSource === "inventory" && !rule.requireSneaking) {
      issues.push(issue(
        "block_transform_inventory_without_sneak",
        "warning",
        rule,
        "Inventory catalyst rules should require sneaking to avoid accidental block replacement."
      ));
    }
  }

  const conflicts = new Map<string, BlockTransform[]>();
  for (const rule of rules.filter((candidate) => candidate.enabled)) {
    const key = `${rule.priority}\u0000${rule.clickedBlock}\u0000${stableStringify(rule.targetState)}`;
    const group = conflicts.get(key) ?? [];
    group.push(rule);
    conflicts.set(key, group);
  }
  for (const group of conflicts.values()) {
    if (group.length < 2) continue;
    const ids = group.map((rule) => rule.id).join(", ");
    for (const rule of group) {
      issues.push(issue(
        "block_transform_priority_conflict",
        "error",
        rule,
        `Enabled rules share the same target and priority ${rule.priority}: ${ids}`
      ));
    }
  }
  return issues;
}

function validateRegistryId(
  problems: BlockTransformProblem[],
  field: "target" | "result" | "catalyst",
  value: string
): void {
  if (NAMESPACED_ID.test(value.trim().toLowerCase())) return;
  problems.push({
    code: `block_transform_invalid_${field}_id`,
    message: `Rule ${field} must be a namespaced id, for example minecraft:stone.`
  });
}

function validateState(
  problems: BlockTransformProblem[],
  field: "target" | "result",
  state: Record<string, string>
): void {
  for (const [rawProperty, rawValue] of Object.entries(state)) {
    const property = rawProperty.trim().toLowerCase();
    if (!BLOCK_STATE_PROPERTY.test(property)) {
      problems.push({
        code: `block_transform_invalid_${field}_state_property`,
        message: `Invalid ${field} block-state property: ${rawProperty || "<blank>"}.`
      });
    }
    if (!rawValue.trim()) {
      problems.push({
        code: `block_transform_invalid_${field}_state_value`,
        message: `${field} block-state value for ${property || "<blank>"} must not be blank.`
      });
    }
  }
}

function isJavaInt(value: number): boolean {
  return Number.isInteger(value) && value >= JAVA_INT_MIN && value <= JAVA_INT_MAX;
}

function isValidRuleId(value: string): boolean {
  return LEGACY_RULE_ID.test(value) || NAMESPACED_ID.test(value);
}

function issue(
  code: string,
  severity: ValidationIssue["severity"],
  rule: BlockTransform,
  message: string
): ValidationIssue {
  return { code, severity, message, entityType: "block_transform", entityId: rule.id };
}

function countBy(values: string[]): Map<string, number> {
  const counts = new Map<string, number>();
  for (const value of values) counts.set(value, (counts.get(value) ?? 0) + 1);
  return counts;
}
