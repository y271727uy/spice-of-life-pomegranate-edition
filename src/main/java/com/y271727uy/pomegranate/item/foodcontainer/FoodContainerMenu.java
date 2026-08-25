package com.y271727uy.pomegranate.item.foodcontainer;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;

public final class FoodContainerMenu extends AbstractContainerMenu {
    private static final int PLAYER_SLOT_COUNT = 36;

    public final ItemStack containerItem;
    private final int containerSlotCount;
    private final Inventory playerInventory;

    public FoodContainerMenu(int id, Inventory playerInventory, Player player) {
        super(FoodContainerMenus.FOOD_CONTAINER.get(), id);
        this.playerInventory = playerInventory;
        this.containerItem = findContainer(playerInventory, player);

        final int[] slots = {0};
        containerItem.getCapability(ForgeCapabilities.ITEM_HANDLER).ifPresent(handler -> {
            slots[0] = handler.getSlots();
            int slotsPerRow = handler.getSlots() > 9 ? (handler.getSlots() + 1) / 2 : handler.getSlots();
            int xStart = (176 - slotsPerRow * 18) / 2;
            int yStart = handler.getSlots() > 9 ? 29 : 35;
            for (int index = 0; index < handler.getSlots(); index++) {
                int row = index / slotsPerRow;
                int column = index % slotsPerRow;
                addSlot(new FoodSlot(handler, index, xStart + column * 18, yStart + row * 18));
            }
        });
        containerSlotCount = slots[0];
        layoutPlayerInventorySlots(8, 84);
    }

    private static ItemStack findContainer(Inventory inventory, Player player) {
        if (player.getMainHandItem().getItem() instanceof FoodContainerItem) return player.getMainHandItem();
        if (player.getOffhandItem().getItem() instanceof FoodContainerItem) return player.getOffhandItem();
        for (ItemStack stack : inventory.items) {
            if (stack.getItem() instanceof FoodContainerItem) return stack;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public void clicked(int slotId, int dragType, ClickType clickType, Player player) {
        if (slotId >= 0 && slotId < slots.size() && slots.get(slotId).getItem().getItem() instanceof FoodContainerItem) {
            return;
        }
        super.clicked(slotId, dragType, clickType, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotId) {
        if (slotId < 0 || slotId >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(slotId);
        if (!slot.hasItem()) return ItemStack.EMPTY;

        ItemStack stack = slot.getItem();
        if (!stack.isEdible()) return ItemStack.EMPTY;
        ItemStack copy = stack.copy();
        if (slotId < containerSlotCount) {
            if (!moveItemStackTo(stack, containerSlotCount, containerSlotCount + PLAYER_SLOT_COUNT, false)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, 0, containerSlotCount, false)) {
            return ItemStack.EMPTY;
        }

        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        if (stack.getCount() == copy.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, stack);
        return copy;
    }

    private void layoutPlayerInventorySlots(int left, int top) {
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(playerInventory, column + row * 9 + 9, left + column * 18, top + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(playerInventory, column, left + column * 18, top + 58));
        }
    }

    @Override
    public boolean stillValid(Player player) {
        return !containerItem.isEmpty();
    }

    public int getContainerSlotCount() {
        return containerSlotCount;
    }
}
