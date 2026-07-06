package com.scarasol.zombiekit.item.armor;

import com.scarasol.zombiekit.ZombieKitMod;
import com.scarasol.zombiekit.init.ZombieKitItems;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * 1.20.1 ArmorMaterial 是接口（本类是枚举实现）。1.21 ArmorMaterial 是 record，
 * 注册进 BuiltInRegistries.ARMOR_MATERIAL，护甲构造收 Holder&lt;ArmorMaterial&gt;。
 *
 * 防御值/附魔值/装备音效/修复材料/护甲层(贴图)/坚韧/击退抗性 进 record。
 * 耐久不再属于材料：1.21 由物品 Properties.durability 决定，故这里保留每材料的
 * 耐久倍率，并提供 applyDurability(...) 给护甲物品构造时套用（倍率沿用上游）。
 *
 * 注意：伪装套(TACTICAL/RIOT)四种贴图变体在 1.20 靠 getArmorTexture(camouflage) 切换，
 * 1.21 该方法已移除，贴图/模型改由客户端 RegisterClientExtensionsEvent + 自定义
 * HumanoidModel/GeoArmorRenderer 处理（客户端代理负责），读取 getCamouflage() 选贴图。
 * 这里的 Layer 仅作占位（标准贴图）。
 */
public class ModArmorMaterial {
    public static final DeferredRegister<ArmorMaterial> REGISTRY = DeferredRegister.create(BuiltInRegistries.ARMOR_MATERIAL, ZombieKitMod.MODID);

    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> SKIING = REGISTRY.register("skiing",
            () -> build(defense(2, 6, 5, 2), 9, SoundEvents.ARMOR_EQUIP_LEATHER, () -> Ingredient.of(ZombieKitItems.CLOTH.get()), "skiing", 1f, 0f));
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> TACTICAL = REGISTRY.register("tactical",
            () -> build(defense(4, 7, 7, 5), 10, SoundEvents.ARMOR_EQUIP_DIAMOND, () -> Ingredient.of(ZombieKitItems.CLOTH.get()), "tactical", 2f, 0f));
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> RIOT = REGISTRY.register("riot",
            () -> build(defense(4, 7, 7, 5), 10, SoundEvents.ARMOR_EQUIP_DIAMOND, () -> Ingredient.of(ZombieKitItems.SPECIAL_STEEL_SHEET.get()), "riot", 3f, 0.1f));
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> BOMB = REGISTRY.register("bomb",
            () -> build(defense(11, 15, 16, 13), 10, SoundEvents.ARMOR_EQUIP_NETHERITE, () -> Ingredient.EMPTY, "bomb", 5f, 0.25f));
    public static final DeferredHolder<ArmorMaterial, ArmorMaterial> EXO = REGISTRY.register("exo",
            () -> build(defense(4, 7, 8, 6), 15, SoundEvents.ARMOR_EQUIP_NETHERITE, () -> Ingredient.of(ZombieKitItems.BULLETPROOF_INSERT.get()), "exo", 4f, 0.2f));

    // 耐久倍率（上游 durabilityMultiplier），按材料注册名映射
    private static final Map<String, Integer> DURABILITY_MULTIPLIER = Map.of(
            "skiing", 15,
            "tactical", 33,
            "riot", 35,
            "bomb", 40,
            "exo", 37
    );

    private static ArmorMaterial build(EnumMap<ArmorItem.Type, Integer> defense, int enchantmentValue, Holder<SoundEvent> sound, java.util.function.Supplier<Ingredient> repair, String name, float toughness, float knockbackResistance) {
        return new ArmorMaterial(defense, enchantmentValue, sound, repair,
                List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(ZombieKitMod.MODID, name))),
                toughness, knockbackResistance);
    }

    private static EnumMap<ArmorItem.Type, Integer> defense(int boots, int leggings, int chestplate, int helmet) {
        EnumMap<ArmorItem.Type, Integer> map = new EnumMap<>(ArmorItem.Type.class);
        map.put(ArmorItem.Type.BOOTS, boots);
        map.put(ArmorItem.Type.LEGGINGS, leggings);
        map.put(ArmorItem.Type.CHESTPLATE, chestplate);
        map.put(ArmorItem.Type.HELMET, helmet);
        map.put(ArmorItem.Type.BODY, chestplate);
        return map;
    }

    /** 按材料的耐久倍率套用到护甲物品 Properties（1.21 耐久在物品侧）。 */
    public static net.minecraft.world.item.Item.Properties applyDurability(Holder<ArmorMaterial> material, net.minecraft.world.item.Item.Properties properties, ArmorItem.Type type) {
        Integer mult = material.unwrapKey()
                .map(key -> DURABILITY_MULTIPLIER.get(key.location().getPath()))
                .orElse(null);
        if (mult != null) {
            return properties.durability(type.getDurability(mult));
        }
        return properties;
    }
}
