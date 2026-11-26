package com.hivemind.fabric.content;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

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
    private static SoundEvent register(String path) {
        Identifier id = Identifier.of("hivemind", path);
        return Registry.register(Registries.SOUND_EVENT, id, SoundEvent.of(id));
    }
    public static void initialize() {}
}
