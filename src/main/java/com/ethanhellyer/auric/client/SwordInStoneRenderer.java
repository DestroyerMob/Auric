package com.ethanhellyer.auric.client;

import com.ethanhellyer.auric.block.SwordInStoneBlock;
import com.ethanhellyer.auric.blockentity.SwordInStoneBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

public class SwordInStoneRenderer implements BlockEntityRenderer<SwordInStoneBlockEntity> {
    private final ItemRenderer itemRenderer;

    public SwordInStoneRenderer(BlockEntityRendererProvider.Context context) {
        this.itemRenderer = context.getItemRenderer();
    }

    @Override
    public void render(SwordInStoneBlockEntity shrine, float partialTick, PoseStack poseStack, MultiBufferSource buffer, int packedLight, int packedOverlay) {
        ItemStack sword = shrine.getSword();
        if (sword.isEmpty()) {
            return;
        }

        BlockState state = shrine.getBlockState();
        if (state.hasProperty(SwordInStoneBlock.HAS_SWORD) && !state.getValue(SwordInStoneBlock.HAS_SWORD)) {
            return;
        }
        Direction facing = state.hasProperty(SwordInStoneBlock.FACING) ? state.getValue(SwordInStoneBlock.FACING) : Direction.NORTH;

        poseStack.pushPose();
        poseStack.translate(0.5D, 0.92D, 0.5D);
        poseStack.mulPose(Axis.YP.rotationDegrees(facing.toYRot()));
        poseStack.mulPose(Axis.ZP.rotationDegrees(-225.0F));
        poseStack.scale(0.85F, 0.85F, 0.85F);
        itemRenderer.renderStatic(
                sword,
                ItemDisplayContext.FIXED,
                packedLight,
                OverlayTexture.NO_OVERLAY,
                poseStack,
                buffer,
                shrine.getLevel(),
                0
        );
        poseStack.popPose();
    }
}
