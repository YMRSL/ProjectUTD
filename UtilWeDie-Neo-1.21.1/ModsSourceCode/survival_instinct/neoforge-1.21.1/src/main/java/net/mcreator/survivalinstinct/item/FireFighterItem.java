package net.mcreator.survivalinstinct.item;
import net.minecraft.core.Holder;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.mcreator.survivalinstinct.client.model.Modelfire_fighter;
import net.mcreator.survivalinstinct.init.SurvivalInstinctModItems;
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
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;

public abstract class FireFighterItem
extends ArmorItem {
    public FireFighterItem(ArmorItem.Type type, Item.Properties properties) {
        super(Holder.direct(new ArmorMaterial(java.util.Map.of(ArmorItem.Type.BOOTS, 3, ArmorItem.Type.LEGGINGS, 6, ArmorItem.Type.CHESTPLATE, 7, ArmorItem.Type.HELMET, 3), 12, BuiltInRegistries.SOUND_EVENT.wrapAsHolder(BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("item.armor.equip_leather"))), () -> Ingredient.of((ItemStack[])new ItemStack[]{new ItemStack((ItemLike)SurvivalInstinctModItems.TIER_II_KEVLAR.get()), new ItemStack((ItemLike)SurvivalInstinctModItems.THREAD.get())}), java.util.List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath("survival_instinct", "fire_fighter"))), 1.0f, 0.0f)), type, properties.durability(type.getDurability(26)));
    }

    public static class Boots
    extends FireFighterItem {
        public Boots() {
            super(ArmorItem.Type.BOOTS, new Item.Properties());
        }

        public void initializeClient(Consumer<IClientItemExtensions> consumer) {
            consumer.accept(new IClientItemExtensions(){

                @OnlyIn(value=Dist.CLIENT)
                public HumanoidModel getHumanoidArmorModel(LivingEntity living, ItemStack stack, EquipmentSlot slot, HumanoidModel defaultModel) {
                    HumanoidModel armorModel = new HumanoidModel(new ModelPart(Collections.emptyList(), Map.of("left_leg", new Modelfire_fighter<net.minecraft.world.entity.Entity>((ModelPart)Minecraft.getInstance().getEntityModels().bakeLayer((ModelLayerLocation)Modelfire_fighter.LAYER_LOCATION)).left_shoe, "right_leg", new Modelfire_fighter<net.minecraft.world.entity.Entity>((ModelPart)Minecraft.getInstance().getEntityModels().bakeLayer((ModelLayerLocation)Modelfire_fighter.LAYER_LOCATION)).right_shoe, "head", new ModelPart(Collections.emptyList(), Collections.emptyMap()), "hat", new ModelPart(Collections.emptyList(), Collections.emptyMap()), "body", new ModelPart(Collections.emptyList(), Collections.emptyMap()), "right_arm", new ModelPart(Collections.emptyList(), Collections.emptyMap()), "left_arm", new ModelPart(Collections.emptyList(), Collections.emptyMap()))));
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
            list.add((Component)Component.literal((String)" \u00a79Extinguishes the fire"));
        }

        public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
            return "survival_instinct:textures/entities/fire_fighter.png";
        }
    }

    public static class Leggings
    extends FireFighterItem {
        public Leggings() {
            super(ArmorItem.Type.LEGGINGS, new Item.Properties());
        }

        public void initializeClient(Consumer<IClientItemExtensions> consumer) {
            consumer.accept(new IClientItemExtensions(){

                @OnlyIn(value=Dist.CLIENT)
                public HumanoidModel getHumanoidArmorModel(LivingEntity living, ItemStack stack, EquipmentSlot slot, HumanoidModel defaultModel) {
                    HumanoidModel armorModel = new HumanoidModel(new ModelPart(Collections.emptyList(), Map.of("left_leg", new Modelfire_fighter<net.minecraft.world.entity.Entity>((ModelPart)Minecraft.getInstance().getEntityModels().bakeLayer((ModelLayerLocation)Modelfire_fighter.LAYER_LOCATION)).left_leg, "right_leg", new Modelfire_fighter<net.minecraft.world.entity.Entity>((ModelPart)Minecraft.getInstance().getEntityModels().bakeLayer((ModelLayerLocation)Modelfire_fighter.LAYER_LOCATION)).right_leg, "head", new ModelPart(Collections.emptyList(), Collections.emptyMap()), "hat", new ModelPart(Collections.emptyList(), Collections.emptyMap()), "body", new ModelPart(Collections.emptyList(), Collections.emptyMap()), "right_arm", new ModelPart(Collections.emptyList(), Collections.emptyMap()), "left_arm", new ModelPart(Collections.emptyList(), Collections.emptyMap()))));
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
            list.add((Component)Component.literal((String)" \u00a79Extinguishes the fire"));
        }

        public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
            return "survival_instinct:textures/entities/fire_fighter.png";
        }
    }

    public static class Chestplate
    extends FireFighterItem {
        public Chestplate() {
            super(ArmorItem.Type.CHESTPLATE, new Item.Properties());
        }

        public void initializeClient(Consumer<IClientItemExtensions> consumer) {
            consumer.accept(new IClientItemExtensions(){

                @OnlyIn(value=Dist.CLIENT)
                public HumanoidModel getHumanoidArmorModel(LivingEntity living, ItemStack stack, EquipmentSlot slot, HumanoidModel defaultModel) {
                    HumanoidModel armorModel = new HumanoidModel(new ModelPart(Collections.emptyList(), Map.of("body", new Modelfire_fighter<net.minecraft.world.entity.Entity>((ModelPart)Minecraft.getInstance().getEntityModels().bakeLayer((ModelLayerLocation)Modelfire_fighter.LAYER_LOCATION)).body, "left_arm", new Modelfire_fighter<net.minecraft.world.entity.Entity>((ModelPart)Minecraft.getInstance().getEntityModels().bakeLayer((ModelLayerLocation)Modelfire_fighter.LAYER_LOCATION)).left_arm, "right_arm", new Modelfire_fighter<net.minecraft.world.entity.Entity>((ModelPart)Minecraft.getInstance().getEntityModels().bakeLayer((ModelLayerLocation)Modelfire_fighter.LAYER_LOCATION)).right_arm, "head", new ModelPart(Collections.emptyList(), Collections.emptyMap()), "hat", new ModelPart(Collections.emptyList(), Collections.emptyMap()), "right_leg", new ModelPart(Collections.emptyList(), Collections.emptyMap()), "left_leg", new ModelPart(Collections.emptyList(), Collections.emptyMap()))));
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
            list.add((Component)Component.literal((String)" \u00a79Extinguishes the fire"));
        }

        public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
            return "survival_instinct:textures/entities/fire_fighter.png";
        }
    }

    public static class Helmet
    extends FireFighterItem {
        public Helmet() {
            super(ArmorItem.Type.HELMET, new Item.Properties());
        }

        public void initializeClient(Consumer<IClientItemExtensions> consumer) {
            consumer.accept(new IClientItemExtensions(){

                public HumanoidModel getHumanoidArmorModel(LivingEntity living, ItemStack stack, EquipmentSlot slot, HumanoidModel defaultModel) {
                    HumanoidModel armorModel = new HumanoidModel(new ModelPart(Collections.emptyList(), Map.of("head", new Modelfire_fighter<net.minecraft.world.entity.Entity>((ModelPart)Minecraft.getInstance().getEntityModels().bakeLayer((ModelLayerLocation)Modelfire_fighter.LAYER_LOCATION)).head, "hat", new ModelPart(Collections.emptyList(), Collections.emptyMap()), "body", new ModelPart(Collections.emptyList(), Collections.emptyMap()), "right_arm", new ModelPart(Collections.emptyList(), Collections.emptyMap()), "left_arm", new ModelPart(Collections.emptyList(), Collections.emptyMap()), "right_leg", new ModelPart(Collections.emptyList(), Collections.emptyMap()), "left_leg", new ModelPart(Collections.emptyList(), Collections.emptyMap()))));
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
            list.add((Component)Component.literal((String)" \u00a79Extinguishes the fire"));
        }

        public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
            return "survival_instinct:textures/entities/fire_fighter.png";
        }
    }
}

