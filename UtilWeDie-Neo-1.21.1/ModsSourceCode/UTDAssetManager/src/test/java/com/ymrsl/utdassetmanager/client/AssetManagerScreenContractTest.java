package com.ymrsl.utdassetmanager.client;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class AssetManagerScreenContractTest {
    @Test
    void ownsTheBackgroundRendererSoVanillaBlurCannotCoverThePanels() throws Exception {
        Path sourcePath = Path.of(
                "src/main/java/com/ymrsl/utdassetmanager/client/AssetManagerScreen.java");
        String source = Files.readString(sourcePath);

        assertTrue(source.contains(
                "public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick)"));
        assertFalse(source.contains("super.renderBackground("));
    }

    @Test
    void containerScreensHaveAnExplicitOpenKeyPath() throws Exception {
        Path sourcePath = Path.of(
                "src/main/java/com/ymrsl/utdassetmanager/client/ClientRuntimeEvents.java");
        String source = Files.readString(sourcePath);

        assertTrue(source.contains("ScreenEvent.KeyPressed.Pre"));
        assertTrue(source.contains("instanceof AbstractContainerScreen<?>"));
        assertTrue(source.contains("OPEN_MANAGER.matches(event.getKeyCode(), event.getScanCode())"));
        assertTrue(source.contains("event.setCanceled(true)"));
    }

    @Test
    void assetStateBadgesAreReadableChineseLabels() throws Exception {
        Path sourcePath = Path.of(
                "src/main/java/com/ymrsl/utdassetmanager/client/AssetManagerScreen.java");
        String source = Files.readString(sourcePath);

        assertTrue(source.contains("badge(graphics, \"标\""));
        assertTrue(source.contains("badge(graphics, \"管\""));
        assertTrue(source.contains("badge(graphics, \"材\""));
        assertTrue(source.contains("badge(graphics, \"产\""));
        assertTrue(source.contains("badge(graphics, \"掉\""));
        assertTrue(source.contains("? \"同\" : \"!\""));
    }
}
