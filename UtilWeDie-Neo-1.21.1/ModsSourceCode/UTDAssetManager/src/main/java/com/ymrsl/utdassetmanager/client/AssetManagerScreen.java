package com.ymrsl.utdassetmanager.client;

import com.ymrsl.utdassetmanager.core.AssetFilter;
import com.ymrsl.utdassetmanager.core.AssetStatus;
import com.ymrsl.utdassetmanager.core.SyncState;
import com.ymrsl.utdassetmanager.model.AssetRecord;
import com.ymrsl.utdassetmanager.model.PresentationApplyScope;
import com.ymrsl.utdassetmanager.model.PresentationDraft;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.MultiLineEditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

public final class AssetManagerScreen extends Screen {
    private static final int BG = 0xFF0B0D0F;
    private static final int PANEL = 0xFF121518;
    private static final int PANEL_ALT = 0xFF171A1D;
    private static final int LINE = 0xFF363126;
    private static final int TEXT = 0xFFD7D0C1;
    private static final int MUTED = 0xFF817A6D;
    private static final int AMBER = 0xFFD69A34;
    private static final int AMBER_DARK = 0xFF503A1A;
    private static final int ERROR = 0xFFCA5D4A;
    private static final int OK = 0xFF8FA37B;
    private static final int ROW_HEIGHT = 30;

    private final Screen previous;
    private final AssetRepository repository = AssetRepository.get();
    private final PresentationDraftRepository presentationRepository = PresentationDraftRepository.get();
    private final List<AssetRecord> visible = new ArrayList<>();
    private final Map<String, ItemStack> iconStacks = new LinkedHashMap<>();
    private final Map<String, String> localizedBaseNames = new LinkedHashMap<>();
    private List<PresentationDraft> presentationDrafts = List.of();
    private EditBox search;
    private EditBox presentationName;
    private MultiLineEditBox presentationDescription;
    private AssetFilter filter = AssetFilter.ALL;
    private AssetScope scope = AssetScope.LOCAL;
    private String selectedKey = "";
    private int scrollRows;
    private int selectedCount;
    private int manifestCount;
    private long lastCandidateRefresh;
    private long lastManifestRevision = Long.MIN_VALUE;
    private long lastPresentationRevision = Long.MIN_VALUE;
    private boolean compactHeight;
    private boolean presentationEditing;
    private AssetIconExportSession iconExportSession;
    private PresentationDraft presentationEditingDraft;
    private String notice = "鼠标放在列表上滚动；候选来自人工白名单、背包与当前容器。";

    private int frameX;
    private int frameY;
    private int frameW;
    private int frameH;
    private int bodyY;
    private int leftW;
    private int rightW;
    private int centerX;
    private int centerW;
    private int rightX;
    private Rect toggleButton = Rect.EMPTY;
    private Rect exportButton = Rect.EMPTY;
    private Rect reloadButton = Rect.EMPTY;
    private Rect batchMarkButton = Rect.EMPTY;
    private Rect batchUnmarkButton = Rect.EMPTY;
    private Rect localScopeButton = Rect.EMPTY;
    private Rect projectScopeButton = Rect.EMPTY;
    private Rect presentationActionButton = Rect.EMPTY;
    private Rect presentationScopeButton = Rect.EMPTY;

    public AssetManagerScreen(Screen previous) {
        super(Component.translatable("screen.utd_asset_manager.title"));
        this.previous = previous;
    }

    @Override
    protected void init() {
        super.init();
        calculateLayout();
        search = new EditBox(font, frameX + 12, bodyY + 14, leftW - 24, 18,
                Component.translatable("screen.utd_asset_manager.search"));
        search.setHint(Component.literal("搜索名称 / ID / variant"));
        search.setMaxLength(512);
        search.setResponder(ignored -> refreshRows());
        addRenderableWidget(search);
        presentationName = new EditBox(font, rightX + 12, bodyY + 83, rightW - 24, 18,
                Component.literal("游戏内中文名"));
        presentationName.setHint(Component.literal("留空则沿用原名"));
        presentationName.setMaxLength(256);
        presentationDescription = new MultiLineEditBox(
                font,
                rightX + 12,
                bodyY + 119,
                rightW - 24,
                58,
                Component.literal("每行一条物品介绍"),
                Component.literal("物品介绍"));
        presentationDescription.setCharacterLimit(2048);
        setPresentationEditorsVisible(false);
        addRenderableWidget(presentationName);
        addRenderableWidget(presentationDescription);
        repository.forceReloadManifest();
        refreshRows();
    }

    private void calculateLayout() {
        frameX = 4;
        frameY = 4;
        frameW = Math.max(1, width - 8);
        frameH = Math.max(1, height - 8);
        compactHeight = frameH < 300;
        bodyY = frameY + 42;
        leftW = Mth.clamp(frameW / 5, 82, 182);
        rightW = Mth.clamp(frameW / 3, 126, 286);
        centerX = frameX + leftW + 1;
        rightX = frameX + frameW - rightW;
        centerW = Math.max(1, rightX - centerX - 1);
        int scopeAvailable = Math.max(64, centerW - 16);
        int localScopeWidth = Math.min(42, Math.max(28, (scopeAvailable - 4) * 2 / 5));
        int projectScopeWidth = Math.min(62, Math.max(32, scopeAvailable - localScopeWidth - 4));
        localScopeButton = new Rect(centerX + 8, bodyY + 5, localScopeWidth, 18);
        projectScopeButton = new Rect(
                localScopeButton.x + localScopeButton.w + 4, bodyY + 5, projectScopeWidth, 18);
        int actionY = frameY + frameH - 34;
        int inner = rightW - 24;
        int small = Math.max(24, (inner - 8) / 3);
        toggleButton = new Rect(rightX + 12, actionY, small, 20);
        exportButton = new Rect(toggleButton.x + small + 4, actionY, small, 20);
        reloadButton = new Rect(exportButton.x + small + 4, actionY, Math.max(24, inner - small * 2 - 8), 20);
        presentationActionButton = new Rect(rightX + 12, actionY - 24, inner, 18);
        presentationScopeButton = new Rect(rightX + 12, bodyY + 184, inner, 18);
        int filterStep = compactHeight ? 15 : 20;
        int filterEnd = bodyY + (compactHeight ? 48 : 56)
                + (AssetFilter.values().length - 1) * filterStep + 14;
        int batchY = compactHeight
                ? Math.min(frameY + frameH - 38, filterEnd + 1)
                : frameY + frameH - 74;
        batchMarkButton = new Rect(frameX + 8, batchY, leftW - 16, 18);
        batchUnmarkButton = new Rect(frameX + 8, batchY + 20, leftW - 16, 18);
    }

    private void refreshRows() {
        long manifestRevision = repository.manifestRevision();
        if (manifestRevision != lastManifestRevision) {
            iconStacks.clear();
            localizedBaseNames.clear();
        }
        presentationDrafts = presentationRepository.all();
        lastPresentationRevision = presentationRepository.revision();
        List<AssetRecord> selectedRecords = repository.allSelected();
        selectedCount = selectedRecords.size();
        manifestCount = repository.manifestDirectorySize();
        List<AssetRecord> source;
        if (scope == AssetScope.PROJECT) {
            source = repository.allManifestDirectory();
        } else {
            Map<String, AssetRecord> candidates = new LinkedHashMap<>();
            for (AssetRecord record : selectedRecords) {
                candidates.put(record.assetKey, record);
            }
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player != null) {
                minecraft.player.getInventory().items.forEach(stack -> addCandidate(candidates, stack));
                minecraft.player.getInventory().armor.forEach(stack -> addCandidate(candidates, stack));
                minecraft.player.getInventory().offhand.forEach(stack -> addCandidate(candidates, stack));
            }
            if (previous instanceof AbstractContainerScreen<?> containerScreen) {
                containerScreen.getMenu().slots.forEach(slot -> addCandidate(candidates, slot.getItem()));
                addCandidate(candidates, containerScreen.getMenu().getCarried());
            }
            source = new ArrayList<>(candidates.values());
        }
        String query = search == null ? "" : search.getValue().trim().toLowerCase(Locale.ROOT);
        visible.clear();
        for (AssetRecord record : source) {
            AssetStatus status = statusForRecord(record);
            if (!filter.matches(status)) {
                continue;
            }
            String haystack = (displayName(record) + " " + safe(record.displayNameZhCn) + " "
                    + safe(record.registryId) + " " + safe(record.assetKey) + " "
                    + safe(record.variantKind) + " " + safe(record.variantDiscriminator) + " "
                    + presentationSearchText(record))
                    .toLowerCase(Locale.ROOT);
            if (query.isEmpty() || haystack.contains(query)) {
                visible.add(record);
            }
        }
        int max = Math.max(0, visible.size() - visibleRowCount());
        scrollRows = Mth.clamp(scrollRows, 0, max);
        if (visible.stream().noneMatch(entry -> entry.assetKey.equals(selectedKey))) {
            selectedKey = visible.isEmpty() ? "" : visible.get(0).assetKey;
        }
        lastCandidateRefresh = System.currentTimeMillis();
        lastManifestRevision = manifestRevision;
    }

    @Override
    public void tick() {
        super.tick();
        long now = System.currentTimeMillis();
        if (now - lastCandidateRefresh < 1_000L) {
            return;
        }
        if (scope == AssetScope.LOCAL
                || repository.manifestRevision() != lastManifestRevision
                || presentationRepository.revision() != lastPresentationRevision) {
            refreshRows();
        } else {
            lastCandidateRefresh = now;
        }
    }

    private void addCandidate(Map<String, AssetRecord> candidates, ItemStack stack) {
        AssetRecord candidate = AssetStackCodec.capture(stack);
        if (candidate != null) {
            candidates.putIfAbsent(candidate.assetKey, candidate);
        }
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // This screen paints an opaque full-screen background itself. Screen.render()
        // must not apply the vanilla pause-menu blur over the custom panels before
        // rendering widgets such as the search box.
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (iconExportSession != null) {
            renderIconExport(graphics);
            return;
        }
        graphics.fill(0, 0, width, height, BG);
        graphics.fill(frameX, frameY, frameX + frameW, frameY + frameH, PANEL);
        graphics.renderOutline(frameX, frameY, frameW, frameH, LINE);
        renderHeader(graphics);
        renderLeftRail(graphics, mouseX, mouseY);
        renderList(graphics, mouseX, mouseY);
        renderInspector(graphics, mouseX, mouseY);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private void renderHeader(GuiGraphics graphics) {
        graphics.fill(frameX, frameY, frameX + frameW, bodyY, 0xFF0F1214);
        graphics.fill(frameX, frameY, frameX + 4, bodyY, AMBER);
        graphics.drawString(font, "UTD // ASSET ARCHIVE", frameX + 14, frameY + 9, AMBER, false);
        graphics.drawString(font, "本机白名单 · 项目目录 · 精确 Components Variant", frameX + 14, frameY + 24, MUTED, false);
        if (frameW >= 560) {
            String count = selectedCount + " 本机标注  /  " + manifestCount + " 项目记录";
            graphics.drawString(font, count, frameX + frameW - 14 - font.width(count), frameY + 14, TEXT, false);
        }
        graphics.fill(frameX, bodyY - 1, frameX + frameW, bodyY, LINE);
    }

    private void renderLeftRail(GuiGraphics graphics, int mouseX, int mouseY) {
        int bottom = frameY + frameH;
        graphics.fill(frameX, bodyY, centerX - 1, bottom, PANEL_ALT);
        graphics.fill(centerX - 1, bodyY, centerX, bottom, LINE);
        graphics.drawString(font, "检索", frameX + 12, bodyY + 3, MUTED, false);
        graphics.drawString(font, "状态分组", frameX + 12, bodyY + 42, MUTED, false);
        int y = bodyY + (compactHeight ? 48 : 56);
        int filterStep = compactHeight ? 15 : 20;
        for (AssetFilter candidate : AssetFilter.values()) {
            boolean active = candidate == filter;
            boolean hovered = inside(mouseX, mouseY, frameX + 8, y - 3, leftW - 16, 18);
            if (active || hovered) {
                graphics.fill(frameX + 8, y - 3, frameX + leftW - 8, y + 13, active ? AMBER_DARK : 0xFF202226);
            }
            if (active) {
                graphics.fill(frameX + 8, y - 3, frameX + 10, y + 13, AMBER);
            }
            graphics.drawString(font, candidate.label(), frameX + 16, y, active ? AMBER : TEXT, false);
            y += filterStep;
        }
        boolean localWritable = scope == AssetScope.LOCAL;
        drawAction(graphics, batchMarkButton, localWritable ? "标注可见" : "项目目录只读",
                mouseX, mouseY, true, localWritable);
        drawAction(graphics, batchUnmarkButton, localWritable ? "取消可见" : "不可批量修改",
                mouseX, mouseY, false, localWritable);
        if (!compactHeight) {
            int noteY = bottom - 28;
            graphics.drawString(font, "O 打开 · /utdasset", frameX + 12, noteY, MUTED, false);
        }
    }

    private void renderList(GuiGraphics graphics, int mouseX, int mouseY) {
        int bottom = frameY + frameH;
        graphics.fill(centerX, bodyY, rightX - 1, bottom, PANEL);
        graphics.fill(rightX - 1, bodyY, rightX, bottom, LINE);
        drawScopeTab(graphics, localScopeButton, "本机", scope == AssetScope.LOCAL, mouseX, mouseY);
        drawScopeTab(graphics, projectScopeButton, "项目目录", scope == AssetScope.PROJECT, mouseX, mouseY);
        int end = Math.min(visible.size(), scrollRows + visibleRowCount());
        String range = visible.isEmpty() ? "0 / 0" : (scrollRows + 1) + "–" + end + " / " + visible.size();
        int rangeX = rightX - 12 - font.width(range);
        boolean rangeInHeader = rangeX > projectScopeButton.x + projectScopeButton.w + 6;
        if (rangeInHeader) {
            graphics.drawString(font, range, rangeX, bodyY + 12, MUTED, false);
        }
        int listTop = bodyY + 32;
        int listBottom = bottom - 31;
        graphics.enableScissor(centerX + 1, listTop, rightX - 1, listBottom);
        int y = listTop;
        for (int index = scrollRows; index < end; index++) {
            AssetRecord record = visible.get(index);
            AssetStatus status = statusForRecord(record);
            boolean selected = record.assetKey.equals(selectedKey);
            boolean hovered = inside(mouseX, mouseY, centerX + 4, y, centerW - 8, ROW_HEIGHT - 2);
            if (selected || hovered) {
                graphics.fill(centerX + 4, y, rightX - 5, y + ROW_HEIGHT - 2, selected ? AMBER_DARK : PANEL_ALT);
            }
            if (selected) {
                graphics.fill(centerX + 4, y, centerX + 7, y + ROW_HEIGHT - 2, AMBER);
            }
            renderRowIcon(graphics, record, centerX + 12, y + 6);
            int textX = centerX + 35;
            int badgeWidth = Math.min(120, Math.max(84, centerW / 3));
            int maxText = Math.max(24, centerW - badgeWidth - 48);
            String name = ellipsize(displayName(record), maxText);
            String identity = AssetStackCodec.supportsVariantPreview(record)
                    ? compactDiscriminator(record.variantDiscriminator)
                    : safe(record.registryId);
            if (!AssetStackCodec.supportsVariantPreview(record)
                    && !safe(record.variantDiscriminator).isBlank()) {
                identity += " · " + compactDiscriminator(record.variantDiscriminator);
            }
            String id = ellipsize(identity, maxText);
            graphics.drawString(font, name.isBlank() ? "<unnamed>" : name, textX, y + 5, TEXT, false);
            graphics.drawString(font, id, textX, y + 17, MUTED, false);
            renderBadges(graphics, status, rightX - badgeWidth - 8, y + 8);
            y += ROW_HEIGHT;
        }
        graphics.disableScissor();
        if (visible.isEmpty()) {
            String emptyMessage = scope == AssetScope.PROJECT && manifestCount == 0
                    ? (repository.manifestErrorMessage().isBlank() ? "项目目录为空" : "项目目录加载失败，仍保留上次有效数据")
                    : "当前筛选没有记录";
            graphics.drawCenteredString(font, emptyMessage, centerX + centerW / 2, listTop + 30, MUTED);
        }
        renderScrollbar(graphics, listTop, listBottom);
        graphics.fill(centerX, bottom - 30, rightX - 1, bottom, 0xFF0F1214);
        String footer = rangeInHeader ? notice : range + " · " + notice;
        graphics.drawString(font, ellipsize(footer, centerW - 24), centerX + 12, bottom - 20, MUTED, false);
    }

    private void drawScopeTab(
            GuiGraphics graphics,
            Rect rect,
            String label,
            boolean active,
            int mouseX,
            int mouseY
    ) {
        boolean hovered = rect.contains(mouseX, mouseY);
        if (active || hovered) {
            graphics.fill(rect.x, rect.y, rect.x + rect.w, rect.y + rect.h,
                    active ? AMBER_DARK : 0xFF202226);
        }
        graphics.renderOutline(rect.x, rect.y, rect.w, rect.h, active ? AMBER : LINE);
        graphics.drawCenteredString(font, label, rect.x + rect.w / 2, rect.y + 5, active ? AMBER : TEXT);
    }

    private void renderRowIcon(GuiGraphics graphics, AssetRecord record, int x, int y) {
        if (scope == AssetScope.PROJECT
                && (!safe(record.variantDiscriminator).isBlank() || componentSensitiveBase(record))
                && !AssetStackCodec.supportsVariantPreview(record)) {
            graphics.renderOutline(x, y, 16, 16, LINE);
            graphics.drawCenteredString(font, "变", x + 8, y + 4, MUTED);
            return;
        }
        ItemStack stack = iconStacks.computeIfAbsent(
                scope.name() + "\n" + record.assetKey,
                ignored -> AssetStackCodec.restore(record));
        if (!stack.isEmpty()) {
            graphics.renderItem(stack, x, y);
            return;
        }
        if (scope == AssetScope.PROJECT) {
            graphics.renderOutline(x, y, 16, 16, LINE);
            graphics.drawCenteredString(font, "档", x + 8, y + 4, MUTED);
        }
    }

    private void renderBadges(GuiGraphics graphics, AssetStatus status, int x, int y) {
        x = badge(graphics, "标", status.humanSelected(), x, y);
        x = badge(graphics, "管", status.catalogued(), x, y);
        x = badge(graphics, "材", status.recipeInput(), x, y);
        x = badge(graphics, "产", status.recipeOutput(), x, y);
        x = badge(graphics, "掉", status.lootEnabled(), x, y);
        badge(graphics, status.issues().isEmpty() ? "同" : "!", !status.needsSync() && status.issues().isEmpty(), x, y);
    }

    private void renderScrollbar(GuiGraphics graphics, int listTop, int listBottom) {
        int rows = visibleRowCount();
        int maxScroll = Math.max(0, visible.size() - rows);
        if (maxScroll == 0) {
            return;
        }
        int trackX = rightX - 7;
        int trackHeight = Math.max(1, listBottom - listTop);
        int thumbHeight = Math.max(12, trackHeight * rows / visible.size());
        int thumbTravel = Math.max(0, trackHeight - thumbHeight);
        int thumbY = listTop + thumbTravel * scrollRows / maxScroll;
        graphics.fill(trackX, listTop, trackX + 3, listBottom, 0xFF292C2F);
        graphics.fill(trackX, thumbY, trackX + 3, thumbY + thumbHeight, AMBER);
    }

    private int badge(GuiGraphics graphics, String label, boolean active, int x, int y) {
        int color = active ? AMBER : 0xFF3A3C3D;
        graphics.renderOutline(x, y, 12, 12, color);
        graphics.drawCenteredString(font, label, x + 6, y + 2, active ? AMBER : MUTED);
        return x + 15;
    }

    private void renderInspector(GuiGraphics graphics, int mouseX, int mouseY) {
        int bottom = frameY + frameH;
        graphics.fill(rightX, bodyY, frameX + frameW, bottom, PANEL_ALT);
        AssetRecord record = selectedRecord();
        graphics.drawString(font, "检查器", rightX + 12, bodyY + 12, MUTED, false);
        if (record == null) {
            setPresentationEditorsVisible(false);
            graphics.drawString(font, "请选择一条记录", rightX + 12, bodyY + 34, TEXT, false);
            return;
        }
        AssetStatus status = statusForRecord(record);
        int x = rightX + 12;
        int max = rightW - 24;
        int y = bodyY + 31;
        graphics.drawString(font, ellipsize(displayName(record), max), x, y, TEXT, false);
        y += 14;
        graphics.drawString(font, ellipsize(record.registryId, max), x, y, AMBER, false);
        y += 17;
        if (presentationEditing && record.assetKey.equals(selectedKey)) {
            renderPresentationEditor(graphics, record, mouseX, mouseY, x, max);
            return;
        }
        setPresentationEditorsVisible(false);
        if (!compactHeight) {
            y = detailIfFits(graphics, "来源",
                    scope == AssetScope.PROJECT ? "项目目录（只读）" : "本机 / 身边物品", x, y, max);
            y = detailIfFits(graphics, "翻译键", record.translationKey, x, y, max);
        }
        String variant = safe(record.variantDiscriminator).isBlank()
                ? record.variantKind + " / " + shortKey(record.variantKey)
                : compactDiscriminator(record.variantDiscriminator);
        y = detailIfFits(graphics, "变体", variant, x, y, max);
        boolean localSelected = scope == AssetScope.PROJECT
                ? repository.isSelectedIdentity(record)
                : repository.isSelected(record.assetKey);
        boolean historicalSelected = repository.manifestHumanSelected(record);
        y = statusLineIfFits(graphics, "本机白名单", localSelected, "", x, y);
        y = statusLineIfFits(graphics, "项目历史标注", historicalSelected, "", x, y);
        y = statusLineIfFits(graphics, "进入项目管理", status.catalogued(), "", x, y);
        y = statusLineIfFits(graphics, "配方材料", status.recipeInput(), Integer.toString(status.recipeInputCount()), x, y);
        y = statusLineIfFits(graphics, "配方产物", status.recipeOutput(), Integer.toString(status.recipeOutputCount()), x, y);
        y = statusLineIfFits(graphics, "Loot 掉落", status.lootEnabled(), "L" + status.lootLevel(), x, y);
        boolean syncOk = status.syncState() == SyncState.SYNCED && !status.stale();
        y = statusLineIfFits(graphics, "同步状态", syncOk, status.syncState().name().toLowerCase(), x, y);
        y = statusLineIfFits(graphics, "异常", status.issues().isEmpty(), Integer.toString(status.issues().size()), x, y);
        if (status.needsSync()) {
            int pulse = 100 + (int) (90 * (0.5 + 0.5 * Math.sin(System.currentTimeMillis() / 220.0)));
            graphics.fill(rightX + rightW - 18, bodyY + 13, rightX + rightW - 13, bodyY + 18,
                    (pulse << 24) | (AMBER & 0x00FFFFFF));
        }
        if (!compactHeight && y < presentationActionButton.y - 48) {
            graphics.drawString(font, "COMPONENT PATCH", x, y + 3, MUTED, false);
            graphics.drawString(font, ellipsize(record.componentsSnbt, max), x, y + 17, 0xFF9A9181, false);
            graphics.drawString(font, "catalog " + shortKey(status.catalogHash()), x, y + 31, MUTED, false);
            graphics.drawString(font, "deploy  " + shortKey(status.deployedHash()), x, y + 43, MUTED, false);
        }
        boolean localWritable = scope == AssetScope.LOCAL;
        String toggleLabel = localWritable
                ? (localSelected ? (toggleButton.w < 50 ? "取消" : "取消标注") : "标注")
                : "只读";
        drawAction(graphics, toggleButton, toggleLabel, mouseX, mouseY, true, localWritable);
        drawAction(graphics, exportButton, scope == AssetScope.PROJECT ? "导出本机+图标" : "导出+图标",
                mouseX, mouseY, false);
        drawAction(graphics, reloadButton, "重载", mouseX, mouseY, false);
        drawAction(graphics, presentationActionButton, "编辑名称 / 介绍", mouseX, mouseY, false,
                !compactHeight && presentationRepository.isWritable());
    }

    private void renderPresentationEditor(
            GuiGraphics graphics,
            AssetRecord record,
            int mouseX,
        int mouseY,
        int x,
        int max
    ) {
        setPresentationEditorsVisible(true);
        graphics.drawString(font, "采集：" + ellipsize(observedDisplayName(record), Math.max(20, max - 30)),
                x, bodyY + 61, MUTED, false);
        graphics.drawString(font, "游戏内中文名", x, bodyY + 72, MUTED, false);
        graphics.drawString(font, "物品介绍（每行一条）", x, bodyY + 108, MUTED, false);
        String scopeLabel = presentationEditingDraft != null
                && presentationEditingDraft.applyScope == PresentationApplyScope.REGISTRY
                ? "范围：整个物品"
                : "范围：当前变体";
        drawAction(graphics, presentationScopeButton, scopeLabel, mouseX, mouseY, false);
        Rect save = presentationSaveButton();
        Rect cancel = presentationCancelButton();
        drawAction(graphics, save, save.w < 72 ? "保存" : "保存显示草稿", mouseX, mouseY, true);
        drawAction(graphics, cancel, "取消", mouseX, mouseY, false);
        drawAction(graphics, toggleButton, scope == AssetScope.LOCAL ? "标注暂锁" : "目录只读",
                mouseX, mouseY, false, false);
        drawAction(graphics, exportButton, "导出暂锁", mouseX, mouseY, false, false);
        drawAction(graphics, reloadButton, "重载暂锁", mouseX, mouseY, false, false);
    }

    private int detail(GuiGraphics graphics, String label, String value, int x, int y, int max) {
        graphics.drawString(font, label, x, y, MUTED, false);
        graphics.drawString(font, ellipsize(safe(value), Math.max(20, max - 76)), x + 76, y, TEXT, false);
        return y + 14;
    }

    private int detailIfFits(GuiGraphics graphics, String label, String value, int x, int y, int max) {
        return y + 14 > presentationActionButton.y - 3 ? y : detail(graphics, label, value, x, y, max);
    }

    private int statusLine(GuiGraphics graphics, String label, boolean active, String detail, int x, int y) {
        graphics.fill(x, y + 3, x + 5, y + 8, active ? OK : ERROR);
        graphics.drawString(font, label, x + 10, y, TEXT, false);
        if (!detail.isBlank()) {
            graphics.drawString(font, detail, rightX + rightW - 12 - font.width(detail), y, active ? AMBER : MUTED, false);
        }
        return y + 13;
    }

    private int statusLineIfFits(
            GuiGraphics graphics,
            String label,
            boolean active,
            String detail,
            int x,
            int y
    ) {
        return y + 13 > presentationActionButton.y - 3 ? y : statusLine(graphics, label, active, detail, x, y);
    }

    private void drawAction(GuiGraphics graphics, Rect rect, String label, int mouseX, int mouseY, boolean primary) {
        drawAction(graphics, rect, label, mouseX, mouseY, primary, true);
    }

    private void drawAction(
            GuiGraphics graphics,
            Rect rect,
            String label,
            int mouseX,
            int mouseY,
            boolean primary,
            boolean enabled
    ) {
        boolean hovered = enabled && rect.contains(mouseX, mouseY);
        if (!enabled) {
            graphics.fill(rect.x, rect.y, rect.x + rect.w, rect.y + rect.h, 0xFF17191B);
            graphics.renderOutline(rect.x, rect.y, rect.w, rect.h, 0xFF303234);
            graphics.drawCenteredString(font, label, rect.x + rect.w / 2, rect.y + 6, MUTED);
            return;
        }
        graphics.fill(rect.x, rect.y, rect.x + rect.w, rect.y + rect.h,
                primary ? (hovered ? 0xFF6A4B1E : AMBER_DARK) : (hovered ? 0xFF292C2F : 0xFF1B1E20));
        graphics.renderOutline(rect.x, rect.y, rect.w, rect.h, primary ? AMBER : LINE);
        graphics.drawCenteredString(font, label, rect.x + rect.w / 2, rect.y + 6, primary ? AMBER : TEXT);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (super.mouseClicked(mouseX, mouseY, button)) {
            return true;
        }
        if (button != 0) {
            return false;
        }
        if (presentationEditing) {
            if (presentationScopeButton.contains(mouseX, mouseY)) {
                togglePresentationScope();
                return true;
            }
            if (presentationSaveButton().contains(mouseX, mouseY)) {
                savePresentationDraft();
                return true;
            }
            if (presentationCancelButton().contains(mouseX, mouseY)) {
                cancelPresentationEdit("已取消名称/介绍编辑");
                return true;
            }
            if (localScopeButton.contains(mouseX, mouseY)
                    || projectScopeButton.contains(mouseX, mouseY)) {
                notice = "请先保存或取消名称/介绍编辑。";
                return true;
            }
            if (presentationActionButton.contains(mouseX, mouseY)) {
                return true;
            }
            // Keep the selected identity stable until the draft is saved or cancelled.
            return true;
        }
        if (localScopeButton.contains(mouseX, mouseY)) {
            switchScope(AssetScope.LOCAL);
            return true;
        }
        if (projectScopeButton.contains(mouseX, mouseY)) {
            switchScope(AssetScope.PROJECT);
            return true;
        }
        int filterY = bodyY + (compactHeight ? 45 : 53);
        int filterStep = compactHeight ? 15 : 20;
        for (AssetFilter candidate : AssetFilter.values()) {
            if (inside(mouseX, mouseY, frameX + 8, filterY, leftW - 16, 18)) {
                filter = candidate;
                scrollRows = 0;
                refreshRows();
                return true;
            }
            filterY += filterStep;
        }
        int listTop = bodyY + 32;
        int listBottom = frameY + frameH - 31;
        int maxScroll = Math.max(0, visible.size() - visibleRowCount());
        if (maxScroll > 0 && inside(mouseX, mouseY, rightX - 10, listTop, 9, listBottom - listTop)) {
            double ratio = Mth.clamp((mouseY - listTop) / Math.max(1.0, listBottom - listTop), 0.0, 1.0);
            scrollRows = Mth.clamp((int) Math.round(ratio * maxScroll), 0, maxScroll);
            return true;
        }
        if (inside(mouseX, mouseY, centerX + 4, listTop, centerW - 8, listBottom - listTop)) {
            int local = (int) ((mouseY - listTop) / ROW_HEIGHT);
            int index = scrollRows + local;
            if (index >= 0 && index < visible.size()) {
                if (presentationEditing) {
                    notice = "请先保存或取消名称/介绍编辑。";
                    return true;
                }
                selectedKey = visible.get(index).assetKey;
                return true;
            }
        }
        if (presentationActionButton.contains(mouseX, mouseY)) {
            beginPresentationEdit();
            return true;
        }
        if (toggleButton.contains(mouseX, mouseY)) {
            toggleSelected();
            return true;
        }
        if (exportButton.contains(mouseX, mouseY)) {
            export();
            return true;
        }
        if (reloadButton.contains(mouseX, mouseY)) {
            repository.forceReloadManifest();
            refreshRows();
            notice = repository.manifestErrorMessage().isBlank()
                    ? "项目目录已重载，共 " + manifestCount + " 条"
                    : "目录重载失败；继续使用上次有效目录";
            return true;
        }
        if (batchMarkButton.contains(mouseX, mouseY)) {
            batchMarkVisible();
            return true;
        }
        if (batchUnmarkButton.contains(mouseX, mouseY)) {
            batchUnmarkVisible();
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE && presentationEditing) {
            cancelPresentationEdit("已取消名称/介绍编辑");
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        int listTop = bodyY + 32;
        int listBottom = frameY + frameH - 31;
        if (inside(mouseX, mouseY, centerX, listTop, centerW, listBottom - listTop)) {
            int max = Math.max(0, visible.size() - visibleRowCount());
            scrollRows = Mth.clamp(scrollRows - (int) Math.signum(scrollY), 0, max);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private void toggleSelected() {
        if (scope != AssetScope.LOCAL) {
            notice = "项目目录只读；请在游戏中拿到物品后，从“本机”页标注。";
            return;
        }
        AssetRecord record = selectedRecord();
        if (record == null) {
            return;
        }
        try {
            if (repository.isSelected(record.assetKey)) {
                repository.unselect(record.assetKey);
                notice = "已取消人工标注: " + safe(record.displayNameZhCn);
            } else {
                repository.select(record);
                notice = "已人工标注: " + safe(record.displayNameZhCn);
            }
        } catch (RuntimeException error) {
            notice = "操作失败: " + safe(error.getMessage());
        }
        selectedKey = record.assetKey;
        refreshRows();
    }

    private void batchMarkVisible() {
        if (scope != AssetScope.LOCAL) {
            notice = "项目目录只读，未修改本机白名单。";
            return;
        }
        try {
            int changed = repository.selectAll(visible);
            notice = "已批量标注 " + changed + " 条当前可见记录";
        } catch (RuntimeException error) {
            notice = "批量标注失败: " + safe(error.getMessage());
        }
        refreshRows();
    }

    private void batchUnmarkVisible() {
        if (scope != AssetScope.LOCAL) {
            notice = "项目目录只读，未修改本机白名单。";
            return;
        }
        try {
            int changed = repository.unselectAll(visible.stream().map(record -> record.assetKey).toList());
            notice = "已批量取消 " + changed + " 条当前可见记录";
        } catch (RuntimeException error) {
            notice = "批量取消失败: " + safe(error.getMessage());
        }
        refreshRows();
    }

    private void export() {
        try {
            iconExportSession = new AssetIconExportSession(repository.allSelected());
            if (iconExportSession.total() == 0) {
                Path output = repository.exportSnapshot();
                Path presentationOutput = presentationRepository.exportSnapshot();
                iconExportSession = null;
                notice = "已导出物品（无可渲染图标）与显示草稿: " + output.getFileName() + " / "
                        + presentationOutput.getFileName();
            } else {
                notice = "正在从游戏客户端渲染 " + iconExportSession.total() + " 个物品图标…";
            }
        } catch (Exception error) {
            iconExportSession = null;
            notice = "导出失败: " + error.getMessage();
        }
    }

    private void renderIconExport(GuiGraphics graphics) {
        AssetIconExportSession session = iconExportSession;
        if (session == null) return;
        try {
            session.renderAndCapturePage(graphics, width, height);
            if (!session.finished()) {
                notice = "正在导出图标 " + session.captured() + " / " + session.total();
                return;
            }
            Path output = repository.exportSnapshot(session.iconDataUrls());
            Path presentationOutput = presentationRepository.exportSnapshot();
            notice = "已导出 " + session.captured() + " 个图标与物品数据: "
                    + output.getFileName() + " / " + presentationOutput.getFileName();
        } catch (Exception error) {
            notice = "图标导出失败，未生成新的物品文件: " + error.getMessage();
        } finally {
            if (session.finished() || notice.startsWith("图标导出失败")) iconExportSession = null;
        }
    }

    private AssetRecord selectedRecord() {
        return visible.stream().filter(entry -> entry.assetKey.equals(selectedKey)).findFirst().orElse(null);
    }

    private void switchScope(AssetScope next) {
        if (scope == next) {
            return;
        }
        scope = next;
        selectedKey = "";
        scrollRows = 0;
        notice = scope == AssetScope.PROJECT
                ? "项目目录只读；管表示已进入项目管理范围，不等于已部署。"
                : "本机页可标注；候选来自白名单、背包与当前容器。";
        refreshRows();
    }

    private String displayName(AssetRecord record) {
        PresentationDraft presentation = resolvedPresentation(record);
        if (presentation != null && presentation.enabled && !safe(presentation.nameZhCn).isBlank()) {
            return presentation.nameZhCn;
        }
        return observedDisplayName(record);
    }

    private String observedDisplayName(AssetRecord record) {
        if (scope != AssetScope.PROJECT) {
            return safe(record.displayNameZhCn);
        }
        if (AssetStackCodec.supportsVariantPreview(record)) {
            ItemStack preview = iconStacks.computeIfAbsent(
                    scope.name() + "\n" + record.assetKey,
                    ignored -> AssetStackCodec.restore(record));
            if (!preview.isEmpty()) {
                String variantName = preview.getHoverName().getString();
                if (!variantName.isBlank()) {
                    return variantName;
                }
            }
        }
        String localized = localizedBaseNames.computeIfAbsent(
                safe(record.registryId), AssetStackCodec::localizedBaseName);
        return localized.isBlank() ? safe(record.displayNameZhCn) : localized;
    }

    private PresentationDraft resolvedPresentation(AssetRecord record) {
        if (record == null) {
            return null;
        }
        PresentationDraft semantic = null;
        PresentationDraft registry = null;
        for (PresentationDraft draft : presentationDrafts) {
            if (!draft.enabled || !safe(draft.registryId).equals(safe(record.registryId))) {
                continue;
            }
            if (draft.applyScope == PresentationApplyScope.IDENTITY
                    && safe(draft.variantDiscriminator).equals(safe(record.variantDiscriminator))) {
                if (safe(draft.assetKey).equals(safe(record.assetKey))) {
                    return draft;
                }
                semantic = draft;
            } else if (draft.applyScope == PresentationApplyScope.REGISTRY) {
                registry = draft;
            }
        }
        return semantic != null ? semantic : registry;
    }

    private String presentationSearchText(AssetRecord record) {
        PresentationDraft draft = resolvedPresentation(record);
        if (draft == null) {
            return "";
        }
        return safe(draft.nameZhCn) + " " + String.join(" ", draft.descriptionZhCn);
    }

    private void beginPresentationEdit() {
        if (compactHeight) {
            notice = "当前窗口高度不足，放大窗口后可编辑中文名和介绍。";
            return;
        }
        if (!presentationRepository.isWritable()) {
            notice = "显示草稿处于只读保护: " + presentationRepository.lastError();
            return;
        }
        AssetRecord record = selectedRecord();
        if (record == null) {
            return;
        }
        PresentationApplyScope defaultScope = safe(record.variantDiscriminator).isBlank()
                ? PresentationApplyScope.REGISTRY
                : PresentationApplyScope.IDENTITY;
        PresentationDraft existing = draftForScope(record, defaultScope);
        boolean fpeVariant = "firstpersonfoodeating:pack_food".equals(record.registryId)
                && !safe(record.variantDiscriminator).isBlank();
        if (existing == null && !fpeVariant) {
            PresentationApplyScope fallback = defaultScope == PresentationApplyScope.IDENTITY
                    ? PresentationApplyScope.REGISTRY
                    : PresentationApplyScope.IDENTITY;
            existing = draftForScope(record, fallback);
        }
        presentationEditingDraft = existing == null
                ? newPresentationDraft(record, defaultScope)
                : existing.copy();
        presentationName.setValue(safe(presentationEditingDraft.nameZhCn).isBlank()
                ? observedDisplayName(record)
                : presentationEditingDraft.nameZhCn);
        presentationDescription.setValue(String.join("\n", presentationEditingDraft.descriptionZhCn));
        presentationEditing = true;
        setPresentationEditorsVisible(true);
        setFocused(presentationName);
        presentationName.setFocused(true);
        notice = "正在编辑显示草稿；不会改写 ItemStack Components。";
    }

    private PresentationDraft newPresentationDraft(AssetRecord record, PresentationApplyScope applyScope) {
        PresentationDraft draft = new PresentationDraft();
        draft.assetKey = record.assetKey;
        draft.registryId = record.registryId;
        draft.variantDiscriminator = record.variantDiscriminator;
        draft.applyScope = applyScope;
        draft.observedNameZhCn = observedDisplayName(record);
        draft.nameZhCn = observedDisplayName(record);
        draft.descriptionZhCn = new ArrayList<>();
        draft.enabled = true;
        draft.baseCatalogHash = statusForRecord(record).catalogHash();
        return draft;
    }

    private PresentationDraft draftForScope(AssetRecord record, PresentationApplyScope applyScope) {
        PresentationDraft semantic = null;
        for (PresentationDraft draft : presentationDrafts) {
            if (draft.applyScope != applyScope || !safe(draft.registryId).equals(safe(record.registryId))) {
                continue;
            }
            if (applyScope == PresentationApplyScope.REGISTRY) {
                return draft;
            }
            if (!safe(draft.variantDiscriminator).equals(safe(record.variantDiscriminator))) {
                continue;
            }
            if (safe(draft.assetKey).equals(safe(record.assetKey))) {
                return draft;
            }
            semantic = draft;
        }
        return semantic;
    }

    private void togglePresentationScope() {
        AssetRecord record = selectedRecord();
        if (record == null || presentationEditingDraft == null) {
            return;
        }
        PresentationApplyScope next = presentationEditingDraft.applyScope == PresentationApplyScope.IDENTITY
                ? PresentationApplyScope.REGISTRY
                : PresentationApplyScope.IDENTITY;
        if (next == PresentationApplyScope.REGISTRY
                && "firstpersonfoodeating:pack_food".equals(record.registryId)
                && !safe(record.variantDiscriminator).isBlank()) {
            notice = "FPE 食品必须按 food_id 当前变体编辑，不能覆盖全部 pack_food。";
            return;
        }
        PresentationDraft existing = draftForScope(record, next);
        if (existing != null) {
            presentationEditingDraft = existing.copy();
            presentationName.setValue(safe(existing.nameZhCn).isBlank()
                    ? observedDisplayName(record)
                    : existing.nameZhCn);
            presentationDescription.setValue(String.join("\n", existing.descriptionZhCn));
        } else {
            presentationEditingDraft.applyScope = next;
        }
        notice = next == PresentationApplyScope.REGISTRY
                ? "范围已改为整个物品类型；保存后同 registry 物品都会受影响。"
                : "范围已改为当前 Components 变体。";
    }

    private void savePresentationDraft() {
        AssetRecord record = selectedRecord();
        if (record == null || presentationEditingDraft == null) {
            return;
        }
        presentationEditingDraft.registryId = record.registryId;
        presentationEditingDraft.variantDiscriminator = record.variantDiscriminator;
        presentationEditingDraft.observedNameZhCn = observedDisplayName(record);
        presentationEditingDraft.nameZhCn = presentationName.getValue().trim();
        presentationEditingDraft.descriptionZhCn = presentationDescription.getValue().lines()
                .map(String::trim)
                .filter(line -> !line.isBlank())
                .toList();
        presentationEditingDraft.enabled = true;
        presentationEditingDraft.baseCatalogHash = statusForRecord(record).catalogHash();
        try {
            presentationRepository.upsert(presentationEditingDraft);
            cancelPresentationEdit("名称/介绍草稿已保存；桌面工作台可生成部署文件。");
            refreshRows();
        } catch (RuntimeException error) {
            notice = "显示草稿保存失败: " + safe(error.getMessage());
        }
    }

    private void cancelPresentationEdit(String message) {
        presentationEditing = false;
        presentationEditingDraft = null;
        setPresentationEditorsVisible(false);
        setFocused(null);
        notice = message;
    }

    private void setPresentationEditorsVisible(boolean visible) {
        if (search != null) {
            search.active = !visible;
            if (visible) search.setFocused(false);
        }
        if (presentationName != null) {
            presentationName.visible = visible;
            presentationName.active = visible;
            if (!visible) presentationName.setFocused(false);
        }
        if (presentationDescription != null) {
            presentationDescription.visible = visible;
            presentationDescription.active = visible;
            if (!visible) presentationDescription.setFocused(false);
        }
    }

    private Rect presentationSaveButton() {
        int half = Math.max(1, (presentationActionButton.w - 4) / 2);
        return new Rect(presentationActionButton.x, presentationActionButton.y, half, presentationActionButton.h);
    }

    private Rect presentationCancelButton() {
        Rect save = presentationSaveButton();
        return new Rect(save.x + save.w + 4, presentationActionButton.y,
                Math.max(1, presentationActionButton.w - save.w - 4), presentationActionButton.h);
    }

    private AssetStatus statusForRecord(AssetRecord record) {
        return scope == AssetScope.PROJECT
                ? repository.projectStatusFor(record)
                : repository.statusFor(record);
    }

    private static boolean componentSensitiveBase(AssetRecord record) {
        String id = safe(record.registryId).toLowerCase(Locale.ROOT);
        String kind = safe(record.variantKind).toLowerCase(Locale.ROOT);
        return id.startsWith("tacz:")
                || id.startsWith("firstpersonfoodeating:")
                || (!"plain".equals(kind) && !"item".equals(kind));
    }

    private int visibleRowCount() {
        return Math.max(1, (frameY + frameH - 31 - (bodyY + 32)) / ROW_HEIGHT);
    }

    private String ellipsize(String text, int maxWidth) {
        String safe = safe(text);
        if (font.width(safe) <= maxWidth) {
            return safe;
        }
        return font.plainSubstrByWidth(safe, Math.max(0, maxWidth - font.width("…"))) + "…";
    }

    private static String shortKey(String value) {
        String safe = safe(value);
        return safe.length() <= 16 ? safe : safe.substring(0, 16);
    }

    private static String compactDiscriminator(String value) {
        String discriminator = safe(value);
        String prefix = "food_id=firstpersonfoodeating:";
        return discriminator.startsWith(prefix)
                ? "food_id=" + discriminator.substring(prefix.length())
                : discriminator;
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static boolean inside(double mouseX, double mouseY, int x, int y, int w, int h) {
        return mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h;
    }

    @Override
    public void onClose() {
        Minecraft.getInstance().setScreen(previous);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private enum AssetScope {
        LOCAL,
        PROJECT
    }

    private record Rect(int x, int y, int w, int h) {
        private static final Rect EMPTY = new Rect(0, 0, 0, 0);

        boolean contains(double mouseX, double mouseY) {
            return inside(mouseX, mouseY, x, y, w, h);
        }
    }
}
