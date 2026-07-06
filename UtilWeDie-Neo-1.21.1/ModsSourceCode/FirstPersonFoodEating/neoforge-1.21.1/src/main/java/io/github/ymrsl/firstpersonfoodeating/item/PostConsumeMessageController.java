package io.github.ymrsl.firstpersonfoodeating.item;

import io.github.ymrsl.firstpersonfoodeating.FirstPersonFoodEatingMod;
import io.github.ymrsl.firstpersonfoodeating.config.ModCommonConfig;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import org.jetbrains.annotations.Nullable;

@EventBusSubscriber(modid = FirstPersonFoodEatingMod.MOD_ID, bus = EventBusSubscriber.Bus.GAME)
public final class PostConsumeMessageController {
    private static final Map<UUID, ActiveMessageSequence> ACTIVE_SEQUENCES = new HashMap<>();
    private static final Map<UUID, Integer> LAST_TRIGGER_TICK = new HashMap<>();

    private PostConsumeMessageController() {
    }

    public static void scheduleFromConsumedStack(Player player, ItemStack consumedStack) {
        scheduleFromStack(player, consumedStack, false);
    }

    public static void scheduleFromUseStart(Player player, ItemStack stackInUse) {
        scheduleFromStack(player, stackInUse, true);
    }

    private static void scheduleFromStack(Player player, ItemStack sourceStack, boolean forceRestart) {
        if (!isFlavorMessagesEnabled()) {
            return;
        }
        if (!(player instanceof ServerPlayer serverPlayer) || sourceStack.isEmpty()) {
            return;
        }
        FoodStackData.FlavorMessagePlan plan = FoodStackData
                .selectFlavorMessagePlan(sourceStack, serverPlayer.getRandom())
                .orElse(null);
        if (plan == null || plan.segments().isEmpty()) {
            return;
        }

        UUID uuid = serverPlayer.getUUID();
        int nowTick = serverPlayer.tickCount;
        if (!forceRestart) {
            int cooldownTicks = Math.max(plan.cooldownTicks(), 0);
            Integer lastTrigger = LAST_TRIGGER_TICK.get(uuid);
            if (lastTrigger != null && cooldownTicks > 0 && nowTick - lastTrigger < cooldownTicks) {
                return;
            }
        }

        ACTIVE_SEQUENCES.put(
                uuid,
                new ActiveMessageSequence(nowTick, plan.mode(), plan.segments())
        );
        LAST_TRIGGER_TICK.put(uuid, nowTick);
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) {
            return;
        }
        if (!isFlavorMessagesEnabled()) {
            ACTIVE_SEQUENCES.remove(serverPlayer.getUUID());
            return;
        }
        ActiveMessageSequence sequence = ACTIVE_SEQUENCES.get(serverPlayer.getUUID());
        if (sequence == null) {
            return;
        }
        int elapsedTicks = Math.max(serverPlayer.tickCount - sequence.startTick, 0);

        while (sequence.nextIndex < sequence.segments.size()) {
            FoodStackData.FlavorSegment segment = sequence.segments.get(sequence.nextIndex);
            if (elapsedTicks < segment.atTicks()) {
                break;
            }
            Component message = buildMessage(segment);
            if (message != null) {
                boolean actionBar = sequence.mode == FoodStackData.MessageMode.ACTIONBAR;
                serverPlayer.displayClientMessage(message, actionBar);
            }
            sequence.nextIndex++;
        }

        if (sequence.nextIndex >= sequence.segments.size()) {
            ACTIVE_SEQUENCES.remove(serverPlayer.getUUID());
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        Player player = event.getEntity();
        if (player == null) {
            return;
        }
        UUID uuid = player.getUUID();
        ACTIVE_SEQUENCES.remove(uuid);
        LAST_TRIGGER_TICK.remove(uuid);
    }

    private static @Nullable Component buildMessage(FoodStackData.FlavorSegment segment) {
        String text = segment.text();
        String langKey = segment.langKey();
        boolean hasText = text != null && !text.isBlank();
        boolean hasLangKey = langKey != null && !langKey.isBlank();
        if (!hasText && !hasLangKey) {
            return null;
        }
        if (hasLangKey) {
            return Component.translatableWithFallback(langKey, hasText ? text : langKey);
        }
        return Component.literal(text);
    }

    private static boolean isFlavorMessagesEnabled() {
        return ModCommonConfig.ENABLE_FLAVOR_MESSAGES.get();
    }

    private static final class ActiveMessageSequence {
        private final int startTick;
        private final FoodStackData.MessageMode mode;
        private final List<FoodStackData.FlavorSegment> segments;
        private int nextIndex;

        private ActiveMessageSequence(int startTick, FoodStackData.MessageMode mode, List<FoodStackData.FlavorSegment> segments) {
            this.startTick = startTick;
            this.mode = mode == null ? FoodStackData.MessageMode.ACTIONBAR : mode;
            this.segments = segments == null ? List.of() : List.copyOf(segments);
            this.nextIndex = 0;
        }
    }
}
