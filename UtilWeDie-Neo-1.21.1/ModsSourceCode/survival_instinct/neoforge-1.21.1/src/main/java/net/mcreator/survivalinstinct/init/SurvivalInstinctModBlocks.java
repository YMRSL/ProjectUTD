package net.mcreator.survivalinstinct.init;

import net.mcreator.survivalinstinct.block.AluminiumBlockBlock;
import net.mcreator.survivalinstinct.block.BarricadeBlock;
import net.mcreator.survivalinstinct.block.BearTrapBlock;
import net.mcreator.survivalinstinct.block.BearTrapCloseBlock;
import net.mcreator.survivalinstinct.block.CashRegisterBlock;
import net.mcreator.survivalinstinct.block.CrateBlock;
import net.mcreator.survivalinstinct.block.DeepslateSulfurOreBlock;
import net.mcreator.survivalinstinct.block.ExplosiveBarrelBlock;
import net.mcreator.survivalinstinct.block.HomemadeMineBlock;
import net.mcreator.survivalinstinct.block.PropaneBlock;
import net.mcreator.survivalinstinct.block.RawSteelliumBlockBlock;
import net.mcreator.survivalinstinct.block.RefrigeratorBlock;
import net.mcreator.survivalinstinct.block.RopeTrapBlock;
import net.mcreator.survivalinstinct.block.SteelliumBlockBlock;
import net.mcreator.survivalinstinct.block.SteelliumOreBlock;
import net.mcreator.survivalinstinct.block.StopSingBlock;
import net.mcreator.survivalinstinct.block.SulfurOreBlock;
import net.mcreator.survivalinstinct.block.TrashCanBlock;
import net.mcreator.survivalinstinct.block.WireBlock;
import net.mcreator.survivalinstinct.block.WireTrapBlock;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;

public class SurvivalInstinctModBlocks {
    public static final DeferredRegister<Block> REGISTRY = DeferredRegister.create(Registries.BLOCK, (String)"survival_instinct");
    public static final DeferredHolder<Block, Block> BEAR_TRAP = REGISTRY.register("bear_trap", () -> new BearTrapBlock());
    public static final DeferredHolder<Block, Block> BEAR_TRAP_CLOSE = REGISTRY.register("bear_trap_close", () -> new BearTrapCloseBlock());
    public static final DeferredHolder<Block, Block> STEELLIUM_ORE = REGISTRY.register("steellium_ore", () -> new SteelliumOreBlock());
    public static final DeferredHolder<Block, Block> STEELLIUM_BLOCK = REGISTRY.register("steellium_block", () -> new SteelliumBlockBlock());
    public static final DeferredHolder<Block, Block> RAW_STEELLIUM_BLOCK = REGISTRY.register("raw_steellium_block", () -> new RawSteelliumBlockBlock());
    public static final DeferredHolder<Block, Block> WIRE = REGISTRY.register("wire", () -> new WireBlock());
    public static final DeferredHolder<Block, Block> REFRIGERATOR = REGISTRY.register("refrigerator", () -> new RefrigeratorBlock());
    public static final DeferredHolder<Block, Block> WIRE_TRAP = REGISTRY.register("wire_trap", () -> new WireTrapBlock());
    public static final DeferredHolder<Block, Block> BARRICADE = REGISTRY.register("barricade", () -> new BarricadeBlock());
    public static final DeferredHolder<Block, Block> EXPLOSIVE_BARREL = REGISTRY.register("explosive_barrel", () -> new ExplosiveBarrelBlock());
    public static final DeferredHolder<Block, Block> SULFUR_ORE = REGISTRY.register("sulfur_ore", () -> new SulfurOreBlock());
    public static final DeferredHolder<Block, Block> DEEPSLATE_SULFUR_ORE = REGISTRY.register("deepslate_sulfur_ore", () -> new DeepslateSulfurOreBlock());
    public static final DeferredHolder<Block, Block> CRATE = REGISTRY.register("crate", () -> new CrateBlock());
    public static final DeferredHolder<Block, Block> CASH_REGISTER = REGISTRY.register("cash_register", () -> new CashRegisterBlock());
    public static final DeferredHolder<Block, Block> TRASH_CAN = REGISTRY.register("trash_can", () -> new TrashCanBlock());
    public static final DeferredHolder<Block, Block> ROPE_TRAP = REGISTRY.register("rope_trap", () -> new RopeTrapBlock());
    public static final DeferredHolder<Block, Block> STOP_SING = REGISTRY.register("stop_sing", () -> new StopSingBlock());
    public static final DeferredHolder<Block, Block> HOMEMADE_MINE = REGISTRY.register("homemade_mine", () -> new HomemadeMineBlock());
    public static final DeferredHolder<Block, Block> PROPANE = REGISTRY.register("propane", () -> new PropaneBlock());
    public static final DeferredHolder<Block, Block> ALUMINIUM_BLOCK = REGISTRY.register("aluminium_block", () -> new AluminiumBlockBlock());
}

