package com.hivemind.fabric;

import com.hivemind.fabric.content.HivemindContent;
import com.hivemind.fabric.content.HivemindSounds;
import com.hivemind.fabric.runtime.HiveRuntime;
import net.fabricmc.api.ModInitializer;

public final class HivemindFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        HivemindSounds.initialize();
        HivemindContent.initialize();
        HiveRuntime.initialize();
    }
}
