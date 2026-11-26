package com.hivemind.fabric.content;

import com.hivemind.core.Caste;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.world.World;

public abstract class HiveEntity extends HostileEntity {
    protected HiveEntity(EntityType<? extends HostileEntity> type, World level) { super(type, level); }
    @Override protected net.minecraft.sound.SoundEvent getHurtSound(net.minecraft.entity.damage.DamageSource source) { return HivemindSounds.HIVE_HURT; }
    @Override protected net.minecraft.sound.SoundEvent getDeathSound() { return HivemindSounds.HIVE_DEATH; }

    public static DefaultAttributeContainer.Builder attributes(Caste caste) {
        double health = switch(caste) { case QUEEN -> 80; case WARRIOR -> 32; case WORKER -> 22; case SCOUT -> 14; };
        double speed = switch(caste) { case QUEEN -> 0.16; case WARRIOR -> 0.25; case WORKER -> 0.23; case SCOUT -> 0.34; };
        double damage = switch(caste) { case QUEEN -> 6; case WARRIOR -> 6; case WORKER -> 3; case SCOUT -> 1; };
        double armor = switch(caste) { case QUEEN -> 8; case WARRIOR -> 6; case WORKER -> 3; case SCOUT -> 1; };
        double range = switch(caste) { case QUEEN -> 6; case WARRIOR -> 48; case WORKER -> 48; case SCOUT -> 20; };
        return HostileEntity.createHostileAttributes().add(EntityAttributes.GENERIC_MAX_HEALTH, health).add(EntityAttributes.GENERIC_MOVEMENT_SPEED, speed)
            .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, damage).add(EntityAttributes.GENERIC_ARMOR, armor).add(EntityAttributes.GENERIC_FOLLOW_RANGE, range);
    }
}
