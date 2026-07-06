package com.scarasol.zombiekit.compat;

import com.atsuishio.superbwarfare.entity.vehicle.DroneEntity;
import com.atsuishio.superbwarfare.item.misc.MonitorItem;
import com.atsuishio.superbwarfare.tools.EntityFindUtil;
import com.atsuishio.superbwarfare.tools.NBTTool;
import com.scarasol.zombiekit.entity.mechanics.MortarEntity;
import com.scarasol.zombiekit.init.ZombieKitSounds;
import com.scarasol.zombiekit.network.CoverFirePacket;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;

import javax.annotation.Nullable;

/**
 * superbwarfare 0.8.9 integration.
 *
 * 1.21.1 / SBW-0.8.9 notes:
 *  - Item class is {@code com.atsuishio.superbwarfare.item.misc.MonitorItem} (was {@code item.Monitor} in 1.20.1).
 *  - ItemStack NBT is no longer {@code getOrCreateTag()}; SBW exposes {@code NBTTool.getTag(stack)} /
 *    {@code NBTTool.saveTag(stack, tag)} as the 1.21 component-backed replacement. The linked-drone id and the
 *    "Using" flag live in that tag with the same keys ({@link MonitorItem#LINKED_DRONE}, "Using").
 *  - This class is only ever class-loaded behind a {@code "superbwarfare:monitor"} item-key guard at the call sites
 *    (EventHandler / CoverFirePacket), so it is safe even though superbwarfare is an optional dependency.
 */
public class SBWCompat {

    /** "Using" flag key on the monitor stack (constant inlined from SBW MonitorItem). */
    private static final String MONITOR_USING_KEY = "Using";

    @Nullable
    public static BlockPos droneCover(Player player, ItemStack itemStack, Level level, boolean simulate) {
        if (!player.getCooldowns().isOnCooldown(itemStack.getItem())) {
            DroneEntity drone = EntityFindUtil.findDrone(level, NBTTool.getTag(itemStack).getString(MonitorItem.LINKED_DRONE));
            if (drone != null) {
                BlockPos pos = MortarEntity.getCoverPos(drone);
                if (pos != null) {
                    if (!simulate) {
                        PacketDistributor.sendToServer(new CoverFirePacket(pos));
                        player.level().playLocalSound(BlockPos.containing(drone.position()), ZombieKitSounds.radio_response.get(), SoundSource.PLAYERS, 1, 1, false);
                    }
                    return pos;
                }
            }
        }
        return null;
    }

    /**
     * Reads the monitor "Using" flag via SBW's NBTTool (replaces {@code stack.getOrCreateTag().getBoolean("Using")}).
     * Called from {@link CoverFirePacket} only inside the monitor item-key guard.
     */
    public static boolean isMonitorUsing(ItemStack monitorStack) {
        return NBTTool.getTag(monitorStack).getBoolean(MONITOR_USING_KEY);
    }
}
