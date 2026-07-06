package org.yanbwe.searchcarefully.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import org.yanbwe.searchcarefully.SearchCarefully;
import org.yanbwe.searchcarefully.Config;
import org.yanbwe.searchcarefully.animation.RotationAnimationHandler;
import org.yanbwe.searchcarefully.mixin.ContainerAccessMixin;
import org.yanbwe.searchcarefully.textures.CustomTextureHandler;
import org.yanbwe.searchcarefully.util.ContainerSearchTracker;
import org.yanbwe.searchcarefully.util.ItemStackHelper;
import org.yanbwe.searchcarefully.util.SearchConstants;

@EventBusSubscriber(value = Dist.CLIENT, modid = SearchCarefully.MODID, bus = EventBusSubscriber.Bus.MOD)
public class ClientOverlayRenderer {

    // 跟踪当前正在渲染的容器界面
    private static AbstractContainerScreen<?> currentScreen = null;
    
    // 跟踪当前客户端正在搜索的槽位
    private static final java.util.Set<Slot> activeSearchSlots = new java.util.HashSet();
    
    // 跟踪工具提示拦截状态
    private static boolean tooltipBlockedThisFrame = false;

    @SubscribeEvent
    public static void onScreenRenderPre(ScreenEvent.Render.Pre event) {
        if (event.getScreen() instanceof AbstractContainerScreen) {
            currentScreen = (AbstractContainerScreen<?>) event.getScreen();
        }
    }

    @SubscribeEvent
    public static void onScreenRenderPost(ScreenEvent.Render.Post event) {
        if (Config.MASK_RENDER_ON_TOP.get()) {
            if (event.getScreen() instanceof AbstractContainerScreen<?> screen) {
                renderTopLayerMasks(event.getGuiGraphics(), screen);
            }
        }
    }

    private static void renderTopLayerMasks(GuiGraphics guiGraphics, AbstractContainerScreen<?> screen) {
        if (screen == null || screen.getMenu() == null) return;

        int guiLeft = ((ContainerAccessMixin) screen).getLeftPos();
        int guiTop = ((ContainerAccessMixin) screen).getTopPos();

        for (Slot slot : screen.getMenu().slots) {
            if (slot.hasItem()) {
                ItemStack stack = slot.getItem();
                if (ItemStackHelper.hasRemainingSearchTime(stack) &&
                    ItemStackHelper.getRemainingSearchTime(stack) > 0) {

                    int x = guiLeft + slot.x;
                    int y = guiTop + slot.y;

                    // Render mask
                    try {
                        var maskTexture = CustomTextureHandler.getMaskTexture();
                        com.mojang.blaze3d.systems.RenderSystem.disableDepthTest();
                        guiGraphics.blit(maskTexture, x, y, 400, 0, 0, 16, 16, 16, 16);
                        com.mojang.blaze3d.systems.RenderSystem.enableDepthTest();
                    } catch (Exception e) {
                        guiGraphics.fill(x, y, x + 16, y + 16, 400, 0xFF000000);
                    }

                    // Render rotation animation
                    try {
                        var rotationTexture = CustomTextureHandler.getRotationAnimationTexture();
                        long currentTime = System.currentTimeMillis();
                        long remainingTime = (long)(ItemStackHelper.getRemainingSearchTime(stack) * 1000L);

                        float[] position = RotationAnimationHandler.getRotatingPosition(remainingTime, currentTime);

                        com.mojang.blaze3d.systems.RenderSystem.disableDepthTest();
                        guiGraphics.blit(rotationTexture,
                            (int)(x + position[0]),
                            (int)(y + position[1]),
                            450, 0, 0, 16, 16, 16, 16);
                        com.mojang.blaze3d.systems.RenderSystem.enableDepthTest();
                    } catch (Exception e) {
                        // Ignore if rotation texture fails
                    }
                }
            }
        }
    }

    // 添加工具提示事件处理，用于隐藏正在搜索的物品的工具提示
    // 使用最高优先级确保在Obscure Tooltips之前执行
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onRenderTooltip(RenderTooltipEvent.Pre event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof AbstractContainerScreen) {
            AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) mc.screen;
            
            // 使用mixin获取悬停的槽位
            Slot hoveredSlot = ((ContainerAccessMixin) screen).getHoveredSlot();
            
            // 检查槽位是否正在搜索
            if (hoveredSlot != null && isSlotBeingSearched(hoveredSlot)) {
                // 如果槽位正在搜索中，取消工具提示的渲染
                event.setCanceled(true);
            }
        }
    }

    @SubscribeEvent
    public static void onClientTickPre(ClientTickEvent.Pre event) {
        resetTooltipFrameState();
    }
    
    @SubscribeEvent
    public static void onClientTickPost(ClientTickEvent.Post event) {
        updateActiveSearchSlots();
    }

    private static void updateActiveSearchSlots() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.screen instanceof AbstractContainerScreen) {
            AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>) mc.screen;
            if (screen.getMenu() != null) {
                activeSearchSlots.clear();
                
                if (Config.ENABLE_SINGLE_SLOT_SEARCH.get()) {
                    // 逐格模式: 只收集当前正在搜索的槽位
                    var trackedSlots = ContainerSearchTracker.getTrackedContainerSlots(screen);
                    for (var state : trackedSlots) {
                        if (state.slotIndex >= 0 && state.slotIndex < screen.getMenu().slots.size()) {
                            activeSearchSlots.add(screen.getMenu().slots.get(state.slotIndex));
                        }
                    }
                } else {
                    // 默认模式: 所有有搜索时间的物品
                    for (Slot slot : screen.getMenu().slots) {
                        if (slot.hasItem()) {
                            ItemStack stack = slot.getItem();
                            if (ItemStackHelper.hasRemainingSearchTime(stack)) {
                                int searchTime = (int) ItemStackHelper.getRemainingSearchTime(stack);
                                if (searchTime > 0) {
                                    activeSearchSlots.add(slot);
                                }
                            }
                        }
                    }
                }
            }
        } else {
            activeSearchSlots.clear();
        }
    }

    /**
     * 根据鼠标位置找到对应的槽位
     * 使用GUI坐标和槽位位置进行精确匹配
     */
    private static Slot getSlotUnderMouse(AbstractContainerScreen<?> screen, int mouseX, int mouseY) {
        if (screen.getMenu() != null) {
            // 获取GUI位置信息
            Point guiPosition = getGuiPosition(screen);
                
            // 遍历菜单中的所有槽位
            for (Slot slot : screen.getMenu().slots) {
                if (isMouseOverSlot(guiPosition.x, guiPosition.y, slot, mouseX, mouseY)) {
                    return slot;
                }
            }
        }
        return null;
    }
        
    /**
     * 获取容器界面的GUI位置坐标
     * 通过Mixin访问器获取准确的界面位置信息
     */
    private static Point getGuiPosition(AbstractContainerScreen<?> screen) {
        int guiLeft = ((ContainerAccessMixin) screen).getLeftPos();
        int guiTop = ((ContainerAccessMixin) screen).getTopPos();
        return new Point(guiLeft, guiTop);
    }
        
    /**
     * 表示GUI坐标的封装类
     * 用于存储界面左上角坐标信息
     */
    private static class Point {
        final int x, y;
        Point(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }
        
    /**
     * 统一的槽位鼠标检测方法
     * 提供与getSlotUnderMouse相同的接口以保持向后兼容
     */
    private static Slot findSlotUnderMouse(AbstractContainerScreen<?> screen, int mouseX, int mouseY) {
        return getSlotUnderMouse(screen, mouseX, mouseY);
    }

    private static boolean isMouseOverSlot(int guiLeft, int guiTop, Slot slot, int mouseX, int mouseY) {
        int slotX = guiLeft + slot.x;
        int slotY = guiTop + slot.y;
        return mouseX >= slotX && mouseY >= slotY && mouseX < slotX + 16 && mouseY < slotY + 16;
    }

    public static boolean isSlotBeingSearched(Slot slot) {
        return activeSearchSlots.contains(slot);
    }

    /**
     * 检查物品是否正在被搜索（用于mixin中的判断）
     */
    public static boolean isItemBeingSearched(ItemStack stack) {
        if (stack.isEmpty()) return false;
        for (Slot slot : activeSearchSlots) {
            if (slot.getItem() == stack) {
                return true;
            }
        }
        return false;
    }

    public static AbstractContainerScreen<?> getCurrentScreen() {
        return currentScreen;
    }
    
    /**
     * 重置工具提示帧状态
     */
    private static void resetTooltipFrameState() {
        tooltipBlockedThisFrame = false;
    }
    
    /**
     * 设置工具提示拦截状态
     */
    public static void setTooltipBlocked(boolean blocked) {
        tooltipBlockedThisFrame = blocked;
    }
    
    /**
     * 检查是否应该拦截工具提示
     */
    public static boolean isTooltipBlocked() {
        return tooltipBlockedThisFrame;
    }
}