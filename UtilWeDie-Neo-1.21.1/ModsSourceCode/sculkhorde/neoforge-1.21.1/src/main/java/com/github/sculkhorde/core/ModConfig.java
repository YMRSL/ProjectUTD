package com.github.sculkhorde.core;

import com.electronwill.nightconfig.core.Config;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.ModConfigSpec;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class    ModConfig {

    public static final Server SERVER;
    public static final ModConfigSpec SERVER_SPEC;

    public static final DataGen DATAGEN;
    public static final ModConfigSpec DATAGEN_SPEC;

    public static class Server {

        public final ModConfigSpec.ConfigValue<Boolean> target_faw_entities;
        public final ModConfigSpec.ConfigValue<Boolean> target_spore_entities;
        public final ModConfigSpec.ConfigValue<Boolean> target_dawn_of_the_flood_entities;
        public final ModConfigSpec.ConfigValue<Boolean> target_the_flesh_that_hates_entities;
        public final ModConfigSpec.ConfigValue<Boolean> target_abominations_infection_entities;
        public final ModConfigSpec.ConfigValue<Boolean> target_another_dimension_infection_entities;
        public final ModConfigSpec.ConfigValue<Boolean> target_complete_distortion_entities;
        public final ModConfigSpec.ConfigValue<Boolean> target_entomophobia_entities;
        public final ModConfigSpec.ConfigValue<Boolean> target_phayriosis_entities;
        public final ModConfigSpec.ConfigValue<Boolean> target_prion_infection_entities;
        public final ModConfigSpec.ConfigValue<Boolean> target_swarm_infection_entities;
        public final ModConfigSpec.ConfigValue<Boolean> target_bulbus_infection_entities;
        public final ModConfigSpec.ConfigValue<Boolean> target_withering_away_reborn_entities;
        public final ModConfigSpec.ConfigValue<Boolean> target_mi_alliance_entities;
        public final ModConfigSpec.ConfigValue<Boolean> target_deeper_and_darker_entities;
        public final ModConfigSpec.ConfigValue<Boolean> target_scape_and_run_parasites_entities;
        public final ModConfigSpec.ConfigValue<Boolean> target_dulling_entities;



        public final ModConfigSpec.ConfigValue<Boolean> block_infestation_enabled;
        public final ModConfigSpec.ConfigValue<Boolean> chunk_loading_enabled;
        public final ModConfigSpec.ConfigValue<Boolean> disable_defeating_sculk_horde;
        public final ModConfigSpec.ConfigValue<Integer> max_unit_population;
        public final ModConfigSpec.ConfigValue<Boolean> trigger_ancient_node_automatically;
        public final ModConfigSpec.ConfigValue<Integer> trigger_ancient_node_wait_days;
        public final ModConfigSpec.ConfigValue<Integer> trigger_ancient_node_time_of_day;
        public final ModConfigSpec.ConfigValue<Boolean> should_all_other_mobs_attack_the_sculk_horde;
        public final ModConfigSpec.ConfigValue<Boolean> should_animals_and_villagers_avoid_the_sculk_horde;

        public final ModConfigSpec.ConfigValue<Integer> gravemind_mass_goal_for_immature_stage;
        public final ModConfigSpec.ConfigValue<Integer> gravemind_mass_goal_for_mature_stage;

        public final ModConfigSpec.ConfigValue<Integer> sculk_node_chunkload_radius;
        public final ModConfigSpec.ConfigValue<Integer> sculk_node_spawn_cooldown_minutes;
        public final ModConfigSpec.ConfigValue<Boolean> enable_node_relocation;

        public final ModConfigSpec.ConfigValue<Boolean> should_sculk_mites_spawn_in_deep_dark;

        public final ModConfigSpec.ConfigValue<Boolean> should_phantoms_load_chunks;
        public final ModConfigSpec.ConfigValue<Boolean> should_sculk_nodes_and_raids_spawn_phantoms;
        public final ModConfigSpec.ConfigValue<Boolean> should_ancient_node_spawn_phantoms;
        
        public final ModConfigSpec.ConfigValue<Boolean> sculk_raid_enabled;
        public final ModConfigSpec.ConfigValue<Integer> sculk_raid_enderman_scouting_duration_minutes;
        public final ModConfigSpec.ConfigValue<Integer> sculk_raid_global_cooldown_between_raids_minutes;
        public final ModConfigSpec.ConfigValue<Integer> sculk_raid_no_raid_zone_duration_minutes;
        public final ModConfigSpec.ConfigValue<Double> purification_speed_multiplier;
        public final ModConfigSpec.ConfigValue<Integer> infestation_purifier_range;
        private final ModConfigSpec.ConfigValue<List<? extends String>> items_infection_cursors_can_eat;
        public static final HashMap<String, Boolean> infection_cursor_item_eat_list = new HashMap<>();

        private final ModConfigSpec.ConfigValue<List<? extends String>> make_block_infestable;
        public static final HashMap<String, Boolean> manually_configured_infestable_blocks = new HashMap<>();

        public final ModConfigSpec.DoubleValue infection_speed_multiplier;
        public final ModConfigSpec.ConfigValue<Integer> max_nodes_active;
        public final ModConfigSpec.ConfigValue<Boolean> disable_auto_performance_system;

        public final ModConfigSpec.ConfigValue<Integer> minutes_required_for_performance_increase;
        public final ModConfigSpec.ConfigValue<Integer> seconds_required_for_performance_decrease;

        private final ModConfigSpec.ConfigValue<List<? extends String>> sculk_horde_target_blacklist;

        public final ModConfigSpec.ConfigValue<Integer> max_infestation_cursor_population;

        public final ModConfigSpec.ConfigValue<Boolean> enable_gpu_compatibility_mode;

        public final ModConfigSpec.ConfigValue<Boolean> experimental_features_enabled;
        public final ModConfigSpec.ConfigValue<Boolean> experimental_brood_hatcher_enabled;
        public final ModConfigSpec.ConfigValue<String> difficulty_mode;
        public final ModConfigSpec.ConfigValue<Boolean> isHordeActiveWithNoPlayers;
        public final ModConfigSpec.ConfigValue<Boolean> hit_squad_event_enabled;
        public final ModConfigSpec.ConfigValue<Boolean> ghast_deployment_event_enabled;

        public void loadItemsInfectionCursorsCanEat()
        {
            infection_cursor_item_eat_list.clear();
            for(String item : ModConfig.SERVER.items_infection_cursors_can_eat.get())
            {
                infection_cursor_item_eat_list.put(item, true);
            }
        }

        public boolean isItemEdibleToCursors(ItemEntity itemEntity)
        {
            ItemStack itemStack = itemEntity.getItem();
            Item item = itemStack.getItem();
            ResourceLocation itemResourceLocation = BuiltInRegistries.ITEM.getKey(item);


            if(itemResourceLocation == null)
            {
                return false;
            }

            String itemName = itemResourceLocation.toString();
            if(infection_cursor_item_eat_list.containsKey(itemName))
            {
                return true;
            }

            if(itemStack.has(net.minecraft.core.component.DataComponents.FOOD))
            {
                return true;
            }

            if (itemName.contains("sapling")) {
                return true;
            }

            return false;
        }

        public void loadConfiguredInfestableBlocks()
        {
            manually_configured_infestable_blocks.clear();
            for(String block : ModConfig.SERVER.make_block_infestable.get())
            {
                manually_configured_infestable_blocks.put(block, true);
            }
        }

        public boolean isBlockConfiguredToBeInfestable(BlockState blockState)
        {
            Block block = blockState.getBlock();
            ResourceLocation itemResourceLocation = BuiltInRegistries.BLOCK.getKey(block);


            if(itemResourceLocation == null)
            {
                return false;
            }

            String blockName = itemResourceLocation.toString();
            if(manually_configured_infestable_blocks.containsKey(blockName))
            {
                return true;
            }

            return false;
        }

        public boolean isEntityOnSculkHordeTargetBlacklist(Entity entity)
        {
            ResourceLocation entityResourceLocation = BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType());
            String entityNameSpace = entityResourceLocation.toString();

            if(sculk_horde_target_blacklist.get().contains(entityNameSpace))
            {
                return true;
            }

            return false;
        }

        public Server(ModConfigSpec.Builder builder) {

            Config.setInsertionOrderPreserved(true);

            builder.push("Performance Settings");
            disable_auto_performance_system = builder.comment("\u662f\u5426\u7981\u7528\u81ea\u52a8\u6027\u80fd\u7cfb\u7edf? \u8bbe\u4e3a true \u5c06\u59cb\u7ec8\u4fdd\u6301\u6700\u9ad8\u6027\u80fd\u6a21\u5f0f(\u611f\u67d3\u6269\u6563/\u602a\u7269\u751f\u6210\u4e0d\u518d\u88ab\u8282\u6d41, \u60f3\u8981 1.20.1 \u90a3\u79cd\u901f\u5ea6\u5c31\u5f00\u8fd9\u4e2a)\u3002(\u9ed8\u8ba4 false)").define("disable_auto_performance_system", false);
            max_unit_population = builder.comment("\u540c\u65f6\u5141\u8bb8\u5b58\u5728\u7684\u5e7d\u533f\u751f\u7269\u6570\u91cf\u4e0a\u9650? (\u9ed8\u8ba4 200)").defineInRange("max_unit_population",200, 0, 1000);
            max_nodes_active = builder.comment("\u540c\u65f6\u53ef\u6fc0\u6d3b\u7684\u5e7d\u533f\u8282\u70b9\u6570\u91cf\u4e0a\u9650? (\u9ed8\u8ba4 1)").defineInRange("max_nodes_active",1, 0, 1000);
            minutes_required_for_performance_increase = builder.comment("\u9700\u8981\u6301\u7eed\u591a\u5c11\u3010\u5206\u949f\u3011\u7684\u826f\u597d\u6027\u80fd(TPS)\u624d\u4f1a\u63d0\u5347\u6027\u80fd\u6a21\u5f0f(\u89e3\u9664\u8282\u6d41)? (\u9ed8\u8ba4 5)").defineInRange("minutes_required_for_performance_increase",5, 1, Integer.MAX_VALUE);
            seconds_required_for_performance_decrease = builder.comment("\u9700\u8981\u6301\u7eed\u591a\u5c11\u3010\u79d2\u3011\u7684\u7cdf\u7cd5\u6027\u80fd(TPS)\u624d\u4f1a\u964d\u4f4e\u6027\u80fd\u6a21\u5f0f(\u52a0\u91cd\u8282\u6d41)? (\u9ed8\u8ba4 30)").defineInRange("seconds_required_for_performance_decrease",30, 1, Integer.MAX_VALUE);
            builder.pop();

            builder.push("Mod Compatability");
            target_faw_entities = builder.comment("\u5e7d\u533f\u90e8\u843d\u662f\u5426\u653b\u51fb\u6765\u81ea 'From Another World' mod \u7684\u751f\u7269? (\u9ed8\u8ba4 false)").define("target_faw_entities",false);
            target_spore_entities = builder.comment("\u5e7d\u533f\u90e8\u843d\u662f\u5426\u653b\u51fb\u6765\u81ea 'Fungal Infection:Spore' mod \u7684\u751f\u7269? (\u9ed8\u8ba4 false)").define("target_spore_entities",false);
            target_deeper_and_darker_entities = builder.comment("\u5e7d\u533f\u90e8\u843d\u662f\u5426\u653b\u51fb\u6765\u81ea 'Deeper and Darker' mod \u7684\u751f\u7269? (\u9ed8\u8ba4 true)").define("target_deeper_and_darker_entities",true);
            target_dulling_entities = builder.comment("\u5e7d\u533f\u90e8\u843d\u662f\u5426\u653b\u51fb\u6765\u81ea 'The Dulling' mod \u7684\u751f\u7269? (\u9ed8\u8ba4 true)").define("target_dulling_entities",true);
            target_mi_alliance_entities = builder.comment("\u5e7d\u533f\u90e8\u843d\u662f\u5426\u653b\u51fb\u6765\u81ea 'Mi Alliance' mod \u7684\u751f\u7269? (\u9ed8\u8ba4 true)").define("target_mi_alliance_entities",true);
            target_scape_and_run_parasites_entities = builder.comment("\u5e7d\u533f\u90e8\u843d\u662f\u5426\u653b\u51fb\u6765\u81ea 'Scape and Run Parsites' mod \u7684\u751f\u7269? (\u9ed8\u8ba4 true)").define("target_scape_and_run_parasites_entities",true);
            target_the_flesh_that_hates_entities = builder.comment("\u5e7d\u533f\u90e8\u843d\u662f\u5426\u653b\u51fb\u6765\u81ea 'The Flesh That Hates' mod \u7684\u751f\u7269? (\u9ed8\u8ba4 true)").define("target_the_flesh_that_hates_entities",true);
            target_dawn_of_the_flood_entities = builder.comment("\u5e7d\u533f\u90e8\u843d\u662f\u5426\u653b\u51fb\u6765\u81ea 'Dawn of the Flood' mod \u7684\u751f\u7269? (\u9ed8\u8ba4 true)").define("target_dawn_of_the_flood_entities",true);
            target_prion_infection_entities = builder.comment("\u5e7d\u533f\u90e8\u843d\u662f\u5426\u653b\u51fb\u6765\u81ea 'Prion Infection' mod \u7684\u751f\u7269? (\u9ed8\u8ba4 true)").define("target_prion_infection_entities",true);
            target_withering_away_reborn_entities = builder.comment("\u5e7d\u533f\u90e8\u843d\u662f\u5426\u653b\u51fb\u6765\u81ea 'Withering Away: Reborn' mod \u7684\u751f\u7269? (\u9ed8\u8ba4 true)").define("target_withering_away_reborn_entities",true);
            target_entomophobia_entities = builder.comment("\u5e7d\u533f\u90e8\u843d\u662f\u5426\u653b\u51fb\u6765\u81ea 'Entomophobia' mod \u7684\u751f\u7269? (\u9ed8\u8ba4 true)").define("target_entomophobia_entities",true);
            target_abominations_infection_entities = builder.comment("\u5e7d\u533f\u90e8\u843d\u662f\u5426\u653b\u51fb\u6765\u81ea 'Abominations Infection' mod \u7684\u751f\u7269? (\u9ed8\u8ba4 true)").define("target_abominations_infection_entities",true);
            target_another_dimension_infection_entities = builder.comment("\u5e7d\u533f\u90e8\u843d\u662f\u5426\u653b\u51fb\u6765\u81ea 'Another Dimension Infection' mod \u7684\u751f\u7269? (\u9ed8\u8ba4 true)").define("target_another_dimension_infection_entities",true);
            target_phayriosis_entities = builder.comment("\u5e7d\u533f\u90e8\u843d\u662f\u5426\u653b\u51fb\u6765\u81ea 'Pharyriosis Parasite Infection' mod \u7684\u751f\u7269? (\u9ed8\u8ba4 true)").define("target_phayriosis_entities",true);
            target_complete_distortion_entities = builder.comment("\u5e7d\u533f\u90e8\u843d\u662f\u5426\u653b\u51fb\u6765\u81ea 'Complete Distortion: Infection from Otherworld' mod \u7684\u751f\u7269? (\u9ed8\u8ba4 true)").define("target_complete_distortion_entities",true);
            target_swarm_infection_entities = builder.comment("\u5e7d\u533f\u90e8\u843d\u662f\u5426\u653b\u51fb\u6765\u81ea 'Swarm Infection' mod \u7684\u751f\u7269? (\u9ed8\u8ba4 true)").define("target_swarm_infection_entities",true);
            target_bulbus_infection_entities = builder.comment("\u5e7d\u533f\u90e8\u843d\u662f\u5426\u653b\u51fb\u6765\u81ea 'The Bulbus Infection' mod \u7684\u751f\u7269? (\u9ed8\u8ba4 true)").define("target_bulbus_infection_entities",true);
            builder.pop();

            builder.push("General Variables");
            difficulty_mode = builder.comment("\u5e7d\u533f\u90e8\u843d\u96be\u5ea6\u3002Auto \u8868\u793a\u8ddf\u968f\u6e38\u620f\u96be\u5ea6\u3002\u53ef\u9009 = {AUTO, EASY, NORMAL, HARD} (\u9ed8\u8ba4 AUTO)").define("difficulty_mode","AUTO");
            should_all_other_mobs_attack_the_sculk_horde = builder.comment("\u9ed8\u8ba4\u60c5\u51b5\u4e0b\u5176\u5b83\u6240\u6709\u751f\u7269\u662f\u5426\u653b\u51fb\u5e7d\u533f\u90e8\u843d? (\u9ed8\u8ba4 true)").define("should_all_other_mobs_attack_the_sculk_horde",true);
            should_animals_and_villagers_avoid_the_sculk_horde = builder.comment("\u9ed8\u8ba4\u60c5\u51b5\u4e0b\u6240\u6709\u52a8\u7269\u548c\u6751\u6c11\u662f\u5426\u8eb2\u907f\u5e7d\u533f\u90e8\u843d? (\u9ed8\u8ba4 true)").define("should_animals_and_villagers_avoid_the_sculk_horde",true);
            block_infestation_enabled = builder.comment("\u5e7d\u533f\u90e8\u843d\u662f\u5426\u611f\u67d3\u65b9\u5757? (\u9ed8\u8ba4 true)").define("block_infestation_enabled",true);
            chunk_loading_enabled = builder.comment("\u5e7d\u533f\u90e8\u843d\u662f\u5426\u52a0\u8f7d\u533a\u5757? \u5173\u95ed\u4f1a\u7834\u574f\u9884\u671f\u4f53\u9a8c(\u4f8b\u5982\u7a81\u88ad\u65e0\u6cd5\u6b63\u5e38\u8fd0\u4f5c)\u3002(\u9ed8\u8ba4 true)").define("chunk_loading_enabled",true);
            disable_defeating_sculk_horde = builder.comment("\u73a9\u5bb6\u662f\u5426\u80fd\u591f\u51fb\u8d25\u5e7d\u533f\u90e8\u843d?").define("disable_defeating_sculk_horde",false);
            sculk_horde_target_blacklist = builder.comment("\u628a\u5b9e\u4f53\u52a0\u5165\u6b64\u5217\u8868\u53ef\u8ba9\u5e7d\u533f\u90e8\u843d\u4e0d\u653b\u51fb\u5b83\u4eec(\u5982 minecraft:creeper)\u3002\u8c28\u614e\u586b\u5199, \u53ef\u80fd\u5f15\u53d1\u95ee\u9898\u3002").defineList("sculk_horde_target_blacklist", Arrays.asList(""), entry -> true);
            enable_gpu_compatibility_mode = builder.comment("\u662f\u5426\u542f\u7528 GPU \u517c\u5bb9\u6a21\u5f0f? \u79fb\u9664\u5e7d\u533f\u751f\u7269\u7684\u53d1\u5149\u5c42, \u4fee\u590d\u5728\u90e8\u5206\u663e\u5361(AMD/Mac)\u4e0a\u5e7d\u533f\u751f\u7269\u663e\u793a\u4e3a\u5168\u9ed1\u7684\u95ee\u9898\u3002\u9700\u91cd\u542f\u6e38\u620f\u3002(\u9ed8\u8ba4 false)").define("enable_gpu_compatibility_mode",false);
            isHordeActiveWithNoPlayers = builder.comment("\u6ca1\u6709\u73a9\u5bb6\u5728\u7ebf\u65f6\u5e7d\u533f\u90e8\u843d\u662f\u5426\u4ecd\u7136\u6d3b\u52a8\u3002(\u9ed8\u8ba4 false)").define("isHordeActiveWithNoPlayers",true);
            hit_squad_event_enabled = builder.comment("\u662f\u5426\u542f\u7528'\u7a81\u51fb\u5c0f\u961f'\u4e8b\u4ef6(\u90e8\u843d\u6d3e\u602a\u730e\u6740\u73a9\u5bb6)? (\u9ed8\u8ba4 true)").define("hit_squad_event_enabled",true);
            ghast_deployment_event_enabled = builder.comment("\u662f\u5426\u542f\u7528'\u6076\u9b42\u6295\u653e'\u4e8b\u4ef6? (\u9ed8\u8ba4 true)").define("ghast_deployment_event_enabled",true);
            builder.pop();

            builder.push("Trigger Automatically Variables");
            trigger_ancient_node_automatically = builder.comment("\u5e7d\u533f\u90e8\u843d\u662f\u5426\u81ea\u52a8\u542f\u52a8? \u9700\u5f00\u542f\u533a\u5757\u52a0\u8f7d\u624d\u53ef\u9760, \u5426\u5219\u4ec5\u5f53\u8fdc\u53e4\u8282\u70b9\u6240\u5728\u533a\u5757\u88ab\u52a0\u8f7d\u65f6\u624d\u89e6\u53d1\u3002(\u9ed8\u8ba4 false)").define("trigger_ancient_node_automatically", false);
            trigger_ancient_node_wait_days = builder.comment("\u89e6\u53d1\u8fdc\u53e4\u8282\u70b9\u524d\u9700\u7b49\u5f85\u591a\u5c11\u5929? (\u9ed8\u8ba4 0)").defineInRange("trigger_ancient_node_wait_days", 0, 0, Integer.MAX_VALUE);
            trigger_ancient_node_time_of_day = builder.comment("\u7b49\u5f85\u5929\u6570\u8fc7\u540e, \u4e00\u5929\u4e2d\u9700\u5230\u8fbe\u591a\u5c11 tick \u624d\u89e6\u53d1\u8fdc\u53e4\u8282\u70b9? \u82e5\u7b49\u5f85\u5929\u6570\u4e3a0, \u5efa\u8bae\u8bbe>1000\u4ee5\u7559\u51fa\u4e16\u754c\u542f\u52a8/\u5361\u987f\u65f6\u95f4\u3002(\u9ed8\u8ba4 2000)").defineInRange("trigger_ancient_node_time_of_day", 2000, 0, 23999);
            builder.pop();

            builder.push("Infestation / Purification Variables");
            purification_speed_multiplier = builder.comment("\u51c0\u5316\u6269\u6563\u901f\u5ea6\u500d\u7387(\u8d8a\u5927\u8d8a\u5feb)\u3002(\u9ed8\u8ba4 1)").defineInRange("purification_speed_multiplier",1f, 0.001, 10f);
            infection_speed_multiplier = builder.comment("\u611f\u67d3\u6269\u6563\u901f\u5ea6\u500d\u7387(\u8d8a\u5927\u8d8a\u5feb)\u3002(\u9ed8\u8ba4 1)").defineInRange("infection_speed_multiplier",1.0, 0.001, 10);
            max_infestation_cursor_population = builder.comment("\u540c\u65f6\u5b58\u5728\u7684\u611f\u67d3\u6e38\u6807\u6570\u91cf\u4e0a\u9650\u3002\u6ce8\u610f: \u4ec5\u5728\u6027\u80fd\u6a21\u5f0f\u4e3a HIGH \u65f6\u751f\u6548; \u60f3\u59cb\u7ec8\u4fdd\u6301 HIGH \u8bf7\u628a disable_auto_performance_system \u8bbe\u4e3a true\u3002(\u9ed8\u8ba4 200)").defineInRange("max_infestation_cursor_population", 200, 1, Integer.MAX_VALUE);
            infestation_purifier_range = builder.comment("\u611f\u67d3\u51c0\u5316\u5668\u7684\u4f5c\u7528\u8303\u56f4? (\u9ed8\u8ba4 5)").defineInRange("purifier_range",48, 0, 100);
            items_infection_cursors_can_eat = builder.comment("\u611f\u67d3\u6e38\u6807\u4f1a\u5403\u6389\u54ea\u4e9b\u6389\u843d\u7269? \u7528\u4e8e\u51cf\u5c11\u5361\u987f\u5e76\u5ef6\u957f\u6e38\u6807\u5bff\u547d\u3002").defineList("items_infection_cursors_can_eat", Arrays.asList("minecraft:wheat_seeds", "minecraft:bamboo", "minecraft:stick", "minecraft:poppy", "minecraft:dandelion", "minecraft:blue_orchid", "minecraft:allium", "minecraft:azure_bluet", "minecraft:red_tulip", "minecraft:orange_tulip", "minecraft:white_tulip", "minecraft:pink_tulip", "minecraft:oxeye_daisy", "minecraft:cornflower", "minecraft:lily_of_the_valley", "minecraft:sunflower", "minecraft:lilac", "minecraft:rose_bush", "minecraft:peony", "minecraft:pink_petals"), entry -> true);
            make_block_infestable = builder.comment("\u628a\u65b9\u5757\u52a0\u5165\u6b64\u5217\u8868\u4f7f\u5176\u53ef\u88ab\u611f\u67d3(\u5982 minecraft:dirt)\u3002\u8c28\u614e\u586b\u5199\u3002\u5bf9\u7a7a\u6c14/\u542b\u65b9\u5757\u5b9e\u4f53/\u5df2\u662f\u611f\u67d3\u65b9\u5757/\u5e26\u4e0d\u53ef\u611f\u67d3\u6807\u7b7e\u7684\u65b9\u5757\u65e0\u6548\u3002").defineList("make_block_infestable", Arrays.asList(""), entry -> true);
            builder.pop();

            builder.push("Gravemind Variables");
            gravemind_mass_goal_for_immature_stage = builder.comment("Gravemind(\u90e8\u843d\u610f\u5fd7)\u8fdb\u5165'\u672a\u6210\u719f'\u9636\u6bb5\u6240\u9700\u7684\u751f\u7269\u8d28? (\u9ed8\u8ba4 5000)").defineInRange("gravemind_mass_goal_for_immature_stage",5000, 0, Integer.MAX_VALUE);
            gravemind_mass_goal_for_mature_stage = builder.comment("Gravemind(\u90e8\u843d\u610f\u5fd7)\u8fdb\u5165'\u6210\u719f'\u9636\u6bb5\u6240\u9700\u7684\u751f\u7269\u8d28? (\u9ed8\u8ba4 20000)").defineInRange("gravemind_mass_goal_for_mature_stage",20000, 0, Integer.MAX_VALUE);
            builder.pop();

            builder.push("Sculk Node Variables");
            sculk_node_chunkload_radius = builder.comment("\u5e7d\u533f\u8282\u70b9\u5468\u56f4\u52a0\u8f7d\u591a\u5c11\u533a\u5757? (\u9ed8\u8ba4 15)").defineInRange("sculk_node_chunkload_radius",15, 0, 15);
            sculk_node_spawn_cooldown_minutes = builder.comment("\u53e6\u4e00\u4e2a\u5e7d\u533f\u8282\u70b9\u751f\u6210\u524d\u9700\u95f4\u9694\u591a\u5c11\u5206\u949f? (\u9ed8\u8ba4 120)").defineInRange("sculk_node_spawn_cooldown_minutes",120, 0, Integer.MAX_VALUE);
            enable_node_relocation = builder.comment("\u8282\u70b9\u662f\u5426\u80fd\u81ea\u884c\u8fc1\u79fb\u5230\u65b0\u533a\u57df? (\u9ed8\u8ba4 true)").define("enable_node_relocation",true);
            builder.pop();

            builder.push("Sculk Mite Variables");
            should_sculk_mites_spawn_in_deep_dark = builder.comment("\u5e7d\u533f\u87a8\u662f\u5426\u5728\u6df1\u6697\u4e4b\u57df\u81ea\u7136\u751f\u6210? (\u9ed8\u8ba4 false)").define("should_sculk_mites_spawn_in_deep_dark",false);
            builder.pop();

            builder.push("Sculk Phantom Variables");
            should_phantoms_load_chunks = builder.comment("\u5e7d\u533f\u5e7b\u7ffc\u662f\u5426\u52a0\u8f7d\u533a\u5757? (\u9ed8\u8ba4 true)").define("should_phantoms_load_chunks",true);
            should_sculk_nodes_and_raids_spawn_phantoms = builder.comment("\u5e7d\u533f\u8282\u70b9\u548c\u7a81\u88ad\u662f\u5426\u4f1a\u751f\u6210\u5e7d\u533f\u5e7b\u7ffc? (\u9ed8\u8ba4 true)").define("should_sculk_nodes_and_raids_spawn_phantoms",true);
            should_ancient_node_spawn_phantoms = builder.comment("\u89e6\u53d1\u8fdc\u53e4\u8282\u70b9\u65f6\u662f\u5426\u751f\u6210\u5e7d\u533f\u5e7b\u7ffc? (\u9ed8\u8ba4 true)").define("should_ancient_node_spawn_phantoms",true);
            builder.pop();

            builder.push("Experimental Features");
            experimental_features_enabled = builder.comment("\u662f\u5426\u542f\u7528\u5b9e\u9a8c\u6027\u529f\u80fd? (\u9ed8\u8ba4 false)").define("experimental_features_enabled",false);
            experimental_brood_hatcher_enabled = builder.comment("\u662f\u5426\u542f\u7528\u5b9e\u9a8c\u6027\u7684'\u5de2\u7a74\u5b75\u5316\u8005'? (\u9ed8\u8ba4 false)").define("experimental_brood_hatcher_enabled",false);
            builder.pop();

            builder.push("Sculk Raid Variables");
            sculk_raid_enabled = builder.comment("\u662f\u5426\u542f\u7528\u5e7d\u533f\u7a81\u88ad? (\u9ed8\u8ba4 true)").define("sculk_raid_enabled",true);
            sculk_raid_enderman_scouting_duration_minutes = builder.comment("\u5e7d\u533f\u672b\u5f71\u4eba\u4fa6\u67e5\u6301\u7eed\u591a\u4e45(\u5206\u949f)? (\u9ed8\u8ba4 8)").defineInRange("sculk_raid_enderman_scouting_duration_minutes",8, 0, Integer.MAX_VALUE);
            sculk_raid_global_cooldown_between_raids_minutes = builder.comment("\u4e24\u6b21\u7a81\u88ad\u4e4b\u95f4\u7684\u5168\u5c40\u51b7\u5374(\u5206\u949f)? (\u9ed8\u8ba4 300)").defineInRange("sculk_raid_global_cooldown_between_raids_minutes", 300 , 0, Integer.MAX_VALUE);
            sculk_raid_no_raid_zone_duration_minutes = builder.comment("\u67d0\u5730\u70b9\u7a81\u88ad\u6210\u529f/\u5931\u8d25\u540e, \u8be5\u5730'\u514d\u7a81\u88ad\u533a'\u6301\u7eed\u591a\u5c11\u5206\u949f? (\u9ed8\u8ba4 480)").defineInRange("sculk_raid_no_raid_zone_duration_minutes", 480 , 0, Integer.MAX_VALUE);
            builder.pop();
        }
    }

    public static boolean isExperimentalFeaturesEnabled() {
        return SERVER.experimental_features_enabled.get();
    }

    public static class DataGen {

        public DataGen(ModConfigSpec.Builder builder){

        }

    }

    static {
        Pair<Server, ModConfigSpec> commonSpecPair = new ModConfigSpec.Builder().configure(Server::new);
        SERVER = commonSpecPair.getLeft();
        SERVER_SPEC = commonSpecPair.getRight();

        Pair<DataGen , ModConfigSpec> commonPair = new ModConfigSpec.Builder().configure(DataGen::new);
        DATAGEN = commonPair.getLeft();
        DATAGEN_SPEC = commonPair.getRight();

    }
}
