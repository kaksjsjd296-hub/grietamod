package com.grietamod.net;

import com.grietamod.client.ClientRiftState;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** Servidor -> Clientes: abrir o cerrar la grieta. */
public class RiftControlPacket {
    public static final byte OPEN = 0;
    public static final byte CLOSE = 1;

    public final byte action;
    public final float yaw;
    public final float elevation;
    /** Ticks que lleva abierta (para jugadores que entran con la grieta ya abierta). */
    public final int elapsed;

    public RiftControlPacket(byte action, float yaw, float elevation, int elapsed) {
        this.action = action;
        this.yaw = yaw;
        this.elevation = elevation;
        this.elapsed = elapsed;
    }

    public static void encode(RiftControlPacket p, FriendlyByteBuf buf) {
        buf.writeByte(p.action);
        buf.writeFloat(p.yaw);
        buf.writeFloat(p.elevation);
        buf.writeVarInt(p.elapsed);
    }

    public static RiftControlPacket decode(FriendlyByteBuf buf) {
        return new RiftControlPacket(buf.readByte(), buf.readFloat(), buf.readFloat(), buf.readVarInt());
    }

    public static void handle(RiftControlPacket p, Supplier<NetworkEvent.Context> ctx) {
        DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientRiftState.onControl(p));
    }
}
