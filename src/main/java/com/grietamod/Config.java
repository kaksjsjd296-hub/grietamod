package com.grietamod;

import net.minecraftforge.common.ForgeConfigSpec;

public class Config {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.IntValue AUTO_CLOSE_SECONDS = BUILDER
            .comment("Segundos que la grieta permanece abierta antes de cerrarse sola. 0 = nunca se cierra sola (solo con el item).")
            .defineInRange("autoCloseSeconds", 0, 0, 86400);

    public static final ForgeConfigSpec SPEC = BUILDER.build();
}
