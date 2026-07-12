import { blockTransformProblems, normalizeBlockTransformRuleId } from "./blockTransforms";
import type { BlockStateProperties, BlockTransform } from "./schema";

export type ParsedAuthoringInput<T> =
  | { ok: true; value: T }
  | { ok: false; error: string };

/**
 * Parses the small JSON object used by block-state match/result fields. The
 * editor deliberately rejects arrays, nested data, and non-string values so a
 * partially typed value can never leak into the canonical project.
 */
export function parseBlockStateInput(raw: string): ParsedAuthoringInput<BlockStateProperties> {
  if (!raw.trim()) return { ok: true, value: {} };
  let parsed: unknown;
  try {
    parsed = JSON.parse(raw);
  } catch {
    return { ok: false, error: "请输入有效 JSON，例如 {\"facing\":\"north\"}。" };
  }
  if (parsed === null || Array.isArray(parsed) || typeof parsed !== "object") {
    return { ok: false, error: "方块状态必须是 JSON 对象。" };
  }
  const entries = Object.entries(parsed);
  if (entries.some(([key]) => !key.trim())) {
    return { ok: false, error: "方块状态属性名不能为空。" };
  }
  if (entries.some(([, value]) => typeof value !== "string")) {
    return { ok: false, error: "方块状态的每个值都必须是字符串。" };
  }
  return {
    ok: true,
    value: Object.fromEntries(entries.map(([key, value]) => [key, value as string]))
  };
}

export function formatBlockStateInput(value: BlockStateProperties): string {
  return Object.keys(value).length ? JSON.stringify(value, null, 2) : "{}";
}

/** Accepts English/Chinese commas and newlines while preserving duplicates for validation. */
export function parseCopyPropertiesInput(raw: string): string[] {
  return raw
    .split(/[,，\r\n]/)
    .map((value) => value.trim())
    .filter(Boolean);
}

/** Mirrors Java id normalization and prevents a rename from becoming ambiguous. */
export function validateBlockTransformIdInput(
  raw: string,
  rule: BlockTransform,
  siblingIds: string[]
): string {
  const normalized = normalizeBlockTransformRuleId(raw);
  const formatProblem = blockTransformProblems({ ...rule, id: raw })
    .find((problem) => problem.code === "block_transform_invalid_rule_id");
  if (formatProblem) return "规则 ID 只能使用简单 ID 或 namespace:path，不能包含空格或非法符号；大写会自动转小写。";
  if (siblingIds.some((id) => normalizeBlockTransformRuleId(id) === normalized)) {
    return `规则 ID 与现有规则重复：${normalized}`;
  }
  return "";
}
