package com.hivemind.neoforge.content;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
public final class HivemindSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, "hivemind");
    public static final DeferredHolder<SoundEvent, SoundEvent> HIVE_PULSE = SOUNDS.register("hive_pulse", () -> SoundEvent.createVariableRangeEvent(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("hivemind", "hive_pulse")));
    public static final DeferredHolder<SoundEvent, SoundEvent> HIVE_CHITTER = SOUNDS.register("hive_chitter", () -> SoundEvent.createVariableRangeEvent(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("hivemind", "hive_chitter")));
    public static final DeferredHolder<SoundEvent, SoundEvent> QUEEN_THRUM = SOUNDS.register("queen_thrum", () -> SoundEvent.createVariableRangeEvent(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("hivemind", "queen_thrum")));
    public static final DeferredHolder<SoundEvent, SoundEvent> WORKER_RASP = SOUNDS.register("worker_rasp", () -> SoundEvent.createVariableRangeEvent(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("hivemind", "worker_rasp")));
    public static final DeferredHolder<SoundEvent, SoundEvent> SCOUT_TRILL = SOUNDS.register("scout_trill", () -> SoundEvent.createVariableRangeEvent(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("hivemind", "scout_trill")));
    public static final DeferredHolder<SoundEvent, SoundEvent> WARRIOR_RATTLE = SOUNDS.register("warrior_rattle", () -> SoundEvent.createVariableRangeEvent(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("hivemind", "warrior_rattle")));
    public static final DeferredHolder<SoundEvent, SoundEvent> HIVE_HURT = SOUNDS.register("hive_hurt", () -> SoundEvent.createVariableRangeEvent(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("hivemind", "hive_hurt")));
    public static final DeferredHolder<SoundEvent, SoundEvent> HIVE_DEATH = SOUNDS.register("hive_death", () -> SoundEvent.createVariableRangeEvent(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("hivemind", "hive_death")));
    private HivemindSounds() {}
}
