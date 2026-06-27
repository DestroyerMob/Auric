package com.ethanhellyer.auric;

import com.ethanhellyer.auric.client.AuricClient;
import com.ethanhellyer.auric.cauldron.CauldronBrewingEvents;
import com.ethanhellyer.auric.camo.CamoNetworking;
import com.ethanhellyer.auric.camo.CamoSkinEvents;
import com.ethanhellyer.auric.config.AuricConfig;
import com.ethanhellyer.auric.imbue.ImbuingEvents;
import com.ethanhellyer.auric.registry.CreativeTabEvents;
import com.ethanhellyer.auric.registry.ModBlockEntityTypes;
import com.ethanhellyer.auric.registry.ModBlocks;
import com.ethanhellyer.auric.registry.ModDataComponents;
import com.ethanhellyer.auric.registry.ModEntityTypes;
import com.ethanhellyer.auric.registry.ModItems;
import com.ethanhellyer.auric.registry.ModMenuTypes;
import com.ethanhellyer.auric.registry.ModRecipeSerializers;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;

@Mod(Auric.MOD_ID)
public final class Auric {
    public static final String MOD_ID = "auric";

    public Auric(IEventBus modBus, ModContainer modContainer) {
        ModDataComponents.register(modBus);
        ModEntityTypes.register(modBus);
        ModBlocks.register(modBus);
        ModItems.register(modBus);
        ModBlockEntityTypes.register(modBus);
        ModMenuTypes.register(modBus);
        ModRecipeSerializers.register(modBus);
        modBus.addListener(CreativeTabEvents::addContents);
        modBus.addListener(CamoNetworking::registerPayloads);
        modContainer.registerConfig(ModConfig.Type.SERVER, AuricConfig.SPEC, "auric-server.toml");

        if (FMLEnvironment.dist.isClient()) {
            AuricClient.register(modBus);
        }

        NeoForge.EVENT_BUS.register(new ImbuingEvents());
        NeoForge.EVENT_BUS.register(new CauldronBrewingEvents());
        NeoForge.EVENT_BUS.register(new CamoSkinEvents());
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
