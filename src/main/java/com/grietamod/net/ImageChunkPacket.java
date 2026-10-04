package com.grietamod.net;

import com.grietamod.client.ClientRiftState;
import com.grietamod.server.RiftManager;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Servidor -> Clientes: un trozo de la imagen PNG de la grieta. total == 0 significa "sin imagen". */
public class ImageChunkPacket {
    public final int index;
    public final int total;
    public final byte[] data;

    public ImageChunkPacket(int index, int total, byte[] data) {
        this.index = index;
        this.total = total;
        this.data = data;
    }

    public static void encode(ImageChunkPacket p, FriendlyByteBuf buf) {
        buf.writeVarInt(p.index);
        buf.writeVarInt(p.total);
        buf.writeByteArray(p.data);
    }

    public static ImageChunkPacket decode(FriendlyByteBuf buf) {
        int index = buf.readVarInt();
        int total = buf.readVarInt();
        byte[] data = buf.readByteArray(RiftManager.CHUNK_SIZE + 64);
        return new ImageChunkPacket(index, total, data);
    }

    public static void handle(ImageChunkPacket p, Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientRiftState.onImageChunk(p));
    }
}
