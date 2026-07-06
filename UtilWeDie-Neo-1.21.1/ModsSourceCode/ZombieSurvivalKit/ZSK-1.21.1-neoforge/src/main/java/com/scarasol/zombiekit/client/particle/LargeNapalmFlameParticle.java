package com.scarasol.zombiekit.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;

public class LargeNapalmFlameParticle extends LargeFlameParticle{
    public LargeNapalmFlameParticle(ClientLevel level, double x, double y, double z, double vx, double vy, double vz) {
        super(level, x, y, z, vx, vy, vz);
    }

    public void particleEffect() {
        RandomSource randomSource = level.getRandom();
        if (randomSource.nextDouble() < 0.1) {
            level.addAlwaysVisibleParticle(ParticleTypes.LAVA, x, y, z, randomSource.nextGaussian() * 0.05, randomSource.nextGaussian() * 0.05, randomSource.nextGaussian() * 0.05);
        }
        if (randomSource.nextDouble() < 0.2) {
            level.addAlwaysVisibleParticle(ParticleTypes.SMOKE, x, y, z, randomSource.nextGaussian() * 0.05, randomSource.nextGaussian() * 0.05, randomSource.nextGaussian() * 0.05);
        }
        if (randomSource.nextDouble() < 0.1) {
            level.addAlwaysVisibleParticle(ParticleTypes.LARGE_SMOKE, x, y, z, randomSource.nextGaussian() * 0.05, randomSource.nextGaussian() * 0.05, randomSource.nextGaussian() * 0.05);
        }
        level.addAlwaysVisibleParticle(ParticleTypes.FLAME, x, y, z, randomSource.nextGaussian() * 0.05, randomSource.nextGaussian() * 0.05, randomSource.nextGaussian() * 0.05);
    }

    public record Factory(SpriteSet sprite) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z, double xSpeed, double ySpeed, double zSpeed) {
            LargeNapalmFlameParticle particle = new LargeNapalmFlameParticle(level, x, y, z, xSpeed, ySpeed, zSpeed);
            particle.pickSprite(this.sprite);
            return particle;
        }
    }
}
