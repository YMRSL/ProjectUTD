import type {
  CanonicalItem,
  ItemPresentationOverride,
  PresentationApplyScope,
  WorkbenchProject
} from "./schema";

const FPE_CARRIER = "firstpersonfoodeating:pack_food";
const FPE_DISCRIMINATOR = "food_id=";

export function presentationForItem(
  project: WorkbenchProject,
  item: CanonicalItem
): ItemPresentationOverride | undefined {
  const identity = project.presentations.find((entry) => entry.applyScope === "identity"
    && (entry.itemKey === item.itemKey
      || (entry.registryId === item.registryId
        && entry.variantDiscriminator === item.variantDiscriminator)));
  if (identity) return identity;

  // Every FPE food shares pack_food, so a registry-wide fallback would rename
  // every food variant at once. Keep this safety rule consistent with the Mod.
  if (isFpePresentationVariant(item)) return undefined;
  return project.presentations.find((entry) => entry.applyScope === "registry"
    && entry.registryId === item.registryId);
}

export function constrainPresentationScope(
  item: Pick<CanonicalItem, "registryId" | "variantDiscriminator">,
  requested: PresentationApplyScope
): PresentationApplyScope {
  return isFpePresentationVariant(item) ? "identity" : requested;
}

export function isFpePresentationVariant(
  item: Pick<CanonicalItem, "registryId" | "variantDiscriminator">
): boolean {
  return fpeIdentity(item) !== null;
}

export function defaultPresentation(item: CanonicalItem): ItemPresentationOverride {
  const applyScope: PresentationApplyScope = item.variantDiscriminator ? "identity" : "registry";
  const keys = derivePresentationKeys(item, applyScope);
  return {
    itemKey: item.itemKey,
    registryId: item.registryId,
    variantDiscriminator: item.variantDiscriminator,
    applyScope,
    enabled: false,
    observedNameZhCn: item.clientNameZhCn,
    nameKey: keys.nameKey,
    descriptionKey: keys.descriptionKey,
    nameZhCn: item.clientNameZhCn,
    descriptionZhCn: "",
    baseCatalogHash: item.catalogHash,
    updatedAt: ""
  };
}

export function derivePresentationKeys(
  item: Pick<CanonicalItem, "registryId" | "variantDiscriminator" | "translationKey">,
  applyScope: PresentationApplyScope
): { nameKey: string; descriptionKey: string } {
  const identity = applyScope === "identity" ? fpeIdentity(item) : null;
  if (identity) {
    return {
      nameKey: `item.${identity.namespace}.${identity.path}`,
      descriptionKey: `tooltip.${identity.namespace}.${identity.path}.desc`
    };
  }
  const [namespace, path = "unknown"] = splitId(item.registryId);
  return {
    nameKey: item.translationKey || `item.${namespace}.${path.replaceAll("/", ".")}`,
    descriptionKey: `tooltip.${namespace}.${path.replaceAll("/", ".")}.desc`
  };
}

export function fpeIdentity(
  item: Pick<CanonicalItem, "registryId" | "variantDiscriminator">
): { namespace: string; path: string } | null {
  if (item.registryId !== FPE_CARRIER || !item.variantDiscriminator.startsWith(FPE_DISCRIMINATOR)) return null;
  const foodId = item.variantDiscriminator.slice(FPE_DISCRIMINATOR.length);
  if (!foodId.includes(":")) return null;
  const [namespace, path] = splitId(foodId);
  return namespace && path ? { namespace, path: path.replaceAll("/", ".") } : null;
}

function splitId(registryId: string): [string, string] {
  const separator = registryId.indexOf(":");
  if (separator < 0) return ["unknown", registryId];
  return [registryId.slice(0, separator), registryId.slice(separator + 1)];
}
