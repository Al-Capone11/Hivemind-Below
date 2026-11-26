package com.hivemind.fabric.content;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.LootableContainerBlockEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.GenericContainerScreenHandler;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;

public final class HiveStorageEntity extends LootableContainerBlockEntity {
    private DefaultedList<ItemStack> items = DefaultedList.ofSize(27, ItemStack.EMPTY);

    public HiveStorageEntity(BlockPos pos, BlockState state) { super(HivemindContent.STORAGE_ENTITY, pos, state); }

    @Override public int size() { return items.size(); }
    @Override protected DefaultedList<ItemStack> getInvStackList() { return items; }
    @Override protected void setInvStackList(DefaultedList<ItemStack> items) { this.items = items; }
    @Override protected Text getContainerName() { return Text.translatable("block.hivemind.hive_storage"); }
    @Override protected ScreenHandler createScreenHandler(int id, PlayerInventory player) { return GenericContainerScreenHandler.createGeneric9x3(id, player, this); }

    @Override
    public void readNbt(NbtCompound input) {
        super.readNbt(input);
        items = DefaultedList.ofSize(27, ItemStack.EMPTY);
        Inventories.readNbt(input, items);
    }

    @Override
    protected void writeNbt(NbtCompound output) {
        super.writeNbt(output);
        Inventories.writeNbt(output, items);
    }
}
