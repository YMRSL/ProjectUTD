package com.ymrsl.utdassetmanager.core;

import java.util.Locale;

public enum SyncState {
    LOCAL_ONLY,
    PENDING,
    SYNCED,
    STALE,
    ERROR;

    public static SyncState parse(String value) {
        if (value == null || value.isBlank()) {
            return LOCAL_ONLY;
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return ERROR;
        }
    }
}
