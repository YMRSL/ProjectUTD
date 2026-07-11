package com.ymrsl.utdassetmanager.core;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

final class AssetFilterTest {
    @Test
    void filtersUseOrthogonalStatusDimensions() {
        AssetStatus status = new AssetStatus(
                true, false, 3, 0, true, 2, SyncState.STALE,
                "catalog", "deployed", true, List.of("hash_mismatch")
        );
        assertTrue(AssetFilter.HUMAN_SELECTED.matches(status));
        assertFalse(AssetFilter.CATALOGUED.matches(status));
        assertTrue(AssetFilter.RECIPE.matches(status));
        assertTrue(AssetFilter.LOOT.matches(status));
        assertTrue(AssetFilter.UNSYNCED.matches(status));
        assertTrue(AssetFilter.ISSUES.matches(status));
    }

    @Test
    void syncedCleanEntryIsNotUnsynced() {
        AssetStatus status = new AssetStatus(
                true, true, 0, 1, false, 0, SyncState.SYNCED,
                "same", "same", false, List.of()
        );
        assertFalse(AssetFilter.UNSYNCED.matches(status));
        assertFalse(AssetFilter.ISSUES.matches(status));
        assertTrue(AssetFilter.RECIPE.matches(status));
    }
}
