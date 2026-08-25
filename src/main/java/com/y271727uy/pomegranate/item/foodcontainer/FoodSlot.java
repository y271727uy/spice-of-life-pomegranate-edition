package com.y271727uy.pomegranate.item.foodcontainer;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;

import javax.annotation.Nonnull;

final class FoodSlot extends SlotItemHandler {
    FoodSlot(IItemHandler itemHandler, int index, int x, int y) {
        super(itemHandler, index, x, y);
    }

    @Override
    public boolean mayPlace(@Nonnull ItemStack stack) {
        return stack.isEdible() && super.mayPlace(stack);
    }
}
