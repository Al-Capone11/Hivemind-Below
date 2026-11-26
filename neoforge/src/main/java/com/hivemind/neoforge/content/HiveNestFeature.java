package com.hivemind.neoforge.content;

import com.hivemind.core.NestLayout;
import com.hivemind.core.OrganicNest;
import com.hivemind.neoforge.runtime.HiveRuntime;
import java.util.List;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.core.BlockPos;
import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.util.RandomSource;
import net.minecraft.core.Direction;

final class HiveNestFeature implements Feature {
    static final MapCodec<HiveNestFeature> CODEC = MapCodec.unit(HiveNestFeature::new);

    @Override
    public MapCodec<HiveNestFeature> codec() { return CODEC; }

    @Override
    public boolean place(WorldGenLevel level, ChunkGenerator generator, RandomSource random, BlockPos seed) {
        if (!HiveRuntime.naturalNestsEnabled(level.getServer()) || level.canSeeSky(seed) || !level.getBlockState(seed).isAir()) return false;
        BlockPos origin = seed.below();
        var plan = OrganicNest.FULL.plan;
        if (!exitOpen(level, origin, 16) || !fits(level, origin, plan)) {
            plan = OrganicNest.SMALL.plan;
            if (!exitOpen(level, origin, 4) || !fits(level, origin, plan)) return false;
        }
        for (var placement : plan) {
            var cell = placement.position();
            var block = switch (placement.material()) {
                case AIR -> Blocks.AIR;
                case CHITIN -> HivemindContent.HIVE_CHITIN.get();
                case BROOD_COMB -> HivemindContent.BROOD_COMB.get();
                case RESERVE_CYST -> HivemindContent.RESERVE_CYST.get();
                case MOLT_BED -> HivemindContent.MOLT_BED.get();
                case ALARM_RIDGE -> HivemindContent.ALARM_RIDGE.get();
                case CORE -> HivemindContent.HIVE_CORE.get();
                case STORAGE -> HivemindContent.HIVE_STORAGE.get();
            };
            level.setBlock(origin.offset(cell.x(), cell.y(), cell.z()), block.defaultBlockState(), 2);
        }
        return level.setBlock(seed, HivemindContent.HIVE_SEED.get().defaultBlockState(), 2);
    }

    private static boolean fits(WorldGenLevel level, BlockPos origin, List<NestLayout.Placement> plan) {
        int free = 0, interior = 0;
        for (var placement : plan) {
            var part = placement.position();
            var cell = origin.offset(part.x(), part.y(), part.z());
            if (level.isOutsideBuildHeight(cell.getY()) || !level.ensureCanWrite(cell)) return false;
            var state = level.getBlockState(cell);
            if (!state.isAir() && (!HiveRuntime.excavationMaterial(state) || !level.getFluidState(cell).isEmpty())) return false;
            if (placement.material() == NestLayout.Material.AIR) {
                interior++;
                if (state.isAir()) free++;
            }
        }
        return free * 3 >= interior;
    }

    private static boolean exitOpen(WorldGenLevel level, BlockPos origin, int start) {
        for (int z = start; z < start + 3; z++) {
            var support = origin.offset(0, 0, z);
            if (!level.ensureCanWrite(support) || !level.getBlockState(support).isFaceSturdy(level, support, Direction.UP)) return false;
            for (int y = 1; y <= 3; y++) {
                var cell = origin.offset(0, y, z);
                if (!level.ensureCanWrite(cell) || !level.getBlockState(cell).isAir()) return false;
            }
        }
        return true;
    }
}
