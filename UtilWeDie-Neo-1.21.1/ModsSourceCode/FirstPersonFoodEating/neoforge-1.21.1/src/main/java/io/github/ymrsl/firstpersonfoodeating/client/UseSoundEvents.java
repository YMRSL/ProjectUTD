package io.github.ymrsl.firstpersonfoodeating.client;

import io.github.ymrsl.firstpersonfoodeating.FirstPersonFoodEatingMod;
import io.github.ymrsl.firstpersonfoodeating.client.script.runtime.FoodAssetsManager;
import io.github.ymrsl.firstpersonfoodeating.client.script.runtime.FoodDisplayDefinition;
import io.github.ymrsl.firstpersonfoodeating.item.ConsumableUseLockController;
import io.github.ymrsl.firstpersonfoodeating.item.FoodStackData;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.sound.PlaySoundEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(
        modid = FirstPersonFoodEatingMod.MOD_ID,
        bus = EventBusSubscriber.Bus.GAME,
        value = Dist.CLIENT
)
public final class UseSoundEvents {
    private UseSoundEvents() {
    }

    @SubscribeEvent
    public static void onPlaySound(PlaySoundEvent event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null) {
            return;
        }

        boolean suppressByActiveCustomUse = false;
        if (minecraft.player.isUsingItem()) {
            ItemStack using = minecraft.player.getUseItem();
            if (!using.isEmpty()) {
                ResourceLocation itemId = FoodStackData.resolveFoodId(using);
                FoodDisplayDefinition display = FoodAssetsManager.get().getDisplay(itemId).orElse(null);
                suppressByActiveCustomUse = display != null;
            }
        }

        boolean suppressByUseEndWindow = ConsumableUseLockController.shouldSuppressVanillaConsumeSound(minecraft.player);
        if (!suppressByActiveCustomUse && !suppressByUseEndWindow) {
            return;
        }

        if (event.getSound() == null) {
            return;
        }
        ResourceLocation soundId = event.getSound().getLocation();
        if (soundId == null) {
            return;
        }
        if (soundId.equals(SoundEvents.GENERIC_DRINK.getLocation())
                || soundId.equals(SoundEvents.GENERIC_EAT.getLocation())
                || soundId.equals(SoundEvents.PLAYER_BURP.getLocation())) {
            event.setSound(null);
        }
    }
}
