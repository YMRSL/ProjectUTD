package net.mcreator.survivalinstinct.item;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

public class SmithingTemplatePrototypeItem
extends Item {
    public SmithingTemplatePrototypeItem() {
        super(new Item.Properties().stacksTo(64).rarity(Rarity.COMMON));
    }

    public void appendHoverText(ItemStack itemstack, Item.TooltipContext context, List<Component> list, TooltipFlag flag) {
        super.appendHoverText(itemstack, context, list, flag);
        list.add((Component)Component.literal((String)"\u00a77Juggernaut Upgrade"));
        list.add((Component)Component.literal((String)""));
        list.add((Component)Component.literal((String)"\u00a77Applies to:"));
        list.add((Component)Component.literal((String)" \u00a79Juggernaut Armor"));
        list.add((Component)Component.literal((String)"\u00a77Ingredients:"));
        list.add((Component)Component.literal((String)" \u00a79Exoskeleton Armor"));
    }
}

