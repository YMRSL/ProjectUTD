package com.scarasol.zombiekit.item.bonus;

import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.List;

/**
 * sun_chestplate 贴图(sun_chestplate__layer_1.png)在 1.21 由客户端代理通过
 * 自定义模型/层渲染，getArmorTexture 已移除。
 */
public class WhiteFood extends ArmorItem {
    public WhiteFood(Holder<ArmorMaterial> armorMaterial, Type equipmentSlot, Properties properties) {
        super(armorMaterial, equipmentSlot, properties);
    }

    @Override
    public void appendHoverText(ItemStack itemstack, TooltipContext context, List<Component> list, TooltipFlag flag) {
        super.appendHoverText(itemstack, context, list, flag);
        list.add(Component.translatable("item.zombiekit.white_food.description_1"));
        list.add(Component.translatable("item.zombiekit.white_food.description_2"));
    }

    @OnlyIn(Dist.CLIENT)
    @Override
    public boolean isFoil(ItemStack itemstack) {
        return true;
    }

}
