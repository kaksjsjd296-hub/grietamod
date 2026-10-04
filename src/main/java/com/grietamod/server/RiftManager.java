package com.grietamod.server;

import com.grietamod.Config;
import com.grietamod.GrietaMod;
import com.grietamod.net.ImageChunkPacket;
import com.grietamod.net.NetworkHandler;
import com.grietamod.net.RiftControlPacket;
import com.grietamod.net.UploadChunkPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;
import net.minecraftforge.network.PacketDistributor;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Estado global de la grieta en el servidor. Todos los jugadores ven la misma grieta
 * porque el servidor se la manda a todos (y a los que entran despues).
 */
@Mod.EventBusSubscriber(modid = GrietaMod.MODID)
public class RiftManager {
    public static final int CHUNK_SIZE = 30000;
    public static final int MAX_IMAGE_BYTES = 1_000_000;
    private static final int MAX_CHUNKS = MAX_IMAGE_BYTES / CHUNK_SIZE + 2;

    private static boolean active = false;
    private static int ticksOpen = 0;
    private static float yaw = 0f;
    private static float elevation = 42f;
    private static byte[] image = null;

    private static final Map<UUID, Upload> uploads = new HashMap<>();

    private static class Upload {
        final int total;
        int next = 0;
        final ByteArrayOutputStream out = new ByteArrayOutputStream();

        Upload(int total) {
            this.total = total;
        }
    }

    public static boolean isActive() {
        return active;
    }

    // ------------------------------------------------------------------ abrir / cerrar

    public static void toggle(ServerPlayer player) {
        if (active) {
            close(player);
        } else {
            open(player);
        }
    }

    public static void open(ServerPlayer player) {
        active = true;
        ticksOpen = 0;
        yaw = player.getYRot();
        elevation = 42f;
        sendImage(PacketDistributor.ALL.noArg());
        NetworkHandler.CHANNEL.send(PacketDistributor.ALL.noArg(),
                new RiftControlPacket(RiftControlPacket.OPEN, yaw, elevation, 0));
        player.server.getPlayerList().broadcastSystemMessage(Component.translatable("message.grietamod.opening"), false);
    }

    public static void close(ServerPlayer player) {
        closeInternal();
        if (player != null) {
            player.server.getPlayerList().broadcastSystemMessage(Component.translatable("message.grietamod.closing"), false);
        }
    }

    private static void closeInternal() {
        active = false;
        ticksOpen = 0;
        NetworkHandler.CHANNEL.send(PacketDistributor.ALL.noArg(),
                new RiftControlPacket(RiftControlPacket.CLOSE, yaw, elevation, 0));
    }

    // ------------------------------------------------------------------ imagen

    private static void sendImage(PacketDistributor.PacketTarget target) {
        byte[] img = image;
        if (img == null || img.length == 0) {
            NetworkHandler.CHANNEL.send(target, new ImageChunkPacket(0, 0, new byte[0]));
            return;
        }
        int total = (img.length + CHUNK_SIZE - 1) / CHUNK_SIZE;
        for (int i = 0; i < total; i++) {
            int from = i * CHUNK_SIZE;
            int to = Math.min(img.length, from + CHUNK_SIZE);
            NetworkHandler.CHANNEL.send(target, new ImageChunkPacket(i, total, Arrays.copyOfRange(img, from, to)));
        }
    }

    /** Llamado cuando un jugador sube trozos de imagen desde el menu de la grieta configuradora. */
    public static void receiveUpload(ServerPlayer player, UploadChunkPacket p) {
        // Solo quien sostiene el item configurador puede cambiar la imagen.
        boolean holding = player.getMainHandItem().is(GrietaMod.RIFT_CONFIG.get())
                || player.getOffhandItem().is(GrietaMod.RIFT_CONFIG.get());
        if (!holding) {
            return;
        }

        if (p.total == 0) {
            image = null;
            uploads.remove(player.getUUID());
            if (active) {
                sendImage(PacketDistributor.ALL.noArg());
            }
            player.displayClientMessage(Component.translatable("message.grietamod.image_removed"), true);
            return;
        }

        if (p.total < 0 || p.total > MAX_CHUNKS || p.data.length > CHUNK_SIZE) {
            uploads.remove(player.getUUID());
            return;
        }

        Upload up = uploads.get(player.getUUID());
        if (p.index == 0) {
            up = new Upload(p.total);
            uploads.put(player.getUUID(), up);
        }
        if (up == null || up.total != p.total || up.next != p.index) {
            uploads.remove(player.getUUID());
            return;
        }

        up.out.write(p.data, 0, p.data.length);
        up.next++;
        if (up.out.size() > MAX_IMAGE_BYTES) {
            uploads.remove(player.getUUID());
            player.displayClientMessage(Component.translatable("message.grietamod.image_too_big"), true);
            return;
        }

        if (up.next == up.total) {
            uploads.remove(player.getUUID());
            byte[] bytes = up.out.toByteArray();
            if (!isPng(bytes)) {
                player.displayClientMessage(Component.translatable("message.grietamod.image_invalid"), true);
                return;
            }
            image = bytes;
            if (active) {
                sendImage(PacketDistributor.ALL.noArg());
            }
            player.displayClientMessage(Component.translatable("message.grietamod.image_saved"), true);
        }
    }

    private static boolean isPng(byte[] b) {
        return b.length > 8
                && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G';
    }

    // ------------------------------------------------------------------ eventos

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !active) {
            return;
        }
        ticksOpen++;
        int limit = Config.AUTO_CLOSE_SECONDS.get() * 20;
        if (limit > 0 && ticksOpen >= limit) {
            closeInternal();
            if (ServerLifecycleHooks.getCurrentServer() != null) {
                ServerLifecycleHooks.getCurrentServer().getPlayerList()
                        .broadcastSystemMessage(Component.translatable("message.grietamod.closing"), false);
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerJoin(PlayerEvent.PlayerLoggedInEvent event) {
        if (active && event.getEntity() instanceof ServerPlayer sp) {
            PacketDistributor.PacketTarget target = PacketDistributor.PLAYER.with(() -> sp);
            sendImage(target);
            NetworkHandler.CHANNEL.send(target,
                    new RiftControlPacket(RiftControlPacket.OPEN, yaw, elevation, ticksOpen));
        }
    }

    @SubscribeEvent
    public static void onPlayerLeave(PlayerEvent.PlayerLoggedOutEvent event) {
        uploads.remove(event.getEntity().getUUID());
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        active = false;
        ticksOpen = 0;
        image = null;
        uploads.clear();
    }
}
