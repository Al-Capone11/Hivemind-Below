package com.hivemind.fabric.content;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

public final class HivemindSounds {
    public static final SoundEvent HIVE_PULSE = register("hive_pulse");
    public static final SoundEvent HIVE_CHITTER = register("hive_chitter");
    public static final SoundEvent QUEEN_THRUM = register("queen_thrum");
    public static final SoundEvent WORKER_RASP = register("worker_rasp");
    public static final SoundEvent SCOUT_TRILL = register("scout_trill");
    public static final SoundEvent WARRIOR_RATTLE = register("warrior_rattle");
    public static final SoundEvent HIVE_HURT = register("hive_hurt");
    public static final SoundEvent HIVE_DEATH = register("hive_death");

    private HivemindSounds() {}

    private static SoundEvent register(String name) {
        Identifier id = Identifier.fromNamespaceAndPath("hivemind", name);
        return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
    }

    public static void initialize() {}
}
