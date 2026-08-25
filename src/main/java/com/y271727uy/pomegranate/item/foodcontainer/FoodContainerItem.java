package com.y271727uy.pomegranate.item.foodcontainer;

import com.y271727uy.pomegranate.PomegranateFoodHandler;
import com.y271727uy.pomegranate.PomegranateConfig;
import com.y271727uy.pomegranate.client.PomegranateClientData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.inventory.tooltip.BundleTooltip;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.event.ForgeEventFactory;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.network.NetworkHooks;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.function.Consumer;

public final class FoodContainerItem extends Item {
    private final int slots;
    private final String displayName;

    public FoodContainerItem(int slots, String displayName) {
        super(new Properties().stacksTo(1));
        this.slots = slots;
        this.displayName = displayName;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isCrouching()) {
            if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
                NetworkHooks.openScreen(serverPlayer, new FoodContainerProvider(displayName));
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }
        if (hasFood(stack) && (player.canEat(false) || hasFoodAllowedWhenFull(stack, level.isClientSide))) {
            player.startUsingItem(hand);
            return InteractionResultHolder.consume(stack);
        }
        return InteractionResultHolder.pass(stack);
    }

    @Nullable
    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
        return new FoodContainerCapabilityProvider(slots);
    }

    @Nullable
    public static ItemStackHandler getInventory(ItemStack stack) {
        return stack.getCapability(ForgeCapabilities.ITEM_HANDLER).resolve()
            .filter(ItemStackHandler.class::isInstance)
            .map(ItemStackHandler.class::cast)
            .orElse(null);
    }

    @Override
    public Optional<TooltipComponent> getTooltipImage(ItemStack container) {
        NonNullList<ItemStack> contents = NonNullList.create();
        ItemStackHandler inventory = getInventory(container);
        if (inventory == null) return Optional.empty();

        int occupiedSlots = 0;
        for (int index = 0; index < inventory.getSlots(); index++) {
            ItemStack food = inventory.getStackInSlot(index);
            if (food.isEmpty()) continue;
            contents.add(food.copy());
            occupiedSlots++;
        }
        return Optional.of(new BundleTooltip(contents, occupiedSlots == inventory.getSlots() ? 64 : 0));
    }

    @Override
    public boolean overrideOtherStackedOnMe(ItemStack container, ItemStack food, Slot slot, ClickAction action, Player player, SlotAccess carriedSlotAccess) {
        if (player.isShiftKeyDown() || action != ClickAction.SECONDARY || !slot.allowModification(player) || !food.isEdible()) return false;

        return insertFood(container, food, carriedSlotAccess::set);
    }

    @Override
    public boolean overrideStackedOnOther(ItemStack container, Slot slot, ClickAction action, Player player) {
        if (player.isShiftKeyDown() || action != ClickAction.SECONDARY || !slot.allowModification(player)) return false;

        ItemStack food = slot.getItem();
        if (!food.isEdible()) return false;
        return insertFood(container, food, slot::set);
    }

    private static boolean insertFood(ItemStack container, ItemStack food, Consumer<ItemStack> setRemainder) {
        ItemStackHandler inventory = getInventory(container);
        if (inventory == null) return false;

        // Insert a copy so the internal inventory never aliases the source slot's stack.
        ItemStack remainder = food.copy();
        for (int index = 0; index < inventory.getSlots() && !remainder.isEmpty(); index++) {
            remainder = inventory.insertItem(index, remainder, false);
        }
        if (remainder.getCount() == food.getCount()) return false;

        setRemainder.accept(remainder);
        return true;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack container, Level level, LivingEntity entity) {
        if (!(entity instanceof ServerPlayer player)) return container;
        ItemStackHandler inventory = getInventory(container);
        if (inventory == null) return container;

        int foodSlot = getBestFoodSlot(inventory, player);
        if (foodSlot < 0) return container;

        ItemStack food = inventory.extractItem(foodSlot, 1, false);
        ItemStack eatenCopy = food.copy();
        ItemStack result = food.finishUsingItem(level, player);

        if (!result.isEmpty() && result.getItem() != eatenCopy.getItem()) {
            if (!player.getInventory().add(result)) player.drop(result, false);
        }
        ForgeEventFactory.onItemUseFinish(player, eatenCopy, 0, result);
        return container;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 32;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.EAT;
    }

    public static int getBestFoodSlot(ItemStackHandler inventory, ServerPlayer player) {
        int lowestCount = Integer.MAX_VALUE;
        int bestSlot = -1;
        for (int index = 0; index < inventory.getSlots(); index++) {
            ItemStack food = inventory.getStackInSlot(index);
            if (food.isEmpty() || !food.isEdible()) continue;
            if (!player.canEat(false) && !PomegranateConfig.canEatWhenFull(food.getItem())) continue;
            int count = PomegranateFoodHandler.getCount(player, food.getItem());
            if (count < lowestCount) {
                lowestCount = count;
                bestSlot = index;
            }
        }
        return bestSlot;
    }

    private static boolean hasFood(ItemStack container) {
        ItemStackHandler inventory = getInventory(container);
        if (inventory == null) return false;
        for (int index = 0; index < inventory.getSlots(); index++) {
            if (!inventory.getStackInSlot(index).isEmpty() && inventory.getStackInSlot(index).isEdible()) return true;
        }
        return false;
    }

    private static boolean hasFoodAllowedWhenFull(ItemStack container, boolean clientSide) {
        ItemStackHandler inventory = getInventory(container);
        if (inventory == null) return false;
        for (int index = 0; index < inventory.getSlots(); index++) {
            ItemStack food = inventory.getStackInSlot(index);
            if (food.isEmpty() || !food.isEdible()) continue;
            boolean canEat = clientSide
                ? PomegranateClientData.canEatWhenFull(food.getItem())
                : PomegranateConfig.canEatWhenFull(food.getItem());
            if (canEat) return true;
        }
        return false;
    }
}
