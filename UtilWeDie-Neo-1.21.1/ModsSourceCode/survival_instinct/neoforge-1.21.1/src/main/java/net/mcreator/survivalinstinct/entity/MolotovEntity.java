package net.mcreator.survivalinstinct.entity;
import net.minecraft.server.level.ServerEntity;

import net.mcreator.survivalinstinct.init.SurvivalInstinctModEntities;
import net.mcreator.survivalinstinct.init.SurvivalInstinctModItems;
import net.mcreator.survivalinstinct.procedures.MolotovProjectileHitsBlockProcedure;
import net.mcreator.survivalinstinct.procedures.MolotovWhileProjectileFlyingTickProcedure;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ItemSupplier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;

@OnlyIn(value=Dist.CLIENT, _interface=ItemSupplier.class)
public class MolotovEntity
extends AbstractArrow
implements ItemSupplier {
    public static final ItemStack PROJECTILE_ITEM = new ItemStack((ItemLike)SurvivalInstinctModItems.MOLOTOV_COCKTAIL.get());


    public MolotovEntity(EntityType<? extends MolotovEntity> type, Level world) {
        super(type, world);
    }

    public MolotovEntity(EntityType<? extends MolotovEntity> type, double x, double y, double z, Level world) {
        super(type, x, y, z, world, PROJECTILE_ITEM, null);
    }

    public MolotovEntity(EntityType<? extends MolotovEntity> type, LivingEntity entity, Level world) {
        super(type, entity, world, PROJECTILE_ITEM, null);
    }

    public Packet<ClientGamePacketListener> getAddEntityPacket(ServerEntity serverEntity) {
        return super.getAddEntityPacket(serverEntity);
    }

    @OnlyIn(value=Dist.CLIENT)
    public ItemStack getItem() {
        return PROJECTILE_ITEM;
    }

    protected ItemStack getDefaultPickupItem() {
        return PROJECTILE_ITEM;
    }

    protected void doPostHurtEffects(LivingEntity entity) {
        super.doPostHurtEffects(entity);
        entity.setArrowCount(entity.getArrowCount() - 1);
    }

    public void onHitEntity(EntityHitResult entityHitResult) {
        super.onHitEntity(entityHitResult);
        MolotovProjectileHitsBlockProcedure.execute((LevelAccessor)this.level(), this.getX(), this.getY(), this.getZ());
    }

    public void onHitBlock(BlockHitResult blockHitResult) {
        super.onHitBlock(blockHitResult);
        MolotovProjectileHitsBlockProcedure.execute((LevelAccessor)this.level(), blockHitResult.getBlockPos().getX(), blockHitResult.getBlockPos().getY(), blockHitResult.getBlockPos().getZ());
    }

    public void tick() {
        super.tick();
        MolotovWhileProjectileFlyingTickProcedure.execute((LevelAccessor)this.level(), this.getX(), this.getY(), this.getZ());
        if (this.inGround) {
            this.discard();
        }
    }

    public static MolotovEntity shoot(Level world, LivingEntity entity, RandomSource source) {
        return MolotovEntity.shoot(world, entity, source, 0.7f, 3.0, 0);
    }

    public static MolotovEntity shoot(Level world, LivingEntity entity, RandomSource source, float pullingPower) {
        return MolotovEntity.shoot(world, entity, source, pullingPower * 0.7f, 3.0, 0);
    }

    public static MolotovEntity shoot(Level world, LivingEntity entity, RandomSource random, float power, double damage, int knockback) {
        MolotovEntity entityarrow = new MolotovEntity((EntityType<? extends MolotovEntity>)((EntityType)SurvivalInstinctModEntities.MOLOTOV.get()), entity, world);
        entityarrow.shoot(entity.getViewVector((float)1.0f).x, entity.getViewVector((float)1.0f).y, entity.getViewVector((float)1.0f).z, power * 2.0f, 0.0f);
        entityarrow.setSilent(true);
        entityarrow.setCritArrow(false);
        entityarrow.setBaseDamage(damage);
        world.addFreshEntity((Entity)entityarrow);
        world.playSound(null, entity.getX(), entity.getY(), entity.getZ(), (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("entity.egg.throw")), SoundSource.PLAYERS, 1.0f, 1.0f / (random.nextFloat() * 0.5f + 1.0f) + power / 2.0f);
        return entityarrow;
    }

    public static MolotovEntity shoot(LivingEntity entity, LivingEntity target) {
        MolotovEntity entityarrow = new MolotovEntity((EntityType<? extends MolotovEntity>)((EntityType)SurvivalInstinctModEntities.MOLOTOV.get()), entity, entity.level());
        double dx = target.getX() - entity.getX();
        double dy = target.getY() + (double)target.getEyeHeight() - 1.1;
        double dz = target.getZ() - entity.getZ();
        entityarrow.shoot(dx, dy - entityarrow.getY() + Math.hypot(dx, dz) * (double)0.2f, dz, 1.4f, 12.0f);
        entityarrow.setSilent(true);
        entityarrow.setBaseDamage(3.0);
        entityarrow.setCritArrow(false);
        entity.level().addFreshEntity((Entity)entityarrow);
        entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(), (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("entity.egg.throw")), SoundSource.PLAYERS, 1.0f, 1.0f / (RandomSource.create().nextFloat() * 0.5f + 1.0f));
        return entityarrow;
    }
}

