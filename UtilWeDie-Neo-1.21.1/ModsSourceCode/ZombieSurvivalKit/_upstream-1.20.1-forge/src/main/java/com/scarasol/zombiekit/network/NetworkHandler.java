package com.scarasol.zombiekit.network;


import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import static com.scarasol.zombiekit.ZombieKitMod.MODID;

public class NetworkHandler {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel PACKET_HANDLER = NetworkRegistry.newSimpleChannel(new ResourceLocation(MODID, MODID), () -> PROTOCOL_VERSION, PROTOCOL_VERSION::equals, PROTOCOL_VERSION::equals);
    private static int messageID = 0;

    public static <T> void addNetworkMessage() {
        PACKET_HANDLER.registerMessage(messageID++, KeyBindPacket.class, KeyBindPacket::encode, KeyBindPacket::decode, KeyBindPacket::handler);
        PACKET_HANDLER.registerMessage(messageID++, SavedDataSyncPacket.class, SavedDataSyncPacket::encode, SavedDataSyncPacket::decode, SavedDataSyncPacket::handler);
        PACKET_HANDLER.registerMessage(messageID++, SyncBlockPacket.class, SyncBlockPacket::encode, SyncBlockPacket::decode, SyncBlockPacket::handler);
        PACKET_HANDLER.registerMessage(messageID++, CoverFirePacket.class, CoverFirePacket::encode, CoverFirePacket::decode, CoverFirePacket::handler);
        PACKET_HANDLER.registerMessage(messageID++, MouseInputPacket.class, MouseInputPacket::encode, MouseInputPacket::decode, MouseInputPacket::handler);
        PACKET_HANDLER.registerMessage(messageID++, DoubleJumpPacket.class, DoubleJumpPacket::encode, DoubleJumpPacket::decode, DoubleJumpPacket::handler);
    }
}
