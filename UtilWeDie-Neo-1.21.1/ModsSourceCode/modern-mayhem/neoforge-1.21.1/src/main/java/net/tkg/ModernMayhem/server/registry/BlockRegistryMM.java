package net.tkg.ModernMayhem.server.registry;

import java.util.function.Supplier;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.tkg.ModernMayhem.server.block.DuffelBagBlock;
import net.tkg.ModernMayhem.server.block.IRLightBlock;
import net.tkg.ModernMayhem.server.registry.ItemRegistryMM;

public class BlockRegistryMM {
    public static final DeferredRegister<Block> REGISTRY = DeferredRegister.create(Registries.BLOCK, (String)"mm");
    public static final DeferredHolder<Block, Block> DUFFEL_BAG_BLOCK = BlockRegistryMM.registerBlock("duffel_bag_block", () -> new DuffelBagBlock(BlockBehaviour.Properties.of().sound(SoundType.WOOL).strength(0.5f).noOcclusion()));
    // IR 红外照明方块: vanilla 发光 0 (裸眼黑), 实际照明由 SDDL viewer-gated 动态光提供。
    public static final DeferredHolder<Block, Block> IR_LIGHT_BLOCK = BlockRegistryMM.registerBlock("ir_light_block", () -> new IRLightBlock(BlockBehaviour.Properties.of().sound(SoundType.METAL).strength(1.5f).lightLevel(state -> 0)));

    private static <T extends Block> DeferredHolder<Block, T> registerBlock(String name, Supplier<T> block) {
        DeferredHolder<Block, T> toReturn = REGISTRY.register(name, block);
        BlockRegistryMM.registerBlockItem(name, toReturn);
        return toReturn;
    }

    private static <T extends Block> DeferredHolder<Item, Item> registerBlockItem(String name, DeferredHolder<Block, T> block) {
        return ItemRegistryMM.ITEMS.register(name, () -> new BlockItem((Block)block.get(), new Item.Properties()));
    }

    public static void init(IEventBus modEventBus) {
        REGISTRY.register(modEventBus);
    }
}

