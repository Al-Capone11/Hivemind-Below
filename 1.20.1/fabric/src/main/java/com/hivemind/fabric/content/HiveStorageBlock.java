package com.hivemind.fabric.content;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.BlockWithEntity;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.Hand;

final class HiveStorageBlock extends BlockWithEntity {
    private static final net.minecraft.util.shape.VoxelShape POD = net.minecraft.util.shape.VoxelShapes.union(
            Block.createCuboidShape(2, 0, 2, 14, 3, 14), Block.createCuboidShape(2, 3, 2, 14, 12, 14),
            Block.createCuboidShape(4, 12, 4, 12, 15, 12), Block.createCuboidShape(6, 15, 6, 10, 16, 10));

    @Override public net.minecraft.util.shape.VoxelShape getOutlineShape(BlockState state, net.minecraft.world.BlockView world, BlockPos pos, net.minecraft.block.ShapeContext context) {
        return POD;
    }

    HiveStorageBlock(Settings settings) { super(settings); }

    @Override public BlockEntity createBlockEntity(BlockPos pos, BlockState state) { return new HiveStorageEntity(pos, state); }
    @Override public BlockRenderType getRenderType(BlockState state) { return BlockRenderType.MODEL; }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (!(world.isClient) && world.getBlockEntity(pos) instanceof HiveStorageEntity storage) player.openHandledScreen(storage);
        return ActionResult.success(world.isClient);
    }

    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState replacement, boolean moved) {
        if (!state.isOf(replacement.getBlock()) && world.getBlockEntity(pos) instanceof HiveStorageEntity storage) ItemScatterer.spawn(world, pos, storage);
        super.onStateReplaced(state, world, pos, replacement, moved);
    }
}
