package com.scarasol.zombiekit.item.weapon;

import com.mojang.blaze3d.vertex.PoseStack;
import com.scarasol.sona.util.SonaMath;
import com.scarasol.zombiekit.ZombieKitMod;
import com.scarasol.zombiekit.client.model.ZombieKitGeoItemModel;
import com.scarasol.zombiekit.client.renderer.FlameThrowerRenderer;
import com.scarasol.zombiekit.config.CommonConfig;
import com.scarasol.zombiekit.init.ZombieKitItems;
import com.scarasol.zombiekit.init.ZombieKitSounds;
import com.scarasol.zombiekit.item.api.BaseFuelCanister;
import com.scarasol.zombiekit.item.api.BaseZombieKitGeoItem;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.AbstractIllager;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.NotNull;
import software.bernie.example.registry.SoundRegistry;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.SingletonGeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.ClientUtils;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;

public class Flamethrower extends Item implements BaseZombieKitGeoItem {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private static final RawAnimation RELOAD = RawAnimation.begin().thenPlay("reload");
    private static final RawAnimation RELOAD_EMPTY = RawAnimation.begin().thenPlay("reload_empty");
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");


    public Flamethrower(Properties properties) {
        super(properties);
        SingletonGeoAnimatable.registerSyncedAnimatable(this);
    }

//    @Override
//    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
//        ItemStack itemStack = player.getItemInHand(hand);
//        if (level.getGameTime() - getReloadTime(itemStack) > 47)
//            player.startUsingItem(hand);
//        return super.use(level, player, hand);
//    }

    @Override
    public void onUseTick(Level level, LivingEntity player, ItemStack itemStack, int tick) {
        if (getPressure(itemStack) <= 0)
            return;
        Vec3 lookAngle = player.getViewVector(1).scale(1.5);
        Vec3 position = player.getHandHoldingItemAngle(ZombieKitItems.FLAMETHROWER.get()).yRot(-20).add(player.getEyePosition()).add(lookAngle);
        double x = position.x();
        double y = position.y();
        double z = position.z();
        if (level instanceof ServerLevel serverLevel) {
            putPressure(itemStack, Math.max(getPressure(itemStack) - CommonConfig.FUEL_CONSUME.get(), 0));
            Vec3 center = player.getOnPos().getCenter();
            double range = Math.min(tick * 1.5, 18D);
            serverLevel.getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(range), e -> shouldBeBurned(player, e))
                    .forEach(target -> burn(target, itemStack));
            BaseFuelCanister fuelCanister = getCanister(itemStack);
            if (fuelCanister != null) {
                serverLevel.getPlayers(e -> e.distanceTo(player) < 60)
                        .forEach(serverPlayer -> serverLevel.sendParticles(serverPlayer, (SimpleParticleType) fuelCanister.getParticleType(), true, x, y, z, 0, lookAngle.x, lookAngle.y, lookAngle.z, 1));
            }
            burnBlock(player, range);
            serverLevel.playSound(null, player.getOnPos(), ZombieKitSounds.flamethrower.get(), SoundSource.PLAYERS, 0.5f, 1);
        }
    }

    public boolean shouldBeBurned(LivingEntity attacker, LivingEntity target) {
        if (attacker instanceof AbstractIllager && target instanceof AbstractIllager)
            return false;
        String id = ForgeRegistries.ENTITY_TYPES.getKey(attacker.getType()).toString();
        if (id.contains("recruits") || id.contains("guardvillagers")) {
            String targetId = ForgeRegistries.ENTITY_TYPES.getKey(target.getType()).toString();
            if (target instanceof AbstractVillager || targetId.contains("recruits") || targetId.contains("guardvillagers"))
                return false;
        }
        if (target.equals(attacker))
            return false;
        if (!target.hasLineOfSight(attacker))
            return false;
        Vec3 position = attacker.position();
        Vec3 lookAngle = attacker.getViewVector(1);
        Vec3 vec3 = target.position().subtract(position);

        return SonaMath.vectorDegreeCalculate(lookAngle, vec3) < 10;
    }

    private void burn(LivingEntity target, ItemStack itemStack) {
        BaseFuelCanister canister = getCanister(itemStack);
        if (canister != null)
            canister.canisterEffect(target);
    }

    public void reload(Player player, Level level, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);
        List<ItemStack> list = player.getInventory().items.stream().filter((stack) -> stack.getItem() instanceof BaseFuelCanister).toList();
        boolean flag = false;
        BaseFuelCanister baseFuelCanister = getCanister(itemStack);
        if (!list.isEmpty()) {
            ItemStack canister = list.get(0);
            putCanister(itemStack, canister);
            if (!player.isCreative())
                canister.shrink(1);
            flag = true;
        } else if (player.isCreative()) {
            ItemStack canister = baseFuelCanister == null ? new ItemStack(ZombieKitItems.FUEL_CANISTER.get()) : new ItemStack((Item) baseFuelCanister);
            putCanister(itemStack, canister);
            flag = true;
        }
        if (flag && level instanceof ServerLevel serverLevel) {
            putReloadTime(itemStack, serverLevel.getGameTime());
            String anime = baseFuelCanister == null ? "reload_empty" : "reload";
            SoundEvent soundEvent = baseFuelCanister == null ? ZombieKitSounds.flamethrower_reload_empty.get() : ZombieKitSounds.flamethrower_reload.get();
            serverLevel.playSound(null, player, soundEvent, SoundSource.PLAYERS, 1, 1);
            triggerAnim(player, GeoItem.getOrAssignId(itemStack, serverLevel), "procedureController", anime);
        }
    }

    @Override
    public void onInventoryTick(ItemStack stack, Level level, Player player, int slotIndex, int selectedIndex) {
        if (level.getGameTime() - getReloadTime(stack) > 21) {
            changeTexture(stack);
        }
        if (getUsing(stack) && selectedIndex == slotIndex) {
            onUseTick(level, player, stack, (int) (level.getGameTime() - getStartUsingTime(stack)));
        }
        super.onInventoryTick(stack, level, player, slotIndex, selectedIndex);
    }

    public void changeTexture(ItemStack itemStack) {
        BaseFuelCanister baseFuelCanister = getCanister(itemStack);
        if (baseFuelCanister != null) {
            String newTexture = baseFuelCanister.getTexture();
            if (!newTexture.equals(getCurrentTexture(itemStack)))
            putCurrentTexture(itemStack, newTexture);
        }else {
            if (!"flamethrower_empty".equals(getCurrentTexture(itemStack)))
                putCurrentTexture(itemStack, "flamethrower_empty");
        }
    }

    public void putStartUsingTick(ItemStack itemStack, long time) {
        itemStack.getOrCreateTag().putLong("StartUsingTime", time);
    }

    public long getStartUsingTime(ItemStack itemStack) {
        if (itemStack.hasTag())
            return itemStack.getTag().getLong("StartUsingTime");
        return 0;
    }

    public void putUsing(ItemStack itemStack, boolean using) {
        itemStack.getOrCreateTag().putBoolean("Using", using);
    }

    public boolean getUsing(ItemStack itemStack) {
        if (itemStack.hasTag())
            return itemStack.getTag().getBoolean("Using");
        return false;
    }

    public void putCurrentTexture(ItemStack itemStack, String currentTexture) {
        itemStack.getOrCreateTag().putString("CurrentTexture", currentTexture);
    }

    public String getCurrentTexture(ItemStack itemStack) {
        if (itemStack.hasTag())
            return "".equals(itemStack.getTag().getString("CurrentTexture")) ? "flamethrower_empty" : itemStack.getTag().getString("CurrentTexture");
        return "flamethrower_empty";
    }

    public void putReloadTime(ItemStack itemStack, long reloadTime) {
        itemStack.getOrCreateTag().putLong("ReloadTime", reloadTime);
    }

    public long getReloadTime(ItemStack itemStack) {
        if (itemStack.hasTag())
            return itemStack.getTag().getLong("ReloadTime");
        return 0;
    }

    @Nullable
    public BaseFuelCanister getCanister(ItemStack itemStack) {
        if (itemStack.hasTag()) {
            String canisterStr = itemStack.getTag().getString("Canister");
            Item canister = ForgeRegistries.ITEMS.getValue(new ResourceLocation(canisterStr));
            if (canister instanceof BaseFuelCanister baseFuelCanister) {
                return baseFuelCanister;
            }
        }
        return null;
    }

    public double getPressure(ItemStack itemStack) {
        if (itemStack.hasTag()) {
            return itemStack.getTag().getDouble("Pressure");
        }
        return 0;
    }

    public void putPressure(ItemStack itemStack, double pressure) {
        itemStack.getOrCreateTag().putDouble("Pressure", pressure);
    }

    public void putCanister(ItemStack itemStack, ItemStack canister) {
        if (canister.isEmpty()) {
            itemStack.getOrCreateTag().putString("Canister", "");
            itemStack.getOrCreateTag().putDouble("Pressure", 0);
        }
        itemStack.getOrCreateTag().putString("Canister", ForgeRegistries.ITEMS.getKey(canister.getItem()).toString());
        itemStack.getOrCreateTag().putDouble("Pressure", canister.getMaxDamage() - canister.getDamageValue());
    }

    public void burnBlock(LivingEntity player, double range) {
        Vec3 lookAngle = player.getViewVector(1);
        Vec3 position = player.position().add(0, player.getEyeHeight(), 0);
        Level level = player.level();
        for (int i = 0; i < range; i++) {
            position = position.add(lookAngle);
            BlockPos pos = BlockPos.containing(position);
            BlockState state = level.getBlockState(pos);
            if (!state.isAir()) {
                if (state.hasProperty(BlockStateProperties.LIT)) {
                    level.setBlock(pos, state.setValue(BlockStateProperties.LIT, true), 3);
                } else {
                    BlockPos firePos = BlockPos.containing(position.subtract(lookAngle));
                    if (level.getBlockState(firePos).isAir() && BaseFireBlock.getState(level, firePos).canSurvive(level, firePos)) {
                        BlockState fire = BaseFireBlock.getState(level, firePos);
                        level.setBlock(firePos, fire, 11);
                        level.gameEvent(player, GameEvent.BLOCK_PLACE, pos);
                    }
                }
                break;
            }
        }
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged;
    }

    @Override
    public @NotNull UseAnim getUseAnimation(@NotNull ItemStack itemstack) {
        return UseAnim.NONE;
    }

    @Override
    public int getUseDuration(ItemStack itemstack) {
        return 72000;
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        super.initializeClient(consumer);
        consumer.accept(new IClientItemExtensions() {
            private final BlockEntityWithoutLevelRenderer renderer = new FlameThrowerRenderer(new ZombieKitGeoItemModel<Flamethrower>());
            public boolean applyForgeHandTransform(PoseStack poseStack, LocalPlayer player, HumanoidArm arm, ItemStack itemInHand, float partialTick, float equipProcess, float swingProcess) {
                if (itemInHand == player.getUseItem()) {
                    int i = arm == HumanoidArm.RIGHT ? 1 : -1;
                    poseStack.translate(i * 0.56F, -0.52F, -0.72F);
                    return true;
                }
                return false;
            }

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return renderer;
            }
        });
    }

    private PlayState procedurePredicate(AnimationState<Flamethrower> event) {
        AnimationController<Flamethrower> controller = event.getController();
        if (!controller.isPlayingTriggeredAnimation()) {
            if (controller.getCurrentRawAnimation() == null || event.getController().hasAnimationFinished()) {
                controller.setAnimation(IDLE);
            }
        }
        return PlayState.CONTINUE;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar data) {
        data.add(new AnimationController<>(this, "procedureController", 0, this::procedurePredicate)
                .triggerableAnim("reload", RELOAD)
                .triggerableAnim("reload_empty",RELOAD_EMPTY)
                .receiveTriggeredAnimations());
    }


    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Override
    public ResourceLocation getTexture() {
        return new ResourceLocation(ZombieKitMod.MODID, "textures/item/flamethrower.png");
    }

    @Override
    public ResourceLocation getModel() {
        if (FlameThrowerRenderer.transformType.firstPerson())
            return new ResourceLocation(ZombieKitMod.MODID, "geo/flamethrower.geo.json");
        return new ResourceLocation(ZombieKitMod.MODID, "geo/flamethrower_third.geo.json");
    }

    public ResourceLocation getTexture(ItemStack stack) {
        return new ResourceLocation(ZombieKitMod.MODID, "textures/item/" + getCurrentTexture(stack) + ".png");
    }


}
