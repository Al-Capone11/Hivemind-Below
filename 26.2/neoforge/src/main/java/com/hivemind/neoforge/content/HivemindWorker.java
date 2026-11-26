package com.hivemind.neoforge.content;
import com.hivemind.core.Caste;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
public final class HivemindWorker extends HiveEntity {
    public HivemindWorker(EntityType<? extends HivemindWorker> type, Level level) { super(type, level); }
    public static AttributeSupplier.Builder createHiveAttributes() { return attributes(Caste.WORKER); }
    @Override protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0, false));
        goalSelector.addGoal(2, new AvoidEntityGoal<>(this, Player.class, 6.0f, 1.1, 1.3));
        goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.9) { @Override public boolean canUse() { return getNavigation().isDone() && super.canUse(); } });
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
    }
    @Override protected net.minecraft.sounds.SoundEvent getAmbientSound() { return HivemindSounds.WORKER_RASP.get(); }
}
