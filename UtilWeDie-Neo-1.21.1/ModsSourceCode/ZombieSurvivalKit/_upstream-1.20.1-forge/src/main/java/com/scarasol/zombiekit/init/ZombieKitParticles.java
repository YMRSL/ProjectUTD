package com.scarasol.zombiekit.init;

import com.scarasol.zombiekit.client.particle.LargeFlameParticle;
import com.scarasol.zombiekit.client.particle.LargeNapalmFlameParticle;
import com.scarasol.zombiekit.client.particle.LargeSmokeParticle;
import com.scarasol.zombiekit.client.particle.LargeSoulFlameParticle;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD, value = {Dist.CLIENT})
public class ZombieKitParticles {
    @SubscribeEvent
    public static void registerParticles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ZombieKitParticleTypes.LARGE_FLAME.get(), LargeFlameParticle.Factory::new);
        event.registerSpriteSet(ZombieKitParticleTypes.LARGE_SOUL_FLAME.get(), LargeSoulFlameParticle.Factory::new);
        event.registerSpriteSet(ZombieKitParticleTypes.LARGE_SMOKE.get(), LargeSmokeParticle.Factory::new);
        event.registerSpriteSet(ZombieKitParticleTypes.LARGE_NAPALM_FLAME.get(), LargeNapalmFlameParticle.Factory::new);
    }
}
