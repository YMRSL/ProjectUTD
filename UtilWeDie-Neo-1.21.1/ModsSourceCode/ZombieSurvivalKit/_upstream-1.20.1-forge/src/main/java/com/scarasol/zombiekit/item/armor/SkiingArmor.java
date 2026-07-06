package com.scarasol.zombiekit.item.armor;

import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.scarasol.zombiekit.client.model.SkiingSuitModel;
import com.scarasol.zombiekit.init.ZombieKitModels;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class SkiingArmor extends ArmorItem {

    public final static Map<EquipmentSlot, HumanoidModel<LivingEntity>> ARMOR_MODEL = Maps.newHashMap();

    public SkiingArmor(ArmorMaterial armorMaterial, Type equipmentSlot, Properties properties) {
        super(armorMaterial, equipmentSlot, properties);
    }

    @Override
    public void onInventoryTick(ItemStack itemStack, Level level, Player player, int slotIndex, int selectedIndex){
        super.onInventoryTick(itemStack, level, player, slotIndex, selectedIndex);
        if (getEquipmentSlot() == EquipmentSlot.HEAD && Iterables.contains(player.getArmorSlots(), itemStack)){
            player.removeEffect(MobEffects.BLINDNESS);
        }
    }

    @Override
    public boolean canWalkOnPowderedSnow(ItemStack itemStack, LivingEntity livingEntity) {
        return getEquipmentSlot() == EquipmentSlot.FEET;
    }

    @Override
    public String getArmorTexture(ItemStack itemStack, Entity entity, EquipmentSlot slot, String type) {
        return "zombiekit:textures/entities/skiing_suit.png";
    }

    @OnlyIn(Dist.CLIENT)
    public HumanoidModel getArmorModel(){
        if (ARMOR_MODEL.isEmpty()) {
            for (EquipmentSlot equipmentSlot : EquipmentSlot.values()) {
                if (equipmentSlot.isArmor()) {
                    ARMOR_MODEL.put(equipmentSlot, ZombieKitModels.getDefaultArmorModel(equipmentSlot, new SkiingSuitModel<>(Minecraft.getInstance().getEntityModels().bakeLayer(SkiingSuitModel.LAYER_LOCATION))));
                }
            }
        }
        return ARMOR_MODEL.get(getEquipmentSlot());
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            @Override
            @OnlyIn(Dist.CLIENT)
            public HumanoidModel getHumanoidArmorModel(LivingEntity living, ItemStack stack, EquipmentSlot slot, HumanoidModel defaultModel) {
                HumanoidModel armorModel = SkiingArmor.this.getArmorModel();
                armorModel.crouching = living.isShiftKeyDown();
                armorModel.riding = defaultModel.riding;
                armorModel.young = living.isBaby();
                return armorModel;
            }
        });
    }


}
