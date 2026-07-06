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
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Client-side handler for the sable × Flashback bridge carriers.
 *
 * <h2>Debug scaffolding (always-on, grep [SABLE-REPLAY])</h2>
 * <ul>
 *   <li>{@code [SABLE-REPLAY] HANDLER} – NeoForge registered handler fired;
 *       prints thread name, level/connection availability, and immediate-or-queue decision.</li>
 *   <li>{@code [SABLE-REPLAY] DISPATCH} – each inner packet decoded and handled;
 *       prints packet class and sequence number.</li>
 *   <li>{@code [SABLE-REPLAY] FAIL} – decode or handle exception (full stack).</li>
 *   <li>{@code [SABLE-REPLAY] SEEK} – FlashbackClearEntities arrived (seek start).</li>
 *   <li>{@code [SABLE-REPLAY] FLUSH} – tick-flusher drained the queue.</li>
 * </ul>
 */
public final class SableReplayClientHandler {
    private static final Logger LOGGER = LogManager.getLogger("utd_doomsday_patch");
    private static final int MAX_QUEUE = 4096;

    private static final Queue<byte[]> QUEUE = new ArrayDeque<>();

    // ── counters ──────────────────────────────────────────────────────────────
    private static long handlerCalls;      // times NeoForge handler lambda fired
    private static long immediateCount;    // dispatched in-handler (immediate path)
    private static long queuedCount;       // queued because level/conn was null
    private static long flushCount;        // dispatched by tick-flusher
    private static long failCount;         // decode/handle exceptions (any path)
    private static long seekCount;         // FlashbackClearEntities received

    private SableReplayClientHandler() {}

    // ─────────────────────────────────────────────────────────────────────────
    // Entry point called by UtdDoomsdayPatch's RegisterPayloadHandlersEvent
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Called from the NeoForge {@code playToClient} handler lambda.
     * Tries to dispatch the carrier immediately (same call frame = same tick,
     * correct ordering relative to Flashback's other inline custom-payload
     * dispatches). Falls back to the tick-flusher queue only when the client
     * world is not yet ready.
     */
    public static void handleFromNeoForge(SableReplayPayload payload,
                                          IPayloadContext context) {
        handlerCalls++;
        Minecraft mc = Minecraft.getInstance();
        ClientPacketListener conn = mc.getConnection();
        boolean hasLevel = mc.level != null;
        boolean hasConn  = conn != null;
        String thread    = Thread.currentThread().getName();

        // ── debug ──
        boolean logThis = handlerCalls <= 10 || handlerCalls % 50 == 0;
        if (logThis) {
            LOGGER.info(
                "[SABLE-REPLAY] HANDLER #{} thread='{}' level={} conn={} bytes={}",
                handlerCalls, thread, hasLevel, hasConn, payload.data().length);
        }

        if (hasLevel && hasConn) {
            // Main path: dispatch immediately so ordering is preserved vs
            // Flashback's other inline custom-payload dispatches in the same frame.
            immediateCount++;
            if (logThis) {
                LOGGER.info("[SABLE-REPLAY] HANDLER #{} → immediate dispatch (immediate#{})",
                        handlerCalls, immediateCount);
            }
            dispatchOne(conn, payload.data(), "immediate");
        } else {
            // Fallback: world not ready yet (initial load).
            queuedCount++;
            if (logThis) {
                LOGGER.info("[SABLE-REPLAY] HANDLER #{} → queued (hasLevel={} hasConn={}), q={}",
                        handlerCalls, hasLevel, hasConn, queuedCount);
            }
            enqueue(payload);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Queue-based fallback (initial load path + public enqueue entry)
    // ─────────────────────────────────────────────────────────────────────────

    public static void enqueue(SableReplayPayload payload) {
        synchronized (QUEUE) {
            if (QUEUE.size() >= MAX_QUEUE) {
                LOGGER.warn("[SABLE-REPLAY] queue overflow ({}), dropping oldest", QUEUE.size());
                QUEUE.poll();
            }
            QUEUE.add(payload.data());
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
                    LOGGER.info("[SABLE-REPLAY] SEEK/LOGOUT clearing {} queued carriers", QUEUE.size());
                    QUEUE.clear();
                }
            }
        }
    }

    private static void flush() {
        Minecraft mc = Minecraft.getInstance();
        ClientPacketListener conn = mc.getConnection();
        if (conn == null || mc.level == null) return;

        int n = 0;
        byte[] data;
        while (true) {
            synchronized (QUEUE) { data = QUEUE.poll(); }
            if (data == null) break;
            dispatchOne(conn, data, "flush");
            n++;
        }
        if (n > 0) {
            flushCount += n;
            LOGGER.info("[SABLE-REPLAY] FLUSH {} carriers this tick (total flush={})", n, flushCount);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Core decode + dispatch
    // ─────────────────────────────────────────────────────────────────────────

    static void dispatchOne(ClientPacketListener conn, byte[] data, String path) {
        try {
            var codec = GameProtocols.CLIENTBOUND_TEMPLATE
                    .bind(RegistryFriendlyByteBuf.decorator(conn.registryAccess()))
                    .codec();
            Packet<? super ClientGamePacketListener> pkt =
                    codec.decode(Unpooled.wrappedBuffer(data));
            if (pkt instanceof net.minecraft.network.protocol.game.ClientboundAddEntityPacket addPkt) {
                net.minecraft.client.Minecraft mc = net.minecraft.client.Minecraft.getInstance();
                if (mc.level != null) {
                    net.minecraft.world.entity.Entity existing = mc.level.getEntity(addPkt.getId());
                    // Log entity state at the time of AddEntityPacket dispatch.
                    LOGGER.info("[SABLE-REPLAY] AddEntity id={} type={}: existing={} removed={}",
                            addPkt.getId(), addPkt.getType(),
                            existing != null ? existing.getClass().getSimpleName() : "null",
                            existing != null ? existing.isRemoved() : "n/a");
                    if (existing != null && !existing.isRemoved()) {
                        // Entity already alive in client world — added either by a prior carrier
                        // (seat at plot-space coords, not cleared by FlashbackClearEntities) or by
                        // Flashback's vanilla entity streaming (vehicle at world-space coords).
                        // Re-sending AddEntityPacket for a live entity causes handleAddEntity to
                        // produce a broken/invisible duplicate.  Skip the add; the SetPassengers
                        // packet that follows will (re-)establish the riding relationship.
                        LOGGER.info("[SABLE-REPLAY] skipping AddEntity id={}: entity already alive ({})",
                                addPkt.getId(), existing.getClass().getSimpleName());
                        return;
                    }
                }
            }
            pkt.handle(conn);
            long total = immediateCount + flushCount;
            // Always log entity-relevant packets so we can trace the add/remove chain.
            boolean isEntityPacket = pkt instanceof net.minecraft.network.protocol.game.ClientboundAddEntityPacket
                    || pkt instanceof net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket
                    || pkt instanceof net.minecraft.network.protocol.game.ClientboundSetPassengersPacket;
            if (isEntityPacket || total <= 15 || total % 100 == 0) {
                String extra = "";
                if (pkt instanceof net.minecraft.network.protocol.game.ClientboundAddEntityPacket ap) {
                    extra = " id=" + ap.getId() + " type=" + ap.getType() + " pos=(" + (int)ap.getX() + "," + (int)ap.getY() + "," + (int)ap.getZ() + ")";
                } else if (pkt instanceof net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket rp) {
                    extra = " ids=" + rp.getEntityIds();
                } else if (pkt instanceof net.minecraft.network.protocol.game.ClientboundSetPassengersPacket pp) {
                    extra = " vehicle=" + pp.getVehicle();
                }
                LOGGER.info("[SABLE-REPLAY] DISPATCH [{}] #{}: {}{}",
                        path, total, pkt.getClass().getSimpleName(), extra);
            }
        } catch (Throwable t) {
            String msg = t.getMessage() != null ? t.getMessage() : t.toString();
            if (msg.contains("Plot already exists")) {
                return;
            }
            failCount++;
            LOGGER.error("[SABLE-REPLAY] FAIL [{}] #{} ({} bytes): {}",
                    path, failCount, data.length, t.toString(), t);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Seek tracking (called from FlashbackClearEntitiesListenerMixin)
    // ─────────────────────────────────────────────────────────────────────────

    public static void onSeekClear() {
        seekCount++;
        LOGGER.info(
            "[SABLE-REPLAY] SEEK #{}: FlashbackClearEntities received — "
            + "handler={} immediate={} queued={} flush={} fail={}",
            seekCount, handlerCalls, immediateCount, queuedCount, flushCount, failCount);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Legacy counters (kept for backwards-compat with mixin that's now removed)
    // ─────────────────────────────────────────────────────────────────────────
    public static void countDirectDispatched() {}
    public static void countDirectFailed()     {}
}
