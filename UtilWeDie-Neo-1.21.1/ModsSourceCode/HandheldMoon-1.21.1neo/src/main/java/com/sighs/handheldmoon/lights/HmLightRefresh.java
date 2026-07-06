package com.sighs.handheldmoon.lights;

import com.sighs.handheldmoon.HandheldMoon;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import toni.sodiumdynamiclights.DynamicLightSource;
import toni.sodiumdynamiclights.SodiumDynamicLights;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Schedules SodiumDynamicLights chunk rebuilds when a HandheldMoon directional/omni source moves,
 * rotates, or disappears. Without this, SDDL would only re-light chunks when its own tracked
 * luminance/position changes; since our cone depends on look direction, we must nudge the
 * surrounding chunks to re-query {@code maxDynamicLightLevel}.
 */
@EventBusSubscriber(modid = HandheldMoon.MOD_ID, value = Dist.CLIENT)
public final class HmLightRefresh {
    private static final double LOOK_DOT_THRESHOLD = Math.cos(Math.toRadians(2.0));
    private static final double POS_EPS_SQ = 0.01;
    private static final int MIN_UPDATE_TICKS = 2;

    private static Map<DynamicLightSource, LightState> lastState = new HashMap<>();

    private HmLightRefresh() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            lastState = new HashMap<>();
            return;
        }

        Map<DynamicLightSource, HmLightCache.RealLightData> dataMap = HmLightCache.getRealLightDataMap();
        if (dataMap.isEmpty() && lastState.isEmpty()) return;

        long gameTime = mc.level.getGameTime();
        Map<DynamicLightSource, LightState> current = new HashMap<>();
        Set<DynamicLightSource> toRefresh = new HashSet<>();

        for (Map.Entry<DynamicLightSource, HmLightCache.RealLightData> entry : dataMap.entrySet()) {
            DynamicLightSource source = entry.getKey();
            LightState observed = LightState.from(entry.getValue());
            LightState prev = lastState.get(source);
            if (prev == null) {
                observed.lastUpdateTick = gameTime;
                toRefresh.add(source);
                current.put(source, observed);
                continue;
            }
            boolean changed = !observed.isSimilar(prev);
            boolean ready = (gameTime - prev.lastUpdateTick) >= MIN_UPDATE_TICKS;
            if (changed && ready) {
                observed.lastUpdateTick = gameTime;
                toRefresh.add(source);
                current.put(source, observed);
            } else {
                current.put(source, prev);
            }
        }

        for (DynamicLightSource source : lastState.keySet()) {
            if (!current.containsKey(source)) {
                toRefresh.add(source);
            }
        }

        if (!toRefresh.isEmpty()) {
            Map<DynamicLightSource, LightState> lookup = new HashMap<>(lastState);
            lookup.putAll(current);
            refresh(toRefresh, lookup);
        }

        lastState = current;
    }

    private static void refresh(Set<DynamicLightSource> sources, Map<DynamicLightSource, LightState> lookup) {
        Set<BlockPos> sections = new HashSet<>();
        for (DynamicLightSource source : sources) {
            LightState state = lookup.get(source);
            if (state == null) continue;
            int chunkRadius = Math.max(1, (int) Math.ceil(state.range / 16.0));
            for (int dx = -chunkRadius; dx <= chunkRadius; dx++) {
                for (int dz = -chunkRadius; dz <= chunkRadius; dz++) {
                    sections.add(new BlockPos(state.chunkX + dx, state.sectionY, state.chunkZ + dz));
                }
            }
            source.sdl$resetDynamicLight();
        }
        var levelRenderer = Minecraft.getInstance().levelRenderer;
        for (BlockPos section : sections) {
            SodiumDynamicLights.scheduleChunkRebuild(levelRenderer, section);
        }
    }

    private static final class LightState {
        final int chunkX;
        final int chunkZ;
        final int sectionY;
        final double x, y, z;
        final Vec3 dir;
        final boolean directional;
        final double range;
        long lastUpdateTick;

        private LightState(int chunkX, int chunkZ, int sectionY, double x, double y, double z, Vec3 dir, boolean directional, double range) {
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
            this.sectionY = sectionY;
            this.x = x;
            this.y = y;
            this.z = z;
            this.dir = dir;
            this.directional = directional;
            this.range = range;
        }

        static LightState from(HmLightCache.RealLightData data) {
            int chunkX = SectionPos.blockToSectionCoord(Math.floor(data.x));
            int chunkZ = SectionPos.blockToSectionCoord(Math.floor(data.z));
            int sectionY = SectionPos.blockToSectionCoord(Math.floor(data.y));
            return new LightState(chunkX, chunkZ, sectionY, data.x, data.y, data.z, data.dir, data.directional, data.range);
        }

        boolean isSimilar(LightState other) {
            if (other == null) return false;
            if (this.chunkX != other.chunkX || this.chunkZ != other.chunkZ || this.sectionY != other.sectionY) return false;
            if (this.directional != other.directional) return false;
            if (Math.abs(this.range - other.range) > 0.0001) return false;
            double dx = this.x - other.x;
            double dy = this.y - other.y;
            double dz = this.z - other.z;
            if ((dx * dx + dy * dy + dz * dz) > POS_EPS_SQ) return false;
            if (this.directional) {
                double dot = this.dir.dot(other.dir);
                return dot >= LOOK_DOT_THRESHOLD;
            }
            return true;
        }
    }
}
