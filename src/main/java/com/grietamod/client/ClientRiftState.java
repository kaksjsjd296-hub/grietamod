package com.grietamod.client;

import com.grietamod.GrietaMod;
import com.grietamod.net.ImageChunkPacket;
import com.grietamod.net.RiftControlPacket;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

/** Estado de la grieta que ve ESTE cliente (lo controla el servidor con paquetes). */
public final class ClientRiftState {
    public static final ResourceLocation IMAGE_LOC = new ResourceLocation(GrietaMod.MODID, "dynamic/rift_image");

    // Duraciones de la animacion (en ticks, 20 ticks = 1 segundo)
    private static final float SKY_TICKS = 120f;   // el cielo se pone rojo en 6 s
    private static final float RIFT_OPEN_TICKS = 80f;  // la grieta se abre en 4 s
    private static final float RIFT_CLOSE_TICKS = 60f; // la grieta se cierra en 3 s
    private static final float SKY_FADE_TICKS = 100f;  // el cielo vuelve a la normalidad en 5 s

    public static boolean open = false;
    public static float sky, prevSky, rift, prevRift;
    public static float yaw, elevation;

    public static boolean imageLoaded = false;
    public static int imageW = 1, imageH = 1;

    private static DynamicTexture imageTexture;
    private static ByteArrayOutputStream imageBuffer;

    private ClientRiftState() {}

    public static boolean isVisible() {
        return sky > 0.001f || prevSky > 0.001f || rift > 0.001f || prevRift > 0.001f;
    }

    public static void tick() {
        prevSky = sky;
        prevRift = rift;
        if (open) {
            sky = Math.min(1f, sky + 1f / SKY_TICKS);
            if (sky >= 0.75f) {
                rift = Math.min(1f, rift + 1f / RIFT_OPEN_TICKS);
            }
        } else {
            rift = Math.max(0f, rift - 1f / RIFT_CLOSE_TICKS);
            if (rift <= 0.3f) {
                sky = Math.max(0f, sky - 1f / SKY_FADE_TICKS);
            }
        }
    }

    public static void reset() {
        open = false;
        sky = prevSky = rift = prevRift = 0f;
        imageBuffer = null;
        clearImage();
    }

    public static void onControl(RiftControlPacket p) {
        if (p.action == RiftControlPacket.OPEN) {
            open = true;
            yaw = p.yaw;
            elevation = p.elevation;
            if (p.elapsed > 0) {
                // Jugador que entra con la grieta ya abierta: saltar la animacion
                sky = rift = 0f;
                for (int i = 0; i < Math.min(p.elapsed, 400); i++) {
                    tick();
                }
                prevSky = sky;
                prevRift = rift;
            } else {
                Minecraft.getInstance().getSoundManager()
                        .play(SimpleSoundInstance.forUI(SoundEvents.WITHER_SPAWN, 0.5f, 0.7f));
            }
        } else {
            open = false;
            Minecraft.getInstance().getSoundManager()
                    .play(SimpleSoundInstance.forUI(SoundEvents.ENDER_DRAGON_GROWL, 0.5f, 0.6f));
        }
    }

    // ------------------------------------------------------------------ imagen

    public static void onImageChunk(ImageChunkPacket p) {
        if (p.total == 0) {
            imageBuffer = null;
            clearImage();
            return;
        }
        if (p.index == 0) {
            imageBuffer = new ByteArrayOutputStream();
        }
        if (imageBuffer == null) {
            return;
        }
        imageBuffer.write(p.data, 0, p.data.length);
        if (p.index == p.total - 1) {
            byte[] bytes = imageBuffer.toByteArray();
            imageBuffer = null;
            setImage(bytes);
        }
    }

    private static void setImage(byte[] png) {
        clearImage();
        try {
            NativeImage ni = NativeImage.read(new ByteArrayInputStream(png));
            imageW = Math.max(1, ni.getWidth());
            imageH = Math.max(1, ni.getHeight());
            imageTexture = new DynamicTexture(ni); // la textura se queda con la imagen
            Minecraft.getInstance().getTextureManager().register(IMAGE_LOC, imageTexture);
            imageTexture.setFilter(true, false);
            imageLoaded = true;
        } catch (Exception e) {
            imageLoaded = false;
            imageTexture = null;
        }
    }

    private static void clearImage() {
        if (imageTexture != null) {
            Minecraft.getInstance().getTextureManager().release(IMAGE_LOC);
            imageTexture = null;
        }
        imageLoaded = false;
    }
}
