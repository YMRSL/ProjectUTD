package com.scarasol.zombiekit.network;

import com.scarasol.zombiekit.item.armor.ExoArmor;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class DoubleJumpPacket {

    public static void encode(DoubleJumpPacket message, FriendlyByteBuf buffer) {
    }

    public static DoubleJumpPacket decode(FriendlyByteBuf buf){
        return new DoubleJumpPacket();
    }

    public static void handler(DoubleJumpPacket message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            if (context.getDirection().getReceptionSide().isServer()) {
                Player player = context.getSender();
                ServerLevel serverLevel = (ServerLevel) player.level();
                if (ExoArmor.numberOfSuit(player) == 4 && ExoArmor.getPower(player.getItemBySlot(EquipmentSlot.CHEST)) > 0 && !player.onGround() && !player.isFallFlying()  && !player.isInWater() && !player.getAbilities().flying && !player.isPassenger() && !player.onClimbable()) {
                    player.jumpFromGround();
                    serverLevel.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY(), player.getZ(), 100, 0.5, 0.2, 0.5, 0.1);
                    ExoArmor.addPower(player.getItemBySlot(EquipmentSlot.CHEST), -1);
                }

            }
        });
        context.setPacketHandled(true);
    }
}
