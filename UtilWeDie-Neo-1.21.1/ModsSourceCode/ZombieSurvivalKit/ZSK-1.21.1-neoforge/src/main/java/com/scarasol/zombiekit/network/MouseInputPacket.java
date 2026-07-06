package com.scarasol.zombiekit.network;

import com.scarasol.sona.util.SonaMath;
import com.scarasol.zombiekit.ZombieKitMod;
import com.scarasol.zombiekit.entity.mechanics.HeavyMachineGunEntity;
import com.scarasol.zombiekit.entity.mechanics.MortarEntity;
import com.scarasol.zombiekit.init.ZombieKitItems;
import com.scarasol.zombiekit.item.armor.ExoArmor;
import com.scarasol.zombiekit.item.weapon.Flamethrower;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

//0 左键；1 右键；2 滚轮
public record MouseInputPacket(int kind, double value) implements CustomPacketPayload {

    public static final Type<MouseInputPacket> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(ZombieKitMod.MODID, "mouse_input"));

    public static final StreamCodec<RegistryFriendlyByteBuf, MouseInputPacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT, MouseInputPacket::kind,
            ByteBufCodecs.DOUBLE, MouseInputPacket::value,
            MouseInputPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handler(MouseInputPacket msg, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                if (player.getVehicle() instanceof HeavyMachineGunEntity heavyMachineGunEntity) {
                    if (msg.kind() == 0) {
                        heavyMachineGunEntity.setFire(msg.value() == 1);
                    }
                } else if (player.getVehicle() instanceof MortarEntity mortarEntity) {
                    if (msg.kind() == 2) {
                        mortarEntity.setAngle((float) (mortarEntity.getAngle() + msg.value()));
                        boolean flag = player.getInventory().hasAnyMatching(itemStack -> itemStack.is(ZombieKitItems.SHOOTING_PARAMETER.get()));
                        if (flag || (ExoArmor.numberOfSuit(player) >= 4 && ExoArmor.getPower(player.getItemBySlot(EquipmentSlot.CHEST)) > 0)) {
                            double x = SonaMath.parabolaXDistanceCalculate(-mortarEntity.getAngle(), MortarEntity.VELOCITY);
                            player.displayClientMessage(Component.literal(Component.translatable("gui.zombiekit.mortar.angle", -mortarEntity.getAngle()).getString() + Component.translatable("gui.zombiekit.mortar.distance", Math.round(x)).getString()), true);
                        } else
                            player.displayClientMessage(Component.translatable("gui.zombiekit.mortar.angle", -mortarEntity.getAngle()), true);
                    }
                } else if (player.getMainHandItem().getItem() instanceof Flamethrower flamethrower) {
                    ItemStack itemStack = player.getItemInHand(InteractionHand.MAIN_HAND);
                    if (msg.value() == 1) {
                        if (player.level().getGameTime() - flamethrower.getReloadTime(itemStack) > 47) {
                            flamethrower.putUsing(itemStack, true);
                            flamethrower.putStartUsingTick(itemStack, player.level().getGameTime());
                        }
                    } else {
                        flamethrower.putUsing(itemStack, false);
                    }
                }
            }
        });
    }
}
