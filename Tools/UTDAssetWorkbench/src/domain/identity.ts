const DISCRIMINATORS: Array<{ key: string; pattern: RegExp }> = [
  { key: "GunId", pattern: /(GunId|gun_id|gunId)[\\"'\s:=\\]+([a-z0-9_.-]+:[a-z0-9_./-]+)/i },
  { key: "AmmoId", pattern: /(AmmoId|ammo_id|ammoId)[\\"'\s:=\\]+([a-z0-9_.-]+:[a-z0-9_./-]+)/i },
  { key: "AttachmentId", pattern: /(AttachmentId|attachment_id|attachmentId)[\\"'\s:=\\]+([a-z0-9_.-]+:[a-z0-9_./-]+)/i },
  { key: "food_id", pattern: /(food_id|foodId)[\\"'\s:=\\]+([a-z0-9_.-]+:[a-z0-9_./-]+)/i }
];

/** Mirrors the Mod contract: first match by fixed key priority, never a composite key. */
export function variantDiscriminator(value: unknown): string {
  const text = JSON.stringify(value).replaceAll("\\\\\"", "\"");
  for (const candidate of DISCRIMINATORS) {
    const match = candidate.pattern.exec(text);
    if (match) return `${candidate.key}=${match[2].toLocaleLowerCase()}`;
  }
  return "";
}

export function extractVariantTokens(value: unknown): Set<string> {
  const discriminator = variantDiscriminator(value);
  return discriminator ? new Set([discriminator]) : new Set();
}
