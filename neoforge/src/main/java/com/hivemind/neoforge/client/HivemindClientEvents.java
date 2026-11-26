package com.hivemind.neoforge.client;
import com.hivemind.neoforge.content.HivemindContent;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
@EventBusSubscriber(modid = "hivemind", value = Dist.CLIENT)
public final class HivemindClientEvents {
    @SubscribeEvent public static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(HivemindContent.QUEEN.get(), context -> new HiveRenderer<>(context, "queen"));
        event.registerEntityRenderer(HivemindContent.SCOUT.get(), context -> new HiveRenderer<>(context, "scout"));
        event.registerEntityRenderer(HivemindContent.WARRIOR.get(), context -> new HiveRenderer<>(context, "warrior"));
        event.registerEntityRenderer(HivemindContent.WORKER.get(), context -> new HiveRenderer<>(context, "worker"));
    }
}
