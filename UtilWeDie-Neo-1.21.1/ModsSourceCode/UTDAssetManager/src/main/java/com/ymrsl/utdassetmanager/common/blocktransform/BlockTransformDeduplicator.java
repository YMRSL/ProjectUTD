package com.ymrsl.utdassetmanager.common.blocktransform;

import java.util.LinkedHashMap;
import java.util.Map;

/** Small bounded same-tick outcome cache. Client and server use separate instances. */
final class BlockTransformDeduplicator {
    private final Map<Key, Entry> entries;

    BlockTransformDeduplicator(int maximumEntries) {
        if (maximumEntries < 1) throw new IllegalArgumentException("maximumEntries must be positive");
        entries = new LinkedHashMap<>(64, 0.75F, true) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<Key, Entry> eldest) {
                return size() > maximumEntries;
            }
        };
    }

    synchronized Outcome find(Key key, long gameTime) {
        Entry entry = entries.get(key);
        return entry != null && entry.gameTime() == gameTime ? entry.outcome() : null;
    }

    synchronized void remember(Key key, long gameTime, Outcome outcome) {
        entries.put(key, new Entry(gameTime, outcome));
    }

    record Key(String playerId, String dimension, long position, String ruleId) {
    }

    enum Outcome {
        SUCCESS,
        FAIL
    }

    private record Entry(long gameTime, Outcome outcome) {
    }
}
