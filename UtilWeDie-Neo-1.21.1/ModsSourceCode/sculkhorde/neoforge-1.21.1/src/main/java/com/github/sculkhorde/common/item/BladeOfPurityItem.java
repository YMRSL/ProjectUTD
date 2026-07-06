package com.github.sculkhorde.common.item;

import com.github.sculkhorde.common.entity.infection.CursorSurfacePurifierEntity;
import com.github.sculkhorde.core.ModMobEffects;
import com.github.sculkhorde.util.EntityAlgorithms;
import com.github.sculkhorde.util.TickUnits;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class BladeOfPurityItem extends SwordItem {

    public BladeOfPurityItem() {
        this(Tiers.DIAMOND, 4, -3F, new Properties().rarity(Rarity.EPIC).setNoRepair().durability(1561));
    }
    public BladeOfPurityItem(Tier tier, int baseDamage, float baseAttackSpeed, Properties prop) {
        super(tier, prop.attributes(SwordItem.createAttributes(tier, baseDamage, baseAttackSpeed)));
    }



    // 净化游标的冷却(10秒)与击杀回耐久量(可调)
    private static final int PURIFY_COOLDOWN_TICKS = 200; // 10s
    private static final int REPAIR_PER_SCULK_KILL = 2;
    private static final String LAST_PURIFY_KEY = "lastPurifyTime";

    private static long getLastPurifyTime(ItemStack stack) {
        return stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag().getLong(LAST_PURIFY_KEY);
    }
    private static void setLastPurifyTime(ItemStack stack, long time) {
        CompoundTag tag = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        tag.putLong(LAST_PURIFY_KEY, time);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
    }

    @Override
    public boolean hurtEnemy(ItemStack itemStack, LivingEntity targetEntity, LivingEntity ownerEntity) {
        if(!ownerEntity.level().isClientSide() && EntityAlgorithms.isSculkLivingEntity.test(targetEntity))
        {
            // 击杀幽匿生物恢复耐久
            if(targetEntity.isDeadOrDying())
            {
                itemStack.setDamageValue(Math.max(0, itemStack.getDamageValue() - REPAIR_PER_SCULK_KILL));
            }

            // 净化: 10 秒冷却, 冷却就绪时命中幽匿生物只生成 1 个净化游标(存活 10 秒), 避免群体刷游标导致卡顿
            long now = ownerEntity.level().getGameTime();
            if(now - getLastPurifyTime(itemStack) >= PURIFY_COOLDOWN_TICKS)
            {
                setLastPurifyTime(itemStack, now);

                CursorSurfacePurifierEntity purifier = new CursorSurfacePurifierEntity(ownerEntity.level());
                purifier.setPos(targetEntity.position());
                purifier.setMaxTransformations(10);
                purifier.setTickIntervalMilliseconds(10);
                purifier.setMaxLifeTimeMillis(TimeUnit.SECONDS.toMillis(10));
                purifier.setMaxRange(32);
                ownerEntity.level().addFreshEntity(purifier);

                targetEntity.addEffect(new MobEffectInstance(ModMobEffects.PURITY, TickUnits.convertSecondsToTicks(10), 0));
            }
        }

        return true;
    }
    

    @Override
    public @NotNull AABB getSweepHitBox(@NotNull ItemStack stack, @NotNull Player player, @NotNull Entity target)
    {
        return target.getBoundingBox().inflate(3.0D, 0.25D, 3.0D);
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack stack, Item.TooltipContext worldIn, List<Component> tooltip, TooltipFlag flagIn) {
        if(InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), GLFW.GLFW_KEY_LEFT_SHIFT))
        {
            tooltip.add(Component.translatable("tooltip.sculkhorde.blade_of_purity.functionality"));
        }
        else if(InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), GLFW.GLFW_KEY_LEFT_CONTROL))
        {
            tooltip.add(Component.translatable("tooltip.sculkhorde.blade_of_purity.lore"));
        }
        else
        {
            tooltip.add(Component.translatable("tooltip.sculkhorde.default"));
        }
    }
}
