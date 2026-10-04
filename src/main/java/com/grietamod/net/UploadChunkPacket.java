package com.grietamod.net;

import com.grietamod.server.RiftManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Cliente -> Servidor: un trozo de la imagen PNG. total == 0 significa "quitar imagen". */
public class UploadChunkPacket {
    public final int index;
    public final int total;
    public final byte[] data;

    public UploadChunkPacket(int index, int total, byte[] data) {
        this.index = index;
        this.total = total;
        this.data = data;
    }

    public static void encode(UploadChunkPacket p, FriendlyByteBuf buf) {
        buf.writeVarInt(p.index);
        buf.writeVarInt(p.total);
        buf.writeByteArray(p.data);
    }

    public static UploadChunkPacket decode(FriendlyByteBuf buf) {
        int index = buf.readVarInt();
        int total = buf.readVarInt();
        byte[] data = buf.readByteArray(RiftManager.CHUNK_SIZE + 64);
        return new UploadChunkPacket(index, total, data);
    }

    public static void handle(UploadChunkPacket p, Supplier<NetworkEvent.Context> ctx) {
        ServerPlayer sender = ctx.get().getSender();
        if (sender != null) {
            RiftManager.receiveUpload(sender, p);
        }
    }
}
