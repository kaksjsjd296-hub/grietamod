package com.grietamod.client;

import net.minecraft.client.Minecraft;

/** Puente seguro para llamar codigo de cliente desde clases comunes. */
public class ClientHooks {
    public static void openConfigScreen() {
        Minecraft.getInstance().setScreen(new RiftConfigScreen());
    }
}
