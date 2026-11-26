package com.hivemind.fabric.mixin;

import com.hivemind.fabric.runtime.HiveLoot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.damage.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
abstract class HuntLootMixin implements HiveLoot.HuntSource {
    @Unique private DamageSource hivemind$lootSource;

    @Inject(method = "dropLoot", at = @At("HEAD"))
    private void hivemind$beginHuntLoot(DamageSource source, boolean playerKill, CallbackInfo callback) {
        hivemind$lootSource = source;
    }

    @Inject(method = "dropLoot", at = @At("RETURN"))
    private void hivemind$endHuntLoot(DamageSource source, boolean playerKill, CallbackInfo callback) {
        hivemind$lootSource = null;
    }

    @Override public DamageSource hivemind$huntSource() { return hivemind$lootSource; }
}
