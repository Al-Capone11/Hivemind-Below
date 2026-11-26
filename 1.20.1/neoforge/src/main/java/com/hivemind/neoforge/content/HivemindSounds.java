package com.hivemind.neoforge.content;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
public final class HivemindSounds {
    public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, "hivemind");
    public static final RegistryObject<SoundEvent> HIVE_PULSE = SOUNDS.register("hive_pulse", () -> SoundEvent.createVariableRangeEvent(new net.minecraft.resources.ResourceLocation("hivemind", "hive_pulse")));
    public static final RegistryObject<SoundEvent> HIVE_CHITTER = SOUNDS.register("hive_chitter", () -> SoundEvent.createVariableRangeEvent(new net.minecraft.resources.ResourceLocation("hivemind", "hive_chitter")));
    public static final RegistryObject<SoundEvent> QUEEN_THRUM = SOUNDS.register("queen_thrum", () -> SoundEvent.createVariableRangeEvent(new net.minecraft.resources.ResourceLocation("hivemind", "queen_thrum")));
    public static final RegistryObject<SoundEvent> WORKER_RASP = SOUNDS.register("worker_rasp", () -> SoundEvent.createVariableRangeEvent(new net.minecraft.resources.ResourceLocation("hivemind", "worker_rasp")));
    public static final RegistryObject<SoundEvent> SCOUT_TRILL = SOUNDS.register("scout_trill", () -> SoundEvent.createVariableRangeEvent(new net.minecraft.resources.ResourceLocation("hivemind", "scout_trill")));
    public static final RegistryObject<SoundEvent> WARRIOR_RATTLE = SOUNDS.register("warrior_rattle", () -> SoundEvent.createVariableRangeEvent(new net.minecraft.resources.ResourceLocation("hivemind", "warrior_rattle")));
    public static final RegistryObject<SoundEvent> HIVE_HURT = SOUNDS.register("hive_hurt", () -> SoundEvent.createVariableRangeEvent(new net.minecraft.resources.ResourceLocation("hivemind", "hive_hurt")));
    public static final RegistryObject<SoundEvent> HIVE_DEATH = SOUNDS.register("hive_death", () -> SoundEvent.createVariableRangeEvent(new net.minecraft.resources.ResourceLocation("hivemind", "hive_death")));
    private HivemindSounds() {}
}
