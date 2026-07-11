package com.ymrsl.utdassetmanager.core;

import java.util.List;

public record AssetStatus(
        boolean humanSelected,
        boolean catalogued,
        int recipeInputCount,
        int recipeOutputCount,
        boolean lootEnabled,
        int lootLevel,
        SyncState syncState,
        String catalogHash,
        String deployedHash,
        boolean stale,
        List<String> issues
) {
    public AssetStatus {
        recipeInputCount = Math.max(0, recipeInputCount);
        recipeOutputCount = Math.max(0, recipeOutputCount);
        lootLevel = Math.max(0, lootLevel);
        syncState = syncState == null ? SyncState.LOCAL_ONLY : syncState;
        catalogHash = catalogHash == null ? "" : catalogHash;
        deployedHash = deployedHash == null ? "" : deployedHash;
        stale = stale || (!catalogHash.isBlank() && !deployedHash.isBlank() && !catalogHash.equals(deployedHash));
        if (stale && syncState == SyncState.SYNCED) {
            syncState = SyncState.STALE;
        }
        issues = issues == null ? List.of() : List.copyOf(issues);
    }

    public boolean recipeInput() {
        return recipeInputCount > 0;
    }

    public boolean recipeOutput() {
        return recipeOutputCount > 0;
    }

    public boolean hasRecipe() {
        return recipeInput() || recipeOutput();
    }

    public boolean needsSync() {
        return syncState != SyncState.SYNCED;
    }
}
