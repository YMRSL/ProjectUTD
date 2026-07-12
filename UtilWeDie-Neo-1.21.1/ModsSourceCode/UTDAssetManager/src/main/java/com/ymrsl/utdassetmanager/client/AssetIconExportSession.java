package com.ymrsl.utdassetmanager.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.ymrsl.utdassetmanager.model.AssetRecord;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;

/** Captures the same client-rendered ItemStack icons shown by the asset screen. */
final class AssetIconExportSession {
    private static final int CELL = 24;
    private static final int ICON = 16;
    private static final int MARGIN = 8;
    private static final int CAPTURE_BACKGROUND = 0xFF010203;

    private final List<Entry> entries = new ArrayList<>();
    private final Map<String, String> iconDataUrls = new LinkedHashMap<>();
    private int offset;

    AssetIconExportSession(List<AssetRecord> records) {
        for (AssetRecord record : records) {
            ItemStack stack = AssetStackCodec.restore(record);
            if (!stack.isEmpty()) entries.add(new Entry(record.assetKey, stack));
        }
    }

    int total() {
        return entries.size();
    }

    int captured() {
        return iconDataUrls.size();
    }

    boolean finished() {
        return offset >= entries.size();
    }

    Map<String, String> iconDataUrls() {
        return Map.copyOf(iconDataUrls);
    }

    void renderAndCapturePage(GuiGraphics graphics, int guiWidth, int guiHeight) throws IOException {
        if (finished()) return;
        int columns = Math.max(1, (guiWidth - MARGIN * 2) / CELL);
        int rows = Math.max(1, (guiHeight - MARGIN * 2) / CELL);
        int capacity = Math.max(1, columns * rows);
        int end = Math.min(entries.size(), offset + capacity);

        graphics.fill(0, 0, guiWidth, guiHeight, CAPTURE_BACKGROUND);
        List<Slot> slots = new ArrayList<>();
        for (int index = offset; index < end; index++) {
            int pageIndex = index - offset;
            int x = MARGIN + (pageIndex % columns) * CELL + (CELL - ICON) / 2;
            int y = MARGIN + (pageIndex / columns) * CELL + (CELL - ICON) / 2;
            Entry entry = entries.get(index);
            graphics.renderItem(entry.stack(), x, y);
            slots.add(new Slot(entry.assetKey(), x, y));
        }
        graphics.flush();

        Minecraft minecraft = Minecraft.getInstance();
        try (NativeImage frame = Screenshot.takeScreenshot(minecraft.getMainRenderTarget())) {
            double scaleX = frame.getWidth() / (double) Math.max(1, guiWidth);
            double scaleY = frame.getHeight() / (double) Math.max(1, guiHeight);
            int background = frame.getPixelRGBA(0, 0);
            for (Slot slot : slots) {
                int sourceX = clamp((int) Math.round(slot.x() * scaleX), 0, frame.getWidth() - 1);
                int sourceY = clamp((int) Math.round(slot.y() * scaleY), 0, frame.getHeight() - 1);
                int iconWidth = Math.max(1, Math.min(frame.getWidth() - sourceX, (int) Math.round(ICON * scaleX)));
                int iconHeight = Math.max(1, Math.min(frame.getHeight() - sourceY, (int) Math.round(ICON * scaleY)));
                try (NativeImage icon = new NativeImage(iconWidth, iconHeight, true)) {
                    for (int y = 0; y < iconHeight; y++) {
                        for (int x = 0; x < iconWidth; x++) {
                            int pixel = frame.getPixelRGBA(sourceX + x, sourceY + y);
                            icon.setPixelRGBA(x, y, pixel == background ? 0 : pixel);
                        }
                    }
                    String dataUrl = "data:image/png;base64,"
                            + Base64.getEncoder().encodeToString(icon.asByteArray());
                    iconDataUrls.put(slot.assetKey(), dataUrl);
                }
            }
        }
        offset = end;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private record Entry(String assetKey, ItemStack stack) {
    }

    private record Slot(String assetKey, int x, int y) {
    }
}
