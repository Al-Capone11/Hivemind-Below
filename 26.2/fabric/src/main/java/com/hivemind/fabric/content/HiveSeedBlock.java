package com.hivemind.fabric.content;

import com.hivemind.fabric.runtime.HiveRuntime;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;

final class HiveSeedBlock extends Block {
    HiveSeedBlock(Properties properties) { super(properties); }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        HiveRuntime.activateNaturalNest(level, pos);
    }
}
