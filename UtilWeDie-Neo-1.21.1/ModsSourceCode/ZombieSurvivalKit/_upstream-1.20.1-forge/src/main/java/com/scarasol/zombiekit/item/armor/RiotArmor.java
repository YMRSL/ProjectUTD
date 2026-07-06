package com.scarasol.zombiekit.item.armor;

import com.google.common.collect.Maps;
import com.scarasol.zombiekit.client.model.BombSuitModel;
import com.scarasol.zombiekit.client.model.RiotSuitModel;
import com.scarasol.zombiekit.client.model.SkiingSuitModel;
import com.scarasol.zombiekit.init.ZombieKitModels;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class RiotArmor extends CamouflageArmor{

    public final static Map<EquipmentSlot, HumanoidModel<LivingEntity>> ARMOR_MODEL = Maps.newHashMap();

    public RiotArmor(ArmorMaterial armorMaterial, Type equipmentSlot, Properties properties, int camouflage) {
        super(armorMaterial, equipmentSlot, properties, camouflage);
    }

    @Override
    public String getArmorTexture(ItemStack itemStack, Entity entity, EquipmentSlot slot, String type) {
        switch (getCamouflage()){
            case 1 -> {
                return "zombiekit:textures/entities/riot_suit_desert.png";
            }
            case 2 -> {
                return "zombiekit:textures/entities/riot_suit_forest.png";
            }
            case 3 -> {
                return "zombiekit:textures/entities/riot_suit_snow.png";
            }
            default -> {
                return "zombiekit:textures/entities/riot_suit_standard.png";
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    public HumanoidModel getArmorModel(){
        if (ARMOR_MODEL.isEmpty()) {
            for (EquipmentSlot equipmentSlot : EquipmentSlot.values()) {
                if (equipmentSlot.isArmor()) {
                    ARMOR_MODEL.put(equipmentSlot, ZombieKitModels.getDefaultArmorModel(equipmentSlot, new RiotSuitModel<>(Minecraft.getInstance().getEntityModels().bakeLayer(RiotSuitModel.LAYER_LOCATION))));
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
                HumanoidModel armorModel = RiotArmor.this.getArmorModel();
                armorModel.crouching = living.isShiftKeyDown();
                armorModel.riding = defaultModel.riding;
                armorModel.young = living.isBaby();
                return armorModel;
            }
        });
    }
}
