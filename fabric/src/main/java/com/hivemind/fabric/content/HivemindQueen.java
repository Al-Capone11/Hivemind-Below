package com.hivemind.fabric.content;
import com.hivemind.core.Caste;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
public final class HivemindQueen extends HiveEntity {
    public HivemindQueen(EntityType<? extends HivemindQueen> type, Level level) { super(type, level); }
    public static AttributeSupplier.Builder createHiveAttributes() { return attributes(Caste.QUEEN); }
    @Override public void setTarget(net.minecraft.world.entity.LivingEntity target) {
        super.setTarget(com.hivemind.fabric.runtime.HiveRuntime.withinRoyalChamber(this, target) ? target : null);
    }
    @Override public void tick() {
        if (getTarget() != null && !com.hivemind.fabric.runtime.HiveRuntime.withinRoyalChamber(this, getTarget())) setTarget(null);
        super.tick();
    }
    @Override protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new MeleeAttackGoal(this, 0.7, false));
        goalSelector.addGoal(5, new LookAtPlayerGoal(this, Player.class, 8));
        goalSelector.addGoal(6, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));
        targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }
    @Override protected net.minecraft.sounds.SoundEvent getAmbientSound() { return HivemindSounds.QUEEN_THRUM; }
}
