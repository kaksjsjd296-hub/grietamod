package com.grietamod.client;

import com.grietamod.GrietaMod;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = GrietaMod.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ClientEvents {

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        if (Minecraft.getInstance().isPaused()) {
            return;
        }
        ClientRiftState.tick();
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientRiftState.reset();
    }

    /** Tine la niebla del horizonte de rojo mientras el cielo esta rojo. */
    @SubscribeEvent
    public static void onFogColor(ViewportEvent.ComputeFogColor event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || !mc.level.dimensionType().hasSkyLight()) {
            return;
        }
        float pt = (float) event.getPartialTick();
        float s = RiftRenderer.smooth(Mth.lerp(pt, ClientRiftState.prevSky, ClientRiftState.sky));
        if (s <= 0.001f) {
            return;
        }
        float k = s * 0.85f;
        event.setRed(Mth.lerp(k, event.getRed(), 0.32f));
        event.setGreen(Mth.lerp(k, event.getGreen(), 0.02f));
        event.setBlue(Mth.lerp(k, event.getBlue(), 0.04f));
    }
}
