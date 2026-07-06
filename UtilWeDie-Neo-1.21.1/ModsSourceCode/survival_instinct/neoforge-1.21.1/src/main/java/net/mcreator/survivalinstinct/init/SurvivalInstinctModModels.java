package net.mcreator.survivalinstinct.init;

import net.mcreator.survivalinstinct.client.model.ModelRecon;
import net.mcreator.survivalinstinct.client.model.Modelbalaclava;
import net.mcreator.survivalinstinct.client.model.Modelchiken_head;
import net.mcreator.survivalinstinct.client.model.Modelexo_heavy_armor;
import net.mcreator.survivalinstinct.client.model.Modelexo_suit_armor;
import net.mcreator.survivalinstinct.client.model.Modelfire_fighter;
import net.mcreator.survivalinstinct.client.model.Modelgas_mask;
import net.mcreator.survivalinstinct.client.model.Modelghillie;
import net.mcreator.survivalinstinct.client.model.Modelhazmat;
import net.mcreator.survivalinstinct.client.model.Modelhunter;
import net.mcreator.survivalinstinct.client.model.Modelhunter_armor;
import net.mcreator.survivalinstinct.client.model.Modeljuggernaut;
import net.mcreator.survivalinstinct.client.model.Modeljuggernaut2;
import net.mcreator.survivalinstinct.client.model.Modeljuggernaut_armor;
import net.mcreator.survivalinstinct.client.model.Modelmilitary_armor;
import net.mcreator.survivalinstinct.client.model.Modelmotorcycle_helmet;
import net.mcreator.survivalinstinct.client.model.Modelnail_proyectile;
import net.mcreator.survivalinstinct.client.model.Modelnight_vision_goggles;
import net.mcreator.survivalinstinct.client.model.Modelpolice_armor;
import net.mcreator.survivalinstinct.client.model.Modelreaper;
import net.mcreator.survivalinstinct.client.model.Modelrecluit;
import net.mcreator.survivalinstinct.client.model.Modelrecluit_armor;
import net.mcreator.survivalinstinct.client.model.Modelrecon_layer_1;
import net.mcreator.survivalinstinct.client.model.Modelrecon_layer_2;
import net.mcreator.survivalinstinct.client.model.Modelrockie;
import net.mcreator.survivalinstinct.client.model.Modelrockie3;
import net.mcreator.survivalinstinct.client.model.Modelrockie_armor;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(bus=EventBusSubscriber.Bus.MOD, value={Dist.CLIENT}, modid = "survival_instinct")
public class SurvivalInstinctModModels {
    @SubscribeEvent
    public static void registerLayerDefinitions(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(Modelgas_mask.LAYER_LOCATION, Modelgas_mask::createBodyLayer);
        event.registerLayerDefinition(Modelhazmat.LAYER_LOCATION, Modelhazmat::createBodyLayer);
        event.registerLayerDefinition(Modelrecluit.LAYER_LOCATION, Modelrecluit::createBodyLayer);
        event.registerLayerDefinition(Modelhunter_armor.LAYER_LOCATION, Modelhunter_armor::createBodyLayer);
        event.registerLayerDefinition(ModelRecon.LAYER_LOCATION, ModelRecon::createBodyLayer);
        event.registerLayerDefinition(Modelghillie.LAYER_LOCATION, Modelghillie::createBodyLayer);
        event.registerLayerDefinition(Modelnight_vision_goggles.LAYER_LOCATION, Modelnight_vision_goggles::createBodyLayer);
        event.registerLayerDefinition(Modelfire_fighter.LAYER_LOCATION, Modelfire_fighter::createBodyLayer);
        event.registerLayerDefinition(Modelrockie_armor.LAYER_LOCATION, Modelrockie_armor::createBodyLayer);
        event.registerLayerDefinition(Modeljuggernaut.LAYER_LOCATION, Modeljuggernaut::createBodyLayer);
        event.registerLayerDefinition(Modelmilitary_armor.LAYER_LOCATION, Modelmilitary_armor::createBodyLayer);
        event.registerLayerDefinition(Modelnail_proyectile.LAYER_LOCATION, Modelnail_proyectile::createBodyLayer);
        event.registerLayerDefinition(Modelrecluit_armor.LAYER_LOCATION, Modelrecluit_armor::createBodyLayer);
        event.registerLayerDefinition(Modelmotorcycle_helmet.LAYER_LOCATION, Modelmotorcycle_helmet::createBodyLayer);
        event.registerLayerDefinition(Modeljuggernaut2.LAYER_LOCATION, Modeljuggernaut2::createBodyLayer);
        event.registerLayerDefinition(Modelhunter.LAYER_LOCATION, Modelhunter::createBodyLayer);
        event.registerLayerDefinition(Modelchiken_head.LAYER_LOCATION, Modelchiken_head::createBodyLayer);
        event.registerLayerDefinition(Modelrecon_layer_1.LAYER_LOCATION, Modelrecon_layer_1::createBodyLayer);
        event.registerLayerDefinition(Modeljuggernaut_armor.LAYER_LOCATION, Modeljuggernaut_armor::createBodyLayer);
        event.registerLayerDefinition(Modelrockie3.LAYER_LOCATION, Modelrockie3::createBodyLayer);
        event.registerLayerDefinition(Modelexo_heavy_armor.LAYER_LOCATION, Modelexo_heavy_armor::createBodyLayer);
        event.registerLayerDefinition(Modelreaper.LAYER_LOCATION, Modelreaper::createBodyLayer);
        event.registerLayerDefinition(Modelrecon_layer_2.LAYER_LOCATION, Modelrecon_layer_2::createBodyLayer);
        event.registerLayerDefinition(Modelrockie.LAYER_LOCATION, Modelrockie::createBodyLayer);
        event.registerLayerDefinition(Modelexo_suit_armor.LAYER_LOCATION, Modelexo_suit_armor::createBodyLayer);
        event.registerLayerDefinition(Modelpolice_armor.LAYER_LOCATION, Modelpolice_armor::createBodyLayer);
        event.registerLayerDefinition(Modelbalaclava.LAYER_LOCATION, Modelbalaclava::createBodyLayer);
    }
}

