package com.github.sculkhorde.common.item;

import com.github.sculkhorde.common.entity.boss.sculk_enderman.SculkSpineSpikeAttackEntity;
import com.github.sculkhorde.util.EntityAlgorithms;
import com.github.sculkhorde.util.SoundUtil;
import com.github.sculkhorde.util.TickUnits;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.glfw.GLFW;

import java.util.List;

public class SculkSweeperSword extends SwordItem {

    public SculkSweeperSword() {
        this(Tiers.DIAMOND, 3, -2.2F, new Item.Properties().rarity(Rarity.EPIC).setNoRepair().durability(100));
    }
    public SculkSweeperSword(Tier tier, int baseDamage, float baseAttackSpeed, Properties prop) {
        super(tier, prop.attributes(SwordItem.createAttributes(tier, baseDamage, baseAttackSpeed)));
    }

    // 击杀幽匿生物恢复的耐久量(可调)
    private static final int REPAIR_PER_SCULK_KILL = 2;
    // 右键技能冷却(类似盾牌被斧劈禁用的状态), 6 秒 = 120 tick
    private static final int SKILL_COOLDOWN_TICKS = 120;

    private void doSpikeAttack(LivingEntity ownerEntity, LivingEntity targetEntity, ItemStack itemStack)
    {
        AABB spikeHitbox = new AABB(targetEntity.blockPosition());
        spikeHitbox = spikeHitbox.inflate(20.0D);
        int spikesSummoned = 0;
        for(LivingEntity possibleSpikeTargets : targetEntity.level().getEntitiesOfClass(LivingEntity.class, spikeHitbox))
        {
            if(possibleSpikeTargets != ownerEntity)
            {
                boolean isSculkLivingEntity = EntityAlgorithms.isSculkLivingEntity.test(possibleSpikeTargets);
                if(isSculkLivingEntity)
                {
                    SculkSpineSpikeAttackEntity sculkSpineSpikeAttackEntity = new SculkSpineSpikeAttackEntity(ownerEntity, possibleSpikeTargets.getX(), possibleSpikeTargets.getY(), possibleSpikeTargets.getZ(), 0);
                    targetEntity.level().addFreshEntity(sculkSpineSpikeAttackEntity);
                    // Give effect
                    EntityAlgorithms.applyEffectToTarget(possibleSpikeTargets, MobEffects.LEVITATION, TickUnits.convertSecondsToTicks(5), 1);
                    spikesSummoned++;
                }
            }
        }

        // 耐久: 创造模式不消耗; 生存模式按召唤的尖刺数消耗(每根 1 点), 封顶留 1 点耐久不让技能直接打坏。
        boolean isCreative = ownerEntity instanceof Player p && p.getAbilities().instabuild;
        if(!isCreative && spikesSummoned > 0)
        {
            itemStack.setDamageValue(Math.min(itemStack.getMaxDamage() - 1, itemStack.getDamageValue() + spikesSummoned));
        }
    }



    @Override
    public boolean hurtEnemy(ItemStack itemStack, LivingEntity targetEntity, LivingEntity ownerEntity) {
        // 击杀幽匿生物恢复耐久
        if(EntityAlgorithms.isSculkLivingEntity.test(targetEntity) && targetEntity.isDeadOrDying())
        {
            itemStack.setDamageValue(Math.max(0, itemStack.getDamageValue() - REPAIR_PER_SCULK_KILL));
        }
        return true;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {

        ItemStack itemstack = player.getItemInHand(hand);
        // 冷却中 或 耐久耗尽 → 不放技能
        if(player.getCooldowns().isOnCooldown(itemstack.getItem()) || itemstack.getDamageValue() >= itemstack.getMaxDamage())
        {
            return InteractionResultHolder.pass(itemstack);
        }
        if(!level.isClientSide())
        {
            doSpikeAttack(player, player, itemstack);
            SoundUtil.playSoundInLevel(level, player.blockPosition(), SoundEvents.EVOKER_FANGS_ATTACK, player.getSoundSource() );
        }
        // 两端都加冷却(客户端立即显示遮罩, 服务端权威+同步)
        player.getCooldowns().addCooldown(itemstack.getItem(), SKILL_COOLDOWN_TICKS);
        return InteractionResultHolder.success(itemstack);
    }

    @Override
    public @NotNull AABB getSweepHitBox(@NotNull ItemStack stack, @NotNull Player player, @NotNull Entity target)
    {
        return target.getBoundingBox().inflate(3.0D, 0.25D, 3.0D);
    }

    public boolean onBlockStartBreak(ItemStack itemstack, BlockPos pos, Player player)
    {
        return true;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void appendHoverText(ItemStack stack, Item.TooltipContext worldIn, List<Component> tooltip, TooltipFlag flagIn) {
        if(InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), GLFW.GLFW_KEY_LEFT_SHIFT))
        {
            tooltip.add(Component.translatable("tooltip.sculkhorde.sculk_sweeper_sword.functionality"));
        }
        else if(InputConstants.isKeyDown(Minecraft.getInstance().getWindow().getWindow(), GLFW.GLFW_KEY_LEFT_CONTROL))
        {
            tooltip.add(Component.translatable("tooltip.sculkhorde.sculk_sweeper_sword.lore"));
        }
        else
        {
            tooltip.add(Component.translatable("tooltip.sculkhorde.default"));
        }
    }

    // Leave at 1f to prevent the sword from crashing the game when it is repaired
    @Override
    public float getXpRepairRatio(ItemStack stack)
    {
        return 1f;
    }

    public boolean isRepairable(@NotNull ItemStack stack)
    {
        return false;
    }

    public boolean canApplyAtEnchantingTable(ItemStack stack, Holder<Enchantment> enchantment)
    {
        if(enchantment.is(Enchantments.MENDING))
        {
            return false;
        }

        return true;
    }
}
