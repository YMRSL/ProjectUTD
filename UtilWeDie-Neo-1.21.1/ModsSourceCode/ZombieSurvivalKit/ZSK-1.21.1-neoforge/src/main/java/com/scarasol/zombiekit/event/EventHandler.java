package com.scarasol.zombiekit.event;

import com.mojang.blaze3d.vertex.PoseStack;
import com.scarasol.sona.init.SonaMobEffects;
import com.scarasol.sona.manager.RustManager;
import com.scarasol.sona.util.SonaRenderer;
import com.scarasol.zombiekit.ZombieKitMod;
import com.scarasol.zombiekit.block.InjectorBlock;
import com.scarasol.zombiekit.block.ShortwaveRadioBlock;
import com.scarasol.zombiekit.client.renderer.FlameThrowerRenderer;
import com.scarasol.zombiekit.compat.SBWCompat;
import com.scarasol.zombiekit.config.CommonConfig;
import com.scarasol.zombiekit.entity.ai.goal.FlamethrowerUsingGoal;
import com.scarasol.zombiekit.entity.ai.goal.HeavyMachineGunUsingGoal;
import com.scarasol.zombiekit.entity.mechanics.HeavyMachineGunEntity;
import com.scarasol.zombiekit.entity.mechanics.MortarEntity;
import com.scarasol.zombiekit.entity.mechanics.UvLampEntity;
import com.scarasol.zombiekit.init.ZombieKitItems;
import com.scarasol.zombiekit.init.ZombieKitTags;
import com.scarasol.zombiekit.item.armor.BombArmor;
import com.scarasol.zombiekit.item.armor.ExoArmor;
import com.scarasol.zombiekit.item.weapon.Flamethrower;
import com.scarasol.zombiekit.network.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import software.bernie.geckolib.event.GeoRenderEvent;

import java.util.Random;


@EventBusSubscriber(modid = ZombieKitMod.MODID)
public class EventHandler {

    @SubscribeEvent
    public static void livingEquipmentChangeEvent(LivingEquipmentChangeEvent event) {
        if (event.getSlot() == EquipmentSlot.MAINHAND || event.getSlot() == EquipmentSlot.OFFHAND)
            return;
        if (event.getFrom().getItem() instanceof BombArmor || event.getTo().getItem() instanceof BombArmor) {
            BombArmor.updateModifier(event.getEntity());
        }
        if (event.getFrom().getItem() instanceof ExoArmor || event.getTo().getItem() instanceof ExoArmor) {
            ExoArmor.updateModifier(event.getEntity());
        }
    }


    @SubscribeEvent
    public static void getAttack(LivingIncomingDamageEvent event) {
        Entity entity = event.getSource().getDirectEntity();
        if (entity != null) {
            event.setCanceled(ExoArmor.reactiveArmor(event.getEntity(), entity));
        }
    }

    @SubscribeEvent
    public static void onWorldLoad(LevelEvent.Load event) {
        ShortwaveRadioBlock.loadRadioString(event.getLevel());
        InjectorBlock.init();
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(EntityJoinLevelEvent event) {
        Level level = event.getEntity().level();
        if (!level.isClientSide() && event.getEntity() instanceof ServerPlayer player) {
            MapVariables mapData = MapVariables.get(level);
            if (mapData != null) {
                PacketDistributor.sendToPlayer(player, new SavedDataSyncPacket(mapData));
            }
        }
    }

    @SubscribeEvent
    public static void KnockbackEvent(LivingKnockBackEvent event) {
        LivingEntity livingEntity = event.getEntity();
        if (livingEntity.getPersistentData().getBoolean("CancelKnockback")) {
            livingEntity.getPersistentData().putBoolean("CancelKnockback", false);
            event.setStrength(event.getStrength() * 0.1f);
        }
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void switchAngleOfMortar(InputEvent.MouseScrollingEvent event) {
        if (Minecraft.getInstance().player.getVehicle() instanceof MortarEntity && Minecraft.getInstance().options.keySprint.isDown()) {
            PacketDistributor.sendToServer(new MouseInputPacket(2, event.getScrollDeltaY()));
            event.setCanceled(true);
        }
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void mouseInput(InputEvent.MouseButton.Pre event) {
        Player player = Minecraft.getInstance().player;
        if (event.getButton() == 0 && player != null && Minecraft.getInstance().screen == null) {
            if (!(player.hasEffect(SonaMobEffects.STUN) || (player.hasEffect(SonaMobEffects.SLIMINESS) && player.hasEffect(SonaMobEffects.FROST)))) {
                if (player.getVehicle() instanceof HeavyMachineGunEntity heavyMachineGunEntity) {
                    PacketDistributor.sendToServer(new MouseInputPacket(0, event.getAction()));
                    event.setCanceled(true);
                    heavyMachineGunEntity.setFire(event.getAction() == 1);
                } else if (player.getMainHandItem().getItem() instanceof Flamethrower) {
                    PacketDistributor.sendToServer(new MouseInputPacket(0, event.getAction()));
                    event.setCanceled(true);
                } else if (event.getAction() == 1 && BuiltInRegistries.ITEM.getKey(player.getMainHandItem().getItem()).toString().equals("superbwarfare:monitor")) {
                    if (Minecraft.getInstance().options.keySprint.isDown() && SBWCompat.droneCover(player, player.getMainHandItem(), player.level(), false) != null)
                        event.setCanceled(true);
                }
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void mouseInput(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_ENTITIES) {

            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.options.keySprint.isDown()) {
                Player player = minecraft.player;
                if (BuiltInRegistries.ITEM.getKey(player.getMainHandItem().getItem()).toString().equals("superbwarfare:monitor")) {
                    BlockPos blockPos = SBWCompat.droneCover(player, player.getMainHandItem(), player.level(), true);
                    if (blockPos != null) {
                        Vec3 camPos = minecraft.gameRenderer.getMainCamera().getPosition();
                        PoseStack poseStack = event.getPoseStack();
                        poseStack.pushPose();
                        MultiBufferSource multiBufferSource = minecraft.renderBuffers().bufferSource();
                        SonaRenderer.renderHalo(poseStack, multiBufferSource, blockPos, camPos, Direction.UP, 5.0F, 0xFF0000, 2);
                        SonaRenderer.renderHalo(poseStack, multiBufferSource, blockPos, camPos, Direction.UP, 2.0F, 0xFF0000, 2);
                        SonaRenderer.renderRotatingCross(poseStack, multiBufferSource, blockPos, camPos, Direction.UP, 5.0F, 1.0F, 0xFF0000, 2);
                        poseStack.popPose();
                    }

                }
            }


        }
    }
    public static boolean illagerWhiteList(Mob mob) {
        return com.scarasol.sona.configuration.CommonConfig.findIndex(BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).toString(), new java.util.ArrayList<String>(CommonConfig.ILLAGER_WHITELIST.get())) != -1;
    }

    @SubscribeEvent
    public static void onEntityJoined(EntityJoinLevelEvent event) {
        Entity entity = event.getEntity();
        if (entity == null)
            return;
        Level level = entity.level();
        if (entity instanceof Mob newSpawn) {
            if (newSpawn.getType().is(ZombieKitTags.MACHINE_GUNNER)) {
                if ((newSpawn instanceof Raider || illagerWhiteList(newSpawn)) && CommonConfig.RAIDER_INDEPENDENCE.get()) {
                    newSpawn.goalSelector.addGoal(1, new HeavyMachineGunUsingGoal<>(newSpawn, livingEntity -> livingEntity instanceof Mob mob && (mob.getType().is(ZombieKitTags.SURVIVORS) || livingEntity instanceof IronGolem || livingEntity instanceof AbstractVillager || (livingEntity instanceof Enemy && !(livingEntity instanceof Creeper || livingEntity instanceof NeutralMob || illagerWhiteList(mob)))), true));
                } else if (newSpawn instanceof Enemy) {
                    newSpawn.goalSelector.addGoal(1, new HeavyMachineGunUsingGoal<>(newSpawn, null, true));
                } else {
                    newSpawn.goalSelector.addGoal(1, new HeavyMachineGunUsingGoal<>(newSpawn, livingEntity -> livingEntity instanceof Enemy && !(livingEntity instanceof Creeper || livingEntity instanceof NeutralMob), false));
                }
            }
            if (newSpawn.getType().is(ZombieKitTags.FLAMETHROWER)) {
                newSpawn.goalSelector.addGoal(1, new FlamethrowerUsingGoal<>(newSpawn));
            }
            if ((newSpawn instanceof Raider || illagerWhiteList(newSpawn)) && CommonConfig.RAIDER_INDEPENDENCE.get()) {
                newSpawn.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(newSpawn, Mob.class, 5, false, false, livingEntity -> livingEntity instanceof Enemy && !(livingEntity instanceof Raider || livingEntity instanceof Creeper || livingEntity instanceof NeutralMob || (livingEntity instanceof Mob mob && illagerWhiteList(mob)))));
            } else if (newSpawn instanceof Enemy && !(newSpawn instanceof Creeper || newSpawn instanceof NeutralMob)) {
                if (newSpawn.getType().is(ZombieKitTags.UV_RESISTANCE))
                    newSpawn.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(newSpawn, Mob.class, 5, false, false, livingEntity -> livingEntity instanceof UvLampEntity));
                if (CommonConfig.RAIDER_INDEPENDENCE.get())
                    newSpawn.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(newSpawn, Mob.class, 5, false, false, livingEntity -> livingEntity instanceof Raider || (livingEntity instanceof Mob mob && illagerWhiteList(mob))));
                if (newSpawn instanceof Zombie && !(newSpawn instanceof ZombieVillager) && !event.loadedFromDisk()) {
                    if (Mth.nextInt(level.getRandom(), 1, 100) <= CommonConfig.EQUIPMENT_INITIALIZATION.get() * 100) {
                        double i = Mth.nextInt(level.getRandom(), 1, 4);
                        if (1 == i) {
                            ItemStack setStack = new ItemStack(ZombieKitItems.MACHETE.get());
                            setStack.setCount(1);
                            RustManager.putRust(setStack, 75);
                            newSpawn.setItemInHand(InteractionHand.MAIN_HAND, setStack);
                        } else if (2 == i) {
                            ItemStack setStack = new ItemStack(ZombieKitItems.CROWBAR.get());
                            setStack.setCount(1);
                            RustManager.putRust(setStack, 75);
                            newSpawn.setItemInHand(InteractionHand.MAIN_HAND, setStack);
                        } else if (3 == i) {
                            ItemStack setStack = new ItemStack(ZombieKitItems.FIRE_AXE.get());
                            setStack.setCount(1);
                            RustManager.putRust(setStack, 75);
                            newSpawn.setItemInHand(InteractionHand.MAIN_HAND, setStack);
                        } else {
                            ItemStack setStack = new ItemStack(ZombieKitItems.KNIFE.get());
                            setStack.setCount(1);
                            RustManager.putRust(setStack, 75);
                            newSpawn.setItemInHand(InteractionHand.MAIN_HAND, setStack);
                        }
                    }
                    if (Mth.nextInt(level.getRandom(), 1, 100) <= CommonConfig.BOMB_ARMOR_INITIALIZATION.get() * 100) {
                        if (Mth.nextInt(level.getRandom(), 1, 100) <= 40) {
                            newSpawn.setItemSlot(EquipmentSlot.FEET, new ItemStack(ZombieKitItems.BOMB_BOOTS.get()));
                        }
                        if (Mth.nextInt(level.getRandom(), 1, 100) <= 20) {
                            newSpawn.setItemSlot(EquipmentSlot.LEGS, new ItemStack(ZombieKitItems.BOMB_LEGGINGS.get()));
                        }
                        if (Mth.nextInt(level.getRandom(), 1, 100) <= 10) {
                            newSpawn.setItemSlot(EquipmentSlot.CHEST, new ItemStack(ZombieKitItems.BOMB_CHESTPLATE.get()));
                        }
                        if (Mth.nextInt(level.getRandom(), 1, 100) <= 30) {
                            newSpawn.setItemSlot(EquipmentSlot.HEAD, new ItemStack(ZombieKitItems.BOMB_HELMET.get()));
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onEntitySpawned(MobSpawnEvent.PositionCheck event) {
        LivingEntity newSpawn = event.getEntity();
        if (newSpawn instanceof Enemy && !(newSpawn instanceof Vex) && event.getLevel() instanceof ServerLevel serverLevel) {
            StructureManager structureFeatureManager = serverLevel.structureManager();
            Structure configuredStructureFeature = structureFeatureManager.registryAccess().registryOrThrow(Registries.STRUCTURE).get(ZombieKitTags.STRUCTURE);
            if (configuredStructureFeature != null) {
                if ((event.getSpawnType() == MobSpawnType.NATURAL || event.getSpawnType() == MobSpawnType.STRUCTURE || event.getSpawnType() == MobSpawnType.REINFORCEMENT) && structureFeatureManager.getStructureAt(BlockPos.containing(event.getX(), event.getY(), event.getZ()), configuredStructureFeature).isValid()) {
                    if (!(newSpawn instanceof Raider || newSpawn instanceof HeavyMachineGunEntity)) {
                        event.setResult(MobSpawnEvent.PositionCheck.Result.FAIL);
                        event.getEntity().discard();
                    }
                    BlockState state = serverLevel.getBlockState(BlockPos.containing(event.getX(), event.getY(), event.getZ()).below());
                    if (state.getBlock() == Blocks.SMOOTH_STONE || state.getBlock() == Blocks.STONE) {
                        PatrollingMonster patrollingMonster;
                        for (int i = 0; i < new Random().nextInt(3) + 1; i++) {
                            if (serverLevel.getRandom().nextDouble() < 0.5) {
                                patrollingMonster = EntityType.PILLAGER.create(serverLevel);
                            } else {
                                patrollingMonster = EntityType.VINDICATOR.create(serverLevel);
                            }
                            patrollingMonster.setPos(event.getX(), event.getY(), event.getZ());
                            patrollingMonster.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(BlockPos.containing(event.getX(), event.getY(), event.getZ())), MobSpawnType.EVENT, null);
                            serverLevel.addFreshEntity(patrollingMonster);
                        }
                    }

                }
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (ExoArmor.numberOfSuit(player) == 4 && player.isFallFlying() && player.isSprinting()) {
            Vec3 vec31 = player.getLookAngle();
            Vec3 vec32 = player.getDeltaMovement();
            player.setDeltaMovement(vec32.add(vec31.x * 0.1D + (vec31.x * 1.5D - vec32.x) * 0.5D, vec31.y * 0.1D + (vec31.y * 1.5D - vec32.y) * 0.5D, vec31.z * 0.1D + (vec31.z * 1.5D - vec32.z) * 0.5D));
        }
    }

    @SubscribeEvent
    public static void projectileHit(ProjectileImpactEvent event) {
        if (event.getRayTraceResult() instanceof EntityHitResult entityHitResult) {
            Entity target = entityHitResult.getEntity();
            if (target instanceof LivingEntity livingEntity && ExoArmor.reactiveArmor(livingEntity, event.getProjectile())) {
                event.setCanceled(true);
            }
        }

    }

    @OnlyIn(Dist.CLIENT)
    @SubscribeEvent
    public static void flameThrowerRender(GeoRenderEvent.Item.Pre event) {
        ItemStack itemStack = event.getItemStack();
        if (FlameThrowerRenderer.transformType != null && FlameThrowerRenderer.transformType.firstPerson())
            event.setCanceled(Minecraft.getInstance().player.getOffhandItem() == itemStack);
    }

}
