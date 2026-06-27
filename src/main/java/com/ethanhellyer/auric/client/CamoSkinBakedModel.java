package com.ethanhellyer.auric.client;

import com.ethanhellyer.auric.camo.CamouflageModelProperties;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.IDynamicBakedModel;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.common.util.TriState;
import org.jetbrains.annotations.Nullable;

public class CamoSkinBakedModel extends BakedModelWrapper<BakedModel> implements IDynamicBakedModel {
    private static final int VERTEX_STRIDE = 8;
    private static final int VERTEX_COUNT = 4;
    private static final int U_OFFSET = 4;
    private static final int V_OFFSET = 5;

    private final Map<ModelResourceLocation, BakedModel> models;

    public CamoSkinBakedModel(BakedModel originalModel, Map<ModelResourceLocation, BakedModel> models) {
        super(originalModel);
        this.models = models;
    }

    @Override
    public ModelData getModelData(BlockAndTintGetter level, BlockPos pos, BlockState state, ModelData modelData) {
        ModelData data = originalModel.getModelData(level, pos, state, modelData);
        BlockState disguise = ClientCamoSkins.get(level, pos);
        if (disguise == null || disguise.isAir()) {
            return data;
        }
        return data.derive()
                .with(CamouflageModelProperties.CAMO_SKIN_STATE, disguise)
                .build();
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand, ModelData extraData, @Nullable RenderType renderType) {
        BlockState disguise = extraData.get(CamouflageModelProperties.CAMO_SKIN_STATE);
        if (state == null || disguise == null || disguise.isAir()) {
            return originalModel.getQuads(state, side, rand, extraData, renderType);
        }

        List<BakedQuad> originalQuads = originalModel.getQuads(state, side, rand, extraData, renderType);
        if (originalQuads.isEmpty()) {
            return originalQuads;
        }

        List<BakedQuad> camouflagedQuads = new ArrayList<>(originalQuads.size());
        for (BakedQuad quad : originalQuads) {
            TextureAtlasSprite sprite = spriteFor(disguise, quad.getDirection(), renderType);
            camouflagedQuads.add(remapSprite(quad, sprite));
        }
        return camouflagedQuads;
    }

    @Override
    public TriState useAmbientOcclusion(BlockState state, ModelData data, RenderType renderType) {
        return originalModel.useAmbientOcclusion(state, data, renderType);
    }

    @Override
    public TextureAtlasSprite getParticleIcon(ModelData data) {
        BlockState disguise = data.get(CamouflageModelProperties.CAMO_SKIN_STATE);
        return disguise == null || disguise.isAir()
                ? originalModel.getParticleIcon(data)
                : modelFor(disguise).getParticleIcon(ModelData.EMPTY);
    }

    @Override
    public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource rand, ModelData data) {
        return originalModel.getRenderTypes(state, rand, data);
    }

    private BakedModel modelFor(BlockState state) {
        return models.getOrDefault(BlockModelShaper.stateToModelLocation(state), originalModel);
    }

    private TextureAtlasSprite spriteFor(BlockState disguise, Direction direction, @Nullable RenderType renderType) {
        BakedModel disguiseModel = modelFor(disguise);
        TextureAtlasSprite sprite = firstSprite(disguiseModel, disguise, direction, renderType);
        if (sprite != null) {
            return sprite;
        }

        for (Direction fallbackDirection : Direction.values()) {
            sprite = firstSprite(disguiseModel, disguise, fallbackDirection, renderType);
            if (sprite != null) {
                return sprite;
            }
        }

        sprite = firstSprite(disguiseModel, disguise, null, renderType);
        return sprite == null ? disguiseModel.getParticleIcon(ModelData.EMPTY) : sprite;
    }

    @Nullable
    private static TextureAtlasSprite firstSprite(BakedModel model, BlockState state, @Nullable Direction side, @Nullable RenderType renderType) {
        List<BakedQuad> quads = model.getQuads(state, side, RandomSource.create(42L), ModelData.EMPTY, renderType);
        return quads.isEmpty() ? null : quads.getFirst().getSprite();
    }

    private static BakedQuad remapSprite(BakedQuad quad, TextureAtlasSprite sprite) {
        int[] vertices = quad.getVertices().clone();
        if (vertices.length >= VERTEX_STRIDE * VERTEX_COUNT) {
            TextureAtlasSprite originalSprite = quad.getSprite();
            for (int vertex = 0; vertex < VERTEX_COUNT; vertex++) {
                int vertexStart = vertex * VERTEX_STRIDE;
                float u = Float.intBitsToFloat(vertices[vertexStart + U_OFFSET]);
                float v = Float.intBitsToFloat(vertices[vertexStart + V_OFFSET]);
                float localU = originalSprite.getUOffset(u);
                float localV = originalSprite.getVOffset(v);
                vertices[vertexStart + U_OFFSET] = Float.floatToRawIntBits(sprite.getU(localU));
                vertices[vertexStart + V_OFFSET] = Float.floatToRawIntBits(sprite.getV(localV));
            }
        }
        return new BakedQuad(vertices, -1, quad.getDirection(), sprite, quad.isShade(), quad.hasAmbientOcclusion());
    }
}
