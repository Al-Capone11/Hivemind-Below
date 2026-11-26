package com.hivemind.fabric.content;
import com.hivemind.core.Caste;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;
public final class HivemindScout extends HiveEntity {
    public HivemindScout(EntityType<? extends HivemindScout> type, World level) { super(type, level); }
    public static DefaultAttributeContainer.Builder createHiveAttributes() { return attributes(Caste.SCOUT); }
    @Override protected void initGoals() {
        goalSelector.add(0, new SwimGoal(this));
        goalSelector.add(1, new FleeEntityGoal<>(this, PlayerEntity.class, 10.0f, 1.2, 1.5));
        goalSelector.add(5, new WanderAroundFarGoal(this, 1.05) {
            @Override public boolean canStart() { return getNavigation().isIdle() && (getWorld().getAmbientDarkness() >= 4 || getWorld().getRandom().nextInt(4) == 0) && super.canStart(); }
        });
        goalSelector.add(6, new LookAroundGoal(this));
    }
    @Override protected net.minecraft.sound.SoundEvent getAmbientSound() { return HivemindSounds.SCOUT_TRILL; }
}
