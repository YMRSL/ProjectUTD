package com.scarasol.zombiekit.item.armor;

import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.scarasol.sona.init.SonaMobEffects;
import com.scarasol.zombiekit.client.model.BombSuitModel;
import com.scarasol.zombiekit.client.model.ExoSuitModel;
import com.scarasol.zombiekit.client.model.TacticalSuitModel;
import com.scarasol.zombiekit.config.CommonConfig;
import com.scarasol.zombiekit.init.ZombieKitModels;
import com.scarasol.zombiekit.init.ZombieKitSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

public class ExoArmor extends ArmorItem {

    public static AttributeModifier COMBAT_MOVEMENT = new AttributeModifier(UUID.fromString("5987CE5F-1549-3D44-EA5E-33BA82D76A95"), "ExoCombat", 0.15, AttributeModifier.Operation.MULTIPLY_TOTAL);

    public final static Map<EquipmentSlot, HumanoidModel<LivingEntity>> ARMOR_MODEL = Maps.newHashMap();
    public static HumanoidModel<LivingEntity> ARMOR_MODEL_EMPTY;

    public ExoArmor(ArmorMaterial armorMaterial, Type equipmentSlot, Properties properties) {
        super(armorMaterial, equipmentSlot, properties);
    }

    @Override
    public void onInventoryTick(ItemStack itemStack, Level level, Player player, int slotIndex, int selectedIndex) {
        super.onInventoryTick(itemStack, level, player, slotIndex, selectedIndex);
        if (getEquipmentSlot() == EquipmentSlot.CHEST && Iterables.contains(player.getArmorSlots(), itemStack) && numberOfSuit(player) == 4) {
            if (player.level() instanceof ServerLevel serverLevel) {
                if (getPower(itemStack) > 0) {
                    fallProtect(itemStack, player, serverLevel);
                    modeFunction(itemStack, player, serverLevel);
                } else {
                    setReactiveArmor(itemStack, -1);
                    switchRadar(itemStack, 0);
                    switchMode(itemStack, 0);
                }
                if (!CommonConfig.FLY_FUNCTION.get())
                    setFlyMode(itemStack, false);
            }
        }
    }

    public void fallProtect(ItemStack itemStack, Player player, ServerLevel serverLevel) {
        if (!player.isInWaterOrBubble() && !player.isFallFlying() && player.fallDistance > 5 && !player.hasEffect(MobEffects.SLOW_FALLING)) {
            serverLevel.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY(), player.getZ(), 100, 0.5, 0.2, 0.5, 0.1);
            player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 200, 0, false, false));
            addPower(itemStack, -1);
        }
        if (!player.onGround() && player.hasEffect(MobEffects.SLOW_FALLING) && serverLevel.getGameTime() % 10 == 0) {
            serverLevel.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY(), player.getZ(), 5, 0, 0.2, 0, 0.05);
        }
    }

    public void modeFunction(ItemStack itemStack, Player player, ServerLevel serverLevel) {
        switch (getMode(itemStack)) {
            case 1 -> {
                if (!player.hasEffect(SonaMobEffects.EXPOSURE.get())) {
                    player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 20, 0, false, false));
                    player.addEffect(new MobEffectInstance(SonaMobEffects.CAMOUFLAGE.get(), 20, 4, false, false));
                    if (serverLevel.getGameTime() % 240 == 0)
                        addPower(itemStack, -1);
                } else {
                    switchMode(player, 1);
                }
            }
            case 2 -> {
                player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 20, 1, false, false));
                player.addEffect(new MobEffectInstance(MobEffects.JUMP, 20, 0, false, false));
                if (serverLevel.getGameTime() % 120 == 0)
                    addPower(itemStack, -1);
            }
        }
        int reactiveArmor = getReactiveArmor(itemStack);
        if (reactiveArmor >= 0) {
            addReactiveArmor(itemStack, 1);
            if (reactiveArmor >= 160) {
                if (serverLevel.getGameTime() % 60 == 0)
                    serverLevel.playSound(null, player, ZombieKitSounds.reactive_armor_ready.get(), SoundSource.PLAYERS, 1, 1);
                serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, player.getX(), player.getY() + 0.5, player.getZ(), 1, 0.2, 0.5, 0.2, 0.05);
            }
            if (serverLevel.getGameTime() % 120 == 0)
                addPower(itemStack, -1);
        }
        int radar = getRadar(itemStack);
        if (radar > 0) {
            if (radar == 1)
                player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 210, 0, false, false));
            if (serverLevel.getGameTime() % (480 / radar) == 0)
                addPower(itemStack, -1);
        }

    }

    @Override
    public boolean canElytraFly(ItemStack stack, LivingEntity entity) {
        return numberOfSuit(entity) == 4 && getFlyMode(stack) && getPower(stack) > 5 && CommonConfig.FLY_FUNCTION.get();
    }

    @Override
    public boolean elytraFlightTick(ItemStack stack, LivingEntity entity, int flightTicks) {
        if (numberOfSuit(entity) < 4 || !CommonConfig.FLY_FUNCTION.get() || !getFlyMode(stack))
            return false;
        if (entity.level() instanceof ServerLevel serverLevel) {
            if (flightTicks % 120 == 0)
                addPower(stack, -1);
            if (entity.isSprinting()) {
                if (flightTicks % 10 == 0)
                    serverLevel.playSound(null, entity, ZombieKitSounds.exo_fly.get(), SoundSource.PLAYERS, 1, 1);
                if (flightTicks % 2 == 0)
                    serverLevel.sendParticles(ParticleTypes.FIREWORK, entity.getX(), entity.getY(), entity.getZ(), 1, entity.getRandom().nextGaussian() * 0.05D, -entity.getDeltaMovement().y * 0.5D, entity.getRandom().nextGaussian() * 0.05D, 0.01);
            } else return !entity.isShiftKeyDown();
        }
        return true;
    }

    @Override
    public String getArmorTexture(ItemStack itemStack, Entity entity, EquipmentSlot slot, String type) {
        return "zombiekit:textures/entities/exo_suit.png";
    }

    @OnlyIn(Dist.CLIENT)
    public HumanoidModel getArmorModel(LivingEntity livingEntity) {
        if (ARMOR_MODEL_EMPTY == null) {
            ARMOR_MODEL_EMPTY = ZombieKitModels.getEmptyArmorModel();
        }
        if (!(livingEntity.isInvisible() && numberOfSuit(livingEntity) == 4 && getMode(livingEntity.getItemBySlot(EquipmentSlot.CHEST)) == 1)) {
            if (ARMOR_MODEL.isEmpty()) {
                for (EquipmentSlot equipmentSlot : EquipmentSlot.values()) {
                    if (equipmentSlot.isArmor()) {
                        ARMOR_MODEL.put(equipmentSlot, ZombieKitModels.getDefaultArmorModel(equipmentSlot, new ExoSuitModel<>(Minecraft.getInstance().getEntityModels().bakeLayer(ExoSuitModel.LAYER_LOCATION))));
                    }
                }
            }
            return ARMOR_MODEL.get(getEquipmentSlot());
        }
        return ARMOR_MODEL_EMPTY;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            @OnlyIn(Dist.CLIENT)
            public HumanoidModel getHumanoidArmorModel(LivingEntity living, ItemStack stack, EquipmentSlot slot, HumanoidModel defaultModel) {
                HumanoidModel armorModel = ExoArmor.this.getArmorModel(living);
                armorModel.crouching = living.isShiftKeyDown();
                armorModel.riding = defaultModel.riding;
                armorModel.young = living.isBaby();
                return armorModel;
            }
        });
    }

    public static int numberOfSuit(LivingEntity livingEntity) {
        int count = 0;
        for (ItemStack itemStack : livingEntity.getArmorSlots()) {
            if (itemStack.getItem() instanceof ExoArmor)
                count++;
        }
        return count;
    }

    public static void switchMode(Player livingEntity, int mode) {
        if (ExoArmor.numberOfSuit(livingEntity) < 4)
            return;
        ItemStack chest = livingEntity.getItemBySlot(EquipmentSlot.CHEST);
        if (getPower(chest) <= 0) {
            livingEntity.displayClientMessage(Component.translatable("item.zombiekit.exo.no_power"), true);
            return;
        }
        int currentMode = getMode(chest);
        if (mode == 3) {
            if (currentMode == 1) {
                livingEntity.displayClientMessage(Component.translatable("item.zombiekit.exo.no_reactive_armor"), true);
            } else {
                if (getReactiveArmor(chest) >= 0) {
                    livingEntity.displayClientMessage(Component.translatable("item.zombiekit.exo.reactive_armor_off"), true);
                    setReactiveArmor(chest, -1);
                } else {
                    livingEntity.displayClientMessage(Component.translatable("item.zombiekit.exo.reactive_armor_on"), true);
                    setReactiveArmor(chest, 0);
                }

            }
        } else if (mode == 4) {
            int radar = getRadar(chest);
            if (radar == 2) {
                livingEntity.displayClientMessage(Component.translatable("item.zombiekit.exo.radar_off"), true);
            } else if (radar == 1) {
                livingEntity.displayClientMessage(Component.translatable("item.zombiekit.exo.radar_on"), true);
                livingEntity.level().playSound(null, livingEntity, ZombieKitSounds.radar_activated.get(), SoundSource.PLAYERS, 1, 1);
                livingEntity.removeEffect(MobEffects.NIGHT_VISION);
            } else {
                livingEntity.displayClientMessage(Component.translatable("item.zombiekit.exo.radar_night_vision"), true);
                livingEntity.level().playSound(null, livingEntity, ZombieKitSounds.radar_activated.get(), SoundSource.PLAYERS, 1, 1);
            }
            switchRadar(chest, (radar + 1) % 3);
        } else if (mode == 5) {
            if (!CommonConfig.FLY_FUNCTION.get()) {
                livingEntity.displayClientMessage(Component.translatable("item.zombiekit.exo.forbidden_fly_mode"), true);
                return;
            }
            if (getMode(chest) == 1) {
                livingEntity.displayClientMessage(Component.translatable("item.zombiekit.exo.no_fly_mode"), true);
            } else {
                boolean flag = getFlyMode(chest);
                if (flag) {
                    livingEntity.displayClientMessage(Component.translatable("item.zombiekit.exo.fly_mode_off"), true);
                    setFlyMode(chest, false);
                } else {
                    livingEntity.displayClientMessage(Component.translatable("item.zombiekit.exo.fly_mode_on"), true);
                    setFlyMode(chest, true);
                }

            }
        } else {
            if (currentMode == mode) {
                switchMode(chest, 0);
                livingEntity.getAttribute(Attributes.MOVEMENT_SPEED).removeModifier(COMBAT_MOVEMENT);
                livingEntity.displayClientMessage(Component.translatable("item.zombiekit.exo.exit_mode"), true);
            } else {
                switchMode(chest, mode);
                if (mode == 1) {
                    setFlyMode(chest, false);
                    setReactiveArmor(chest, -1);
                    livingEntity.getAttribute(Attributes.MOVEMENT_SPEED).removeModifier(COMBAT_MOVEMENT);
                    livingEntity.displayClientMessage(Component.translatable("item.zombiekit.exo.sneak_mode"), true);
                } else if (mode == 2) {
                    livingEntity.getAttribute(Attributes.MOVEMENT_SPEED).removeModifier(COMBAT_MOVEMENT);
                    livingEntity.getAttribute(Attributes.MOVEMENT_SPEED).addPermanentModifier(COMBAT_MOVEMENT);
                    livingEntity.displayClientMessage(Component.translatable("item.zombiekit.exo.combat_mode"), true);
                }
            }
            livingEntity.level().playSound(null, livingEntity, ZombieKitSounds.mode_switch.get(), SoundSource.PLAYERS, 1, 1);
        }
    }

    public static boolean reactiveArmor(LivingEntity target, Entity attacker) {
        int count = numberOfSuit(target);
        if (count == 4 && getReactiveArmor(target.getItemBySlot(EquipmentSlot.CHEST)) >= 160) {
            Vec3 vec3;
            if (attacker instanceof Projectile projectile && projectile.getOwner() != null) {
                vec3 = new Vec3(projectile.getOwner().getX() - target.getX(), projectile.getOwner().getY() - target.getY(), projectile.getOwner().getZ() - target.getZ()).scale(1 / attacker.distanceTo(target));
            } else {
                vec3 = new Vec3(attacker.getX() - target.getX(), 0, attacker.getZ() - target.getZ()).scale(1.5);
            }
            attacker.setDeltaMovement(vec3);
            setReactiveArmor(target.getItemBySlot(EquipmentSlot.CHEST), 0);
            target.level().playSound(null, target, ZombieKitSounds.reactive_armor_release.get(), SoundSource.PLAYERS, 1, 1);
            if (attacker instanceof LivingEntity livingEntity)
                livingEntity.addEffect(new MobEffectInstance(SonaMobEffects.STUN.get(), 100, 0, false, false));
            return true;
        }
        return false;
    }

    public static void updateModifier(LivingEntity livingEntity) {
        int count = numberOfSuit(livingEntity);
        if (count < 4 || getMode(livingEntity.getItemBySlot(EquipmentSlot.CHEST)) != 2) {
            livingEntity.getAttribute(Attributes.MOVEMENT_SPEED).removeModifier(COMBAT_MOVEMENT);
        }
    }

    public static void addPower(ItemStack itemStack, int power) {
        int currentPower = itemStack.getOrCreateTag().getInt("Power");
        if (power > 0) {
            power = Math.min(currentPower + power, 100);
        } else {
            power = Math.max(currentPower + power, 0);
        }
        setPower(itemStack, power);
    }

    public static void setPower(ItemStack itemStack, int power) {
        itemStack.getOrCreateTag().putInt("Power", power);
    }

    public static void setFlyMode(ItemStack itemStack, boolean fly) {
        itemStack.getOrCreateTag().putBoolean("FlyMode", fly);
    }

    public static boolean getFlyMode(ItemStack itemStack) {
        return itemStack.getOrCreateTag().getBoolean("FlyMode");
    }

    public static int getPower(ItemStack itemStack) {
        return itemStack.getOrCreateTag().getInt("Power");
    }

    public static void switchMode(ItemStack itemStack, int mode) {
        itemStack.getOrCreateTag().putInt("Mode", mode);
    }

    public static int getMode(ItemStack itemStack) {
        return itemStack.getOrCreateTag().getInt("Mode");
    }

    public static int getRadar(ItemStack itemStack) {
        return itemStack.getOrCreateTag().getInt("Radar");
    }

    public static void switchRadar(ItemStack itemStack, int radar) {
        itemStack.getOrCreateTag().putInt("Radar", radar);
    }

    public static int getReactiveArmor(ItemStack itemStack) {
        return itemStack.getOrCreateTag().getInt("ReactiveArmor");
    }

    public static void setReactiveArmor(ItemStack itemStack, int reactiveArmor) {
        itemStack.getOrCreateTag().putInt("ReactiveArmor", reactiveArmor);
    }

    public static void addReactiveArmor(ItemStack itemStack, int reactiveArmor) {
        int coolDown = itemStack.getOrCreateTag().getInt("ReactiveArmor");
        if (reactiveArmor > 0) {
            reactiveArmor = Math.min(coolDown + reactiveArmor, 160);
        } else {
            reactiveArmor = Math.max(coolDown + reactiveArmor, 0);
        }
        setReactiveArmor(itemStack, reactiveArmor);
    }
}
