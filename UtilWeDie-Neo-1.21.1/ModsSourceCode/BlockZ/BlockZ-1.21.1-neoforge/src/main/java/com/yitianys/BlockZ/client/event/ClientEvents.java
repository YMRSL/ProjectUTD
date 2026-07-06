package com.yitianys.BlockZ.client.event;

import com.yitianys.BlockZ.BlockZ;
import com.yitianys.BlockZ.client.ClientSettings;
import com.yitianys.BlockZ.config.BlockZConfigs;
import com.yitianys.BlockZ.menu.DayZInventoryMenu;
import com.yitianys.BlockZ.network.LootPickupC2S;
import com.yitianys.BlockZ.network.OpenDayZMenuC2S;
import com.yitianys.BlockZ.network.RequestSwitchToDayZMenuC2S;
import com.yitianys.BlockZ.util.InventoryUtils;
import com.yitianys.BlockZ.util.ItemSizeManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.ContainerEntity;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.Slot;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 客户端 GAME 总线事件（SPLIT 后只保留 KEEP）。
 *
 * <p>删除的 DROP 逻辑：TaczClientCompat/TaczProneCompat（瞄准相机/卧倒）、DayZMainMenuScreen 菜单音乐、
 * 体力呼吸声与冲刺锁、DayZStatsManager 统计、ProneManager、DayZZombieEntity/DayZZombieConfig、
 * RenderGuiOverlayEvent（HUD 覆盖层 + 荒野暗角）、相机视角强制。
 *
 * <p>保留：库存界面打开拦截（转 DayZ 界面 / 容器切换）、右键瞄准地面物品拾取、登出时清理同步状态。
 *
 * <p>NeoForge 迁移：事件包名 forge→neoforge；{@code ForgeRegistries.MENU_TYPES}→{@code BuiltInRegistries.MENU}；
 * 发包走 {@code PacketDistributor.sendToServer}；交互取消用 {@code setCanceled}+{@code setCancellationResult}
 * 与 {@code setUseBlock/setUseItem(TriState.FALSE)}。
 */
@EventBusSubscriber(modid = BlockZ.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public class ClientEvents {

    @SubscribeEvent
    public static void onClientPlayerLoggedOut(ClientPlayerNetworkEvent.LoggingOut event) {
        BlockZConfigs.clearSyncedValues();
        ItemSizeManager.clearSyncedClientState();
    }

    private static boolean isArclightServer() {
        Minecraft mc = Minecraft.getInstance();
        ClientPacketListener connection = mc.getConnection();
        if (connection == null) return false;
        String brand = null;
        try {
            Method method = connection.getClass().getMethod("getServerBrand");
            Object value = method.invoke(connection);
            if (value instanceof String text) {
                brand = text;
            }
        } catch (Exception ignored) {
        }

        if (brand == null) {
            try {
                Field field = connection.getClass().getDeclaredField("serverBrand");
                field.setAccessible(true);
                Object value = field.get(connection);
                if (value instanceof String text) {
                    brand = text;
                }
            } catch (Exception ignored) {
            }
        }

        if (brand == null) {
            try {
                for (Field field : connection.getClass().getDeclaredFields()) {
                    if (field.getType() != String.class) continue;
                    String name = field.getName().toLowerCase(Locale.ROOT);
                    if (!name.contains("brand")) continue;
                    field.setAccessible(true);
                    Object value = field.get(connection);
                    if (value instanceof String text) {
                        brand = text;
                        break;
                    }
                }
            } catch (Exception ignored) {
            }
        }

        if (brand == null || brand.isBlank()) return false;
        String lower = brand.toLowerCase(Locale.ROOT);
        return lower.contains("arclight") || lower.contains("bukkit");
    }

    private static boolean shouldSkipDayZOverride(AbstractContainerScreen<?> screen) {
        try {
            if (screen.getMenu() != null) {
                var key = BuiltInRegistries.MENU.getKey(screen.getMenu().getType());
                if (key == null || !"minecraft".equals(key.getNamespace())) {
                    return true;
                }
                String menuName = screen.getMenu().getClass().getName().toLowerCase(Locale.ROOT);
                if (menuName.contains("tacz") || !isSafeVanillaContainerMenu(screen)) {
                    return true;
                }
            }
        } catch (Exception ignored) {
            return true;
        }

        String screenName = screen.getClass().getName().toLowerCase(Locale.ROOT);
        return screenName.contains("tacz");
    }

    private static boolean isSafeVanillaContainerMenu(AbstractContainerScreen<?> screen) {
        if (screen.getMenu() instanceof ChestMenu chestMenu) {
            return !(chestMenu.getContainer() instanceof ContainerEntity);
        }

        String menuClassName = screen.getMenu().getClass().getName();
        return "net.minecraft.world.inventory.DispenserMenu".equals(menuClassName)
                || "net.minecraft.world.inventory.HopperMenu".equals(menuClassName)
                || "net.minecraft.world.inventory.ShulkerBoxMenu".equals(menuClassName);
    }

    @SubscribeEvent
    public static void onScreenOpening(ScreenEvent.Opening event) {
        boolean inventoryUiEnabled = BlockZConfigs.isDayzInventoryEnabled() && ClientSettings.dayzEnabled;
        // NeoForge：ScreenEvent.Opening#getScreen() 返回当前(旧)屏幕；
        // 即将打开的屏幕用 getNewScreen()。
        net.minecraft.client.gui.screens.Screen opening = event.getNewScreen();
        if (opening instanceof InventoryScreen) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null) return;

            // 只有当 DayZ UI 启用时才拦截
            if (inventoryUiEnabled) {
                BlockZ.LOGGER.info("Intercepting InventoryScreen opening, sending OpenDayZMenuC2S to server. dayzEnabled={}, inventoryUiEnabled={}", ClientSettings.dayzEnabled, inventoryUiEnabled);
                PacketDistributor.sendToServer(new OpenDayZMenuC2S());
                if (!isArclightServer()) {
                    event.setCanceled(true);
                }
            } else {
                BlockZ.LOGGER.info("Allowing InventoryScreen opening. dayzEnabled={}, inventoryUiEnabled={}", ClientSettings.dayzEnabled, inventoryUiEnabled);
            }
        } else if (opening instanceof AbstractContainerScreen<?> containerScreen) {
            if (inventoryUiEnabled) {
                if (opening instanceof com.yitianys.BlockZ.client.gui.DayZInventoryScreen) return;
                if (opening instanceof com.yitianys.BlockZ.client.gui.DayZChestScreen) return;

                Minecraft mc = Minecraft.getInstance();
                if (mc.player == null) return;
                if (containerScreen.getMenu() == mc.player.inventoryMenu) return;

                if (isArclightServer() && containerScreen.getMenu() instanceof ChestMenu chestMenu) {
                    event.setCanceled(true);
                    mc.setScreen(new com.yitianys.BlockZ.client.gui.DayZChestScreen(chestMenu, mc.player.getInventory(), containerScreen.getTitle()));
                    return;
                }

                if (shouldSkipDayZOverride(containerScreen)) {
                    return;
                }

                int minX = Integer.MAX_VALUE;
                int minY = Integer.MAX_VALUE;
                List<Slot> containerSlots = new ArrayList<>();
                for (Slot slot : containerScreen.getMenu().slots) {
                    if (slot.container == mc.player.getInventory()) continue;
                    containerSlots.add(slot);
                    if (slot.x < minX) minX = slot.x;
                    if (slot.y < minY) minY = slot.y;
                }

                if (!containerSlots.isEmpty()) {
                    List<DayZInventoryMenu.VicinitySlotLayout> layout = new ArrayList<>();
                    int baseX = minX == Integer.MAX_VALUE ? 0 : minX;
                    int baseY = minY == Integer.MAX_VALUE ? 0 : minY;
                    for (Slot slot : containerSlots) {
                        layout.add(new DayZInventoryMenu.VicinitySlotLayout(
                                slot.getSlotIndex(),
                                slot.x - baseX,
                                slot.y - baseY
                        ));
                    }
                    DayZInventoryMenu.setPendingClientLayout(layout);
                }

                PacketDistributor.sendToServer(new RequestSwitchToDayZMenuC2S(containerScreen.getTitle()));
                if (!isArclightServer()) {
                    event.setCanceled(true);
                }
            }
        }
    }

    @SubscribeEvent
    public static void onClientRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        tryPickup(event);
    }

    @SubscribeEvent
    public static void onClientRightClickItem(PlayerInteractEvent.RightClickItem event) {
        tryPickup(event);
    }

    @SubscribeEvent
    public static void onClientRightClickEmpty(PlayerInteractEvent.RightClickEmpty event) {
        tryPickup(event);
    }

    private static void tryPickup(PlayerInteractEvent event) {
        if (!event.getLevel().isClientSide) return;
        if (!ClientSettings.dayzEnabled) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        Entity entity = InventoryUtils.getTargetedItemEntity(mc.player, 4.0); // 4 blocks reach
        if (entity != null) {
            PacketDistributor.sendToServer(new LootPickupC2S(entity.getId()));
            // NeoForge 1.21.1：仅 RightClickBlock / RightClickItem 实现 ICancellableEvent，
            // RightClickEmpty 不可取消，跳过。
            if (event instanceof PlayerInteractEvent.RightClickBlock blockEvent) {
                blockEvent.setCanceled(true);
                blockEvent.setUseBlock(TriState.FALSE);
                blockEvent.setUseItem(TriState.FALSE);
            } else if (event instanceof PlayerInteractEvent.RightClickItem itemEvent) {
                itemEvent.setCanceled(true);
            }
        }
    }
}
