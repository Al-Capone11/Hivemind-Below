package com.hivemind.fabric.mixin;

import com.hivemind.fabric.runtime.HiveLoot;
import net.minecraft.entity.Entity;
import net.minecraft.item.ItemStack;
import net.minecraft.entity.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
abstract class HostileLootMixin {
    @Inject(method = "dropStack(Lnet/minecraft/item/ItemStack;F)Lnet/minecraft/entity/ItemEntity;", at = @At("HEAD"), cancellable = true)
    private void hivemind$markHostileLoot(ItemStack stack, float offset, CallbackInfoReturnable<ItemEntity> callback) {
        Entity source = (Entity) (Object) this;
        if (HiveLoot.collectHunt(source, stack)) {
            callback.setReturnValue(null);
            return;
        }
        HiveLoot.mark(source, stack);
    }
}
