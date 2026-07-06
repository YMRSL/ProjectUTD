package net.mcreator.survivalinstinct.init;

import net.mcreator.survivalinstinct.block.entity.CashRegisterBlockEntity;
import net.mcreator.survivalinstinct.block.entity.RefrigeratorBlockEntity;
import net.mcreator.survivalinstinct.block.entity.TrashCanBlockEntity;
import net.mcreator.survivalinstinct.init.SurvivalInstinctModBlocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;

public class SurvivalInstinctModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> REGISTRY = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, (String)"survival_instinct");
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<?>> REFRIGERATOR = SurvivalInstinctModBlockEntities.register("refrigerator", SurvivalInstinctModBlocks.REFRIGERATOR, RefrigeratorBlockEntity::new);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<?>> CASH_REGISTER = SurvivalInstinctModBlockEntities.register("cash_register", SurvivalInstinctModBlocks.CASH_REGISTER, CashRegisterBlockEntity::new);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<?>> TRASH_CAN = SurvivalInstinctModBlockEntities.register("trash_can", SurvivalInstinctModBlocks.TRASH_CAN, TrashCanBlockEntity::new);

    private static DeferredHolder<BlockEntityType<?>, BlockEntityType<?>> register(String registryname, DeferredHolder<Block, Block> block, BlockEntityType.BlockEntitySupplier<?> supplier) {
        return REGISTRY.register(registryname, () -> BlockEntityType.Builder.of((BlockEntityType.BlockEntitySupplier)supplier, (Block[])new Block[]{(Block)block.get()}).build(null));
    }
}

