package com.y271727uy.pomegranate;

import com.y271727uy.pomegranate.SOLCarrot;
import com.y271727uy.pomegranate.tracking.CapabilityHandler;
import com.y271727uy.pomegranate.tracking.FoodList;
import com.google.common.collect.Lists;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;

import static net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus.MOD;

@Mod.EventBusSubscriber(modid = SOLCarrot.MOD_ID, bus = MOD)
public final class PomegranateConfig {
    private static String localizationPath(String path) {
        return "config." + SOLCarrot.MOD_ID + ".pomegranate." + path;
    }

    private static String legacyLocalizationPath(String path) {
        return "config." + SOLCarrot.MOD_ID + "." + path;
    }

    public static final Server SERVER;
    public static final ForgeConfigSpec SERVER_SPEC;
    public static final Client CLIENT;
    public static final ForgeConfigSpec CLIENT_SPEC;

    // cached TagKey lists
    private static List<TagKey<Item>> STAPLE_TAG_KEYS = null;
    private static List<TagKey<Item>> PRODUCE_TAG_KEYS = null;

    static {
        Pair<Server, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(Server::new);
        SERVER = specPair.getLeft();
        SERVER_SPEC = specPair.getRight();
    }

    static {
        Pair<Client, ForgeConfigSpec> specPair = new ForgeConfigSpec.Builder().configure(Client::new);
        CLIENT = specPair.getLeft();
        CLIENT_SPEC = specPair.getRight();
    }

    public static void setUp() {
        ModLoadingContext context = ModLoadingContext.get();
        context.registerConfig(ModConfig.Type.SERVER, SERVER_SPEC, "spice_of_life_pomegranate_edition-server.toml");
        context.registerConfig(ModConfig.Type.CLIENT, CLIENT_SPEC);
    }

    public static int getMaxUniqueFoods() { return SERVER.maxUniqueFoods.get(); }
    public static int getOverweightStapleThreshold() { return SERVER.overweightStapleThreshold.get(); }
    public static int getOverweightProduceResetThreshold() { return SERVER.overweightProduceResetThreshold.get(); }
    public static boolean enablePunishments() { return SERVER.enablePunishments.get(); }
    public static boolean enableRewards() { return SERVER.enableRewards.get(); }
    public static boolean enableOverweight() { return SERVER.enableOverweight.get(); }
    public static boolean enableEatingWhenFull() { return SERVER.enableEatingWhenFull.get(); }
    public static FullHungerMode getFullHungerMode() { return SERVER.fullHungerMode.get(); }
    public static List<String> getFullHungerItemList() { return new ArrayList<>(SERVER.fullHungerItemList.get()); }
    public static List<String> getUneatableWhenFullItems() { return new ArrayList<>(SERVER.uneatableWhenFullItems.get()); }
    public static List<String> getStapleTags() { return new ArrayList<>(SERVER.stapleTags.get()); }
    public static List<String> getProduceTags() { return new ArrayList<>(SERVER.produceTags.get()); }

    // Classic-style config getters
    public static int getMaxFoodHistorySize() { return SERVER.maxFoodHistorySize.get(); }
    public static int getMaxShortFoodHistorySize() { return SERVER.maxShortFoodHistorySize.get(); }
    public static double getShortfoodDecayModifiers() { return SERVER.ShortfoodDecayModifiers.get(); }
    public static List<Double> getFoodDecayModifiers() { return new ArrayList<>(SERVER.foodDecayModifiers.get()); }

    public static int getBaseHearts() { return SERVER.baseHearts.get(); }
    public static int getHeartsPerMilestone() { return SERVER.heartsPerMilestone.get(); }
    public static List<Integer> getMilestones() { return new ArrayList<>(SERVER.milestones.get()); }
    public static List<String> getBlacklist() { return new ArrayList<>(SERVER.blacklist.get()); }
    public static List<String> getWhitelist() { return new ArrayList<>(SERVER.whitelist.get()); }
    public static int getMinimumFoodValue() { return SERVER.minimumFoodValue.get(); }
    public static boolean shouldResetOnDeath() { return SERVER.shouldResetOnDeath.get(); }
    public static boolean limitProgressionToSurvival() { return SERVER.limitProgressionToSurvival.get(); }
    public static boolean shouldPlayMilestoneSounds() { return CLIENT.shouldPlayMilestoneSounds.get(); }
    public static boolean shouldSpawnIntermediateParticles() { return CLIENT.shouldSpawnIntermediateParticles.get(); }
    public static boolean shouldSpawnMilestoneParticles() { return CLIENT.shouldSpawnMilestoneParticles.get(); }
    public static boolean isFoodTooltipEnabled() { return CLIENT.isFoodTooltipEnabled.get(); }
    public static boolean shouldShowProgressAboveHotbar() { return CLIENT.shouldShowProgressAboveHotbar.get(); }
    public static boolean shouldShowUneatenFoods() { return CLIENT.shouldShowUneatenFoods.get(); }
    public static boolean isSingleRowHeartOverlayEnabled() { return CLIENT.isSingleRowHeartOverlayEnabled.get(); }

    public static int milestone(int index) { return SERVER.milestones.get().get(index); }
    public static int getMilestoneCount() { return SERVER.milestones.get().size(); }
    public static int highestMilestone() { return milestone(getMilestoneCount() - 1); }
    public static boolean hasWhitelist() { return !SERVER.whitelist.get().isEmpty(); }

    public static boolean isAllowed(Item food) {
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(food);
        if (key == null) return false;
        String id = key.toString();
        return hasWhitelist()
            ? matchesAnyPattern(id, SERVER.whitelist.get())
            : !matchesAnyPattern(id, SERVER.blacklist.get());
    }

    public static boolean shouldCount(Item food) {
        return isHearty(food) && isAllowed(food);
    }

    public static boolean isHearty(Item food) {
        var properties = food.getFoodProperties();
        return properties != null && properties.getNutrition() >= getMinimumFoodValue();
    }

    public static boolean canEatWhenFull(Item item) {
        if (!enableEatingWhenFull()) return false;
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(item);
        if (key == null) return false;
        String id = key.toString();
        if (getUneatableWhenFullItems().contains(id)) return false;
        return getFullHungerMode() == FullHungerMode.BLACKLIST
            ? !getFullHungerItemList().contains(id)
            : getFullHungerItemList().contains(id);
    }

    public static boolean isUneatableWhenFull(Item item) {
        ResourceLocation key = ForgeRegistries.ITEMS.getKey(item);
        return key != null && getUneatableWhenFullItems().contains(key.toString());
    }

    @SubscribeEvent
    public static void onConfigReload(ModConfigEvent.Reloading event) {
        if (event.getConfig().getSpec() != SERVER_SPEC) return;
        synchronized (PomegranateConfig.class) {
            STAPLE_TAG_KEYS = null;
            PRODUCE_TAG_KEYS = null;
        }
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        for (var player : server.getPlayerList().getPlayers()) {
            FoodList.get(player).invalidateProgressInfo();
            CapabilityHandler.syncFoodList(player);
        }
    }

    // Return TagKey<Item> lists built from configured tag strings. Cached.
    public static synchronized List<TagKey<Item>> getStapleTagKeys() {
        if (STAPLE_TAG_KEYS == null) {
            STAPLE_TAG_KEYS = buildTagKeys(getStapleTags());
        }
        return STAPLE_TAG_KEYS;
    }

    public static synchronized List<TagKey<Item>> getProduceTagKeys() {
        if (PRODUCE_TAG_KEYS == null) {
            PRODUCE_TAG_KEYS = buildTagKeys(getProduceTags());
        }
        return PRODUCE_TAG_KEYS;
    }

    private static List<TagKey<Item>> buildTagKeys(List<String> tags) {
        List<TagKey<Item>> keys = new ArrayList<>();
        for (String s : tags) {
            try {
                ResourceLocation rl = new ResourceLocation(s);
                TagKey<Item> key = TagKey.create(Registries.ITEM, rl);
                keys.add(key);
            } catch (Exception e) {
                // ignore malformed entries
            }
        }
        return keys;
    }

    public static class Server {
        public final ForgeConfigSpec.IntValue maxUniqueFoods;
        public final ForgeConfigSpec.IntValue overweightStapleThreshold;
        public final ForgeConfigSpec.IntValue overweightProduceResetThreshold;
        public final ForgeConfigSpec.BooleanValue enablePunishments;
        public final ForgeConfigSpec.BooleanValue enableRewards;
        public final ForgeConfigSpec.BooleanValue enableOverweight;
        public final ForgeConfigSpec.BooleanValue enableEatingWhenFull;
        public final ForgeConfigSpec.EnumValue<FullHungerMode> fullHungerMode;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> fullHungerItemList;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> uneatableWhenFullItems;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> stapleTags;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> produceTags;

        public final ForgeConfigSpec.IntValue baseHearts;
        public final ForgeConfigSpec.IntValue heartsPerMilestone;
        public final ForgeConfigSpec.ConfigValue<List<? extends Integer>> milestones;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> blacklist;
        public final ForgeConfigSpec.ConfigValue<List<? extends String>> whitelist;
        public final ForgeConfigSpec.IntValue minimumFoodValue;
        public final ForgeConfigSpec.BooleanValue shouldResetOnDeath;
        public final ForgeConfigSpec.BooleanValue limitProgressionToSurvival;

        // Classic fields
        public final ForgeConfigSpec.IntValue maxFoodHistorySize;
        public final ForgeConfigSpec.IntValue maxShortFoodHistorySize;
        public final ForgeConfigSpec.DoubleValue ShortfoodDecayModifiers;
        public final ForgeConfigSpec.ConfigValue<List<? extends Double>> foodDecayModifiers;

        Server(ForgeConfigSpec.Builder builder) {
            builder.push("pomegranate");

            maxUniqueFoods = builder
                .translation(localizationPath("max_unique_foods"))
                .comment("The maximum number of unique foods tracked before the list resets.")
                .defineInRange("maxUniqueFoods", 150, 1, 1000);

            overweightStapleThreshold = builder
                .translation(localizationPath("overweight_staple_threshold"))
                .comment("How many staple foods cause the player to become overweight.")
                .defineInRange("overweightStapleThreshold", 10, 1, 1000);

            overweightProduceResetThreshold = builder
                .translation(localizationPath("overweight_produce_reset_threshold"))
                .comment("How many fruits/vegetables are needed to clear the overweight state.")
                .defineInRange("overweightProduceResetThreshold", 10, 1, 1000);

            enablePunishments = builder
                .translation(localizationPath("enable_punishments"))
                .comment("If true, repeated foods apply punishments and reduced recovery.")
                .define("enablePunishments", true);

            enableRewards = builder
                .translation(localizationPath("enable_rewards"))
                .comment("If true, variety rewards grant temporary positive effects.")
                .define("enableRewards", true);

            enableOverweight = builder
                .translation(localizationPath("enable_overweight"))
                .comment("If true, eating too many staples causes overweight penalties.")
                .define("enableOverweight", true);

            stapleTags = builder
                .translation(localizationPath("staple_tags"))
                .comment("Item tags used to identify staple foods.")
                .defineList("stapleTags", Lists.newArrayList("spice_of_life_pomegranate_edition:staples", "forge:bread", "forge:grain", "forge:grains", "diet:grain", "diet:grains"), e -> e instanceof String);

            produceTags = builder
                .translation(localizationPath("produce_tags"))
                .comment("Item tags used to identify fruits and vegetables.")
                .defineList("produceTags", Lists.newArrayList("spice_of_life_pomegranate_edition:produce", "forge:vegetables", "forge:fruits", "diet:vegetables", "diet:fruits"), e -> e instanceof String);

            builder.push("eat_when_full");

            enableEatingWhenFull = builder
                .translation(localizationPath("enable_eating_when_full"))
                .comment("If true, configured foods can be eaten even when the player is not hungry.")
                .define("enableEatingWhenFull", true);

            fullHungerMode = builder
                .translation(localizationPath("full_hunger_mode"))
                .comment("BLACKLIST allows every food except listed items; WHITELIST allows only listed items.")
                .defineEnum("fullHungerMode", FullHungerMode.BLACKLIST);

            fullHungerItemList = builder
                .translation(localizationPath("full_hunger_item_list"))
                .comment("Food item IDs interpreted according to fullHungerMode.")
                .defineList("fullHungerItemList", Lists.newArrayList(), PomegranateConfig::isValidItemId);

            uneatableWhenFullItems = builder
                .translation(localizationPath("uneatable_when_full_items"))
                .comment("Food item IDs that must not be eaten when full, even if they normally can be.")
                .defineList("uneatableWhenFullItems", Lists.newArrayList(), PomegranateConfig::isValidItemId);

            builder.pop();

            // Classic config group
            builder.push("classic");

            maxFoodHistorySize = builder
                .comment("Maximum number of food history entries to track")
                .defineInRange("maxFoodHistorySize", 100, 5, 1000);

            maxShortFoodHistorySize = builder
                .comment("Maximum number of short food history entries to consider for decay")
                .defineInRange("maxShortFoodHistorySize", 5, 1, 1000);

            ShortfoodDecayModifiers = builder
                .comment("Short decay modifier per repeat (double)")
                .defineInRange("ShortfoodDecayModifiers", 0.01D, 0.0D, 1.0D);

            foodDecayModifiers = builder
                .comment("List of decay modifiers applied per short-history index")
                .defineList("foodDecayModifiers", Lists.newArrayList(1.0D, 0.90D, 0.75D, 0.50D, 0.05D), o -> o instanceof Double);

            builder.pop(); // end classic

            builder.pop(); // end pomegranate

            builder.push("milestones");
            baseHearts = builder.translation(legacyLocalizationPath("base_hearts"))
                .comment("Number of hearts you start out with.").defineInRange("baseHearts", 10, 0, 1000);
            heartsPerMilestone = builder.translation(legacyLocalizationPath("hearts_per_milestone"))
                .comment("Number of hearts gained for reaching a milestone.").defineInRange("heartsPerMilestone", 1, 0, 1000);
            milestones = builder.translation(legacyLocalizationPath("milestones"))
                .comment("Unique-food milestones that grant hearts.")
                .defineList("milestones", Lists.newArrayList(6, 12, 18, 24, 30, 36, 42, 48, 54, 60, 66, 72, 78, 84, 90, 96, 102, 108, 114, 120, 126, 132, 138, 144, 150), value -> value instanceof Integer);
            builder.pop();

            builder.push("filtering");
            blacklist = builder.translation(legacyLocalizationPath("blacklist"))
                .comment("Foods in this list do not affect health or appear in the food book.")
                .defineList("blacklist", Lists.newArrayList(), value -> value instanceof String);
            whitelist = builder.translation(legacyLocalizationPath("whitelist"))
                .comment("When non-empty, only foods in this list count.")
                .defineList("whitelist", Lists.newArrayList(), value -> value instanceof String);
            minimumFoodValue = builder.translation(legacyLocalizationPath("minimum_food_value"))
                .comment("Minimum nutrition needed to count toward milestones.")
                .defineInRange("minimumFoodValue", 1, 0, 1000);
            builder.pop();

            builder.push("miscellaneous");
            shouldResetOnDeath = builder.translation(legacyLocalizationPath("reset_on_death"))
                .comment("Whether death resets the food list and bonus hearts.").define("resetOnDeath", false);
            limitProgressionToSurvival = builder.translation(legacyLocalizationPath("limit_progression_to_survival"))
                .comment("Whether foods eaten outside survival mode are ignored.").define("limitProgressionToSurvival", false);
            builder.pop();
        }
    }

    public enum FullHungerMode {
        BLACKLIST,
        WHITELIST
    }

    public static class Client {
        public final ForgeConfigSpec.BooleanValue shouldPlayMilestoneSounds;
        public final ForgeConfigSpec.BooleanValue shouldSpawnIntermediateParticles;
        public final ForgeConfigSpec.BooleanValue shouldSpawnMilestoneParticles;
        public final ForgeConfigSpec.BooleanValue isFoodTooltipEnabled;
        public final ForgeConfigSpec.BooleanValue shouldShowProgressAboveHotbar;
        public final ForgeConfigSpec.BooleanValue shouldShowUneatenFoods;
        public final ForgeConfigSpec.BooleanValue isSingleRowHeartOverlayEnabled;

        Client(ForgeConfigSpec.Builder builder) {
            builder.push("milestone celebration");
            shouldPlayMilestoneSounds = builder.translation(legacyLocalizationPath("should_play_milestone_sounds"))
                .comment("Whether milestones play a sound.").define("shouldPlayMilestoneSounds", true);
            shouldSpawnIntermediateParticles = builder.translation(legacyLocalizationPath("should_spawn_intermediate_particles"))
                .comment("Whether trying a new food spawns particles.").define("shouldSpawnIntermediateParticles", true);
            shouldSpawnMilestoneParticles = builder.translation(legacyLocalizationPath("should_spawn_milestone_particles"))
                .comment("Whether milestones spawn particles.").define("shouldSpawnMilestoneParticles", true);
            builder.pop();

            builder.push("miscellaneous");
            isFoodTooltipEnabled = builder.translation(legacyLocalizationPath("is_food_tooltip_enabled"))
                .comment("Whether food tooltips show progression information.").define("isFoodTooltipEnabled", true);
            shouldShowProgressAboveHotbar = builder.translation(legacyLocalizationPath("should_show_progress_above_hotbar"))
                .comment("Whether milestone messages appear above the hotbar.").define("shouldShowProgressAboveHotbar", true);
            shouldShowUneatenFoods = builder.translation(legacyLocalizationPath("should_show_uneaten_foods"))
                .comment("Whether the food book lists foods not yet eaten.").define("shouldShowUneatenFoods", true);
            isSingleRowHeartOverlayEnabled = builder.translation(legacyLocalizationPath("single_row_heart_overlay_enabled"))
                .comment("Whether bonus health uses a single-row heart overlay.").define("singleRowHeartOverlayEnabled", true);
            builder.pop();
        }
    }

    private static boolean isValidItemId(Object value) {
        return value instanceof String id && ResourceLocation.tryParse(id) != null;
    }

    private static boolean matchesAnyPattern(String query, Collection<? extends String> patterns) {
        for (String glob : patterns) {
            StringBuilder pattern = new StringBuilder(glob.length());
            for (String part : glob.split("\\*", -1)) {
                if (!part.isEmpty()) pattern.append(Pattern.quote(part));
                pattern.append(".*");
            }
            pattern.delete(pattern.length() - 2, pattern.length());
            if (Pattern.matches(pattern.toString(), query)) return true;
        }
        return false;
    }

    private PomegranateConfig() {}
}
