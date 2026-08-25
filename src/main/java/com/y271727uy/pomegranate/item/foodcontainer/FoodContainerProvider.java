package com.y271727uy.pomegranate.item.foodcontainer;

import com.y271727uy.pomegranate.lib.Localization;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

import javax.annotation.Nullable;

final class FoodContainerProvider implements MenuProvider {
    private final String displayName;

    FoodContainerProvider(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(Localization.keyString("gui", "food_container." + displayName));
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new FoodContainerMenu(id, inventory, player);
    }
}
