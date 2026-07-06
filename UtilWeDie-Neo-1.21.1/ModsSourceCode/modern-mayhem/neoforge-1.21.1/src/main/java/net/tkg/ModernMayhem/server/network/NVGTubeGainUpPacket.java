package net.tkg.ModernMayhem.server.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.tkg.ModernMayhem.ModernMayhemMod;
import net.tkg.ModernMayhem.server.item.curios.facewear.NVGGogglesItem;
import net.tkg.ModernMayhem.server.item.generic.GenericSpecialGogglesItem;
import net.tkg.ModernMayhem.server.registry.SoundRegistryMM;
import net.tkg.ModernMayhem.server.util.CuriosUtil;

public class NVGTubeGainUpPacket implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<NVGTubeGainUpPacket> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ModernMayhemMod.ID, "nvg_tube_gain_up"));
    public static final StreamCodec<RegistryFriendlyByteBuf, NVGTubeGainUpPacket> STREAM_CODEC = StreamCodec.of((buf, packet) -> {}, buf -> new NVGTubeGainUpPacket());

    public NVGTubeGainUpPacket() {
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(NVGTubeGainUpPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ItemStack nvgItem;
            ServerPlayer player = (ServerPlayer) context.player();
            if (player != null && CuriosUtil.hasNVGEquipped((Player)player) && (nvgItem = CuriosUtil.getFaceWearItem((Player)player)).getItem() instanceof NVGGogglesItem) {
                if (GenericSpecialGogglesItem.isAutoGainEnabled(nvgItem)) {
                    return;
                }
                GenericSpecialGogglesItem.switchConfigUp(nvgItem);
                if (GenericSpecialGogglesItem.hasConfigIndexChanged((Player)player, nvgItem)) {
                    player.playNotifySound((SoundEvent)SoundRegistryMM.SMALL_CLICK.get(), SoundSource.NEUTRAL, 1.0f, 1.0f);
                }
            }
        });
    }
}
