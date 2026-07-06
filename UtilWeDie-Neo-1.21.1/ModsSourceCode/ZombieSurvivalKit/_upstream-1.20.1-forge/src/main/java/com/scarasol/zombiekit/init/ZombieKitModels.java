package com.scarasol.zombiekit.init;

import com.google.common.collect.Maps;
import com.scarasol.zombiekit.client.model.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, value = {Dist.CLIENT})
public class ZombieKitModels {
    public static final Map<String, ModelPart> ARMOR_MAP = Maps.newHashMap(Map.of(
            "head", new ModelPart(Collections.emptyList(), Collections.emptyMap()),
            "hat", new ModelPart(Collections.emptyList(), Collections.emptyMap()),
            "body", new ModelPart(Collections.emptyList(), Collections.emptyMap()),
            "right_arm", new ModelPart(Collections.emptyList(), Collections.emptyMap()),
            "left_arm", new ModelPart(Collections.emptyList(), Collections.emptyMap()),
            "right_leg", new ModelPart(Collections.emptyList(), Collections.emptyMap()),
            "left_leg", new ModelPart(Collections.emptyList(), Collections.emptyMap())
    ));

    @SubscribeEvent
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(WrenchModel.LAYER_LOCATION, WrenchModel::createBodyLayer);
        event.registerLayerDefinition(TacticalSuitModel.LAYER_LOCATION, TacticalSuitModel::createBodyLayer);
        event.registerLayerDefinition(BombSuitModel.LAYER_LOCATION, BombSuitModel::createBodyLayer);
        event.registerLayerDefinition(RiotSuitModel.LAYER_LOCATION, RiotSuitModel::createBodyLayer);
        event.registerLayerDefinition(DroneModel.LAYER_LOCATION, DroneModel::createBodyLayer);
        event.registerLayerDefinition(ExoSuitModel.LAYER_LOCATION, ExoSuitModel::createBodyLayer);
        event.registerLayerDefinition(UVlampModel.LAYER_LOCATION, UVlampModel::createBodyLayer);
        event.registerLayerDefinition(FlaresModel.LAYER_LOCATION, FlaresModel::createBodyLayer);
        event.registerLayerDefinition(SkiingSuitModel.LAYER_LOCATION, SkiingSuitModel::createBodyLayer);
    }
    
    public static HumanoidModel<LivingEntity> getDefaultArmorModel(EquipmentSlot equipmentSlot, AbstractArmorModel<Entity> entityModel) {
        Map<String, ModelPart> map = getArmorMap();

        switch (equipmentSlot){
            case HEAD -> map.put("head", entityModel.Head);
            case CHEST -> {
                map.put("body", entityModel.Body);
                map.put("right_arm", entityModel.RightArm);
                map.put("left_arm", entityModel.LeftArm);
            }
            case LEGS -> {
                map.put("right_leg", entityModel.RightLeg);
                map.put("left_leg", entityModel.LeftLeg);
            }
            default -> {
                map.put("right_leg", entityModel.RightShoes);
                map.put("left_leg", entityModel.LeftShoes);
            }
        }
        
        return new HumanoidModel<>(new ModelPart(Collections.emptyList(), Map.copyOf(map)));
    }

    public static Map<String, ModelPart> getArmorMap() {
        return Maps.newHashMap(ARMOR_MAP);
    }

    public static HumanoidModel<LivingEntity> getEmptyArmorModel() {
        return new HumanoidModel<>(new ModelPart(Collections.emptyList(), Map.copyOf(getArmorMap())));
    }

}
