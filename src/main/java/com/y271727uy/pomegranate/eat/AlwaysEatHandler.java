package com.y271727uy.pomegranate.eat;

import com.y271727uy.pomegranate.PomegranateConfig;
import com.y271727uy.pomegranate.SOLCarrot;
import com.y271727uy.pomegranate.client.PomegranateClientData;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = SOLCarrot.MOD_ID)
public final class AlwaysEatHandler {
    @SubscribeEvent
    public static void onRightClickItem(PlayerInteractEvent.RightClickItem event) {
        ItemStack stack = event.getItemStack();
        if (!stack.isEdible()) return;

        Player player = event.getEntity();
        if (player.canEat(false)) return;

        boolean isUneatable = player.level().isClientSide
            ? PomegranateClientData.isUneatableWhenFull(stack.getItem())
            : PomegranateConfig.isUneatableWhenFull(stack.getItem());
        if (isUneatable) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
            return;
        }

        boolean canEat = player.level().isClientSide
            ? PomegranateClientData.canEatWhenFull(stack.getItem())
            : PomegranateConfig.canEatWhenFull(stack.getItem());
        if (!canEat) return;

        player.startUsingItem(event.getHand());
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.CONSUME);
    }

    private AlwaysEatHandler() {}
}
