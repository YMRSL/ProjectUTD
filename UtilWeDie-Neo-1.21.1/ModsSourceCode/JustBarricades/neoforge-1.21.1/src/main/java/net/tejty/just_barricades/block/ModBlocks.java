package net.tejty.just_barricades.block;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.tejty.just_barricades.JustBarricades;
import net.tejty.just_barricades.block.custom.BarricadeBlock;
import net.tejty.just_barricades.item.ModItems;

import java.util.function.Supplier;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS =
            DeferredRegister.createBlocks(JustBarricades.MODID);

    public static final DeferredBlock<Block> OAK_BARRICADE = registerBlock("oak_barricade",
            () -> new BarricadeBlock(BlockBehaviour.Properties.of().destroyTime(5).isViewBlocking((state, getter, pos) -> false).mapColor(MapColor.WOOD).pushReaction(PushReaction.DESTROY).sound(SoundType.WOOD).strength(-1.0F, 3600000.0F)));
    public static final DeferredBlock<Block> SPRUCE_BARRICADE = registerBlock("spruce_barricade",
            () -> new BarricadeBlock(BlockBehaviour.Properties.of().destroyTime(5).isViewBlocking((state, getter, pos) -> false).mapColor(MapColor.PODZOL).pushReaction(PushReaction.DESTROY).sound(SoundType.WOOD).strength(-1.0F, 3600000.0F)));
    public static final DeferredBlock<Block> BIRCH_BARRICADE = registerBlock("birch_barricade",
            () -> new BarricadeBlock(BlockBehaviour.Properties.of().destroyTime(5).isViewBlocking((state, getter, pos) -> false).mapColor(MapColor.SAND).pushReaction(PushReaction.DESTROY).sound(SoundType.WOOD).strength(-1.0F, 3600000.0F)));
    public static final DeferredBlock<Block> JUNGLE_BARRICADE = registerBlock("jungle_barricade",
            () -> new BarricadeBlock(BlockBehaviour.Properties.of().destroyTime(5).isViewBlocking((state, getter, pos) -> false).mapColor(MapColor.DIRT).pushReaction(PushReaction.DESTROY).sound(SoundType.WOOD).strength(-1.0F, 2)));
    public static final DeferredBlock<Block> ACACIA_BARRICADE = registerBlock("acacia_barricade",
            () -> new BarricadeBlock(BlockBehaviour.Properties.of().destroyTime(5).isViewBlocking((state, getter, pos) -> false).mapColor(MapColor.COLOR_ORANGE).pushReaction(PushReaction.DESTROY).sound(SoundType.WOOD).strength(-1.0F, 3600000.0F)));
    public static final DeferredBlock<Block> CHERRY_BARRICADE = registerBlock("cherry_barricade",
            () -> new BarricadeBlock(BlockBehaviour.Properties.of().destroyTime(5).isViewBlocking((state, getter, pos) -> false).mapColor(MapColor.TERRACOTTA_WHITE).pushReaction(PushReaction.DESTROY).sound(SoundType.CHERRY_WOOD).strength(-1.0F, 2)));
    public static final DeferredBlock<Block> DARK_OAK_BARRICADE = registerBlock("dark_oak_barricade",
            () -> new BarricadeBlock(BlockBehaviour.Properties.of().destroyTime(5).isViewBlocking((state, getter, pos) -> false).mapColor(MapColor.COLOR_BROWN).pushReaction(PushReaction.DESTROY).sound(SoundType.CHERRY_WOOD).strength(-1.0F, 2)));
    public static final DeferredBlock<Block> MANGROVE_BARRICADE = registerBlock("mangrove_barricade",
            () -> new BarricadeBlock(BlockBehaviour.Properties.of().destroyTime(5).isViewBlocking((state, getter, pos) -> false).mapColor(MapColor.COLOR_RED).pushReaction(PushReaction.DESTROY).sound(SoundType.CHERRY_WOOD).strength(-1.0F, 2)));
    public static final DeferredBlock<Block> BAMBOO_BARRICADE = registerBlock("bamboo_barricade",
            () -> new BarricadeBlock(BlockBehaviour.Properties.of().destroyTime(5).isViewBlocking((state, getter, pos) -> false).mapColor(MapColor.COLOR_YELLOW).pushReaction(PushReaction.DESTROY).sound(SoundType.BAMBOO_WOOD).strength(-1.0F, 2)));

    private static <T extends Block> DeferredBlock<T> registerBlock(String name, Supplier<T> block) {
        DeferredBlock<T> toReturn = BLOCKS.register(name, block);
        registerBlockItem(name, toReturn);
        return toReturn;
    }

    private static <T extends Block> void registerBlockItem(String name, DeferredBlock<T> block) {
        ModItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
