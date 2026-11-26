package com.hivemind.fabric.content;

import com.hivemind.core.Caste;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.animal.bee.Bee;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.entity.monster.spider.CaveSpider;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;

public final class HivemindContent {
    public static final EntityType<HivemindQueen> QUEEN = entity("queen", HivemindQueen::new, 2.2f, 2.85f);
    public static final EntityType<HivemindScout> SCOUT = entity("scout", HivemindScout::new, 0.7f, 0.7f);
    public static final EntityType<HivemindWarrior> WARRIOR = entity("warrior", HivemindWarrior::new, 1.2f, 1.0f);
    public static final EntityType<HivemindWorker> WORKER = entity("worker", HivemindWorker::new, 0.9f, 0.8f);
    public static final Block HIVE_CHITIN = block("hive_chitin", BlockBehaviour.Properties.of().strength(2.5f).sound(SoundType.HONEY_BLOCK));
    public static final Block BROOD_COMB = block("brood_comb", BlockBehaviour.Properties.of().strength(2.5f).sound(SoundType.HONEY_BLOCK));
    public static final Block MOLT_BED = block("molt_bed", BlockBehaviour.Properties.of().strength(2.5f).sound(SoundType.HONEY_BLOCK));
    public static final Block ALARM_RIDGE = block("alarm_ridge", BlockBehaviour.Properties.of().strength(2.5f).sound(SoundType.HONEY_BLOCK));
    public static final Block HIVE_CORE = block("hive_core", BlockBehaviour.Properties.of().strength(3.5f).sound(SoundType.HONEY_BLOCK).noOcclusion().lightLevel(state -> 4));
    public static final Block HIVE_SEED = registerSeed();
    public static final Block HIVE_BROOD = block("hive_brood", BlockBehaviour.Properties.of().noCollision().noOcclusion().strength(0.3f));
    public static final Block RESERVE_CYST = block("reserve_cyst", BlockBehaviour.Properties.of().noCollision().noOcclusion().strength(0.3f));
    public static final Block HIVE_STORAGE = storageBlock();
    public static final net.minecraft.world.level.block.entity.BlockEntityType<HiveStorageEntity> STORAGE_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE,
            Identifier.fromNamespaceAndPath("hivemind", "hive_storage"), new net.minecraft.world.level.block.entity.BlockEntityType<>(HiveStorageEntity::new, java.util.Set.of(HIVE_STORAGE)));
    public static final Item QUEEN_EGG = item("queen_spawn_egg", QUEEN);
    public static final Item SCOUT_EGG = item("scout_spawn_egg", SCOUT);
    public static final Item WARRIOR_EGG = item("warrior_spawn_egg", WARRIOR);
    public static final Item WORKER_EGG = item("worker_spawn_egg", WORKER);

    private HivemindContent() {}

    private static <T extends Entity> EntityType<T> entity(String name, EntityType.EntityFactory<T> factory, float width, float height) {
        Identifier id = Identifier.fromNamespaceAndPath("hivemind", name);
        ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, id);
        EntityType<T> type = EntityType.Builder.of(factory, MobCategory.MONSTER)
                .sized(width, height).clientTrackingRange(10).build(key);
        return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, type);
    }

    private static Block block(String name, BlockBehaviour.Properties properties) {
        Identifier id = Identifier.fromNamespaceAndPath("hivemind", name);
        Block block = new Block(properties.setId(ResourceKey.create(Registries.BLOCK, id)));
        Registry.register(BuiltInRegistries.BLOCK, id, block);
        Registry.register(BuiltInRegistries.ITEM, id, new BlockItem(block, new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id))));
        return block;
    }

    private static Item item(String name, EntityType<?> entity) {
        Identifier id = Identifier.fromNamespaceAndPath("hivemind", name);
        return Registry.register(BuiltInRegistries.ITEM, id, new SpawnEggItem(new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id)).spawnEgg(entity)));
    }

    private static Block storageBlock() {
        Identifier id = Identifier.fromNamespaceAndPath("hivemind", "hive_storage");
        var blockKey = ResourceKey.create(Registries.BLOCK, id);
        Block block = new HiveStorageBlock(BlockBehaviour.Properties.of().setId(blockKey).strength(2.5f).noOcclusion());
        Registry.register(BuiltInRegistries.BLOCK, blockKey, block);
        Registry.register(BuiltInRegistries.ITEM, id, new BlockItem(block, new Item.Properties().setId(ResourceKey.create(Registries.ITEM, id))));
        return block;
    }

    private static Block registerSeed() {
        var key = ResourceKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath("hivemind", "hive_seed"));
        return Registry.register(BuiltInRegistries.BLOCK, key,
                new HiveSeedBlock(BlockBehaviour.Properties.of().setId(key).randomTicks().strength(0.6f)));
    }

    public static void initialize() {
        Registry.register(BuiltInRegistries.FEATURE_TYPE, Identifier.fromNamespaceAndPath("hivemind", "cave_hive"), HiveNestFeature.CODEC);
        net.fabricmc.fabric.api.biome.v1.BiomeModifications.addFeature(
                net.fabricmc.fabric.api.biome.v1.BiomeSelectors.foundInOverworld(),
                net.minecraft.world.level.levelgen.GenerationStep.Decoration.UNDERGROUND_DECORATION, ResourceKey.create(Registries.PLACED_FEATURE, Identifier.fromNamespaceAndPath("hivemind", "hive_seed")));
        FabricDefaultAttributeRegistry.register(QUEEN, HivemindQueen.createHiveAttributes());
        FabricDefaultAttributeRegistry.register(SCOUT, HivemindScout.createHiveAttributes());
        FabricDefaultAttributeRegistry.register(WARRIOR, HivemindWarrior.createHiveAttributes());
        FabricDefaultAttributeRegistry.register(WORKER, HivemindWorker.createHiveAttributes());
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, Identifier.fromNamespaceAndPath("hivemind", "main"),
                net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab.builder().title(Component.translatable("itemGroup.hivemind.main"))
                        .icon(() -> new ItemStack(HIVE_CORE)).displayItems((parameters, output) -> {
                            output.accept(HIVE_CHITIN); output.accept(BROOD_COMB); output.accept(MOLT_BED); output.accept(ALARM_RIDGE); output.accept(HIVE_CORE); output.accept(HIVE_BROOD); output.accept(RESERVE_CYST); output.accept(HIVE_STORAGE);
                            output.accept(QUEEN_EGG); output.accept(SCOUT_EGG); output.accept(WARRIOR_EGG); output.accept(WORKER_EGG);
                        }).build());
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.NATURAL_BLOCKS).register(output -> {
            output.accept(HIVE_CHITIN); output.accept(BROOD_COMB); output.accept(MOLT_BED); output.accept(ALARM_RIDGE);
            output.accept(HIVE_CORE);
            output.accept(HIVE_BROOD); output.accept(RESERVE_CYST);
            output.accept(QUEEN_EGG);
            output.accept(SCOUT_EGG);
            output.accept(WARRIOR_EGG);
            output.accept(WORKER_EGG);
        });
    }

    public static EntityType<? extends Entity> type(Caste caste) {
        return switch (caste) {
            case QUEEN -> QUEEN;
            case SCOUT -> SCOUT;
            case WARRIOR -> WARRIOR;
            case WORKER -> WORKER;
        };
    }
}
