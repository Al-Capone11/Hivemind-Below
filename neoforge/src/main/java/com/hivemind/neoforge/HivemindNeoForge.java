package com.hivemind.neoforge;

import com.hivemind.neoforge.runtime.HiveRuntime;
import com.hivemind.neoforge.runtime.HiveLoot;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import com.hivemind.neoforge.content.HivemindContent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.fml.common.Mod;

@Mod("hivemind")
public final class HivemindNeoForge {
    public HivemindNeoForge() {
        HivemindContent.register(FMLJavaModLoadingContext.get().getModEventBus());
        MinecraftForge.EVENT_BUS.addListener(this::registerCommands);
        MinecraftForge.EVENT_BUS.addListener(this::serverStarted);
        MinecraftForge.EVENT_BUS.addListener(this::serverStopping);
        MinecraftForge.EVENT_BUS.addListener(this::serverTick);
        MinecraftForge.EVENT_BUS.addListener(this::entityJoin);
        MinecraftForge.EVENT_BUS.addListener(this::livingDeath);
        MinecraftForge.EVENT_BUS.addListener(this::hostileLoot);
        MinecraftForge.EVENT_BUS.addListener(this::protectInterior);
        MinecraftForge.EVENT_BUS.addListener(net.minecraftforge.eventbus.api.EventPriority.LOWEST, this::structureDamage);
    }
    private void protectInterior(MobSpawnEvent.SpawnPlacementCheck event) {
        if (HiveRuntime.blocksForeignSpawn(event.getEntityType(), event.getLevel(), event.getSpawnType(), event.getPos()))
            event.setResult(net.minecraftforge.eventbus.api.Event.Result.DENY);
    }
    private void hostileLoot(LivingDropsEvent event) {
        event.getDrops().removeIf(drop -> HiveRuntime.collectHunt(event.getEntity(), event.getSource(), drop.getItem()));
        for (var drop : event.getDrops()) HiveLoot.mark(event.getEntity(), drop.getItem());
    }
    private void registerCommands(RegisterCommandsEvent event) { HiveRuntime.registerCommands(event.getDispatcher()); }
    private void serverStarted(ServerStartedEvent event) { HiveRuntime.load(event.getServer()); }
    private void serverStopping(ServerStoppingEvent event) { HiveRuntime.stop(event.getServer()); }
    private void serverTick(TickEvent.ServerTickEvent event) { if (event.phase == TickEvent.Phase.END) HiveRuntime.tick(event.getServer()); }
    private void entityJoin(EntityJoinLevelEvent event) {
        if (event.getLevel() instanceof ServerLevel level) HiveRuntime.onEntityLoad(event.getEntity(), level);
    }
    private void structureDamage(net.minecraftforge.event.level.BlockEvent.BreakEvent event) {
        if (!event.isCanceled() && event.getLevel() instanceof ServerLevel level) HiveRuntime.onStructureDamage(level, event.getPlayer(), event.getPos(), event.getState());
    }
    private void livingDeath(LivingDeathEvent event) { if (event.getEntity().level() instanceof ServerLevel) HiveRuntime.onDeath(event.getEntity(), event.getSource()); }
}
