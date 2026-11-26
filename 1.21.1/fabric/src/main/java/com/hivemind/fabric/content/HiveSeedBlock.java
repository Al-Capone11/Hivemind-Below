package com.hivemind.fabric.content;

import com.hivemind.fabric.runtime.HiveRuntime;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;

final class HiveSeedBlock extends Block {
    HiveSeedBlock(Settings properties) { super(properties); }

    @Override
    public void randomTick(BlockState state, ServerWorld level, BlockPos pos, Random random) {
        HiveRuntime.activateNaturalNest(level, pos);
    }
}
