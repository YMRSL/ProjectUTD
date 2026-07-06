package net.tkg.ModernMayhem.server.item.generic;

import java.util.EnumMap;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.crafting.Ingredient;
import net.tkg.ModernMayhem.server.config.CommonConfig;
import net.tkg.ModernMayhem.server.util.ArmorProperties;

public class GenericStatConfigurableArmorItem
extends ArmorItem {
    public static final int[] BASE_DURABILITY = new int[]{13, 15, 16, 11};
    public final ArmorProperties armorConfig;

    public GenericStatConfigurableArmorItem(final ArmorProperties pConfigs, final ArmorItem.Type pType) {
        super(GenericStatConfigurableArmorItem.makeMaterial(pConfigs), pType, new Item.Properties().stacksTo(1).rarity(Rarity.COMMON).durability(BASE_DURABILITY[pType.getSlot().getIndex()] * pConfigs.getDefaultDurability(pType)));
        this.armorConfig = pConfigs;
    }

    // 1.21.1: ArmorMaterial 变 final record (defense Map, enchantValue, equipSound Holder, repairIngredient, layers, toughness, knockback)。
    // 防御/韧性/击退取静态默认值构建材料 (静态防护); 动态随耐久衰减在 getAttributeModifiers(ItemStack) 里实现。
    private static Holder<ArmorMaterial> makeMaterial(ArmorProperties pConfigs) {
        EnumMap<ArmorItem.Type, Integer> defense = new EnumMap<ArmorItem.Type, Integer>(ArmorItem.Type.class);
        for (ArmorItem.Type t : ArmorItem.Type.values()) {
            defense.put(t, (int)pConfigs.getDefaultProtection(t));
        }
        float toughness = (float)pConfigs.getDefaultToughness(ArmorItem.Type.CHESTPLATE);
        float knockback = (float)pConfigs.getDefaultKnockback(ArmorItem.Type.CHESTPLATE);
        ArmorMaterial material = new ArmorMaterial(defense, 0, pConfigs.getEquipSound(), () -> Ingredient.of(), List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath("mm", pConfigs.getName()))), toughness, knockback);
        return Holder.direct(material);
    }

    public int getMaxDamage(ItemStack stack) {
        return BASE_DURABILITY[this.getType().getSlot().getIndex()] * this.armorConfig.getDurabilityMultiplier(this.getType());
    }

    public boolean canBeDepleted() {
        return this.getMaxDamage(ItemStack.EMPTY) > 0;
    }

    public <T extends LivingEntity> int damageItem(ItemStack stack, int amount, T entity, Consumer<Item> onBroken) {
        int newDamage;
        if (((Boolean)CommonConfig.ENABLE_DYNAMIC_ARMOR_STATS.get()).booleanValue() && (newDamage = stack.getDamageValue() + amount) >= stack.getMaxDamage()) {
            stack.setDamageValue(stack.getMaxDamage());
            return 0;
        }
        return super.damageItem(stack, amount, entity, onBroken);
    }

    // 动态护甲属性: 1.21.1 删了 getAttributeModifiers(EquipmentSlot,ItemStack) 覆盖点; 改用 NeoForge per-stack
    // getAttributeModifiers(ItemStack) 返回 ItemAttributeModifiers, 按当前耐久百分比缩放防护值 (开关同 ENABLE_DYNAMIC_ARMOR_STATS)。
    @Override
    public ItemAttributeModifiers getDefaultAttributeModifiers(ItemStack stack) {
        if (!((Boolean)CommonConfig.ENABLE_DYNAMIC_ARMOR_STATS.get()).booleanValue()) {
            return super.getDefaultAttributeModifiers(stack);
        }
        ArmorItem.Type type = this.getType();
        double protection = this.armorConfig.getProtectionAmount(type);
        double toughness = this.armorConfig.getToughnessAmount(type);
        double knockback = this.armorConfig.getKnockbackResistance(type);
        int maxD = stack.getMaxDamage();
        if (maxD > 0) {
            double durabilityPercent = Math.max(0.0, 1.0 - (double)stack.getDamageValue() / (double)maxD);
            double factor = 0.1 + 0.9 * durabilityPercent;
            protection *= factor;
            toughness *= factor;
            knockback *= factor;
        }
        EquipmentSlotGroup group = EquipmentSlotGroup.bySlot(type.getSlot());
        ResourceLocation base = ResourceLocation.fromNamespaceAndPath("mm", "armor_" + type.getName());
        ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder();
        if (protection > 0.0) {
            builder.add(Attributes.ARMOR, new AttributeModifier(base.withSuffix("_armor"), protection, AttributeModifier.Operation.ADD_VALUE), group);
        }
        if (toughness > 0.0) {
            builder.add(Attributes.ARMOR_TOUGHNESS, new AttributeModifier(base.withSuffix("_toughness"), toughness, AttributeModifier.Operation.ADD_VALUE), group);
        }
        if (knockback > 0.0) {
            builder.add(Attributes.KNOCKBACK_RESISTANCE, new AttributeModifier(base.withSuffix("_knockback"), knockback, AttributeModifier.Operation.ADD_VALUE), group);
        }
        return builder.build();
    }
}
