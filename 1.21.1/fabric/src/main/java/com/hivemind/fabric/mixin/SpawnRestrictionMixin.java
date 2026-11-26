package com.hivemind.fabric.mixin;

import com.hivemind.fabric.runtime.HiveRuntime;
import net.minecraft.entity.SpawnRestriction;
import net.minecraft.entity.EntityType;
import net.minecraft.world.ServerWorldAccess;
import net.minecraft.entity.SpawnReason;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(SpawnRestriction.class)
abstract class SpawnRestrictionMixin {
    @Inject(method = "canSpawn", at = @At("HEAD"), cancellable = true)
    private static void hivemind$protectInterior(EntityType<?> type, ServerWorldAccess level, SpawnReason reason,
                                                 BlockPos pos, Random random, CallbackInfoReturnable<Boolean> callback) {
        if (HiveRuntime.blocksForeignSpawn(type, level, reason, pos)) callback.setReturnValue(false);
    }
}
