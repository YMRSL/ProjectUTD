package net.tkg.ModernMayhem.client.light.ir;

import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

/**
 * 夜视仪 IR 红外照明锥光数学。算"某方块相对一条(起点+视线方向)射线的锥形衰减亮度(0~15)"。
 * 移植自 HandheldMoon 手电筒的 LineLightMath (SDDL 验证过的成熟实现)。
 */
public final class IrLightMath {
    private IrLightMath() {
    }

    /** yaw/pitch(度) → 玩家第一人称视线单位向量。 */
    public static Vec3 computeDirection(float yawDeg, float pitchDeg) {
        double yaw = yawDeg * Mth.DEG_TO_RAD;
        double pitch = pitchDeg * Mth.DEG_TO_RAD;
        return new Vec3(
                -Math.sin(yaw) * Math.cos(pitch),
                -Math.sin(pitch),
                Math.cos(yaw) * Math.cos(pitch));
    }

    /** 锥光亮度(无遮挡)。dx/dy/dz 已归一化视线方向; innerAngleRad/outerAngleRad 锥内/外半角(弧度)。 */
    public static double computeLight(double sx, double sy, double sz,
                                      double dx, double dy, double dz,
                                      double luminance,
                                      BlockPos query,
                                      double range,
                                      double innerAngleRad,
                                      double outerAngleRad) {
        double cx = query.getX() + 0.5;
        double cy = query.getY() + 0.5;
        double cz = query.getZ() + 0.5;

        double vx = cx - sx;
        double vy = cy - sy;
        double vz = cz - sz;

        double dist2 = vx * vx + vy * vy + vz * vz;
        double range2 = range * range;
        if (dist2 > range2) return 0.0;

        double dot = dx * vx + dy * vy + dz * vz;
        if (dot <= 0.0) return 0.0;

        double cosInner = Math.cos(innerAngleRad);
        double cosOuter = Math.cos(outerAngleRad);
        double cosOuterSq = cosOuter * cosOuter;
        if (dot * dot < cosOuterSq * dist2) return 0.0;

        double invDistF = Mth.fastInvSqrt((float) dist2);
        double dotNorm = dot * invDistF;

        double angleAtt = dotNorm >= cosInner ? 1.0 : (dotNorm - cosOuter) / (cosInner - cosOuter);
        double distMul = 1.0 - (dist2 / range2);

        double res = luminance * angleAtt * distMul;
        return Math.max(res, 0.0);
    }

    /** 锥光亮度(带方块遮挡: 光源到方块中心射线检测, 被挡则 0)。 */
    public static double computeLightOccluded(Level level,
                                              double sx, double sy, double sz,
                                              double dx, double dy, double dz,
                                              double luminance,
                                              BlockPos query,
                                              double range,
                                              double innerAngleRad,
                                              double outerAngleRad) {
        double res = computeLight(sx, sy, sz, dx, dy, dz, luminance, query, range, innerAngleRad, outerAngleRad);
        if (res <= 0.0) return 0.0;
        if (level == null) return res;

        Vec3 start = new Vec3(sx, sy, sz);
        Vec3 end = new Vec3(query.getX() + 0.5, query.getY() + 0.5, query.getZ() + 0.5);
        try {
            HitResult hit = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, CollisionContext.empty()));
            if (hit.getType() == HitResult.Type.BLOCK && !((BlockHitResult) hit).getBlockPos().equals(query)) {
                return 0.0;
            }
        } catch (Exception e) {
            return res;
        }
        return res;
    }
}
