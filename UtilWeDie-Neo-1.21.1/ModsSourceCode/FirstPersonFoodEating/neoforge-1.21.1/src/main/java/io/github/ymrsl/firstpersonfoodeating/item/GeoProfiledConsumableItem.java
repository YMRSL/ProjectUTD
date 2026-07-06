package io.github.ymrsl.firstpersonfoodeating.item;

import io.github.ymrsl.firstpersonfoodeating.client.script.runtime.geo.FoodGeoItemRenderer;
import java.util.function.Consumer;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.world.item.UseAnim;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class GeoProfiledConsumableItem extends ProfiledConsumableItem {
    private static final Logger LOGGER = LogManager.getLogger();

    public GeoProfiledConsumableItem(Properties properties, UseAnim useAnim, int useDurationTicks) {
        super(properties, useAnim, useDurationTicks);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        LOGGER.info("[firstpersonfoodeating] Attach custom geo item renderer for {}", this);
        consumer.accept(new IClientItemExtensions() {
            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                return FoodGeoItemRenderer.getInstance();
            }
        });
    }
}
