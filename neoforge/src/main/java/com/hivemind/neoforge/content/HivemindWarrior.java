package com.hivemind.neoforge.content;
import com.hivemind.core.Caste;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.*;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
public final class HivemindWarrior extends HiveEntity {
    public HivemindWarrior(EntityType<? extends HivemindWarrior> type, Level level) { super(type, level); }
    public static AttributeSupplier.Builder createHiveAttributes() { return attributes(Caste.WARRIOR); }
    @Override protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.05, true));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8) {
            @Override public boolean canUse() { return getNavigation().isDone() && (level().getSkyDarken() >= 4 || level().getRandom().nextInt(4) == 0) && super.canUse(); }
        });
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new HurtByTargetGoal(this));

        targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(this, Animal.class, true, (target, level) -> isUnassigned() && isPrey(target)));
    }
    public static boolean isPrey(net.minecraft.world.entity.LivingEntity animal) {
        var type = animal.getType();
        return type == net.minecraft.world.entity.EntityTypes.COW || type == net.minecraft.world.entity.EntityTypes.SHEEP || type == net.minecraft.world.entity.EntityTypes.PIG || type == net.minecraft.world.entity.EntityTypes.RABBIT || type == net.minecraft.world.entity.EntityTypes.CHICKEN;
    }
    private boolean isUnassigned() { return com.hivemind.core.HiveIdentity.colonyId(entityTags()).isEmpty(); }
    @Override protected net.minecraft.sounds.SoundEvent getAmbientSound() { return HivemindSounds.WARRIOR_RATTLE.get(); }
}
