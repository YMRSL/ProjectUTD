package com.scarasol.sona.manager;

import com.scarasol.sona.configuration.CommonConfig;
import com.scarasol.sona.init.SonaDataComponents;
import com.scarasol.sona.init.SonaSounds;
import com.scarasol.sona.network.RotPacket;
import com.scarasol.sona.util.SonaBlockUtil;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.PlayerEnderChestContainer;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Random;

public class RotManager {


    public static void rotWhenStack(ItemStack itemStack, double rotValue1, double rotValue2, int count1, int count2, long saveTime) {
        if (isEdible(itemStack) && count2 > 0) {
            double rotValue = (rotValue1 * count1 + rotValue2 * count2) / (count1 + count2);
            putRot(itemStack, rotValue);
            putRotSaveTime(itemStack, Math.max(Math.min(getRotSaveTime(itemStack), saveTime), 0));
        }
    }

    public static boolean isEdible(ItemStack itemStack) {
        return itemStack.has(DataComponents.FOOD);
    }

    public static void putRot(ItemStack itemStack, double rotValue) {
        itemStack.set(SonaDataComponents.ROT_VALUE.get(), rotValue);
    }

    public static double getRot(ItemStack itemStack) {
        return itemStack.getOrDefault(SonaDataComponents.ROT_VALUE.get(), 0.0);
    }

    public static void addRot(ItemStack itemStack, double addition) {
        if (addition > 0)
            addition = addition * CommonConfig.ROT_WEIGHT.get().floatValue();
        addActualRot(itemStack, addition);
    }

    public static void addActualRot(ItemStack itemStack, double addition) {
        double rot = addition > 0 ? Math.min(100, addition + getRot(itemStack)) : Math.max(0, addition + getRot(itemStack));
        putRot(itemStack, rot);
    }

    public static void putMultiplier(ItemStack itemStack, double rotMultiplier) {
        itemStack.set(SonaDataComponents.ROT_MULTIPLIER.get(), rotMultiplier);
    }

    public static double getMultiplier(ItemStack itemStack) {
        double multiplier = itemStack.getOrDefault(SonaDataComponents.ROT_MULTIPLIER.get(), 0.0);
        if (multiplier == 0) {
            multiplier = initMultiplier(itemStack);
            putMultiplier(itemStack, multiplier);
        }
        if (isWarped(itemStack)) {
            multiplier = multiplier * CommonConfig.WARPED_WEIGHT.get();
        }
        return multiplier;
    }

    public static void putRotSaveTime(ItemStack itemStack, long gameTime) {
        itemStack.set(SonaDataComponents.ROT_SAVE_TIME.get(), gameTime);
    }

    public static long getRotSaveTime(ItemStack itemStack) {
        return itemStack.getOrDefault(SonaDataComponents.ROT_SAVE_TIME.get(), 0L);
    }

    public static void putWarp(ItemStack itemStack, boolean warped) {
        itemStack.set(SonaDataComponents.WARPED.get(), warped);
    }

    public static boolean isWarped(ItemStack itemStack) {
        return itemStack.getOrDefault(SonaDataComponents.WARPED.get(), false);
    }

    public static double initMultiplier(ItemStack itemStack) {
        int index = Math.max(CommonConfig.findIndex(BuiltInRegistries.ITEM.getKey(itemStack.getItem()).toString(), CommonConfig.ROT_DETAIL.get()), CommonConfig.tagSearch(itemStack, CommonConfig.ROT_DETAIL.get()));
        double rate = 1;
        if (index == -1) {
            ResourceLocation resourceLocation = BuiltInRegistries.ITEM.getKey(itemStack.getItem());
            if ("zombiekit".equals(resourceLocation.getNamespace())) {
                if ("chocolate".equals(resourceLocation.getPath())) {
                    rate = 0.02;
                } else if ("compressed_biscuit".equals(resourceLocation.getPath())) {
                    rate = 0.01;
                }
            }
            return rate;
        }
        String[] str = CommonConfig.ROT_DETAIL.get().get(index).split(",");
        if (str.length < 3) {
            return rate;
        }
        if (Double.parseDouble(str[1].trim()) <= 0) {
            return rate;
        }
        rate = Double.parseDouble(str[1].trim());
        return rate;
    }

    public static void eatRotFood(LivingEntity livingEntity, ItemStack itemStack) {
        double rotValue = getRot(itemStack);
        // 用户定制：发霉食物负面效果的触发概率相对原版 ×1.3（高 30%）。
        // 原版触发阈值用 random > t（命中概率 = 1 - t）；提高命中概率 30% 即 newChance = oldChance*1.3，
        // 对应新阈值 newT = 1 - oldChance*1.3。≥90 档原本就是 100%，无需调整。
        //   ≥70 档：25% → 32.5%，阈值 0.75 → 0.675
        //   ≥40 档：10% → 13%， 阈值 0.90 → 0.87
        if (rotValue >= 90) {
            livingEntity.addEffect(new MobEffectInstance(MobEffects.POISON, 600, 0, false, false));
            livingEntity.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 400, 0, false, false));
        } else if (rotValue >= 70 && livingEntity.level().getRandom().nextDouble() > 0.675) {
            livingEntity.addEffect(new MobEffectInstance(MobEffects.POISON, 600, 0, false, false));
            livingEntity.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 400, 0, false, false));
        } else if (rotValue >= 40 && livingEntity.level().getRandom().nextDouble() > 0.87) {
            livingEntity.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 400, 0, false, false));
        }
    }

    public static boolean warpFood(ItemStack food, ItemStack warpItem, LivingEntity livingEntity) {
        if (!canBeRotten(food) || !isEdible(food))
            return false;
        if (!CommonConfig.WARPED_ITEMS.get().contains(BuiltInRegistries.ITEM.getKey(warpItem.getItem()).toString()))
            return false;
        int warpCount = Math.min(warpItem.getCount(), food.getCount());
        ItemStack warpedFood = food.copy();
        putWarp(warpedFood, true);
        warpedFood.setCount(warpCount);
        if (livingEntity instanceof Player player)
            ItemHandlerHelper.giveItemToPlayer(player, warpedFood);
        warpItem.shrink(warpCount);
        food.shrink(warpCount);
        rotParticle(livingEntity.level(), new Random(), livingEntity.getX(), livingEntity.getY(), livingEntity.getZ());
        return true;
    }

    public static void rotParticle(Level level, Random random, double x, double y, double z) {
        SimpleParticleType particleType = ParticleTypes.COMPOSTER;
        SoundEvent soundEvent = SonaSounds.SLIDER_ZIPPER_BAG.get();
        for (int i = 0; i < 10; ++i) {
            double d4 = random.nextGaussian() * 0.02;
            double d5 = random.nextGaussian() * 0.02;
            double d6 = random.nextGaussian() * 0.02;
            double d = 0.95;
            level.addParticle(particleType, x + 0.13124999403953552 + 0.737500011920929 * (double) random.nextFloat(), y + d + (double) random.nextFloat() * (1.0 - d), z + 0.13124999403953552 + 0.737500011920929 * (double) random.nextFloat(), d4, d5, d6);
            level.playSound(null, x, y, z, soundEvent, SoundSource.PLAYERS, 1, 1);
        }
    }

    public static double getContainerMultiplier(Level level, @Nullable BlockPos blockPos) {
        int index = -1;
        if (blockPos == null)
            index = CommonConfig.findIndex("minecraft:ender_chest", CommonConfig.ROT_CONTAINER.get());
        else {
            BlockEntity blockEntity = level.getBlockEntity(blockPos);
            if (blockEntity != null)
                index = CommonConfig.findIndex(BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(blockEntity.getType()).toString(), CommonConfig.ROT_CONTAINER.get());
            if (index == -1)
                index = CommonConfig.findIndex(BuiltInRegistries.BLOCK.getKey(level.getBlockState(blockPos).getBlock()).toString(), CommonConfig.ROT_CONTAINER.get());
        }
        if (index != -1) {
            String[] str = CommonConfig.ROT_CONTAINER.get().get(index).split(",");
            if (str.length >= 2) {
                return Double.parseDouble(str[1].trim());
            }
        }
        return 1;
    }

    public static void syncRotValue(double rotValue, int slot, boolean flag, ServerPlayer player) {
        PacketDistributor.sendToPlayer(player, new RotPacket(rotValue, slot, flag));
    }

    public static void containerOpen(Player player, AbstractContainerMenu abstractContainerMenu) {
        double containerMultiplier = 1;
        double temperature = 1;
        BlockPos blockPos = SonaBlockUtil.tryFindContainerBlockPos(abstractContainerMenu);
        if (blockPos == null) {
            if (abstractContainerMenu.slots.get(0).container instanceof PlayerEnderChestContainer) {
                containerMultiplier = RotManager.getContainerMultiplier(player.level(), null);
            }
        } else {

            double height = blockPos.getY();
            containerMultiplier = RotManager.getContainerMultiplier(player.level(), blockPos);
            Level level = player.level();
            BlockEntity blockEntity = level.getBlockEntity(blockPos);
            if (blockEntity != null && CommonConfig.ROT_TEMPERATURE.get() && CommonConfig.findIndex(BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(blockEntity.getType()).toString(), CommonConfig.ROT_TEMPERATURE_WHITELIST.get()) == -1) {
                if (height < 0) {
                    temperature = 0.5;
                } else if (height < 30) {
                    temperature = 0.7;
                } else if (height < 60) {
                    temperature = 0.9;
                }
                temperature = temperature * (level.getBiome(blockPos).value().getBaseTemperature() / 2 + 0.6);
            }
        }
        if (containerMultiplier != 0 && player instanceof ServerPlayer serverPlayer) {
            RotManager.rotInContainer(serverPlayer, abstractContainerMenu.slots, abstractContainerMenu.slots.size() - serverPlayer.getInventory().items.size(), containerMultiplier, serverPlayer.level().getGameTime(), temperature);
        }
    }

    public static void rotInContainer(ServerPlayer serverPlayer, List<Slot> slots, int size, double containerMultiplier, long gameTime, double temperature) {
        for (int i = 0; i < size; i++) {
            Slot slot = slots.get(i);
            ItemStack itemStack = slot.getItem().copy();
            if (isEdible(itemStack) && canBeRotten(itemStack)) {
                long saveTime = getRotSaveTime(itemStack);
                if (saveTime == 0) continue;
                double rotValue = getRot(itemStack);
                int cycle = 1680;
                cycle = Math.max((int) (cycle / temperature / getMultiplier(itemStack) / containerMultiplier), 1);
                double rotAddition = (gameTime - saveTime) / cycle;
                rotAddition = rotAddition * CommonConfig.ROT_WEIGHT.get();
                if (rotValue + rotAddition >= 100) {
                    rotten(itemStack, null, slot);
                } else {
                    addActualRot(itemStack, rotAddition);
                    slot.set(itemStack);
                    syncRotValue(getRot(itemStack), i, false, serverPlayer);
                    slot.setChanged();
                }
            }
        }
    }

    public static void rotTimeUpdate(List<Slot> slots, int size, long gameTime) {
        for (int i = 0; i < size; i++) {
            Slot slot = slots.get(i);
            ItemStack itemStack = slot.getItem();
            if (isEdible(itemStack) && canBeRotten(itemStack)) {
                putRotSaveTime(itemStack, gameTime);
                slot.set(itemStack);
                slot.setChanged();
            }
        }
    }

    public static void rotten(ItemStack itemStack, SlotAccess slotAccess, Slot slot) {
        int index = Math.max(CommonConfig.findIndex(BuiltInRegistries.ITEM.getKey(itemStack.getItem()).toString(), CommonConfig.ROT_DETAIL.get()), CommonConfig.tagSearch(itemStack, CommonConfig.ROT_DETAIL.get()));
        if (index == -1) {
            int amount = itemStack.getCount();
            itemStack.setCount(0);
            Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(CommonConfig.ROT_RESULT.get()));
            if (item != null) {
                ItemStack output = new ItemStack(item);
                output.setCount(amount);
                if (slot == null && slotAccess != null) {
                    slotAccess.set(output);
                } else if (slot != null && slotAccess == null) {
                    slot.set(output);
                    slot.setChanged();
                }
            }
            return;
        }
        String[] str = CommonConfig.ROT_DETAIL.get().get(index).split(",");
        if (str.length < 3) {
            int amount = itemStack.getCount();
            itemStack.setCount(0);
            Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(CommonConfig.ROT_RESULT.get()));
            if (item != null) {
                ItemStack output = new ItemStack(item);
                output.setCount(amount);
                if (slot == null && slotAccess != null) {
                    slotAccess.set(output);
                } else if (slot != null && slotAccess == null) {
                    slot.set(output);
                    slot.setChanged();
                }
            }
            return;
        }
        int count = itemStack.getCount();
        itemStack.setCount(0);
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(str[2].trim()));
        if (item == null)
            item = BuiltInRegistries.ITEM.get(ResourceLocation.parse(CommonConfig.ROT_RESULT.get()));
        if (item != null) {
            ItemStack itemStackNew = new ItemStack(item);
            itemStackNew.setCount(count);
            if (slot == null && slotAccess != null) {
                slotAccess.set(itemStackNew);
            } else if (slot != null && slotAccess == null) {
                slot.set(itemStackNew);
                slot.setChanged();
            }
        }

    }

    public static boolean canBeRotten(ItemStack itemStack) {
        ResourceLocation resourceLocation = BuiltInRegistries.ITEM.getKey(itemStack.getItem());
        if ("zombiekit".equals(resourceLocation.getNamespace())) {
            if (resourceLocation.getPath().contains("canned")) {
                return false;
            }
        }
        return !CommonConfig.ROT_WHITELIST.get().contains(BuiltInRegistries.ITEM.getKey(itemStack.getItem()).toString());
    }

    public static void rotTick(Object object, Entity entity, int slot, double temperature) {
        if (object instanceof ItemStack itemStack) {
            if (isEdible(itemStack) && canBeRotten(itemStack)) {
                int cycle = 1680;
                if (CommonConfig.ROT_TEMPERATURE.get()) {
                    cycle = Math.max((int) (cycle / temperature / getMultiplier(itemStack)), 1);
                } else {
                    cycle = Math.max((int) (cycle / getMultiplier(itemStack)), 1);
                }
                if (entity.level().getGameTime() % cycle == 0) {
                    addRot(itemStack, 1);
                    if (entity instanceof ServerPlayer serverPlayer)
                        syncRotValue(getRot(itemStack), slot, true, serverPlayer);
                }
                if (getRot(itemStack) >= 100) {
                    rotten(itemStack, entity.getSlot(slot), null);
                }
            }
        }
    }

    /**
     * 1.20.1 上游在 {@code ItemStack#isSameItemSameTags} 里手动剥掉
     * "RotValue"/"RotSaveTime"/"RotMultiplier"/"Warped" 这几个 NBT 键再比较，
     * 以允许腐烂值不同的同种食物互相堆叠。1.21 这些数据迁到了 DataComponent，
     * 故改为：复制两个 stack、移除腐烂相关组件后用 {@link ItemStack#isSameItemSameComponents} 比较。
     * <p>
     * 由 {@code ItemStackMixin#OnIsSameItemSameComponents} 调用。
     */
    // 重入保护：本方法内部会再调 ItemStack.isSameItemSameComponents，而该方法被
    // ItemStackMixin#OnIsSameItemSameComponents 拦截又会回调本方法，若不拦会无限递归 → StackOverflow。
    private static final ThreadLocal<Boolean> COMPARING = ThreadLocal.withInitial(() -> Boolean.FALSE);

    public static boolean matchesIgnoringRot(ItemStack a, ItemStack b) {
        if (COMPARING.get())
            return false; // 已在比较去腐烂副本的过程中，直接放行原生比较，不再递归
        if (a.isEmpty() || b.isEmpty())
            return a.isEmpty() && b.isEmpty();
        if (!a.is(b.getItem()))
            return false;
        ItemStack copyA = stripRotComponents(a.copy());
        ItemStack copyB = stripRotComponents(b.copy());
        COMPARING.set(Boolean.TRUE);
        try {
            return ItemStack.isSameItemSameComponents(copyA, copyB);
        } finally {
            COMPARING.set(Boolean.FALSE);
        }
    }

    private static ItemStack stripRotComponents(ItemStack itemStack) {
        itemStack.remove(SonaDataComponents.ROT_VALUE.get());
        itemStack.remove(SonaDataComponents.ROT_SAVE_TIME.get());
        itemStack.remove(SonaDataComponents.ROT_MULTIPLIER.get());
        itemStack.remove(SonaDataComponents.WARPED.get());
        return itemStack;
    }

    public static void tooltipInsert(List<Component> toolTip, ItemStack itemStack) {
        double rotValue = getRot(itemStack);
        if (rotValue < 40) {
            toolTip.add(Math.min(1, toolTip.size()), Component.translatable("tooltip.sona.rot.fresh").withStyle(ChatFormatting.DARK_GREEN));
        } else if (rotValue < 70) {
            toolTip.add(Math.min(1, toolTip.size()), Component.translatable("tooltip.sona.rot.slightly_spoiled").withStyle(ChatFormatting.DARK_AQUA));
        } else if (rotValue < 90) {
            toolTip.add(Math.min(1, toolTip.size()), Component.translatable("tooltip.sona.rot.spoiled").withStyle(ChatFormatting.YELLOW));
        } else {
            toolTip.add(Math.min(1, toolTip.size()), Component.translatable("tooltip.sona.rot.heavily_spoiled").withStyle(ChatFormatting.RED));
        }
        if (isWarped(itemStack))
            toolTip.add(Math.min(2, toolTip.size()), Component.translatable("tooltip.sona.rot.warped").withStyle(ChatFormatting.ITALIC).withStyle(ChatFormatting.DARK_GREEN));
    }


}
