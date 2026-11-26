package com.hivemind.neoforge.content;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.Containers;

final class HiveStorageBlock extends BaseEntityBlock {
    private static final net.minecraft.world.phys.shapes.VoxelShape POD = net.minecraft.world.phys.shapes.Shapes.or(
            Block.box(2, 0, 2, 14, 3, 14), Block.box(2, 3, 2, 14, 12, 14),
            Block.box(4, 12, 4, 12, 15, 12), Block.box(6, 15, 6, 10, 16, 10));

    @Override public net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter world, BlockPos pos, net.minecraft.world.phys.shapes.CollisionContext context) {
        return POD;
    }

    HiveStorageBlock(Properties settings) { super(settings); }

    private static final com.mojang.serialization.MapCodec<HiveStorageBlock> CODEC = simpleCodec(HiveStorageBlock::new);
    @Override protected com.mojang.serialization.MapCodec<HiveStorageBlock> codec() { return CODEC; }

    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new HiveStorageEntity(pos, state); }
    @Override public RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level world, BlockPos pos, Player player, BlockHitResult hit) {
        if (world instanceof net.minecraft.server.level.ServerLevel && world.getBlockEntity(pos) instanceof HiveStorageEntity storage) player.openMenu(storage);
        return InteractionResult.SUCCESS;
    }

}
