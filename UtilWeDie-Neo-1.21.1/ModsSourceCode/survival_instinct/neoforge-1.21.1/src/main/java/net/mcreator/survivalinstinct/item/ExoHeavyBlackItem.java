package net.mcreator.survivalinstinct.item;
import net.minecraft.core.Holder;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.mcreator.survivalinstinct.client.model.Modelexo_heavy_armor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;

public abstract class ExoHeavyBlackItem
extends ArmorItem {
    public ExoHeavyBlackItem(ArmorItem.Type type, Item.Properties properties) {
        super(Holder.direct(new ArmorMaterial(java.util.Map.of(ArmorItem.Type.BOOTS, 4, ArmorItem.Type.LEGGINGS, 8, ArmorItem.Type.CHESTPLATE, 10, ArmorItem.Type.HELMET, 5), 16, BuiltInRegistries.SOUND_EVENT.wrapAsHolder(BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("item.armor.equip_leather"))), () -> Ingredient.of(), java.util.List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath("survival_instinct", "exo_heavy_armor_black"))), 4.0f, 0.1f)), type, properties.durability(type.getDurability(43)));
    }

    public static class Boots
    extends ExoHeavyBlackItem {
        public Boots() {
            super(ArmorItem.Type.BOOTS, new Item.Properties());
        }

        public void initializeClient(Consumer<IClientItemExtensions> consumer) {
            consumer.accept(new IClientItemExtensions(){

                @OnlyIn(value=Dist.CLIENT)
                public HumanoidModel getHumanoidArmorModel(LivingEntity living, ItemStack stack, EquipmentSlot slot, HumanoidModel defaultModel) {
                    HumanoidModel armorModel = new HumanoidModel(new ModelPart(Collections.emptyList(), Map.of("left_leg", new Modelexo_heavy_armor<net.minecraft.world.entity.Entity>((ModelPart)Minecraft.getInstance().getEntityModels().bakeLayer((ModelLayerLocation)Modelexo_heavy_armor.LAYER_LOCATION)).left_shoe, "right_leg", new Modelexo_heavy_armor<net.minecraft.world.entity.Entity>((ModelPart)Minecraft.getInstance().getEntityModels().bakeLayer((ModelLayerLocation)Modelexo_heavy_armor.LAYER_LOCATION)).right_shoe, "head", new ModelPart(Collections.emptyList(), Collections.emptyMap()), "hat", new ModelPart(Collections.emptyList(), Collections.emptyMap()), "body", new ModelPart(Collections.emptyList(), Collections.emptyMap()), "right_arm", new ModelPart(Collections.emptyList(), Collections.emptyMap()), "left_arm", new ModelPart(Collections.emptyList(), Collections.emptyMap()))));
                    armorModel.crouching = living.isShiftKeyDown();
                    armorModel.riding = defaultModel.riding;
                    armorModel.young = living.isBaby();
                    return armorModel;
                }
            });
        }

        public void appendHoverText(ItemStack itemstack, Item.TooltipContext context, List<Component> list, TooltipFlag flag) {
            super.appendHoverText(itemstack, context, list, flag);
            list.add((Component)Component.literal((String)"\u00a77Bonus Armor set:"));
            list.add((Component)Component.literal((String)" \u00a79Resistance II, Strength II, Night Vision"));
            list.add((Component)Component.literal((String)" \u00a79While crouching you will get Jump Boost IV"));
            list.add((Component)Component.literal((String)"\u00a77Ability Armor set:"));
            list.add((Component)Component.literal((String)" \u00a79Press X to dash in the direction you're facing"));
        }

        public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
            return "survival_instinct:textures/entities/exo_heavy_armor_black.png";
        }
    }

    public static class Leggings
    extends ExoHeavyBlackItem {
        public Leggings() {
            super(ArmorItem.Type.LEGGINGS, new Item.Properties());
        }

        public void initializeClient(Consumer<IClientItemExtensions> consumer) {
            consumer.accept(new IClientItemExtensions(){

                @OnlyIn(value=Dist.CLIENT)
                public HumanoidModel getHumanoidArmorModel(LivingEntity living, ItemStack stack, EquipmentSlot slot, HumanoidModel defaultModel) {
                    HumanoidModel armorModel = new HumanoidModel(new ModelPart(Collections.emptyList(), Map.of("left_leg", new Modelexo_heavy_armor<net.minecraft.world.entity.Entity>((ModelPart)Minecraft.getInstance().getEntityModels().bakeLayer((ModelLayerLocation)Modelexo_heavy_armor.LAYER_LOCATION)).left_leg, "right_leg", new Modelexo_heavy_armor<net.minecraft.world.entity.Entity>((ModelPart)Minecraft.getInstance().getEntityModels().bakeLayer((ModelLayerLocation)Modelexo_heavy_armor.LAYER_LOCATION)).right_leg, "head", new ModelPart(Collections.emptyList(), Collections.emptyMap()), "hat", new ModelPart(Collections.emptyList(), Collections.emptyMap()), "body", new ModelPart(Collections.emptyList(), Collections.emptyMap()), "right_arm", new ModelPart(Collections.emptyList(), Collections.emptyMap()), "left_arm", new ModelPart(Collections.emptyList(), Collections.emptyMap()))));
                    armorModel.crouching = living.isShiftKeyDown();
                    armorModel.riding = defaultModel.riding;
                    armorModel.young = living.isBaby();
                    return armorModel;
                }
            });
        }

        public void appendHoverText(ItemStack itemstack, Item.TooltipContext context, List<Component> list, TooltipFlag flag) {
            super.appendHoverText(itemstack, context, list, flag);
            list.add((Component)Component.literal((String)"\u00a77Bonus Armor set:"));
            list.add((Component)Component.literal((String)" \u00a79Resistance II, Strength II, Night Vision"));
            list.add((Component)Component.literal((String)" \u00a79While crouching you will get Jump Boost IV"));
            list.add((Component)Component.literal((String)"\u00a77Ability Armor set:"));
            list.add((Component)Component.literal((String)" \u00a79Press X to dash in the direction you're facing"));
        }

        public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
            return "survival_instinct:textures/entities/exo_heavy_armor_black.png";
        }
    }

    public static class Chestplate
    extends ExoHeavyBlackItem {
        public Chestplate() {
            super(ArmorItem.Type.CHESTPLATE, new Item.Properties());
        }

        public void initializeClient(Consumer<IClientItemExtensions> consumer) {
            consumer.accept(new IClientItemExtensions(){

                @OnlyIn(value=Dist.CLIENT)
                public HumanoidModel getHumanoidArmorModel(LivingEntity living, ItemStack stack, EquipmentSlot slot, HumanoidModel defaultModel) {
                    HumanoidModel armorModel = new HumanoidModel(new ModelPart(Collections.emptyList(), Map.of("body", new Modelexo_heavy_armor<net.minecraft.world.entity.Entity>((ModelPart)Minecraft.getInstance().getEntityModels().bakeLayer((ModelLayerLocation)Modelexo_heavy_armor.LAYER_LOCATION)).body, "left_arm", new Modelexo_heavy_armor<net.minecraft.world.entity.Entity>((ModelPart)Minecraft.getInstance().getEntityModels().bakeLayer((ModelLayerLocation)Modelexo_heavy_armor.LAYER_LOCATION)).left_arm, "right_arm", new Modelexo_heavy_armor<net.minecraft.world.entity.Entity>((ModelPart)Minecraft.getInstance().getEntityModels().bakeLayer((ModelLayerLocation)Modelexo_heavy_armor.LAYER_LOCATION)).right_arm, "head", new ModelPart(Collections.emptyList(), Collections.emptyMap()), "hat", new ModelPart(Collections.emptyList(), Collections.emptyMap()), "right_leg", new ModelPart(Collections.emptyList(), Collections.emptyMap()), "left_leg", new ModelPart(Collections.emptyList(), Collections.emptyMap()))));
                    armorModel.crouching = living.isShiftKeyDown();
                    armorModel.riding = defaultModel.riding;
                    armorModel.young = living.isBaby();
                    return armorModel;
                }
            });
        }

        public void appendHoverText(ItemStack itemstack, Item.TooltipContext context, List<Component> list, TooltipFlag flag) {
            super.appendHoverText(itemstack, context, list, flag);
            list.add((Component)Component.literal((String)"\u00a77Bonus Armor set:"));
            list.add((Component)Component.literal((String)" \u00a79Resistance II, Strength II, Night Vision"));
            list.add((Component)Component.literal((String)" \u00a79While crouching you will get Jump Boost IV"));
            list.add((Component)Component.literal((String)"\u00a77Ability Armor set:"));
            list.add((Component)Component.literal((String)" \u00a79Press X to dash in the direction you're facing"));
        }

        public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
            return "survival_instinct:textures/entities/exo_heavy_armor_black.png";
        }
    }

    public static class Helmet
    extends ExoHeavyBlackItem {
        public Helmet() {
            super(ArmorItem.Type.HELMET, new Item.Properties());
        }

        public void initializeClient(Consumer<IClientItemExtensions> consumer) {
            consumer.accept(new IClientItemExtensions(){

                public HumanoidModel getHumanoidArmorModel(LivingEntity living, ItemStack stack, EquipmentSlot slot, HumanoidModel defaultModel) {
                    HumanoidModel armorModel = new HumanoidModel(new ModelPart(Collections.emptyList(), Map.of("head", new Modelexo_heavy_armor<net.minecraft.world.entity.Entity>((ModelPart)Minecraft.getInstance().getEntityModels().bakeLayer((ModelLayerLocation)Modelexo_heavy_armor.LAYER_LOCATION)).head, "hat", new ModelPart(Collections.emptyList(), Collections.emptyMap()), "body", new ModelPart(Collections.emptyList(), Collections.emptyMap()), "right_arm", new ModelPart(Collections.emptyList(), Collections.emptyMap()), "left_arm", new ModelPart(Collections.emptyList(), Collections.emptyMap()), "right_leg", new ModelPart(Collections.emptyList(), Collections.emptyMap()), "left_leg", new ModelPart(Collections.emptyList(), Collections.emptyMap()))));
                    armorModel.crouching = living.isShiftKeyDown();
                    armorModel.riding = defaultModel.riding;
                    armorModel.young = living.isBaby();
                    return armorModel;
                }
            });
        }

        public void appendHoverText(ItemStack itemstack, Item.TooltipContext context, List<Component> list, TooltipFlag flag) {
            super.appendHoverText(itemstack, context, list, flag);
            list.add((Component)Component.literal((String)"\u00a77Bonus Armor set:"));
            list.add((Component)Component.literal((String)" \u00a79Resistance II, Strength II, Night Vision"));
            list.add((Component)Component.literal((String)" \u00a79While crouching you will get Jump Boost IV"));
            list.add((Component)Component.literal((String)"\u00a77Ability Armor set:"));
            list.add((Component)Component.literal((String)" \u00a79Press X to dash in the direction you're facing"));
        }

        public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
            return "survival_instinct:textures/entities/exo_heavy_armor_black.png";
        }
    }
}

