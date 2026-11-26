package com.hivemind.neoforge.content;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

public final class HivemindSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(BuiltInRegistries.SOUND_EVENT, "hivemind");
    public static final DeferredHolder<SoundEvent, SoundEvent> HIVE_PULSE = SOUNDS.register("hive_pulse", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> HIVE_CHITTER = SOUNDS.register("hive_chitter", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> QUEEN_THRUM = SOUNDS.register("queen_thrum", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> WORKER_RASP = SOUNDS.register("worker_rasp", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> SCOUT_TRILL = SOUNDS.register("scout_trill", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> WARRIOR_RATTLE = SOUNDS.register("warrior_rattle", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> HIVE_HURT = SOUNDS.register("hive_hurt", SoundEvent::createVariableRangeEvent);
    public static final DeferredHolder<SoundEvent, SoundEvent> HIVE_DEATH = SOUNDS.register("hive_death", SoundEvent::createVariableRangeEvent);

    private HivemindSounds() {}
}
