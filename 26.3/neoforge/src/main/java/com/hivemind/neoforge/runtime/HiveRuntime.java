package com.hivemind.neoforge.runtime;

import com.hivemind.core.Caste;
import com.hivemind.core.HiveSettings;
import com.hivemind.core.HiveInterior;
import com.hivemind.core.CombatTactic;
import com.hivemind.core.Colony;
import com.hivemind.core.ColonyLocation;
import com.hivemind.core.ColonyStatus;
import com.hivemind.core.RaidPlan;
import com.hivemind.core.ColonyRegistry;
import com.hivemind.core.ColonyState;
import com.hivemind.core.HiveIdentity;
import com.hivemind.core.NestLayout;
import com.hivemind.core.OrganicNest;
import com.hivemind.core.OutpostLayout;
import com.hivemind.core.OutpostTunnelLayout;
import com.hivemind.core.PlayerMemory;
import com.hivemind.neoforge.content.HivemindContent;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

public final class HiveRuntime {
    private static final System.Logger LOGGER = System.getLogger("Hivemind");
    private static final String SAVE_FILE = "hivemind-colonies.dat";
    private static ColonyRegistry colonies = new ColonyRegistry();
    private static MinecraftServer server;
    private static boolean registryReady;
    private static long ticks;
    private record OutpostSearch(long window, String issue) {}
    private static final java.util.Map<UUID, OutpostSearch> OUTPOST_SEARCHES = new java.util.HashMap<>();
    private static final java.util.Map<UUID, RaidPlan> RAIDS = new java.util.HashMap<>();
    private static HiveSettings settings = HiveSettings.DEFAULT;
    private static final java.util.Map<MinecraftServer, HiveSettings> GENERATION_SETTINGS = new java.util.WeakHashMap<>();
    private static final int[][] BUILD_APPROACHES = {{0,-1},{-1,0},{1,0},{0,1},{-1,-1},{-1,1},{1,-1},{1,1},{0,0}};
    private static final com.hivemind.core.RepairBackoff REPAIR_BACKOFF = new com.hivemind.core.RepairBackoff();

    private HiveRuntime() {}

    public static void initialize() {}

    public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("hivemind")
                .then(Commands.literal("nest").requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                        .executes(context -> createNest(context.getSource())))
                .then(Commands.literal("status").executes(context -> showStatus(context.getSource()))));
    }

    private static int createNest(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        if (!registryReady) { source.sendFailure(Component.literal("Hivemind: los datos de colonia no se han cargado.")); return 0; }
        var player = source.getPlayerOrException();
        ServerLevel level = player.level();
        BlockPos origin = player.blockPosition().offset(0, -1, -16);
        if (!safeForNest(level, origin)) {
            source.sendFailure(Component.literal("Hivemind: el área contiene entidades, bloques protegidos o está fuera del mundo."));
            return 0;
        }
        Colony colony = colonies.create(new ColonyLocation(level.dimension().identifier().toString(), origin.getX(), origin.getY(), origin.getZ()));
        colony.addFood(36);
        colony.enableSpecializedRooms();
        colony.enableVaultedRooms();
        colony.enableOrganicNest();
        buildNest(level, origin);
        seedStorage(level, colony);

        if (!spawnMember(level, colony, Caste.QUEEN, origin.offset(OrganicNest.of(colony).throne.x(), 1, OrganicNest.of(colony).throne.z()))) {
            colony.beginCollapse();
            colony.finishExtermination();
            save(server);
            source.sendFailure(Component.literal("Hivemind: no se encontró espacio libre para la reina."));
            return 0;
        }
        spawnMember(level, colony, Caste.SCOUT, origin.offset(-3, 1, 8));
        spawnMember(level, colony, Caste.SCOUT, origin.offset(2, 1, 1));
        spawnMember(level, colony, Caste.WARRIOR, origin.offset(1, 1, 10));
        spawnMember(level, colony, Caste.WARRIOR, origin.offset(-2, 1, 9));
        spawnMember(level, colony, Caste.WORKER, origin.offset(-7, 1, -7));
        spawnMember(level, colony, Caste.WORKER, origin.offset(8, 1, 1));
        save(server);
        source.sendSuccess(() -> Component.literal("Hivemind: colmena creada (" + colony.id() + ") con " + colony.totalPopulation() + " integrantes; centro " + origin.getX() + ", " + origin.getY() + ", " + origin.getZ() + "."), true);
        return 1;
    }

    private static boolean safeForNest(ServerLevel level, BlockPos origin) {
        if (level.isOutsideBuildHeight(origin.getY()) || level.isOutsideBuildHeight(origin.getY() + NestLayout.MAX_ROOF)) return false;
        int extent = OrganicNest.FULL.extent;
        var area = new net.minecraft.world.phys.AABB(origin.getX()-extent, origin.getY(), origin.getZ()-extent,
                origin.getX()+extent+1, origin.getY()+OrganicNest.FULL.height+1, origin.getZ()+extent+1);
        if (!level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class, area, entity -> entity.isAlive() && !entity.isSpectator() && OrganicNest.FULL.intersects(entity.getBoundingBox().minX-origin.getX(), entity.getBoundingBox().minY-origin.getY(), entity.getBoundingBox().minZ-origin.getZ(), entity.getBoundingBox().maxX-origin.getX(), entity.getBoundingBox().maxY-origin.getY(), entity.getBoundingBox().maxZ-origin.getZ())).isEmpty()) return false;
        for (NestLayout.Placement placement : OrganicNest.FULL.plan) {
            NestLayout.Position cell = placement.position();
            BlockPos pos = origin.offset(cell.x(), cell.y(), cell.z());
            if (!level.hasChunkAt(pos) || level.getBlockEntity(pos) != null || level.getBlockState(pos).getDestroySpeed(level, pos) < 0) return false;
            if (!level.getWorldBorder().isWithinBounds(pos) || !level.getFluidState(pos).isEmpty()) return false;
        }
        return true;
    }

    private static void buildNest(ServerLevel level, BlockPos origin) {
        buildNest(level, origin, OrganicNest.FULL.plan);
    }

    private static void buildNest(ServerLevel level, BlockPos origin, java.util.List<NestLayout.Placement> plan) {
        for (NestLayout.Placement placement : plan) {
            NestLayout.Position cell = placement.position();
            BlockPos pos = origin.offset(cell.x(), cell.y(), cell.z());
            var block = nestBlock(placement.material());
            level.setBlock(pos, block.defaultBlockState(), 3);
        }
    }

    private static net.minecraft.world.level.block.Block nestBlock(NestLayout.Material material) {
        return switch (material) {
            case CHITIN -> HivemindContent.HIVE_CHITIN.get();
            case BROOD_COMB -> HivemindContent.BROOD_COMB.get();
            case RESERVE_CYST -> HivemindContent.RESERVE_CYST.get();
            case MOLT_BED -> HivemindContent.MOLT_BED.get();
            case ALARM_RIDGE -> HivemindContent.ALARM_RIDGE.get();
            case AIR -> Blocks.AIR;
            case CORE -> HivemindContent.HIVE_CORE.get();
            case STORAGE -> HivemindContent.HIVE_STORAGE.get();
        };
    }

    private static boolean spawnMember(ServerLevel level, Colony colony, Caste caste, BlockPos position) {
        Mob mob = (Mob) HivemindContent.type(caste).create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        if (mob == null) return false;
        float yaw = level.getRandom().nextFloat() * 360.0F;
        boolean placed = false;
        for (int radius = 0; radius <= 3 && !placed; radius++) {
            for (int x = -radius; x <= radius && !placed; x++) {
                for (int z = -radius; z <= radius; z++) {
                    BlockPos candidate = position.offset(x, 0, z);
                    ColonyLocation home = colony.home();
                    if (home != null && colony.hasOrganicNest() && !OrganicNest.of(colony).inside(candidate.getX()-home.x(), candidate.getY()-home.y(), candidate.getZ()-home.z())) continue;
                    if (home != null && !colony.hasOrganicNest() && (Math.abs(candidate.getX() - home.x()) > (colony.hasSpecializedRooms() ? 12 : 5) || Math.abs(candidate.getZ() - home.z()) > (colony.hasSpecializedRooms() ? 12 : 5))) continue;
                    BlockPos floor = candidate.below();
                    if (!level.getBlockState(floor).isFaceSturdy(level, floor, net.minecraft.core.Direction.UP)) continue;
                    mob.snapTo(candidate, yaw, 0.0F);
                    boolean clearOfEntities = level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,
                            mob.getBoundingBox().inflate(0.02), entity -> entity.isAlive() && !entity.isSpectator()).isEmpty();
                    if (clearOfEntities && level.noCollision(mob)) { placed = true; break; }
                }
            }
        }
        if (!placed) {
            LOGGER.log(System.Logger.Level.WARNING, "No hay espacio libre para " + caste + " en " + position);
            return false;
        }
        mob.setPersistenceRequired();
        ColonyLocation nestHome = caste == Caste.QUEEN ? colony.coreLocation() : colony.home();
        BlockPos anchor = nestHome == null ? position : new BlockPos(nestHome.x(), nestHome.y() + 1, nestHome.z());
        mob.setHomeTo(anchor, caste == Caste.QUEEN ? (colony.hasOrganicNest() ? 4 : 12) : caste == Caste.SCOUT ? 64 : 28);
        mob.addTag(HiveIdentity.colonyTag(colony.id()));
        mob.addTag(HiveIdentity.casteTag(caste));
        mob.setCustomName(Component.literal(casteName(caste)));
        mob.setCustomNameVisible(caste == Caste.QUEEN);
        if (mob.getAttribute(Attributes.MAX_HEALTH) != null) {
            mob.setHealth(mob.getMaxHealth());
        }
        if (!level.addFreshEntity(mob)) return false;
        colony.addMember(mob.getUUID(), caste);
        return true;
    }

    private static String casteName(Caste caste) {
        return switch (caste) {
            case QUEEN -> "Hivemind Queen";
            case SCOUT -> "Hivemind Scout";
            case WARRIOR -> "Hivemind Warrior";
            case WORKER -> "Hivemind Worker";
        };
    }

    private static boolean outpostShellComplete(ServerLevel level, ColonyLocation home, ColonyLocation site) {
        if (site == null) return false;
        BlockPos center = new BlockPos(site.x(), site.y(), site.z());
        for (NestLayout.Position part : OutpostLayout.shell(OutpostTunnelLayout.side(site.x() - home.x(), site.z() - home.z()))) {
            BlockPos cell = center.offset(part.x(), part.y(), part.z());
            if (!level.hasChunkAt(cell) || !level.getBlockState(cell).is(HivemindContent.HIVE_CHITIN)) return false;
        }
        return true;
    }

    private static int populationLimit(ServerLevel level, Colony colony, ColonyLocation home) {
        if (!colony.hasSpecializedRooms()) return colony.populationLimit(false, 0);
        BlockPos center = new BlockPos(home.x(), home.y(), home.z());
        boolean annex = !hasSouthernOutpost(colony, home);
        for (NestLayout.Position part : com.hivemind.core.GrowthLayout.shell()) {
            if (!annex) break;
            BlockPos cell = center.offset(part.x(), part.y(), part.z());
            annex = level.hasChunkAt(cell) && (part.y() == 0 ? level.getBlockState(cell).isFaceSturdy(level, cell, net.minecraft.core.Direction.UP)
                    : level.getBlockState(cell).is(HivemindContent.HIVE_CHITIN));
        }
        return colony.populationLimit(annex, activeOutposts(level, colony, home));
    }

    private static boolean hasSouthernOutpost(Colony colony, ColonyLocation home) {
        return colony.hasOutpost(new ColonyLocation(home.dimension(), home.x(), home.y(), home.z() + 24));
    }

    private static int activeOutposts(ServerLevel level, Colony colony, ColonyLocation home) {
        return (int) colony.outposts().stream().filter(Colony.Outpost::complete).map(Colony.Outpost::location)
                .filter(post -> outpostShellComplete(level, home, post) && outpostAccessOpen(level, home, post)).count();
    }

    private static java.util.List<String> outpostStatus(Colony colony) {
        if (!colony.outposts().isEmpty()) {
            var lines = new java.util.ArrayList<String>();
            for (var post : colony.outposts()) {
                lines.add(outpostStatus(colony, post));
                var home = colony.home();
                var site = post.location();
                if (home == null || !home.dimension().equals(site.dimension()) || home.y() != site.y()) continue;
                int dx = site.x() - home.x(), dz = site.z() - home.z();
                if (!OutpostTunnelLayout.supported(dx, dz)) continue;
                var door = OutpostTunnelLayout.connectingDoor(dx, dz);
                lines.add("  Entrada desde la colmena: " + (home.x() + door.x()) + ", "
                        + (home.y() + door.y()) + ", " + (home.z() + door.z()));
            }
            return lines;
        }
        if (colony.state() != ColonyState.ACTIVE || !colony.hasSpecializedRooms()) return java.util.List.of("Ninguno");
        if (!settings.expansion()) return java.util.List.of("Expansión desactivada");
        if (colony.totalPopulation() < 10 || colony.population(Caste.WORKER) < 3)
            return java.util.List.of("El primer refugio requiere 10 integrantes y 3 obreros");
        var search = OUTPOST_SEARCHES.get(colony.id());
        return java.util.List.of(search == null ? "En espera de planificación" : search.issue());
    }

    private static String outpostStatus(Colony colony, Colony.Outpost entry) {
        ColonyLocation post = entry.location();
        String location = ColonyStatus.coordinates(post) + " · ";
        if (colony.state() != ColonyState.ACTIVE) return location + "Inactivo";
        if (server == null) return location + "Desconocido";
        for (var level : server.getAllLevels()) {
            if (!level.dimension().identifier().toString().equals(post.dimension())) continue;
            ColonyLocation home = colony.home();
            String issue = outpostPlanIssue(level, home, post);
            if (issue != null) return location + "Bloqueado · " + issue;
            BlockPos center = new BlockPos(post.x(), post.y(), post.z());
            int placed = 0;
            var shell = OutpostLayout.shell(OutpostTunnelLayout.side(post.x() - home.x(), post.z() - home.z()));
            for (NestLayout.Position part : shell) {
                BlockPos cell = center.offset(part.x(), part.y(), part.z());
                var state = level.getBlockState(cell);
                if (state.is(HivemindContent.HIVE_CHITIN)) placed++;
            }
            boolean accessOpen = outpostAccessOpen(level, home, post);
            if (entry.complete() && placed == shell.size() && accessOpen) return location + "Activo (+3 plazas)";
            String phase = accessOpen ? "En construcción" : "Abriendo acceso";
            String detail = availableFood(level, colony, home) < 1 ? "; sin alimento" : "";
            return location + phase + " · " + placed + "/" + shell.size() + " bloques" + detail;
        }
        return location + "Dimensión descargada";
    }

    private static String capacityStatus(Colony colony) {
        ColonyLocation home = colony.home();
        if (home != null && server != null) for (var level : server.getAllLevels()) {
            if (level.dimension().identifier().toString().equals(home.dimension())) return Integer.toString(populationLimit(level, colony, home));
        }
        return Integer.toString(colony.populationLimit(false, 0));
    }

    private static int huntingReserve(Colony colony) { return Math.max(12, colony.totalPopulation() * 2); }

    private static String huntingStatus(Colony colony) {
        if (!settings.hunting()) return "desactivada";
        if (colony.state() != ColonyState.ACTIVE || colony.home() == null || server == null) return "inactiva";
        if (colony.population(Caste.WARRIOR) == 0 || colony.population(Caste.WORKER) == 0) return "sin combatientes u obreros";
        for (var level : server.getAllLevels()) {
            if (!level.dimension().identifier().toString().equals(colony.home().dimension())) continue;
            if (availableFood(level, colony, colony.home()) >= huntingReserve(colony)) return "pausada por reservas";
            return level.getSkyDarken() >= 4 ? "habilitada" : "en espera de la noche";
        }
        return "dimensión descargada";
    }

    private static String foodStatus(Colony colony) {
        ColonyLocation home = colony.home();
        if (home == null || server == null || !colony.hasSpecializedRooms()) return Integer.toString(colony.food());
        for (var level : server.getAllLevels()) {
            if (!level.dimension().identifier().toString().equals(home.dimension())) continue;
            BlockPos position = roomPosition(home, NestLayout.Room.STORAGE).above();
            if (!level.hasChunkAt(position)) return colony.food() + " (+ almacén descargado)";
            if (storageInventory(level, colony, home) == null) return colony.food() + " (almacén ausente)";
            return Integer.toString(availableFood(level, colony, home));
        }
        return colony.food() + " (+ almacén descargado)";
    }

    private static int showStatus(CommandSourceStack source) {
        if (!registryReady) { source.sendFailure(Component.literal("Hivemind: los datos de colonia no se han cargado.")); return 0; }
        var all = colonies.colonies();
        source.sendSuccess(() -> Component.literal(ColonyStatus.summary(all)), false);
        for (Colony colony : all.stream().sorted(java.util.Comparator
                .comparing(Colony::state).thenComparing(Colony::id)).toList()) {
            var lines = ColonyStatus.lines(colony, capacityStatus(colony), foodStatus(colony), huntingStatus(colony), outpostStatus(colony));
            for (String line : lines) source.sendSuccess(() -> Component.literal(line), false);
            if (colony.state() == ColonyState.ACTIVE) source.sendSuccess(() -> Component.literal(raidStatus(colony)), false);
        }
        return 1;
    }

    public static void load(MinecraftServer minecraftServer) {
        registryReady = false;
        server = minecraftServer;
        ticks = 0L;
        REPAIR_BACKOFF.clear();
        OUTPOST_SEARCHES.clear();
        RAIDS.clear();
        Path file = dataFile(minecraftServer);
        synchronized (GENERATION_SETTINGS) {
            settings = GENERATION_SETTINGS.computeIfAbsent(minecraftServer, HiveRuntime::readSettings);
        }
        try {
            colonies = ColonyRegistry.load(file);
            registryReady = true;
            LOGGER.log(System.Logger.Level.INFO, "Loaded " + colonies.colonies().size() + " Hivemind colonies.");
        } catch (IOException exception) {
            colonies = new ColonyRegistry();
            LOGGER.log(System.Logger.Level.ERROR, "Could not load Hivemind colony data; colony processing and saving are disabled.", exception);
        }
    }

    public static void save(MinecraftServer minecraftServer) {
        if (minecraftServer == null || !registryReady || server != minecraftServer) return;
        try {
            colonies.save(dataFile(minecraftServer));
        } catch (IOException exception) {
            LOGGER.log(System.Logger.Level.ERROR, "Could not save Hivemind colony data.", exception);
        }
    }

    public static void stop(MinecraftServer minecraftServer) {
        save(minecraftServer);
        if (server == minecraftServer) { registryReady = false; server = null; }
    }
    public static boolean naturalNestsEnabled(MinecraftServer minecraftServer) {
        synchronized (GENERATION_SETTINGS) {
            return GENERATION_SETTINGS.computeIfAbsent(minecraftServer, HiveRuntime::readSettings).naturalColoniesPerDimension() > 0;
        }
    }

    private static HiveSettings readSettings(MinecraftServer minecraftServer) {
        Path config = dataFile(minecraftServer).resolveSibling("hivemind.properties");
        try {
            return HiveSettings.load(config);
        } catch (IOException | IllegalArgumentException exception) {
            LOGGER.log(System.Logger.Level.WARNING, "Using Hivemind defaults because " + config + " could not be loaded.", exception);
            return HiveSettings.DEFAULT;
        }
    }

    private static Path dataFile(MinecraftServer minecraftServer) {
        return minecraftServer.getWorldPath(LevelResource.ROOT).resolve("data").resolve(SAVE_FILE);
    }

    public static void tick(MinecraftServer minecraftServer) {
        if (!registryReady || server != minecraftServer) return;
        ticks++;
        if (ticks % 100L == 0L) {
            updateColonies(minecraftServer);
            if (ticks % 600L == 0L) save(minecraftServer);
        }
    }


    public static void activateNaturalNest(ServerLevel level, BlockPos seed) {
        if (!registryReady || server != level.getServer() || settings.naturalColoniesPerDimension() == 0
                || !level.getBlockState(seed).is(HivemindContent.HIVE_SEED.get())) return;
        String dimension = level.dimension().identifier().toString();
        long active = colonies.colonies().stream().filter(hive -> hive.state() == ColonyState.ACTIVE)
                .map(Colony::home).filter(java.util.Objects::nonNull).filter(home -> home.dimension().equals(dimension)).count();
        if (active >= settings.naturalColoniesPerDimension()) return;
        BlockPos origin = seed.below();
        if (level.isOutsideBuildHeight(origin.getY()) || level.isOutsideBuildHeight(origin.getY() + 4)) return;
        for (int dx : new int[]{-16, 0, 16}) for (int dz : new int[]{-16, 0, 18})
            if (!level.hasChunkAt(origin.offset(dx, 0, dz))) return;
        if (level.canSeeSky(seed) || level.getBrightness(net.minecraft.world.level.LightLayer.BLOCK, seed) > 7) return;
        long spacingSquared = (long) settings.colonySpacing() * settings.colonySpacing();
        boolean nearby = colonies.colonies().stream().map(Colony::home).filter(java.util.Objects::nonNull)
                .filter(home -> home.dimension().equals(dimension))
                .anyMatch(home -> Math.pow(home.x() - origin.getX(), 2) + Math.pow(home.z() - origin.getZ(), 2) < spacingSquared);
        if (nearby) return;
        boolean organicSeed = level.getBlockState(origin.offset(OrganicNest.FULL.throne.x(), 0, OrganicNest.FULL.throne.z())).is(HivemindContent.HIVE_CORE.get())
                || level.getBlockState(origin.offset(OrganicNest.SMALL.throne.x(), 0, OrganicNest.SMALL.throne.z())).is(HivemindContent.HIVE_CORE.get());
        boolean generated = organicSeed || level.getBlockState(origin).is(HivemindContent.HIVE_CORE.get());
        boolean specialized = generated
                ? isStorage(level.getBlockState(origin.offset(10, 1, 0)))
                : naturalExitOpen(level, origin, 16) && safeNaturalFootprint(level, origin, seed, OrganicNest.FULL.plan, OrganicNest.FULL.extent);
        var plan = specialized ? OrganicNest.FULL.plan : OrganicNest.SMALL.plan;
        if (!generated && !specialized && (!naturalExitOpen(level, origin, 4) || !safeNaturalFootprint(level, origin, seed, plan, OrganicNest.SMALL.extent))) return;
        var previous = new java.util.LinkedHashMap<BlockPos, net.minecraft.world.level.block.state.BlockState>();
        if (generated) previous.put(seed, level.getBlockState(seed));
        else for (var placement : plan) {
            var part = placement.position();
            BlockPos cell = origin.offset(part.x(), part.y(), part.z());
            previous.put(cell, level.getBlockState(cell));
        }
        Colony colony = colonies.create(new ColonyLocation(dimension, origin.getX(), origin.getY(), origin.getZ()));
        colony.addFood(specialized ? 24 : 12);
        if (!generated || organicSeed) colony.enableOrganicNest();
        if (specialized) {
            colony.enableSpecializedRooms();
            if (!generated || organicSeed || level.getBlockState(origin.offset(-10, 0, 0)).is(HivemindContent.BROOD_COMB.get())) colony.enableFacetedRooms();
            if (!generated || organicSeed || level.getBlockState(origin.offset(9, 4, 2)).is(HivemindContent.RESERVE_CYST.get())) colony.enableVaultedRooms();
        }
        if (generated) level.setBlock(seed, Blocks.AIR.defaultBlockState(), 3);
        else buildNest(level, origin, plan);
        if (!spawnMember(level, colony, Caste.QUEEN, colony.hasOrganicNest() ? origin.offset(OrganicNest.of(colony).throne.x(), 1, OrganicNest.of(colony).throne.z()) : origin.above())) {
            previous.forEach((cell, state) -> level.setBlock(cell, state, 3));
            colonies.discardUnfounded(colony.id());
            return;
        }
        if (colony.hasOrganicNest()) {
            if (specialized) {
                spawnMember(level, colony, Caste.SCOUT, origin.offset(2,1,1));
                spawnMember(level, colony, Caste.WARRIOR, origin.offset(1,1,10));
                spawnMember(level, colony, Caste.WARRIOR, origin.offset(-2,1,9));
                spawnMember(level, colony, Caste.WORKER, origin.offset(8,1,1));
                spawnMember(level, colony, Caste.WORKER, origin.offset(-7,1,-7));
                seedStorage(level, colony);
            } else {
                spawnMember(level, colony, Caste.SCOUT, origin.above());
                spawnMember(level, colony, Caste.WORKER, origin.offset(-6,1,-3));
                spawnMember(level, colony, Caste.WORKER, origin.offset(-1,1,-3));
            }
            save(server);
            return;
        }
        int memberOffset = specialized ? 4 : 2;
        spawnMember(level, colony, Caste.SCOUT, origin.offset(-memberOffset, 1, 0));
        spawnMember(level, colony, Caste.WORKER, origin.offset(memberOffset, 1, -2));
        spawnMember(level, colony, Caste.WORKER, origin.offset(-memberOffset, 1, 2));
        if (specialized) {
            spawnMember(level, colony, Caste.WARRIOR, origin.offset(0, 1, 4));
            spawnMember(level, colony, Caste.WARRIOR, origin.offset(0, 1, -4));
            seedStorage(level, colony);
        }
        save(server);
    }

    private static boolean naturalExitOpen(ServerLevel level, BlockPos origin, int start) {
        for (int z = start; z < start + 3; z++) {
            BlockPos support = origin.offset(0, 0, z);
            if (!level.hasChunkAt(support) || !level.getWorldBorder().isWithinBounds(support)) return false;
            if (!level.getBlockState(support).isFaceSturdy(level, support, net.minecraft.core.Direction.UP)) return false;
            for (int y = 1; y <= 3; y++) if (!level.getBlockState(origin.offset(0, y, z)).isAir()) return false;
        }
        return true;
    }

    private static boolean safeNaturalFootprint(ServerLevel level, BlockPos origin, BlockPos seed,
                                                java.util.List<NestLayout.Placement> plan, int radius) {
        var bounds = new net.minecraft.world.phys.AABB(origin.getX() - radius, origin.getY(), origin.getZ() - radius,
                origin.getX() + radius + 1, origin.getY() + (plan == OrganicNest.SMALL.plan ? OrganicNest.SMALL.height + 1 : NestLayout.MAX_ROOF + 1), origin.getZ() + radius + 1);
        if (!level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class, bounds, entity -> entity.isAlive() && !entity.isSpectator()).isEmpty()) return false;
        if (level.isOutsideBuildHeight(origin.getY() + (plan == OrganicNest.SMALL.plan ? OrganicNest.SMALL.height : NestLayout.MAX_ROOF))) return false;
        int openInterior = 0, interior = 0;
        for (var placement : plan) {
            var part = placement.position();
            BlockPos cell = origin.offset(part.x(), part.y(), part.z());
            if (!level.hasChunkAt(cell) || !level.getWorldBorder().isWithinBounds(cell)
                    || level.getBlockEntity(cell) != null || !level.getFluidState(cell).isEmpty()) return false;
            var state = level.getBlockState(cell);
            boolean open = state.isAir() || cell.equals(seed);
            if (!open && !excavationMaterial(state)) return false;
            if (placement.material() == NestLayout.Material.AIR) {
                interior++;
                if (open) openInterior++;
            }
        }
        return openInterior * 3 >= interior;
    }

    private static void updateColonies(MinecraftServer minecraftServer) {
        for (ServerLevel level : minecraftServer.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                Optional<UUID> colonyId = HiveIdentity.colonyId(entity.entityTags());
                if (colonyId.isEmpty()) continue;
                Colony colony = colonies.get(colonyId.get()).orElse(null);
                if (colony == null || colony.state() != ColonyState.ACTIVE) {
                    entity.discard();
                    continue;
                }
                if (entity instanceof Mob mob) HiveIdentity.caste(entity.entityTags()).ifPresent(caste -> performCasteTasks(level, mob, colony, caste));
                HiveIdentity.caste(entity.entityTags()).ifPresent(caste -> {
                    if (!colony.membersSnapshot().containsKey(entity.getUUID())) colony.addMember(entity.getUUID(), caste);
                });
            }
        }
        for (Colony colony : colonies.colonies()) {
            if (colony.state() == ColonyState.COLLAPSING) finishCollapse(minecraftServer, colony);
            else if (colony.state() == ColonyState.ACTIVE) noticePlayers(minecraftServer, colony);
        }
    }

    public static boolean blocksForeignSpawn(net.minecraft.world.entity.EntityType<?> type, net.minecraft.world.level.ServerLevelAccessor access, net.minecraft.world.entity.EntitySpawnReason reason, BlockPos pos) {
        if (!(access instanceof ServerLevel level) || !registryReady || level.getServer() != server) return false;
        switch (reason) {
            case NATURAL, CHUNK_GENERATION, SPAWNER, REINFORCEMENT, TRIAL_SPAWNER -> { }
            default -> { return false; }
        }
        for (Caste caste : Caste.values()) if (type == HivemindContent.type(caste)) return false;
        String dimension = level.dimension().identifier().toString();
        for (var colony : colonies.colonies()) {
            if (colony.state() == ColonyState.ACTIVE && colony.home() != null && dimension.equals(colony.home().dimension())
                    && insideHive(level, colony, colony.home(), pos)) return true;
        }
        return false;
    }

    private static boolean insideHive(ServerLevel level, Colony colony, ColonyLocation home, BlockPos pos) {
        int x = pos.getX() - home.x(), y = pos.getY() - home.y(), z = pos.getZ() - home.z();
        if (y < 1 || y >= NestLayout.MAX_ROOF || Math.abs(x) > 44 || Math.abs(z) > 44) return false;
        if (colony.hasOrganicNest() && OrganicNest.of(colony).inside(x,y,z)) return true;
        if (!colony.hasSpecializedRooms()) {
            if (colony.hasOrganicNest()) return false;
            if (Math.abs(x) > 6 || Math.abs(z) > 6) return false;
            return HiveInterior.traditional(x, y, z, nestRadius(level, home));
        }
        if (!colony.hasOrganicNest() && HiveInterior.nest(x, y, z, colony.hasFacetedRooms(), colony.hasVaultedRooms())) return true;
        if (HiveInterior.annex(x, y, z) && level.getBlockState(pos.offset(0, 5 - y, 0)).is(HivemindContent.HIVE_CHITIN.get())) return true;
        for (var post : colony.outposts()) {
            if (!post.complete()) continue;
            var site = post.location();
            if (HiveInterior.outpost(x, y, z, site.x() - home.x(), site.z() - home.z())) return true;
        }
        return false;
    }

    private static net.minecraft.world.entity.LivingEntity findIntruder(ServerLevel level, Mob defender, Colony colony, ColonyLocation home) {
        return level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class, defender.getBoundingBox().inflate(16),
                    candidate -> candidate instanceof net.minecraft.world.entity.monster.Enemy && !(candidate instanceof com.hivemind.neoforge.content.HiveEntity)
                            && candidate.isAlive() && insideHive(level, colony, home, candidate.blockPosition()) && (defender.hasLineOfSight(candidate)
                            || level.getBlockState(candidate.blockPosition().below()).is(HivemindContent.ALARM_RIDGE.get())))
                .stream().min(java.util.Comparator.comparingDouble(defender::distanceToSqr)).orElse(null);
    }

    private static boolean availableForDuty(Mob worker, Colony colony, ColonyLocation home) {
        if (worker.getTarget() != null || worker.getHealth() < worker.getMaxHealth() * 0.5f) return false;
        if (!colony.hasSpecializedRooms() || worker.getY() >= home.y()) return true;
        int x = (int) Math.floor(worker.getX()) - home.x(), z = (int) Math.floor(worker.getZ()) - home.z();
        return colony.hasOrganicNest() ? !OrganicNest.of(colony).footprint(x,z) : NestLayout.roomAt(x, z) == null;
    }

    private static com.hivemind.core.WorkerPolicy.Duties workerDuties(ServerLevel level, Colony colony, ColonyLocation home) {
        BlockPos center = new BlockPos(home.x(), home.y(), home.z());
        var workers = level.getEntitiesOfClass(com.hivemind.neoforge.content.HivemindWorker.class, new net.minecraft.world.phys.AABB(center).inflate(128.0), candidate ->
                candidate.isAlive() && HiveIdentity.colonyId(candidate.entityTags()).filter(colony.id()::equals).isPresent());
        var candidates = workers.stream().map(candidate -> new com.hivemind.core.WorkerPolicy.Candidate(candidate.getUUID(),
                availableForDuty(candidate, colony, home) && !candidate.entityTags().contains("hivemind_carry_food")
                        && !candidate.entityTags().contains("hivemind_carry_chitin"))).toList();
        return com.hivemind.core.WorkerPolicy.duties(candidates, colony.hasOutpostProject());
    }

    private static java.util.List<java.util.UUID> activeRepairCarriers(ServerLevel level, Colony colony, ColonyLocation home) {
        BlockPos center = new BlockPos(home.x(), home.y(), home.z());
        var workers = level.getEntitiesOfClass(com.hivemind.neoforge.content.HivemindWorker.class, new net.minecraft.world.phys.AABB(center).inflate(128.0), worker ->
                worker.isAlive() && worker.entityTags().contains("hivemind_carry_chitin") && isChitin(worker.getMainHandItem())
                && HiveIdentity.colonyId(worker.entityTags()).filter(colony.id()::equals).isPresent());
        return com.hivemind.core.WorkerPolicy.repairCarriers(workers.stream().map(worker ->
                new com.hivemind.core.WorkerPolicy.Carrier(worker.getUUID(), availableForDuty(worker, colony, home) && REPAIR_BACKOFF.ready(worker.getUUID(), level.getGameTime()))).toList());
    }

    private static boolean mayFetchRepairMaterial(ServerLevel level, Mob mob, Colony colony, ColonyLocation home) {
        int slots = 2 - activeRepairCarriers(level, colony, home).size();
        if (slots <= 0) return false;
        BlockPos barrel = roomPosition(home, NestLayout.Room.STORAGE).offset(0, 1, 0);
        var duties = workerDuties(level, colony, home);
        boolean buildingOutpost = colony.hasOutpostProject();
        var workers = level.getEntitiesOfClass(com.hivemind.neoforge.content.HivemindWorker.class, new net.minecraft.world.phys.AABB(barrel).inflate(48.0), worker ->
                worker.isAlive()
                && (!buildingOutpost || !duties.outpostBuilders().contains(worker.getUUID()))
                && availableForDuty(worker, colony, home) && worker.getMainHandItem().isEmpty()
                && !worker.entityTags().contains("hivemind_carry_food") && !worker.entityTags().contains("hivemind_carry_chitin")
                && REPAIR_BACKOFF.ready(worker.getUUID(), level.getGameTime())
                && HiveIdentity.colonyId(worker.entityTags()).filter(colony.id()::equals).isPresent());
        return workers.stream().sorted(java.util.Comparator.<com.hivemind.neoforge.content.HivemindWorker>comparingDouble(worker ->
                worker.distanceToSqr(barrel.getX() + 0.5, barrel.getY(), barrel.getZ() + 0.5)).thenComparing(worker -> worker.getUUID()))
                .limit(slots).anyMatch(worker -> worker == mob);
    }

    private static boolean approachStorage(ServerLevel level, Mob mob, ColonyLocation home) {
        BlockPos barrel = roomPosition(home, NestLayout.Room.STORAGE).offset(0, 1, 0);
        BlockPos stand = outpostApproach(level, mob, barrel, home.y());
        if (stand == null) {
            REPAIR_BACKOFF.defer(mob.getUUID(), level.getGameTime());
            return false;
        }
        if (mob.distanceToSqr(stand.getX() + 0.5, stand.getY(), stand.getZ() + 0.5) <= 2.25) return true;
        mob.getNavigation().moveTo(stand.getX() + 0.5, stand.getY(), stand.getZ() + 0.5, 1.0);
        return false;
    }

    private static void returnChitinToStorage(ServerLevel level, Mob mob, Colony colony, ColonyLocation home) {
        if (!colony.hasSpecializedRooms()) return;
        if (storageInventory(level, colony, home) != null && approachStorage(level, mob, home) && storeStack(level, colony, home, mob.getMainHandItem())) {
            mob.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, net.minecraft.world.item.ItemStack.EMPTY);
            mob.removeTag("hivemind_carry_chitin");
        }
    }

    private static boolean isOutpostBuilder(ServerLevel level, Mob mob, Colony colony, ColonyLocation home) {
        return workerDuties(level, colony, home).outpostBuilders().contains(mob.getUUID());
    }

    private record ExcavationJob(BlockPos block, BlockPos approach) {}

    public static boolean excavationMaterial(net.minecraft.world.level.block.state.BlockState state) {
        return state.is(Blocks.STONE) ||
                state.is(Blocks.DEEPSLATE) ||
                state.is(Blocks.DIRT) ||
                state.is(Blocks.COARSE_DIRT) ||
                state.is(Blocks.ROOTED_DIRT) ||
                state.is(Blocks.GRASS_BLOCK) ||
                state.is(Blocks.TUFF) ||
                state.is(Blocks.ANDESITE) ||
                state.is(Blocks.DIORITE) ||
                state.is(Blocks.GRANITE) ||
                state.is(Blocks.CALCITE) ||
                state.is(Blocks.SANDSTONE) ||
                state.is(Blocks.MOSS_BLOCK) ||
                state.is(Blocks.CLAY);
    }

    private static boolean isGrowthExcavator(ServerLevel level, Mob mob, Colony colony, ColonyLocation home) {
        return workerDuties(level, colony, home).growthExcavators().contains(mob.getUUID());
    }

    private static ExcavationJob findGrowthExcavation(ServerLevel level, Mob worker, Colony colony, ColonyLocation home) {
        if (!settings.expansion() || hasSouthernOutpost(colony, home)) return null;
        if (!colony.hasSpecializedRooms() || colony.population(Caste.WORKER) < 3
                || availableFood(level, colony, home) < 8 || !(level.getGameRules().get(net.minecraft.world.level.gamerules.GameRules.MOB_GRIEFING))) return null;
        net.minecraft.world.Container supplies = storageInventory(level, colony, home);
        boolean hasChitin = false;
        if (supplies != null) for (int slot = 0; slot < supplies.getContainerSize(); slot++) {
            if (isChitin(supplies.getItem(slot))) { hasChitin = true; break; }
        }
        if (!hasChitin) return null;
        BlockPos origin = new BlockPos(home.x(), home.y(), home.z());
        for (int z = 14; z <= 26; z++) for (int x = -3; x <= 3; x++) {
            if (!com.hivemind.core.GrowthLayout.inFootprint(x, z)) continue;
            BlockPos floor = origin.offset(x, 0, z);
            if (!level.hasChunkAt(floor) || !level.getWorldBorder().isWithinBounds(floor)
                    || (!level.getBlockState(floor).isAir() && !level.getBlockState(floor).isFaceSturdy(level, floor, net.minecraft.core.Direction.UP))) return null;
            for (int y = 1; y <= com.hivemind.core.GrowthLayout.ROOF; y++) {
                BlockPos cell = origin.offset(x, y, z);
                var state = level.getBlockState(cell);
                if (level.getBlockEntity(cell) != null || !level.getFluidState(cell).isEmpty()) return null;
                if (!state.isAir() && !excavationMaterial(state)
                        && !((com.hivemind.core.GrowthLayout.isShell(x, y, z) || com.hivemind.core.GrowthLayout.isFormerCeiling(new NestLayout.Position(x, y, z))) && state.is(HivemindContent.HIVE_CHITIN))) return null;
            }
        }
        for (NestLayout.Position part : com.hivemind.core.GrowthLayout.excavation()) {
            BlockPos target = origin.offset(part.x(), part.y(), part.z());
            var state = level.getBlockState(target);
            boolean formerCeiling = com.hivemind.core.GrowthLayout.isFormerCeiling(part) && state.is(HivemindContent.HIVE_CHITIN);
            if (!excavationMaterial(state) && !formerCeiling) continue;
            boolean safe = true;
            for (net.minecraft.core.Direction dir : net.minecraft.core.Direction.values()) {
                BlockPos neighbor = target.relative(dir);
                if (!level.hasChunkAt(neighbor) || !level.getFluidState(neighbor).isEmpty()) { safe = false; break; }
            }
            if (!safe || level.getBlockState(target.offset(0, 1, 0)).getBlock() instanceof net.minecraft.world.level.block.FallingBlock) continue;
            BlockPos approach = outpostApproach(level, worker, target, home.y());
            if (approach != null) return new ExcavationJob(target, approach);
        }
        return null;
    }

    private static void excavateGrowth(ServerLevel level, Mob worker, Colony colony, ColonyLocation home, ExcavationJob job) {
        BlockPos target = job.block();
        BlockPos approach = job.approach();
        if (worker.distanceToSqr(approach.getX() + 0.5, approach.getY(), approach.getZ() + 0.5) > 2.25) {
            worker.getNavigation().moveTo(approach.getX() + 0.5, approach.getY(), approach.getZ() + 0.5, 0.9);
            return;
        }
        var state = level.getBlockState(target);
        var relative = new NestLayout.Position(target.getX() - home.x(), target.getY() - home.y(), target.getZ() - home.z());
        boolean formerCeiling = com.hivemind.core.GrowthLayout.isFormerCeiling(relative) && state.is(HivemindContent.HIVE_CHITIN);
        if ((!excavationMaterial(state) && !formerCeiling) || !outpostBreakSafe(level, target)) return;
        if (!level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class, new net.minecraft.world.phys.AABB(target), entity -> entity.isAlive()).isEmpty()) return;
        if (level.destroyBlock(target, false, worker)) consumeFood(level, colony, home, 1);
    }

    private static boolean leaveUnderFloor(ServerLevel level, Mob worker, ColonyLocation home, Colony colony) {
        int x = (int) Math.floor(worker.getX()) - home.x(), z = (int) Math.floor(worker.getZ()) - home.z();
        NestLayout.Room room = NestLayout.roomAt(x, z);
        if (worker.getY() >= home.y() || (colony.hasOrganicNest() ? !OrganicNest.of(colony).footprint(x,z) : room == null)) return false;
        NestLayout.Position center = colony.hasOrganicNest() ? new NestLayout.Position(x,0,z) : NestLayout.center(room);
        int radius = room == NestLayout.Room.CENTRAL ? 6 : 3;
        int footY = (int) Math.floor(worker.getY());
        java.util.List<BlockPos> candidates = new java.util.ArrayList<>();
        for (int i = -radius; i <= radius; i++) {
            for (int y = footY - 1; y <= footY + 1; y++) {
                candidates.add(new BlockPos(home.x() + center.x() - radius - 1, y, home.z() + center.z() + i));
                candidates.add(new BlockPos(home.x() + center.x() + radius + 1, y, home.z() + center.z() + i));
                candidates.add(new BlockPos(home.x() + center.x() + i, y, home.z() + center.z() - radius - 1));
                candidates.add(new BlockPos(home.x() + center.x() + i, y, home.z() + center.z() + radius + 1));
            }
        }
        candidates.sort(java.util.Comparator.comparingDouble((BlockPos pos) -> worker.distanceToSqr(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5)));
        int start = (int) Math.floorMod((level.getGameTime() / 100L) * 4L, candidates.size());
        int attempts = 0;
        for (int i = 0; i < candidates.size(); i++) {
            BlockPos pos = candidates.get((start + i) % candidates.size());
            if ((colony.hasOrganicNest() ? OrganicNest.of(colony).footprint(pos.getX()-home.x(),pos.getZ()-home.z()) : NestLayout.roomAt(pos.getX() - home.x(), pos.getZ() - home.z()) != null) || !level.hasChunkAt(pos)
                    || !level.getBlockState(pos).isAir() || !level.getBlockState(pos.offset(0, 1, 0)).isAir()) continue;
            BlockPos floor = pos.offset(0, -1, 0);
            if (!level.getBlockState(floor).isFaceSturdy(level, floor, net.minecraft.core.Direction.UP)) continue;
            if (++attempts > 4) return false;
            var route = worker.getNavigation().createPath(pos, 0);
            if (route == null || !route.canReach()) continue;
            worker.getNavigation().moveTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 1.0);
            return true;
        }
        return false;
    }

    private static void performCasteTasks(ServerLevel level, Mob mob, Colony colony, Caste caste) {
        if (caste == Caste.WARRIOR || caste == Caste.WORKER) {
            var range = mob.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.FOLLOW_RANGE);
            if (range != null && range.getBaseValue() == (caste == Caste.WARRIOR ? 24.0 : 12.0)) range.setBaseValue(48.0);
        }
        ColonyLocation home = colony.home();
        if (home == null || !level.dimension().identifier().toString().equals(home.dimension())) return;
        ColonyLocation anchor = caste == Caste.QUEEN ? colony.coreLocation() : home;
        mob.setHomeTo(new BlockPos(anchor.x(), anchor.y() + 1, anchor.z()), caste == Caste.QUEEN ? (colony.hasOrganicNest() ? 4 : 12) : caste == Caste.WORKER ? 28 : colony.territoryRadius());
        if (caste == Caste.WORKER && colony.hasSpecializedRooms() && leaveUnderFloor(level, mob, home, colony)) return;
        final double hx = anchor.x() + 0.5, hy = anchor.y() + 1.0, hz = anchor.z() + 0.5;
        double homeDistance = mob.distanceToSqr(hx, hy, hz);
        double territory = colony.territoryRadius();
        if (caste != Caste.QUEEN && homeDistance > territory * territory) {
            mob.setTarget(null);
            mob.getNavigation().moveTo(hx, hy, hz, 1.1);
            return;
        }
        if (caste != Caste.QUEEN && avoidKnownFire(level, mob, colony, home)) return;
        var restSite = caste == Caste.QUEEN || !colony.hasSpecializedRooms() ? OrganicNest.of(colony).throne : OrganicNest.FULL.rest;
        BlockPos rest = colony.hasOrganicNest() ? new BlockPos(home.x(), home.y(), home.z()).offset(restSite.x(), 0, restSite.z()) : roomPosition(home, colony.hasSpecializedRooms() && caste != Caste.QUEEN ? NestLayout.Room.REST : NestLayout.Room.CENTRAL);
        double restDistance = mob.distanceToSqr(rest.getX() + 0.5, rest.getY() + 1.0, rest.getZ() + 0.5);
        if (restDistance < 16.0 && mob.getTarget() == null && mob.getHealth() < mob.getMaxHealth()
                && level.getGameTime() % 1200L < 100L && consumeFood(level, colony, home, 1)) mob.heal(level.getBlockState(mob.blockPosition().below()).is(HivemindContent.MOLT_BED.get()) ? 2.0f : 1.0f);
        if (colony.hasSpecializedRooms() && caste != Caste.QUEEN && mob.getTarget() == null
                && mob.getHealth() < mob.getMaxHealth() * 0.5f && restDistance >= 16.0) {
            mob.getNavigation().moveTo(rest.getX() + 0.5, rest.getY() + 1.0, rest.getZ() + 0.5, 1.05);
            return;
        }
        if (caste != Caste.QUEEN && homeDistance > 64.0 && mob.getHealth() < mob.getMaxHealth() * 0.3f) {
            mob.setTarget(null);
            mob.getNavigation().moveTo(hx, hy, hz, 1.15);
            return;
        }
        if (deliverColonyReport(level, mob, colony, hx, hy, hz, homeDistance)) return;
        if (caste != Caste.SCOUT) {
            var intruder = findIntruder(level, mob, colony, home);
            if (intruder != null && (caste != Caste.QUEEN || !colony.hasOrganicNest() || intruder.distanceToSqr(hx, hy, hz) < 16) && (caste == Caste.WARRIOR || insideHive(level, colony, home, mob.blockPosition()))) {
                mob.setTarget(intruder);
                return;
            }
        }
        if (caste == Caste.WARRIOR && followRaid(level, mob, colony, home)) return;
        if (caste == Caste.WARRIOR && mob.getTarget() != null && com.hivemind.neoforge.content.HivemindWarrior.isPrey(mob.getTarget())
                && (!mob.getTarget().isAlive() || !mayHunt(level, mob, colony, home) || mob.getTarget().distanceToSqr(hx, hy, hz) > territory * territory)) {
            mob.setTarget(null);
            mob.getNavigation().stop();
        }
        if (caste == Caste.WARRIOR) {
            var target = mob.getTarget();
            boolean hunting = target != null && com.hivemind.neoforge.content.HivemindWarrior.isPrey(target);
            if (target == null || hunting) {
                protectAllies(level, mob, colony);
                if (mob.getTarget() == target) {
                    Player threat = findThreat(level, mob, colony, home);
                    if (threat != null) mob.setTarget(threat);
                }
            }
            if (mob.getTarget() instanceof Player threat) coordinateDefense(level, mob, threat, colony);
            if (mob.getTarget() == null && deliverHunt(level, mob, colony, home)) return;
            if (mob.getTarget() == null && !mob.entityTags().contains(HUNT_CARGO) && mayHunt(level, mob, colony, home)) hunt(level, mob, colony, home);
        }
        if (caste == Caste.QUEEN && mob.getTarget() != null && mob.getTarget().distanceToSqr(hx, hy, hz) > (colony.hasOrganicNest() ? 16.0 : 64.0)) mob.setTarget(null);
        if (caste == Caste.QUEEN && mob.getTarget() == null && homeDistance > (colony.hasOrganicNest() ? 16.0 : 64.0)) mob.getNavigation().moveTo(hx, hy, hz, 0.8);
        if (colony.hasSpecializedRooms() && caste == Caste.WARRIOR && mob.getTarget() == null
                && level.getSkyDarken() < 4) {
            BlockPos guard = roomPosition(home, colony.adaptations().contains("guard_core") ? NestLayout.Room.CENTRAL : NestLayout.Room.DEFENSE);
            if (colony.hasOrganicNest()) {
                var watch = colony.adaptations().contains("guard_core") ? OrganicNest.FULL.throne : OrganicNest.FULL.gate;
                guard = new BlockPos(home.x()+watch.x(), home.y(), home.z()+watch.z());
            }
            NestLayout.Position post = NestLayout.guardOffset(mob.getUUID());
            guard = guard.offset(post.x(), 0, post.z());
            if (mob.distanceToSqr(guard.getX() + 0.5, guard.getY() + 1.0, guard.getZ() + 0.5) > 2.25) {
                mob.getNavigation().moveTo(guard.getX() + 0.5, guard.getY() + 1.0, guard.getZ() + 0.5, 0.9);
                return;
            }
        }

        if (caste == Caste.WARRIOR && mob.getTarget() == null && level.getSkyDarken() >= 4) patrol(level, mob, home, colony.territoryRadius());

        if (caste == Caste.SCOUT) {
            observeFire(level, mob);
            String report = mob.entityTags().stream().filter(tag -> tag.startsWith("hivemind_report_")).findFirst().orElse(null);
            if (report == null) {
                for (Player player : level.players()) {
                    if (!player.isSpectator() && !player.isCreative() && player.isAlive() && mob.distanceToSqr(player) < 144.0 && mob.hasLineOfSight(player)) {
                        if (player.distanceToSqr(hx, hy, hz) < 64.0) {
                            mob.addTag(HiveIdentity.incursionReportTag(player.getUUID()));
                            return;
                        }
                        report = "hivemind_report_" + player.getUUID();
                        mob.addTag(report);
                        break;
                    }
                }
            }
            if (report != null) {
                if (homeDistance < 36.0) {
                    try {
                        PlayerMemory memory = colony.memoryFor(UUID.fromString(report.substring("hivemind_report_".length())));
                        if (memory.encounters() == 0 || level.getGameTime() - memory.lastEncounter() >= 1200L) memory.recordEncounter(level.getGameTime());
                    } catch (IllegalArgumentException ignored) { }
                    mob.removeTag(report);
                } else {
                    mob.getNavigation().moveTo(hx, hy, hz, 1.1);
                }
            }
            if (report == null) {
                scoutFood(level, mob, colony, home, homeDistance);
                var assignedPost = colony.outposts().stream().filter(Colony.Outpost::complete).toList();
                ColonyLocation post = assignedPost.isEmpty() ? null : assignedPost.get(Math.floorMod(mob.getUUID().hashCode(), assignedPost.size())).location();
                boolean carryingFoodReport = mob.entityTags().stream().anyMatch(tag -> tag.startsWith("hivemind_food_"));
                if (!carryingFoodReport && post != null
                        && post.dimension().equals(home.dimension()) && level.getGameTime() % 6000L < 2000L
                        && mob.distanceToSqr(post.x() + 0.5, post.y() + 1.0, post.z() + 0.5) > 16.0) {
                    mob.getNavigation().moveTo(post.x() + 0.5, post.y() + 1.0, post.z() + 0.5, 1.0);
                } else if (!carryingFoodReport) patrol(level, mob, home, level.getSkyDarken() >= 4 ? colony.territoryRadius() : 16);
            }
        } else if (caste == Caste.WORKER && mob.getTarget() == null) {
            if (mob.entityTags().contains("hivemind_carry_chitin") && !isChitin(mob.getMainHandItem())) mob.removeTag("hivemind_carry_chitin");
            if (mob.entityTags().contains("hivemind_carry_chitin")) {
                if (colony.hasSpecializedRooms() && !activeRepairCarriers(level, colony, home).contains(mob.getUUID())) {
                    returnChitinToStorage(level, mob, colony, home);
                    return;
                }
                if (findDamagedWall(level, colony, home) != null || findAnnexWork(level, colony, home) != null) {
                    repairWall(level, mob, colony, home);
                } else {
                    returnChitinToStorage(level, mob, colony, home);
                }
                return;
            }

            if (mob.entityTags().contains("hivemind_carry_food")) {
                var rejected = mob.getMainHandItem();
                if (!isFood(rejected)) {
                    if (!rejected.isEmpty()) mob.spawnAtLocation(level, rejected);
                    mob.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, net.minecraft.world.item.ItemStack.EMPTY);
                    mob.removeTag("hivemind_carry_food");
                    return;
                }
                if (colony.hasSpecializedRooms()) {
                    if (storageInventory(level, colony, home) == null || !approachStorage(level, mob, home)) return;
                } else {
                    BlockPos destination = roomPosition(home, NestLayout.Room.CENTRAL);
                    if (mob.distanceToSqr(destination.getX() + 0.5, destination.getY() + 1.0, destination.getZ() + 0.5) > 6.25) {
                        mob.getNavigation().moveTo(destination.getX() + 0.5, destination.getY() + 1.0, destination.getZ() + 0.5, 1.0);
                        return;
                    }
                }
                net.minecraft.world.item.ItemStack held = mob.getMainHandItem();
                if (!colony.hasSpecializedRooms()) {
                    colony.addFood(4);
                    mob.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, net.minecraft.world.item.ItemStack.EMPTY);
                    mob.removeTag("hivemind_carry_food");
                } else if (held.isEmpty()) {
                    mob.removeTag("hivemind_carry_food");
                } else if (storeStack(level, colony, home, held)) {
                    mob.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, net.minecraft.world.item.ItemStack.EMPTY);
                    mob.removeTag("hivemind_carry_food");
                }
                return;
            }
            if (colony.hasSpecializedRooms() && (colony.hasOutposts() || settings.expansion() && colony.totalPopulation() >= 10)
                    && availableFood(level, colony, home) > 0
                    && isOutpostBuilder(level, mob, colony, home) && workOnOutposts(level, mob, colony, home)) return;

            if (colony.hasSpecializedRooms() && isGrowthExcavator(level, mob, colony, home)) {
                ExcavationJob excavation = findGrowthExcavation(level, mob, colony, home);
                if (excavation != null) { excavateGrowth(level, mob, colony, home, excavation); return; }
            }

            var food = level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, mob.getBoundingBox().inflate(12.0),
                    item -> !item.getItem().isEmpty() && isFood(item.getItem()));
            if (food.isEmpty()) {
                boolean needsChitin = findDamagedWall(level, colony, home) != null || findAnnexWork(level, colony, home) != null;
                net.minecraft.world.Container supplies = needsChitin ? storageInventory(level, colony, home) : null;
                if (REPAIR_BACKOFF.ready(mob.getUUID(), level.getGameTime()) && supplies != null && mayFetchRepairMaterial(level, mob, colony, home) && mob.getMainHandItem().isEmpty()) {
                    for (int i = 0; i < supplies.getContainerSize(); i++) {
                        net.minecraft.world.item.ItemStack stored = supplies.getItem(i);
                        if (!isChitin(stored)) continue;
                        if (approachStorage(level, mob, home)) {
                            net.minecraft.world.item.ItemStack carried = stored.split(1);
                            supplies.setChanged();
                            mob.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, carried);
                            mob.addTag("hivemind_carry_chitin");
                        }
                        return;
                    }
                }
                if (REPAIR_BACKOFF.ready(mob.getUUID(), level.getGameTime()) && activeRepairCarriers(level, colony, home).size() < 2 && (needsChitin || colony.hasSpecializedRooms())) {
                    var dropped = level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, mob.getBoundingBox().inflate(12.0), item -> isChitin(item.getItem()));
                    var nearest = dropped.stream().min(java.util.Comparator.comparingDouble(mob::distanceToSqr)).orElse(null);
                    if (nearest != null) {
                        if (mob.distanceToSqr(nearest) < 2.25) {
                            if (!mob.getMainHandItem().isEmpty()) return;
                            net.minecraft.world.item.ItemStack carried = nearest.getItem().split(1);
                            if (carried.isEmpty()) return;
                            if (nearest.getItem().isEmpty()) nearest.discard();
                            mob.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, carried);
                            mob.addTag("hivemind_carry_chitin");
                        } else mob.getNavigation().moveTo(nearest, 1.0);
                        return;
                    }
                }
            }

            ColonyLocation site = colony.foodSite();
            if (food.isEmpty() && site != null) {
                if (!site.dimension().equals(home.dimension()) || level.getGameTime() - colony.foodSiteTime() > 24000L) {
                    colony.clearFoodSite();
                } else if (mob.distanceToSqr(site.x() + 0.5, site.y() + 0.5, site.z() + 0.5) > 16.0) {
                    mob.getNavigation().moveTo(site.x() + 0.5, site.y(), site.z() + 0.5, 1.0);
                    return;
                } else colony.clearFoodSite();
            }

            if (!food.isEmpty()) {
                var item = food.stream().min(java.util.Comparator.comparingDouble(mob::distanceToSqr)).orElse(null);
                if (item == null) return;
                if (mob.distanceToSqr(item) < 2.25) {
                    if (!mob.getMainHandItem().isEmpty()) return;
                    net.minecraft.world.item.ItemStack carried = item.getItem().split(1);
                    if (carried.isEmpty()) return;
                    if (item.getItem().isEmpty()) item.discard();
                    mob.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, carried);
                    mob.addTag("hivemind_carry_food");
                } else mob.getNavigation().moveTo(item, 1.0);
            }
            if (food.isEmpty() && colony.hasSpecializedRooms() && mob.distanceToSqr(hx, hy, hz) > 196.0) {
                mob.getNavigation().moveTo(hx, hy, hz, 0.9);
            }
        }
    }

    private static boolean deliverColonyReport(ServerLevel level, Mob mob, Colony colony, double hx, double hy, double hz, double homeDistance) {
        String loss = mob.entityTags().stream().filter(tag -> tag.startsWith("hivemind_loss_") && tag.length() >= "hivemind_loss_".length() + 36).findFirst().orElse(null);
        if (loss != null) {
            if (homeDistance < 36.0) {
                var report = HiveIdentity.lossReport(loss);
                if (report.isPresent()) {
                    var delivered = report.get();
                    colony.recordWitnessedLoss(delivered.attacker(), delivered.tactic(), level.getGameTime());
                    save(server);
                }
                mob.removeTag(loss);
            } else {
                mob.setTarget(null);
                mob.getNavigation().moveTo(hx, hy, hz, 1.1);
            }
            return true;
        }

        String damageReport = mob.entityTags().stream().filter(tag -> tag.startsWith("hivemind_damage_")).findFirst().orElse(null);
        if (damageReport != null) {
            if (homeDistance < 36.0) {
                var attacker = HiveIdentity.damageReportAttacker(damageReport);
                if (attacker.isPresent()) {
                    colony.recordWitnessedDamage(attacker.get(), 1, level.getGameTime());
                    save(server);
                }
                mob.removeTag(damageReport);
            } else {
                mob.setTarget(null);
                mob.getNavigation().moveTo(hx, hy, hz, 1.1);
            }
            return true;
        }

        String fireReport = mob.entityTags().stream().filter(tag -> tag.startsWith("hivemind_fire_")).findFirst().orElse(null);
        if (fireReport != null) {
            if (homeDistance < 36.0) {
                var playerId = HiveIdentity.fireReportPlayer(fireReport);
                if (playerId.isPresent()) { colony.recordWitnessedFire(playerId.get(), level.getGameTime()); save(server); }
                mob.removeTag(fireReport);
            } else {
                mob.setTarget(null);
                mob.getNavigation().moveTo(hx, hy, hz, 1.1);
            }
            return true;
        }

        String incursionReport = mob.entityTags().stream().filter(tag -> tag.startsWith("hivemind_incursion_")).findFirst().orElse(null);
        if (incursionReport != null) {
            if (homeDistance < 36.0) {
                var playerId = HiveIdentity.incursionReportPlayer(incursionReport);
                if (playerId.isPresent()) { colony.recordWitnessedIncursion(playerId.get(), level.getGameTime()); save(server); }
                mob.removeTag(incursionReport);
            } else {
                mob.setTarget(null);
                mob.getNavigation().moveTo(hx, hy, hz, 1.1);
            }
            return true;
        }
        return false;
    }

    private static void observeFire(ServerLevel level, Mob scout) {
        if (scout.entityTags().stream().anyMatch(tag -> tag.startsWith("hivemind_fire_"))) return;
        for (Player player : level.players()) {
            if (!player.isAlive() || player.isCreative() || player.isSpectator() || scout.distanceToSqr(player) > 144.0 || !scout.hasLineOfSight(player)) continue;
            var held = player.getMainHandItem();
            if (!held.is(net.minecraft.world.item.Items.FLINT_AND_STEEL) && !held.is(net.minecraft.world.item.Items.FIRE_CHARGE)) continue;
            BlockPos center = player.blockPosition();
            for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) for (int y = -1; y <= 1; y++) {
                var state = level.getBlockState(center.offset(x, y, z));
                if (state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE)) {
                    scout.addTag(HiveIdentity.fireReportTag(player.getUUID()));
                    return;
                }
            }
        }
    }

    private static boolean avoidKnownFire(ServerLevel level, Mob mob, Colony colony, ColonyLocation home) {
        if (!colony.adaptations().contains("avoid_fire")) return false;
        BlockPos origin = mob.blockPosition();
        BlockPos fire = null;
        for (int x = -1; x <= 1 && fire == null; x++) for (int z = -1; z <= 1 && fire == null; z++) for (int y = 0; y <= 1; y++) {
            BlockPos candidate = origin.offset(x, y, z);
            var state = level.getBlockState(candidate);
            if (state.is(Blocks.FIRE) || state.is(Blocks.SOUL_FIRE)) { fire = candidate; break; }
        }
        if (fire == null && !mob.isOnFire()) return false;
        double dx = fire == null ? home.x() + 0.5 - mob.getX() : mob.getX() - fire.getX() - 0.5;
        double dz = fire == null ? home.z() + 0.5 - mob.getZ() : mob.getZ() - fire.getZ() - 0.5;
        double length = Math.sqrt(dx * dx + dz * dz);
        if (length < 0.01) { dx = 1; dz = 0; length = 1; }
        mob.setTarget(null);
        mob.getNavigation().moveTo(mob.getX() + 6 * dx / length, mob.getY(), mob.getZ() + 6 * dz / length, 1.2);
        return true;
    }

    private static Player findThreat(ServerLevel level, Mob mob, Colony colony, ColonyLocation home) {
        var memories = colony.memorySnapshot();
        boolean night = level.getSkyDarken() >= 4;
        Player best = null;
        int priority = -1;
        double distance = Double.MAX_VALUE;
        for (var player : level.players()) {
            if (!player.isAlive() || player.isCreative() || player.isSpectator()) continue;
            var known = memories.get(player.getUUID());
            int threat = known == null ? 0 : known.threatLevel().ordinal();
            boolean nearCenter = player.distanceToSqr(home.x() + 0.5, home.y() + 1.0, home.z() + 0.5) < 256.0;
            boolean nearOutpost = colony.outposts().stream().filter(Colony.Outpost::complete).map(Colony.Outpost::location)
                    .anyMatch(post -> player.distanceToSqr(post.x() + 0.5, post.y() + 1.0, post.z() + 0.5) < 64.0);
            if (!nearCenter && !nearOutpost && (!night || threat < com.hivemind.core.ThreatLevel.HOSTILE.ordinal())) continue;
            int reach = known != null && known.tacticCount(CombatTactic.RANGED) >= 2 ? 32 : 24;
            double separation = mob.distanceToSqr(player);
            if (separation > reach * reach || threat < priority || threat == priority && separation >= distance || !mob.hasLineOfSight(player)) continue;
            if (player.distanceToSqr(home.x() + 0.5, home.y() + 1.0, home.z() + 0.5) > (double) colony.territoryRadius() * colony.territoryRadius()) continue;
            best = player;
            priority = threat;
            distance = separation;
        }
        return best;
    }

    private static void coordinateDefense(ServerLevel level, Mob defender, Player target, Colony colony) {
        if (target.isCreative() || target.isSpectator() || !target.isAlive()) return;
        PlayerMemory memory = colony.memorySnapshot().get(target.getUUID());
        if (memory == null) return;
        if (memory.tacticCount(CombatTactic.RANGED) >= 2 && colony.adaptations().contains("anti_ranged")
                && defender.distanceToSqr(target) > 36.0 && defender.distanceToSqr(target) < 256.0) {
            double dx = target.getX() - defender.getX(), dz = target.getZ() - defender.getZ();
            double length = Math.hypot(dx, dz);
            if (length > 1.0) {
                int side = (defender.getUUID().hashCode() & 1) == 0 ? 1 : -1;
                tryRoamingPath(level, defender, (int) Math.floor(defender.getX() + (dx + side * dz) * 4.0 / length),
                        (int) Math.floor(defender.getY()), (int) Math.floor(defender.getZ() + (dz - side * dx) * 4.0 / length), 1.1);
            }
        }
        boolean flank = memory.tacticCount(CombatTactic.MELEE) >= 2 && colony.adaptations().contains("anti_melee");
        boolean defend = memory.tacticCount(CombatTactic.INCURSION) >= 2 && colony.adaptations().contains("guard_core");
        if (!flank && !defend) return;
        int recruited = 0;
        for (Mob ally : level.getEntitiesOfClass(Mob.class, defender.getBoundingBox().inflate(10.0), candidate -> candidate != defender && candidate.isAlive())) {
            if (ally.getTarget() != null || ally.distanceToSqr(target) > 144.0 || !ally.hasLineOfSight(target)) continue;
            if (HiveIdentity.colonyId(ally.entityTags()).filter(colony.id()::equals).isEmpty()) continue;
            if (HiveIdentity.caste(ally.entityTags()).orElse(null) != Caste.WARRIOR) continue;
            ally.setTarget(target);
            if (flank) {
                double dx = target.getX() - defender.getX(), dz = target.getZ() - defender.getZ();
                double length = Math.hypot(dx, dz);
                if (length > 1.0) {
                    int side = recruited % 2 == 0 ? 1 : -1;
                    tryRoamingPath(level, ally, (int) Math.floor(target.getX() + side * dz * 4.0 / length),
                            (int) Math.floor(target.getY()), (int) Math.floor(target.getZ() - side * dx * 4.0 / length), 1.1);
                }
            }
            if (++recruited >= 2) break;
        }
    }

    private static void protectAllies(ServerLevel level, Mob defender, Colony colony) {
        var allies = level.getEntitiesOfClass(Mob.class, defender.getBoundingBox().inflate(12.0), ally -> ally != defender && ally.isAlive());
        for (Mob ally : allies) {
            if (HiveIdentity.colonyId(ally.entityTags()).filter(colony.id()::equals).isEmpty()) continue;
            Caste caste = HiveIdentity.caste(ally.entityTags()).orElse(null);
            if (caste != Caste.SCOUT && caste != Caste.WORKER && caste != Caste.QUEEN) continue;
            var attacker = ally.getLastHurtByMob();
            if (attacker == null || !attacker.isAlive() || ally.tickCount - ally.getLastHurtByMobTimestamp() > 200 || !defender.hasLineOfSight(ally)) continue;
            if (attacker instanceof Player player && (player.isCreative() || player.isSpectator())) continue;
            if (HiveIdentity.colonyId(attacker.entityTags()).filter(colony.id()::equals).isPresent()) continue;
            defender.setTarget(attacker);
            return;
        }
    }


    private static void patrol(ServerLevel level, Mob mob, ColonyLocation home, int radius) {
        if (!mob.getNavigation().isDone() || mob.getTarget() != null) return;
        var random = level.getRandom();
        for (int attempt = 0; attempt < 4; attempt++) {
            int x = (int) Math.floor(mob.getX()) + random.nextInt(25) - 12;
            int z = (int) Math.floor(mob.getZ()) + random.nextInt(25) - 12;
            if (Math.hypot(x - home.x(), z - home.z()) > radius - 2) continue;
            if (tryRoamingPath(level, mob, x, (int) Math.floor(mob.getY()), z, 0.95)) break;
        }
    }

    private static boolean tryRoamingPath(ServerLevel level, Mob mob, int x, int y, int z, double speed) {
        for (int step : new int[]{0, 1, -1, 2, -2}) {
            BlockPos stand = new BlockPos(x, y + step, z);
            if (!level.hasChunkAt(stand) || !level.getWorldBorder().isWithinBounds(stand) || !level.getBlockState(stand).isAir()) continue;
            var route = mob.getNavigation().createPath(stand, 0);
            if (route != null && route.canReach()) return mob.getNavigation().moveTo(route, speed);
        }
        return false;
    }

    private static final String HUNT_CARGO = "hivemind_hunt_cargo";

    private static boolean deliverHunt(ServerLevel level, Mob mob, Colony colony, ColonyLocation home) {
        if (mob.entityTags().contains(HUNT_CARGO)) {
            var held = mob.getMainHandItem();
            if (!isFood(held)) {
                mob.removeTag(HUNT_CARGO);
                return false;
            }
            if (colony.hasSpecializedRooms()) {
                if (storageInventory(level, colony, home) == null) return false;
                if (!approachStorage(level, mob, home)) return true;
                if (!storeStack(level, colony, home, held)) { mob.spawnAtLocation(level, held); }
            } else {
                if (mob.distanceToSqr(home.x() + 0.5, home.y() + 1.0, home.z() + 0.5) > 6.25) {
                    mob.getNavigation().moveTo(home.x() + 0.5, home.y() + 1.0, home.z() + 0.5, 1.05);
                    return true;
                }
                colony.addFood(held.getCount() * 4);
            }
            mob.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, net.minecraft.world.item.ItemStack.EMPTY);
            mob.removeTag(HUNT_CARGO);
            return true;
        }
        return false;
    }

    public static boolean collectHunt(net.minecraft.world.entity.LivingEntity prey, net.minecraft.world.damagesource.DamageSource source, net.minecraft.world.item.ItemStack meat) {
        if (!registryReady || !settings.hunting() || !isFood(meat)
                || !com.hivemind.neoforge.content.HivemindWarrior.isPrey(prey)
                || !(source.getEntity() instanceof com.hivemind.neoforge.content.HivemindWarrior hunter)
                || !hunter.isAlive() || !hunter.getMainHandItem().isEmpty() || hunter.distanceToSqr(prey) > 36.0) return false;
        var colony = HiveIdentity.colonyId(hunter.entityTags()).flatMap(colonies::get).orElse(null);
        if (colony == null || colony.state() != ColonyState.ACTIVE || colony.home() == null) return false;
        hunter.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND, meat.copy());
        hunter.setDropChance(net.minecraft.world.entity.EquipmentSlot.MAINHAND, 2.0f);
        hunter.addTag(HUNT_CARGO);
        if (hunter.getTarget() == prey) hunter.setTarget(null);
        return true;
    }

    private static boolean mayHunt(ServerLevel level, Mob mob, Colony colony, ColonyLocation home) {
        return settings.hunting() && level.getSkyDarken() >= 4
                && mob.getHealth() >= mob.getMaxHealth() * 0.6f
                && availableFood(level, colony, home) < huntingReserve(colony);
    }

    private static void hunt(ServerLevel level, Mob mob, Colony colony, ColonyLocation home) {
        double territory = colony.territoryRadius();
        double searchRadius = Math.min(48.0, territory);
        var prey = level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class, mob.getBoundingBox().inflate(searchRadius),
                animal -> com.hivemind.neoforge.content.HivemindWarrior.isPrey(animal) && animal.isAlive() && !animal.isBaby() && !animal.hasCustomName()
                        && mob.distanceToSqr(animal) <= searchRadius * searchRadius
                        && animal.distanceToSqr(home.x() + 0.5, home.y() + 1.0, home.z() + 0.5) <= territory * territory
                        && mob.hasLineOfSight(animal));
        prey.sort(java.util.Comparator.comparingDouble(mob::distanceToSqr));
        for (var animal : prey.stream().limit(8).toList()) {
            var route = mob.getNavigation().createPath(animal, 0);
            if (route != null && route.canReach()) {
                mob.setTarget(animal);
                return;
            }
        }
    }

    private static void scoutFood(ServerLevel level, Mob mob, Colony colony, ColonyLocation home, double homeDistance) {
        String report = mob.entityTags().stream().filter(tag -> tag.startsWith("hivemind_food_")).findFirst().orElse(null);
        if (report == null) {
            var found = level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, mob.getBoundingBox().inflate(12.0),
                    item -> !item.getItem().isEmpty() && isFood(item.getItem()) && mob.hasLineOfSight(item));
            if (!found.isEmpty()) {
                BlockPos pos = found.get(0).blockPosition();
                report = "hivemind_food_" + pos.getX() + "_" + pos.getY() + "_" + pos.getZ();
                mob.addTag(report);
            }
        }
        if (report == null) return;
        if (homeDistance >= 36.0) {
            mob.getNavigation().moveTo(home.x() + 0.5, home.y() + 1.0, home.z() + 0.5, 1.1);
            return;
        }
        try {
            String[] parts = report.substring("hivemind_food_".length()).split("_");
            if (parts.length == 3) colony.recordFoodSite(new ColonyLocation(home.dimension(), Integer.parseInt(parts[0]),
                    Integer.parseInt(parts[1]), Integer.parseInt(parts[2])), level.getGameTime());
        } catch (IllegalArgumentException ignored) { }
        mob.removeTag(report);
    }


    private static int nestRadius(ServerLevel level, ColonyLocation home) {
        BlockPos center = new BlockPos(home.x(), home.y(), home.z());
        if (level.getBlockState(center.offset(OrganicNest.SMALL.throne.x(), 0, OrganicNest.SMALL.throne.z())).is(HivemindContent.HIVE_CORE.get())) return 3;
        int oldWalls = 0;
        if (level.getBlockState(center.offset(3, 1, 2)).is(HivemindContent.HIVE_CHITIN.get())) oldWalls++;
        if (level.getBlockState(center.offset(-3, 1, 2)).is(HivemindContent.HIVE_CHITIN.get())) oldWalls++;
        if (level.getBlockState(center.offset(2, 1, -3)).is(HivemindContent.HIVE_CHITIN.get())) oldWalls++;
        return oldWalls >= 2 ? 3 : 6;
    }
    private static boolean annexSpaceClear(ServerLevel level, ColonyLocation home) {
        BlockPos center = new BlockPos(home.x(), home.y(), home.z());
        int radius = nestRadius(level, home);
        if (!level.hasChunkAt(center.offset(radius + 5, 0, -2)) || !level.hasChunkAt(center.offset(radius + 5, 0, 2))) return false;
        for (int x = radius + 2; x <= radius + 4; x++) for (int z = -1; z <= 1; z++) for (int y = 1; y <= 2; y++)
            if (!level.getBlockState(center.offset(x, y, z)).isAir()) return false;
        return true;
    }

    private static BlockPos findGrowthWork(ServerLevel level, Colony colony, ColonyLocation home) {
        return findGrowthWork(level, colony, home, null);
    }

    private static BlockPos findGrowthWork(ServerLevel level, Colony colony, ColonyLocation home, Mob worker) {
        if (!settings.expansion() || hasSouthernOutpost(colony, home)) return null;
        if (colony.population(Caste.WORKER) < 3 || availableFood(level, colony, home) < 8) return null;
        BlockPos origin = new BlockPos(home.x(), home.y(), home.z());
        for (int z = 14; z <= 26; z++) for (int x = -3; x <= 3; x++) {
            if (!com.hivemind.core.GrowthLayout.inFootprint(x, z)) continue;
            BlockPos floor = origin.offset(x, 0, z);
            if (!level.hasChunkAt(floor) || (!level.getBlockState(floor).isAir() && !level.getBlockState(floor).isFaceSturdy(level, floor, net.minecraft.core.Direction.UP))) return null;
            for (int y = 1; y <= com.hivemind.core.GrowthLayout.ROOF; y++) {
                BlockPos cell = floor.offset(0, y, 0);
                var state = level.getBlockState(cell);
                if (level.getBlockEntity(cell) != null || !state.isAir() && !(com.hivemind.core.GrowthLayout.isShell(x, y, z) && state.is(HivemindContent.HIVE_CHITIN))) return null;
            }
        }
        for (NestLayout.Position part : com.hivemind.core.GrowthLayout.shell()) {
            BlockPos cell = origin.offset(part.x(), part.y(), part.z());
            if (level.getBlockState(cell).isAir() && (worker == null || repairApproach(level, worker, home, cell) != null)) return cell;
        }
        return null;
    }

    private static BlockPos findAnnexWork(ServerLevel level, Colony colony, ColonyLocation home) {
        if (!settings.expansion()) return null;
        if (colony.hasSpecializedRooms()) return findGrowthWork(level, colony, home);
        if (colony.population(Caste.WORKER) < 3 || availableFood(level, colony, home) < 8 || !annexSpaceClear(level, home)) return null;
        BlockPos center = new BlockPos(home.x(), home.y(), home.z());
        int radius = nestRadius(level, home);
        for (int y = 0; y <= 3; y++) for (int x = radius + 1; x <= radius + 5; x++) for (int z = -2; z <= 2; z++) {
            if (x != radius + 1 && x != radius + 5 && z != -2 && z != 2 && y != 0 && y != 3) continue;
            if (x == radius + 1 && z == 0 && y >= 1 && y <= 2) continue;
            BlockPos position = center.offset(x, y, z);
            if (level.getBlockState(position).isAir()) return position;
        }
        return null;
    }

    private static void openFinishedAnnex(ServerLevel level, Colony colony, ColonyLocation home) {
        if (!settings.expansion() || colony.hasSpecializedRooms()) return;
        if (colony.population(Caste.WORKER) < 3 || availableFood(level, colony, home) < 8 || !annexSpaceClear(level, home) || findAnnexWork(level, colony, home) != null) return;
        BlockPos center = new BlockPos(home.x(), home.y(), home.z());
        for (int y = 1; y <= 2; y++) {
            BlockPos doorway = center.offset(nestRadius(level, home), y, 0);
            if (level.getBlockState(doorway).is(HivemindContent.HIVE_CHITIN.get())) level.setBlock(doorway, Blocks.AIR.defaultBlockState(), 3);
        }
    }


    private static BlockPos findDamagedWall(ServerLevel level, Colony colony, ColonyLocation home) {
        return findDamagedWall(level, colony, home, null);
    }

    private static BlockPos findDamagedWall(ServerLevel level, Colony colony, ColonyLocation home, Mob worker) {
        BlockPos center = new BlockPos(home.x(), home.y(), home.z());
        if (colony.hasOrganicNest()) {
            for (var part : OrganicNest.of(colony).repairs) {
                var cell = part.position();
                BlockPos target = center.offset(cell.x(), cell.y(), cell.z());
                if (!level.hasChunkAt(target) || !level.getBlockState(target).isAir()) continue;
                if (colony.outposts().stream().anyMatch(entry -> OutpostTunnelLayout.isNestExit(cell.x(), cell.y(), cell.z(),
                        entry.location().x()-home.x(), entry.location().z()-home.z()))) continue;
                if (worker == null || repairApproach(level, worker, home, target) != null) return target;
            }
            return null;
        }
        int radius = nestRadius(level, home);
        for (int y = 1; y <= (radius == 3 ? 3 : 4); y++) for (int x = -radius; x <= radius; x++) for (int z = -radius; z <= radius; z++) {
            if (colony.hasFacetedRooms() || Math.abs(x) != radius && Math.abs(z) != radius) continue;
            if (colony.hasSpecializedRooms() && NestLayout.isCentralDoor(x, y, z)) continue;
            if (!colony.hasSpecializedRooms() && (Math.abs(x) <= 1 && z == radius && y <= 3 || x == radius && z == 0 && y <= 2)) continue;
            BlockPos position = center.offset(x, y, z);
            if (level.getBlockState(position).isAir() && (worker == null || repairApproach(level, worker, home, position) != null)) return position;
        }
        if (colony.hasSpecializedRooms()) for (NestLayout.Room room : NestLayout.Room.values()) {
            NestLayout.Position offset = NestLayout.center(room);
            BlockPos roomCenter = center.offset(offset.x(), 0, offset.z());
            int r = room == NestLayout.Room.CENTRAL ? 6 : 3;
            if (!level.hasChunkAt(roomCenter.offset(-r, 0, -r)) || !level.hasChunkAt(roomCenter.offset(-r, 0, r)) || !level.hasChunkAt(roomCenter.offset(r, 0, -r)) || !level.hasChunkAt(roomCenter.offset(r, 0, r))) continue;
            for (int y = 0; y <= 4; y++) for (int x = -r; x <= r; x++) for (int z = -r; z <= r; z++) {
                if (colony.hasVaultedRooms()) {
                    if (!NestLayout.vaultedShell(r, x, y, z) && !NestLayout.reserveCyst(offset.x() + x, y, offset.z() + z)) continue;
                } else if (colony.hasFacetedRooms()) {
                    if (!NestLayout.cellShell(r, x, y, z)) continue;
                } else if (y > 0 && Math.abs(x) != r && Math.abs(z) != r) continue;
                if (NestLayout.isRoomDoor(room, x, y, z)) continue;
                boolean exit = false;
                for (var entry : colony.outposts()) {
                    var post = entry.location();
                    if (OutpostTunnelLayout.isRoomExit(room, x, y, z, post.x() - home.x(), post.z() - home.z())) { exit = true; break; }
                }
                if (exit) continue;
                BlockPos position = roomCenter.offset(x, y, z);
                if (level.getBlockState(position).isAir() && (worker == null || repairApproach(level, worker, home, position) != null)) return position;
            }
        }
        return null;
    }

    private static BlockPos repairApproach(ServerLevel level, Mob worker, ColonyLocation home, BlockPos target) {
        net.minecraft.world.phys.AABB occupied = target.getY() == home.y()
                ? new net.minecraft.world.phys.AABB(target.getX(), target.getY() - 1, target.getZ(), target.getX() + 1, target.getY() + 1, target.getZ() + 1)
                : new net.minecraft.world.phys.AABB(target);
        if (!level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class, occupied, entity -> entity.isAlive()).isEmpty()) return null;
        for (int[] side : BUILD_APPROACHES) {
            if (side[0] == 0 && side[1] == 0 && target.getY() < home.y() + 3) continue;
            BlockPos stand = new BlockPos(target.getX() + side[0], home.y() + 1, target.getZ() + side[1]);
            BlockPos floor = stand.offset(0, -1, 0);
            if (!level.hasChunkAt(stand) || !level.getBlockState(stand).isAir()
                    || !level.getBlockState(stand.offset(0, 1, 0)).isAir()
                    || !level.getBlockState(floor).isFaceSturdy(level, floor, net.minecraft.core.Direction.UP)) continue;
            if (worker.distanceToSqr(stand.getX() + 0.5, stand.getY(), stand.getZ() + 0.5) <= 2.25) return stand;
            var path = worker.getNavigation().createPath(stand, 0);
            if (path != null && path.canReach()) return stand;
        }
        return null;
    }

    private static void repairWall(ServerLevel level, Mob mob, Colony colony, ColonyLocation home) {
        BlockPos position = findDamagedWall(level, colony, home, mob);
        if (position == null) {
            position = colony.hasSpecializedRooms() ? findGrowthWork(level, colony, home, mob) : findAnnexWork(level, colony, home);
            if (position != null && !colony.hasSpecializedRooms()) {
                BlockPos center = new BlockPos(home.x(), home.y(), home.z());
                for (int y = 1; y <= 2; y++) {
                    BlockPos door = center.offset(nestRadius(level, home), y, 0);
                    if (level.getBlockState(door).is(HivemindContent.HIVE_CHITIN.get())) level.setBlock(door, Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }
        if (position == null) { REPAIR_BACKOFF.defer(mob.getUUID(), level.getGameTime()); returnChitinToStorage(level, mob, colony, home); return; }
        BlockPos stand = repairApproach(level, mob, home, position);
        if (stand == null) { REPAIR_BACKOFF.defer(mob.getUUID(), level.getGameTime()); returnChitinToStorage(level, mob, colony, home); return; }
        if (mob.distanceToSqr(stand.getX() + 0.5, stand.getY(), stand.getZ() + 0.5) > 2.25) {
            mob.getNavigation().moveTo(stand.getX() + 0.5, stand.getY(), stand.getZ() + 0.5, 0.9);
            return;
        }
        var material = colony.hasFacetedRooms()
                ? NestLayout.repairMaterial(position.getX() - home.x(), position.getY() - home.y(), position.getZ() - home.z())
                : NestLayout.Material.CHITIN;
        if (colony.hasVaultedRooms() && NestLayout.reserveCyst(position.getX() - home.x(), position.getY() - home.y(), position.getZ() - home.z()))
            material = NestLayout.Material.RESERVE_CYST;
        if (colony.hasOrganicNest()) {
            var planned = OrganicNest.of(colony).at(position.getX()-home.x(), position.getY()-home.y(), position.getZ()-home.z());
            material = planned == null || planned == NestLayout.Material.AIR ? NestLayout.Material.CHITIN : planned;
        }
        if (level.setBlock(position, nestBlock(material).defaultBlockState(), 3)) {
            if (isChitin(mob.getMainHandItem())) mob.getMainHandItem().shrink(1);
            mob.removeTag("hivemind_carry_chitin");
            openFinishedAnnex(level, colony, home);
        }
    }


    private static final net.minecraft.tags.TagKey<net.minecraft.world.level.block.Block> OUTPOST_VEGETATION = net.minecraft.tags.TagKey.create(net.minecraft.core.registries.Registries.BLOCK, net.minecraft.resources.Identifier.fromNamespaceAndPath("hivemind", "outpost_vegetation"));

    private static boolean outpostVegetation(net.minecraft.world.level.block.state.BlockState state) { return state.is(OUTPOST_VEGETATION); }

    private record OutpostAccessJob(BlockPos target, BlockPos approach, boolean placeFloor, boolean replaceFloor) {}

    private static boolean outpostAccessOpen(ServerLevel level, ColonyLocation home, ColonyLocation post) {
        if (home == null || !home.dimension().equals(post.dimension()) || home.y() != post.y()) return false;
        BlockPos origin = new BlockPos(home.x(), home.y(), home.z());
        return OutpostTunnelLayout.accessClear(post.x() - home.x(), post.z() - home.z(), part -> {
            BlockPos cell = origin.offset(part.x(), part.y(), part.z());
            return level.hasChunkAt(cell) && level.getWorldBorder().isWithinBounds(cell)
                    && level.getFluidState(cell).isEmpty() && level.getBlockState(cell).isFaceSturdy(level, cell, net.minecraft.core.Direction.UP);
        }, part -> {
            BlockPos cell = origin.offset(part.x(), part.y(), part.z());
            return level.hasChunkAt(cell) && level.getWorldBorder().isWithinBounds(cell)
                    && level.getFluidState(cell).isEmpty() && level.getBlockState(cell).isAir();
        });
    }

    private static String outpostPlanIssue(ServerLevel level, ColonyLocation home, ColonyLocation post) {
        if (home == null || post == null || !home.dimension().equals(post.dimension()) || post.y() != home.y()) return "ubicación incompatible con la colmena";
        int dx = post.x() - home.x(), dz = post.z() - home.z();
        if (!OutpostTunnelLayout.supported(dx, dz)) return "trazado desconocido";
        BlockPos origin = new BlockPos(home.x(), home.y(), home.z());
        boolean canDig = level.getGameRules().get(net.minecraft.world.level.gamerules.GameRules.MOB_GRIEFING);
        for (NestLayout.Position part : OutpostTunnelLayout.corridor(dx, dz)) {
            BlockPos cell = origin.offset(part.x(), part.y(), part.z());
            String issue = outpostCellIssue(level, cell);
            if (issue != null) return issue;
            var state = level.getBlockState(cell);
            if (state.isAir()) continue;
            if (part.y() == 0) {
                if (!state.isFaceSturdy(level, cell, net.minecraft.core.Direction.UP)) {
                    if (!outpostVegetation(state)) return outpostIssue("suelo inestable", cell);
                    if (!canDig) return outpostIssue("mobGriefing desactivado", cell);
                }
                continue;
            }
            boolean door = OutpostTunnelLayout.isNestExit(part.x(), part.y(), part.z(), dx, dz) && state.is(HivemindContent.HIVE_CHITIN);
            if (!excavationMaterial(state) && !outpostVegetation(state) && !door) return outpostIssue("bloque no excavable " + state.getBlock(), cell);
            if (!canDig) return outpostIssue("mobGriefing desactivado", cell);
        }
        for (NestLayout.Position part : OutpostTunnelLayout.chamber(dx, dz)) {
            BlockPos cell = origin.offset(part.x(), part.y(), part.z());
            String issue = outpostCellIssue(level, cell);
            if (issue != null) return issue;
            var state = level.getBlockState(cell);
            if (state.isAir()) continue;
            boolean shell = OutpostLayout.isShell(part.x() - dx, part.y(), part.z() - dz, OutpostTunnelLayout.side(dx, dz)) && state.is(HivemindContent.HIVE_CHITIN);
            if (shell) continue;
            boolean door = (OutpostTunnelLayout.isPostDoor(part.x(), part.y(), part.z(), dx, dz)
                    || OutpostLayout.isLegacyInterior(part.x() - dx, part.y(), part.z() - dz)) && state.is(HivemindContent.HIVE_CHITIN);
            if (!excavationMaterial(state) && !outpostVegetation(state) && !door) {
                String block = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
                String reason = OutpostTunnelLayout.isConnectingDoor(part, dx, dz) ? "entrada obstruida por " : "bloque no excavable ";
                return outpostIssue(reason + block, cell);
            }
            if (!canDig) return outpostIssue("mobGriefing desactivado", cell);
        }
        return null;
    }

    private static String outpostCellIssue(ServerLevel level, BlockPos cell) {
        if (!level.hasChunkAt(cell)) return outpostIssue("chunk descargado", cell);
        if (!level.getWorldBorder().isWithinBounds(cell)) return outpostIssue("fuera del borde del mundo", cell);
        if (!level.getFluidState(cell).isEmpty()) return outpostIssue("fluido en el plano", cell);
        if (level.getBlockEntity(cell) != null) return outpostIssue("bloque con datos " + level.getBlockState(cell).getBlock(), cell);
        return null;
    }

    private static String outpostIssue(String reason, BlockPos cell) {
        return reason + " en " + cell.getX() + ", " + cell.getY() + ", " + cell.getZ();
    }

    private static boolean outpostBreakSafe(ServerLevel level, BlockPos target) {
        if (level.getBlockEntity(target) != null || !(level.getGameRules().get(net.minecraft.world.level.gamerules.GameRules.MOB_GRIEFING))) return false;
        for (net.minecraft.core.Direction dir : net.minecraft.core.Direction.values()) {
            BlockPos neighbor = target.relative(dir);
            if (!level.hasChunkAt(neighbor) || !level.getFluidState(neighbor).isEmpty()) return false;
        }
        return !(level.getBlockState(target.offset(0, 1, 0)).getBlock() instanceof net.minecraft.world.level.block.FallingBlock);
    }

    private static BlockPos outpostApproach(ServerLevel level, Mob worker, BlockPos target, int floorY) {
        for (int[] side : BUILD_APPROACHES) {
            if (side[0] == 0 && side[1] == 0 && target.getY() < floorY + 3) continue;
            BlockPos stand = new BlockPos(target.getX() + side[0], floorY + 1, target.getZ() + side[1]);
            BlockPos support = stand.offset(0, -1, 0);
            if (!level.hasChunkAt(stand) || !level.getBlockState(stand).isAir()
                    || !level.getBlockState(stand.offset(0, 1, 0)).isAir()
                    || !level.getBlockState(support).isFaceSturdy(level, support, net.minecraft.core.Direction.UP)) continue;
            if (worker.distanceToSqr(stand.getX() + 0.5, stand.getY(), stand.getZ() + 0.5) <= 2.25) return stand;
            var path = worker.getNavigation().createPath(stand, 0);
            if (path != null && path.canReach()) return stand;
        }
        return null;
    }

    private static boolean workOutpostAccess(ServerLevel level, Mob worker, Colony colony, ColonyLocation home, ColonyLocation post) {
        int dx = post.x() - home.x(), dz = post.z() - home.z();
        BlockPos origin = new BlockPos(home.x(), home.y(), home.z());
        OutpostAccessJob job = null;
        boolean pending = false;
        for (NestLayout.Position part : OutpostTunnelLayout.corridor(dx, dz)) {
            BlockPos target = origin.offset(part.x(), part.y(), part.z());
            var state = level.getBlockState(target);
            boolean place = part.y() == 0 && state.isAir();
            boolean replaceFloor = part.y() == 0 && outpostVegetation(state);
            boolean remove = replaceFloor || part.y() > 0 && !state.isAir();
            if (!place && !remove) continue;
            pending = true;
            if (remove && !outpostBreakSafe(level, target)) continue;
            BlockPos approach = outpostApproach(level, worker, target, home.y());
            if (approach != null) { job = new OutpostAccessJob(target, approach, place, replaceFloor); break; }
        }
        if (!pending) for (NestLayout.Position part : OutpostTunnelLayout.chamber(dx, dz)) {
            BlockPos target = origin.offset(part.x(), part.y(), part.z());
            var state = level.getBlockState(target);
            boolean door = (OutpostTunnelLayout.isPostDoor(part.x(), part.y(), part.z(), dx, dz)
                    || OutpostLayout.isLegacyInterior(part.x() - dx, part.y(), part.z() - dz)) && state.is(HivemindContent.HIVE_CHITIN);
            boolean placeFloor = part.y() == 0 && state.isAir();
            if (!placeFloor && !excavationMaterial(state) && !outpostVegetation(state) && !door) continue;
            pending = true;
            if (!placeFloor && !outpostBreakSafe(level, target)) continue;
            BlockPos approach = outpostApproach(level, worker, target, home.y());
            if (approach != null) { job = new OutpostAccessJob(target, approach, placeFloor, part.y() == 0 && !placeFloor); break; }
        }
        if (!pending) for (NestLayout.Position part : OutpostTunnelLayout.obsoleteCover(dx, dz)) {
            BlockPos target = origin.offset(part.x(), part.y(), part.z());
            if (!level.hasChunkAt(target) || !level.getWorldBorder().isWithinBounds(target)
                    || !level.getBlockState(target).is(HivemindContent.HIVE_CHITIN) || !outpostBreakSafe(level, target)) continue;
            BlockPos approach = outpostApproach(level, worker, target, home.y());
            if (approach == null) continue;
            pending = true;
            job = new OutpostAccessJob(target, approach, false, false);
            break;
        }
        if (!pending) return false;
        if (job == null || availableFood(level, colony, home) < 1) return false;
        BlockPos target = job.target();
        BlockPos approach = job.approach();
        if (worker.distanceToSqr(approach.getX() + 0.5, approach.getY(), approach.getZ() + 0.5) > 2.25) {
            worker.getNavigation().moveTo(approach.getX() + 0.5, approach.getY(), approach.getZ() + 0.5, 0.9);
            return true;
        }
        if (!level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class, new net.minecraft.world.phys.AABB(target), entity -> entity.isAlive()).isEmpty()) return true;
        if (job.placeFloor()) {
            if (level.getBlockState(target).isAir() && level.setBlock(target, HivemindContent.HIVE_CHITIN.get().defaultBlockState(), 3)) consumeFood(level, colony, home, 1);
        } else if (outpostBreakSafe(level, target) && level.destroyBlock(target, false, worker)) {
            if (job.replaceFloor() && level.getBlockState(target).isAir()) level.setBlock(target, HivemindContent.HIVE_CHITIN.get().defaultBlockState(), 3);
            consumeFood(level, colony, home, 1);
        }
        return true;
    }

    private static boolean workOnOutpostCover(ServerLevel level, Mob worker, Colony colony, ColonyLocation home, ColonyLocation post) {
        int dx = post.x() - home.x(), dz = post.z() - home.z();
        BlockPos origin = new BlockPos(home.x(), home.y(), home.z());
        for (NestLayout.Position part : OutpostTunnelLayout.cover(dx, dz)) {
            BlockPos target = origin.offset(part.x(), part.y(), part.z());
            if (!level.hasChunkAt(target) || !level.getWorldBorder().isWithinBounds(target)
                    || !level.getBlockState(target).isAir() || !level.getFluidState(target).isEmpty()) continue;
            if (!level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class, new net.minecraft.world.phys.AABB(target), entity -> entity.isAlive() && !entity.isSpectator()).isEmpty()) continue;
            BlockPos stand = outpostApproach(level, worker, target, home.y());
            if (stand == null) continue;
            if (worker.distanceToSqr(stand.getX() + 0.5, stand.getY(), stand.getZ() + 0.5) > 2.25) {
                worker.getNavigation().moveTo(stand.getX() + 0.5, stand.getY(), stand.getZ() + 0.5, 0.9);
                return true;
            }
            if (availableFood(level, colony, home) < 1) return false;
            if (level.setBlock(target, HivemindContent.HIVE_CHITIN.get().defaultBlockState(), 3)) consumeFood(level, colony, home, 1);
            return true;
        }
        return false;
    }

    private static boolean workOnOutposts(ServerLevel level, Mob worker, Colony colony, ColonyLocation home) {
        var sites = colony.outposts();
        for (var site : sites) {
            ColonyLocation post = site.location();
            if (outpostPlanIssue(level, home, post) != null) continue;
            if (workOutpostAccess(level, worker, colony, home, post)) return true;
            if (!outpostAccessOpen(level, home, post)) continue;
            if (workOutpostShell(level, worker, colony, home, site)) return true;
            if (outpostShellComplete(level, home, post) && workOnOutpostCover(level, worker, colony, home, post)) return true;
        }
        if (!settings.expansion() || sites.size() >= 3 || colony.hasOutpostProject()
                || colony.totalPopulation() < (sites.isEmpty() ? 10 : sites.size() == 1 ? 15 : 18)
                || colony.population(Caste.WORKER) < 3 || availableFood(level, colony, home) < 24
                || level.getGameTime() % 2400L >= 100L || activeOutposts(level, colony, home) < sites.size()) return false;
        long window = level.getGameTime() / 2400L;
        var lastSearch = OUTPOST_SEARCHES.get(colony.id());
        if (lastSearch != null && lastSearch.window() == window) return false;
        String firstIssue = null;
        for (var offset : OutpostTunnelLayout.candidateOffsets()) {
            if (colony.hasOutpostBranch(OutpostTunnelLayout.entranceRoom(offset.x(), offset.z()))) continue;
            BlockPos site = new BlockPos(home.x() + offset.x(), home.y(), home.z() + offset.z());
            var location = new ColonyLocation(home.dimension(), site.getX(), site.getY(), site.getZ());
            String issue = outpostSiteIssue(level, home, site);
            if (issue != null) {
                if (firstIssue == null) firstIssue = issue;
                continue;
            }
            boolean nearOther = colonies.colonies().stream().filter(other -> !other.id().equals(colony.id()))
                    .flatMap(other -> java.util.stream.Stream.concat(java.util.stream.Stream.ofNullable(other.home()),
                            other.outposts().stream().map(Colony.Outpost::location)))
                    .filter(pos -> pos.dimension().equals(home.dimension()))
                    .anyMatch(pos -> Math.pow(pos.x() - site.getX(), 2) + Math.pow(pos.z() - site.getZ(), 2) < 24 * 24);
            if (nearOther) {
                if (firstIssue == null) firstIssue = outpostIssue("cerca de otra colmena", site);
                continue;
            }
            OUTPOST_SEARCHES.remove(colony.id());
            colony.beginOutpost(location);
            save(server);
            return true;
        }
        OUTPOST_SEARCHES.put(colony.id(), new OutpostSearch(window,
                firstIssue == null ? "sin sitios disponibles" : "sin sitio viable, primer motivo: " + firstIssue));
        return false;
    }

    private static boolean workOutpostShell(ServerLevel level, Mob worker, Colony colony, ColonyLocation home, Colony.Outpost entry) {
        ColonyLocation outpost = entry.location();
        BlockPos center = new BlockPos(outpost.x(), outpost.y(), outpost.z());
        var occupants = level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,
                new net.minecraft.world.phys.AABB(center).inflate(5.0), entity -> entity.isAlive() && !entity.isSpectator());
        boolean complete = true;
        BlockPos nearest = null, approach = null;
        double nearestDistance = Double.MAX_VALUE;
        for (NestLayout.Position position : OutpostLayout.shell(OutpostTunnelLayout.side(outpost.x() - home.x(), outpost.z() - home.z()))) {
            BlockPos cell = center.offset(position.x(), position.y(), position.z());
            var state = level.getBlockState(cell);
            if (state.is(HivemindContent.HIVE_CHITIN.get())) continue;
            complete = false;
            if (!state.isAir() || occupants.stream().anyMatch(entity -> entity.getBoundingBox().intersects(new net.minecraft.world.phys.AABB(cell)))) continue;
            double distance = worker.distanceToSqr(cell.getX() + 0.5, cell.getY() + 0.5, cell.getZ() + 0.5);
            if (distance >= nearestDistance) continue;
            BlockPos stand = outpostApproach(level, worker, cell, center.getY());
            if (stand == null) continue;
            nearest = cell;
            approach = stand;
            nearestDistance = distance;
        }
        if (complete) {
            if (entry.complete()) return false;
            colony.finishOutpost(outpost);
            save(server);
            return true;
        }
        if (nearest == null || availableFood(level, colony, home) < 1) return false;
        if (worker.distanceToSqr(approach.getX() + 0.5, approach.getY(), approach.getZ() + 0.5) > 2.25) {
            worker.getNavigation().moveTo(approach.getX() + 0.5, approach.getY(), approach.getZ() + 0.5, 0.9);
            return true;
        }
        if (level.setBlock(nearest, HivemindContent.HIVE_CHITIN.get().defaultBlockState(), 3)) consumeFood(level, colony, home, 1);
        return true;
    }

    private static String outpostSiteIssue(ServerLevel level, ColonyLocation home, BlockPos center) {
        ColonyLocation site = new ColonyLocation(home.dimension(), center.getX(), center.getY(), center.getZ());
        String issue = outpostPlanIssue(level, home, site);
        if (issue != null) return issue;
        var area = new net.minecraft.world.phys.AABB(center.getX() - 2, center.getY(), center.getZ() - 2, center.getX() + 3, center.getY() + 5, center.getZ() + 3);
        return level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class, area, entity -> entity.isAlive() && !entity.isSpectator()).isEmpty() ? null : outpostIssue("sitio ocupado", center);
    }

    private static BlockPos roomPosition(ColonyLocation home, NestLayout.Room room) {
        NestLayout.Position position = NestLayout.center(room);
        return new BlockPos(home.x() + position.x(), home.y() + position.y(), home.z() + position.z());
    }

    private static boolean isStorage(net.minecraft.world.level.block.state.BlockState state) {
        return state.is(HivemindContent.HIVE_STORAGE.get()) || state.is(Blocks.BARREL);
    }

    private static void migrateStorage(ServerLevel level, Colony colony, ColonyLocation home) {
        if (!colony.hasSpecializedRooms()) return;
        BlockPos pos = roomPosition(home, NestLayout.Room.STORAGE).above();
        if (!level.hasChunkAt(pos)) return;
        var previous = level.getBlockState(pos);
        if (!previous.is(Blocks.BARREL) || !(level.getBlockEntity(pos) instanceof net.minecraft.world.Container barrel)) return;
        var contents = new java.util.ArrayList<net.minecraft.world.item.ItemStack>(barrel.getContainerSize());
        for (int slot = 0; slot < barrel.getContainerSize(); slot++) contents.add(barrel.getItem(slot).copy());
        barrel.clearContent();
        if (!level.setBlock(pos, HivemindContent.HIVE_STORAGE.get().defaultBlockState(), 3)
                || !(level.getBlockEntity(pos) instanceof com.hivemind.neoforge.content.HiveStorageEntity)) {
            level.setBlock(pos, previous, 3);
        }
        if (level.getBlockEntity(pos) instanceof net.minecraft.world.Container replacement) {
            for (int slot = 0; slot < contents.size(); slot++) replacement.setItem(slot, contents.get(slot));
            replacement.setChanged();
        }
    }

    private static net.minecraft.world.Container storageInventory(ServerLevel level, Colony colony, ColonyLocation home) {
        if (!colony.hasSpecializedRooms()) return null;
        BlockPos storage = roomPosition(home, NestLayout.Room.STORAGE).above();
        if (!level.hasChunkAt(storage)) return null;
        return isStorage(level.getBlockState(storage)) && level.getBlockEntity(storage) instanceof net.minecraft.world.Container inventory ? inventory : null;
    }

    private static void seedStorage(ServerLevel level, Colony colony) {
        ColonyLocation home = colony.home();
        if (home == null) return;
        net.minecraft.world.Container inventory = storageInventory(level, colony, home);
        int pieces = colony.food() / 4;
        if (inventory == null || pieces == 0 || !inventory.getItem(0).isEmpty()) return;
        inventory.setItem(0, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BEEF, pieces));
        inventory.setChanged();
        colony.consumeFood(pieces * 4);
    }

    private static int availableFood(ServerLevel level, Colony colony, ColonyLocation home) {
        long food = colony.food();
        net.minecraft.world.Container inventory = storageInventory(level, colony, home);
        if (inventory != null) for (int i = 0; i < inventory.getContainerSize(); i++) {
            net.minecraft.world.item.ItemStack stack = inventory.getItem(i);
            if (!stack.isEmpty() && isFood(stack)) food += (long) stack.getCount() * 4;
        }
        return (int) Math.min(Integer.MAX_VALUE, food);
    }

    private static boolean consumeFood(ServerLevel level, Colony colony, ColonyLocation home, int amount) {
        if (availableFood(level, colony, home) < amount) return false;
        net.minecraft.world.Container inventory = storageInventory(level, colony, home);
        while (colony.food() < amount && inventory != null) {
            boolean found = false;
            for (int i = 0; i < inventory.getContainerSize(); i++) {
                net.minecraft.world.item.ItemStack stack = inventory.getItem(i);
                if (!stack.isEmpty() && isFood(stack)) {
                    stack.shrink(1);
                    inventory.setChanged();
                    colony.addFood(4);
                    found = true;
                    break;
                }
            }
            if (!found) return false;
        }
        return colony.consumeFood(amount);
    }

    private static boolean storeStack(ServerLevel level, Colony colony, ColonyLocation home, net.minecraft.world.item.ItemStack held) {
        if (held.isEmpty() || (!isFood(held) && !isChitin(held))) return false;
        net.minecraft.world.Container inventory = storageInventory(level, colony, home);
        if (inventory == null) return false;
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            net.minecraft.world.item.ItemStack existing = inventory.getItem(i);
            if (!existing.isEmpty() && net.minecraft.world.item.ItemStack.isSameItemSameComponents(existing, held) && existing.getMaxStackSize() - existing.getCount() >= held.getCount()) {
                existing.grow(held.getCount());
                inventory.setChanged();
                return true;
            }
        }
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (inventory.getItem(i).isEmpty()) {
                inventory.setItem(i, held.copy());
                inventory.setChanged();
                return true;
            }
        }
        return false;
    }

    private static boolean isChitin(net.minecraft.world.item.ItemStack stack) {
        return !stack.isEmpty() && !HiveLoot.hostileDrop(stack) && stack.is(HivemindContent.HIVE_CHITIN.get().asItem());
    }

    private static void replenishChitin(ServerLevel level, Colony colony, ColonyLocation home) {
        var inventory = storageInventory(level, colony, home);
        if (inventory == null || availableFood(level, colony, home) < Math.max(16, colony.totalPopulation() * 2) + 4) return;
        BlockPos barrel = roomPosition(home, NestLayout.Room.STORAGE).above();
        var workers = level.getEntitiesOfClass(com.hivemind.neoforge.content.HivemindWorker.class,
                new net.minecraft.world.phys.AABB(barrel).inflate(4.0), worker -> worker.isAlive()
                        && worker.getTarget() == null && HiveIdentity.colonyId(worker.entityTags()).filter(colony.id()::equals).isPresent());
        if (workers.isEmpty()) return;
        int stock = 0, empty = -1;
        net.minecraft.world.item.ItemStack partial = null;
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            var item = inventory.getItem(slot);
            if (item.isEmpty() && empty < 0) empty = slot;
            if (!isChitin(item)) continue;
            stock += item.getCount();
            if (item.getMaxStackSize() - item.getCount() >= 4) partial = item;
        }
        if (stock >= 32 || (partial == null && empty < 0) || !consumeFood(level, colony, home, 4)) return;
        if (partial == null) inventory.setItem(empty, new net.minecraft.world.item.ItemStack(HivemindContent.HIVE_CHITIN.get().asItem(), 4));
        else partial.grow(4);
        inventory.setChanged();
    }

    private static boolean isFood(net.minecraft.world.item.ItemStack stack) {
        if (HiveLoot.hostileDrop(stack)) return false;
        return stack.is(net.minecraft.world.item.Items.BEEF) || stack.is(net.minecraft.world.item.Items.PORKCHOP)
                || stack.is(net.minecraft.world.item.Items.MUTTON) || stack.is(net.minecraft.world.item.Items.CHICKEN)
                || stack.is(net.minecraft.world.item.Items.RABBIT) || stack.is(net.minecraft.world.item.Items.COD)
                || stack.is(net.minecraft.world.item.Items.SALMON);
    }

    private static void noticePlayers(MinecraftServer minecraftServer, Colony colony) {
        ColonyLocation home = colony.home();
        if (home == null) return;
        for (ServerLevel level : minecraftServer.getAllLevels()) {
            if (!level.dimension().identifier().toString().equals(home.dimension())) continue;
            migrateStorage(level, colony, home);
            updateRaid(level, colony, home);
            reproduce(level, colony, home);
        }
    }

    private static String raidStatus(Colony colony) {
        var raid = RAIDS.get(colony.id());
        if (raid == null) return "  Incursión: ninguna · Combatientes registrados: " + colony.population(Caste.WARRIOR);
        return "  Incursión: " + switch (raid.phase()) {
            case GATHERING -> "reuniendo " + raid.members().size() + " combatientes";
            case ADVANCING -> "en marcha · " + raid.members().size() + " combatientes";
            case WITHDRAWING -> "retirada";
            case COOLDOWN -> "en espera tras la retirada";
        };
    }

    private static void updateRaid(ServerLevel level, Colony colony, ColonyLocation home) {
        RaidPlan raid = RAIDS.get(colony.id());
        boolean night = level.getSkyDarken() >= 4;
        if (raid != null) {
            if (raid.phase() == RaidPlan.Phase.COOLDOWN && ticks - raid.since() >= 1800) {
                RAIDS.remove(colony.id());
                return;
            }
            boolean valid = night && raidTarget(level, colony, home, raid.target()) != null;
            var ready = valid && raid.phase() == RaidPlan.Phase.GATHERING
                    ? raidWarriors(level, colony, home, raid.target(), 6).stream().map(warrior -> warrior.getUUID()).toList()
                    : java.util.List.<UUID>of();
            RAIDS.put(colony.id(), raid.advance(ticks, valid, ready));
            return;
        }
        if (!night || ticks % 600 != 0 || colony.population(Caste.WARRIOR) < 2 || colony.queenId().isEmpty()) return;
        var memories = colony.memorySnapshot();
        Player selected = null;
        int highestThreat = com.hivemind.core.ThreatLevel.HOSTILE.ordinal() - 1;
        for (var candidate : level.players()) {
            var memory = memories.get(candidate.getUUID());
            if (memory == null || level.getGameTime() - memory.lastEncounter() > 72000L) continue;
            int threat = memory.threatLevel().ordinal();
            if (threat <= highestThreat || !raidTargetEligible(candidate, colony, home)) continue;
            selected = candidate;
            highestThreat = threat;
        }
        if (selected == null) return;
        UUID target = selected.getUUID();
        var members = raidWarriors(level, colony, home, target, 20).stream()
                .sorted(java.util.Comparator.comparingDouble(candidate -> candidate.distanceToSqr(home.x() + 0.5, home.y() + 1.0, home.z() + 0.5)))
                .limit(3).map(candidate -> candidate.getUUID()).toList();
        if (members.size() >= 2) RAIDS.put(colony.id(), new RaidPlan(target, members, RaidPlan.Phase.GATHERING, ticks));
    }

    private static java.util.List<com.hivemind.neoforge.content.HivemindWarrior> raidWarriors(ServerLevel level, Colony colony, ColonyLocation home, UUID target, int radius) {
        BlockPos nest = new BlockPos(home.x(), home.y() + 1, home.z());
        return level.getEntitiesOfClass(com.hivemind.neoforge.content.HivemindWarrior.class, new net.minecraft.world.phys.AABB(nest).inflate(radius),
                candidate -> candidate.isAlive() && candidate.getHealth() >= candidate.getMaxHealth() * 0.7f
                        && candidate.distanceToSqr(home.x() + 0.5, home.y() + 1.0, home.z() + 0.5) <= radius * radius
                        && (candidate.getTarget() == null || candidate.getTarget().getUUID().equals(target) || com.hivemind.neoforge.content.HivemindWarrior.isPrey(candidate.getTarget()))
                        && HiveIdentity.colonyId(candidate.entityTags()).filter(colony.id()::equals).isPresent());
    }

    private static boolean raidTargetEligible(Player player, Colony colony, ColonyLocation home) {
        if (!player.isAlive() || player.isCreative() || player.isSpectator()) return false;
        double distance = player.distanceToSqr(home.x() + 0.5, home.y() + 1.0, home.z() + 0.5);
        int radius = colony.territoryRadius();
        return distance > 256 && distance <= (double) radius * radius;
    }

    private static Player raidTarget(ServerLevel level, Colony colony, ColonyLocation home, UUID target) {
        for (var player : level.players()) {
            if (player.getUUID().equals(target) && raidTargetEligible(player, colony, home)) return player;
        }
        return null;
    }

    private static boolean followRaid(ServerLevel level, Mob mob, Colony colony, ColonyLocation home) {
        RaidPlan raid = RAIDS.get(colony.id());
        if (raid == null || raid.phase() == RaidPlan.Phase.COOLDOWN || !raid.members().contains(mob.getUUID())) return false;
        if (mob.getTarget() != null && !mob.getTarget().getUUID().equals(raid.target())
                && !com.hivemind.neoforge.content.HivemindWarrior.isPrey(mob.getTarget())) return false;
        Player target = raidTarget(level, colony, home, raid.target());
        if (target == null || raid.phase() != RaidPlan.Phase.ADVANCING) {
            mob.setTarget(null);
            if (mob.distanceToSqr(home.x() + 0.5, home.y() + 1.0, home.z() + 0.5) > 25)
                mob.getNavigation().moveTo(home.x() + 0.5, home.y() + 1.0, home.z() + 0.5, 1.05);
            else mob.getNavigation().stop();
            return true;
        }
        if (mob.distanceToSqr(target) <= 400 && mob.hasLineOfSight(target)) {
            mob.setTarget(target);
            return true;
        }
        mob.setTarget(null);
        if (colony.hasSpecializedRooms() && mob.getZ() < home.z() + 15.5
                && mob.distanceToSqr(home.x() + 0.5, home.y() + 1.0, home.z() + 0.5) < 225
                && tryRoamingPath(level, mob, home.x(), home.y() + 1, home.z() + 16, 1.05)) return true;
        double dx = target.getX() - mob.getX(), dz = target.getZ() - mob.getZ();
        double length = Math.max(1, Math.hypot(dx, dz));
        for (int step : new int[]{10, 6, 3}) {
            int x = (int) Math.floor(mob.getX() + dx * Math.min(1, step / length));
            int z = (int) Math.floor(mob.getZ() + dz * Math.min(1, step / length));
            if (tryRoamingPath(level, mob, x, (int) Math.floor(mob.getY()), z, 1.05)) break;
        }
        return true;
    }

    public static boolean withinRoyalChamber(net.minecraft.world.entity.Mob queen, net.minecraft.world.entity.LivingEntity target) {
        if (target == null) return true;
        var colony = HiveIdentity.colonyId(queen.entityTags()).flatMap(colonies::get).orElse(null);
        if (colony == null || !colony.hasOrganicNest()) return true;
        var core = colony.coreLocation();
        return core != null && target.distanceToSqr(core.x()+0.5, core.y()+1.0, core.z()+0.5) <= 16.0;
    }

    private static boolean tendBrood(ServerLevel level, Mob queen, BlockPos egg, int floorY) {
        if (queen.getTarget() != null) return false;
        if (queen.distanceToSqr(egg.getX() + 0.5, egg.getY(), egg.getZ() + 0.5) <= 16.0) {
            queen.getNavigation().stop();
            queen.getLookControl().setLookAt(egg.getX() + 0.5, egg.getY() + 0.5, egg.getZ() + 0.5);
            return true;
        }
        var range = queen.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.FOLLOW_RANGE);
        if (range != null && range.getBaseValue() == 6.0) range.setBaseValue(20.0);
        BlockPos approach = outpostApproach(level, queen, egg, floorY);
        if (approach != null) queen.getNavigation().moveTo(approach.getX() + 0.5, approach.getY(), approach.getZ() + 0.5, 0.8);
        return false;
    }

    private static void reproduce(ServerLevel level, Colony colony, ColonyLocation home) {
        BlockPos nest = new BlockPos(home.x(), home.y(), home.z());
        if (!level.hasChunkAt(nest) || colony.queenId().isEmpty()) return;
        BlockPos royal = new BlockPos(colony.coreLocation().x(), home.y(), colony.coreLocation().z());
        var queens = level.getEntitiesOfClass(com.hivemind.neoforge.content.HivemindQueen.class, new net.minecraft.world.phys.AABB(royal).inflate(12.0),
                queen -> queen.isAlive() && queen.getUUID().equals(colony.queenId().orElse(null)));
        if (queens.isEmpty()) return;
        var queen = queens.get(0);
        if (level.getGameTime() % 1200L < 100L) replenishChitin(level, colony, home);
        int upkeep = settings.upkeep(colony.totalPopulation());
        if (upkeep > 0 && colony.beginUpkeep(level.getGameTime() / 24000L))
            consumeFood(level, colony, home, Math.min(upkeep, availableFood(level, colony, home)));
        ColonyLocation pending = colony.broodSite();
        if (pending != null) {
            if (!pending.dimension().equals(home.dimension())) { colony.clearBrood(); save(server); return; }
            BlockPos egg = new BlockPos(pending.x(), pending.y(), pending.z());
            if (!level.hasChunkAt(egg)) return;
            if (!level.getBlockState(egg).is(HivemindContent.HIVE_BROOD.get())) { colony.clearBrood(); save(server); return; }
            if (!tendBrood(level, queen, egg, home.y())) {
                colony.deferBrood(100L);
                return;
            }
            if (level.getGameTime() < colony.broodReadyTime()) return;
            Caste caste = colony.broodCaste();
            if (level.setBlock(egg, Blocks.AIR.defaultBlockState(), 3)) {
                if (spawnMember(level, colony, caste, egg)) {
                    colony.clearBrood();
                    colony.advanceGeneration();
                    save(server);
                } else {
                    level.setBlock(egg, HivemindContent.HIVE_BROOD.get().defaultBlockState(), 3);
                }
            }
            return;
        }
        if (colony.totalPopulation() >= populationLimit(level, colony, home) || availableFood(level, colony, home) < 4 + upkeep || level.getGameTime() == 0L
                || level.getGameTime() % 12000L >= 1200L) return;
        BlockPos birthSpot = null;
        var nursery = OrganicNest.of(colony).nursery;
        if (colony.hasOrganicNest() || colony.hasFacetedRooms()) {
            BlockPos comb = colony.hasOrganicNest() ? nest.offset(nursery.x(), 1, nursery.z()) : nest.offset(-10, 1, 0);
            for (int dx = -1; dx <= 1 && birthSpot == null; dx++) for (int dz = -1; dz <= 1; dz++) {
                BlockPos cell = comb.offset(dx, 0, dz);
                if (level.getBlockState(cell.below()).is(HivemindContent.BROOD_COMB.get()) && level.getBlockState(cell).isAir()
                        && level.getBlockState(cell.above()).isAir()) { birthSpot = cell; break; }
            }
        }
        for (int radius = 2; radius <= 3 && birthSpot == null; radius++) {
            for (int dx = -radius; dx <= radius && birthSpot == null; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    BlockPos candidate = (colony.hasOrganicNest() ? nest.offset(nursery.x(), 0, nursery.z()) : colony.hasSpecializedRooms() ? nest.offset(-10, 0, 0) : nest).offset(dx, 1, dz);
                    if (colony.hasOrganicNest() && (!OrganicNest.of(colony).inside(candidate.getX()-home.x(),1,candidate.getZ()-home.z())
                            || Math.abs(candidate.getX()-royal.getX())+Math.abs(candidate.getZ()-royal.getZ()) > 4)) continue;
                    if (level.getBlockState(candidate).isAir() && level.getBlockState(candidate.above()).isAir()) {
                        birthSpot = candidate;
                        break;
                    }
                }
            }
        }
        if (birthSpot == null || !tendBrood(level, queen, birthSpot, home.y())) return;
        Caste next = colony.population(Caste.SCOUT) < 2 ? Caste.SCOUT : colony.population(Caste.WARRIOR) * 3 < colony.totalPopulation() ? Caste.WARRIOR : Caste.WORKER;
        if (level.setBlock(birthSpot, HivemindContent.HIVE_BROOD.get().defaultBlockState(), 3)) {
            consumeFood(level, colony, home, 4);
            colony.startBrood(new ColonyLocation(home.dimension(), birthSpot.getX(), birthSpot.getY(), birthSpot.getZ()), next, level.getGameTime() + (level.getBlockState(birthSpot.below()).is(HivemindContent.BROOD_COMB.get()) ? 1800L : 2400L));
            save(server);
        }
    }


    private static void finishCollapse(MinecraftServer minecraftServer, Colony colony) {
        RAIDS.remove(colony.id());
        OUTPOST_SEARCHES.remove(colony.id());
        for (ServerLevel level : minecraftServer.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (HiveIdentity.colonyId(entity.entityTags()).filter(colony.id()::equals).isPresent()) entity.discard();
            }
        }
        colony.finishExtermination();
    }

    public static void onEntityLoad(Entity entity, ServerLevel level) {
        if (!registryReady || server != level.getServer()) return;
        Optional<UUID> colonyId = HiveIdentity.colonyId(entity.entityTags());
        if (colonyId.isEmpty()) return;
        Colony colony = colonies.get(colonyId.get()).orElse(null);
        if (colony == null || colony.state() != ColonyState.ACTIVE) {
            entity.discard();
            return;
        }
        HiveIdentity.caste(entity.entityTags()).ifPresent(caste -> {
            if (!colony.membersSnapshot().containsKey(entity.getUUID())) colony.addMember(entity.getUUID(), caste);
        });
    }

    public static void onStructureDamage(ServerLevel level, Player player, BlockPos position, net.minecraft.world.level.block.state.BlockState state) {
        if (!registryReady || server != level.getServer() || player.isCreative() || player.isSpectator()) return;
        boolean hiveBlock = state.is(HivemindContent.HIVE_CHITIN.get())
                || state.is(HivemindContent.BROOD_COMB.get())
                || state.is(HivemindContent.MOLT_BED.get())
                || state.is(HivemindContent.ALARM_RIDGE.get()) || state.is(HivemindContent.HIVE_CORE.get())
                || state.is(HivemindContent.HIVE_BROOD.get());
        for (Colony colony : colonies.colonies()) {
            ColonyLocation home = colony.home();
            if (colony.state() != ColonyState.ACTIVE || home == null || !home.dimension().equals(level.dimension().identifier().toString())) continue;
            boolean nearHome = Math.abs(position.getX() - home.x()) <= (colony.hasSpecializedRooms() ? 14 : 6)
                    && Math.abs(position.getZ() - home.z()) <= (colony.hasSpecializedRooms() ? 14 : 6)
                    && Math.abs(position.getY() - home.y()) <= 6;
            boolean nearPost = colony.outposts().stream().map(Colony.Outpost::location).anyMatch(post ->
                    Math.abs(position.getX() - post.x()) <= OutpostLayout.RADIUS && Math.abs(position.getZ() - post.z()) <= OutpostLayout.RADIUS
                            && position.getY() >= post.y() && position.getY() <= post.y() + OutpostLayout.ROOF);
            if (colony.hasOrganicNest()) nearHome = OrganicNest.of(colony).at(position.getX()-home.x(), position.getY()-home.y(), position.getZ()-home.z()) != null;
            boolean storage = colony.hasSpecializedRooms() && isStorage(state)
                    && position.equals(roomPosition(home, NestLayout.Room.STORAGE).above());
            if ((!nearHome && !nearPost) || (!hiveBlock && !storage)) continue;
            var witnesses = level.getEntitiesOfClass(Mob.class, new net.minecraft.world.phys.AABB(position).inflate(16.0),
                    witness -> witness.isAlive() && HiveIdentity.colonyId(witness.entityTags()).filter(colony.id()::equals).isPresent() && witness.hasLineOfSight(player));
            if (!witnesses.isEmpty()) witnesses.get(0).addTag(HiveIdentity.damageReportTag(player.getUUID(), UUID.randomUUID()));
        }
    }

    private static void notifyWitnesses(net.minecraft.world.entity.LivingEntity victim, Player attacker, Colony colony, net.minecraft.world.damagesource.DamageSource source) {
        CombatTactic tactic = source.is(net.minecraft.tags.DamageTypeTags.IS_FIRE) ? CombatTactic.FIRE
                : source.getDirectEntity() instanceof net.minecraft.world.entity.projectile.Projectile ? CombatTactic.RANGED
                : source.getDirectEntity() == attacker ? CombatTactic.MELEE : null;
        if (!(victim.level() instanceof ServerLevel level)) return;
        boolean delegated = false;
        for (Mob witness : level.getEntitiesOfClass(Mob.class, victim.getBoundingBox().inflate(24.0), mob -> mob != victim && mob.isAlive())) {
            if (HiveIdentity.colonyId(witness.entityTags()).filter(colony.id()::equals).isEmpty() || !witness.hasLineOfSight(victim)) continue;
            if (!delegated) { witness.addTag(HiveIdentity.lossReportTag(attacker.getUUID(), victim.getUUID(), tactic)); delegated = true; }
            if (HiveIdentity.caste(witness.entityTags()).orElse(null) == Caste.WARRIOR) witness.setTarget(attacker);
        }
    }


    public static void onDeath(net.minecraft.world.entity.LivingEntity entity, net.minecraft.world.damagesource.DamageSource damageSource) {
        REPAIR_BACKOFF.remove(entity.getUUID());
        if (!registryReady) return;
        Optional<UUID> colonyId = HiveIdentity.colonyId(entity.entityTags());
        if (colonyId.isEmpty()) return;
        Colony colony = colonies.get(colonyId.get()).orElse(null);
        if (colony == null) return;
        Player player = damageSource.getEntity() instanceof Player attacker ? attacker : null;
        if (player != null && colony.state() == ColonyState.ACTIVE) notifyWitnesses(entity, player, colony, damageSource);
        HiveIdentity.caste(entity.entityTags()).ifPresent(caste -> colony.removeMember(entity.getUUID(), caste));
        if (entity.entityTags().contains(HiveIdentity.casteTag(Caste.QUEEN))) colony.beginCollapse();
        if (server != null) save(server);
    }
}

