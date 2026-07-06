package io.github.ymrsl.firstpersonfoodeating.item;

import io.github.ymrsl.firstpersonfoodeating.registry.ModMobEffects;
import java.util.ArrayList;
import net.minecraft.core.Holder;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.core.registries.BuiltInRegistries;

public class ProfiledConsumableItem extends Item {
    private static final int LOCK_TRIGGER_TICKS = 20;
    private static final int MAX_USE_DURATION = 72_000;
    // 用户定制：医疗包每降低 1 个 Sona 有害效果 1 级所消耗的耐久(治疗成本统一降到 1/3，原为 3)。
    private static final int SONA_EFFECT_DURABILITY_COST = 1;
    private final UseAnim useAnim;
    private final int scriptedUseDurationTicks;

    public ProfiledConsumableItem(Properties properties, UseAnim useAnim, int useDurationTicks) {
        super(properties);
        this.useAnim = useAnim;
        this.scriptedUseDurationTicks = Math.max(1, useDurationTicks);
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return MAX_USE_DURATION;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return useAnim;
    }

    @Override
    public int getMaxStackSize(ItemStack stack) {
        return FoodStackData.getMaxStackSize(stack, super.getMaxStackSize(stack));
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        if (!FoodStackData.isDurabilityUseEnabled(stack)) {
            return super.isBarVisible(stack);
        }
        return FoodStackData.getDurabilityMax(stack) > 0;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        if (!FoodStackData.isDurabilityUseEnabled(stack)) {
            return super.getBarWidth(stack);
        }
        int max = Math.max(FoodStackData.getDurabilityMax(stack), 1);
        int current = Mth.clamp(FoodStackData.getDurabilityCurrent(stack), 0, max);
        return Mth.clamp(Math.round(13.0f * ((float) current / (float) max)), 0, 13);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        if (!FoodStackData.isDurabilityUseEnabled(stack)) {
            return super.getBarColor(stack);
        }
        int max = Math.max(FoodStackData.getDurabilityMax(stack), 1);
        int current = Mth.clamp(FoodStackData.getDurabilityCurrent(stack), 0, max);
        float ratio = (float) current / (float) max;
        return Mth.hsvToRgb(ratio / 3.0f, 1.0f, 1.0f);
    }

    @Override
    public Component getName(ItemStack stack) {
        ResourceLocation foodId = FoodStackData.getFoodId(stack).orElse(null);
        if (foodId == null) {
            return super.getName(stack);
        }
        String translationKey = "item." + foodId.getNamespace() + "." + foodId.getPath();
        return Component.translatableWithFallback(translationKey, foodId.getPath());
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (ConsumableUseLockController.isUseTemporarilyBlocked(player)) {
            return InteractionResultHolder.fail(player.getItemInHand(hand));
        }
        ItemStack stack = player.getItemInHand(hand);
        FoodStackData.clearActiveUsePlan(stack);
        int fallbackUseDurationTicks = FoodStackData.getUseDurationTicks(stack, scriptedUseDurationTicks);
        FoodStackData.ActiveUsePlan plan = UsePlanResolver.resolveActiveUsePlan(player, stack, fallbackUseDurationTicks);
        if (plan == null && FoodStackData.isDurabilityUseEnabled(stack)) {
            // 用户定制：满血(无 HP 可恢复→resolveActiveUsePlan 返回 null)时，只要身上有 Sona 有害效果，
            // 或血滴血条(受伤值)未满，医疗包(HP 型耐久物品)仍允许使用以治疗这些。构造一个不恢复 HP(NONE/0)
            // 的占位计划放行使用动作，实际的"补血条 + 每效果 3 耐久降 1 级"在 finishUsingItem 中执行。
            if (isHpDurabilityMedkit(stack) && FoodStackData.getDurabilityCurrent(stack) >= 1
                    && (hasSonaHarmfulEffect(player) || SonaCompatBridge.getMissingInjury(player) > 0)) {
                plan = new FoodStackData.ActiveUsePlan(
                        "use",
                        FoodStackData.getUseDurationTicks(stack, scriptedUseDurationTicks),
                        0,
                        FoodStackData.ResourceType.NONE
                );
            } else {
                return InteractionResultHolder.fail(stack);
            }
        }
        if (plan != null) {
            FoodStackData.setActiveUsePlan(stack, plan);
        }
        return ItemUtils.startUsingInstantly(level, player, hand);
    }

    @Override
    public void onUseTick(Level level, LivingEntity livingEntity, ItemStack stack, int remainingUseDuration) {
        super.onUseTick(level, livingEntity, stack, remainingUseDuration);
        if (!(livingEntity instanceof Player player)) {
            return;
        }
        int usedTicks = Math.max(getUseDuration(stack, livingEntity) - remainingUseDuration, 0);
        int resolvedUseDurationTicks = FoodStackData.getUseDurationTicks(stack, scriptedUseDurationTicks);
        resolvedUseDurationTicks = sanitizeLegacyUseDurationTicks(stack, resolvedUseDurationTicks);
        int triggerTicks = Math.min(LOCK_TRIGGER_TICKS, Math.max(resolvedUseDurationTicks - 1, 1));
        int autoFinishTicks = Math.max(resolvedUseDurationTicks - triggerTicks, 1);
        ConsumableUseLockController.tryStartLock(
                player,
                stack,
                usedTicks,
                triggerTicks,
                autoFinishTicks
        );
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity livingEntity) {
        if (FoodStackData.getFoodId(stack).isEmpty()) {
            return super.finishUsingItem(stack, level, livingEntity);
        }
        if (!(livingEntity instanceof Player player)) {
            return super.finishUsingItem(stack, level, livingEntity);
        }
        FoodStackData.ActiveUsePlan activePlan = FoodStackData.getActiveUsePlan(stack)
                .orElse(new FoodStackData.ActiveUsePlan(
                        "use",
                        FoodStackData.getUseDurationTicks(stack, scriptedUseDurationTicks),
                        1,
                        FoodStackData.ResourceType.NONE
                ));
        boolean durabilityUse = FoodStackData.isDurabilityUseEnabled(stack);
        int nutrition = FoodStackData.getNutrition(stack, 2);
        float saturation = FoodStackData.getSaturation(stack, 0.1f);
        FoodStackData.ThirstSpec thirstSpec = FoodStackData.getThirstSpec(stack).orElse(null);
        boolean thirstLinked = thirstSpec != null && ThirstCompatBridge.isAvailable();
        boolean applyNutrition = FoodStackData.shouldApplyChannel(
                stack, thirstLinked, FoodStackData.EffectChannel.NUTRITION
        );
        boolean applySaturation = FoodStackData.shouldApplyChannel(
                stack, thirstLinked, FoodStackData.EffectChannel.SATURATION
        );
        boolean applyPotionEffects = FoodStackData.shouldApplyChannel(
                stack, thirstLinked, FoodStackData.EffectChannel.EFFECTS
        );
        boolean applyCustomEffects = FoodStackData.shouldApplyChannel(
                stack, thirstLinked, FoodStackData.EffectChannel.CUSTOM_EFFECTS
        );
        if (!level.isClientSide) {
            if (player instanceof ServerPlayer serverPlayer) {
                CriteriaTriggers.CONSUME_ITEM.trigger(serverPlayer, stack);
            }
            if (durabilityUse) {
                int requested = Math.max(activePlan.consumeAmount(), 0);
                int applied = switch (activePlan.resourceType()) {
                    case HP -> UsePlanResolver.applyHealthRestore(player, requested);
                    case ARMOR -> UsePlanResolver.applyArmorRepair(player, requested);
                    default -> requested;
                };
                // 用户定制：所有治疗的耐久消耗降到原来的 1/3。
                int budget = FoodStackData.getDurabilityCurrent(stack);
                int hpCost = Math.min(budget, Math.round(Math.max(applied, 0) / 3.0f));
                budget -= hpCost;
                // 仅 HP 型耐久医疗包：再用剩余耐久联动治疗 Sona——
                //   1) 按"已损血滴血条(受伤值)"补充：1 耐久补 3 点血条(成本 1/3)；
                //   2) 每个 Sona 有害效果消耗 1 耐久(原 3)、降低 1 级。
                int sonaSpent = 0;
                if (isHpDurabilityMedkit(stack)) {
                    int refilled = SonaCompatBridge.refillInjury(player, budget * 3);
                    int injCost = Math.min(budget, Math.round(refilled / 3.0f));
                    budget -= injCost;
                    sonaSpent += injCost;
                    int effCost = treatSonaEffects(player, budget);
                    budget -= effCost;
                    sonaSpent += effCost;
                }
                int totalCost = hpCost + sonaSpent;
                int remainingDurability = Math.max(FoodStackData.getDurabilityCurrent(stack) - totalCost, 0);
                FoodStackData.setDurabilityCurrent(stack, remainingDurability);
                if (applied > 0 || sonaSpent > 0) {
                    if (applyPotionEffects) {
                        applyConfiguredEffects(player, stack);
                    }
                    if (applyCustomEffects) {
                        applyConfiguredCustomEffects(player, stack);
                    }
                    applyThirstLinkedDelta(player, thirstSpec);
                }
                if (!player.getAbilities().instabuild && remainingDurability <= 0) {
                    stack.shrink(1);
                }
            } else {
                int eatNutrition = applyNutrition ? nutrition : 0;
                float eatSaturation = applySaturation ? saturation : 0.0f;
                if (eatNutrition > 0) {
                    player.getFoodData().eat(eatNutrition, eatSaturation);
                } else if (eatSaturation > 0.0f) {
                    float addedSaturation = Math.max(eatSaturation * 2.0f, 0.0f);
                    float nextSaturation = Math.min(
                            player.getFoodData().getSaturationLevel() + addedSaturation,
                            player.getFoodData().getFoodLevel()
                    );
                    player.getFoodData().setSaturation(nextSaturation);
                }
                if (applyPotionEffects) {
                    applyConfiguredEffects(player, stack);
                }
                if (applyCustomEffects) {
                    applyConfiguredCustomEffects(player, stack);
                }
                applyThirstLinkedDelta(player, thirstSpec);
                if (!player.getAbilities().instabuild) {
                    stack.shrink(1);
                }
            }
            // 用户定制：按 foodId 处理 FPE 医疗物品 ↔ Sona 的联动（绷带止血 / 针剂·药瓶抗感染解毒等）。
            // 因所有 FPE 食物共用注册物品 pack_food，无法用 Sona 的"按注册 id 匹配"配置区分，故在此按 foodId 分发。
            applySonaItemLinkage(player, stack);
            player.awardStat(Stats.ITEM_USED.get(this));
        }
        FoodStackData.clearActiveUsePlan(stack);
        return stack;
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity livingEntity, int timeLeft) {
        super.releaseUsing(stack, level, livingEntity, timeLeft);
        if (!(livingEntity instanceof Player player)) {
            FoodStackData.clearActiveUsePlan(stack);
            return;
        }
        if (!ConsumableUseLockController.isLocked(player, stack)) {
            FoodStackData.clearActiveUsePlan(stack);
        }
    }

    private static int sanitizeLegacyUseDurationTicks(ItemStack stack, int currentTicks) {
        ResourceLocation foodId = FoodStackData.getFoodId(stack).orElse(null);
        if (foodId == null) {
            return currentTicks;
        }
        String path = foodId.getPath();
        if (path == null || path.isBlank()) {
            return currentTicks;
        }
        // Migrate old bang_a/b/c stacks created before precise clip matching.
        if (path.startsWith("i_bang_") && !"i_bang_d".equals(path) && currentTicks > 70) {
            int fixed = 62;
            FoodStackData.applyProfile(
                    stack,
                    foodId,
                    fixed,
                    FoodStackData.getNutrition(stack, 2),
                    FoodStackData.getSaturation(stack, 0.1f),
                    FoodStackData.getMaxStackSize(stack, stack.getMaxStackSize())
            );
            return fixed;
        }
        return currentTicks;
    }

    private static void applyConfiguredEffects(Player player, ItemStack stack) {
        for (FoodStackData.FoodEffect configured : FoodStackData.getEffects(stack)) {
            if (configured == null || configured.effectId() == null) {
                continue;
            }
            if (player.getRandom().nextFloat() > configured.chance()) {
                continue;
            }
            var effect = net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT
                    .getHolder(configured.effectId()).orElse(null);
            if (effect == null) {
                continue;
            }
            MobEffectInstance instance = new MobEffectInstance(
                    effect,
                    configured.durationTicks(),
                    configured.amplifier(),
                    configured.ambient(),
                    configured.showParticles(),
                    configured.showIcon()
            );
            player.addEffect(instance, player);
        }
    }

    private static void applyThirstLinkedDelta(Player player, FoodStackData.ThirstSpec thirstSpec) {
        if (player == null || thirstSpec == null) {
            return;
        }
        int thirstDelta = thirstSpec.thirstDelta();
        int waterDelta = thirstSpec.waterDelta();
        if (thirstDelta == 0 && waterDelta == 0) {
            return;
        }
        if (!ThirstCompatBridge.isAvailable()) {
            return;
        }
        ThirstCompatBridge.applyThirstDelta(player, thirstDelta, waterDelta);
    }

    private static void applyConfiguredCustomEffects(Player player, ItemStack stack) {
        for (FoodStackData.CustomEffect custom : FoodStackData.getCustomEffects(stack)) {
            if (custom == null || custom.type() == null || custom.type().isBlank()) {
                continue;
            }
            if (player.getRandom().nextFloat() > custom.chance()) {
                continue;
            }
            String type = custom.type();
            switch (type) {
                case "heal_instant" -> {
                    if (custom.value() > 0.0f) {
                        player.heal(custom.value());
                    }
                }
                case "heal_gradual" -> {
                    int durationTicks = custom.durationTicks() > 0
                            ? custom.durationTicks()
                            : Math.max(Math.round(custom.value() * 20.0f), 20);
                    if (durationTicks > 0) {
                        applyOrRefreshEffect(
                                player,
                                ModMobEffects.HEAL_GRADUAL,
                                durationTicks,
                                0,
                                false,
                                false
                        );
                    }
                }
                case "bandage" -> {
                    int durationTicks = custom.durationTicks() > 0 ? custom.durationTicks() : 72_000;
                    int intervalTicks = custom.intervalTicks() > 0
                            ? custom.intervalTicks()
                            : Math.max(Math.round(custom.value() * 20.0f), 1);
                    int intervalAmplifier = Mth.clamp(intervalTicks - 1, 0, 254);
                    if (durationTicks > 0) {
                        applyOrRefreshEffect(
                                player,
                                ModMobEffects.BANDAGE,
                                durationTicks,
                                intervalAmplifier,
                                false,
                                false
                        );
                    }
                }
                case "immune" -> {
                    int durationTicks = custom.durationTicks() > 0
                            ? custom.durationTicks()
                            : Math.max(Math.round(custom.value() * 20.0f), 20);
                    if (durationTicks > 0) {
                        applyOrRefreshEffect(
                                player,
                                ModMobEffects.IMMUNE,
                                durationTicks,
                                0,
                                false,
                                false
                        );
                    }
                    clearHarmfulEffects(player);
                }
                case "extra_armor" -> {
                    if (custom.value() > 0.0f) {
                        player.setAbsorptionAmount(Math.max(player.getAbsorptionAmount(), custom.value()));
                    }
                }
                case "emergency_painkiller" -> {
                    int totalAmount = Math.max(Math.round(custom.value()), 0);
                    if (totalAmount > 0) {
                        CustomStatusEffectController.applyEmergencyPainkiller(player, totalAmount);
                    }
                }
                case "healthy" -> {
                    int durationTicks = custom.durationTicks() > 0
                            ? custom.durationTicks()
                            : Math.max(Math.round(custom.value() * 20.0f), 20);
                    if (durationTicks > 0) {
                        applyOrRefreshEffect(
                                player,
                                ModMobEffects.HEALTHY,
                                durationTicks,
                                0,
                                true,
                                false
                        );
                    }
                }
                case "armor_restore_durability" -> {
                    // Handled by durability_use + use_selector pipeline.
                }
                default -> {
                    // Unknown custom effect type: silently ignore for forward compatibility.
                }
            }
        }
    }

    private static void applyOrRefreshEffect(
            Player player,
            Holder<MobEffect> effect,
            int durationTicks,
            int amplifier,
            boolean stackDuration,
            boolean strongerOnly
    ) {
        if (player == null || effect == null || durationTicks <= 0) {
            return;
        }
        MobEffectInstance current = player.getEffect(effect);
        int nextDuration = durationTicks;
        int nextAmplifier = Math.max(amplifier, 0);
        if (current != null) {
            if (stackDuration) {
                nextDuration = Mth.clamp(current.getDuration() + durationTicks, 1, 72_000);
                nextAmplifier = Math.max(current.getAmplifier(), nextAmplifier);
            } else if (strongerOnly) {
                if (current.getAmplifier() > nextAmplifier) {
                    return;
                }
                if (current.getAmplifier() == nextAmplifier && current.getDuration() >= durationTicks) {
                    return;
                }
                nextDuration = Math.max(current.getDuration(), durationTicks);
            } else {
                nextDuration = Math.max(current.getDuration(), durationTicks);
                nextAmplifier = Math.max(current.getAmplifier(), nextAmplifier);
            }
        }
        MobEffectInstance next = new MobEffectInstance(effect, nextDuration, nextAmplifier, false, true, true);
        player.addEffect(next, player);
    }

    private static void clearHarmfulEffects(Player player) {
        for (MobEffectInstance instance : new ArrayList<>(player.getActiveEffects())) {
            Holder<MobEffect> effect = instance.getEffect();
            if (effect != null && effect.value().getCategory() == MobEffectCategory.HARMFUL) {
                player.removeEffect(effect);
            }
        }
    }

    // ===== 用户定制：医疗包 ↔ Sona 联动辅助 =====

    /**
     * 是否为参与 Sona 耐久治疗联动的「医疗包/急救包」：须是 HP 型耐久物品，且 food id 属于急救包家族
     * (jijiubao / i_jijiubao_*)。整合包里真正的 HP 耐久医疗物品就是 i_jijiubao_a/b/c；i_jia_*(护甲修复)
     * 因 resource=armor 已被排除，i_dai_*(薯片零食)等无耐久也被排除。绷带/针剂/药瓶是普通食物，走
     * {@link #applySonaItemLinkage} 的 foodId 分发，不在此列。
     */
    private static boolean isHpDurabilityMedkit(ItemStack stack) {
        if (!FoodStackData.isDurabilityUseEnabled(stack)) {
            return false;
        }
        boolean hp = FoodStackData.getDurabilityUseSpec(stack)
                .map(spec -> spec.resourceType() == FoodStackData.ResourceType.HP)
                .orElse(false);
        if (!hp) {
            return false;
        }
        ResourceLocation foodId = FoodStackData.resolveFoodId(stack);
        if (foodId == null) {
            return false;
        }
        String path = foodId.getPath();
        return path.equals("jijiubao") || path.startsWith("i_jijiubao_");
    }

    /**
     * 按 foodId 分发 FPE 医疗物品与 Sona 的联动（食物类，无耐久，alwaysEdible 随时可用）：
     * <ul>
     *   <li>绷带 i_bengdai_* → 止血(补血滴血条 + 清流血)。</li>
     *   <li>仅指定抗感染药物降低感染度并解毒：i_zhenji_c(抗感染针剂)-20、i_zhenji_d(超人战斗针剂)-40、
     *       i_zhenji_f(T 型病毒短效抗原针剂)-60、i_yaoping_a(T 病毒抗生素/抗病毒药剂)-30。</li>
     *   <li>其余针剂(肾上腺素/体力/温度免疫/尸化试剂)、维生素 i_yaoping_b、急救包等不在此处理——
     *       急救包走耐久分支 Part B，其它保留各自原有配置效果。</li>
     * </ul>
     */
    private static void applySonaItemLinkage(Player player, ItemStack stack) {
        ResourceLocation foodId = FoodStackData.resolveFoodId(stack);
        if (foodId == null) {
            return;
        }
        String p = foodId.getPath();
        if (p.startsWith("i_bengdai_") || p.equals("bengdai")) {
            SonaCompatBridge.addInjuryAndBandage(player, 8.0f, 15.0f);
            return;
        }
        if (p.equals("i_zhenji_g")) {
            // 尸化实验药剂：强力 buff 但代价是大幅增加感染 + 中毒/饥饿。
            SonaCompatBridge.addInfection(player, 50.0);                                            // +50 感染度
            player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 2400, 2, false, true));       // 饥饿3，2 分钟
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, 2400, 2, false, true));  // 力量3
            player.addEffect(new MobEffectInstance(MobEffects.DIG_SPEED, 2400, 1, false, true));     // 急迫2
            player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 2400, 0, false, true));// 速度1
            player.addEffect(new MobEffectInstance(MobEffects.JUMP, 2400, 0, false, true));          // 跳跃提升1
            player.setAbsorptionAmount(Math.max(player.getAbsorptionAmount(), 10.0f));               // 额外生命值10点
            player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 200, 1, false, true));   // 生命恢复2，10 秒
            player.addEffect(new MobEffectInstance(MobEffects.POISON, 600, 0, false, true));         // 中毒1，30 秒
            return;
        }
        double infectionCure = switch (p) {
            case "i_zhenji_c" -> -20.0; // 抗感染针剂(短效抗感染)
            case "i_zhenji_d" -> -40.0; // 超人战斗针剂
            case "i_zhenji_f" -> -60.0; // T 型病毒短效抗原针剂
            case "i_yaoping_a" -> -30.0; // T 病毒抗生素(抗病毒药剂)
            default -> 0.0;
        };
        if (infectionCure < 0) {
            SonaCompatBridge.addInfection(player, infectionCure);
            detoxVanilla(player);
        }
    }

    /** 解毒：清除发霉食物等造成的中毒/反胃(原版效果，不依赖 Sona)。 */
    private static void detoxVanilla(Player player) {
        player.removeEffect(MobEffects.POISON);
        player.removeEffect(MobEffects.CONFUSION);
    }

    /** 该效果是否为 Sona(命名空间 sona) 的有害效果。仅用原版注册表，无需依赖 Sona。 */
    private static boolean isSonaHarmful(MobEffectInstance instance) {
        if (instance == null) {
            return false;
        }
        Holder<MobEffect> effect = instance.getEffect();
        if (effect == null || effect.value().getCategory() != MobEffectCategory.HARMFUL) {
            return false;
        }
        ResourceLocation id = BuiltInRegistries.MOB_EFFECT.getKey(effect.value());
        return id != null && "sona".equals(id.getNamespace());
    }

    private static boolean hasSonaHarmfulEffect(Player player) {
        if (player == null) {
            return false;
        }
        for (MobEffectInstance instance : player.getActiveEffects()) {
            if (isSonaHarmful(instance)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 按当前 Sona 有害效果逐个降低 1 级，每个消耗 {@link #SONA_EFFECT_DURABILITY_COST} 耐久，
     * 受 {@code availableDurability} 上限约束(耐久不足以再降一个时停止)。返回实际消耗的耐久。
     * 降 1 级：amplifier>0 则以 amplifier-1、保留剩余时长重新施加；amplifier==0(1 级) 则直接移除。
     */
    private static int treatSonaEffects(Player player, int availableDurability) {
        if (player == null || availableDurability < SONA_EFFECT_DURABILITY_COST) {
            return 0;
        }
        java.util.List<MobEffectInstance> targets = new ArrayList<>();
        for (MobEffectInstance instance : player.getActiveEffects()) {
            if (isSonaHarmful(instance)) {
                targets.add(instance);
            }
        }
        int spent = 0;
        for (MobEffectInstance instance : targets) {
            if (availableDurability - spent < SONA_EFFECT_DURABILITY_COST) {
                break;
            }
            Holder<MobEffect> effect = instance.getEffect();
            int amplifier = instance.getAmplifier();
            int duration = instance.getDuration();
            player.removeEffect(effect);
            if (amplifier > 0) {
                player.addEffect(new MobEffectInstance(effect, duration, amplifier - 1,
                        instance.isAmbient(), instance.isVisible(), instance.showIcon()), player);
            }
            spent += SONA_EFFECT_DURABILITY_COST;
        }
        return spent;
    }
}
