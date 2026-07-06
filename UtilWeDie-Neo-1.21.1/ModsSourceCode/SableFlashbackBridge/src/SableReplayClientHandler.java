package com.utdpatch.doomsday.compat;

import io.netty.buffer.Unpooled;
import java.util.ArrayDeque;
import java.util.Queue;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.GameProtocols;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Client side of the sable x Flashback bridge: receives
 * {@link SableReplayPayload} carriers (forwarded raw by Flashback's replay
 * server), decodes the carried clientbound game packet and dispatches it into
 * the local {@link ClientPacketListener} — exactly as if the original server
 * had sent it. Packets are queued until the replay client actually has a
 * level+connection, since snapshot payloads can arrive before the viewer's
 * world exists.
 *
 * Every stage logs under the [SABLE-REPLAY] marker so a single latest.log
 * tells the whole story: carrier received -> queued -> flushed -> dispatched
 * (or failed, with the packet class and error).
 */
public final class SableReplayClientHandler {
    private static final Logger LOGGER = LogManager.getLogger("utd_doomsday_patch");
    private static final int MAX_QUEUE = 4096;

    private static final Queue<byte[]> QUEUE = new ArrayDeque<>();
    private static long received;
    private static long dispatched;
    private static long failed;

    private SableReplayClientHandler() {
    }

    /** Payload entry (guaranteed main thread by NeoForge handler registration). */
    public static void enqueue(SableReplayPayload payload) {
        synchronized (QUEUE) {
            if (QUEUE.size() >= MAX_QUEUE) {
                LOGGER.warn("[SABLE-REPLAY] carrier queue overflow ({}), dropping oldest", QUEUE.size());
                QUEUE.poll();
            }
            QUEUE.add(payload.data());
            received++;
            if (received <= 5 || received % 100 == 0) {
                LOGGER.info("[SABLE-REPLAY] carrier received #{} ({} bytes), queued={}",
                        received, payload.data().length, QUEUE.size());
            }
        }
    }

    public static final class TickFlusher {
        @SubscribeEvent
        public void onClientTick(ClientTickEvent.Post event) {
            flush();
        }

        @SubscribeEvent
        public void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
            synchronized (QUEUE) {
                if (!QUEUE.isEmpty()) {
                    LOGGER.info("[SABLE-REPLAY] clearing {} queued carriers on logout", QUEUE.size());
                    QUEUE.clear();
                }
            }
        }
    }

    private static void flush() {
        Minecraft mc = Minecraft.getInstance();
        ClientPacketListener connection = mc.getConnection();
        if (connection == null || mc.level == null) {
            return;
        }
        byte[] data;
        int flushedThisTick = 0;
        while (true) {
            synchronized (QUEUE) {
                data = QUEUE.poll();
            }
            if (data == null) {
                break;
            }
            dispatchOne(connection, data);
            flushedThisTick++;
        }
        if (flushedThisTick > 0) {
            LOGGER.info("[SABLE-REPLAY] flushed {} carriers (total dispatched={}, failed={})",
                    flushedThisTick, dispatched, failed);
        }
    }

    private static void dispatchOne(ClientPacketListener connection, byte[] data) {
        try {
            var codec = GameProtocols.CLIENTBOUND_TEMPLATE
                    .bind(RegistryFriendlyByteBuf.decorator(connection.registryAccess()))
                    .codec();
            Packet<? super ClientGamePacketListener> packet = codec.decode(Unpooled.wrappedBuffer(data));
            packet.handle(connection);
            dispatched++;
            if (dispatched <= 10 || dispatched % 100 == 0) {
                LOGGER.info("[SABLE-REPLAY] dispatched #{}: {}", dispatched, packet.getClass().getSimpleName());
            }
        } catch (Throwable t) {
            failed++;
            LOGGER.error("[SABLE-REPLAY] failed to decode/dispatch carrier ({} bytes, fail #{})",
                    data.length, failed, t);
        }
    }
}
