package com.hivemind.neoforge.client;
import com.hivemind.neoforge.content.HivemindContent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.client.event.EntityRenderersEvent;
@EventBusSubscriber(modid = "hivemind", bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class HivemindClientEvents {
    @SubscribeEvent public static void renderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(HivemindContent.QUEEN.get(), context -> new HiveRenderer<>(context, "queen"));
        event.registerEntityRenderer(HivemindContent.SCOUT.get(), context -> new HiveRenderer<>(context, "scout"));
        event.registerEntityRenderer(HivemindContent.WARRIOR.get(), context -> new HiveRenderer<>(context, "warrior"));
        event.registerEntityRenderer(HivemindContent.WORKER.get(), context -> new HiveRenderer<>(context, "worker"));
    }
}