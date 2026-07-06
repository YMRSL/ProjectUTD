package net.mcreator.survivalinstinct.entity;
import net.minecraft.server.level.ServerEntity;

import net.mcreator.survivalinstinct.init.SurvivalInstinctModEntities;
import net.mcreator.survivalinstinct.init.SurvivalInstinctModItems;
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
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;

@OnlyIn(value=Dist.CLIENT, _interface=ItemSupplier.class)
public class NailProyectileEntity
extends AbstractArrow
implements ItemSupplier {
    public static final ItemStack PROJECTILE_ITEM = new ItemStack((ItemLike)SurvivalInstinctModItems.NAIL.get());


    public NailProyectileEntity(EntityType<? extends NailProyectileEntity> type, Level world) {
        super(type, world);
    }

    public NailProyectileEntity(EntityType<? extends NailProyectileEntity> type, double x, double y, double z, Level world) {
        super(type, x, y, z, world, PROJECTILE_ITEM, null);
    }

    public NailProyectileEntity(EntityType<? extends NailProyectileEntity> type, LivingEntity entity, Level world) {
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

    public void tick() {
        super.tick();
        if (this.inGround) {
            this.discard();
        }
    }

    public static NailProyectileEntity shoot(Level world, LivingEntity entity, RandomSource source) {
        return NailProyectileEntity.shoot(world, entity, source, 1.4f, 1.9, 0);
    }

    public static NailProyectileEntity shoot(Level world, LivingEntity entity, RandomSource source, float pullingPower) {
        return NailProyectileEntity.shoot(world, entity, source, pullingPower * 1.4f, 1.9, 0);
    }

    public static NailProyectileEntity shoot(Level world, LivingEntity entity, RandomSource random, float power, double damage, int knockback) {
        NailProyectileEntity entityarrow = new NailProyectileEntity((EntityType<? extends NailProyectileEntity>)((EntityType)SurvivalInstinctModEntities.NAIL_PROYECTILE.get()), entity, world);
        entityarrow.shoot(entity.getViewVector((float)1.0f).x, entity.getViewVector((float)1.0f).y, entity.getViewVector((float)1.0f).z, power * 2.0f, 0.0f);
        entityarrow.setSilent(true);
        entityarrow.setCritArrow(false);
        entityarrow.setBaseDamage(damage);
        world.addFreshEntity((Entity)entityarrow);
        world.playSound(null, entity.getX(), entity.getY(), entity.getZ(), (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("survival_instinct:nailgun_shoot")), SoundSource.PLAYERS, 1.0f, 1.0f / (random.nextFloat() * 0.5f + 1.0f) + power / 2.0f);
        return entityarrow;
    }

    public static NailProyectileEntity shoot(LivingEntity entity, LivingEntity target) {
        NailProyectileEntity entityarrow = new NailProyectileEntity((EntityType<? extends NailProyectileEntity>)((EntityType)SurvivalInstinctModEntities.NAIL_PROYECTILE.get()), entity, entity.level());
        double dx = target.getX() - entity.getX();
        double dy = target.getY() + (double)target.getEyeHeight() - 1.1;
        double dz = target.getZ() - entity.getZ();
        entityarrow.shoot(dx, dy - entityarrow.getY() + Math.hypot(dx, dz) * (double)0.2f, dz, 2.8f, 12.0f);
        entityarrow.setSilent(true);
        entityarrow.setBaseDamage(1.9);
        entityarrow.setCritArrow(false);
        entity.level().addFreshEntity((Entity)entityarrow);
        entity.level().playSound(null, entity.getX(), entity.getY(), entity.getZ(), (SoundEvent)BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse("survival_instinct:nailgun_shoot")), SoundSource.PLAYERS, 1.0f, 1.0f / (RandomSource.create().nextFloat() * 0.5f + 1.0f));
        return entityarrow;
    }
}

