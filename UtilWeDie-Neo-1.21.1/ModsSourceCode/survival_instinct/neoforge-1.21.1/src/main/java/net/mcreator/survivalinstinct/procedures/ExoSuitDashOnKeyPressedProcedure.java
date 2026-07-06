package net.mcreator.survivalinstinct.procedures;

import net.mcreator.survivalinstinct.init.SurvivalInstinctModItems;
import net.mcreator.survivalinstinct.init.SurvivalInstinctModMobEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;

public class ExoSuitDashOnKeyPressedProcedure {
    public static void execute(LevelAccessor world, double x, double y, double z, Entity entity) {
        block43: {
            LivingEntity _livEnt32;
            block46: {
                ItemStack itemStack;
                ItemStack itemStack2;
                ItemStack itemStack3;
                ItemStack itemStack4;
                block45: {
                    ItemStack itemStack5;
                    ItemStack itemStack6;
                    ItemStack itemStack7;
                    ItemStack itemStack8;
                    block44: {
                        ItemStack itemStack9;
                        ItemStack itemStack10;
                        ItemStack itemStack11;
                        ItemStack itemStack12;
                        block42: {
                            ItemStack itemStack13;
                            ItemStack itemStack14;
                            ItemStack itemStack15;
                            ItemStack itemStack16;
                            if (entity == null) {
                                return;
                            }
                            if (entity instanceof LivingEntity) {
                                LivingEntity _entGetArmor = (LivingEntity)entity;
                                itemStack16 = _entGetArmor.getItemBySlot(EquipmentSlot.FEET);
                            } else {
                                itemStack16 = ItemStack.EMPTY;
                            }
                            if (itemStack16.getItem() == SurvivalInstinctModItems.EXO_HEAVY_DESERT_BOOTS.get()) break block42;
                            if (entity instanceof LivingEntity) {
                                LivingEntity _entGetArmor = (LivingEntity)entity;
                                itemStack15 = _entGetArmor.getItemBySlot(EquipmentSlot.FEET);
                            } else {
                                itemStack15 = ItemStack.EMPTY;
                            }
                            if (itemStack15.getItem() == SurvivalInstinctModItems.EXO_HEAVY_BLACK_BOOTS.get()) break block42;
                            if (entity instanceof LivingEntity) {
                                LivingEntity _entGetArmor = (LivingEntity)entity;
                                itemStack14 = _entGetArmor.getItemBySlot(EquipmentSlot.FEET);
                            } else {
                                itemStack14 = ItemStack.EMPTY;
                            }
                            if (itemStack14.getItem() == SurvivalInstinctModItems.EXO_HEAVY_GREEN_BOOTS.get()) break block42;
                            if (entity instanceof LivingEntity) {
                                LivingEntity _entGetArmor = (LivingEntity)entity;
                                itemStack13 = _entGetArmor.getItemBySlot(EquipmentSlot.FEET);
                            } else {
                                itemStack13 = ItemStack.EMPTY;
                            }
                            if (itemStack13.getItem() != SurvivalInstinctModItems.EXO_BOOTS.get()) break block43;
                        }
                        if (entity instanceof LivingEntity) {
                            LivingEntity _entGetArmor = (LivingEntity)entity;
                            itemStack12 = _entGetArmor.getItemBySlot(EquipmentSlot.LEGS);
                        } else {
                            itemStack12 = ItemStack.EMPTY;
                        }
                        if (itemStack12.getItem() == SurvivalInstinctModItems.EXO_HEAVY_DESERT_LEGGINGS.get()) break block44;
                        if (entity instanceof LivingEntity) {
                            LivingEntity _entGetArmor = (LivingEntity)entity;
                            itemStack11 = _entGetArmor.getItemBySlot(EquipmentSlot.LEGS);
                        } else {
                            itemStack11 = ItemStack.EMPTY;
                        }
                        if (itemStack11.getItem() == SurvivalInstinctModItems.EXO_HEAVY_BLACK_LEGGINGS.get()) break block44;
                        if (entity instanceof LivingEntity) {
                            LivingEntity _entGetArmor = (LivingEntity)entity;
                            itemStack10 = _entGetArmor.getItemBySlot(EquipmentSlot.LEGS);
                        } else {
                            itemStack10 = ItemStack.EMPTY;
                        }
                        if (itemStack10.getItem() == SurvivalInstinctModItems.EXO_LEGGINGS.get()) break block44;
                        if (entity instanceof LivingEntity) {
                            LivingEntity _entGetArmor = (LivingEntity)entity;
                            itemStack9 = _entGetArmor.getItemBySlot(EquipmentSlot.LEGS);
                        } else {
                            itemStack9 = ItemStack.EMPTY;
                        }
                        if (itemStack9.getItem() != SurvivalInstinctModItems.EXO_HEAVY_GREEN_LEGGINGS.get()) break block43;
                    }
                    if (entity instanceof LivingEntity) {
                        LivingEntity _entGetArmor = (LivingEntity)entity;
                        itemStack8 = _entGetArmor.getItemBySlot(EquipmentSlot.CHEST);
                    } else {
                        itemStack8 = ItemStack.EMPTY;
                    }
                    if (itemStack8.getItem() == SurvivalInstinctModItems.EXO_HEAVY_DESERT_CHESTPLATE.get()) break block45;
                    if (entity instanceof LivingEntity) {
                        LivingEntity _entGetArmor = (LivingEntity)entity;
                        itemStack7 = _entGetArmor.getItemBySlot(EquipmentSlot.CHEST);
                    } else {
                        itemStack7 = ItemStack.EMPTY;
                    }
                    if (itemStack7.getItem() == SurvivalInstinctModItems.EXO_HEAVY_BLACK_CHESTPLATE.get()) break block45;
                    if (entity instanceof LivingEntity) {
                        LivingEntity _entGetArmor = (LivingEntity)entity;
                        itemStack6 = _entGetArmor.getItemBySlot(EquipmentSlot.CHEST);
                    } else {
                        itemStack6 = ItemStack.EMPTY;
                    }
                    if (itemStack6.getItem() == SurvivalInstinctModItems.EXO_CHESTPLATE.get()) break block45;
                    if (entity instanceof LivingEntity) {
                        LivingEntity _entGetArmor = (LivingEntity)entity;
                        itemStack5 = _entGetArmor.getItemBySlot(EquipmentSlot.CHEST);
                    } else {
                        itemStack5 = ItemStack.EMPTY;
                    }
                    if (itemStack5.getItem() != SurvivalInstinctModItems.EXO_HEAVY_GREEN_CHESTPLATE.get()) break block43;
                }
                if (entity instanceof LivingEntity) {
                    LivingEntity _entGetArmor = (LivingEntity)entity;
                    itemStack4 = _entGetArmor.getItemBySlot(EquipmentSlot.HEAD);
                } else {
                    itemStack4 = ItemStack.EMPTY;
                }
                if (itemStack4.getItem() == SurvivalInstinctModItems.EXO_HEAVY_DESERT_HELMET.get()) break block46;
                if (entity instanceof LivingEntity) {
                    LivingEntity _entGetArmor = (LivingEntity)entity;
                    itemStack3 = _entGetArmor.getItemBySlot(EquipmentSlot.HEAD);
                } else {
                    itemStack3 = ItemStack.EMPTY;
                }
                if (itemStack3.getItem() == SurvivalInstinctModItems.EXO_HEAVY_GREEN_HELMET.get()) break block46;
                if (entity instanceof LivingEntity) {
                    LivingEntity _entGetArmor = (LivingEntity)entity;
                    itemStack2 = _entGetArmor.getItemBySlot(EquipmentSlot.HEAD);
                } else {
                    itemStack2 = ItemStack.EMPTY;
                }
                if (itemStack2.getItem() == SurvivalInstinctModItems.EXO_HELMET.get()) break block46;
                if (entity instanceof LivingEntity) {
                    LivingEntity _entGetArmor = (LivingEntity)entity;
                    itemStack = _entGetArmor.getItemBySlot(EquipmentSlot.HEAD);
                } else {
                    itemStack = ItemStack.EMPTY;
                }
                if (itemStack.getItem() != SurvivalInstinctModItems.EXO_HEAVY_BLACK_HELMET.get()) break block43;
            }
            if (!(entity instanceof LivingEntity) || !(_livEnt32 = (LivingEntity)entity).hasEffect(SurvivalInstinctModMobEffects.DASH_ON_COOLDOWN)) {
                LivingEntity _entity;
                Level _level;
                if (world instanceof Level) {
                    _level = (Level)world;
                    if (!_level.isClientSide()) {
                        _level.playSound(null, BlockPos.containing((double)x, (double)y, (double)z), (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("survival_instinct:exo_dash_02")), SoundSource.PLAYERS, 1.0f, 1.0f);
                    } else {
                        _level.playLocalSound(x, y, z, (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("survival_instinct:exo_dash_02")), SoundSource.PLAYERS, 1.0f, 1.0f, false);
                    }
                }
                if (entity instanceof LivingEntity && !(_entity = (LivingEntity)entity).level().isClientSide()) {
                    _entity.addEffect(new MobEffectInstance(SurvivalInstinctModMobEffects.DASH_ON_COOLDOWN, 45, 0, true, false));
                }
                entity.setDeltaMovement(new Vec3(entity.getDeltaMovement().x() * 2.5 + entity.getLookAngle().x * 1.25, entity.getDeltaMovement().y() + 0.5 + entity.getLookAngle().y * 0.15, entity.getDeltaMovement().z() * 2.5 + entity.getLookAngle().z * 1.25));
                if (world instanceof ServerLevel) {
                    _level = (ServerLevel)world;
                    ((ServerLevel)_level).sendParticles((ParticleOptions)ParticleTypes.POOF, x, y, z, 5, 0.2, 0.2, 0.2, 0.5);
                }
            }
        }
    }
}

