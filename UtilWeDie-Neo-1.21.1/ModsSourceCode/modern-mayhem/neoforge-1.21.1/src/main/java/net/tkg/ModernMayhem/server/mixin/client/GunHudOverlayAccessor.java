package net.tkg.ModernMayhem.server.mixin.client;

import com.tacz.guns.client.gui.overlay.GunHudOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value={GunHudOverlay.class})
public interface GunHudOverlayAccessor {
    @Accessor(value="cacheInventoryAmmoCount", remap=false)
    public static int getCacheInventoryAmmoCount() {
        throw new AssertionError();
    }

    @Accessor(value="cacheInventoryAmmoCount", remap=false)
    public static void setCacheInventoryAmmoCount(int value) {
        throw new AssertionError();
    }
}

