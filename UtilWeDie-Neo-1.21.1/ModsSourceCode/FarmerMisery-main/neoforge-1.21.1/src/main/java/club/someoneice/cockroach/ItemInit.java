package club.someoneice.cockroach;

import club.someoneice.cockroach.item.*;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ItemInit {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ModMain.MODID);

    public static final DeferredItem<Item> ROACH_IN_BOTTLE = ITEMS.register("bottled_roach", ItemRoachInBottle::new);
    public static final DeferredItem<Item> GOKIBURI_YAKI = ITEMS.register("gokiburi_yaki", ItemGokiburiYaki::new);
    public static final DeferredItem<Item> JUICY_ROAST_ROACH = ITEMS.register("juicy_roast_roach", ItemJuicyRoast::new);
    public static final DeferredItem<Item> PROTEIN_BLOCK = ITEMS.register("protein_block", ItemProteinBlock::new);
    public static final DeferredItem<Item> ROACH_BURGER = ITEMS.register("roach_burger", ItemRoachBurger::new);
    public static final DeferredItem<Item> ROACH_MEATBALL = ITEMS.register("roach_meatball", ItemRoachBall::new);
    public static final DeferredItem<Item> ROACH_PATTY = ITEMS.register("roach_patty", ItemRoachBall::new);
    public static final DeferredItem<Item> ROACH_ROLL = ITEMS.register("roach_roll", ItemRoachRoll::new);
    public static final DeferredItem<Item> ROACH_SANDWICH = ITEMS.register("roach_sandwich", ItemRoachRoll::new);
    public static final DeferredItem<Item> ROACH_SALAD_PLATTER = ITEMS.register("roach_salad_platter", ItemRoachSaladPlatter::new);
    public static final DeferredItem<Item> ROACH_SKEWER = ITEMS.register("roach_skewer", ItemRoachSkewer::new);
    public static final DeferredItem<Item> ROACH_WHISKER_CANDY = ITEMS.register("roach_whisker_candy", ItemRoachWhiskerCandy::new);
    public static final DeferredItem<Item> THROWABLE_ROACH_BOTTLE = ITEMS.register("splash_bottled_roach", ItemThrowableRoachBottle::new);

    public static final DeferredItem<Item> KANGFU_XIN_YE = ITEMS.register("kangfu_xin_ye", ItemKangfuXinYe::new);
}
