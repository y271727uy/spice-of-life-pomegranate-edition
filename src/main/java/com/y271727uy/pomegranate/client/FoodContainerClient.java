package com.y271727uy.pomegranate.client;

import com.y271727uy.pomegranate.SOLCarrot;
import com.y271727uy.pomegranate.item.foodcontainer.FoodContainerMenu;
import com.y271727uy.pomegranate.item.foodcontainer.FoodContainerMenus;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = SOLCarrot.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class FoodContainerClient {
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> MenuScreens.register(FoodContainerMenus.FOOD_CONTAINER.get(), FoodContainerScreen::new));
    }

    private static final class FoodContainerScreen extends AbstractContainerScreen<FoodContainerMenu> {
        private static final ResourceLocation BACKGROUND = SOLCarrot.resourceLocation("textures/gui/inventory.png");
        private static final ResourceLocation SLOT = SOLCarrot.resourceLocation("textures/gui/slot.png");

        private FoodContainerScreen(FoodContainerMenu menu, Inventory inventory, Component title) {
            super(menu, inventory, title);
        }

        @Override
        public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
            renderBackground(graphics);
            super.render(graphics, mouseX, mouseY, partialTicks);
            renderTooltip(graphics, mouseX, mouseY);
        }

        @Override
        protected void renderBg(GuiGraphics graphics, float partialTicks, int mouseX, int mouseY) {
            graphics.blit(BACKGROUND, leftPos, topPos, 0, 0, imageWidth, imageHeight);
            for (int index = 0; index < menu.getContainerSlotCount(); index++) {
                net.minecraft.world.inventory.Slot slot = menu.slots.get(index);
                graphics.blit(SLOT, leftPos + slot.x - 1, topPos + slot.y - 1, 0, 0, 18, 18, 18, 18);
            }
        }
    }

    private FoodContainerClient() {}
}
