package com.hivemind.neoforge.content;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.network.chat.Component;
import net.minecraft.core.NonNullList;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

public final class HiveStorageEntity extends RandomizableContainerBlockEntity {
    private NonNullList<ItemStack> items = NonNullList.withSize(27, ItemStack.EMPTY);

    public HiveStorageEntity(BlockPos pos, BlockState state) { super(HivemindContent.STORAGE_ENTITY.get(), pos, state); }

    @Override public int getContainerSize() { return items.size(); }
    @Override protected NonNullList<ItemStack> getItems() { return items; }
    @Override protected void setItems(NonNullList<ItemStack> items) { this.items = items; }
    @Override protected Component getDefaultName() { return Component.translatable("block.hivemind.hive_storage"); }
    @Override protected AbstractContainerMenu createMenu(int id, Inventory player) { return ChestMenu.threeRows(id, player, this); }

    @Override
    public void load(CompoundTag input) {
        super.load(input);
        items = NonNullList.withSize(27, ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, items);
    }

    @Override
    protected void saveAdditional(CompoundTag output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items);
    }
}
