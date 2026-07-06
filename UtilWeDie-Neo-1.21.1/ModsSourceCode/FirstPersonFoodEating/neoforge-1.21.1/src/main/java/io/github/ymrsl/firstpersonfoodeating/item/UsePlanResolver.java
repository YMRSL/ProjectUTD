package io.github.ymrsl.firstpersonfoodeating.item;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

public final class UsePlanResolver {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final boolean DEBUG_USE_SELECTOR = Boolean.getBoolean("firstpersonfoodeating.debug_use_selector");
    private static int debugBudget = 120;

    private UsePlanResolver() {
    }

    public static @Nullable FoodStackData.ActiveUsePlan resolveActiveUsePlan(
            Player player,
            ItemStack stack,
            int fallbackDurationTicks
    ) {
        if (player == null || stack.isEmpty()) {
            return null;
        }
        FoodStackData.UseSelectorSpec selector = FoodStackData.getUseSelectorSpec(stack).orElse(null);
        FoodStackData.DurabilityUseSpec durability = FoodStackData.getDurabilityUseSpec(stack).orElse(null);
        boolean durabilityEnabled = durability != null && durability.enabled();

        FoodStackData.ResourceType targetResource = FoodStackData.ResourceType.NONE;
        if (durabilityEnabled) {
            targetResource = durability.resourceType();
        } else if (selector != null) {
            targetResource = selectorModeToResource(selector.mode());
        }

        int availableAmount = durabilityEnabled ? durability.currentDurability() : Integer.MAX_VALUE;
        int requestedAmount = requestedAmountByResource(player, targetResource);
        if (targetResource == FoodStackData.ResourceType.NONE) {
            requestedAmount = 1;
        }
        if (durabilityEnabled) {
            if (availableAmount <= 0) {
                return null;
            }
            if (targetResource != FoodStackData.ResourceType.NONE && requestedAmount <= 0) {
                return null;
            }
        }
        if (!durabilityEnabled && requestedAmount <= 0) {
            requestedAmount = 1;
        }

        int consumeAmount = Math.max(1, Math.min(requestedAmount, availableAmount));
        if (targetResource != FoodStackData.ResourceType.NONE && requestedAmount > 0) {
            consumeAmount = Math.min(requestedAmount, availableAmount);
        }
        if (consumeAmount <= 0) {
            return null;
        }

        String selectedClip = selector == null ? "use" : selector.defaultClip();
        int selectedDurationTicks = Math.max(fallbackDurationTicks, 1);
        if (selector != null && !selector.rules().isEmpty()) {
            FoodStackData.UseSelectorRule matchedRule = pickRule(selector.rules(), consumeAmount);
            if (matchedRule != null) {
                selectedClip = matchedRule.clipName();
                selectedDurationTicks = matchedRule.durationTicks();
            }
        }
        FoodStackData.ActiveUsePlan plan = new FoodStackData.ActiveUsePlan(
                selectedClip,
                selectedDurationTicks,
                consumeAmount,
                targetResource
        );
        if (DEBUG_USE_SELECTOR && debugBudget > 0) {
            debugBudget--;
            LOGGER.info(
                    "[firstpersonfoodeating] UsePlan resolved: food={}, clip={}, duration={}, amount={}, resource={}, durabilityEnabled={}, available={}, requested={}",
                    FoodStackData.resolveFoodId(stack),
                    plan.clipName(),
                    plan.durationTicks(),
                    plan.consumeAmount(),
                    plan.resourceType().serializedName(),
                    durabilityEnabled,
                    availableAmount,
                    requestedAmount
            );
        }
        return plan;
    }

    public static int applyHealthRestore(Player player, int restoreAmount) {
        if (player == null || restoreAmount <= 0) {
            return 0;
        }
        float before = player.getHealth();
        player.heal(restoreAmount);
        float delta = Math.max(player.getHealth() - before, 0.0f);
        return Math.round(delta);
    }

    public static int applyArmorRepair(Player player, int repairAmount) {
        if (player == null || repairAmount <= 0) {
            return 0;
        }
        List<ItemStack> repairable = new ArrayList<>();
        for (ItemStack armor : player.getInventory().armor) {
            if (armor == null || armor.isEmpty() || !armor.isDamageableItem()) {
                continue;
            }
            if (armor.getDamageValue() <= 0) {
                continue;
            }
            repairable.add(armor);
        }
        repairable.sort(Comparator.comparingInt(ItemStack::getDamageValue).reversed());
        int remaining = repairAmount;
        int repaired = 0;
        for (ItemStack armor : repairable) {
            if (remaining <= 0) {
                break;
            }
            int currentDamage = Math.max(armor.getDamageValue(), 0);
            if (currentDamage <= 0) {
                continue;
            }
            int fix = Math.min(currentDamage, remaining);
            armor.setDamageValue(currentDamage - fix);
            remaining -= fix;
            repaired += fix;
        }
        if (repaired > 0) {
            player.getInventory().setChanged();
        }
        return repaired;
    }

    public static int getMissingArmorDurability(Player player) {
        if (player == null) {
            return 0;
        }
        int missing = 0;
        for (ItemStack armor : player.getInventory().armor) {
            if (armor == null || armor.isEmpty() || !armor.isDamageableItem()) {
                continue;
            }
            missing += Math.max(armor.getDamageValue(), 0);
        }
        return Math.max(missing, 0);
    }

    private static FoodStackData.ResourceType selectorModeToResource(FoodStackData.UseSelectorMode mode) {
        if (mode == null) {
            return FoodStackData.ResourceType.NONE;
        }
        return switch (mode) {
            case HP -> FoodStackData.ResourceType.HP;
            case ARMOR -> FoodStackData.ResourceType.ARMOR;
            default -> FoodStackData.ResourceType.NONE;
        };
    }

    private static int requestedAmountByResource(Player player, FoodStackData.ResourceType resourceType) {
        if (player == null || resourceType == null) {
            return 0;
        }
        return switch (resourceType) {
            case HP -> {
                float missing = Math.max(player.getMaxHealth() - player.getHealth(), 0.0f);
                yield (int) Math.ceil(missing);
            }
            case ARMOR -> getMissingArmorDurability(player);
            default -> 0;
        };
    }

    private static @Nullable FoodStackData.UseSelectorRule pickRule(
            List<FoodStackData.UseSelectorRule> rules,
            int consumeAmount
    ) {
        if (rules == null || rules.isEmpty()) {
            return null;
        }
        FoodStackData.UseSelectorRule best = null;
        for (FoodStackData.UseSelectorRule rule : rules) {
            if (rule == null) {
                continue;
            }
            if (consumeAmount < rule.minAmount() || consumeAmount > rule.maxAmount()) {
                continue;
            }
            if (best == null) {
                best = rule;
                continue;
            }
            int bestRange = best.maxAmount() - best.minAmount();
            int range = rule.maxAmount() - rule.minAmount();
            if (range < bestRange) {
                best = rule;
            }
        }
        if (best != null) {
            return best;
        }
        FoodStackData.UseSelectorRule lower = null;
        FoodStackData.UseSelectorRule upper = null;
        for (FoodStackData.UseSelectorRule rule : rules) {
            if (rule == null) {
                continue;
            }
            if (consumeAmount > rule.maxAmount()) {
                if (lower == null || rule.maxAmount() > lower.maxAmount()) {
                    lower = rule;
                }
                continue;
            }
            if (consumeAmount < rule.minAmount()) {
                if (upper == null || rule.minAmount() < upper.minAmount()) {
                    upper = rule;
                }
            }
        }
        if (lower != null) {
            return lower;
        }
        return upper;
    }
}

