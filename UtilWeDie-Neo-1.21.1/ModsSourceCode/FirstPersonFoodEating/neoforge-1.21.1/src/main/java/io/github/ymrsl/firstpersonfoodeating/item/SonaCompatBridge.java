package io.github.ymrsl.firstpersonfoodeating.item;

import com.scarasol.sona.accessor.mixin.ILivingEntityAccessor;
import com.scarasol.sona.manager.InfectionManager;
import com.scarasol.sona.manager.InjuryManager;
import net.minecraft.world.entity.player.Player;
import net.neoforged.fml.ModList;

/**
 * 与 Sona(modid {@code sona}, package {@code com.scarasol.sona}) 的可选兼容桥，仿 {@link ThirstCompatBridge}。
 *
 * <p>负责医疗包 ↔ Sona「血滴血条(受伤值)」的联动：读取已损血条、按量补充。Sona 的受伤值语义为
 * 100=满(健康)、0=空(濒死)，血滴血条填充程度即该值，故「已损血条 = 100 - 受伤值」。
 *
 * <p>玩家实体在运行时已被 Sona 的 LivingEntityMixin 混入 {@link ILivingEntityAccessor}。所有逻辑都
 * 先经 {@link ModList#isLoaded(String)} 门控并 try/catch 包裹：Sona 缺席时整体退化为安全 no-op，
 * 且因提前返回，HotSpot 惰性链接不会去加载 Sona 的类，FPE 仍可在无 Sona 时独立运行。
 * 受伤值为服务端权威，仅在逻辑服务端写入。
 */
public final class SonaCompatBridge {
    private static final boolean SONA_PRESENT = ModList.get().isLoaded("sona");

    private SonaCompatBridge() {
    }

    public static boolean isAvailable() {
        return SONA_PRESENT;
    }

    /** 已损血滴血条 = 100 - 受伤值(向下取整)。Sona 缺席返回 0。 */
    public static int getMissingInjury(Player player) {
        if (!SONA_PRESENT || player == null) {
            return 0;
        }
        try {
            if (player instanceof ILivingEntityAccessor accessor) {
                float injury = InjuryManager.getInjury(accessor);
                return Math.max(0, (int) Math.floor(100.0f - injury));
            }
        } catch (Throwable ignored) {
            // 可选集成失败绝不能影响使用物品。
        }
        return 0;
    }

    /**
     * 以最多 {@code maxAmount} 补充血滴血条(提升受伤值)，返回实际补充量(供调用方据此扣除等量耐久)。
     * 仅服务端生效。
     */
    public static int refillInjury(Player player, int maxAmount) {
        if (!SONA_PRESENT || player == null || maxAmount <= 0) {
            return 0;
        }
        if (player.level().isClientSide()) {
            return 0;
        }
        try {
            if (player instanceof ILivingEntityAccessor accessor) {
                float before = InjuryManager.getInjury(accessor);
                int missing = Math.max(0, (int) Math.floor(100.0f - before));
                int refill = Math.min(missing, maxAmount);
                if (refill <= 0) {
                    return 0;
                }
                InjuryManager.addInjury(accessor, refill);
                float after = InjuryManager.getInjury(accessor);
                return Math.max(0, Math.round(after - before));
            }
        } catch (Throwable ignored) {
        }
        return 0;
    }

    /** 改变感染值(负数=抗感染/治疗)。仅服务端生效。 */
    public static void addInfection(Player player, double amount) {
        if (!SONA_PRESENT || player == null || amount == 0) {
            return;
        }
        if (player.level().isClientSide()) {
            return;
        }
        try {
            if (player instanceof ILivingEntityAccessor accessor) {
                InfectionManager.addInfection(accessor, (float) amount);
            }
        } catch (Throwable ignored) {
        }
    }

    /**
     * 止血：补充血滴血条(受伤值)与绷带值。Sona 的 addBandage 在加绷带时会顺带清除流血(LACERATION)。
     * 仅服务端生效。
     */
    public static void addInjuryAndBandage(Player player, float injury, float bandage) {
        if (!SONA_PRESENT || player == null) {
            return;
        }
        if (player.level().isClientSide()) {
            return;
        }
        try {
            if (player instanceof ILivingEntityAccessor accessor) {
                if (injury != 0) {
                    InjuryManager.addInjury(accessor, injury);
                }
                if (bandage != 0) {
                    InjuryManager.addBandage(accessor, bandage);
                }
            }
        } catch (Throwable ignored) {
        }
    }
}
