package net.tkg.ModernMayhem.server.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.tkg.ModernMayhem.server.registry.BlockEntityRegistryMM;

/**
 * IR 照明方块的方块实体。无自身数据/无 tick; 仅作为 SodiumDynamicLights 的动态光源载体 ——
 * SDDL 的 BlockEntityMixin 让所有 BE 实现 DynamicLightSource, 我们给本类型注册 viewer-gated
 * 亮度 handler (戴夜视仪? 亮度 : 0), SDDL 的方块光 tick 会自动据此追踪/重光。
 */
public class IRLightBlockEntity extends BlockEntity {
    public IRLightBlockEntity(BlockPos pos, BlockState state) {
        super(BlockEntityRegistryMM.IR_LIGHT.get(), pos, state);
    }
}
