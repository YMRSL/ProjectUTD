package com.yitianys.BlockZ.init;

import com.yitianys.BlockZ.BlockZ;
import com.yitianys.BlockZ.item.BackpackItem;
import com.yitianys.BlockZ.item.ClothingItem;
import com.yitianys.BlockZ.item.LockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(BlockZ.MODID);

    // --- 背包 ---
    public static final DeferredItem<Item> BACKPACK_COYOTE = ITEMS.registerItem("backpack_coyote",
        BackpackItem::new, new Item.Properties().stacksTo(1));
    public static final DeferredItem<Item> BACKPACK_ALICE = ITEMS.registerItem("backpack_alice",
        BackpackItem::new, new Item.Properties().stacksTo(1));
    public static final DeferredItem<Item> BACKPACK_CZECH = ITEMS.registerItem("backpack_czech",
        BackpackItem::new, new Item.Properties().stacksTo(1));
    public static final DeferredItem<Item> BACKPACK_CZECHPOUCH = ITEMS.registerItem("backpack_czechpouch",
        BackpackItem::new, new Item.Properties().stacksTo(1));
    public static final DeferredItem<Item> BACKPACK_PATROLPACK = ITEMS.registerItem("backpack_patrolpack",
        BackpackItem::new, new Item.Properties().stacksTo(1));

    // --- 衣服装备 ---
    // 背心
    public static final DeferredItem<Item> VEST_0 = ITEMS.registerItem("vest_0",
        props -> new ClothingItem(props, ClothingItem.ClothingType.VEST), new Item.Properties().stacksTo(1));

    // 手套
    public static final DeferredItem<Item> GLOVES_0 = ITEMS.registerItem("gloves_0",
        props -> new ClothingItem(props, ClothingItem.ClothingType.GLOVES), new Item.Properties().stacksTo(1));
    public static final DeferredItem<Item> GLOVES_1 = ITEMS.registerItem("gloves_1",
        props -> new ClothingItem(props, ClothingItem.ClothingType.GLOVES), new Item.Properties().stacksTo(1));
    public static final DeferredItem<Item> GLOVES_2 = ITEMS.registerItem("gloves_2",
        props -> new ClothingItem(props, ClothingItem.ClothingType.GLOVES), new Item.Properties().stacksTo(1));

    // 上衣
    public static final DeferredItem<Item> SHIRT_0 = ITEMS.registerItem("shirt_0",
        props -> new ClothingItem(props, ClothingItem.ClothingType.SHIRT), new Item.Properties().stacksTo(1));
    public static final DeferredItem<Item> SHIRT_1 = ITEMS.registerItem("shirt_1",
        props -> new ClothingItem(props, ClothingItem.ClothingType.SHIRT), new Item.Properties().stacksTo(1));
    public static final DeferredItem<Item> SHIRT_2 = ITEMS.registerItem("shirt_2",
        props -> new ClothingItem(props, ClothingItem.ClothingType.SHIRT), new Item.Properties().stacksTo(1));
    public static final DeferredItem<Item> SHIRT_3 = ITEMS.registerItem("shirt_3",
        props -> new ClothingItem(props, ClothingItem.ClothingType.SHIRT), new Item.Properties().stacksTo(1));
    public static final DeferredItem<Item> SHIRT_4 = ITEMS.registerItem("shirt_4",
        props -> new ClothingItem(props, ClothingItem.ClothingType.SHIRT), new Item.Properties().stacksTo(1));
    public static final DeferredItem<Item> SHIRT_5 = ITEMS.registerItem("shirt_5",
        props -> new ClothingItem(props, ClothingItem.ClothingType.SHIRT), new Item.Properties().stacksTo(1));
    public static final DeferredItem<Item> SHIRT_6 = ITEMS.registerItem("shirt_6",
        props -> new ClothingItem(props, ClothingItem.ClothingType.SHIRT), new Item.Properties().stacksTo(1));
    public static final DeferredItem<Item> SHIRT_7 = ITEMS.registerItem("shirt_7",
        props -> new ClothingItem(props, ClothingItem.ClothingType.SHIRT), new Item.Properties().stacksTo(1));
    public static final DeferredItem<Item> SHIRT_8 = ITEMS.registerItem("shirt_8",
        props -> new ClothingItem(props, ClothingItem.ClothingType.SHIRT), new Item.Properties().stacksTo(1));
    public static final DeferredItem<Item> SHIRT_9 = ITEMS.registerItem("shirt_9",
        props -> new ClothingItem(props, ClothingItem.ClothingType.SHIRT), new Item.Properties().stacksTo(1));
    public static final DeferredItem<Item> SHIRT_10 = ITEMS.registerItem("shirt_10",
        props -> new ClothingItem(props, ClothingItem.ClothingType.SHIRT), new Item.Properties().stacksTo(1));
    public static final DeferredItem<Item> SHIRT_11 = ITEMS.registerItem("shirt_11",
        props -> new ClothingItem(props, ClothingItem.ClothingType.SHIRT), new Item.Properties().stacksTo(1));
    public static final DeferredItem<Item> SHIRT_12 = ITEMS.registerItem("shirt_12",
        props -> new ClothingItem(props, ClothingItem.ClothingType.SHIRT), new Item.Properties().stacksTo(1));
    public static final DeferredItem<Item> SHIRT_13 = ITEMS.registerItem("shirt_13",
        props -> new ClothingItem(props, ClothingItem.ClothingType.SHIRT), new Item.Properties().stacksTo(1));
    public static final DeferredItem<Item> SHIRT_14 = ITEMS.registerItem("shirt_14",
        props -> new ClothingItem(props, ClothingItem.ClothingType.SHIRT), new Item.Properties().stacksTo(1));

    // 裤子
    public static final DeferredItem<Item> PANTS_0 = ITEMS.registerItem("pants_0",
        props -> new ClothingItem(props, ClothingItem.ClothingType.PANTS), new Item.Properties().stacksTo(1));
    public static final DeferredItem<Item> PANTS_1 = ITEMS.registerItem("pants_1",
        props -> new ClothingItem(props, ClothingItem.ClothingType.PANTS), new Item.Properties().stacksTo(1));
    public static final DeferredItem<Item> PANTS_2 = ITEMS.registerItem("pants_2",
        props -> new ClothingItem(props, ClothingItem.ClothingType.PANTS), new Item.Properties().stacksTo(1));
    public static final DeferredItem<Item> PANTS_3 = ITEMS.registerItem("pants_3",
        props -> new ClothingItem(props, ClothingItem.ClothingType.PANTS), new Item.Properties().stacksTo(1));
    public static final DeferredItem<Item> PANTS_4 = ITEMS.registerItem("pants_4",
        props -> new ClothingItem(props, ClothingItem.ClothingType.PANTS), new Item.Properties().stacksTo(1));
    public static final DeferredItem<Item> PANTS_5 = ITEMS.registerItem("pants_5",
        props -> new ClothingItem(props, ClothingItem.ClothingType.PANTS), new Item.Properties().stacksTo(1));

    // 鞋子
    public static final DeferredItem<Item> SHOES_0 = ITEMS.registerItem("shoes_0",
        props -> new ClothingItem(props, ClothingItem.ClothingType.SHOES), new Item.Properties().stacksTo(1));
    public static final DeferredItem<Item> SHOES_1 = ITEMS.registerItem("shoes_1",
        props -> new ClothingItem(props, ClothingItem.ClothingType.SHOES), new Item.Properties().stacksTo(1));
    public static final DeferredItem<Item> SHOES_2 = ITEMS.registerItem("shoes_2",
        props -> new ClothingItem(props, ClothingItem.ClothingType.SHOES), new Item.Properties().stacksTo(1));
    public static final DeferredItem<Item> SHOES_3 = ITEMS.registerItem("shoes_3",
        props -> new ClothingItem(props, ClothingItem.ClothingType.SHOES), new Item.Properties().stacksTo(1));

    // 旧项保留 (可选)
    public static final DeferredItem<Item> GLOVES = ITEMS.registerItem("gloves",
        props -> new ClothingItem(props, ClothingItem.ClothingType.GLOVES), new Item.Properties().stacksTo(1));
    public static final DeferredItem<Item> SHIRT = ITEMS.registerItem("shirt",
        props -> new ClothingItem(props, ClothingItem.ClothingType.SHIRT), new Item.Properties().stacksTo(1));
    public static final DeferredItem<Item> PANTS = ITEMS.registerItem("pants",
        props -> new ClothingItem(props, ClothingItem.ClothingType.PANTS), new Item.Properties().stacksTo(1));
    public static final DeferredItem<Item> SHOES = ITEMS.registerItem("shoes",
        props -> new ClothingItem(props, ClothingItem.ClothingType.SHOES), new Item.Properties().stacksTo(1));

    // --- 系统物品 ---
    public static final DeferredItem<Item> LOCK_ITEM = ITEMS.registerItem("lock_item",
        LockItem::new, new Item.Properties().stacksTo(1));
}
