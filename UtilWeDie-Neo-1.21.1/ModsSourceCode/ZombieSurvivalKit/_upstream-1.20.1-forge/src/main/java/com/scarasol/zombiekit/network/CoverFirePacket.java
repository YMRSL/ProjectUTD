package com.scarasol.zombiekit.network;

import com.scarasol.zombiekit.api.MortarLevel;
import com.scarasol.zombiekit.data.LaunchSchedule;
import com.scarasol.zombiekit.init.ZombieKitSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.function.Supplier;

public class CoverFirePacket {
    private final BlockPos blockPos;

    public CoverFirePacket(BlockPos blockPos) {
        this.blockPos = blockPos;
    }

    public BlockPos getBlockPos() {
        return blockPos;
    }


    public static CoverFirePacket decode(FriendlyByteBuf buf) {
        return new CoverFirePacket(buf.readBlockPos());
    }

    public static void encode(CoverFirePacket msg, FriendlyByteBuf buf) {
        buf.writeBlockPos(msg.getBlockPos());
    }

    public static void handler(CoverFirePacket msg, Supplier<NetworkEvent.Context> context) {
        context.get().enqueueWork(() -> {
            if (msg != null) {
                context.get().enqueueWork(() -> {
                    if (context.get().getDirection().getReceptionSide().isServer()){
                        ServerPlayer player = context.get().getSender();
                        if (player != null) {
                            Level level = player.level();
                            if (level instanceof MortarLevel mortarLevel) {
                                if (player.getUseItem().is(Items.SPYGLASS)) {
                                    player.stopUsingItem();
                                    player.getCooldowns().addCooldown(Items.SPYGLASS, 100);
                                }else if (ForgeRegistries.ITEMS.getKey(player.getMainHandItem().getItem()).toString().equals("superbwarfare:monitor")) {
                                    if (!player.getMainHandItem().getOrCreateTag().getBoolean("Using"))
                                        return;
                                    player.getCooldowns().addCooldown(player.getMainHandItem().getItem(), 100);
                                }
                                level.playSound(null, BlockPos.containing(player.position()), ZombieKitSounds.radio_response.get(), SoundSource.PLAYERS, 1, 1);
                                mortarLevel.getMortarManager().postSchedule(new LaunchSchedule(level.getGameTime(), msg.getBlockPos()));
                            }
                        }

                    }
                });
            }
        });
        context.get().setPacketHandled(true);
    }
}
