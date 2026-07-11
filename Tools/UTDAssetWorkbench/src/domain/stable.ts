import type { JsonObject, JsonValue } from "./schema";

export function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}

export function asJsonObject(value: unknown): JsonObject {
  return isRecord(value) ? (value as JsonObject) : {};
}

export function stableStringify(value: unknown, space = 0): string {
  return JSON.stringify(sortValue(value), null, space);
}

function sortValue(value: unknown): unknown {
  if (Array.isArray(value)) return value.map(sortValue);
  if (!isRecord(value)) return value;
  return Object.fromEntries(
    Object.keys(value)
      .sort((a, b) => a.localeCompare(b, "en"))
      .map((key) => [key, sortValue(value[key])])
  );
}

export function fingerprint(value: unknown): string {
  const text = stableStringify(value);
  let hash = 0xcbf29ce484222325n;
  const prime = 0x100000001b3n;
  for (let index = 0; index < text.length; index += 1) {
    hash ^= BigInt(text.charCodeAt(index));
    hash = BigInt.asUintN(64, hash * prime);
  }
  return hash.toString(16).padStart(16, "0");
}

export function cloneJson<T extends JsonValue | JsonObject>(value: T): T {
  return JSON.parse(JSON.stringify(value)) as T;
}

export function numberOr(value: unknown, fallback: number): number {
  const parsed = typeof value === "number" ? value : Number(value);
  return Number.isFinite(parsed) ? parsed : fallback;
}

export function stringOr(value: unknown, fallback = ""): string {
  return typeof value === "string" ? value : fallback;
}

export function stringArray(value: unknown): string[] {
  return Array.isArray(value) ? value.filter((entry): entry is string => typeof entry === "string") : [];
}

export function registryIdFromIdentity(value: string): string {
  const brace = value.indexOf("{");
  const component = value.indexOf("[");
  const splitAt = [brace, component].filter((index) => index >= 0).sort((a, b) => a - b)[0];
  return splitAt === undefined ? value : value.slice(0, splitAt);
}

export function namespaceOf(registryId: string): string {
  return registryId.includes(":") ? registryId.split(":", 1)[0] : "unknown";
}

export function displayNameFromId(registryId: string): string {
  const path = registryId.includes(":") ? registryId.slice(registryId.indexOf(":") + 1) : registryId;
  return path.replace(/[\/_-]+/g, " ").replace(/\b\w/g, (letter) => letter.toUpperCase());
}
