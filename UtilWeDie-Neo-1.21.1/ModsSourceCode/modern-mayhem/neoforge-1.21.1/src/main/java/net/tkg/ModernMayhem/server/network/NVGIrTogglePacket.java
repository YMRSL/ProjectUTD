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

/**
 * 玩家按 IR 键时发到服务端: 切换主动红外照明的开关状态 (写入夜视仪 NBT "IrActive")。
 * 只有四镜头 GPNVG 的 IR 可主动开关; 单/双镜头常开, 按键对其无效。
 */
public class NVGIrTogglePacket implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<NVGIrTogglePacket> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(ModernMayhemMod.ID, "nvg_ir_toggle"));
    public static final StreamCodec<RegistryFriendlyByteBuf, NVGIrTogglePacket> STREAM_CODEC = StreamCodec.of((buf, packet) -> {}, buf -> new NVGIrTogglePacket());

    public NVGIrTogglePacket() {
    }

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(NVGIrTogglePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            ItemStack facewearItem;
            ServerPlayer player = (ServerPlayer) context.player();
            if (player != null && CuriosUtil.hasNVGEquipped((Player)player) && (facewearItem = CuriosUtil.getFaceWearItem((Player)player)).getItem() instanceof NVGGogglesItem nvg && nvg.isQuad()) {
                GenericSpecialGogglesItem.switchIrMode(facewearItem);
                player.playNotifySound((SoundEvent)SoundRegistryMM.SMALL_CLICK.get(), SoundSource.NEUTRAL, 1.0f, 1.0f);
            }
        });
    }
}
