package com.hivemind.neoforge.content;
import com.hivemind.core.Caste;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Bee;
import net.minecraft.world.entity.monster.CaveSpider;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import java.util.function.Supplier;
public final class HivemindContent {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, "hivemind");
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, "hivemind");
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, "hivemind");
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, "hivemind");
    public static final RegistryObject<EntityType<HivemindQueen>> QUEEN = ENTITY_TYPES.register("queen", () -> EntityType.Builder.of(HivemindQueen::new, MobCategory.MONSTER).sized(2.2f, 2.85f).clientTrackingRange(10).build("hivemind:queen"));
    public static final RegistryObject<EntityType<HivemindScout>> SCOUT = ENTITY_TYPES.register("scout", () -> EntityType.Builder.of(HivemindScout::new, MobCategory.MONSTER).sized(0.7f, 0.7f).clientTrackingRange(10).build("hivemind:scout"));
    public static final RegistryObject<EntityType<HivemindWarrior>> WARRIOR = ENTITY_TYPES.register("warrior", () -> EntityType.Builder.of(HivemindWarrior::new, MobCategory.MONSTER).sized(1.2f, 1.0f).clientTrackingRange(10).build("hivemind:warrior"));
    public static final RegistryObject<EntityType<HivemindWorker>> WORKER = ENTITY_TYPES.register("worker", () -> EntityType.Builder.of(HivemindWorker::new, MobCategory.MONSTER).sized(0.9f, 0.8f).clientTrackingRange(10).build("hivemind:worker"));
    public static final RegistryObject<Block> HIVE_CHITIN = BLOCKS.register("hive_chitin", () -> new Block(BlockBehaviour.Properties.of().strength(2.5f).sound(SoundType.HONEY_BLOCK)));
    public static final RegistryObject<Block> BROOD_COMB = BLOCKS.register("brood_comb", () -> new Block(BlockBehaviour.Properties.of().strength(2.5f).sound(SoundType.HONEY_BLOCK)));
    public static final RegistryObject<Block> MOLT_BED = BLOCKS.register("molt_bed", () -> new Block(BlockBehaviour.Properties.of().strength(2.5f).sound(SoundType.HONEY_BLOCK)));
    public static final RegistryObject<Block> ALARM_RIDGE = BLOCKS.register("alarm_ridge", () -> new Block(BlockBehaviour.Properties.of().strength(2.5f).sound(SoundType.HONEY_BLOCK)));
    public static final RegistryObject<Block> HIVE_CORE = BLOCKS.register("hive_core", () -> new Block(BlockBehaviour.Properties.of().strength(3.5f).sound(SoundType.HONEY_BLOCK).noOcclusion().lightLevel(s -> 4)));
    private static final RegistryObject<Item> CHITIN_ITEM = ITEMS.register("hive_chitin", () -> new BlockItem(HIVE_CHITIN.get(), new Item.Properties()));
    private static final RegistryObject<Item> BROOD_COMB_ITEM = ITEMS.register("brood_comb", () -> new BlockItem(BROOD_COMB.get(), new Item.Properties()));
    private static final RegistryObject<Item> MOLT_BED_ITEM = ITEMS.register("molt_bed", () -> new BlockItem(MOLT_BED.get(), new Item.Properties()));
    private static final RegistryObject<Item> ALARM_RIDGE_ITEM = ITEMS.register("alarm_ridge", () -> new BlockItem(ALARM_RIDGE.get(), new Item.Properties()));
    private static final RegistryObject<Item> CORE_ITEM = ITEMS.register("hive_core", () -> new BlockItem(HIVE_CORE.get(), new Item.Properties()));

    public static final RegistryObject<Block> HIVE_SEED = BLOCKS.register("hive_seed", () -> new HiveSeedBlock(BlockBehaviour.Properties.of().randomTicks().strength(0.6f)));
    public static final RegistryObject<Block> HIVE_BROOD = BLOCKS.register("hive_brood", () -> new Block(BlockBehaviour.Properties.of().noCollission().noOcclusion().strength(0.3f)));
    public static final RegistryObject<Block> RESERVE_CYST = BLOCKS.register("reserve_cyst", () -> new Block(BlockBehaviour.Properties.of().noCollission().noOcclusion().strength(0.3f)));
    public static final RegistryObject<Block> HIVE_STORAGE = BLOCKS.register("hive_storage", () -> new HiveStorageBlock(BlockBehaviour.Properties.of().strength(2.5f).noOcclusion()));
    private static final DeferredRegister<net.minecraft.world.level.block.entity.BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, "hivemind");
    public static final RegistryObject<net.minecraft.world.level.block.entity.BlockEntityType<HiveStorageEntity>> STORAGE_ENTITY = BLOCK_ENTITIES.register("hive_storage", () -> net.minecraft.world.level.block.entity.BlockEntityType.Builder.of(HiveStorageEntity::new, HIVE_STORAGE.get()).build(null));
    private static final RegistryObject<Item> STORAGE_ITEM = ITEMS.register("hive_storage", () -> new BlockItem(HIVE_STORAGE.get(), new Item.Properties()));
    private static final RegistryObject<Item> BROOD_ITEM = ITEMS.register("hive_brood", () -> new BlockItem(HIVE_BROOD.get(), new Item.Properties()));
    private static final RegistryObject<Item> RESERVE_CYST_ITEM = ITEMS.register("reserve_cyst", () -> new BlockItem(RESERVE_CYST.get(), new Item.Properties()));
    public static final RegistryObject<Item> QUEEN_EGG = egg("queen_spawn_egg", QUEEN, 0x39211e, 0xd59a54);
    public static final RegistryObject<Item> SCOUT_EGG = egg("scout_spawn_egg", SCOUT, 0x72756a, 0xe1c98c);
    public static final RegistryObject<Item> WARRIOR_EGG = egg("warrior_spawn_egg", WARRIOR, 0x352a29, 0xa86c49);
    public static final RegistryObject<Item> WORKER_EGG = egg("worker_spawn_egg", WORKER, 0x8f6b3d, 0xe5be70);
    public static final RegistryObject<CreativeModeTab> HIVE_TAB = TABS.register("main", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.hivemind.main")).icon(() -> new ItemStack(HIVE_CORE.get()))
            .displayItems((parameters, output) -> {
                output.accept(HIVE_CHITIN.get()); output.accept(BROOD_COMB.get()); output.accept(MOLT_BED.get()); output.accept(ALARM_RIDGE.get()); output.accept(HIVE_CORE.get()); output.accept(HIVE_BROOD.get()); output.accept(RESERVE_CYST.get()); output.accept(STORAGE_ITEM.get());
                output.accept(QUEEN_EGG.get()); output.accept(SCOUT_EGG.get()); output.accept(WARRIOR_EGG.get()); output.accept(WORKER_EGG.get());
            }).build());
    private static final DeferredRegister<net.minecraft.world.level.levelgen.feature.Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, "hivemind");
    static { FEATURES.register("cave_hive", HiveNestFeature::new); }

    private HivemindContent() {}
    private static RegistryObject<Item> egg(String name, Supplier<? extends EntityType<? extends Mob>> type, int base, int spots) {
        return ITEMS.register(name, () -> new net.minecraftforge.common.ForgeSpawnEggItem(type, base, spots, new Item.Properties()));
    }
    public static void register(IEventBus bus) {
        BLOCK_ENTITIES.register(bus);
        FEATURES.register(bus);
        ENTITY_TYPES.register(bus); BLOCKS.register(bus); ITEMS.register(bus); TABS.register(bus); HivemindSounds.SOUNDS.register(bus);
        bus.addListener(HivemindContent::attributes); bus.addListener(HivemindContent::creative);
    }
    private static void attributes(EntityAttributeCreationEvent event) {
        event.put(QUEEN.get(), HivemindQueen.createHiveAttributes().build()); event.put(SCOUT.get(), HivemindScout.createHiveAttributes().build());
        event.put(WARRIOR.get(), HivemindWarrior.createHiveAttributes().build()); event.put(WORKER.get(), HivemindWorker.createHiveAttributes().build());
    }
    private static void creative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey().equals(CreativeModeTabs.SPAWN_EGGS)) { event.accept(QUEEN_EGG.get()); event.accept(SCOUT_EGG.get()); event.accept(WARRIOR_EGG.get()); event.accept(WORKER_EGG.get()); }
        if (event.getTabKey().equals(CreativeModeTabs.BUILDING_BLOCKS)) { event.accept(CHITIN_ITEM.get()); event.accept(CORE_ITEM.get()); event.accept(BROOD_ITEM.get()); }
    }
    public static EntityType<? extends Entity> type(Caste caste) { return switch (caste) { case QUEEN -> QUEEN.get(); case SCOUT -> SCOUT.get(); case WARRIOR -> WARRIOR.get(); case WORKER -> WORKER.get(); }; }
}
