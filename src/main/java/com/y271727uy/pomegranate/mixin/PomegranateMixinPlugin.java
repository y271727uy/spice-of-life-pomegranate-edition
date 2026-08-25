package com.y271727uy.pomegranate.mixin;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public final class PomegranateMixinPlugin implements IMixinConfigPlugin {
    private static final String APPLESKIN_MIXIN = "com.y271727uy.pomegranate.mixin.appleskin.AppleSkinFoodHelperMixin";
    private static final String APPLESKIN_FOOD_HELPER = "squeek.appleskin.helpers.FoodHelper";

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (!APPLESKIN_MIXIN.equals(mixinClassName)) return true;

        try {
            Class.forName(APPLESKIN_FOOD_HELPER, false, getClass().getClassLoader());
            return true;
        } catch (ClassNotFoundException ignored) {
            return false;
        }
    }

    @Override public void onLoad(String mixinPackage) {}
    @Override public String getRefMapperConfig() { return null; }
    @Override public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}
    @Override public List<String> getMixins() { return null; }
    @Override public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
    @Override public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {}
}
