package com.hivemind.fabric.client;
import com.hivemind.fabric.content.HivemindContent;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
public final class HivemindClient implements ClientModInitializer {
    @Override public void onInitializeClient() {
        EntityRendererRegistry.register(HivemindContent.QUEEN, context -> new HiveRenderer<>(context, "queen"));
        EntityRendererRegistry.register(HivemindContent.SCOUT, context -> new HiveRenderer<>(context, "scout"));
        EntityRendererRegistry.register(HivemindContent.WARRIOR, context -> new HiveRenderer<>(context, "warrior"));
        EntityRendererRegistry.register(HivemindContent.WORKER, context -> new HiveRenderer<>(context, "worker"));
    }
}
