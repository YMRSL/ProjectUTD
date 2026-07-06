package com.yitianys.BlockZ.network;

import com.yitianys.BlockZ.BlockZ;
import com.yitianys.BlockZ.compat.CuriosIntegration;
import com.yitianys.BlockZ.config.BlockZConfigs;
import com.yitianys.BlockZ.menu.DayZInventoryMenu;
import com.yitianys.BlockZ.util.ItemHandlerContainer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.Slot;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.wrapper.CombinedInvWrapper;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * C2S：请求把当前打开的原版/模组容器界面切换为 DayZ 界面（复用同一容器）。
 */
public record RequestSwitchToDayZMenuC2S(Component title) implements CustomPacketPayload {
    public static final Type<RequestSwitchToDayZMenuC2S> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(BlockZ.MODID, "request_switch_to_dayz_menu"));

    public static final StreamCodec<RegistryFriendlyByteBuf, RequestSwitchToDayZMenuC2S> STREAM_CODEC = StreamCodec.composite(
            ComponentSerialization.STREAM_CODEC, RequestSwitchToDayZMenuC2S::title,
            RequestSwitchToDayZMenuC2S::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RequestSwitchToDayZMenuC2S msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player() instanceof ServerPlayer player)) return;
            if (!BlockZConfigs.isDayzInventoryEnabled()) return;

            // Check current open container
            AbstractContainerMenu menu = player.containerMenu;
            if (menu == null || menu == player.inventoryMenu) return;

            // 尝试从 Slot 获取 Container / IItemHandler（模组常用 ItemHandler）
            Container targetContainer = null;
            Set<IItemHandler> handlers = new LinkedHashSet<>();
            if (menu instanceof ChestMenu chestMenu) {
                targetContainer = chestMenu.getContainer();
            } else {
                // 遍历 Slot 寻找非玩家背包的 Container
                for (Slot slot : menu.slots) {
                    IItemHandler handlerFromSlot = resolveHandlerFromSlot(slot);
                    if (handlerFromSlot != null) {
                        handlers.add(handlerFromSlot);
                        continue;
                    }
                    if (slot.container != player.getInventory()) {
                        targetContainer = slot.container;
                        break;
                    }
                }
            }

            if (targetContainer == null && !handlers.isEmpty()) {
                IItemHandler handler;
                if (handlers.size() == 1) {
                    handler = handlers.iterator().next();
                } else {
                    List<IItemHandlerModifiable> modifiableHandlers = new ArrayList<>();
                    for (IItemHandler itemHandler : handlers) {
                        if (itemHandler instanceof IItemHandlerModifiable modifiable) {
                            modifiableHandlers.add(modifiable);
                        }
                    }
                    if (modifiableHandlers.size() == handlers.size()) {
                        handler = new CombinedInvWrapper(modifiableHandlers.toArray(new IItemHandlerModifiable[0]));
                    } else {
                        handler = handlers.iterator().next();
                    }
                }
                targetContainer = new ItemHandlerContainer(handler);
            }

            if (targetContainer != null) {
                final Container container = targetContainer; // for lambda

                // Open new DayZInventoryMenu with the same container
                player.openMenu(new SimpleMenuProvider(
                        (id, inv, p) -> new DayZInventoryMenu(id, inv, container, false),
                        msg.title
                ), buf -> {
                    buf.writeInt(BlockZConfigs.getInitialPocketSlots());
                    buf.writeBoolean(false); // No Pos
                    // New Protocol:
                    // boolean hasPos (false)
                    // byte type (2 = Virtual Container)
                    // int containerSize
                    buf.writeByte(2); // Type: Virtual Container
                    buf.writeInt(container.getContainerSize());
                    CuriosIntegration.writeAdditionalDayZSlotRefs(player, buf);
                });
            } else {
                // 如果找不到 Container，可能是纯逻辑 Menu，无法转换
            }
        });
    }

    private static IItemHandler resolveHandlerFromSlot(Slot slot) {
        try {
            Method method = slot.getClass().getMethod("getItemHandler");
            if (IItemHandler.class.isAssignableFrom(method.getReturnType())) {
                Object result = method.invoke(slot);
                if (result instanceof IItemHandler handler) return handler;
            }
        } catch (Exception ignored) {
        }

        try {
            for (Field field : slot.getClass().getDeclaredFields()) {
                if (!IItemHandler.class.isAssignableFrom(field.getType())) continue;
                field.setAccessible(true);
                Object value = field.get(slot);
                if (value instanceof IItemHandler handler) return handler;
            }
        } catch (Exception ignored) {
        }
        return null;
    }
}
