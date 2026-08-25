package com.y271727uy.pomegranate.client;

import com.y271727uy.pomegranate.SOLCarrot;
import com.y271727uy.pomegranate.PomegranateConfig;
import com.y271727uy.pomegranate.item.foodcontainer.FoodContainerItem;
import com.y271727uy.pomegranate.tracking.FoodList;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.tooltip.BundleTooltip;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import static com.y271727uy.pomegranate.lib.Localization.localizedComponent;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(value = Dist.CLIENT, modid = SOLCarrot.MOD_ID)
public final class TooltipHandler {
	@SubscribeEvent(priority = EventPriority.LOW)
	public static void onItemTooltip(ItemTooltipEvent event) {
		if (event.getItemStack().getItem() instanceof FoodContainerItem) {
			if (!Screen.hasShiftDown()) {
				event.getToolTip().add(Component.translatable("spice_of_life_pomegranate_edition.tooltip.food_container.show_contents").withStyle(ChatFormatting.DARK_GRAY));
			}
			return;
		}

		if (!PomegranateConfig.isFoodTooltipEnabled()) return;
		
		Player player = event.getEntity();
		if (player == null) return;
		
		Item food = event.getItemStack().getItem();
		if (!food.isEdible()) return;
		
		FoodList foodList = FoodList.get(player);
		boolean hasBeenEaten = foodList.hasEaten(food);
		boolean isAllowed = PomegranateConfig.isAllowed(food);
		boolean isHearty = PomegranateConfig.isHearty(food);
		
		var tooltip = event.getToolTip();
		if (!isAllowed) {
			if (hasBeenEaten) {
				tooltip.add(localizedTooltip("disabled.eaten", ChatFormatting.DARK_RED));
			}
			String key = PomegranateConfig.hasWhitelist() ? "whitelist" : "blacklist";
			tooltip.add(localizedTooltip("disabled." + key, ChatFormatting.DARK_GRAY));
		} else if (isHearty) {
			if (hasBeenEaten) {
				tooltip.add(localizedTooltip("hearty.eaten", ChatFormatting.DARK_GREEN));
			} else {
				tooltip.add(localizedTooltip("hearty.not_eaten", ChatFormatting.DARK_AQUA));
			}
		} else {
			if (hasBeenEaten) {
				tooltip.add(localizedTooltip("cheap.eaten", ChatFormatting.DARK_RED));
			}
			tooltip.add(localizedTooltip("cheap", ChatFormatting.DARK_GRAY));
		}

		if (hasBeenEaten) {
			int timesEaten = PomegranateClientData.getFoodCount(food);
			String pickyKey = pickyTooltipKey(timesEaten);
			tooltip.add(localizedTooltip(pickyKey, ChatFormatting.GOLD));
		}
	}

	@SubscribeEvent
	public static void onGatherTooltipComponents(RenderTooltipEvent.GatherComponents event) {
		if (Screen.hasShiftDown() || !(event.getItemStack().getItem() instanceof FoodContainerItem)) return;
		event.getTooltipElements().removeIf(element -> element.right().filter(BundleTooltip.class::isInstance).isPresent());
	}

	@SubscribeEvent
	public static void onMouseButtonPressed(ScreenEvent.MouseButtonPressed.Pre event) {
		if (shouldBlockShiftRightClick(event.getScreen(), event.getButton())) event.setCanceled(true);
	}

	@SubscribeEvent
	public static void onMouseButtonReleased(ScreenEvent.MouseButtonReleased.Pre event) {
		if (shouldBlockShiftRightClick(event.getScreen(), event.getButton())) event.setCanceled(true);
	}

	private static boolean shouldBlockShiftRightClick(Screen screen, int button) {
		if (button != 1 || !Screen.hasShiftDown() || !(screen instanceof AbstractContainerScreen<?> containerScreen)) return false;

		ItemStack carried = containerScreen.getMenu().getCarried();
		Slot slot = containerScreen.getSlotUnderMouse();
		return carried.getItem() instanceof FoodContainerItem
			|| slot != null && slot.getItem().getItem() instanceof FoodContainerItem;
	}

	private static String pickyTooltipKey(int timesEaten) {
		// Repeated-food tiers: 0-7, 8-10, 11-13, 14+.
		if (timesEaten <= 7) {
			return "pomegranate.picky.0_5";
		}
		if (timesEaten <= 10) {
			return "pomegranate.picky.6_9";
		}
		if (timesEaten <= 13) {
			return "pomegranate.picky.10_12";
		}
		return "pomegranate.picky.13_plus";
	}
	
	private static MutableComponent localizedTooltip(String path, ChatFormatting color) {
		return localizedComponent("tooltip", path).withStyle(color);
	}
	
	private TooltipHandler() {}
}
