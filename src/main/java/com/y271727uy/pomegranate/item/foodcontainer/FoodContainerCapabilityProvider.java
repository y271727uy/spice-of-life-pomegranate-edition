package com.y271727uy.pomegranate.item.foodcontainer;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.ItemStackHandler;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

final class FoodContainerCapabilityProvider implements ICapabilitySerializable<CompoundTag> {
    private final LazyOptional<ItemStackHandler> inventory;

    FoodContainerCapabilityProvider(int slots) {
        inventory = LazyOptional.of(() -> new ItemStackHandler(slots) {
            @Override
            public boolean isItemValid(int slot, @Nonnull net.minecraft.world.item.ItemStack stack) {
                return !(stack.getItem() instanceof FoodContainerItem) && stack.isEdible();
            }
        });
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> capability, @Nullable Direction side) {
        return capability == ForgeCapabilities.ITEM_HANDLER ? inventory.cast() : LazyOptional.empty();
    }

    @Override
    public CompoundTag serializeNBT() {
        return inventory.resolve().map(ItemStackHandler::serializeNBT).orElseGet(CompoundTag::new);
    }

    @Override
    public void deserializeNBT(CompoundTag nbt) {
        inventory.ifPresent(handler -> handler.deserializeNBT(nbt));
    }
}
