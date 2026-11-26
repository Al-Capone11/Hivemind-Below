package com.hivemind.fabric.content;
import com.hivemind.core.Caste;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.ai.goal.RevengeGoal;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;
public final class HivemindWorker extends HiveEntity {
    public HivemindWorker(EntityType<? extends HivemindWorker> type, World level) { super(type, level); }
    public static DefaultAttributeContainer.Builder createHiveAttributes() { return attributes(Caste.WORKER); }
    @Override protected void initGoals() {
        goalSelector.add(0, new SwimGoal(this));
        goalSelector.add(1, new MeleeAttackGoal(this, 1.0, false));
        goalSelector.add(2, new FleeEntityGoal<>(this, PlayerEntity.class, 6.0f, 1.1, 1.3));
        goalSelector.add(6, new WanderAroundFarGoal(this, 0.9) { @Override public boolean canStart() { return getNavigation().isIdle() && super.canStart(); } });
        goalSelector.add(7, new LookAroundGoal(this));
        targetSelector.add(1, new RevengeGoal(this));
    }
    @Override protected net.minecraft.sound.SoundEvent getAmbientSound() { return HivemindSounds.WORKER_RASP; }
}
