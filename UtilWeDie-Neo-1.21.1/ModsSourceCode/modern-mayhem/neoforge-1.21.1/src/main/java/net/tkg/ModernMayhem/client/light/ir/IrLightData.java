package net.tkg.ModernMayhem.client.light.ir;

import net.minecraft.world.phys.Vec3;

/**
 * 一条 IR 锥光的快照 (每客户端 tick 重建)。起点=玩家眼睛, dir=已归一化视线方向,
 * luminance/range 按夜视仪镜头档位(单/双/四)算好后传入。
 */
public final class IrLightData {
    public final double x;
    public final double y;
    public final double z;
    public final Vec3 dir;
    public final double luminance;
    public final double range;

    private IrLightData(double x, double y, double z, Vec3 dir, double luminance, double range) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.dir = dir;
        this.luminance = luminance;
        this.range = range;
    }

    public static IrLightData directional(Vec3 source, Vec3 dir, double luminance, double range) {
        Vec3 d = (dir == null || dir.lengthSqr() < 1.0e-6) ? new Vec3(0.0, 0.0, 1.0) : dir.normalize();
        return new IrLightData(source.x, source.y, source.z, d, luminance, range);
    }
}
