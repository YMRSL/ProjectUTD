package net.tejty.just_barricades.block;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.tejty.just_barricades.JustBarricades;
import net.tejty.just_barricades.block.custom.BarricadeBlock;
import net.tejty.just_barricades.item.ModItems;

import java.util.function.Supplier;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, JustBarricades.MODID);


    public static final RegistryObject<Block> OAK_BARRICADE = registerBlock("oak_barricade",
            () -> new BarricadeBlock(BlockBehaviour.Properties.of().destroyTime(5).isViewBlocking((state, getter, pos) -> false).mapColor(MapColor.WOOD).pushReaction(PushReaction.DESTROY).sound(SoundType.WOOD).strength(-1.0F, 3600000.0F)));
    public static final RegistryObject<Block> SPRUCE_BARRICADE = registerBlock("spruce_barricade",
            () -> new BarricadeBlock(BlockBehaviour.Properties.of().destroyTime(5).isViewBlocking((state, getter, pos) -> false).mapColor(MapColor.PODZOL).pushReaction(PushReaction.DESTROY).sound(SoundType.WOOD).strength(-1.0F, 3600000.0F)));
    public static final RegistryObject<Block> BIRCH_BARRICADE = registerBlock("birch_barricade",
            () -> new BarricadeBlock(BlockBehaviour.Properties.of().destroyTime(5).isViewBlocking((state, getter, pos) -> false).mapColor(MapColor.SAND).pushReaction(PushReaction.DESTROY).sound(SoundType.WOOD).strength(-1.0F, 3600000.0F)));
    public static final RegistryObject<Block> JUNGLE_BARRICADE = registerBlock("jungle_barricade",
            () -> new BarricadeBlock(BlockBehaviour.Properties.of().destroyTime(5).isViewBlocking((state, getter, pos) -> false).mapColor(MapColor.DIRT).pushReaction(PushReaction.DESTROY).sound(SoundType.WOOD).strength(-1.0F, 2)));
    public static final RegistryObject<Block> ACACIA_BARRICADE = registerBlock("acacia_barricade",
            () -> new BarricadeBlock(BlockBehaviour.Properties.of().destroyTime(5).isViewBlocking((state, getter, pos) -> false).mapColor(MapColor.COLOR_ORANGE).pushReaction(PushReaction.DESTROY).sound(SoundType.WOOD).strength(-1.0F, 3600000.0F)));
    public static final RegistryObject<Block> CHERRY_BARRICADE = registerBlock("cherry_barricade",
            () -> new BarricadeBlock(BlockBehaviour.Properties.of().destroyTime(5).isViewBlocking((state, getter, pos) -> false).mapColor(MapColor.TERRACOTTA_WHITE).pushReaction(PushReaction.DESTROY).sound(SoundType.CHERRY_WOOD).strength(-1.0F, 2)));
    public static final RegistryObject<Block> DARK_OAK_BARRICADE = registerBlock("dark_oak_barricade",
            () -> new BarricadeBlock(BlockBehaviour.Properties.of().destroyTime(5).isViewBlocking((state, getter, pos) -> false).mapColor(MapColor.COLOR_BROWN).pushReaction(PushReaction.DESTROY).sound(SoundType.CHERRY_WOOD).strength(-1.0F, 2)));
    public static final RegistryObject<Block> MANGROVE_BARRICADE = registerBlock("mangrove_barricade",
            () -> new BarricadeBlock(BlockBehaviour.Properties.of().destroyTime(5).isViewBlocking((state, getter, pos) -> false).mapColor(MapColor.COLOR_RED).pushReaction(PushReaction.DESTROY).sound(SoundType.CHERRY_WOOD).strength(-1.0F, 2)));
    public static final RegistryObject<Block> BAMBOO_BARRICADE = registerBlock("bamboo_barricade",
            () -> new BarricadeBlock(BlockBehaviour.Properties.of().destroyTime(5).isViewBlocking((state, getter, pos) -> false).mapColor(MapColor.COLOR_YELLOW).pushReaction(PushReaction.DESTROY).sound(SoundType.BAMBOO_WOOD).strength(-1.0F, 2)));


    private static <T extends Block> RegistryObject<T> registerBlock(String name, Supplier<T> block) {
        RegistryObject<T> toReturn = BLOCKS.register(name, block);
        registerBlockItem(name, toReturn);
        return toReturn;
    }

    private static <T extends Block> RegistryObject<Item> registerBlockItem(String name, RegistryObject<T> block) {
        return ModItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
