package net.tkg.ModernMayhem.server.item.generic;

import io.netty.buffer.Unpooled;
import java.util.List;
import java.util.stream.IntStream;
import net.minecraft.ChatFormatting;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.tkg.ModernMayhem.server.GUI.GenericBackpackGUI;
import net.tkg.ModernMayhem.server.util.CuriosUtil;
import net.tkg.ModernMayhem.server.util.ItemNBTUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import top.theillusivec4.curios.api.CuriosApi;
import top.theillusivec4.curios.api.type.capability.ICurioItem;
import top.theillusivec4.curios.api.type.capability.ICuriosItemHandler;

public abstract class GenericBackpackItem
extends Item
implements ICurioItem {
    private final byte curiosSlotType;

    public GenericBackpackItem(byte pCuriosSlotType) {
        super(new Item.Properties().stacksTo(1));
        this.curiosSlotType = pCuriosSlotType;
    }

    public abstract int getInventoryLines();

    public abstract int getInventoryColumns();

    public abstract boolean canSupplyAmmo();

    public int getInventorySize() {
        return this.getInventoryLines() * this.getInventoryColumns();
    }

    private void validateAndResizeInventory(ItemStack stack, Player player) {
        RegistryAccess provider = player.level().registryAccess();
        CompoundTag tag = ItemNBTUtil.getOrCreateTag(stack);
        if (!tag.contains("inventory")) {
            return;
        }
        int configSize = this.getInventorySize();
        ItemStackHandler currentHandler = new ItemStackHandler();
        currentHandler.deserializeNBT(provider, tag.getCompound("inventory"));
        if (currentHandler.getSlots() != configSize) {
            ItemStackHandler newHandler = new ItemStackHandler(configSize);
            for (int i = 0; i < currentHandler.getSlots(); ++i) {
                ItemStack s = currentHandler.getStackInSlot(i);
                if (i < configSize) {
                    newHandler.setStackInSlot(i, s);
                    continue;
                }
                if (s.isEmpty() || player.getInventory().add(s)) continue;
                player.drop(s, false);
            }
            tag.put("inventory", (Tag)newHandler.serializeNBT(provider));
            ItemNBTUtil.setTag(stack, tag);
        }
    }

    @NotNull
    public InteractionResultHolder<ItemStack> use(@NotNull Level pLevel, @NotNull Player pPlayer, @NotNull InteractionHand pUsedHand) {
        if (this.getInventorySize() > 0) {
            ItemStack stack = pPlayer.getItemInHand(pUsedHand);
            if (!pLevel.isClientSide) {
                this.validateAndResizeInventory(stack, pPlayer);
            }
            GenericBackpackItem.InitInventory(stack, this.getInventorySize(), pLevel.registryAccess());
            if (pPlayer instanceof ServerPlayer && pUsedHand == InteractionHand.MAIN_HAND) {
                this.OpenGUIFromPlayerInventory(pPlayer, stack);
            }
        }
        return super.use(pLevel, pPlayer, pUsedHand);
    }

    public boolean overrideStackedOnOther(ItemStack pStack, Slot pSlot, ClickAction pAction, Player pPlayer) {
        block7: {
            if (this.getInventorySize() <= 0) break block7;
            RegistryAccess provider = pPlayer.level().registryAccess();
            ItemStack slotStack = pSlot.getItem();
            CompoundTag tag = GenericBackpackItem.InitInventory(pStack, this.getInventorySize(), provider);
            if (pAction == ClickAction.SECONDARY) {
                ItemStackHandler inventory = new ItemStackHandler(this.getInventorySize());
                inventory.deserializeNBT(provider, tag.getCompound("inventory"));
                if (slotStack.isEmpty()) {
                    for (int i = inventory.getSlots() - 1; i >= 0; --i) {
                        ItemStack stack = inventory.getStackInSlot(i);
                        if (stack.isEmpty()) continue;
                        pSlot.set(inventory.extractItem(i, stack.getCount(), false));
                        tag.put("inventory", (Tag)inventory.serializeNBT(provider));
                        ItemNBTUtil.setTag(pStack, tag);
                        return true;
                    }
                } else {
                    for (int i = 0; i < inventory.getSlots(); ++i) {
                        ItemStack stack = inventory.getStackInSlot(i);
                        if (ItemStack.isSameItemSameComponents((ItemStack)slotStack, (ItemStack)inventory.getStackInSlot(i))) {
                            ItemStack remaining = inventory.insertItem(i, slotStack, false);
                            pSlot.set(remaining);
                            tag.put("inventory", (Tag)inventory.serializeNBT(provider));
                            ItemNBTUtil.setTag(pStack, tag);
                            if (remaining.getCount() <= 0) {
                                return true;
                            }
                        }
                        if (!stack.isEmpty()) continue;
                        inventory.insertItem(i, slotStack, false);
                        pSlot.set(ItemStack.EMPTY);
                        tag.put("inventory", (Tag)inventory.serializeNBT(provider));
                        ItemNBTUtil.setTag(pStack, tag);
                        return true;
                    }
                }
            }
        }
        return super.overrideStackedOnOther(pStack, pSlot, pAction, pPlayer);
    }

    public boolean overrideOtherStackedOnMe(ItemStack pStack, ItemStack pOther, Slot pSlot, ClickAction pAction, Player pPlayer, SlotAccess pAccess) {
        return super.overrideOtherStackedOnMe(pStack, pOther, pSlot, pAction, pPlayer, pAccess);
    }

    protected static CompoundTag InitInventory(ItemStack pStack, int pInventorySize, RegistryAccess provider) {
        CompoundTag tag = ItemNBTUtil.getOrCreateTag(pStack);
        if (!tag.contains("inventory")) {
            tag.put("inventory", (Tag)new ItemStackHandler(pInventorySize).serializeNBT(provider));
            ItemNBTUtil.setTag(pStack, tag);
        }
        return tag;
    }

    public void OpenGUIFromPlayerInventory(Player pPlayer, ItemStack pStack) {
        if (this.getInventorySize() > 0) {
            ServerPlayer player = (ServerPlayer)pPlayer;
            RegistryAccess provider = pPlayer.level().registryAccess();
            RegistryFriendlyByteBuf data = new RegistryFriendlyByteBuf(Unpooled.buffer(), provider);
            this.validateAndResizeInventory(pStack, pPlayer);
            CompoundTag tag = ItemNBTUtil.getOrCreateTag(pStack);
            if (tag.contains("inventory")) {
                data.writeByte(this.getInventoryLines());
                data.writeByte(this.getInventoryColumns());
                data.writeNbt(tag.getCompound("inventory"));
                data.writeBoolean(false);
                ItemStack.OPTIONAL_STREAM_CODEC.encode(data, pStack);
            }
            player.openMenu((MenuProvider)new SimpleMenuProvider((pContainerId, pPlayerInventory, pPlayer1) -> new GenericBackpackGUI(pContainerId, pPlayerInventory, data), pStack.getDisplayName()), friendlyByteBuf -> {
                friendlyByteBuf.writeByte(this.getInventoryLines());
                friendlyByteBuf.writeByte(this.getInventoryColumns());
                friendlyByteBuf.writeNbt(tag.getCompound("inventory"));
                friendlyByteBuf.writeBoolean(false);
                ItemStack.OPTIONAL_STREAM_CODEC.encode(friendlyByteBuf, pStack);
            });
        }
    }

    public void OpenGUIFromCuriosInventory(Player pPlayer, ItemStack pStack) {
        if (this.getInventorySize() > 0) {
            ServerPlayer player = (ServerPlayer)pPlayer;
            RegistryAccess provider = pPlayer.level().registryAccess();
            RegistryFriendlyByteBuf data = new RegistryFriendlyByteBuf(Unpooled.buffer(), provider);
            CompoundTag tag = ItemNBTUtil.getOrCreateTag(pStack);
            boolean resetStackInInv = !tag.contains("inventory");
            String curiosSlotTypeIdentifer = switch (this.curiosSlotType) {
                case 0 -> "back";
                case 1 -> "body";
                default -> "";
            };
            int backpackSlotID = switch (this.curiosSlotType) {
                case 0 -> CuriosUtil.getBackpackSlotID(pPlayer);
                case 1 -> CuriosUtil.getRigSlotID(pPlayer);
                default -> -1;
            };
            this.validateAndResizeInventory(pStack, pPlayer);
            GenericBackpackItem.InitInventory(pStack, this.getInventorySize(), provider);
            tag = ItemNBTUtil.getOrCreateTag(pStack);
            ICuriosItemHandler playerCuriosInventory = (ICuriosItemHandler)CuriosApi.getCuriosInventory((LivingEntity)pPlayer).get();
            if (resetStackInInv) {
                playerCuriosInventory.getStacksHandler(curiosSlotTypeIdentifer).ifPresent(iCurioStacksHandler -> iCurioStacksHandler.getStacks().setStackInSlot(backpackSlotID, pStack));
                playerCuriosInventory = (ICuriosItemHandler)CuriosApi.getCuriosInventory((LivingEntity)pPlayer).get();
            }
            data.writeByte(this.getInventoryLines());
            data.writeByte(this.getInventoryColumns());
            data.writeNbt(tag.getCompound("inventory"));
            data.writeBoolean(true);
            data.writeByte(backpackSlotID);
            data.writeByte((int)this.curiosSlotType);
            ICuriosItemHandler finalPlayerCuriosInventory = playerCuriosInventory;
            CompoundTag finalTag = tag;
            player.openMenu((MenuProvider)new SimpleMenuProvider((pContainerId, pPlayerInventory, pPlayer1) -> new GenericBackpackGUI(pContainerId, pPlayerInventory, data, finalPlayerCuriosInventory), pStack.getDisplayName()), friendlyByteBuf -> {
                friendlyByteBuf.writeByte(this.getInventoryLines());
                friendlyByteBuf.writeByte(this.getInventoryColumns());
                friendlyByteBuf.writeNbt(finalTag.getCompound("inventory"));
                friendlyByteBuf.writeBoolean(true);
                friendlyByteBuf.writeByte(backpackSlotID);
                friendlyByteBuf.writeByte((int)this.curiosSlotType);
            });
        }
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        if (this.canSupplyAmmo()) {
            tooltip.add((Component)Component.translatable((String)"description.mm.rig_supplies_ammo").withStyle(ChatFormatting.GREEN));
        }
        CompoundTag tag = ItemNBTUtil.getTag(stack);
        if (tag != null && tag.contains("inventory") && context.registries() != null) {
            ItemStackHandler inventory = new ItemStackHandler();
            inventory.deserializeNBT(context.registries(), tag.getCompound("inventory"));
            int shownItems = 0;
            for (int i = 0; i < inventory.getSlots(); ++i) {
                ItemStack item = inventory.getStackInSlot(i);
                if (item.isEmpty()) continue;
                MutableComponent line = Component.literal((String)"• ").withStyle(ChatFormatting.GRAY);
                line.append(item.getHoverName());
                line.append((Component)Component.literal((String)(" x" + item.getCount())).withStyle(ChatFormatting.GRAY));
                tooltip.add((Component)line);
                if (++shownItems >= 5) break;
            }
            int totalItems = (int)IntStream.range(0, inventory.getSlots()).mapToObj(arg_0 -> ((ItemStackHandler)inventory).getStackInSlot(arg_0)).filter(s -> !s.isEmpty()).count();
            if (shownItems < totalItems) {
                tooltip.add((Component)Component.literal((String)("...and " + (totalItems - shownItems) + " more")).withStyle(ChatFormatting.DARK_GRAY));
            }
        }
    }
}
