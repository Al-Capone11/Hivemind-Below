package com.hivemind.fabric.mixin;

import com.hivemind.fabric.runtime.HiveRuntime;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SpawnPlacements.class)
abstract class SpawnRestrictionMixin {
    @Inject(method = "checkSpawnRules", at = @At("HEAD"), cancellable = true)
    private static void hivemind$protectInterior(EntityType<?> type, ServerLevelAccessor level, EntitySpawnReason reason,
                                                 BlockPos pos, RandomSource random, CallbackInfoReturnable<Boolean> callback) {
        if (HiveRuntime.blocksForeignSpawn(type, level, reason, pos)) callback.setReturnValue(false);
    }
}
