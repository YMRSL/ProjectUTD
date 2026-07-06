package com.scarasol.zombiekit.init;

import com.scarasol.zombiekit.ZombieKitMod;
import com.scarasol.zombiekit.block.entity.InjectorBlockEntity;
import com.scarasol.zombiekit.block.entity.MortarRackBlockEntity;
import com.scarasol.zombiekit.block.entity.ShortwaveRadioBlockEntity;
import com.scarasol.zombiekit.block.entity.VacuumPackagingMachineBlockEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ZombieKitBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> REGISTRY = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, ZombieKitMod.MODID);
    public static final RegistryObject<BlockEntityType<?>> INJECTOR = register("injector", ZombieKitBlocks.INJECTOR, InjectorBlockEntity::new);
    public static final RegistryObject<BlockEntityType<?>> SHORTWAVE_RADIO = register("shortwave_radio", ZombieKitBlocks.SHORTWAVE_RADIO, ShortwaveRadioBlockEntity::new);
    public static final RegistryObject<BlockEntityType<?>> VACUUM_PACKAGING_MACHINE = register("vacuum_packaging_machine", ZombieKitBlocks.VACUUM_PACKAGING_MACHINE, VacuumPackagingMachineBlockEntity::new);
    public static final RegistryObject<BlockEntityType<?>> MORTAR_RACK = register("mortar_rack", ZombieKitBlocks.MORTAR_RACK, MortarRackBlockEntity::new);


    private static RegistryObject<BlockEntityType<? extends BlockEntity>> register(String registryName, RegistryObject<Block> block, BlockEntityType.BlockEntitySupplier<?> supplier) {
        return REGISTRY.register(registryName, () -> BlockEntityType.Builder.of(supplier, block.get()).build(null));
    }

}
