package club.someoneice.cockroach.entity;

import club.someoneice.cockroach.EntityInit;
import club.someoneice.cockroach.ItemInit;
import club.someoneice.cockroach.compat.AlexsMobsCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import org.jetbrains.annotations.NotNull;

public class BottleEntityRoach extends ThrowableItemProjectile implements ItemSupplier {
    public BottleEntityRoach(EntityType<? extends ThrowableItemProjectile> pEntityType, Level pLevel) {
        super(pEntityType, pLevel);
    }

    public BottleEntityRoach(double pX, double pY, double pZ, Level pLevel) {
        super(EntityInit.ROACH.get(), pX, pY, pZ, pLevel);
    }

    public BottleEntityRoach(LivingEntity pShooter, Level pLevel) {
        super(EntityInit.ROACH.get(), pShooter, pLevel);
    }

    @Override
    protected @NotNull Item getDefaultItem() {
        return ItemInit.THROWABLE_ROACH_BOTTLE.get();
    }

    @Override
    protected void onHitBlock(BlockHitResult pResult) {
        super.onHitBlock(pResult);
        spawnRoach(pResult.getBlockPos());
    }

    @Override
    protected void onHitEntity(EntityHitResult pResult) {
        super.onHitEntity(pResult);
        spawnRoach(pResult.getEntity().getOnPos());
    }

    @Override
    protected double getDefaultGravity() {
        return 0.05D;
    }

    private void spawnRoach(BlockPos pPos) {
        if (this.level().isClientSide()) {
            return;
        }
        // Only spawns when Alex's Mobs is installed; otherwise the bottle simply breaks.
        AlexsMobsCompat.spawnCockroach(this.level(), pPos.getX(), pPos.getY() + 1, pPos.getZ());
    }
}
