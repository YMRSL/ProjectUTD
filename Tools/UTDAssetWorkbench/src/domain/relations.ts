import type { CanonicalItem, CanonicalLootPolicy, CanonicalRef } from "./schema";

export function hasVariantIdentity(item: CanonicalItem): boolean {
  return Boolean(
    item.variantDiscriminator
    || item.variantKey
    || !["item", "plain", "base"].includes(item.identityKind.toLocaleLowerCase())
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
