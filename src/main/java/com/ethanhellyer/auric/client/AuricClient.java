package com.ethanhellyer.auric.client;

import com.ethanhellyer.auric.Auric;
import com.ethanhellyer.auric.camo.CamouflageHelper;
import com.ethanhellyer.auric.blockentity.PotionCauldronBlockEntity;
import com.ethanhellyer.auric.registry.ModBlockEntityTypes;
import com.ethanhellyer.auric.registry.ModBlocks;
import com.ethanhellyer.auric.registry.ModEntityTypes;
import com.ethanhellyer.auric.registry.ModMenuTypes;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.entity.ThrownItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.ModelEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.event.RegisterRenderBuffersEvent;

public final class AuricClient {
    private AuricClient() {
    }

    public static void register(IEventBus modBus) {
        modBus.addListener(AuricClient::registerScreens);
        modBus.addListener(AuricClient::registerEntityRenderers);
        modBus.addListener(AuricClient::registerBlockColors);
        modBus.addListener(AuricClient::registerRenderBuffers);
        modBus.addListener(AuricClient::modifyBakedModels);
    }

    private static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(ModMenuTypes.IMBUING_TABLE.get(), ImbuingTableScreen::new);
        event.register(ModMenuTypes.BLOCK_PALETTE.get(), BlockPaletteScreen::new);
    }

    private static void registerEntityRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntityTypes.SCULK_EXPERIENCE_BOTTLE.get(), ThrownItemRenderer::new);
        event.registerBlockEntityRenderer(ModBlockEntityTypes.SWORD_IN_STONE.get(), SwordInStoneRenderer::new);
    }

    private static void registerBlockColors(RegisterColorHandlersEvent.Block event) {
        event.register((state, level, pos, tintIndex) -> {
            if (tintIndex != 0) {
                return -1;
            }
            if (level != null && pos != null && level.getBlockEntity(pos) instanceof PotionCauldronBlockEntity cauldron) {
                return cauldron.getPotionColor();
            }
            return level != null && pos != null ? BiomeColors.getAverageWaterColor(level, pos) : -1;
        }, ModBlocks.POTION_CAULDRON.get());

        event.register((state, level, pos, tintIndex) -> {
            if (level != null && pos != null) {
                BlockState disguise = CamouflageHelper.disguiseState(level, pos);
                if (!disguise.is(ModBlocks.CAMOUFLAGED_BLOCK.get())) {
                    return Minecraft.getInstance().getBlockColors().getColor(disguise, level, pos, tintIndex);
                }
            }
            return -1;
        }, ModBlocks.CAMOUFLAGED_BLOCK.get());
    }

    private static void registerRenderBuffers(RegisterRenderBuffersEvent event) {
        event.registerRenderBuffer(AuricGlintRenderTypes.glint());
        event.registerRenderBuffer(AuricGlintRenderTypes.glintTranslucent());
        event.registerRenderBuffer(AuricGlintRenderTypes.entityGlint());
        event.registerRenderBuffer(AuricGlintRenderTypes.entityGlintDirect());
        event.registerRenderBuffer(AuricGlintRenderTypes.layeredToolGlint());
        event.registerRenderBuffer(AuricGlintRenderTypes.layeredToolGlintTranslucent());
        event.registerRenderBuffer(AuricGlintRenderTypes.layeredToolEntityGlint());
        event.registerRenderBuffer(AuricGlintRenderTypes.layeredToolEntityGlintDirect());
    }

    private static void modifyBakedModels(ModelEvent.ModifyBakingResult event) {
        ModelResourceLocation camoModel = BlockModelShaper.stateToModelLocation(ResourceLocation.fromNamespaceAndPath(Auric.MOD_ID, ModBlocks.CAMOUFLAGED_BLOCK_ID), ModBlocks.CAMOUFLAGED_BLOCK.get().defaultBlockState());
        BakedModel original = event.getModels().get(camoModel);
        if (original != null) {
            event.getModels().put(camoModel, new CamouflagedBlockModel(original, event.getModels()));
        }

        event.getModels().replaceAll((location, model) -> {
            if (ModelResourceLocation.INVENTORY_VARIANT.equals(location.variant()) || model instanceof CamoSkinBakedModel) {
                return model;
            }
            return new CamoSkinBakedModel(model, event.getModels());
        });
    }
}
