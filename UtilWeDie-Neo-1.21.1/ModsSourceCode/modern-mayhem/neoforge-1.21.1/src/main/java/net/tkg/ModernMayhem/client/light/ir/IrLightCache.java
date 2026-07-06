package net.tkg.ModernMayhem.client.light.ir;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.tkg.ModernMayhem.client.config.ClientConfig;
import net.tkg.ModernMayhem.server.item.NVGGoggleList;
import net.tkg.ModernMayhem.server.item.curios.facewear.NVGGogglesItem;
import net.tkg.ModernMayhem.server.item.generic.GenericSpecialGogglesItem;
import net.tkg.ModernMayhem.server.util.CuriosUtil;
import toni.sodiumdynamiclights.DynamicLightSource;

/**
 * 每客户端 tick 重建"当前可见的 IR 锥光源"列表, 供 SDDL mixin 逐方块查询。
 *
 * 拟真可见性: 红外裸眼不可见 —— 只有当【本地观察者】戴着开机夜视仪时(viewerCanSeeIr), 才登记任何 IR 光,
 * 否则一律不登记 → 裸眼全黑。SDDL 逐客户端算光, 每个客户端按自己的本地观察者独立判定。
 *
 * 分档(以镜头数): 单镜头 range/lum 最小, 双镜头中, 四镜头最大(且需主动开启)。范围收在 SDDL 迭代上限内。
 *
 * viewerCanSeeIr 同时被 IR 测试实体(末影螨)/方块的 handler 复用。
 */
@EventBusSubscriber(modid = "mm", value = Dist.CLIENT)
public final class IrLightCache {
    private static volatile boolean viewerCanSeeIr = false;
    private static volatile Set<DynamicLightSource> selfList = new HashSet<>();
    private static volatile Map<DynamicLightSource, IrLightData> data = new HashMap<>();

    private IrLightCache() {
    }

    public static boolean viewerCanSeeIr() {
        return viewerCanSeeIr;
    }

    /** 需被强制登记为 SDDL 光源(luminance>0)的玩家集合, 供 SodiumDynamicLightsMixin 强制其被追踪/迭代。 */
    public static Set<DynamicLightSource> getSelfLightSourceList() {
        return selfList;
    }

    public static IrLightData getData(DynamicLightSource source) {
        return data.get(source);
    }

    public static Map<DynamicLightSource, IrLightData> getDataMap() {
        return data;
    }

    public static double innerAngle() {
        return ClientConfig.IR_INNER_ANGLE.get();
    }

    public static double outerAngle() {
        return ClientConfig.IR_OUTER_ANGLE.get();
    }

    public static boolean hasNvgPoweredOn(Player player) {
        ItemStack fw = CuriosUtil.getFaceWearItem(player);
        return fw != null && fw.getItem() instanceof NVGGogglesItem && GenericSpecialGogglesItem.getNVGCheck(fw);
    }

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) {
            clear();
            return;
        }

        boolean canSee = hasNvgPoweredOn(mc.player);
        viewerCanSeeIr = canSee;
        if (!canSee) {
            if (!data.isEmpty() || !selfList.isEmpty()) {
                data = new HashMap<>();
                selfList = new HashSet<>();
            }
            return;
        }

        Map<DynamicLightSource, IrLightData> dataMap = new HashMap<>();
        Set<DynamicLightSource> self = new HashSet<>();

        for (Player player : mc.level.players()) {
            ItemStack fw = CuriosUtil.getFaceWearItem(player);
            if (fw == null || !(fw.getItem() instanceof NVGGogglesItem nvg)) {
                continue;
            }
            if (!GenericSpecialGogglesItem.getNVGCheck(fw)) {
                continue;
            }
            NVGGoggleList.IrTier tier = nvg.getIrTier();
            if (tier == NVGGoggleList.IrTier.NONE) {
                continue;
            }
            // 单/双镜头 IR 常开; 四镜头需主动开启
            boolean emit = (tier != NVGGoggleList.IrTier.QUAD) || GenericSpecialGogglesItem.isIrActive(fw);
            if (!emit) {
                continue;
            }
            double range = ClientConfig.irRangeFor(tier);
            double lum = ClientConfig.irLumFor(tier);
            Vec3 eye = player.getEyePosition(1.0f);
            Vec3 dir = IrLightMath.computeDirection(player.getYRot(), player.getXRot());
            DynamicLightSource src = (DynamicLightSource) player;
            dataMap.put(src, IrLightData.directional(eye, dir, lum, range));
            self.add(src);
        }

        data = dataMap;
        selfList = self;
    }

    private static void clear() {
        viewerCanSeeIr = false;
        if (!data.isEmpty() || !selfList.isEmpty()) {
            data = new HashMap<>();
            selfList = new HashSet<>();
        }
    }
}
