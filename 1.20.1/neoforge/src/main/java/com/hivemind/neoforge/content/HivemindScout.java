package com.hivemind.neoforge.content;
import com.hivemind.core.Caste;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
public final class HivemindScout extends HiveEntity {
    public HivemindScout(EntityType<? extends HivemindScout> type, Level level) { super(type, level); }
    public static AttributeSupplier.Builder createHiveAttributes() { return attributes(Caste.SCOUT); }
    @Override protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new AvoidEntityGoal<>(this, Player.class, 10.0f, 1.2, 1.5));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.05) {
            @Override public boolean canUse() { return getNavigation().isDone() && (level().getSkyDarken() >= 4 || level().getRandom().nextInt(4) == 0) && super.canUse(); }
        });
        goalSelector.addGoal(6, new RandomLookAroundGoal(this));
    }
    @Override protected net.minecraft.sounds.SoundEvent getAmbientSound() { return HivemindSounds.SCOUT_TRILL.get(); }
}
