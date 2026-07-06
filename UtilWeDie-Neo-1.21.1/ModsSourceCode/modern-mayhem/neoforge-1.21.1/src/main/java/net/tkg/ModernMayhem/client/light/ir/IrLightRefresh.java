package net.tkg.ModernMayhem.client.light.ir;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
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

/**
 * IR 锥光的区块重光调度。锥光依赖玩家朝向, 而 SDDL 自己不会因为"只转头不移动"就重算光照,
 * 所以这里检测每个 IR 源的位置/朝向/范围变化(或出现/消失), 变了就调 SDDL 重建周边 section。
 *
 * 性能要点: 重光范围用 2D(水平 dx/dz) 而非 3D —— (2r+1)^2 个 section 而非 (2r+1)^3,
 * range≤15 → r=1 → 9 个 section/次(对比旧版 3D 的 125 个, 轻一个量级)。镜像 HandheldMoon。
 */
@EventBusSubscriber(modid = "mm", value = Dist.CLIENT)
public final class IrLightRefresh {
    private static final double LOOK_DOT_THRESHOLD = Math.cos(Math.toRadians(2.0));
    private static final double POS_EPS_SQ = 0.01;
    private static final int MIN_UPDATE_TICKS = 2;

    private static Map<DynamicLightSource, LightState> lastState = new HashMap<>();

    private IrLightRefresh() {
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            lastState = new HashMap<>();
            return;
        }

        Map<DynamicLightSource, IrLightData> dataMap = IrLightCache.getDataMap();
        if (dataMap.isEmpty() && lastState.isEmpty()) {
            return;
        }

        long gameTime = mc.level.getGameTime();
        Map<DynamicLightSource, LightState> current = new HashMap<>();
        Set<DynamicLightSource> toRefresh = new HashSet<>();

        for (Map.Entry<DynamicLightSource, IrLightData> entry : dataMap.entrySet()) {
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

        // 消失的源(含本地观察者摘下夜视仪导致全部清空)也要重光一次清掉残留
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
            if (state == null) {
                continue;
            }
            int chunkRadius = Math.max(1, (int) Math.ceil(state.range / 16.0));
            // 2D 水平重光 (锥光主要在水平面铺开; 垂直只覆盖光源所在 section ±1)
            for (int dx = -chunkRadius; dx <= chunkRadius; ++dx) {
                for (int dz = -chunkRadius; dz <= chunkRadius; ++dz) {
                    sections.add(new BlockPos(state.sectionX + dx, state.sectionY, state.sectionZ + dz));
                    sections.add(new BlockPos(state.sectionX + dx, state.sectionY - 1, state.sectionZ + dz));
                    sections.add(new BlockPos(state.sectionX + dx, state.sectionY + 1, state.sectionZ + dz));
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
        final int sectionX;
        final int sectionY;
        final int sectionZ;
        final double x;
        final double y;
        final double z;
        final Vec3 dir;
        final double range;
        long lastUpdateTick;

        LightState(int sectionX, int sectionY, int sectionZ, double x, double y, double z, Vec3 dir, double range) {
            this.sectionX = sectionX;
            this.sectionY = sectionY;
            this.sectionZ = sectionZ;
            this.x = x;
            this.y = y;
            this.z = z;
            this.dir = dir;
            this.range = range;
        }

        static LightState from(IrLightData data) {
            int sx = SectionPos.blockToSectionCoord(Math.floor(data.x));
            int sy = SectionPos.blockToSectionCoord(Math.floor(data.y));
            int sz = SectionPos.blockToSectionCoord(Math.floor(data.z));
            return new LightState(sx, sy, sz, data.x, data.y, data.z, data.dir, data.range);
        }

        boolean isSimilar(LightState other) {
            if (other == null) {
                return false;
            }
            if (this.sectionX != other.sectionX || this.sectionY != other.sectionY || this.sectionZ != other.sectionZ) {
                return false;
            }
            if (Math.abs(this.range - other.range) > 0.0001) {
                return false;
            }
            double dx = this.x - other.x;
            double dy = this.y - other.y;
            double dz = this.z - other.z;
            if ((dx * dx + dy * dy + dz * dz) > POS_EPS_SQ) {
                return false;
            }
            double dot = this.dir.dot(other.dir);
            return dot >= LOOK_DOT_THRESHOLD;
        }
    }
}
