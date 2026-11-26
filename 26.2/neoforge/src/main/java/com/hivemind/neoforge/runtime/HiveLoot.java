package com.hivemind.neoforge.runtime;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import com.hivemind.neoforge.content.HiveEntity;

public final class HiveLoot {
    private static final String KEY = "hivemind_hostile_loot";
    private HiveLoot() {}

    public static void mark(Entity source, ItemStack stack) {
        if (source instanceof Enemy && !(source instanceof HiveEntity) && !stack.isEmpty()) net.minecraft.world.item.component.CustomData.update(net.minecraft.core.component.DataComponents.CUSTOM_DATA, stack, tag -> tag.putBoolean(KEY, true));
    }

    public static boolean hostileDrop(ItemStack stack) { return stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY).copyTag().contains(KEY); }
}
