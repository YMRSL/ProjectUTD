package net.mcreator.doomsdaydecoration.functionality;

import net.mcreator.doomsdaydecoration.DoomsdayDecoration;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Container/loot layer for Doomsday Decoration ("functionality" track).
 *
 * <p>Native NeoForge 1.21.1 reimplementation of Raiiiden/DoomsdayFunctionality's
 * BlockEntity loot system. No mixins, no custom network packets: the loot container
 * is opened with vanilla {@code player.openMenu(MenuProvider)} and synced by the
 * standard container protocol.</p>
 *
 * <p>This class owns the {@link BlockEntityType} and {@link MenuType} registrations.
 * The set of blocks that carry the BlockEntity is decided by the <b>block layer</b>
 * (a Deco block implements {@code EntityBlock} and returns a
 * {@link DoomsdayBlockEntity} from {@code newBlockEntity}); this type is built with
 * {@code .build(null)} (validBlocks == null) so it accepts <i>any</i> block that
 * returns this BE, instead of a hardcoded list. That keeps the mount layer free to
 * mark blocks lootable from the manifest.</p>
 */
public final class ModFunctionality {
    private ModFunctionality() {}

    private static final String MODID = DoomsdayDecoration.MODID;

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MODID);
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, MODID);

    /**
     * Generic lootable BlockEntity type. Built with an <i>empty</i> validBlocks set
     * ({@code of(...)} with no block args) and {@code build(null)} passing the
     * datafixer {@code Type} (null is fine for modded BEs). The set of valid blocks is
     * filled later, after blocks are registered, by
     * {@code ModRegistry.registerLootableBlocks} via {@code BlockEntityTypeAddBlocksEvent}.
     *
     * <p><b>Important (1.21.1):</b> the validBlocks set must NOT stay empty. NeoForge
     * 1.21.1 validates the BlockState against {@code BlockEntityType.validBlocks} both in
     * the {@code BlockEntity} constructor ({@code validateBlockState} throws
     * IllegalStateException) and in {@code LevelChunk.setBlockEntity}; an empty set
     * crashes on every placement / chunk-load of a lootable block. The list cannot be
     * supplied here because the blocks are not registered yet when this supplier runs,
     * hence the event-based injection.</p>
     */
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DoomsdayBlockEntity>> DOOMSDAY_BE =
            BLOCK_ENTITIES.register("generic",
                    () -> BlockEntityType.Builder.<DoomsdayBlockEntity>of(DoomsdayBlockEntity::new).build(null));

    public static final DeferredHolder<MenuType<?>, MenuType<DoomsdayContainerMenu>> DOOMSDAY_MENU =
            MENUS.register("doomsday_container",
                    () -> IMenuTypeExtension.create(DoomsdayContainerMenu::new));

    /** Called from the mod constructor to attach both registers to the mod bus. */
    public static void init(IEventBus modBus) {
        BLOCK_ENTITIES.register(modBus);
        MENUS.register(modBus);
    }
}
