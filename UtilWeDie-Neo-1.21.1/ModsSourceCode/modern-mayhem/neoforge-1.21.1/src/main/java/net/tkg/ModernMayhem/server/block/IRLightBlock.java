package net.tkg.ModernMayhem.server.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.tkg.ModernMayhem.server.block.entity.IRLightBlockEntity;
import org.jetbrains.annotations.Nullable;

/**
 * 红外 (IR) 照明方块。本身 vanilla 发光等级为 0 (裸眼全黑), 真正的照明由 SodiumDynamicLights
 * 动态光提供, 且只有【本地观察者戴着开机夜视仪】时才可见 (见 IrBlockSddlCompat 注册的 viewer-gated handler)。
 * 拟真的"红外路灯/诱饵": 裸眼一片黑, 透过夜视仪一片亮。也是单人验证 IR 可见性的最佳测试道具。
 */
public class IRLightBlock extends Block implements EntityBlock {
    public IRLightBlock(BlockBehaviour.Properties properties) {
        super(properties);
    }

    @Override
    @Nullable
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new IRLightBlockEntity(pos, state);
    }

    /**
     * 客户端给一个空 ticker —— SDDL 只对【会 tick 的方块实体(TickingBlockEntity)】驱动动态光更新
     * (它 hook 的是方块实体 tick)。没有 ticker 的静态 BE 永远不会被 SDDL 处理 → 不发光。
     * 这个空 ticker 不做任何逻辑, 仅让 BE 进入 tick 列表, 从而触发 SDDL 的逐 BE 动态光计算。
     */
    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? (lvl, pos, st, be) -> {} : null;
    }
}
