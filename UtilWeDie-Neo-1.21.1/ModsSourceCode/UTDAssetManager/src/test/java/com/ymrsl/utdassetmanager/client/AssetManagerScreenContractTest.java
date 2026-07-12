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

    @Test
    void projectDirectoryIsASeparateReadOnlyScope() throws Exception {
        Path sourcePath = Path.of(
                "src/main/java/com/ymrsl/utdassetmanager/client/AssetManagerScreen.java");
        String source = Files.readString(sourcePath);

        assertTrue(source.contains("private AssetScope scope = AssetScope.LOCAL"));
        assertTrue(source.contains("source = repository.allManifestDirectory()"));
        assertTrue(source.contains("repository.projectStatusFor(record)"));
        assertTrue(source.contains("repository.isSelectedIdentity(record)"));
        assertTrue(source.contains("String haystack = (displayName(record)"));
        assertTrue(source.contains("项目目录只读"));
        assertTrue(source.contains("if (scope != AssetScope.LOCAL)"));
        assertTrue(source.contains("componentSensitiveBase(record)"));
    }

    @Test
    void longVariantSearchAndFoodPreviewAreSupported() throws Exception {
        Path screenPath = Path.of(
                "src/main/java/com/ymrsl/utdassetmanager/client/AssetManagerScreen.java");
        String screen = Files.readString(screenPath);
        Path codecPath = Path.of(
                "src/main/java/com/ymrsl/utdassetmanager/client/AssetStackCodec.java");
        String codec = Files.readString(codecPath);

        assertTrue(screen.contains("search.setMaxLength(512)"));
        assertTrue(screen.contains("compactDiscriminator(record.variantDiscriminator)"));
        assertTrue(screen.contains("AssetStackCodec.supportsVariantPreview(record)"));
        assertTrue(codec.contains("firstpersonfoodeating:pack_food"));
        assertTrue(codec.contains("DataComponents.CUSTOM_DATA"));
        assertTrue(codec.contains("!\"{}\".equals(itemStackSnbt)"));
    }

    @Test
    void inspectorEditsPresentationWithoutMutatingItemStackComponents() throws Exception {
        Path screenPath = Path.of(
                "src/main/java/com/ymrsl/utdassetmanager/client/AssetManagerScreen.java");
        String screen = Files.readString(screenPath);
        Path tooltipPath = Path.of(
                "src/main/java/com/ymrsl/utdassetmanager/client/AssetTooltipEvents.java");
        String tooltip = Files.readString(tooltipPath);

        assertTrue(screen.contains("MultiLineEditBox"));
        assertTrue(screen.contains("编辑名称 / 介绍"));
        assertTrue(screen.contains("presentationRepository.upsert"));
        assertTrue(screen.contains("不会改写 ItemStack Components"));
        assertTrue(tooltip.contains("PresentationDraftRepository.get().resolveEnabled"));
        assertTrue(tooltip.contains("已进入项目管理"));
    }

    @Test
    void projectDirectorySupportsExplicitMultiSelectAndSelectAllForIconExport() throws Exception {
        Path sourcePath = Path.of(
                "src/main/java/com/ymrsl/utdassetmanager/client/AssetManagerScreen.java");
        String source = Files.readString(sourcePath);

        assertTrue(source.contains("exportSelectionKeys"));
        assertTrue(source.contains("toggleExportSelection"));
        assertTrue(source.contains("全选可见"));
        assertTrue(source.contains("清空导出选择"));
        assertTrue(source.contains("repository.allManifestDirectory()"));
        assertTrue(source.contains("repository.exportSnapshot(iconExportRecords"));
    }
}
