package com.hivemind.fabric;

import net.fabricmc.api.ModInitializer;
import com.hivemind.fabric.content.HivemindContent;
import com.hivemind.fabric.content.HivemindSounds;
import com.hivemind.fabric.runtime.HiveRuntime;

public final class HivemindFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        HivemindSounds.initialize();
        HivemindContent.initialize();
        HiveRuntime.initialize();
    }
}
