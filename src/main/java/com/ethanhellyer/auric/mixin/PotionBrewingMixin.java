package com.ethanhellyer.auric.mixin;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionBrewing;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(PotionBrewing.class)
public abstract class PotionBrewingMixin {
    @Redirect(
            method = "addVanillaMixes",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/alchemy/PotionBrewing$Builder;addContainerRecipe(Lnet/minecraft/world/item/Item;Lnet/minecraft/world/item/Item;Lnet/minecraft/world/item/Item;)V",
                    ordinal = 1
            )
    )
    private static void auric$replaceDragonBreathLingeringRecipe(PotionBrewing.Builder builder, Item input, Item ingredient, Item output) {
        builder.addContainerRecipe(input, Items.ECHO_SHARD, output);
    }
}
