package com.scarasol.sona.compat;

import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.fml.ModList;
import noppes.npcs.api.NpcAPI;
import noppes.npcs.api.IWorld;
import noppes.npcs.api.entity.IEntity;
import org.slf4j.Logger;

/**
 * 与 Custom NPCs(modid {@code customnpcs}) 的可选兼容桥，仿 FPE 侧的 SonaCompatBridge。
 *
 * <p>用于「玩家感染死亡」时生成一个 CNPC 克隆体（如 Fungal Infected）替代原版僵尸，并改名。
 * 走 CNPC 脚本 API 对应的 Java 接口：{@code NpcAPI.Instance().getIWorld(ServerLevel)
 * .spawnClone(x, y, z, tab, cloneName)} → {@link IEntity}，再 {@link IEntity#setName(String)}。
 *
 * <p>一切先经 {@link ModList#isLoaded(String)} 门控并 try/catch：CNPC 缺席时退化为安全 no-op，
 * 且因提前返回，HotSpot 惰性链接不会加载 noppes 的类，Sona 仍可在无 CNPC 时独立运行。
 */
public final class CnpcCompatBridge {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final boolean CNPC_PRESENT = ModList.get().isLoaded("customnpcs");

    private CnpcCompatBridge() {
    }

    public static boolean isAvailable() {
        return CNPC_PRESENT;
    }

    /**
     * 在指定坐标生成存放于 {@code tab} 的名为 {@code cloneName} 的 CNPC 克隆体，并(若给定)改名为
     * {@code newName}。成功返回 true。CNPC 缺席或失败返回 false（调用方据此回退原版逻辑）。
     */
    public static boolean spawnClone(ServerLevel level, double x, double y, double z,
                                     int tab, String cloneName, String newName) {
        if (!CNPC_PRESENT || level == null || cloneName == null || cloneName.isEmpty()) {
            LOGGER.warn("[sona] spawnClone skipped: cnpcPresent={}, level={}, cloneName={}", CNPC_PRESENT, level, cloneName);
            return false;
        }
        try {
            IWorld world = NpcAPI.Instance().getIWorld(level);
            if (world == null) {
                LOGGER.warn("[sona] spawnClone: getIWorld returned null");
                return false;
            }
            IEntity npc = world.spawnClone(x, y, z, tab, cloneName);
            if (npc == null) {
                LOGGER.warn("[sona] spawnClone: returned null (tab={}, name='{}', pos={},{},{}) — clone not found?", tab, cloneName, x, y, z);
                return false;
            }
            if (newName != null && !newName.isEmpty()) {
                npc.setName(newName);
            }
            LOGGER.info("[sona] spawnClone OK: tab={}, clone='{}' renamed to '{}' at {},{},{}", tab, cloneName, newName, x, y, z);
            return true;
        } catch (Throwable t) {
            // 可选集成失败绝不能影响死亡流程。
            LOGGER.error("[sona] spawnClone threw (tab={}, name='{}')", tab, cloneName, t);
            return false;
        }
    }
}
