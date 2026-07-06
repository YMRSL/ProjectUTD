package com.scarasol.zombiekit.compat;

import com.atsuishio.superbwarfare.entity.vehicle.DroneEntity;
import com.atsuishio.superbwarfare.item.Monitor;
import com.atsuishio.superbwarfare.tools.EntityFindUtil;
import com.scarasol.zombiekit.entity.mechanics.MortarEntity;
import com.scarasol.zombiekit.init.ZombieKitSounds;
import com.scarasol.zombiekit.network.CoverFirePacket;
import com.scarasol.zombiekit.network.NetworkHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;

public class SBWCompat {
    @Nullable
    public static BlockPos droneCover(Player player, ItemStack itemStack, Level level, boolean simulate) {
        if (!player.getCooldowns().isOnCooldown(itemStack.getItem())) {
            DroneEntity drone = EntityFindUtil.findDrone(level, itemStack.getOrCreateTag().getString(Monitor.LINKED_DRONE));
            if (drone != null) {
                BlockPos pos = MortarEntity.getCoverPos(drone);
                if (pos != null) {
                    if (!simulate) {
                        NetworkHandler.PACKET_HANDLER.sendToServer(new CoverFirePacket(pos));
                        player.level().playLocalSound(BlockPos.containing(drone.position()), ZombieKitSounds.radio_response.get(), SoundSource.PLAYERS, 1, 1, false);
                    }
                    return pos;
                }
            }
        }
        return null;
    }


}
