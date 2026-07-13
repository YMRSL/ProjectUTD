import { refreshProject } from "./mutations";
import type { CanonicalItem, CanonicalRef, WorkbenchProject } from "./schema";

export interface IdentityCleanupSummary {
  retiredRegistryIds: number;
  removedItems: number;
  removedRecipes: number;
  removedLootPolicies: number;
  remappedLogicalPolicies: number;
  mergedAliasItems: number;
  promotedVariantItems: number;
}

export interface IdentityCleanupResult {
  project: WorkbenchProject;
  summary: IdentityCleanupSummary;
  removedRecipeIds: string[];
  removedItemKeys: string[];
}

/**
 * Applies explicit retired-item decisions and merges logical gun IDs into the
 * actual TaCZ carrier identity (`tacz:modern_kinetic_gun + GunId`).
 */
export function cleanIdentityCatalog(
  project: WorkbenchProject,
  retiredRegistryIds: Iterable<string>
): IdentityCleanupResult {
  const next = structuredClone(project);
  const retired = new Set([...retiredRegistryIds].map((value) => value.trim()).filter(Boolean));
  const identityMap = buildRecipeIdentityMap(next);
  const touched = new Set<string>();
  let remappedLogicalPolicies = 0;

  for (const policy of next.lootPolicies) {
    const mapped = identityMap.get(policy.identityKey);
    if (!mapped) continue;
    const discriminator = mapped.variantDiscriminator ?? policy.variantDiscriminator;
    if (policy.registryId !== mapped.ref || policy.variantDiscriminator !== discriminator) {
      policy.registryId = mapped.ref;
      policy.variantDiscriminator = discriminator;
      remappedLogicalPolicies += 1;
    }
  }

  const removedRecipeIds = next.recipes
    .filter((recipe) => [...recipe.inputs, ...recipe.outputs]
      .some((ref) => ref.refKind === "item" && retired.has(ref.ref)))
    .map((recipe) => recipe.id);
  const removedRecipeIdSet = new Set(removedRecipeIds);
  next.recipes = next.recipes.filter((recipe) => !removedRecipeIdSet.has(recipe.id));

  const retiredItemKeys = new Set(next.items
    .filter((item) => retired.has(item.registryId))
    .map((item) => item.itemKey));
  const retiredLootIdentityKeys = new Set(next.lootPolicies
    .filter((policy) => retired.has(policy.registryId) || retired.has(policy.identityKey))
    .map((policy) => policy.identityKey));
  const beforeLoot = next.lootPolicies.length;
  next.lootPolicies = next.lootPolicies.filter((policy) =>
    !retired.has(policy.registryId) && !retired.has(policy.identityKey)
  );

  const aliasToTarget = new Map<string, CanonicalItem>();
  for (const policy of next.lootPolicies) {
    if (!policy.variantDiscriminator || policy.registryId === policy.identityKey) continue;
    const target = next.items.find((item) =>
      item.registryId === policy.registryId
      && item.variantDiscriminator === policy.variantDiscriminator
    );
    if (!target) continue;
    const alias = next.items.find((item) =>
      item.source === "loot_registry"
      && item.registryId === policy.identityKey
      && !item.variantDiscriminator
    );
    if (!alias || alias.itemKey === target.itemKey) continue;
    aliasToTarget.set(alias.itemKey, target);
    promoteTarget(alias, target);
    touched.add(target.itemKey);
  }

  const removedItemKeys = new Set([...retiredItemKeys, ...aliasToTarget.keys()]);
  next.items = next.items.filter((item) => !removedItemKeys.has(item.itemKey));
  migrateItemKeyReferences(next, aliasToTarget, removedItemKeys, retired);
  next.issues = next.issues.filter((issue) => {
    if (issue.entityType === "recipe" && removedRecipeIdSet.has(issue.entityId)) return false;
    if (issue.entityType === "item" && removedItemKeys.has(issue.entityId)) return false;
    if (issue.entityType === "loot" && retiredLootIdentityKeys.has(issue.entityId)) return false;
    return true;
  });

  const cleaned = refreshProject(next, touched);
  return {
    project: cleaned,
    summary: {
      retiredRegistryIds: retired.size,
      removedItems: removedItemKeys.size,
      removedRecipes: removedRecipeIds.length,
      removedLootPolicies: beforeLoot - next.lootPolicies.length,
      remappedLogicalPolicies,
      mergedAliasItems: aliasToTarget.size,
      promotedVariantItems: new Set([...aliasToTarget.values()].map((item) => item.itemKey)).size
    },
    removedRecipeIds,
    removedItemKeys: [...removedItemKeys]
  };
}

function buildRecipeIdentityMap(project: WorkbenchProject): Map<string, CanonicalRef> {
  const candidates = new Map<string, CanonicalRef>();
  const ambiguous = new Set<string>();
  for (const recipe of project.recipes) {
    for (const ref of [...recipe.inputs, ...recipe.outputs]) {
      if (ref.refKind !== "item" || !ref.identityKey || !ref.variantDiscriminator) continue;
      const keys = [ref.identityKey];
      if (ref.variantDiscriminator.startsWith("GunId=")) {
        keys.push(ref.variantDiscriminator.slice("GunId=".length));
      }
      for (const key of keys) {
        const existing = candidates.get(key);
        if (existing && (existing.ref !== ref.ref || existing.variantDiscriminator !== ref.variantDiscriminator)) {
          ambiguous.add(key);
        } else candidates.set(key, ref);
      }
    }
  }
  for (const key of ambiguous) candidates.delete(key);
  return candidates;
}

function promoteTarget(alias: CanonicalItem, target: CanonicalItem): void {
  target.managed = true;
  target.ownership = "utd";
  target.source = "loot_registry";
  target.humanSelected ||= alias.humanSelected;
  if (target.categoryKey === "uncategorized" && alias.categoryKey !== "uncategorized") {
    target.categoryKey = alias.categoryKey;
    target.categoryLabelZhCn = alias.categoryLabelZhCn;
    target.categoryLevel = alias.categoryLevel;
  }
}

function migrateItemKeyReferences(
  project: WorkbenchProject,
  aliasToTarget: Map<string, CanonicalItem>,
  removedItemKeys: Set<string>,
  retiredRegistryIds: Set<string>
): void {
  project.presentations = project.presentations.flatMap((entry) => {
    const target = aliasToTarget.get(entry.itemKey);
    if (target) return [{
      ...entry,
      itemKey: target.itemKey,
      registryId: target.registryId,
      variantDiscriminator: target.variantDiscriminator
    }];
    return removedItemKeys.has(entry.itemKey) ? [] : [entry];
  });
  project.itemProperties = project.itemProperties.flatMap((entry) => {
    const target = aliasToTarget.get(entry.itemKey);
    if (target) return [{
      ...entry,
      itemKey: target.itemKey,
      registryId: target.registryId,
      variantDiscriminator: target.variantDiscriminator
    }];
    return removedItemKeys.has(entry.itemKey) ? [] : [entry];
  });
  project.blockTransforms = project.blockTransforms.filter((entry) => {
    const identityKey = entry.catalyst.identityKey;
    if (!identityKey) return !retiredRegistryIds.has(entry.catalyst.ref);
    const target = aliasToTarget.get(identityKey);
    if (target) {
      entry.catalyst.ref = target.registryId;
      entry.catalyst.identityKey = target.itemKey;
      entry.catalyst.variantDiscriminator = target.variantDiscriminator;
      return true;
    }
    return !removedItemKeys.has(identityKey);
  });
}
