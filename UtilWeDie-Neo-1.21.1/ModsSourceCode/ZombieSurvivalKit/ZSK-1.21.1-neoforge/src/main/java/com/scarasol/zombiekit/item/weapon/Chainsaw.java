package com.scarasol.zombiekit.item.weapon;

import com.scarasol.zombiekit.config.CommonConfig;
import com.scarasol.zombiekit.init.ZombieKitDataComponents;
import com.scarasol.zombiekit.init.ZombieKitItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class Chainsaw extends Item {
    public Chainsaw(Properties properties) {
        super(properties);
    }

    private static boolean getPower(ItemStack stack) {
        return Boolean.TRUE.equals(stack.get(ZombieKitDataComponents.CHAINSAW_POWER.get()));
    }

    private static void setPower(ItemStack stack, boolean power) {
        stack.set(ZombieKitDataComponents.CHAINSAW_POWER.get(), power);
    }

    private static void setCustomModelData(ItemStack stack, int value) {
        stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(value));
    }

    @Override
    public void appendHoverText(ItemStack itemstack, TooltipContext context, List<Component> list, TooltipFlag flag) {
        super.appendHoverText(itemstack, context, list, flag);
        list.add(Component.translatable("item.zombiekit.chainsaw.description_1"));
        list.add(Component.translatable("item.zombiekit.chainsaw.description_2"));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        ItemStack itemStack = player.getItemInHand(hand);
        if (!world.isClientSide()) {
            if (!getPower(itemStack)){
                if (player.isShiftKeyDown()){
                    ItemStack itemOther;
                    if (hand == InteractionHand.MAIN_HAND){
                        itemOther = player.getItemInHand(InteractionHand.OFF_HAND);
                    }else {
                        itemOther = player.getItemInHand(InteractionHand.MAIN_HAND);
                    }
                    if (itemOther.is(ZombieKitItems.BATTERY.get())){
                        int damage = itemOther.getDamageValue();
                        itemOther.setDamageValue(itemStack.getDamageValue());
                        itemStack.setDamageValue(damage);
                        player.getCooldowns().addCooldown(itemStack.getItem(), 140);
                        return new InteractionResultHolder<>(InteractionResult.PASS, itemStack);
                    }
                }else if (itemStack.getDamageValue() >= 100){
                    world.playSound(null, player.getX(), player.getY(), player.getZ(), BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("zombiekit:chainsaw_start_failed")), SoundSource.PLAYERS, 1, 1);
                    return new InteractionResultHolder<>(InteractionResult.PASS, itemStack);
                }else {
                    setPower(itemStack, true);
                    setCustomModelData(itemStack, 1);
                }
            }else if (player.isShiftKeyDown()){
                setPower(itemStack, false);
                setCustomModelData(itemStack, 0);
            }else {
                player.startUsingItem(hand);
            }
        }
        return new InteractionResultHolder<>(InteractionResult.SUCCESS, itemStack);
    }

    @Override
    public void inventoryTick(ItemStack itemstack, Level world, Entity entity, int slot, boolean selected) {
        super.inventoryTick(itemstack, world, entity, slot, selected);
        if (entity instanceof LivingEntity livingEntity){
            ItemStack offHandItem = livingEntity.getOffhandItem();
            if ((selected || offHandItem == itemstack) && getPower(itemstack)){
                if (world.getGameTime() % 10 == 0){
                    if (livingEntity.isUsingItem() && livingEntity.getUseItem().is(ZombieKitItems.CHAINSAW.get())){
                        world.playSound(null, entity.getX(), entity.getY(), entity.getZ(), BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("zombiekit:chainsaw_attack")), SoundSource.PLAYERS, 0.6f, 1);
                    }else {
                        world.playSound(null, entity.getX(), entity.getY(), entity.getZ(), BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("zombiekit:chainsaw_idle")), SoundSource.PLAYERS, 0.3f, 1);
                    }
                }
                if (!world.isClientSide() && livingEntity instanceof Player player && !player.getAbilities().instabuild){
                    if (world.getGameTime() % CommonConfig.CHAINSAW_POWER.get() == 0) {
                        int newDamage = itemstack.getDamageValue() + 4;
                        if (newDamage >= itemstack.getMaxDamage()) {
                            itemstack.shrink(1);
                        } else {
                            itemstack.setDamageValue(newDamage);
                        }

                        if (itemstack.getDamageValue() >= 100){
                            setPower(itemstack, false);
                            setCustomModelData(itemstack, 0);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void onUseTick(Level level, LivingEntity livingEntity, ItemStack itemStack, int duration) {
        double x = livingEntity.getX();
        double y = livingEntity.getY();
        double z = livingEntity.getZ();
        Vec3 vec = livingEntity.getViewVector(1);
        Vec3 _center;
        if (vec.y < 0){
            _center = new Vec3(x + vec.x * 1.5, y + vec.y * 0.5, z + vec.z * 1.5);
        }else {
            _center = new Vec3(x + vec.x * 1.5, y + vec.y + 1, z + vec.z * 1.5);
        }
        List<Entity> _entfound = level.getEntitiesOfClass(Entity.class, new AABB(_center, _center).inflate(0.9d), e -> true).stream().sorted(Comparator.comparingDouble(_entcnd -> _entcnd.distanceToSqr(_center))).collect(Collectors.toList());
        for (Entity entityiterator : _entfound) {
            if (!livingEntity.equals(entityiterator)){
                entityiterator.invulnerableTime = 0;
                entityiterator.getPersistentData().putBoolean("CancelKnockback", true);
                entityiterator.hurt(livingEntity.level().damageSources().mobAttack(livingEntity), CommonConfig.CHAINSAW_DAMAGE.get().floatValue());
            }
        }
    }

    @Override
    public boolean isValidRepairItem(ItemStack itemStack, ItemStack itemStack2) {
        return itemStack2.is(ZombieKitItems.BATTERY.get());
    }

    @Override
    public UseAnim getUseAnimation(ItemStack itemstack) {
        return UseAnim.BOW;
    }

    @Override
    public int getUseDuration(ItemStack itemstack, LivingEntity entity) {
        return 72000;
    }
}
