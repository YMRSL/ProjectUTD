package com.utd.crustychunks;

import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;

/**
 * Items-only port of Warium (Crusty Chunks) gun-part items, so the TaCZ recipe
 * layer that references {@code crusty_chunks:*} parts resolves. Plain items, no
 * functionality (per scope). Magazine items are intentionally NOT registered yet
 * (held until the recipe rework is finalised).
 */
public final class CrustyChunksItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(CrustyChunksMod.MODID);

    public static final DeferredItem<Item> STEEL_COMPONENT = ITEMS.registerSimpleItem("steel_component");
    public static final DeferredItem<Item> SMALL_CASING = ITEMS.registerSimpleItem("small_casing");
    public static final DeferredItem<Item> MEDIUM_CASING = ITEMS.registerSimpleItem("medium_casing");
    public static final DeferredItem<Item> LARGE_CASING = ITEMS.registerSimpleItem("large_casing");
    public static final DeferredItem<Item> SHOTGUN_CASING = ITEMS.registerSimpleItem("shotgun_casing");
    public static final DeferredItem<Item> PISTOL_RECEIVER = ITEMS.registerSimpleItem("pistol_receiver");
    public static final DeferredItem<Item> REVOLVER_RECEIVER = ITEMS.registerSimpleItem("revolver_receiver");
    public static final DeferredItem<Item> SMG_RECEIVER = ITEMS.registerSimpleItem("smg_receiver");
    public static final DeferredItem<Item> MG_RECEIVER = ITEMS.registerSimpleItem("mg_receiver");
    public static final DeferredItem<Item> BOLT_ACTION_RECEIVER = ITEMS.registerSimpleItem("bolt_action_receiver");
    public static final DeferredItem<Item> AUTOMATIC_RIFLE_RECEIVER = ITEMS.registerSimpleItem("automatic_rifle_receiver");
    public static final DeferredItem<Item> FUEL_TANK = ITEMS.registerSimpleItem("fuel_tank");
    public static final DeferredItem<Item> GRENADE = ITEMS.registerSimpleItem("grenade");
    public static final DeferredItem<Item> SMOKE_GRENADE = ITEMS.registerSimpleItem("smoke_grenade");
    public static final DeferredItem<Item> INCENDIARY_BOTTLE = ITEMS.registerSimpleItem("incendiary_bottle");
    public static final DeferredItem<Item> INCENDIARY_GRENADE = ITEMS.registerSimpleItem("incendiary_grenade");
    public static final DeferredItem<Item> HOLLOWED_HUGE_PROJECTILE = ITEMS.registerSimpleItem("hollowed_huge_projectile");

    // Magazines — placeholder items (recipe rework pending; registered so recipes resolve).
    public static final DeferredItem<Item> SMALLMAGAZINE = ITEMS.registerSimpleItem("smallmagazine");
    public static final DeferredItem<Item> MEDIUM_MAGAZINE = ITEMS.registerSimpleItem("medium_magazine");
    public static final DeferredItem<Item> LARGE_MAGAZINE = ITEMS.registerSimpleItem("large_magazine");
    public static final DeferredItem<Item> SMG_MAGAZINE = ITEMS.registerSimpleItem("smg_magazine");
    public static final DeferredItem<Item> LMG_MAGAZINE = ITEMS.registerSimpleItem("lmg_magazine");
    public static final DeferredItem<Item> MACHINE_GUN_BOX = ITEMS.registerSimpleItem("machine_gun_box");

    public static final List<DeferredItem<Item>> ALL = List.of(
            STEEL_COMPONENT, SMALL_CASING, MEDIUM_CASING, LARGE_CASING, SHOTGUN_CASING,
            PISTOL_RECEIVER, REVOLVER_RECEIVER, SMG_RECEIVER, MG_RECEIVER, BOLT_ACTION_RECEIVER,
            AUTOMATIC_RIFLE_RECEIVER, FUEL_TANK, GRENADE, SMOKE_GRENADE, INCENDIARY_BOTTLE,
            INCENDIARY_GRENADE, HOLLOWED_HUGE_PROJECTILE,
            SMALLMAGAZINE, MEDIUM_MAGAZINE, LARGE_MAGAZINE, SMG_MAGAZINE, LMG_MAGAZINE, MACHINE_GUN_BOX
    );

    private CrustyChunksItems() {
    }
}
