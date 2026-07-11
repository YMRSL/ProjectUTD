import type { CanonicalItem } from "./schema";

export interface CatalogIdentityLabel {
  primary: string;
  context: string;
  exact: string;
}

/**
 * Puts the variant-specific value first so rows sharing one carrier item remain
 * distinguishable even when the catalog rail is narrow.
 */
export function catalogIdentityLabel(item: CanonicalItem): CatalogIdentityLabel | null {
  const exact = item.variantDiscriminator.trim();
  if (exact) {
    const separator = exact.indexOf("=");
    if (separator > 0 && separator < exact.length - 1) {
      const key = exact.slice(0, separator).trim();
      const value = exact.slice(separator + 1).trim();
      const namespaceSeparator = value.indexOf(":");
      if (namespaceSeparator > 0 && namespaceSeparator < value.length - 1) {
        return {
          primary: value.slice(namespaceSeparator + 1),
          context: `${key} · ${value.slice(0, namespaceSeparator)}`,
          exact
        };
      }
      return { primary: value, context: key, exact };
    }
    return { primary: exact, context: "VARIANT", exact };
  }

  if (item.itemKey !== item.registryId) {
    return { primary: item.itemKey, context: "ASSET KEY", exact: item.itemKey };
  }
  return null;
}

export function matchesCatalogQuery(item: CanonicalItem, query: string): boolean {
  const needle = query.trim().toLocaleLowerCase();
  if (!needle) return true;

  return [
    item.clientNameZhCn,
    item.registryId,
    item.translationKey,
    item.itemKey,
    item.variantDiscriminator,
    item.identityComponentsCanonical,
    item.componentsCanonical,
    item.componentsSnbt,
    item.variantKey ?? ""
  ].some((value) => value.toLocaleLowerCase().includes(needle));
}
