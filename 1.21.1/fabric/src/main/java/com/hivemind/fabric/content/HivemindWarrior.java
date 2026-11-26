package com.hivemind.fabric.content;
import com.hivemind.core.Caste;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.ai.goal.*;
import net.minecraft.entity.passive.AnimalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.world.World;
public final class HivemindWarrior extends HiveEntity {
    public HivemindWarrior(EntityType<? extends HivemindWarrior> type, World level) { super(type, level); }
    public static DefaultAttributeContainer.Builder createHiveAttributes() { return attributes(Caste.WARRIOR); }
    @Override protected void initGoals() {
        goalSelector.add(0, new SwimGoal(this));
        goalSelector.add(1, new MeleeAttackGoal(this, 1.05, true));
        goalSelector.add(5, new WanderAroundFarGoal(this, 0.8) {
            @Override public boolean canStart() { return getNavigation().isIdle() && (getWorld().getAmbientDarkness() >= 4 || getWorld().getRandom().nextInt(4) == 0) && super.canStart(); }
        });
        goalSelector.add(6, new LookAtEntityGoal(this, PlayerEntity.class, 8));
        goalSelector.add(7, new LookAroundGoal(this));
        targetSelector.add(1, new RevengeGoal(this));

        targetSelector.add(3, new ActiveTargetGoal<>(this, AnimalEntity.class, true, target -> isUnassigned() && isPrey(target)));
    }
    public static boolean isPrey(net.minecraft.entity.LivingEntity animal) {
        var type = animal.getType();
        return type == EntityType.COW || type == EntityType.SHEEP || type == EntityType.PIG || type == EntityType.RABBIT || type == EntityType.CHICKEN;
    }
    private boolean isUnassigned() { return com.hivemind.core.HiveIdentity.colonyId(getCommandTags()).isEmpty(); }
    @Override protected net.minecraft.sound.SoundEvent getAmbientSound() { return HivemindSounds.WARRIOR_RATTLE; }
}
