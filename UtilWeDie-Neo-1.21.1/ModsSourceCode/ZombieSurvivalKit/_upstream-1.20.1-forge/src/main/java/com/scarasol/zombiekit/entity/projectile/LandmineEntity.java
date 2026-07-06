package com.scarasol.zombiekit.entity.projectile;

import com.scarasol.zombiekit.block.BombBlock;
import com.scarasol.zombiekit.block.LandmineBlock;
import com.scarasol.zombiekit.init.ZombieKitBlocks;
import com.scarasol.zombiekit.init.ZombieKitEntities;
import com.scarasol.zombiekit.init.ZombieKitItems;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.network.PlayMessages;

public class LandmineEntity extends ModProjectile{

    public LandmineEntity(EntityType<? extends LandmineEntity> type, LivingEntity owner, Level world) {
        super(type, owner, world);
    }

    public LandmineEntity(EntityType<? extends LandmineEntity> type, double x, double y, double z, Level world) {
        super(type, x, y, z, world);
    }



    public LandmineEntity(PlayMessages.SpawnEntity packet, Level world) {
        super(ZombieKitEntities.LANDMINE.get(), world);
    }

    public LandmineEntity(EntityType<? extends LandmineEntity> type, Level world) {
        super(type, world);
    }

    @Override
    public void doEffects(Level level, double x, double y, double z) {
        BlockPos pos = BlockPos.containing(x, y, z);
        Block block = ZombieKitBlocks.LANDMINE.get();
        if (block.canSurvive(block.defaultBlockState(), level, pos)) {
            level.setBlock(pos, block.defaultBlockState(), 3);
        }
        this.discard();
    }

    @Override
    protected ItemStack getPickupItem() {
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack getItem() {
        return new ItemStack(ZombieKitItems.LANDMINE.get());
    }
}
