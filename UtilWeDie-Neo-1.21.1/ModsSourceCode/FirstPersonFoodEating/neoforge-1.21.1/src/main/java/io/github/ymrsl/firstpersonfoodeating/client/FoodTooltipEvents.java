package io.github.ymrsl.firstpersonfoodeating.client;

import io.github.ymrsl.firstpersonfoodeating.FirstPersonFoodEatingMod;
import io.github.ymrsl.firstpersonfoodeating.client.script.runtime.FoodAssetsManager;
import io.github.ymrsl.firstpersonfoodeating.client.script.runtime.FoodDisplayDefinition;
import io.github.ymrsl.firstpersonfoodeating.item.FoodStackData;
import io.github.ymrsl.firstpersonfoodeating.registry.ModItems;
import java.util.List;
import java.util.Locale;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.fml.common.EventBusSubscriber;
import net.minecraft.core.registries.BuiltInRegistries;
import org.jetbrains.annotations.Nullable;

@EventBusSubscriber(
        modid = FirstPersonFoodEatingMod.MOD_ID,
        bus = EventBusSubscriber.Bus.GAME,
        value = Dist.CLIENT
)
public final class FoodTooltipEvents {
    private FoodTooltipEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onItemTooltip(ItemTooltipEvent event) {
        if (ModItems.PACK_FOOD == null || !ModItems.PACK_FOOD.isBound()) {
            return;
        }
        ItemStack stack = event.getItemStack();
        if (stack.isEmpty() || !stack.is(ModItems.PACK_FOOD.get())) {
            return;
        }
        FoodStackData.synchronizeVanillaFoodComponent(stack);

        ResourceLocation foodId = FoodStackData.getFoodId(stack).orElse(null);
        if (foodId == null) {
            return;
        }
        FoodDisplayDefinition display = FoodAssetsManager.get().getDisplay(foodId).orElse(null);
        List<Component> tooltip = event.getToolTip();
        if (tooltip == null) {
            return;
        }

        boolean showNutrition = display == null || display.shouldTooltipShowNutrition();
        boolean showEffects = display == null || display.shouldTooltipShowEffects();

        if (showNutrition) {
            int nutrition = FoodStackData.getNutrition(stack, 0);
            float saturationModifier = FoodStackData.getSaturation(stack, 0.0f);
            double saturationPoints = Math.max(nutrition * saturationModifier * 2.0f, 0.0f);
            if (nutrition > 0) {
                tooltip.add(Component.translatable("tooltip.firstpersonfoodeating.nutrition", nutrition).withStyle(ChatFormatting.GRAY));
            }
            if (saturationPoints > 0.0001d) {
                tooltip.add(Component.translatable(
                        "tooltip.firstpersonfoodeating.saturation",
                        formatDecimal(saturationPoints)
                ).withStyle(ChatFormatting.DARK_GRAY));
            }
        }
        FoodStackData.ThirstSpec thirstSpec = FoodStackData.getThirstSpec(stack).orElse(null);
        if (thirstSpec != null) {
            if (thirstSpec.thirstDelta() != 0) {
                tooltip.add(Component.translatable(
                        "tooltip.firstpersonfoodeating.thirst",
                        formatSigned(thirstSpec.thirstDelta())
                ).withStyle(ChatFormatting.AQUA));
            }
            if (thirstSpec.waterDelta() != 0) {
                tooltip.add(Component.translatable(
                        "tooltip.firstpersonfoodeating.hydration",
                        formatSigned(thirstSpec.waterDelta())
                ).withStyle(ChatFormatting.DARK_AQUA));
            }
        }

        if (showEffects) {
            List<FoodStackData.FoodEffect> effects = FoodStackData.getEffects(stack);
            List<FoodStackData.CustomEffect> customEffects = FoodStackData.getCustomEffects(stack);
            if (!effects.isEmpty()) {
                boolean headerAdded = false;
                for (FoodStackData.FoodEffect effect : effects) {
                    Component effectLine = formatEffectLine(effect);
                    if (effectLine == null) {
                        continue;
                    }
                    if (!headerAdded) {
                        tooltip.add(Component.translatable("tooltip.firstpersonfoodeating.effects").withStyle(ChatFormatting.AQUA));
                        headerAdded = true;
                    }
                    tooltip.add(effectLine);
                }
            }
            if (!customEffects.isEmpty()) {
                boolean headerAdded = false;
                for (FoodStackData.CustomEffect customEffect : customEffects) {
                    Component effectLine = formatCustomEffectLine(customEffect);
                    if (effectLine == null) {
                        continue;
                    }
                    if (!headerAdded) {
                        tooltip.add(Component.translatable("tooltip.firstpersonfoodeating.custom_effects").withStyle(ChatFormatting.GOLD));
                        headerAdded = true;
                    }
                    tooltip.add(effectLine);
                }
            }
        }

        if (display != null) {
            for (FoodDisplayDefinition.TooltipLine line : display.resolveTooltipLines()) {
                tooltip.add(line.toComponent());
            }
        }
    }

    private static @Nullable Component formatEffectLine(FoodStackData.FoodEffect configuredEffect) {
        if (configuredEffect == null || configuredEffect.effectId() == null) {
            return null;
        }
        MobEffect effect = net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT
                .get(configuredEffect.effectId());
        if (effect == null) {
            return null;
        }
        MutableComponent line = Component.translatable(effect.getDescriptionId()).copy();
        if (configuredEffect.amplifier() > 0) {
            line.append(" ").append(Component.translatable("potion.potency." + configuredEffect.amplifier()));
        }
        line.append(Component.translatable(
                "tooltip.firstpersonfoodeating.effect_duration",
                String.format(Locale.ROOT, "%.1f", configuredEffect.durationTicks() / 20.0f)
        ));
        if (configuredEffect.chance() < 0.999f) {
            int percent = Math.round(configuredEffect.chance() * 100.0f);
            line.append(Component.translatable("tooltip.firstpersonfoodeating.effect_chance", percent));
        }
        return line.withStyle(ChatFormatting.BLUE);
    }

    private static @Nullable Component formatCustomEffectLine(FoodStackData.CustomEffect effect) {
        if (effect == null || effect.type() == null || effect.type().isBlank()) {
            return null;
        }
        return Component.translatable(
                "tooltip.firstpersonfoodeating.custom_effect." + effect.type(),
                String.format(Locale.ROOT, "%.2f", effect.value()),
                String.format(Locale.ROOT, "%.1f", effect.durationTicks() / 20.0f)
        ).withStyle(ChatFormatting.GOLD);
    }

    private static String formatSigned(int value) {
        if (value > 0) {
            return "+" + value;
        }
        return Integer.toString(value);
    }

    private static String formatDecimal(double value) {
        double roundedInt = Math.rint(value);
        if (Math.abs(value - roundedInt) < 1.0e-4) {
            return Integer.toString((int) roundedInt);
        }
        double roundedOneDecimal = Math.rint(value * 10.0) / 10.0;
        if (Math.abs(value - roundedOneDecimal) < 1.0e-4) {
            return String.format(Locale.ROOT, "%.1f", roundedOneDecimal);
        }
        return String.format(Locale.ROOT, "%.2f", value);
    }
}
