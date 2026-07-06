package com.scarasol.zombiekit.item.armor;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;

public class BombArmor extends ArmorItem {

    public static final ResourceLocation ATTACK_SPEED_ID = ResourceLocation.fromNamespaceAndPath("zombiekit", "bomb_armor_attack_speed");
    public static final ResourceLocation SLOWNESS_ID = ResourceLocation.fromNamespaceAndPath("zombiekit", "bomb_armor_slowness");
    public static final ResourceLocation HEALTH_BOOST_ID = ResourceLocation.fromNamespaceAndPath("zombiekit", "bomb_armor_health_boost");

    public static AttributeModifier ATTACK_SPEED = new AttributeModifier(ATTACK_SPEED_ID, -0.3, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    public static AttributeModifier SLOWNESS = new AttributeModifier(SLOWNESS_ID, -0.3, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
    public static AttributeModifier HEALTH_BOOST = new AttributeModifier(HEALTH_BOOST_ID, 20, AttributeModifier.Operation.ADD_VALUE);

    public BombArmor(Holder<ArmorMaterial> armorMaterial, Type equipmentSlot, Properties properties) {
        super(armorMaterial, equipmentSlot, ModArmorMaterial.applyDurability(armorMaterial, properties, equipmentSlot));
    }

    public static void updateModifier(LivingEntity livingEntity){
        int count = numberOfSuit(livingEntity);
        if (count == 0){
            livingEntity.getAttribute(Attributes.MAX_HEALTH).removeModifier(HEALTH_BOOST_ID);
            livingEntity.getAttribute(Attributes.MOVEMENT_SPEED).removeModifier(SLOWNESS_ID);
            if (livingEntity instanceof Player)
                livingEntity.getAttribute(Attributes.ATTACK_SPEED).removeModifier(ATTACK_SPEED_ID);
        }else if (count == 4){
            livingEntity.getAttribute(Attributes.MAX_HEALTH).removeModifier(HEALTH_BOOST_ID);
            livingEntity.getAttribute(Attributes.MAX_HEALTH).addPermanentModifier(HEALTH_BOOST);
        }else if (count == 1){
            livingEntity.getAttribute(Attributes.MOVEMENT_SPEED).removeModifier(SLOWNESS_ID);
            livingEntity.getAttribute(Attributes.MOVEMENT_SPEED).addPermanentModifier(SLOWNESS);
            if (livingEntity instanceof Player){
                livingEntity.getAttribute(Attributes.ATTACK_SPEED).removeModifier(ATTACK_SPEED_ID);
                livingEntity.getAttribute(Attributes.ATTACK_SPEED).addPermanentModifier(ATTACK_SPEED);

            }
        }else if (count == 3){
            livingEntity.getAttribute(Attributes.MAX_HEALTH).removeModifier(HEALTH_BOOST_ID);
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
