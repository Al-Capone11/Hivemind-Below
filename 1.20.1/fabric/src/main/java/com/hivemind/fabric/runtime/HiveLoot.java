package com.hivemind.fabric.runtime;

import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.item.ItemStack;
import com.hivemind.fabric.content.HiveEntity;

public final class HiveLoot {
    private static final String KEY = "hivemind_hostile_loot";
    private HiveLoot() {}

    public interface HuntSource {
        net.minecraft.entity.damage.DamageSource hivemind$huntSource();
    }

    public static boolean collectHunt(Entity source, ItemStack stack) {
        return source instanceof net.minecraft.entity.LivingEntity prey
                && source instanceof HuntSource hunt && hunt.hivemind$huntSource() != null
                && HiveRuntime.collectHunt(prey, hunt.hivemind$huntSource(), stack);
    }

    public static void mark(Entity source, ItemStack stack) {
        if (source instanceof Monster && !(source instanceof HiveEntity) && !stack.isEmpty()) stack.getOrCreateNbt().putBoolean(KEY, true);
    }

    public static boolean hostileDrop(ItemStack stack) { return stack.hasNbt() && stack.getNbt().contains(KEY); }
}
