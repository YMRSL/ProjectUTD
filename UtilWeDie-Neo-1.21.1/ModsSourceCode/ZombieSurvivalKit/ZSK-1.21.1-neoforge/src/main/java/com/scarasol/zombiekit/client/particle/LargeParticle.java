package com.scarasol.zombiekit.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.BlockPos;

public abstract class LargeParticle extends TextureSheetParticle {

    protected float scale;
    protected boolean willFloat;

    public LargeParticle(ClientLevel level, double x, double y, double z, double vx, double vy, double vz) {
        super(level, x, y, z, vx, vy, vz);
        this.xd = this.xd * 0.01D + vx;
        this.yd = this.yd * 0.01D + vy;
        this.zd = this.zd * 0.01D + vz;
        this.quadSize *= 5.0F;
        this.scale = this.quadSize;
        this.rCol = this.gCol = this.bCol = 1.0F;
        this.lifetime = (int) (8.0D / (Math.random() * 0.8D + 0.2D)) + 4;
        this.hasPhysics = false;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
    }

    @Override
    public float getQuadSize(float partialTicks) {
        float relativeAge = (this.age + partialTicks) / this.lifetime;
        return scale * (0.9F + relativeAge * relativeAge * 1.2F);
    }

    public float getScale() {
        return scale;
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;

        particleEffect();

        if (this.age++ >= this.lifetime) {
            this.remove();
        }

        if (willFloat)
            this.yd += 0.002D;

        this.move(this.xd, this.yd, this.zd);
        this.xd *= 0.96D;
        this.yd *= 0.96D;
        this.zd *= 0.96D;

        if (this.onGround) {
            this.xd *= 0.7D;
            this.zd *= 0.7D;
        } else if (!level.getBlockState(BlockPos.containing(x, y, z)).isAir()) {
            this.xd *= 0.3D;
            this.zd *= 0.3D;
        }
    }

    public abstract void particleEffect();
}
