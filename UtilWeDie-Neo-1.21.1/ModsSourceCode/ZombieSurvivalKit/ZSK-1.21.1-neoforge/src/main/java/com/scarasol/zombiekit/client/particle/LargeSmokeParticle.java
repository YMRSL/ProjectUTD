package com.scarasol.zombiekit.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;

public class LargeSmokeParticle extends LargeParticle{

    public LargeSmokeParticle(ClientLevel level, double x, double y, double z, double vx, double vy, double vz) {
        super(level, x, y, z, vx, vy, vz);
        this.scale *= 10F;
        this.lifetime = (int) (400.0D / (Math.random() * 0.8D + 0.2D)) + 200;
    }

    @Override
    public int getLightColor(float partialTicks) {
        BlockPos blockpos = BlockPos.containing(this.x, this.y, this.z);
        return this.level.hasChunkAt(blockpos) ? Math.max(LevelRenderer.getLightColor(this.level, blockpos), 1) : 1;
    }

    @Override
    public void particleEffect() {
    }

    public record Factory(SpriteSet sprite) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            LargeSmokeParticle particle = new LargeSmokeParticle(level, x, y, z, xSpeed, ySpeed, zSpeed);
            particle.pickSprite(this.sprite);
            return particle;
        }
    }
}
