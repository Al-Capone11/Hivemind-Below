package com.hivemind.fabric.content;

import com.hivemind.core.NestLayout;
import com.hivemind.core.OrganicNest;
import com.hivemind.fabric.runtime.HiveRuntime;
import java.util.List;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.block.Blocks;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.world.gen.feature.util.FeatureContext;
import net.minecraft.util.math.Direction;

final class HiveNestFeature extends Feature<DefaultFeatureConfig> {
    HiveNestFeature() { super(DefaultFeatureConfig.CODEC); }

    @Override
    public boolean generate(FeatureContext<DefaultFeatureConfig> context) {
        var level = context.getWorld();
        BlockPos seed = context.getOrigin();
        if (!HiveRuntime.naturalNestsEnabled(level.getServer()) || level.isSkyVisible(seed) || !level.getBlockState(seed).isAir()) return false;
        BlockPos origin = seed.down();
        var plan = OrganicNest.FULL.plan;
        if (!exitOpen(level, origin, 16) || !fits(level, origin, plan)) {
            plan = OrganicNest.SMALL.plan;
            if (!exitOpen(level, origin, 4) || !fits(level, origin, plan)) return false;
        }
        for (var placement : plan) {
            var cell = placement.position();
            var block = switch (placement.material()) {
                case AIR -> Blocks.AIR;
                case CHITIN -> HivemindContent.HIVE_CHITIN;
                case BROOD_COMB -> HivemindContent.BROOD_COMB;
                case RESERVE_CYST -> HivemindContent.RESERVE_CYST;
                case MOLT_BED -> HivemindContent.MOLT_BED;
                case ALARM_RIDGE -> HivemindContent.ALARM_RIDGE;
                case CORE -> HivemindContent.HIVE_CORE;
                case STORAGE -> HivemindContent.HIVE_STORAGE;
            };
            level.setBlockState(origin.add(cell.x(), cell.y(), cell.z()), block.getDefaultState(), 2);
        }
        return level.setBlockState(seed, HivemindContent.HIVE_SEED.getDefaultState(), 2);
    }

    private static boolean fits(StructureWorldAccess level, BlockPos origin, List<NestLayout.Placement> plan) {
        int free = 0, interior = 0;
        for (var placement : plan) {
            var part = placement.position();
            var cell = origin.add(part.x(), part.y(), part.z());
            if (level.isOutOfHeightLimit(cell.getY()) || !level.isValidForSetBlock(cell)) return false;
            var state = level.getBlockState(cell);
            if (!state.isAir() && (!HiveRuntime.excavationMaterial(state) || !level.getFluidState(cell).isEmpty())) return false;
            if (placement.material() == NestLayout.Material.AIR) {
                interior++;
                if (state.isAir()) free++;
            }
        }
        return free * 3 >= interior;
    }

    private static boolean exitOpen(StructureWorldAccess level, BlockPos origin, int start) {
        for (int z = start; z < start + 3; z++) {
            var support = origin.add(0, 0, z);
            if (!level.isValidForSetBlock(support) || !level.getBlockState(support).isSideSolidFullSquare(level, support, Direction.UP)) return false;
            for (int y = 1; y <= 3; y++) {
                var cell = origin.add(0, y, z);
                if (!level.isValidForSetBlock(cell) || !level.getBlockState(cell).isAir()) return false;
            }
        }
        return true;
    }
}
