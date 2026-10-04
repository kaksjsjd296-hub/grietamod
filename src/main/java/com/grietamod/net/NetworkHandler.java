package com.grietamod.net;

import com.grietamod.GrietaMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class NetworkHandler {
    private static final String PROTOCOL = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(GrietaMod.MODID, "main"),
            () -> PROTOCOL, PROTOCOL::equals, PROTOCOL::equals);

    public static void register() {
        int id = 0;
        CHANNEL.messageBuilder(UploadChunkPacket.class, id++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(UploadChunkPacket::encode)
                .decoder(UploadChunkPacket::decode)
                .consumerMainThread(UploadChunkPacket::handle)
                .add();
        CHANNEL.messageBuilder(ImageChunkPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(ImageChunkPacket::encode)
                .decoder(ImageChunkPacket::decode)
                .consumerMainThread(ImageChunkPacket::handle)
                .add();
        CHANNEL.messageBuilder(RiftControlPacket.class, id++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(RiftControlPacket::encode)
                .decoder(RiftControlPacket::decode)
                .consumerMainThread(RiftControlPacket::handle)
                .add();
    }
}
