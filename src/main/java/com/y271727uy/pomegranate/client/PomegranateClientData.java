package com.y271727uy.pomegranate.client;

import com.y271727uy.pomegranate.data.PomegranateData;
import com.y271727uy.pomegranate.PomegranateConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class PomegranateClientData {
	private static final Map<Item, Integer> clientFoodCounts = new HashMap<>();
	private static final List<Item> clientFoodHistory = new ArrayList<>();

	private static int maxFoodHistorySize = 100;
	private static int maxShortFoodHistorySize = 5;
	private static double shortFoodDecayModifier = 0.0;
	private static List<Double> foodDecayModifiers = new ArrayList<>();
	private static boolean eatingWhenFullEnabled = true;
	private static String fullHungerMode = "BLACKLIST";
	private static List<String> fullHungerItemList = new ArrayList<>();
	private static List<String> uneatableWhenFullItems = new ArrayList<>();
	
	public static int getFoodCount(Item item) {
		// Prefer client-side cache populated by server sync
		Integer cached = clientFoodCounts.get(item);
		if (cached != null) return cached;

		Player player = Minecraft.getInstance().player;
		if (player == null) {
			return 0;
		}
		
		// Fallback to reading from player persistent data (may be empty on client unless synced)
		return PomegranateData.getFoodCount(player, item);
	}

	public static int getTimesEatenLong(Item item) {
		int count = 0;
		for (Item historyItem : clientFoodHistory) {
			if (historyItem == item) count++;
		}
		return count;
	}

	public static int getTimesEatenShort(Item item) {
		int maxShort = Math.max(1, maxShortFoodHistorySize);
		int startIndex = Math.max(0, clientFoodHistory.size() - maxShort);
		int count = 0;
		for (int i = startIndex; i < clientFoodHistory.size(); i++) {
			if (clientFoodHistory.get(i) == item) count++;
		}
		return count;
	}

	public static void setFoodHistory(List<Item> items) {
		clientFoodHistory.clear();
		clientFoodHistory.addAll(items);
	}

	public static void setClassicConfig(int maxHistory, int maxShortHistory, double shortDecay, List<Double> decayModifiers) {
		maxFoodHistorySize = maxHistory;
		maxShortFoodHistorySize = maxShortHistory;
		shortFoodDecayModifier = shortDecay;
		foodDecayModifiers = new ArrayList<>(decayModifiers);
	}

	public static List<Double> getFoodDecayModifiers() {
		if (!foodDecayModifiers.isEmpty()) return new ArrayList<>(foodDecayModifiers);
		// fallback to local config defaults if server hasn't synced yet
		return PomegranateConfig.getFoodDecayModifiers();
	}

	public static double getShortFoodDecayModifier() {
		if (shortFoodDecayModifier != 0.0) return shortFoodDecayModifier;
		return PomegranateConfig.getShortfoodDecayModifiers();
	}
	
	public static void setFoodCount(Item item, int count) {
		clientFoodCounts.put(item, count);
	}

	public static void setEatingWhenFullSettings(boolean enabled, String mode, List<String> itemList, List<String> uneatableItems) {
		eatingWhenFullEnabled = enabled;
		fullHungerMode = mode;
		fullHungerItemList = new ArrayList<>(itemList);
		uneatableWhenFullItems = new ArrayList<>(uneatableItems);
	}

	public static boolean canEatWhenFull(Item item) {
		if (!eatingWhenFullEnabled) return false;
		var key = ForgeRegistries.ITEMS.getKey(item);
		if (key == null) return false;
		String id = key.toString();
		if (uneatableWhenFullItems.contains(id)) return false;
		return "BLACKLIST".equals(fullHungerMode)
			? !fullHungerItemList.contains(id)
			: fullHungerItemList.contains(id);
	}

	public static boolean isUneatableWhenFull(Item item) {
		var key = ForgeRegistries.ITEMS.getKey(item);
		return key != null && uneatableWhenFullItems.contains(key.toString());
	}
	
	public static void clear() {
		clientFoodCounts.clear();
		clientFoodHistory.clear();
		foodDecayModifiers = new ArrayList<>();
		shortFoodDecayModifier = 0.0;
		maxFoodHistorySize = 100;
		maxShortFoodHistorySize = 5;
		eatingWhenFullEnabled = true;
		fullHungerMode = "BLACKLIST";
		fullHungerItemList = new ArrayList<>();
		uneatableWhenFullItems = new ArrayList<>();
	}
	
	private PomegranateClientData() {}
}
