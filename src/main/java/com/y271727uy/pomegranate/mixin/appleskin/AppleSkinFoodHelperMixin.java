package com.y271727uy.pomegranate.mixin.appleskin;

import com.y271727uy.pomegranate.client.PomegranateClientData;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets = "squeek.appleskin.helpers.FoodHelper", remap = false)
public abstract class AppleSkinFoodHelperMixin {
    @Inject(method = "canConsume", at = @At("RETURN"), cancellable = true)
    private static void spiceOfLifePomegranateEdition$allowConfiguredFoodWhenFull(ItemStack stack, Player player, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ() && stack.isEdible() && PomegranateClientData.canEatWhenFull(stack.getItem())) {
            cir.setReturnValue(true);
        }
    }
}
