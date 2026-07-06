package io.github.ymrsl.firstpersonfoodeating.client.script.runtime;

import io.github.ymrsl.firstpersonfoodeating.client.script.FoodAnimationConstant;
import io.github.ymrsl.firstpersonfoodeating.client.script.statemachine.LuaAnimationStateMachine;
import io.github.ymrsl.firstpersonfoodeating.client.script.statemachine.LuaStateMachineFactory;
import io.github.ymrsl.firstpersonfoodeating.item.ConsumableUseLockController;
import io.github.ymrsl.firstpersonfoodeating.item.FoodStackData;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.util.RandomSource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.luaj.vm2.LuaTable;

public final class FoodScriptedHandStateMachine {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final float TICK_SECONDS = 1.0f / 20.0f;
    private static final int BASE_TRACK = 0;
    private static final int MAIN_TRACK = 1;
    private static final int PUT_AWAY_TICKS = 6;
    // 偶发：使用上升沿那一帧 INPUT_USE 撞上 draw/切换混合态被状态机丢弃，导致整段使用停在 idle/hold。
    // 上升沿后开一个短重试窗口，使用期间若"使用动作 clip"还没真正播上就补触发，直到进入为止(进入即停，不重播)。
    private static final int USE_TRIGGER_RETRY_TICKS = 6;
    private static int useEndDrawLogBudget = 12;
    private static int bobStateLogBudget = 12;
    private static int bobSkipLogBudget = 10;
    private static int soundResolveWarnBudget = 16;
    private static int soundPlayLogBudget = 24;
    private static int soundFallbackLogBudget = 24;
    private static int autoSoundLogBudget = 32;

    private LuaAnimationStateMachine<FoodAnimationStateContext> stateMachine;
    private FoodAnimationController controller;
    private FoodDisplayDefinition activeDisplay;
    private ResourceLocation trackedItemKey;
    private int trackedSlot = -1;
    private FoodDisplayDefinition pendingDisplay;
    private ResourceLocation pendingItemKey;
    private int pendingSlot = -1;
    private int pendingPutAwayTicks = 0;
    private boolean inspectRequested;
    private boolean wasUsingMainHand;
    private int useTriggerRetryTicks;
    private String lastMoveInput = null;
    private FoodAnimationRunner trackedMainRunner;
    private float trackedMainRunnerPrevSeconds = 0.0f;
    private List<TimedSound> autoClipSounds = List.of();
    private int autoClipSoundIndex = 0;

    public void requestInspect() {
        inspectRequested = true;
    }

    public void reset() {
        if (stateMachine != null && stateMachine.isInitialized()) {
            stateMachine.exit();
        }
        stateMachine = null;
        controller = null;
        activeDisplay = null;
        trackedItemKey = null;
        trackedSlot = -1;
        pendingDisplay = null;
        pendingItemKey = null;
        pendingSlot = -1;
        pendingPutAwayTicks = 0;
        inspectRequested = false;
        wasUsingMainHand = false;
        useTriggerRetryTicks = 0;
        lastMoveInput = null;
        trackedMainRunner = null;
        trackedMainRunnerPrevSeconds = 0.0f;
        autoClipSounds = List.of();
        autoClipSoundIndex = 0;
    }

    public boolean handles(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        ResourceLocation key = FoodStackData.resolveFoodId(stack);
        return FoodAssetsManager.get().getDisplay(key).isPresent();
    }

    public boolean tick(LocalPlayer player) {
        ItemStack stack = player.getMainHandItem();
        ResourceLocation itemKey = stack.isEmpty() ? null : FoodStackData.resolveFoodId(stack);
        int selectedSlot = player.getInventory().selected;
        FoodDisplayDefinition display = itemKey == null ? null : FoodAssetsManager.get().getDisplay(itemKey).orElse(null);

        if (pendingPutAwayTicks > 0) {
            // If player changes selection while put-away is still running, keep the pending
            // target in sync so we can switch directly without exposing a vanilla fallback frame.
            pendingDisplay = display;
            pendingItemKey = itemKey;
            pendingSlot = selectedSlot;

            stateMachine.processContextIfExist(context -> {
                context.updateTickData(stack, false, 0.0f, false, false, player.onGround(), 0.0f);
                context.setPartialTicks(0.0f);
                context.setPutAwayTime(0.25f);
                context.setUseClipName(resolveUseClipName(stack));
            });
            stateMachine.update(TICK_SECONDS);
            playTriggeredClipSoundEffects(player);
            playAutoStateClipSounds(player);
            pendingPutAwayTicks--;
            if (pendingPutAwayTicks <= 0) {
                if (pendingDisplay == null || pendingItemKey == null) {
                    // Final safeguard: re-sample current hand item at the exact frame switch ends.
                    ItemStack finalStack = player.getMainHandItem();
                    ResourceLocation finalKey = finalStack.isEmpty() ? null : FoodStackData.resolveFoodId(finalStack);
                    FoodDisplayDefinition finalDisplay =
                            finalKey == null ? null : FoodAssetsManager.get().getDisplay(finalKey).orElse(null);
                    pendingDisplay = finalDisplay;
                    pendingItemKey = finalKey;
                    pendingSlot = player.getInventory().selected;
                }
                if (pendingDisplay != null && pendingItemKey != null) {
                    if (!initForDisplay(pendingDisplay, pendingItemKey, pendingSlot)) {
                        reset();
                        return false;
                    }
                } else {
                    reset();
                    return false;
                }
                pendingDisplay = null;
                pendingItemKey = null;
                pendingSlot = -1;
            }
            wasUsingMainHand = false;
            return true;
        }

        if (stateMachine != null && (display == null || itemKey == null
                || trackedItemKey == null
                || !trackedItemKey.equals(itemKey)
                || trackedSlot != selectedSlot)) {
            queuePutAwayThenSwitch(display, itemKey, selectedSlot);
            stateMachine.processContextIfExist(context -> {
                context.updateTickData(stack, false, 0.0f, false, false, player.onGround(), 0.0f);
                context.setPartialTicks(0.0f);
                context.setPutAwayTime(0.25f);
                context.setUseClipName(resolveUseClipName(stack));
            });
            stateMachine.update(TICK_SECONDS);
            playTriggeredClipSoundEffects(player);
            playAutoStateClipSounds(player);
            wasUsingMainHand = false;
            return true;
        }

        if (display == null || itemKey == null) {
            reset();
            return false;
        }

        if (stateMachine == null || trackedItemKey == null) {
            if (!initForDisplay(display, itemKey, selectedSlot)) {
                reset();
                return false;
            }
        }

        boolean vanillaUsingMainHand = player.isUsingItem() && player.getUsedItemHand() == InteractionHand.MAIN_HAND;
        boolean lockUsingMainHand = ConsumableUseLockController.isLocked(player, stack);
        boolean usingMainHand = vanillaUsingMainHand || lockUsingMainHand;
        float useProgress = lockUsingMainHand
                ? 1.0f
                : computeUseProgress(player, stack, vanillaUsingMainHand);
        boolean moving = player.input != null && !player.isMovingSlowly() && player.input.getMoveVector().length() > 0.01f;
        boolean sprinting = !player.isMovingSlowly() && player.isSprinting();
        boolean onGround = player.onGround();
        float walkDist = player.walkDist + (player.walkDist - player.walkDistO);

        stateMachine.processContextIfExist(context -> {
            context.updateTickData(stack, usingMainHand, useProgress, sprinting, moving, onGround, walkDist);
            context.setPartialTicks(0.0f);
            context.setPutAwayTime(0.25f);
            context.setUseClipName(resolveUseClipName(stack));
        });

        if (inspectRequested) {
            stateMachine.trigger(FoodAnimationConstant.INPUT_INSPECT);
            inspectRequested = false;
        }

        if (usingMainHand && !wasUsingMainHand) {
            stateMachine.trigger(FoodAnimationConstant.INPUT_USE);
            stateMachine.trigger(FoodAnimationConstant.INPUT_RELOAD);
            useTriggerRetryTicks = USE_TRIGGER_RETRY_TICKS;
        } else if (usingMainHand) {
            // 使用持续中：若上升沿的 INPUT_USE 被丢弃(使用动作 clip 尚未播上)，在短窗口内补触发，进入即停。
            if (useTriggerRetryTicks > 0) {
                if (isUseClipPlaying(stack)) {
                    useTriggerRetryTicks = 0;
                } else {
                    stateMachine.trigger(FoodAnimationConstant.INPUT_USE);
                    useTriggerRetryTicks--;
                }
            }
        } else if (wasUsingMainHand) {
            useTriggerRetryTicks = 0;
            stateMachine.trigger(FoodAnimationConstant.INPUT_USE_END);
            if (!stack.isEmpty()) {
                stateMachine.trigger(FoodAnimationConstant.INPUT_DRAW);
                if (useEndDrawLogBudget > 0) {
                    useEndDrawLogBudget--;
                    LOGGER.info("[firstpersonfoodeating] Trigger draw after use_end: item={}, slot={}, stackCount={}",
                            itemKey, selectedSlot, stack.getCount());
                }
            }
        }

        boolean suppressMoveSway = usingMainHand || isMainActionPlaying();
        String moveInput;
        if (suppressMoveSway) {
            moveInput = FoodAnimationConstant.INPUT_IDLE;
        } else if (sprinting) {
            moveInput = FoodAnimationConstant.INPUT_RUN;
        } else if (moving) {
            moveInput = FoodAnimationConstant.INPUT_WALK;
        } else {
            moveInput = FoodAnimationConstant.INPUT_IDLE;
        }
        if (!moveInput.equals(lastMoveInput)) {
            stateMachine.trigger(moveInput);
            lastMoveInput = moveInput;
        }

        stateMachine.update(TICK_SECONDS);
        playTriggeredClipSoundEffects(player);
        playAutoStateClipSounds(player);
        wasUsingMainHand = usingMainHand;
        return true;
    }

    public FoodDisplayDefinition getActiveDisplay() {
        return activeDisplay;
    }

    public boolean isSwitching() {
        return pendingPutAwayTicks > 0;
    }

    /**
     * Retract the current food (put_away) and immediately re-draw the same item. Used when the
     * off hand performs an action (placing/using an item): there is no off-hand food animation, so
     * mirror TaCZ's behaviour of lowering and re-raising the held item to cover the off-hand action
     * instead of showing a frozen "drooping arms" pose. No-op if nothing is active or a switch/
     * retract is already in progress.
     */
    public void triggerOffhandRetractRedraw() {
        if (stateMachine == null || activeDisplay == null || trackedItemKey == null) {
            return;
        }
        if (pendingPutAwayTicks > 0) {
            return;
        }
        queuePutAwayThenSwitch(activeDisplay, trackedItemKey, trackedSlot);
    }

    public boolean shouldApplyVanillaStaticIdleBob(LocalPlayer player) {
        if (player == null || controller == null || stateMachine == null || !stateMachine.isInitialized()) {
            return false;
        }

        FoodAnimationRunner base = controller.getAnimation(BASE_TRACK);
        if (base == null || base.isStopped() || !isStaticIdleClip(base)) {
            if (bobSkipLogBudget > 0) {
                bobSkipLogBudget--;
                LOGGER.info("[firstpersonfoodeating] Skip static_idle bob: baseClip={}, baseStopped={}, mainClip={}",
                        safeClipName(base), base != null && base.isStopped(), safeClipName(controller.getAnimation(MAIN_TRACK)));
            }
            return false;
        }

        FoodAnimationRunner main = controller.getAnimation(MAIN_TRACK);
        if (main != null && !main.isStopped()) {
            return false;
        }

        boolean moving = player.input != null && !player.isMovingSlowly()
                && player.input.getMoveVector().length() > 0.01f;
        boolean jumping = !player.onGround() && !player.getAbilities().flying && !player.isFallFlying();
        boolean flying = player.getAbilities().flying || player.isFallFlying();
        boolean result = moving || jumping || flying;
        if (result && bobStateLogBudget > 0) {
            bobStateLogBudget--;
            LOGGER.info("[firstpersonfoodeating] Apply static_idle vanilla bob: moving={}, jumping={}, flying={}, baseClip={}, mainClip={}",
                    moving, jumping, flying,
                    safeClipName(base),
                    safeClipName(main));
        }
        return result;
    }

    public Map<String, FoodAnimationController.BonePose> sampleBonePose() {
        if (controller == null) {
            return Map.of();
        }
        Minecraft minecraft = Minecraft.getInstance();
        float frameTime = minecraft.isPaused() ? 0.0f : minecraft.getTimer().getGameTimeDeltaPartialTick(true);
        return controller.sampleCombinedBonePose(frameTime);
    }

    public String debugTrackSummary() {
        if (controller == null) {
            return "controller=<null>";
        }
        return "base=" + safeClipName(controller.getAnimation(BASE_TRACK))
                + ",main=" + safeClipName(controller.getAnimation(MAIN_TRACK))
                + ",switching=" + isSwitching();
    }

    public HandPose samplePose(float equipProgress, float swingProgress) {
        if (controller == null) {
            return HandPose.IDENTITY;
        }
        Minecraft minecraft = Minecraft.getInstance();
        float frameTime = minecraft.isPaused() ? 0.0f : minecraft.getTimer().getGameTimeDeltaPartialTick(true);
        FoodAnimationClip.RootTransform root = controller.sampleCombinedRootTransform(frameTime);
        float swing = Mth.sin(Mth.sqrt(swingProgress) * (float) Math.PI);

        float x = root.position().x() / 64.0f - 0.015f * swing;
        float y = -root.position().y() / 64.0f + 0.010f * swing;
        float z = root.position().z() / 64.0f - 0.020f * equipProgress;

        float pitch = -root.rotationDeg().x() * 0.35f - 4.0f * swing;
        float yaw = root.rotationDeg().y() * 0.35f + 4.0f * swing;
        float roll = -root.rotationDeg().z() * 0.35f - 2.0f * swing;
        return new HandPose(x, y, z, pitch, yaw, roll, 1.0f, 1.0f, 1.0f);
    }

    private boolean initForDisplay(FoodDisplayDefinition display, ResourceLocation itemKey, int selectedSlot) {
        BedrockAnimationBank bank = FoodAssetsManager.get().getAnimationBank(display.getAnimationId()).orElse(null);
        LuaTable script = FoodAssetsManager.get().getScript(display.getStateMachineId());
        if (bank == null || script == null) {
            LOGGER.warn("[firstpersonfoodeating] Failed to init scripted state machine for item {} (animation={}, script={}, bankPresent={}, scriptPresent={})",
                    itemKey, display.getAnimationId(), display.getStateMachineId(), bank != null, script != null);
            return false;
        }
        controller = new FoodAnimationController(bank.clips());
        stateMachine = new LuaStateMachineFactory<FoodAnimationStateContext>()
                .setController(controller)
                .setLuaScripts(script)
                .build();
        FoodAnimationStateContext context = new FoodAnimationStateContext();
        context.setPutAwayTime(0.25f);
        context.setUseClipName("use");
        stateMachine.setContext(context);
        stateMachine.initialize();
        stateMachine.trigger(FoodAnimationConstant.INPUT_DRAW);
        LOGGER.info("[firstpersonfoodeating] Scripted state machine initialized for item {} (clips={})",
                itemKey, bank.clips().size());

        activeDisplay = display;
        trackedItemKey = itemKey;
        trackedSlot = selectedSlot;
        wasUsingMainHand = false;
        lastMoveInput = null;
        trackedMainRunner = null;
        trackedMainRunnerPrevSeconds = 0.0f;
        autoClipSounds = List.of();
        autoClipSoundIndex = 0;
        return true;
    }

    private static boolean isStaticIdleClip(FoodAnimationRunner runner) {
        String clip = safeClipName(runner);
        return "static_idle".equals(clip) || clip.endsWith(".static_idle");
    }

    private boolean isMainActionPlaying() {
        if (controller == null) {
            return false;
        }
        FoodAnimationRunner runner = controller.getAnimation(MAIN_TRACK);
        if (runner == null || runner.isStopped()) {
            return false;
        }
        String clipName = runner.getClip().name();
        if (clipName == null || clipName.isBlank()) {
            return false;
        }
        // Do not stack walk/run sway on top of inspect/use/draw/put-away/main track.
        return true;
    }

    /** 主轨当前是否正在播放"使用动作"的 clip（用于判断 INPUT_USE 是否已被状态机接受并进入使用动作）。 */
    private boolean isUseClipPlaying(ItemStack stack) {
        if (controller == null) {
            return false;
        }
        FoodAnimationRunner runner = controller.getAnimation(MAIN_TRACK);
        if (runner == null || runner.isStopped()) {
            return false;
        }
        String clipName = runner.getClip().name();
        if (clipName == null || clipName.isBlank()) {
            return false;
        }
        String useClip = resolveUseClipName(stack);
        return useClip != null && useClip.equals(clipName);
    }

    private static String safeClipName(FoodAnimationRunner runner) {
        if (runner == null || runner.getClip() == null || runner.getClip().name() == null) {
            return "<none>";
        }
        return runner.getClip().name();
    }

    private void queuePutAwayThenSwitch(FoodDisplayDefinition nextDisplay, ResourceLocation nextItemKey, int nextSlot) {
        pendingDisplay = nextDisplay;
        pendingItemKey = nextItemKey;
        pendingSlot = nextSlot;
        pendingPutAwayTicks = PUT_AWAY_TICKS;
        stateMachine.processContextIfExist(context -> context.setPutAwayTime(0.25f));
        stateMachine.trigger(FoodAnimationConstant.INPUT_PUT_AWAY);
        lastMoveInput = null;
    }

    private static float computeUseProgress(LocalPlayer player, ItemStack stack, boolean usingMainHand) {
        if (!usingMainHand) {
            return 0.0f;
        }
        int total = Math.max(stack.getUseDuration(player), 1);
        int remain = Math.max(player.getUseItemRemainingTicks(), 0);
        return Mth.clamp(1.0f - (float) remain / (float) total, 0.0f, 1.0f);
    }

    private static String resolveUseClipName(ItemStack stack) {
        String fromActivePlan = FoodStackData.getActiveUseClipName(stack).orElse(null);
        if (fromActivePlan != null && !fromActivePlan.isBlank()) {
            return fromActivePlan;
        }
        FoodStackData.UseSelectorSpec selector = FoodStackData.getUseSelectorSpec(stack).orElse(null);
        if (selector != null && selector.defaultClip() != null && !selector.defaultClip().isBlank()) {
            return selector.defaultClip();
        }
        return "use";
    }

    private void playTriggeredClipSoundEffects(LocalPlayer player) {
        if (player == null || controller == null || activeDisplay == null) {
            return;
        }
        String fallbackNamespace = activeDisplay.getAnimationId() == null
                ? FoodStackData.resolveFoodId(player.getMainHandItem()).getNamespace()
                : activeDisplay.getAnimationId().getNamespace();
        for (String effectKey : controller.drainTriggeredSoundEffects()) {
            ResourceLocation soundId = resolveSoundEffectId(effectKey, fallbackNamespace);
            ResourceLocation resolved = resolvePlayableSoundId(soundId, effectKey, activeDisplay);
            if (resolved == null) {
                continue;
            }
            playClientSound(player, resolved);
            if (soundPlayLogBudget > 0) {
                soundPlayLogBudget--;
                LOGGER.info("[firstpersonfoodeating] Play animation sound: effect={}, resolved={}", effectKey, resolved);
            }
        }
    }

    private void playAutoStateClipSounds(LocalPlayer player) {
        if (player == null || controller == null || activeDisplay == null) {
            trackedMainRunner = null;
            trackedMainRunnerPrevSeconds = 0.0f;
            autoClipSounds = List.of();
            autoClipSoundIndex = 0;
            return;
        }
        FoodAnimationRunner currentMainRunner = controller.getAnimation(MAIN_TRACK);
        if (currentMainRunner != trackedMainRunner) {
            trackedMainRunner = currentMainRunner;
            trackedMainRunnerPrevSeconds = 0.0f;
            autoClipSounds = buildAutoClipSoundSchedule(currentMainRunner);
            autoClipSoundIndex = 0;
        }
        if (trackedMainRunner == null || autoClipSounds.isEmpty()) {
            return;
        }

        float currentSec = Math.max(trackedMainRunner.getProgressSeconds(), 0.0f);
        float previousSec = Math.max(trackedMainRunnerPrevSeconds, 0.0f);
        if (currentSec + 0.0001f < previousSec) {
            previousSec = 0.0f;
            autoClipSoundIndex = 0;
        }

        while (autoClipSoundIndex < autoClipSounds.size()) {
            TimedSound timed = autoClipSounds.get(autoClipSoundIndex);
            if (timed.timeSeconds() > currentSec + 0.0001f) {
                break;
            }
            boolean crossed = timed.timeSeconds() > previousSec + 0.0001f
                    || (previousSec <= 0.0001f && timed.timeSeconds() <= 0.0001f);
            if (crossed) {
                playClientSound(player, timed.soundId());
                if (autoSoundLogBudget > 0) {
                    autoSoundLogBudget--;
                    LOGGER.info("[firstpersonfoodeating] Play auto clip sound: clip={}, at={}s, sound={}",
                            safeClipName(trackedMainRunner), timed.timeSeconds(), timed.soundId());
                }
            }
            autoClipSoundIndex++;
        }
        trackedMainRunnerPrevSeconds = currentSec;
    }

    private List<TimedSound> buildAutoClipSoundSchedule(FoodAnimationRunner runner) {
        if (runner == null || runner.getClip() == null || activeDisplay == null || activeDisplay.getItemId() == null) {
            return List.of();
        }
        if (!runner.getClip().soundEffectFrames().isEmpty()) {
            return List.of();
        }
        String clipName = normalizeClipSuffix(runner.getClip().name(), activeDisplay.getAnimationId());
        String category = deriveItemCategory(activeDisplay.getItemId());
        if (category == null) {
            return List.of();
        }
        String namespace = activeDisplay.getItemId().getNamespace();
        List<ResourceLocation> categorySounds = collectCategorySoundIds(namespace, category);
        if (categorySounds.isEmpty()) {
            return List.of();
        }

        if (clipName.contains("put")) {
            ResourceLocation putAway = pickSingleSound(categorySounds, id -> isPutAwaySound(id.getPath()));
            return putAway == null ? List.of() : List.of(new TimedSound(0.0f, putAway));
        }
        if (clipName.contains("inspect") || clipName.contains("inpect")) {
            ResourceLocation inspect = pickSingleSound(categorySounds, id -> isInspectSound(id.getPath()));
            return inspect == null ? List.of() : List.of(new TimedSound(0.0f, inspect));
        }
        if (clipName.contains("draw")) {
            ResourceLocation draw = pickSingleSound(categorySounds,
                    id -> isDrawSound(id.getPath()) && !id.getPath().contains("drawspoon"));
            return draw == null ? List.of() : List.of(new TimedSound(0.0f, draw));
        }
        if (!clipName.contains("use")) {
            return List.of();
        }

        List<ResourceLocation> useSequence = selectUseSoundSequence(categorySounds);
        if (useSequence.isEmpty()
                && activeDisplay.getUseSoundId() != null
                && FoodAssetsManager.get().hasSoundEvent(activeDisplay.getUseSoundId())) {
            useSequence = List.of(activeDisplay.getUseSoundId());
        }
        if (useSequence.isEmpty()) {
            return List.of();
        }
        if (useSequence.size() == 1) {
            return List.of(new TimedSound(0.0f, useSequence.get(0)));
        }

        float clipLength = Math.max(runner.getLengthSeconds(), 0.05f);
        float sequenceWindow = Math.max(Math.min(clipLength * 0.85f, clipLength - 0.01f), 0.05f);
        float step = sequenceWindow / (float) (useSequence.size() - 1);
        List<TimedSound> scheduled = new ArrayList<>();
        for (int i = 0; i < useSequence.size(); i++) {
            float t = Math.min(step * i, clipLength - 0.01f);
            scheduled.add(new TimedSound(Math.max(t, 0.0f), useSequence.get(i)));
        }
        return scheduled;
    }

    private static List<ResourceLocation> selectUseSoundSequence(List<ResourceLocation> categorySounds) {
        List<ResourceLocation> primaryUse = new ArrayList<>();
        List<ResourceLocation> secondaryUse = new ArrayList<>();
        for (ResourceLocation id : categorySounds) {
            String path = id.getPath();
            if (isInspectSound(path) || isPutAwaySound(path)) {
                continue;
            }
            if (path.contains("use")) {
                primaryUse.add(id);
                continue;
            }
            if (pathMatchesUse(path)) {
                secondaryUse.add(id);
            }
        }
        List<ResourceLocation> sequence = primaryUse.isEmpty() ? secondaryUse : primaryUse;
        sequence.sort(Comparator.comparing(ResourceLocation::toString));
        return sequence;
    }

    private static List<ResourceLocation> collectCategorySoundIds(String namespace, String category) {
        LinkedHashSet<ResourceLocation> ids = new LinkedHashSet<>();
        ids.addAll(FoodAssetsManager.get().findSoundEventsByPrefix(namespace, category + "."));
        if ("guantou".equals(category)) {
            ids.addAll(FoodAssetsManager.get().findSoundEventsByPrefix(namespace, "item_foodcan_"));
        } else if ("zhenji".equals(category)) {
            ids.addAll(FoodAssetsManager.get().findSoundEventsByPrefix(namespace, "item_injector_"));
        }
        return new ArrayList<>(ids);
    }

    private static ResourceLocation pickSingleSound(
            List<ResourceLocation> soundIds,
            java.util.function.Predicate<ResourceLocation> predicate
    ) {
        return soundIds.stream()
                .filter(predicate)
                .sorted(Comparator.comparing(ResourceLocation::toString))
                .findFirst()
                .orElse(null);
    }

    private static boolean pathMatchesUse(String path) {
        return path.contains("use")
                || path.contains("drink")
                || path.contains("foodtake")
                || path.contains("injection")
                || path.contains("open")
                || path.contains("drawspoon")
                || path.contains("kolpachok");
    }

    private static boolean isInspectSound(String path) {
        return path.contains("inspect") || path.contains("inpect");
    }

    private static boolean isPutAwaySound(String path) {
        return path.contains("put_away") || path.contains("putaway") || path.contains("put_awey");
    }

    private static boolean isDrawSound(String path) {
        return path.contains("draw");
    }

    private static String normalizeClipSuffix(String rawName, ResourceLocation animationId) {
        if (rawName == null) {
            return "";
        }
        String normalized = rawName;
        if (normalized.startsWith("animation.")) {
            normalized = normalized.substring("animation.".length());
        }
        if (animationId != null) {
            String prefix = animationId.getPath() + ".";
            if (normalized.startsWith(prefix)) {
                normalized = normalized.substring(prefix.length());
            }
        }
        int lastDot = normalized.lastIndexOf('.');
        if (lastDot >= 0 && lastDot + 1 < normalized.length()) {
            normalized = normalized.substring(lastDot + 1);
        }
        return normalized.toLowerCase();
    }

    private static String deriveItemCategory(ResourceLocation itemId) {
        if (itemId == null) {
            return null;
        }
        String path = itemId.getPath();
        if (path == null || path.isBlank()) {
            return null;
        }
        String normalized = path.startsWith("i_") ? path.substring(2) : path;
        int split = normalized.indexOf('_');
        if (split <= 0) {
            return normalized;
        }
        return normalized.substring(0, split);
    }

    private static int parseEffectSequenceIndex(String effectLower) {
        if (effectLower == null || effectLower.isBlank()) {
            return -1;
        }
        StringBuilder digits = new StringBuilder();
        boolean inDigits = false;
        for (int i = 0; i < effectLower.length(); i++) {
            char c = effectLower.charAt(i);
            if (c >= '0' && c <= '9') {
                digits.append(c);
                inDigits = true;
                continue;
            }
            if (inDigits) {
                break;
            }
        }
        if (digits.length() == 0) {
            return -1;
        }
        try {
            return Integer.parseInt(digits.toString());
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    private static ResourceLocation resolveSoundEffectId(String effectKey, String fallbackNamespace) {
        if (effectKey == null || effectKey.isBlank()) {
            return null;
        }
        String trimmed = effectKey.trim();
        if (trimmed.contains(":")) {
            return ResourceLocation.tryParse(trimmed);
        }
        ResourceLocation resolved = ResourceLocation.tryParse(fallbackNamespace + ":" + trimmed);
        if (resolved == null && soundResolveWarnBudget > 0) {
            soundResolveWarnBudget--;
            LOGGER.warn("[firstpersonfoodeating] Failed to resolve animation sound id '{}' with fallback namespace '{}'", trimmed, fallbackNamespace);
        }
        return resolved;
    }

    private static ResourceLocation resolvePlayableSoundId(
            ResourceLocation resolvedByEffect,
            String effectKey,
            FoodDisplayDefinition display
    ) {
        if (resolvedByEffect != null && FoodAssetsManager.get().hasSoundEvent(resolvedByEffect)) {
            return resolvedByEffect;
        }
        ResourceLocation alias = resolveEffectAliasSoundId(effectKey, display);
        if (alias != null && FoodAssetsManager.get().hasSoundEvent(alias)) {
            return alias;
        }
        // Avoid repeatedly forcing one fallback sound on mismatched effect ids.
        if (effectKey != null && !effectKey.isBlank()) {
            return null;
        }
        if (display == null || display.getUseSoundId() == null) {
            return null;
        }
        ResourceLocation fallback = display.getUseSoundId();
        if (!FoodAssetsManager.get().hasSoundEvent(fallback)) {
            return null;
        }
        if (soundFallbackLogBudget > 0) {
            soundFallbackLogBudget--;
            LOGGER.info("[firstpersonfoodeating] Fallback to display use_sound: requested={}, fallback={}",
                    resolvedByEffect, fallback);
        }
        return fallback;
    }

    private static ResourceLocation resolveEffectAliasSoundId(String effectKey, FoodDisplayDefinition display) {
        if (effectKey == null || effectKey.isBlank() || display == null || display.getItemId() == null) {
            return null;
        }
        ResourceLocation itemId = display.getItemId();
        String category = deriveItemCategory(itemId);
        if (category == null) {
            return null;
        }
        List<ResourceLocation> categorySounds = collectCategorySoundIds(itemId.getNamespace(), category);
        if (categorySounds.isEmpty()) {
            return null;
        }
        String effectLower = effectKey.toLowerCase();
        List<ResourceLocation> sequence = selectUseSoundSequence(categorySounds);
        if ("zhenji".equals(category) && !sequence.isEmpty()) {
            if (effectLower.contains("draw")) {
                return sequence.get(0);
            }
            if (effectLower.contains("kolpachok")) {
                return sequence.get(Math.min(1, sequence.size() - 1));
            }
            if (effectLower.contains("inject")) {
                return sequence.get(Math.min(2, sequence.size() - 1));
            }
            if (effectLower.contains("put") || effectLower.contains("away")) {
                return sequence.get(sequence.size() - 1);
            }
        }
        if (effectLower.contains("inspect") || effectLower.contains("inpect")) {
            ResourceLocation inspect = pickSingleSound(categorySounds, id -> isInspectSound(id.getPath()));
            if (inspect != null) {
                return inspect;
            }
        }
        if (effectLower.contains("put") || effectLower.contains("away")) {
            ResourceLocation putAway = pickSingleSound(categorySounds, id -> isPutAwaySound(id.getPath()));
            if (putAway != null) {
                return putAway;
            }
        }
        if (effectLower.contains("draw") && !effectLower.contains("drawspoon")) {
            ResourceLocation draw = pickSingleSound(categorySounds,
                    id -> isDrawSound(id.getPath()) && !id.getPath().contains("drawspoon"));
            if (draw != null) {
                return draw;
            }
        }
        if (sequence.isEmpty()) {
            return null;
        }
        int index = parseEffectSequenceIndex(effectLower);
        if (index < 0) {
            if (effectLower.contains("draw")) {
                index = 0;
            } else if (effectLower.contains("open")) {
                index = Math.min(1, sequence.size() - 1);
            } else if (effectLower.contains("drink")
                    || effectLower.contains("foodtake")
                    || effectLower.contains("inject")
                    || effectLower.contains("kolpachok")) {
                index = Math.min(2, sequence.size() - 1);
            } else {
                index = 0;
            }
        }
        return sequence.get(Math.min(Math.max(index, 0), sequence.size() - 1));
    }

    private static void playClientSound(LocalPlayer player, ResourceLocation soundId) {
        if (player == null || soundId == null) {
            return;
        }
        Minecraft.getInstance().getSoundManager().play(new SimpleSoundInstance(
                soundId,
                SoundSource.PLAYERS,
                1.0f,
                1.0f,
                RandomSource.create(),
                false,
                0,
                SoundInstance.Attenuation.LINEAR,
                player.getX(),
                player.getY(),
                player.getZ(),
                false
        ));
    }

    private record TimedSound(float timeSeconds, ResourceLocation soundId) {
    }

    public record HandPose(
            float x,
            float y,
            float z,
            float pitch,
            float yaw,
            float roll,
            float scaleX,
            float scaleY,
            float scaleZ
    ) {
        public static final HandPose IDENTITY = new HandPose(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 1.0f);
    }
}
