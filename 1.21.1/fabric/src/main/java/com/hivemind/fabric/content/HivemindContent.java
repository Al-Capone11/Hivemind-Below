package com.hivemind.fabric.content;

import com.hivemind.core.Caste;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.AbstractBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.entity.passive.BeeEntity;
import net.minecraft.entity.mob.CaveSpiderEntity;
import net.minecraft.entity.mob.SilverfishEntity;
import net.minecraft.entity.mob.ZombieEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.SpawnEggItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public final class HivemindContent {
    public static final EntityType<HivemindQueen> QUEEN = entity("queen", HivemindQueen::new, 2.2f, 2.85f);
    public static final EntityType<HivemindScout> SCOUT = entity("scout", HivemindScout::new, 0.7f, 0.7f);
    public static final EntityType<HivemindWarrior> WARRIOR = entity("warrior", HivemindWarrior::new, 1.2f, 1.0f);
    public static final EntityType<HivemindWorker> WORKER = entity("worker", HivemindWorker::new, 0.9f, 0.8f);
    public static final Block HIVE_CHITIN = block("hive_chitin", new Block(AbstractBlock.Settings.copy(Blocks.HONEYCOMB_BLOCK).strength(2.5f)));
    public static final Block BROOD_COMB = block("brood_comb", new Block(AbstractBlock.Settings.copy(Blocks.HONEYCOMB_BLOCK).strength(2.5f)));
    public static final Block MOLT_BED = block("molt_bed", new Block(AbstractBlock.Settings.copy(Blocks.HONEYCOMB_BLOCK).strength(2.5f)));
    public static final Block ALARM_RIDGE = block("alarm_ridge", new Block(AbstractBlock.Settings.copy(Blocks.HONEYCOMB_BLOCK).strength(2.5f)));
    public static final Block HIVE_CORE = block("hive_core", new Block(AbstractBlock.Settings.copy(Blocks.HONEYCOMB_BLOCK).strength(3.5f).nonOpaque().luminance(state -> 4)));
    public static final Block HIVE_SEED = Registry.register(Registries.BLOCK, Identifier.of("hivemind", "hive_seed"),
            new HiveSeedBlock(AbstractBlock.Settings.create().ticksRandomly().strength(0.6f)));
    public static final Block HIVE_BROOD = block("hive_brood", new Block(AbstractBlock.Settings.create().noCollision().nonOpaque().strength(0.3f)));
    public static final Block RESERVE_CYST = block("reserve_cyst", new Block(AbstractBlock.Settings.create().noCollision().nonOpaque().strength(0.3f)));
    public static final Block HIVE_STORAGE = block("hive_storage", new HiveStorageBlock(AbstractBlock.Settings.create().strength(2.5f).nonOpaque()));
    public static final net.minecraft.block.entity.BlockEntityType<HiveStorageEntity> STORAGE_ENTITY = Registry.register(Registries.BLOCK_ENTITY_TYPE,
            Identifier.of("hivemind", "hive_storage"), net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder.create(HiveStorageEntity::new, HIVE_STORAGE).build());
    public static final Item QUEEN_EGG = egg("queen_spawn_egg", QUEEN, 0x39211e, 0xd59a54);
    public static final Item SCOUT_EGG = egg("scout_spawn_egg", SCOUT, 0x72756a, 0xe1c98c);
    public static final Item WARRIOR_EGG = egg("warrior_spawn_egg", WARRIOR, 0x352a29, 0xa86c49);
    public static final Item WORKER_EGG = egg("worker_spawn_egg", WORKER, 0x8f6b3d, 0xe5be70);

    private HivemindContent() {}
    private static <T extends Entity> EntityType<T> entity(String name, EntityType.EntityFactory<T> factory, float width, float height) {
        Identifier id = Identifier.of("hivemind", name);
        return Registry.register(Registries.ENTITY_TYPE, id, EntityType.Builder.create(factory, SpawnGroup.MONSTER).dimensions(width, height).maxTrackingRange(10).build(id.toString()));
    }
    private static Block block(String name, Block block) {
        Identifier id = Identifier.of("hivemind", name);
        Registry.register(Registries.BLOCK, id, block);
        Registry.register(Registries.ITEM, id, new BlockItem(block, new Item.Settings()));
        return block;
    }
    private static Item egg(String name, EntityType<? extends net.minecraft.entity.mob.MobEntity> type, int shell, int spots) {
        return Registry.register(Registries.ITEM, Identifier.of("hivemind", name), new SpawnEggItem(type, shell, spots, new Item.Settings()));
    }
    public static void initialize() {
        Registry.register(Registries.FEATURE, Identifier.of("hivemind", "cave_hive"), new HiveNestFeature());
        net.fabricmc.fabric.api.biome.v1.BiomeModifications.addFeature(
                net.fabricmc.fabric.api.biome.v1.BiomeSelectors.foundInOverworld(),
                net.minecraft.world.gen.GenerationStep.Feature.UNDERGROUND_DECORATION, net.minecraft.registry.RegistryKey.of(net.minecraft.registry.RegistryKeys.PLACED_FEATURE, Identifier.of("hivemind", "hive_seed")));
        FabricDefaultAttributeRegistry.register(QUEEN, HivemindQueen.createHiveAttributes());
        FabricDefaultAttributeRegistry.register(SCOUT, HivemindScout.createHiveAttributes());
        FabricDefaultAttributeRegistry.register(WARRIOR, HivemindWarrior.createHiveAttributes());
        FabricDefaultAttributeRegistry.register(WORKER, HivemindWorker.createHiveAttributes());
        Registry.register(Registries.ITEM_GROUP, Identifier.of("hivemind", "main"), FabricItemGroup.builder()
                .displayName(Text.translatable("itemGroup.hivemind.main")).icon(() -> new ItemStack(HIVE_CORE))
                .entries((context, entries) -> {
                    entries.add(HIVE_CHITIN); entries.add(BROOD_COMB); entries.add(MOLT_BED); entries.add(ALARM_RIDGE); entries.add(HIVE_CORE); entries.add(HIVE_BROOD); entries.add(RESERVE_CYST); entries.add(HIVE_STORAGE);
                    entries.add(QUEEN_EGG); entries.add(SCOUT_EGG); entries.add(WARRIOR_EGG); entries.add(WORKER_EGG);
                }).build());
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.NATURAL).register(entries -> {
            entries.add(HIVE_CHITIN); entries.add(BROOD_COMB); entries.add(MOLT_BED); entries.add(ALARM_RIDGE); entries.add(HIVE_CORE); entries.add(HIVE_BROOD); entries.add(RESERVE_CYST);
            entries.add(QUEEN_EGG); entries.add(SCOUT_EGG); entries.add(WARRIOR_EGG); entries.add(WORKER_EGG);
        });
    }
    public static EntityType<? extends Entity> type(Caste caste) {
        return switch (caste) { case QUEEN -> QUEEN; case SCOUT -> SCOUT; case WARRIOR -> WARRIOR; case WORKER -> WORKER; };
    }
}
