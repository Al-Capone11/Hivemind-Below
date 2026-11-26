package com.hivemind.neoforge;

import com.hivemind.neoforge.content.HivemindContent;
import com.hivemind.neoforge.runtime.HiveRuntime;
import com.hivemind.neoforge.runtime.HiveLoot;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.minecraft.server.level.ServerLevel;

@Mod("hivemind")
public final class HivemindNeoForge {
    public HivemindNeoForge(IEventBus modBus) {
        HivemindContent.register(modBus);
        NeoForge.EVENT_BUS.addListener(this::registerCommands);
        NeoForge.EVENT_BUS.addListener(this::serverStarted);
        NeoForge.EVENT_BUS.addListener(this::serverStopping);
        NeoForge.EVENT_BUS.addListener(this::serverTick);
        NeoForge.EVENT_BUS.addListener(this::entityJoin);
        NeoForge.EVENT_BUS.addListener(this::livingDeath);
        NeoForge.EVENT_BUS.addListener(this::hostileLoot);
        NeoForge.EVENT_BUS.addListener(this::protectInterior);
        NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.LOWEST, this::structureDamage);
    }
    private void protectInterior(MobSpawnEvent.SpawnPlacementCheck event) {
        if (HiveRuntime.blocksForeignSpawn(event.getEntityType(), event.getLevel(), event.getSpawnType(), event.getPos()))
            event.setResult(MobSpawnEvent.SpawnPlacementCheck.Result.FAIL);
    }
    private void hostileLoot(LivingDropsEvent event) {
        event.getDrops().removeIf(drop -> HiveRuntime.collectHunt(event.getEntity(), event.getSource(), drop.getItem()));
        for (var drop : event.getDrops()) HiveLoot.mark(event.getEntity(), drop.getItem());
    }
    private void registerCommands(RegisterCommandsEvent event) { HiveRuntime.registerCommands(event.getDispatcher()); }
    private void serverStarted(ServerStartedEvent event) { HiveRuntime.load(event.getServer()); }
    private void serverStopping(ServerStoppingEvent event) { HiveRuntime.stop(event.getServer()); }
    private void serverTick(ServerTickEvent.Post event) { HiveRuntime.tick(event.getServer()); }
    private void entityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel() instanceof ServerLevel level) HiveRuntime.onEntityLoad(event.getEntity(), level);
    }
    private void structureDamage(net.neoforged.neoforge.event.level.block.BreakBlockEvent event) {
        if (!event.isCanceled() && event.getLevel() instanceof ServerLevel level) HiveRuntime.onStructureDamage(level, event.getPlayer(), event.getPos(), event.getState());
    }
    private void livingDeath(LivingDeathEvent event) { if (event.getEntity().level() instanceof ServerLevel) HiveRuntime.onDeath(event.getEntity(), event.getSource()); }
}
