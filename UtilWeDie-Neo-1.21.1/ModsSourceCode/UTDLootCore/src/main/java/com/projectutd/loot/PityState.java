package com.projectutd.loot;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

final class PityState {
    static final String CIVILIAN_T4 = "utdCivilianT4Miss";
    static final String MEDICAL_T4 = "utdMedicalT4Miss";
    static final String MILITARY_T4 = "utdMilitaryT4Miss";
    static final String MILITARY_T5 = "utdMilitaryT5Miss";
    private static final String[] ALL_KEYS = {CIVILIAN_T4, MEDICAL_T4, MILITARY_T4, MILITARY_T5};

    private PityState() {}

    static String tier4Key(String channel) {
        return switch (channel == null ? "" : channel) {
            case "civilian" -> CIVILIAN_T4;
            case "medical" -> MEDICAL_T4;
            case "military" -> MILITARY_T4;
            default -> null;
        };
    }

    static String tier5Key(String channel) {
        return "military".equals(channel) ? MILITARY_T5 : null;
    }

    static int get(ServerPlayer player, String key) {
        return key == null ? 0 : Math.max(0, player.getPersistentData().getInt(key));
    }

    static void set(ServerPlayer player, String key, int value) {
        if (key != null) {
            player.getPersistentData().putInt(key, Math.max(0, value));
        }
    }

    static void reset(ServerPlayer player) {
        CompoundTag data = player.getPersistentData();
        for (String key : ALL_KEYS) {
            data.remove(key);
        }
    }

    static void copy(ServerPlayer source, ServerPlayer target) {
        CompoundTag sourceData = source.getPersistentData();
        CompoundTag targetData = target.getPersistentData();
        for (String key : ALL_KEYS) {
            if (sourceData.contains(key)) {
                targetData.putInt(key, sourceData.getInt(key));
            }
        }
    }

    static String summary(ServerPlayer player) {
        return "civilian T4=" + get(player, CIVILIAN_T4)
            + ", medical T4=" + get(player, MEDICAL_T4)
            + ", military T4=" + get(player, MILITARY_T4)
            + ", military T5=" + get(player, MILITARY_T5);
    }
}
