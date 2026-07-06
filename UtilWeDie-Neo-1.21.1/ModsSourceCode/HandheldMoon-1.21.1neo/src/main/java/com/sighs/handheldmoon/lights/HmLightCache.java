package com.sighs.handheldmoon.lights;

import com.sighs.handheldmoon.HandheldMoon;
import com.sighs.handheldmoon.entity.FullMoonEntity;
import com.sighs.handheldmoon.registry.Config;
import com.sighs.handheldmoon.util.LineLightMath;
import com.sighs.handheldmoon.util.Utils;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import toni.sodiumdynamiclights.DynamicLightSource;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Client-tick rebuilt registry of HandheldMoon's active dynamic light sources, driving the
 * SodiumDynamicLights mixins:
 * <ul>
 *   <li>{@link #selfLightSourceList} — sources that should glow at luminance 15 on themselves
 *       (consumed by {@code SodiumDynamicLightsMixin} and {@code HmLightHandler#selfLight}).</li>
 *   <li>{@link #realLightData} — per-source directional/omni cone data
 *       (consumed by {@code DirectionalLightMixin} -> {@code HmLightHandler#entityLight}).</li>
 * </ul>
 *
 * <p>Sources are SDDL's own {@link DynamicLightSource} casts of vanilla entities (SDDL's
 * EntityMixin / PlayerEntityMixin already implement the interface), so we never call
 * {@code addLightSource} — we only override the per-cell luminance SDDL computes for them.
 */
@EventBusSubscriber(modid = HandheldMoon.MOD_ID, value = Dist.CLIENT)
public final class HmLightCache {
    private static final double PLAYER_RANGE = 32.0;
    private static final double LAMP_RANGE = 32.0;
    private static final double MOON_RANGE = 18.0;
    private static final double INNER = 0.5;
    private static final double OUTER = 0.7;

    private static volatile Set<DynamicLightSource> selfLightSourceList = new HashSet<>();
    private static volatile Map<DynamicLightSource, RealLightData> realLightData = new HashMap<>();

    private HmLightCache() {
    }

    public static Set<DynamicLightSource> getSelfLightSourceList() {
        return selfLightSourceList;
    }

    public static RealLightData getRealLightData(DynamicLightSource lightSource) {
        return realLightData.get(lightSource);
    }

    public static Map<DynamicLightSource, RealLightData> getRealLightDataMap() {
        return realLightData;
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            if (!selfLightSourceList.isEmpty() || !realLightData.isEmpty()) {
                selfLightSourceList = new HashSet<>();
                realLightData = new HashMap<>();
            }
            return;
        }

        boolean realLight = Config.REAL_LIGHT.get();
        double luminance = Config.REAL_LIGHT_LUMINANCE.get();

        Set<DynamicLightSource> selfList = new HashSet<>();
        Map<DynamicLightSource, RealLightData> dataMap = new HashMap<>();

        // Players holding / wearing a powered flashlight: directional cone + self light.
        for (Player player : mc.level.players()) {
            DynamicLightSource source = (DynamicLightSource) player;
            if (Utils.isUsingFlashlight(player)) {
                selfList.add(source);
                if (realLight) {
                    Vec3 eye = player.getEyePosition(1.0f);
                    Vec3 dir = LineLightMath.computeDirection(player.getYRot(), player.getXRot(), false);
                    dataMap.put(source, RealLightData.directional(eye, dir, luminance, PLAYER_RANGE));
                }
            }
        }

        // FullMoonEntity: lamp-bound -> directional flashlight; mini-moon -> omni point light.
        for (var entity : mc.level.entitiesForRendering()) {
            if (!(entity instanceof FullMoonEntity moon)) continue;
            DynamicLightSource source = (DynamicLightSource) moon;
            selfList.add(source);
            if (!realLight) continue;

            Vec3 pos = moon.position();
            if (moon.isLampBound()) {
                int lampLum = moon.getLampLuminance();
                if (lampLum <= 0) continue;
                float yaw = moon.getLampYRot();
                float pitch = moon.getLampXRot() - 90.0f;
                Vec3 dir = LineLightMath.computeDirection(yaw, pitch, true).reverse();
                dataMap.put(source, RealLightData.directional(pos, dir, lampLum, LAMP_RANGE));
            } else {
                dataMap.put(source, RealLightData.omni(pos, 15.0, MOON_RANGE));
            }
        }

        selfLightSourceList = selfList;
        realLightData = dataMap;
    }

    /**
     * Snapshot of one source's light geometry. The source point is captured here (rather than read
     * from {@code DynamicLightSource.sdl$getDynamicLightX()} which lags by a tick) so the cone is
     * stable and matches the rendered position.
     */
    public static final class RealLightData {
        public final double x, y, z;
        public final Vec3 dir;
        public final boolean directional;
        public final boolean omniMoon;
        public final double luminance;
        public final double range;

        private RealLightData(double x, double y, double z, Vec3 dir, boolean directional, boolean omniMoon, double luminance, double range) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.dir = dir;
            this.directional = directional;
            this.omniMoon = omniMoon;
            this.luminance = luminance;
            this.range = range;
        }

        public static RealLightData directional(Vec3 source, Vec3 dir, double luminance, double range) {
            if (dir == null || dir.lengthSqr() < 1.0e-6) {
                return new RealLightData(source.x, source.y, source.z, Vec3.ZERO, false, false, luminance, range);
            }
            return new RealLightData(source.x, source.y, source.z, dir.normalize(), true, false, luminance, range);
        }

        public static RealLightData omni(Vec3 source, double luminance, double range) {
            // Omni "mini moon": centered on the block (+0.5 offsets handled in handler).
            return new RealLightData(source.x, source.y, source.z, Vec3.ZERO, false, true, luminance, range);
        }
    }

    public static double innerAngle() {
        return INNER;
    }

    public static double outerAngle() {
        return OUTER;
    }
}
