package com.scarasol.zombiekit.network;

import com.scarasol.zombiekit.ZombieKitMod;
import com.scarasol.zombiekit.api.MortarLevel;
import com.scarasol.zombiekit.data.LaunchSchedule;
import com.scarasol.zombiekit.init.ZombieKitSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record CoverFirePacket(BlockPos blockPos) implements CustomPacketPayload {

    public static final Type<CoverFirePacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ZombieKitMod.MODID, "cover_fire"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CoverFirePacket> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, CoverFirePacket::blockPos,
            CoverFirePacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public BlockPos getBlockPos() {
        return blockPos;
    }

    public static void handler(CoverFirePacket msg, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                Level level = player.level();
                if (level instanceof MortarLevel mortarLevel) {
                    if (player.getUseItem().is(Items.SPYGLASS)) {
                        player.stopUsingItem();
                        player.getCooldowns().addCooldown(Items.SPYGLASS, 100);
                    } else if (BuiltInRegistries.ITEM.getKey(player.getMainHandItem().getItem()).toString().equals("superbwarfare:monitor")) {
                        if (!com.scarasol.zombiekit.compat.SBWCompat.isMonitorUsing(player.getMainHandItem()))
                            return;
                        player.getCooldowns().addCooldown(player.getMainHandItem().getItem(), 100);
                    }
                    level.playSound(null, BlockPos.containing(player.position()), ZombieKitSounds.radio_response.get(), SoundSource.PLAYERS, 1, 1);
                    mortarLevel.getMortarManager().postSchedule(new LaunchSchedule(level.getGameTime(), msg.getBlockPos()));
                }
            }
        });
    }
}
