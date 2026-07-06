package com.scarasol.zombiekit.init;

import com.scarasol.zombiekit.client.particle.LargeFlameParticle;
import com.scarasol.zombiekit.client.particle.LargeNapalmFlameParticle;
import com.scarasol.zombiekit.client.particle.LargeSmokeParticle;
import com.scarasol.zombiekit.client.particle.LargeSoulFlameParticle;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;

@EventBusSubscriber(modid = "zombiekit", bus = EventBusSubscriber.Bus.MOD, value = {Dist.CLIENT})
public class ZombieKitParticles {
    @SubscribeEvent
    public static void registerParticles(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(ZombieKitParticleTypes.LARGE_FLAME.get(), LargeFlameParticle.Factory::new);
        event.registerSpriteSet(ZombieKitParticleTypes.LARGE_SOUL_FLAME.get(), LargeSoulFlameParticle.Factory::new);
        event.registerSpriteSet(ZombieKitParticleTypes.LARGE_SMOKE.get(), LargeSmokeParticle.Factory::new);
        event.registerSpriteSet(ZombieKitParticleTypes.LARGE_NAPALM_FLAME.get(), LargeNapalmFlameParticle.Factory::new);
    }
}
