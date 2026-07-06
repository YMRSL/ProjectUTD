package io.github.ymrsl.firstpersonfoodeating.client;

import io.github.ymrsl.firstpersonfoodeating.FirstPersonFoodEatingMod;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.lwjgl.glfw.GLFW;

@EventBusSubscriber(
        modid = FirstPersonFoodEatingMod.MOD_ID,
        bus = EventBusSubscriber.Bus.MOD,
        value = Dist.CLIENT
)
public final class ClientKeyMappings {
    public static final KeyMapping INSPECT = new KeyMapping(
            "key.firstpersonfoodeating.inspect",
            GLFW.GLFW_KEY_V,
            "key.categories.firstpersonfoodeating"
    );

    private ClientKeyMappings() {
    }

    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(INSPECT);
    }
}
