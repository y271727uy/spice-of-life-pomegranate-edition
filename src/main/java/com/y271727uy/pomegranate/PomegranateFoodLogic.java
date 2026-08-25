package com.y271727uy.pomegranate;

import com.y271727uy.pomegranate.client.PomegranateClientData;
import com.y271727uy.pomegranate.communication.PomegranateDataMessage;
import com.y271727uy.pomegranate.data.PomegranateData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.tags.TagKey;
import net.minecraft.world.food.FoodProperties;
import net.minecraftforge.network.NetworkDirection;

/**
 * Server-side logic for modifying food nutrition/saturation based on repeated consumption.
 */
public final class PomegranateFoodLogic {
    private PomegranateFoodLogic() {}

    public static class CustomFoodData {
        private final int nutrition;
        private final float saturation;

        public CustomFoodData(int nutrition, float saturation) {
            this.nutrition = nutrition;
            this.saturation = saturation;
        }

        public int getNutrition() { return nutrition; }
        public float getSaturation() { return saturation; }
    }

    public static boolean shouldModifyFood(ItemStack stack) {
        if (stack == null) return false;
        if (!stack.isEdible()) return false;
        return PomegranateConfig.enablePunishments() || PomegranateConfig.enableRewards();
    }

    public static CustomFoodData getCustomFoodData(ServerPlayer player, ItemStack stack) {
        // Use the classic-style history/decay calculation
        int customNutrition = PomegranateFoodHandler.computeCustomNutrition(player, stack);
        float customSaturation = PomegranateFoodHandler.computeCustomSaturation(player, stack);

        // Update counts and history
        PomegranateFoodHandler.addFoodToHistory(player, PomegranateFoodHandler.loadFoodHistory(player), stack);
        int totalCount = PomegranateFoodHandler.incrementAndGetCount(player, stack.getItem());

        // Overweight/staple/produce bookkeeping using TagKey checks
        for (TagKey<Item> key : PomegranateConfig.getStapleTagKeys()) {
            if (stack.is(key)) {
                PomegranateData.incrementStapleCount(player);
                break;
            }
        }
        for (TagKey<Item> key : PomegranateConfig.getProduceTagKeys()) {
            if (stack.is(key)) {
                PomegranateData.incrementProduceCount(player);
                break;
            }
        }

        // If overweight conditions met, apply HUNGER
        if (PomegranateConfig.enableOverweight()) {
            int stapleCount = PomegranateData.getStapleCount(player);
            Level level = player.level();
            if (stapleCount >= PomegranateConfig.getOverweightStapleThreshold() && level != null && !level.isClientSide()) {
                player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 20 * 6, 0));
            }
            int produceCount = PomegranateData.getProduceCount(player);
            if (produceCount >= PomegranateConfig.getOverweightProduceResetThreshold()) {
                PomegranateData.resetOverweight(player);
            }
        }

        // Repeated-food punishment tiers. The two reduction tiers use the food's
        // original values, rather than compounding the short-history decay.
        if (totalCount >= 14) {
            // count >= 14 -> restore becomes 0
            customNutrition = 0;
            customSaturation = 0f;
        } else if (totalCount >= 11) {
            // counts 11-13 -> halve the prior tier: 25% of the original values
            FoodProperties food = stack.getItem().getFoodProperties(stack, player);
            customNutrition = food == null ? 0 : (int) Math.floor(food.getNutrition() * 0.25);
            customSaturation = food == null ? 0f : food.getSaturationModifier() * 0.25f;
        } else if (totalCount >= 8) {
            // counts 8-10 -> 50% of the original values
            FoodProperties food = stack.getItem().getFoodProperties(stack, player);
            customNutrition = food == null ? 0 : (int) Math.floor(food.getNutrition() * 0.5);
            customSaturation = food == null ? 0f : food.getSaturationModifier() * 0.5f;
        }

        // Apply punitive status effects after nutrition has reached zero.
        if (!player.level().isClientSide()) {
            // check highest tiers first
            if (totalCount >= 33) { // nausea 60s level V
                player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 60 * 20, 4));
            } else if (totalCount >= 23) { // mining fatigue + weakness 60s level V
                player.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, 60 * 20, 4));
                player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60 * 20, 4));
            } else if (totalCount >= 18) { // hunger, nausea, weakness 20s level III
                player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 20 * 20, 2));
                player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 20 * 20, 2));
                player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 20 * 20, 2));
            } else if (totalCount >= 16) { // hunger + nausea 10s level II
                player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 10 * 20, 1));
                player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 10 * 20, 1));
            } else if (totalCount >= 14) { // hunger 5s level I
                player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 5 * 20, 0));
            }
        }

        // Sync updated counts/history/config to the client so client-only integrations (AppleSkin, tooltips) stay correct.
        if (player.connection != null && player.connection.connection != null) {
            SOLCarrot.channel.sendTo(
                new PomegranateDataMessage(player),
                player.connection.connection,
                NetworkDirection.PLAY_TO_CLIENT
            );
        }

        return new CustomFoodData(customNutrition, customSaturation);
    }

    /**
     * Client-safe computation of modified food values for display integrations (e.g., AppleSkin).
     * This method has no side effects: it does not mutate counts, history, or apply effects.
     */
    public static CustomFoodData getCustomFoodDataForDisplay(net.minecraft.world.entity.player.Player player, ItemStack stack) {
        if (player == null) return null;
        if (stack == null || !stack.isEdible()) return null;
        if (!shouldModifyFood(stack)) return null;

        FoodProperties food = stack.getItem().getFoodProperties(stack, player);
        if (food == null) return null;

        int sameFoodLong;
        int sameFoodShort;
        int totalCount;

        if (player instanceof ServerPlayer serverPlayer) {
            sameFoodLong = PomegranateFoodHandler.getTimesEatenLong(serverPlayer, stack);
            sameFoodShort = PomegranateFoodHandler.getTimesEatenShort(serverPlayer, stack);
            totalCount = PomegranateFoodHandler.getCount(serverPlayer, stack.getItem());
        } else {
            sameFoodLong = PomegranateClientData.getTimesEatenLong(stack.getItem());
            sameFoodShort = PomegranateClientData.getTimesEatenShort(stack.getItem());
            totalCount = PomegranateClientData.getFoodCount(stack.getItem());
        }

        // mimic server behavior: tiers are based on count AFTER this consumption
        int totalAfterEat = totalCount + 1;

        // decay-based modifier (classic)
        java.util.List<Double> decay = PomegranateClientData.getFoodDecayModifiers();
        double shortModifier = PomegranateClientData.getShortFoodDecayModifier();
        double modifier = 1.0;
        if (decay != null && !decay.isEmpty()) {
            if (sameFoodShort < decay.size()) {
                modifier = decay.get(sameFoodShort);
            } else {
                modifier = decay.get(Math.max(0, decay.size() - 1));
            }
            modifier = modifier - shortModifier * sameFoodLong;
            if (modifier < 0.0) modifier = 0.0;
        }

        int customNutrition = Math.max(1, (int) (food.getNutrition() * modifier));
        float customSaturation = Math.max(0.01f, food.getSaturationModifier());

        // Keep client-side previews aligned with the server's repeated-food tiers.
        if (totalAfterEat >= 14) {
            customNutrition = 0;
            customSaturation = 0f;
        } else if (totalAfterEat >= 11) {
            customNutrition = (int) Math.floor(food.getNutrition() * 0.25);
            customSaturation = food.getSaturationModifier() * 0.25f;
        } else if (totalAfterEat >= 8) {
            customNutrition = (int) Math.floor(food.getNutrition() * 0.5);
            customSaturation = food.getSaturationModifier() * 0.5f;
        }

        return new CustomFoodData(customNutrition, customSaturation);
    }
}
