package net.mcreator.survivalinstinct.network;

import net.mcreator.survivalinstinct.SurvivalInstinctMod;
import net.mcreator.survivalinstinct.procedures.ExoSuitDashOnKeyPressedProcedure;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

@EventBusSubscriber(modid = SurvivalInstinctMod.MODID, bus = EventBusSubscriber.Bus.MOD)
public class ExoSuitDashMessage implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ExoSuitDashMessage> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(SurvivalInstinctMod.MODID, "exo_suit_dash"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ExoSuitDashMessage> STREAM_CODEC = StreamCodec.of(
            (buf, message) -> {
                buf.writeInt(message.type);
                buf.writeInt(message.pressedms);
            },
            buf -> new ExoSuitDashMessage(buf.readInt(), buf.readInt()));

    public final int type;
    public final int pressedms;

    public ExoSuitDashMessage(int type, int pressedms) {
        this.type = type;
        this.pressedms = pressedms;
    }

    @Override
    public CustomPacketPayload.Type<ExoSuitDashMessage> type() {
        return TYPE;
    }

    public static void handle(ExoSuitDashMessage message, IPayloadContext context) {
        context.enqueueWork(() -> ExoSuitDashMessage.pressAction((Player) context.player(), message.type, message.pressedms));
    }

    public static void pressAction(Player entity, int type, int pressedms) {
        Level world = entity.level();
        double x = entity.getX();
        double y = entity.getY();
        double z = entity.getZ();
        if (!world.hasChunkAt(entity.blockPosition())) {
            return;
        }
        if (type == 0) {
            ExoSuitDashOnKeyPressedProcedure.execute((LevelAccessor) world, x, y, z, (Entity) entity);
        }
    }

    @SubscribeEvent
    public static void registerMessage(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToServer(TYPE, STREAM_CODEC, ExoSuitDashMessage::handle);
    }
}
