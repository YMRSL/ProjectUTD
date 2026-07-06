package com.scarasol.zombiekit.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;

//source: https://github.com/TeamTwilight/twilightforest/tree/1.20.1
public class LargeFlameParticle extends LargeParticle {

    public LargeFlameParticle(ClientLevel level, double x, double y, double z, double vx, double vy, double vz) {
        super(level, x, y, z, vx, vy, vz);
        willFloat = true;
    }

    @Override
    public int getLightColor(float partialTicks) {
        float var2 = (this.age + partialTicks) / this.lifetime;

        if (var2 < 0.0F) {
            var2 = 0.0F;
        }

        if (var2 > 1.0F) {
            var2 = 1.0F;
        }

        int var3 = super.getLightColor(partialTicks);
        int var4 = var3 & 255;
        int var5 = var3 >> 16 & 255;
        var4 += (int) (var2 * 15.0F * 16.0F);

        if (var4 > 240) {
            var4 = 240;
        }

        return var4 | var5 << 16;
    }

    public void particleEffect() {
        RandomSource randomSource = level.getRandom();
        if (randomSource.nextDouble() < 0.05) {
            level.addAlwaysVisibleParticle(ParticleTypes.LAVA, x, y, z, randomSource.nextGaussian() * 0.05, randomSource.nextGaussian() * 0.05, randomSource.nextGaussian() * 0.05);
        }
        level.addAlwaysVisibleParticle(ParticleTypes.FLAME, x, y, z, randomSource.nextGaussian() * 0.05, randomSource.nextGaussian() * 0.05, randomSource.nextGaussian() * 0.05);
    }

    public record Factory(SpriteSet sprite) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            LargeFlameParticle particle = new LargeFlameParticle(level, x, y, z, xSpeed, ySpeed, zSpeed);
            particle.pickSprite(this.sprite);
            return particle;
        }
    }

}
