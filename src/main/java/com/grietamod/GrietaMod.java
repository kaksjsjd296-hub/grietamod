package com.grietamod;

import com.grietamod.item.RiftActivatorItem;
import com.grietamod.item.RiftConfigItem;
import com.grietamod.net.NetworkHandler;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod(GrietaMod.MODID)
public class GrietaMod {
    public static final String MODID = "grietamod";

    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    /** Item que ABRE / CIERRA la grieta. */
    public static final RegistryObject<Item> RIFT_ACTIVATOR = ITEMS.register("grieta",
            () -> new RiftActivatorItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));

    /** Item que CONFIGURA la imagen (abre el menu). */
    public static final RegistryObject<Item> RIFT_CONFIG = ITEMS.register("grieta_configuradora",
            () -> new RiftConfigItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));

    public static final RegistryObject<CreativeModeTab> TAB = TABS.register("main",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + MODID))
                    .icon(() -> new ItemStack(RIFT_ACTIVATOR.get()))
                    .displayItems((params, output) -> {
                        output.accept(RIFT_ACTIVATOR.get());
                        output.accept(RIFT_CONFIG.get());
                    })
                    .build());

    public GrietaMod() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ITEMS.register(bus);
        TABS.register(bus);
        bus.addListener(this::commonSetup);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(NetworkHandler::register);
    }
}
