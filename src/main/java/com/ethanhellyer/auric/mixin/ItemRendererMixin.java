package com.ethanhellyer.auric.mixin;

import com.ethanhellyer.auric.client.AuricGlintRenderTypes;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayDeque;
import java.util.Deque;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemRenderer.class)
public abstract class ItemRendererMixin {
    private static final ThreadLocal<Deque<ItemStack>> AURIC_RENDERING_STACKS = ThreadLocal.withInitial(ArrayDeque::new);

    @Inject(method = "render", at = @At("HEAD"))
    private void auric$pushRenderedStack(
            ItemStack stack,
            ItemDisplayContext displayContext,
            boolean leftHand,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay,
            BakedModel model,
            CallbackInfo callbackInfo
    ) {
        AURIC_RENDERING_STACKS.get().push(stack);
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void auric$popRenderedStack(
            ItemStack stack,
            ItemDisplayContext displayContext,
            boolean leftHand,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight,
            int packedOverlay,
            BakedModel model,
            CallbackInfo callbackInfo
    ) {
        Deque<ItemStack> stacks = AURIC_RENDERING_STACKS.get();
        if (!stacks.isEmpty()) {
            stacks.pop();
        }
        if (stacks.isEmpty()) {
            AURIC_RENDERING_STACKS.remove();
        }
    }

    @Redirect(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/entity/ItemRenderer;getCompassFoilBuffer(Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/renderer/RenderType;Lcom/mojang/blaze3d/vertex/PoseStack$Pose;)Lcom/mojang/blaze3d/vertex/VertexConsumer;"
            )
    )
    private VertexConsumer auric$getCompassFoilBuffer(MultiBufferSource bufferSource, RenderType renderType, PoseStack.Pose pose) {
        VertexConsumer vanillaConsumer = ItemRenderer.getCompassFoilBuffer(bufferSource, renderType, pose);
        return AuricGlintRenderTypes.appendCompassBuffer(bufferSource, pose, vanillaConsumer, auric$currentStack());
    }

    @Redirect(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/entity/ItemRenderer;getFoilBufferDirect(Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/renderer/RenderType;ZZ)Lcom/mojang/blaze3d/vertex/VertexConsumer;"
            )
    )
    private VertexConsumer auric$getFoilBufferDirect(MultiBufferSource bufferSource, RenderType renderType, boolean isItem, boolean hasFoil) {
        VertexConsumer vanillaConsumer = ItemRenderer.getFoilBufferDirect(bufferSource, renderType, isItem, hasFoil);
        return AuricGlintRenderTypes.appendDirectBuffer(bufferSource, isItem, vanillaConsumer, auric$currentStack());
    }

    @Redirect(
            method = "render",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/entity/ItemRenderer;getFoilBuffer(Lnet/minecraft/client/renderer/MultiBufferSource;Lnet/minecraft/client/renderer/RenderType;ZZ)Lcom/mojang/blaze3d/vertex/VertexConsumer;"
            )
    )
    private VertexConsumer auric$getFoilBuffer(MultiBufferSource bufferSource, RenderType renderType, boolean isItem, boolean hasFoil) {
        VertexConsumer vanillaConsumer = ItemRenderer.getFoilBuffer(bufferSource, renderType, isItem, hasFoil);
        return AuricGlintRenderTypes.appendBuffer(bufferSource, renderType, isItem, vanillaConsumer, auric$currentStack());
    }

    private static ItemStack auric$currentStack() {
        Deque<ItemStack> stacks = AURIC_RENDERING_STACKS.get();
        return stacks.isEmpty() ? ItemStack.EMPTY : stacks.peek();
    }
}
