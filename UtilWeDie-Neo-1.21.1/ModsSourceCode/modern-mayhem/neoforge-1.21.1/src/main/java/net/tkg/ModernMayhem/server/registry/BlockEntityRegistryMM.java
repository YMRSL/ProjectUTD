package net.tkg.ModernMayhem.server.registry;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.tkg.ModernMayhem.server.block.entity.DuffelBagBlockEntity;
import net.tkg.ModernMayhem.server.block.entity.IRLightBlockEntity;
import net.tkg.ModernMayhem.server.registry.BlockRegistryMM;

public class BlockEntityRegistryMM {
    public static final DeferredRegister<BlockEntityType<?>> REGISTRY = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, (String)"mm");
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<DuffelBagBlockEntity>> DUFFEL_BAG = REGISTRY.register("duffel_bag", () -> BlockEntityType.Builder.of(DuffelBagBlockEntity::new, (Block[])new Block[]{(Block)BlockRegistryMM.DUFFEL_BAG_BLOCK.get()}).build(null));
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<IRLightBlockEntity>> IR_LIGHT = REGISTRY.register("ir_light", () -> BlockEntityType.Builder.of(IRLightBlockEntity::new, (Block[])new Block[]{(Block)BlockRegistryMM.IR_LIGHT_BLOCK.get()}).build(null));

    public static void init(IEventBus modEventBus) {
        REGISTRY.register(modEventBus);
    }
}

