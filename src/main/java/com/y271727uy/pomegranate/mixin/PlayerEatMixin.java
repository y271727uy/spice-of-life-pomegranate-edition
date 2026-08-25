package com.y271727uy.pomegranate.mixin;

import com.y271727uy.pomegranate.PomegranateConfig;
import com.y271727uy.pomegranate.PomegranateFoodLogic;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Player.class)
public abstract class PlayerEatMixin {
	@Redirect(
		method = "eat",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/world/food/FoodData;eat(Lnet/minecraft/world/item/Item;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/LivingEntity;)V",
			remap = false
		)
	)
	private void spiceOfLifePomegranateEdition$modifyFoodData(FoodData foodData, Item item, ItemStack stack, LivingEntity entity) {
		Player player = (Player) (Object) this;
		if (player instanceof ServerPlayer serverPlayer
			&& PomegranateConfig.enablePunishments()
			&& PomegranateFoodLogic.shouldModifyFood(stack)
			&& !(PomegranateConfig.limitProgressionToSurvival() && player.isCreative())) {
			PomegranateFoodLogic.CustomFoodData data = PomegranateFoodLogic.getCustomFoodData(serverPlayer, stack);
			foodData.eat(data.getNutrition(), data.getSaturation());
			return;
		}

		foodData.eat(item, stack, entity);
	}
}
