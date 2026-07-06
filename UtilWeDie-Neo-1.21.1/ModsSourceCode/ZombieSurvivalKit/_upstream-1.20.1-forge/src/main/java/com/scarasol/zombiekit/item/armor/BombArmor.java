package com.scarasol.zombiekit.item.armor;

import com.google.common.collect.Maps;
import com.scarasol.zombiekit.client.model.BombSuitModel;
import com.scarasol.zombiekit.client.model.RiotSuitModel;
import com.scarasol.zombiekit.client.model.TacticalSuitModel;
import com.scarasol.zombiekit.init.ZombieKitModels;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
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
import java.util.UUID;
import java.util.function.Consumer;

public class BombArmor extends ArmorItem {

    public static AttributeModifier ATTACK_SPEED = new AttributeModifier(UUID.fromString("0753E5E5-B0B5-6828-0B38-350A5885B144"), "bombArmorAttackSpeed", -0.3, AttributeModifier.Operation.MULTIPLY_TOTAL);
    public static AttributeModifier SLOWNESS = new AttributeModifier(UUID.fromString("CE35FE84-FF47-B780-0EC6-9164DCF1CED6"), "bombArmorSlowness", -0.3, AttributeModifier.Operation.MULTIPLY_TOTAL);
    public static AttributeModifier HEALTH_BOOST = new AttributeModifier(UUID.fromString("bec5f226-4b78-5014-b906-2fb397957925"), "bombArmorHealthBoost", 20, AttributeModifier.Operation.ADDITION);
    public final static Map<EquipmentSlot, HumanoidModel<LivingEntity>> ARMOR_MODEL = Maps.newHashMap();

    public BombArmor(ArmorMaterial armorMaterial, Type equipmentSlot, Properties properties) {
        super(armorMaterial, equipmentSlot, properties);
    }

    @Override
    public String getArmorTexture(ItemStack itemStack, Entity entity, EquipmentSlot slot, String type) {
        return "zombiekit:textures/entities/bomb_suit.png";
    }

    @OnlyIn(Dist.CLIENT)
    public HumanoidModel getArmorModel(){
        if (ARMOR_MODEL.isEmpty()) {
            for (EquipmentSlot equipmentSlot : EquipmentSlot.values()) {
                if (equipmentSlot.isArmor()) {
                    ARMOR_MODEL.put(equipmentSlot, ZombieKitModels.getDefaultArmorModel(equipmentSlot, new BombSuitModel<>(Minecraft.getInstance().getEntityModels().bakeLayer(BombSuitModel.LAYER_LOCATION))));
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
            public HumanoidModel<LivingEntity> getHumanoidArmorModel(LivingEntity living, ItemStack stack, EquipmentSlot slot, HumanoidModel defaultModel) {
                HumanoidModel<LivingEntity> armorModel = BombArmor.this.getArmorModel();
                armorModel.crouching = living.isShiftKeyDown();
                armorModel.riding = defaultModel.riding;
                armorModel.young = living.isBaby();
                return armorModel;
            }
        });
    }

    public static void updateModifier(LivingEntity livingEntity){
        int count = numberOfSuit(livingEntity);
        if (count == 0){
            livingEntity.getAttribute(Attributes.MAX_HEALTH).removeModifier(HEALTH_BOOST);
            livingEntity.getAttribute(Attributes.MOVEMENT_SPEED).removeModifier(SLOWNESS);
            if (livingEntity instanceof Player)
                livingEntity.getAttribute(Attributes.ATTACK_SPEED).removeModifier(ATTACK_SPEED);
        }else if (count == 4){
            livingEntity.getAttribute(Attributes.MAX_HEALTH).removeModifier(HEALTH_BOOST);
            livingEntity.getAttribute(Attributes.MAX_HEALTH).addPermanentModifier(HEALTH_BOOST);
        }else if (count == 1){
            livingEntity.getAttribute(Attributes.MOVEMENT_SPEED).removeModifier(SLOWNESS);
            livingEntity.getAttribute(Attributes.MOVEMENT_SPEED).addPermanentModifier(SLOWNESS);
            if (livingEntity instanceof Player){
                livingEntity.getAttribute(Attributes.ATTACK_SPEED).removeModifier(ATTACK_SPEED);
                livingEntity.getAttribute(Attributes.ATTACK_SPEED).addPermanentModifier(ATTACK_SPEED);

            }
        }else if (count == 3){
            livingEntity.getAttribute(Attributes.MAX_HEALTH).removeModifier(HEALTH_BOOST);
        }
    }

    public static int numberOfSuit(LivingEntity livingEntity){
        int count = 0;
        for (ItemStack itemStack : livingEntity.getArmorSlots()){
            if (itemStack.getItem() instanceof BombArmor)
                count++;
        }
        return count;
    }
}
