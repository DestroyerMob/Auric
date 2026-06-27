package com.ethanhellyer.auric.client;

import com.ethanhellyer.auric.camo.CamouflageModelProperties;
import com.ethanhellyer.auric.registry.ModBlocks;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BlockElement;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.block.model.BlockFaceUV;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.BlockModelRotation;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.BakedModelWrapper;
import net.neoforged.neoforge.client.model.IDynamicBakedModel;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.common.util.TriState;
import org.joml.Vector3f;
import org.jetbrains.annotations.Nullable;

public class CamouflagedBlockModel extends BakedModelWrapper<BakedModel> implements IDynamicBakedModel {
    private final Map<ModelResourceLocation, BakedModel> models;

    public CamouflagedBlockModel(BakedModel originalModel, Map<ModelResourceLocation, BakedModel> models) {
        super(originalModel);
        this.models = models;
    }

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand, ModelData extraData, @Nullable RenderType renderType) {
        BlockState real = real(extraData);
        BlockState disguise = disguise(extraData);
        if (real.getBlock() instanceof DoorBlock) {
            return camouflagedDoorQuads(real, disguise, side);
        }
        return modelFor(disguise).getQuads(disguise, side, rand, ModelData.EMPTY, renderType);
    }

    @Override
    public TriState useAmbientOcclusion(BlockState state, ModelData data, RenderType renderType) {
        if (real(data).getBlock() instanceof DoorBlock) {
            return TriState.TRUE;
        }
        BlockState disguise = disguise(data);
        return modelFor(disguise).useAmbientOcclusion(disguise, ModelData.EMPTY, renderType);
    }

    @Override
    public TextureAtlasSprite getParticleIcon(ModelData data) {
        BlockState disguise = disguise(data);
        return modelFor(disguise).getParticleIcon(ModelData.EMPTY);
    }

    @Override
    public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource rand, ModelData data) {
        BlockState disguise = disguise(data);
        return modelFor(disguise).getRenderTypes(disguise, rand, ModelData.EMPTY);
    }

    private BakedModel modelFor(BlockState disguise) {
        if (disguise.is(ModBlocks.CAMOUFLAGED_BLOCK.get())) {
            return originalModel;
        }
        return models.getOrDefault(BlockModelShaper.stateToModelLocation(disguise), originalModel);
    }

    private List<BakedQuad> camouflagedDoorQuads(BlockState real, BlockState disguise, @Nullable Direction side) {
        if (side != null) {
            return List.of();
        }

        TextureAtlasSprite sprite = modelFor(disguise).getParticleIcon(ModelData.EMPTY);
        AABB bounds = real.getShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO).bounds();
        BlockElement element = new BlockElement(
                new Vector3f((float) bounds.minX * 16.0F, (float) bounds.minY * 16.0F, (float) bounds.minZ * 16.0F),
                new Vector3f((float) bounds.maxX * 16.0F, (float) bounds.maxY * 16.0F, (float) bounds.maxZ * 16.0F),
                faces(),
                null,
                true);
        List<BakedQuad> quads = new ArrayList<>();
        for (Direction direction : Direction.values()) {
            quads.add(BlockModel.bakeFace(element, element.faces.get(direction), sprite, direction, BlockModelRotation.X0_Y0));
        }
        return quads;
    }

    private static Map<Direction, BlockElementFace> faces() {
        Map<Direction, BlockElementFace> faces = new EnumMap<>(Direction.class);
        for (Direction direction : Direction.values()) {
            faces.put(direction, new BlockElementFace(null, 0, "#camo", new BlockFaceUV(null, 0)));
        }
        return faces;
    }

    private static BlockState disguise(ModelData data) {
        BlockState disguise = data.get(CamouflageModelProperties.DISGUISE_STATE);
        return disguise == null || disguise.is(ModBlocks.CAMOUFLAGED_BLOCK.get())
                ? Blocks.STONE.defaultBlockState()
                : disguise;
    }

    private static BlockState real(ModelData data) {
        BlockState real = data.get(CamouflageModelProperties.REAL_STATE);
        return real == null || real.is(ModBlocks.CAMOUFLAGED_BLOCK.get())
                ? Blocks.STONE.defaultBlockState()
                : real;
    }
}
