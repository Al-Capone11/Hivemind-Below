package com.hivemind.fabric.content;
import com.hivemind.core.Caste;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;
public final class HivemindQueen extends HiveEntity {
    public HivemindQueen(EntityType<? extends HivemindQueen> type, World level) { super(type, level); }
    public static DefaultAttributeContainer.Builder createHiveAttributes() { return attributes(Caste.QUEEN); }
    @Override public void setTarget(net.minecraft.entity.LivingEntity target) {
        super.setTarget(com.hivemind.fabric.runtime.HiveRuntime.withinRoyalChamber(this, target) ? target : null);
    }
    @Override public void tick() {
        if (getTarget() != null && !com.hivemind.fabric.runtime.HiveRuntime.withinRoyalChamber(this, getTarget())) setTarget(null);
        super.tick();
    }
    @Override protected void initGoals() {
        goalSelector.add(0, new SwimGoal(this));
        goalSelector.add(1, new MeleeAttackGoal(this, 0.7, false));
        goalSelector.add(5, new LookAtEntityGoal(this, PlayerEntity.class, 8));
        goalSelector.add(6, new LookAroundGoal(this));
        targetSelector.add(1, new RevengeGoal(this));
        targetSelector.add(2, new ActiveTargetGoal<>(this, PlayerEntity.class, true));
    }
    @Override protected net.minecraft.sound.SoundEvent getAmbientSound() { return HivemindSounds.QUEEN_THRUM; }
}
