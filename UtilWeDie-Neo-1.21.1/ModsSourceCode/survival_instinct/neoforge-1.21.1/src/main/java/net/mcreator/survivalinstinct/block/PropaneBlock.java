package net.mcreator.survivalinstinct.block;

import net.mcreator.survivalinstinct.procedures.ExplosiveBarrelOnBlockHitByProjectileProcedure;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class PropaneBlock
extends Block {
    public PropaneBlock() {
        super(BlockBehaviour.Properties.of().instrument(NoteBlockInstrument.BASEDRUM).sound(SoundType.METAL).strength(4.0f, 10.0f).requiresCorrectToolForDrops().noOcclusion().isRedstoneConductor((bs, br, bp) -> false));
    }

    public boolean propagatesSkylightDown(BlockState state, BlockGetter reader, BlockPos pos) {
        return true;
    }

    public int getLightBlock(BlockState state, BlockGetter worldIn, BlockPos pos) {
        return 0;
    }

    public VoxelShape getVisualShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return Shapes.empty();
    }

    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        return Shapes.or((VoxelShape)PropaneBlock.box((double)3.0, (double)2.0, (double)3.0, (double)13.0, (double)12.0, (double)13.0), (VoxelShape[])new VoxelShape[]{PropaneBlock.box((double)4.0, (double)0.0, (double)4.0, (double)12.0, (double)2.0, (double)12.0), PropaneBlock.box((double)7.0, (double)12.0, (double)7.0, (double)9.0, (double)14.0, (double)9.0), PropaneBlock.box((double)7.0, (double)12.5, (double)5.0, (double)8.0, (double)13.5, (double)7.0), PropaneBlock.box((double)6.0, (double)14.0, (double)6.0, (double)10.0, (double)15.0, (double)10.0)});
    }

    public void wasExploded(Level world, BlockPos pos, Explosion e) {
        super.wasExploded(world, pos, e);
        ExplosiveBarrelOnBlockHitByProjectileProcedure.execute((LevelAccessor)world, pos.getX(), pos.getY(), pos.getZ());
    }

    public void onProjectileHit(Level world, BlockState blockstate, BlockHitResult hit, Projectile entity) {
        ExplosiveBarrelOnBlockHitByProjectileProcedure.execute((LevelAccessor)world, hit.getBlockPos().getX(), hit.getBlockPos().getY(), hit.getBlockPos().getZ());
    }
}

