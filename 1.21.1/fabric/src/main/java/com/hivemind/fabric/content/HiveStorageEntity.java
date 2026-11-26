package com.hivemind.fabric.content;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.LockableContainerBlockEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.registry.RegistryWrapper;

public final class HiveStorageEntity extends LockableContainerBlockEntity {
    private DefaultedList<ItemStack> items = DefaultedList.ofSize(27, ItemStack.EMPTY);

    public HiveStorageEntity(BlockPos pos, BlockState state) { super(HivemindContent.STORAGE_ENTITY, pos, state); }

    @Override public int size() { return items.size(); }
    @Override protected DefaultedList<ItemStack> getHeldStacks() { return items; }
    @Override protected void setHeldStacks(DefaultedList<ItemStack> items) { this.items = items; }
    @Override protected Text getContainerName() { return Text.translatable("block.hivemind.hive_storage"); }
    @Override protected ScreenHandler createScreenHandler(int id, PlayerInventory player) { return GenericContainerScreenHandler.createGeneric9x3(id, player, this); }

    @Override
    protected void readNbt(NbtCompound input, RegistryWrapper.WrapperLookup registries) {
        super.readNbt(input, registries);
        items = DefaultedList.ofSize(27, ItemStack.EMPTY);
        Inventories.readNbt(input, items, registries);
    }

    @Override
    protected void writeNbt(NbtCompound output, RegistryWrapper.WrapperLookup registries) {
        super.writeNbt(output, registries);
        Inventories.writeNbt(output, items, registries);
    }
}
