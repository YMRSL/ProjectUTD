import type { CanonicalItem, CanonicalLootPolicy, CanonicalRef } from "./schema";

export function hasVariantIdentity(item: CanonicalItem): boolean {
  const identityKind = item.identityKind.toLocaleLowerCase();
  // Runtime captures use a stable SHA asset_key/variant_key even for a plain
  // stack. An explicit plain/base kind is authoritative when no discriminator
  // exists; otherwise recipe refs would create a second registry-id row.
  if (!item.variantDiscriminator && ["plain", "base"].includes(identityKind)) return false;
  return Boolean(
    item.variantDiscriminator
    || item.variantKey
    || !["item", "plain", "base"].includes(identityKind)
  );
}

export function refMatchesItem(ref: CanonicalRef, item: CanonicalItem): boolean {
  if (ref.refKind !== "item" || ref.ref !== item.registryId) return false;
  if (item.variantDiscriminator) return ref.variantDiscriminator === item.variantDiscriminator;
  if (hasVariantIdentity(item)) return false;
  return !ref.variantDiscriminator;
}

export function lootMatchesItem(policy: CanonicalLootPolicy, item: CanonicalItem): boolean {
  if (policy.registryId !== item.registryId) return false;
  if (policy.identityKey === item.itemKey) return true;
  if (item.variantDiscriminator) return policy.variantDiscriminator === item.variantDiscriminator;
  if (hasVariantIdentity(item)) return false;
  return !policy.variantDiscriminator;
}
