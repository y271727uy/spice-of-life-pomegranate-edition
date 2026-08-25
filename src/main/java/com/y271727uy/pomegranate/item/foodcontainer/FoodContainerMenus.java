package com.y271727uy.pomegranate.item.foodcontainer;

import com.y271727uy.pomegranate.SOLCarrot;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class FoodContainerMenus {
    private static final DeferredRegister<MenuType<?>> MENU_TYPES =
        DeferredRegister.create(ForgeRegistries.MENU_TYPES, SOLCarrot.MOD_ID);

    public static final RegistryObject<MenuType<FoodContainerMenu>> FOOD_CONTAINER = MENU_TYPES.register(
        "food_container", () -> IForgeMenuType.create((id, inventory, data) -> new FoodContainerMenu(id, inventory, inventory.player))
    );

    public static void setUp(IEventBus eventBus) {
        MENU_TYPES.register(eventBus);
    }

    private FoodContainerMenus() {}
}
