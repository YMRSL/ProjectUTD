import { isRecord, stringOr } from "./stable";

export interface SnapshotEnvelope {
  items: unknown[];
  generatedAt?: string;
  projectId?: string;
}

export function parseJsonOrAssignment(text: string, assignment?: string): unknown {
  const trimmed = text.trim();
  if (trimmed.startsWith("{") || trimmed.startsWith("[")) return JSON.parse(trimmed);
  if (!assignment) throw new Error("Input is not JSON and no assignment marker was supplied.");
  return parseAssignmentPayload(text, assignment);
}

export function parseAssignmentPayload(text: string, assignment: string): unknown {
  const markerIndex = text.indexOf(assignment);
  if (markerIndex < 0) throw new Error(`Assignment marker not found: ${assignment}`);
  const start = findPayloadStart(text, markerIndex + assignment.length);
  const end = findBalancedEnd(text, start);
  return JSON.parse(text.slice(start, end + 1));
}

function findPayloadStart(text: string, from: number): number {
  for (let index = from; index < text.length; index += 1) {
    if (text[index] === "{" || text[index] === "[") return index;
  }
  throw new Error("Assignment payload does not contain a JSON object or array.");
}

function findBalancedEnd(text: string, start: number): number {
  const open = text[start];
  const close = open === "{" ? "}" : "]";
  let depth = 0;
  let inString = false;
  let escaped = false;
  for (let index = start; index < text.length; index += 1) {
    const char = text[index];
    if (inString) {
      if (escaped) escaped = false;
      else if (char === "\\") escaped = true;
      else if (char === '"') inString = false;
      continue;
    }
    if (char === '"') inString = true;
    else if (char === open) depth += 1;
    else if (char === close) {
      depth -= 1;
      if (depth === 0) return index;
    }
  }
  throw new Error("Assignment payload is not balanced.");
}

export function unwrapSnapshot(input: unknown): SnapshotEnvelope {
  if (Array.isArray(input)) return { items: input };
  if (!isRecord(input)) throw new Error("Whitelist snapshot must be an array or object.");
  const candidates = [input.items, input.whitelist, input.records, input.entries];
  const items = candidates.find(Array.isArray);
  if (!items) throw new Error("Whitelist snapshot has no items/whitelist/records/entries array.");
  return {
    items,
    generatedAt: stringOr(input.generatedAt ?? input.generated_at ?? input.exportedAt ?? input.exported_at) || undefined,
    projectId: stringOr(input.projectId ?? input.project_id ?? input.producer) || undefined
  };
}
