package com.hivemind.neoforge.content;

import com.hivemind.core.Caste;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.animal.bee.Bee;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.entity.monster.spider.CaveSpider;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;

public final class HivemindContent {
    public static final DeferredRegister.Entities ENTITY_TYPES = DeferredRegister.createEntities("hivemind");
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks("hivemind");
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems("hivemind");
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, "hivemind");

    public static final DeferredHolder<EntityType<?>, EntityType<HivemindQueen>> QUEEN = ENTITY_TYPES.registerEntityType("queen", HivemindQueen::new, MobCategory.MONSTER, b -> b.sized(2.2f, 2.85f).clientTrackingRange(10));
    public static final DeferredHolder<EntityType<?>, EntityType<HivemindScout>> SCOUT = ENTITY_TYPES.registerEntityType("scout", HivemindScout::new, MobCategory.MONSTER, b -> b.sized(0.7f, 0.7f).clientTrackingRange(10));
    public static final DeferredHolder<EntityType<?>, EntityType<HivemindWarrior>> WARRIOR = ENTITY_TYPES.registerEntityType("warrior", HivemindWarrior::new, MobCategory.MONSTER, b -> b.sized(1.2f, 1.0f).clientTrackingRange(10));
    public static final DeferredHolder<EntityType<?>, EntityType<HivemindWorker>> WORKER = ENTITY_TYPES.registerEntityType("worker", HivemindWorker::new, MobCategory.MONSTER, b -> b.sized(0.9f, 0.8f).clientTrackingRange(10));
    public static final DeferredBlock<Block> HIVE_CHITIN = BLOCKS.registerBlock("hive_chitin", Block::new, p -> p.strength(2.5f).sound(SoundType.HONEY_BLOCK));
    public static final DeferredBlock<Block> BROOD_COMB = BLOCKS.registerBlock("brood_comb", Block::new, p -> p.strength(2.5f).sound(SoundType.HONEY_BLOCK));
    public static final DeferredBlock<Block> MOLT_BED = BLOCKS.registerBlock("molt_bed", Block::new, p -> p.strength(2.5f).sound(SoundType.HONEY_BLOCK));
    public static final DeferredBlock<Block> ALARM_RIDGE = BLOCKS.registerBlock("alarm_ridge", Block::new, p -> p.strength(2.5f).sound(SoundType.HONEY_BLOCK));
    public static final DeferredBlock<Block> HIVE_CORE = BLOCKS.registerBlock("hive_core", Block::new, p -> p.strength(3.5f).sound(SoundType.HONEY_BLOCK).noOcclusion().lightLevel(state -> 4));
    public static final DeferredBlock<Block> HIVE_SEED = BLOCKS.registerBlock("hive_seed", HiveSeedBlock::new, p -> p.randomTicks().strength(0.6f));
    public static final DeferredBlock<Block> HIVE_BROOD = BLOCKS.registerBlock("hive_brood", Block::new, p -> p.noCollision().noOcclusion().strength(0.3f));
    public static final DeferredBlock<Block> RESERVE_CYST = BLOCKS.registerBlock("reserve_cyst", Block::new, p -> p.noCollision().noOcclusion().strength(0.3f));
    public static final DeferredBlock<Block> HIVE_STORAGE = BLOCKS.registerBlock("hive_storage", HiveStorageBlock::new, p -> p.strength(2.5f).noOcclusion());
    private static final DeferredRegister<net.minecraft.world.level.block.entity.BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, "hivemind");
    public static final DeferredHolder<net.minecraft.world.level.block.entity.BlockEntityType<?>, net.minecraft.world.level.block.entity.BlockEntityType<HiveStorageEntity>> STORAGE_ENTITY = BLOCK_ENTITIES.register("hive_storage", () -> new net.minecraft.world.level.block.entity.BlockEntityType<>(HiveStorageEntity::new, java.util.Set.of(HIVE_STORAGE.get())));
    private static final DeferredItem<net.minecraft.world.item.BlockItem> STORAGE_ITEM = ITEMS.registerSimpleBlockItem(HIVE_STORAGE);
    public static final DeferredItem<net.minecraft.world.item.BlockItem> CHITIN_ITEM = ITEMS.registerSimpleBlockItem(HIVE_CHITIN);
    public static final DeferredItem<net.minecraft.world.item.BlockItem> BROOD_COMB_ITEM = ITEMS.registerSimpleBlockItem(BROOD_COMB);
    public static final DeferredItem<net.minecraft.world.item.BlockItem> MOLT_BED_ITEM = ITEMS.registerSimpleBlockItem(MOLT_BED);
    public static final DeferredItem<net.minecraft.world.item.BlockItem> ALARM_RIDGE_ITEM = ITEMS.registerSimpleBlockItem(ALARM_RIDGE);
    public static final DeferredItem<net.minecraft.world.item.BlockItem> CORE_ITEM = ITEMS.registerSimpleBlockItem(HIVE_CORE);
    public static final DeferredItem<net.minecraft.world.item.BlockItem> BROOD_ITEM = ITEMS.registerSimpleBlockItem(HIVE_BROOD);
    public static final DeferredItem<net.minecraft.world.item.BlockItem> RESERVE_CYST_ITEM = ITEMS.registerSimpleBlockItem(RESERVE_CYST);
    public static final DeferredItem<Item> QUEEN_EGG = egg("queen_spawn_egg", QUEEN);
    public static final DeferredItem<Item> SCOUT_EGG = egg("scout_spawn_egg", SCOUT);
    public static final DeferredItem<Item> WARRIOR_EGG = egg("warrior_spawn_egg", WARRIOR);
    public static final DeferredItem<Item> WORKER_EGG = egg("worker_spawn_egg", WORKER);

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> HIVE_TAB = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.hivemind.main")).icon(() -> new ItemStack(HIVE_CORE.get()))
            .displayItems((parameters, output) -> {
                output.accept(HIVE_CHITIN.get()); output.accept(BROOD_COMB.get()); output.accept(MOLT_BED.get()); output.accept(ALARM_RIDGE.get()); output.accept(HIVE_CORE.get()); output.accept(HIVE_BROOD.get()); output.accept(RESERVE_CYST.get()); output.accept(STORAGE_ITEM.get());
                output.accept(QUEEN_EGG.get()); output.accept(SCOUT_EGG.get()); output.accept(WARRIOR_EGG.get()); output.accept(WORKER_EGG.get());
            }).build());
    private static final DeferredRegister<com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.levelgen.feature.Feature>> FEATURES = DeferredRegister.create(BuiltInRegistries.FEATURE_TYPE, "hivemind");
    static { FEATURES.register("cave_hive", () -> HiveNestFeature.CODEC); }

    private HivemindContent() {}

    private static DeferredItem<Item> egg(String name, DeferredHolder<EntityType<?>, ? extends EntityType<? extends Entity>> type) {
        return ITEMS.registerItem(name, properties -> new SpawnEggItem(properties.spawnEgg(type.get())));
    }

    public static void register(IEventBus modBus) {
        BLOCK_ENTITIES.register(modBus);
        FEATURES.register(modBus);
        ENTITY_TYPES.register(modBus);
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        TABS.register(modBus);
        HivemindSounds.SOUNDS.register(modBus);
        modBus.addListener(HivemindContent::registerAttributes);
        modBus.addListener(HivemindContent::addCreativeItems);
    }

    private static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(QUEEN.get(), HivemindQueen.createHiveAttributes().build());
        event.put(SCOUT.get(), HivemindScout.createHiveAttributes().build());
        event.put(WARRIOR.get(), HivemindWarrior.createHiveAttributes().build());
        event.put(WORKER.get(), HivemindWorker.createHiveAttributes().build());
    }

    private static void addCreativeItems(BuildCreativeModeTabContentsEvent event) {
        if (!event.getTabKey().equals(CreativeModeTabs.NATURAL_BLOCKS)) return;
        event.accept(HIVE_CHITIN.get()); event.accept(BROOD_COMB.get()); event.accept(MOLT_BED.get()); event.accept(ALARM_RIDGE.get());
        event.accept(HIVE_CORE.get());
        event.accept(HIVE_BROOD.get()); event.accept(RESERVE_CYST.get());
        event.accept(QUEEN_EGG.get());
        event.accept(SCOUT_EGG.get());
        event.accept(WARRIOR_EGG.get());
        event.accept(WORKER_EGG.get());
    }

    public static EntityType<? extends Entity> type(Caste caste) {
        return switch (caste) {
            case QUEEN -> QUEEN.get();
            case SCOUT -> SCOUT.get();
            case WARRIOR -> WARRIOR.get();
            case WORKER -> WORKER.get();
        };
    }
}
