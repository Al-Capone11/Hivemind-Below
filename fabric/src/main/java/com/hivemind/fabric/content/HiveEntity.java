package com.hivemind.fabric.content;

import com.hivemind.core.Caste;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

public abstract class HiveEntity extends Monster {
    protected HiveEntity(EntityType<? extends Monster> type, Level level) { super(type, level); }
    @Override protected net.minecraft.sounds.SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource source) { return HivemindSounds.HIVE_HURT; }
    @Override protected net.minecraft.sounds.SoundEvent getDeathSound() { return HivemindSounds.HIVE_DEATH; }

    public static AttributeSupplier.Builder attributes(Caste caste) {
        double health = switch(caste) { case QUEEN -> 80; case WARRIOR -> 32; case WORKER -> 22; case SCOUT -> 14; };
        double speed = switch(caste) { case QUEEN -> 0.16; case WARRIOR -> 0.25; case WORKER -> 0.23; case SCOUT -> 0.34; };
        double damage = switch(caste) { case QUEEN -> 6; case WARRIOR -> 6; case WORKER -> 3; case SCOUT -> 1; };
        double armor = switch(caste) { case QUEEN -> 8; case WARRIOR -> 6; case WORKER -> 3; case SCOUT -> 1; };
        double range = switch(caste) { case QUEEN -> 6; case WARRIOR -> 48; case WORKER -> 48; case SCOUT -> 20; };
        return Monster.createMonsterAttributes().add(Attributes.MAX_HEALTH, health).add(Attributes.MOVEMENT_SPEED, speed)
            .add(Attributes.ATTACK_DAMAGE, damage).add(Attributes.ARMOR, armor).add(Attributes.FOLLOW_RANGE, range);
    }
}
