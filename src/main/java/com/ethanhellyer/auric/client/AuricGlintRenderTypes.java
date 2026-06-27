package com.ethanhellyer.auric.client;

import com.ethanhellyer.auric.Auric;
import com.ethanhellyer.auric.registry.ModDataComponents;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.SheetedDecalTextureGenerator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexMultiConsumer;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Matrix4f;

@OnlyIn(Dist.CLIENT)
public final class AuricGlintRenderTypes {
    private static final ResourceLocation AURIC_GLINT_ITEM = Auric.id("textures/misc/auric_glint_item.png");
    private static final ResourceLocation AURIC_GLINT_ENTITY = Auric.id("textures/misc/auric_glint_entity.png");
    private static final RenderStateShard.TexturingStateShard AURIC_GLINT_TEXTURING = new RenderStateShard.TexturingStateShard(
            "auric_glint_texturing",
            () -> setupAuricGlintTexturing(8.0F),
            RenderSystem::resetTextureMatrix
    );
    private static final RenderStateShard.TexturingStateShard AURIC_ENTITY_GLINT_TEXTURING = new RenderStateShard.TexturingStateShard(
            "auric_entity_glint_texturing",
            () -> setupAuricGlintTexturing(0.16F),
            RenderSystem::resetTextureMatrix
    );

    private static final RenderType AURIC_GLINT_TRANSLUCENT = RenderType.create(
            "auric_glint_translucent",
            DefaultVertexFormat.POSITION_TEX,
            VertexFormat.Mode.QUADS,
            1536,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.RENDERTYPE_GLINT_TRANSLUCENT_SHADER)
                    .setTextureState(new RenderStateShard.TextureStateShard(AURIC_GLINT_ITEM, true, false))
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .setCullState(RenderStateShard.NO_CULL)
                    .setDepthTestState(RenderStateShard.EQUAL_DEPTH_TEST)
                    .setTransparencyState(RenderStateShard.GLINT_TRANSPARENCY)
                    .setTexturingState(AURIC_GLINT_TEXTURING)
                    .setOutputState(RenderStateShard.ITEM_ENTITY_TARGET)
                    .createCompositeState(false)
    );
    private static final RenderType AURIC_GLINT = RenderType.create(
            "auric_glint",
            DefaultVertexFormat.POSITION_TEX,
            VertexFormat.Mode.QUADS,
            1536,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.RENDERTYPE_GLINT_SHADER)
                    .setTextureState(new RenderStateShard.TextureStateShard(AURIC_GLINT_ITEM, true, false))
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .setCullState(RenderStateShard.NO_CULL)
                    .setDepthTestState(RenderStateShard.EQUAL_DEPTH_TEST)
                    .setTransparencyState(RenderStateShard.GLINT_TRANSPARENCY)
                    .setTexturingState(AURIC_GLINT_TEXTURING)
                    .createCompositeState(false)
    );
    private static final RenderType AURIC_ENTITY_GLINT = RenderType.create(
            "auric_entity_glint",
            DefaultVertexFormat.POSITION_TEX,
            VertexFormat.Mode.QUADS,
            1536,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.RENDERTYPE_ENTITY_GLINT_SHADER)
                    .setTextureState(new RenderStateShard.TextureStateShard(AURIC_GLINT_ENTITY, true, false))
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .setCullState(RenderStateShard.NO_CULL)
                    .setDepthTestState(RenderStateShard.EQUAL_DEPTH_TEST)
                    .setTransparencyState(RenderStateShard.GLINT_TRANSPARENCY)
                    .setOutputState(RenderStateShard.ITEM_ENTITY_TARGET)
                    .setTexturingState(AURIC_ENTITY_GLINT_TEXTURING)
                    .createCompositeState(false)
    );
    private static final RenderType AURIC_ENTITY_GLINT_DIRECT = RenderType.create(
            "auric_entity_glint_direct",
            DefaultVertexFormat.POSITION_TEX,
            VertexFormat.Mode.QUADS,
            1536,
            RenderType.CompositeState.builder()
                    .setShaderState(RenderStateShard.RENDERTYPE_ENTITY_GLINT_DIRECT_SHADER)
                    .setTextureState(new RenderStateShard.TextureStateShard(AURIC_GLINT_ENTITY, true, false))
                    .setWriteMaskState(RenderStateShard.COLOR_WRITE)
                    .setCullState(RenderStateShard.NO_CULL)
                    .setDepthTestState(RenderStateShard.EQUAL_DEPTH_TEST)
                    .setTransparencyState(RenderStateShard.GLINT_TRANSPARENCY)
                    .setTexturingState(AURIC_ENTITY_GLINT_TEXTURING)
                    .createCompositeState(false)
    );

    private AuricGlintRenderTypes() {
    }

    public static RenderType glintTranslucent() {
        return AURIC_GLINT_TRANSLUCENT;
    }

    public static RenderType glint() {
        return AURIC_GLINT;
    }

    public static RenderType entityGlint() {
        return AURIC_ENTITY_GLINT;
    }

    public static RenderType entityGlintDirect() {
        return AURIC_ENTITY_GLINT_DIRECT;
    }

    public static VertexConsumer appendCompassBuffer(
            MultiBufferSource bufferSource,
            PoseStack.Pose pose,
            VertexConsumer vanillaConsumer,
            ItemStack stack
    ) {
        if (!hasAuricGlint(stack)) {
            return vanillaConsumer;
        }

        return VertexMultiConsumer.create(
                new SheetedDecalTextureGenerator(bufferSource.getBuffer(glint()), pose, 0.0078125F),
                vanillaConsumer
        );
    }

    public static VertexConsumer appendDirectBuffer(
            MultiBufferSource bufferSource,
            boolean isItem,
            VertexConsumer vanillaConsumer,
            ItemStack stack
    ) {
        if (!hasAuricGlint(stack)) {
            return vanillaConsumer;
        }

        return VertexMultiConsumer.create(bufferSource.getBuffer(isItem ? glint() : entityGlintDirect()), vanillaConsumer);
    }

    public static VertexConsumer appendBuffer(
            MultiBufferSource bufferSource,
            RenderType renderType,
            boolean isItem,
            VertexConsumer vanillaConsumer,
            ItemStack stack
    ) {
        if (!hasAuricGlint(stack)) {
            return vanillaConsumer;
        }

        RenderType auricGlint = Minecraft.useShaderTransparency() && renderType == Sheets.translucentItemSheet()
                ? glintTranslucent()
                : isItem ? glint() : entityGlint();
        return VertexMultiConsumer.create(bufferSource.getBuffer(auricGlint), vanillaConsumer);
    }

    private static boolean hasAuricGlint(ItemStack stack) {
        return !stack.isEmpty() && stack.has(ModDataComponents.IMBUE.get());
    }

    private static void setupAuricGlintTexturing(float scale) {
        long time = (long) ((double) Util.getMillis() * Minecraft.getInstance().options.glintSpeed().get() * 8.0);
        float xOffset = (float) (time % 90000L) / 90000.0F;
        float yOffset = (float) (time % 47000L) / 47000.0F;
        Matrix4f matrix = new Matrix4f().translation(0.35F + xOffset, 0.15F - yOffset, 0.0F);
        matrix.rotateZ((float) (-Math.PI / 14.0)).scale(scale);
        RenderSystem.setTextureMatrix(matrix);
    }
}
