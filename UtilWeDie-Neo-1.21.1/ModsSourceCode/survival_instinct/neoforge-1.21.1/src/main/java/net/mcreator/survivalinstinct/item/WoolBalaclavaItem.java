package net.mcreator.survivalinstinct.item;
import net.minecraft.core.Holder;

import java.util.Collections;
import java.util.Map;
import java.util.function.Consumer;
import net.mcreator.survivalinstinct.client.model.Modelbalaclava;
import net.mcreator.survivalinstinct.init.SurvivalInstinctModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;

public abstract class WoolBalaclavaItem
extends ArmorItem {
    public WoolBalaclavaItem(ArmorItem.Type type, Item.Properties properties) {
        super(Holder.direct(new ArmorMaterial(java.util.Map.of(ArmorItem.Type.BOOTS, 2, ArmorItem.Type.LEGGINGS, 5, ArmorItem.Type.CHESTPLATE, 6, ArmorItem.Type.HELMET, 1), 12, BuiltInRegistries.SOUND_EVENT.wrapAsHolder(BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("item.armor.equip_leather"))), () -> Ingredient.of((ItemStack[])new ItemStack[]{new ItemStack((ItemLike)Blocks.BLACK_WOOL), new ItemStack((ItemLike)SurvivalInstinctModItems.THREAD.get())}), java.util.List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath("survival_instinct", "wool_balaclava"))), 0.0f, 0.0f)), type, properties.durability(type.getDurability(10)));
    }

    public static class Helmet
    extends WoolBalaclavaItem {
        public Helmet() {
            super(ArmorItem.Type.HELMET, new Item.Properties());
        }

        public void initializeClient(Consumer<IClientItemExtensions> consumer) {
            consumer.accept(new IClientItemExtensions(){

                public HumanoidModel getHumanoidArmorModel(LivingEntity living, ItemStack stack, EquipmentSlot slot, HumanoidModel defaultModel) {
                    HumanoidModel armorModel = new HumanoidModel(new ModelPart(Collections.emptyList(), Map.of("head", new Modelbalaclava<net.minecraft.world.entity.Entity>((ModelPart)Minecraft.getInstance().getEntityModels().bakeLayer((ModelLayerLocation)Modelbalaclava.LAYER_LOCATION)).head, "hat", new ModelPart(Collections.emptyList(), Collections.emptyMap()), "body", new ModelPart(Collections.emptyList(), Collections.emptyMap()), "right_arm", new ModelPart(Collections.emptyList(), Collections.emptyMap()), "left_arm", new ModelPart(Collections.emptyList(), Collections.emptyMap()), "right_leg", new ModelPart(Collections.emptyList(), Collections.emptyMap()), "left_leg", new ModelPart(Collections.emptyList(), Collections.emptyMap()))));
                    armorModel.crouching = living.isShiftKeyDown();
                    armorModel.riding = defaultModel.riding;
                    armorModel.young = living.isBaby();
                    return armorModel;
                }
            });
        }

        public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
            return "survival_instinct:textures/entities/wool_balaclava.png";
        }
    }
}

