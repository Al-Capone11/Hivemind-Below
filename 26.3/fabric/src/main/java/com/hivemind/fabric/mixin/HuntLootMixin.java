package com.hivemind.fabric.mixin;

import com.hivemind.fabric.runtime.HiveLoot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
abstract class HuntLootMixin implements HiveLoot.HuntSource {
    @Unique private DamageSource hivemind$lootSource;

    @Inject(method = "dropFromLootTable(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;Z)V", at = @At("HEAD"))
    private void hivemind$beginHuntLoot(ServerLevel level, DamageSource source, boolean playerKill, CallbackInfo callback) {
        hivemind$lootSource = source;
    }

    @Inject(method = "dropFromLootTable(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;Z)V", at = @At("RETURN"))
    private void hivemind$endHuntLoot(ServerLevel level, DamageSource source, boolean playerKill, CallbackInfo callback) {
        hivemind$lootSource = null;
    }

    @Override public DamageSource hivemind$huntSource() { return hivemind$lootSource; }
}
