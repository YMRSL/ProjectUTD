package com.ymrsl.utdassetmanager.client;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

public final class CanonicalTagEncoder {
    private CanonicalTagEncoder() {
    }

    public static String encode(Tag tag) {
        if (tag == null) {
            return "null";
        }
        if (tag instanceof CompoundTag compound) {
            List<String> keys = new ArrayList<>(compound.getAllKeys());
            Collections.sort(keys);
            StringBuilder builder = new StringBuilder("C{");
            for (String key : keys) {
                builder.append(key.length()).append(':').append(key).append('=').append(encode(compound.get(key))).append(';');
            }
            return builder.append('}').toString();
        }
        if (tag instanceof ListTag list) {
            StringBuilder builder = new StringBuilder("L[");
            for (Tag entry : list) {
                builder.append(encode(entry)).append(';');
            }
            return builder.append(']').toString();
        }
        String value = tag.toString();
        return "T" + tag.getId() + ':' + value.length() + ':' + value;
    }
}
