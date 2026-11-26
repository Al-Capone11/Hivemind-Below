package com.hivemind.core;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** In-memory registry; Minecraft world persistence is implemented by each version adapter. */
public final class ColonyRegistry {
    private static final int MAGIC = 0x48495645;
    private static final int FORMAT_VERSION = 11;
    private static final int MAX_COLONIES = 10_000;
    private static final int MAX_MEMBERS = 100_000;
    private static final int MAX_MEMORIES = 100_000;
    private final Map<UUID, Colony> colonies = new HashMap<>();

    public Colony create() {
        return create(null);
    }

    public Colony create(ColonyLocation home) {
        Colony colony;
        do {
            colony = new Colony(UUID.randomUUID());
        } while (colonies.containsKey(colony.id()));
        colonies.put(colony.id(), colony);
        if (home != null) colony.setHome(home);
        return colony;
    }

    public Optional<Colony> get(UUID id) {
        return Optional.ofNullable(colonies.get(id));
    }

    public void discardUnfounded(UUID id) {
        Colony colony = colonies.get(id);
        if (colony != null && colony.state() == ColonyState.ACTIVE && colony.totalPopulation() == 0) colonies.remove(id);
    }

    public Collection<Colony> colonies() {
        return Collections.unmodifiableCollection(colonies.values());
    }

    public void removeExterminated() {
        colonies.values().removeIf(colony -> colony.state() == ColonyState.EXTERMINATED);
    }

    public void save(Path file) throws IOException {
        Path parent = file.toAbsolutePath().getParent();
        Files.createDirectories(parent);
        Path temporary = file.resolveSibling(file.getFileName() + ".tmp");
        try (DataOutputStream output = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(temporary)))) {
            output.writeInt(MAGIC);
            output.writeInt(FORMAT_VERSION);
            output.writeInt(colonies.size());
            for (Colony colony : colonies.values()) writeColony(output, colony);
        }
        try {
            Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException ignored) {
            Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public static ColonyRegistry load(Path file) throws IOException {
        ColonyRegistry registry = new ColonyRegistry();
        if (!Files.exists(file)) return registry;
        try (DataInputStream input = new DataInputStream(new BufferedInputStream(Files.newInputStream(file)))) {
            if (input.readInt() != MAGIC) throw new IOException("Invalid Hivemind data file: " + file);
            int version = input.readInt();
            if (version < 1 || version > FORMAT_VERSION) throw new IOException("Unsupported Hivemind data version " + version);
            int count = bounded(input.readInt(), 0, MAX_COLONIES, "colony count");
            for (int i = 0; i < count; i++) {
                Colony colony = readColony(input, version);
                if (registry.colonies.putIfAbsent(colony.id(), colony) != null) {
                    throw new IOException("Duplicate colony id " + colony.id());
                }
            }
            if (input.read() != -1) throw new IOException("Unexpected trailing data in " + file);
        } catch (IllegalArgumentException | ArithmeticException error) {
            throw new IOException("Invalid Hivemind data file: " + file, error);
        }
        return registry;
    }

    private static void writeColony(DataOutputStream output, Colony colony) throws IOException {
        writeUuid(output, colony.id());
        output.writeByte(colony.state().ordinal());
        output.writeBoolean(colony.queenId().isPresent());
        if (colony.queenId().isPresent()) writeUuid(output, colony.queenId().orElseThrow());
        ColonyLocation home = colony.home();
        output.writeBoolean(home != null);
        if (home != null) {
            output.writeUTF(home.dimension());
            output.writeInt(home.x());
            output.writeInt(home.y());
            output.writeInt(home.z());
        }
        output.writeInt(colony.food());
        output.writeInt(colony.generation());
        Map<UUID, Caste> members = colony.membersSnapshot();
        output.writeInt(members.size());
        for (Map.Entry<UUID, Caste> member : members.entrySet()) {
            writeUuid(output, member.getKey());
            output.writeByte(member.getValue().ordinal());
        }
        Map<UUID, PlayerMemory> memories = colony.memorySnapshot();
        output.writeInt(memories.size());
        for (PlayerMemory memory : memories.values()) {
            writeUuid(output, memory.playerId());
            output.writeInt(memory.encounters());
            output.writeInt(memory.colonyLosses());
            output.writeInt(memory.structuralDamage());
            output.writeLong(memory.lastEncounter());
            output.writeInt(memory.tactics().size());
            for (Map.Entry<CombatTactic, Integer> tactic : memory.tactics().entrySet()) {
                output.writeByte(tactic.getKey().ordinal());
                output.writeInt(tactic.getValue());
                output.writeLong(memory.lastTactic(tactic.getKey()));
            }
        }
        Set<String> adaptations = colony.adaptations();
        output.writeInt(adaptations.size());
        for (String adaptation : adaptations) output.writeUTF(adaptation);
        ColonyLocation foodSite = colony.foodSite();
        output.writeBoolean(foodSite != null);
        if (foodSite != null) {
            output.writeUTF(foodSite.dimension());
            output.writeInt(foodSite.x());
            output.writeInt(foodSite.y());
            output.writeInt(foodSite.z());
            output.writeLong(colony.foodSiteTime());
        }
        ColonyLocation brood = colony.broodSite();
        output.writeBoolean(brood != null);
        if (brood != null) {
            output.writeUTF(brood.dimension());
            output.writeInt(brood.x());
            output.writeInt(brood.y());
            output.writeInt(brood.z());
            output.writeByte(colony.broodCaste().ordinal());
            output.writeLong(colony.broodReadyTime());
        }
        output.writeBoolean(colony.hasSpecializedRooms());
        var outposts = colony.outposts();
        output.writeByte(outposts.size());
        for (Colony.Outpost site : outposts) {
            ColonyLocation outpost = site.location();
            output.writeUTF(outpost.dimension());
            output.writeInt(outpost.x());
            output.writeInt(outpost.y());
            output.writeInt(outpost.z());
            output.writeBoolean(site.complete());
        }
        output.writeLong(colony.upkeepDay());
        output.writeBoolean(colony.hasFacetedRooms());
        output.writeBoolean(colony.hasVaultedRooms());
        output.writeBoolean(colony.hasOrganicNest());
    }

    private static Colony readColony(DataInputStream input, int version) throws IOException {
        UUID id = readUuid(input);
        ColonyState state = enumValue(ColonyState.values(), input.readUnsignedByte(), "colony state");
        UUID queenId = input.readBoolean() ? readUuid(input) : null;
        ColonyLocation home = input.readBoolean()
                ? new ColonyLocation(input.readUTF(), input.readInt(), input.readInt(), input.readInt())
                : null;
        int food = bounded(input.readInt(), 0, Integer.MAX_VALUE, "food");
        int generation = bounded(input.readInt(), 0, 6, "generation");
        int memberCount = bounded(input.readInt(), 0, MAX_MEMBERS, "member count");
        Map<UUID, Caste> members = new HashMap<>();
        for (int i = 0; i < memberCount; i++) {
            UUID entity = readUuid(input);
            Caste caste = enumValue(Caste.values(), input.readUnsignedByte(), "caste");
            if (members.putIfAbsent(entity, caste) != null) throw new IOException("Duplicate colony member " + entity);
        }
        int memoryCount = bounded(input.readInt(), 0, MAX_MEMORIES, "player memory count");
        Map<UUID, PlayerMemory> memories = new HashMap<>();
        for (int i = 0; i < memoryCount; i++) {
            UUID player = readUuid(input);
            int encounters = bounded(input.readInt(), 0, Integer.MAX_VALUE, "encounters");
            int losses = bounded(input.readInt(), 0, Integer.MAX_VALUE, "colony losses");
            int damage = bounded(input.readInt(), 0, Integer.MAX_VALUE, "structural damage");
            long lastEncounter = input.readLong();
            int tacticCount = bounded(input.readInt(), 0, CombatTactic.values().length, "tactic count");
            Map<CombatTactic, Integer> tactics = new EnumMap<>(CombatTactic.class);
            Map<CombatTactic, Long> times = new EnumMap<>(CombatTactic.class);
            for (int j = 0; j < tacticCount; j++) {
                CombatTactic tactic = enumValue(CombatTactic.values(), input.readUnsignedByte(), "combat tactic");
                int observed = bounded(input.readInt(), 0, Integer.MAX_VALUE, "observed tactic count");
                tactics.put(tactic, observed);
                if (version >= 8) times.put(tactic, input.readLong());
            }
            PlayerMemory memory = new PlayerMemory(player);
            memory.restore(encounters, losses, damage, lastEncounter, tactics);
            memory.restoreTacticTimes(times);
            if (memories.putIfAbsent(player, memory) != null) throw new IOException("Duplicate player memory " + player);
        }
        int adaptationCount = bounded(input.readInt(), 0, 5, "adaptation count");
        Set<String> adaptations = new java.util.HashSet<>();
        for (int i = 0; i < adaptationCount; i++) adaptations.add(input.readUTF());
        Colony colony = new Colony(id);
        colony.restoreData(state, queenId, home, food, generation, members, memories, adaptations);
        if (version >= 2 && input.readBoolean()) {
            ColonyLocation foodSite = new ColonyLocation(input.readUTF(), input.readInt(), input.readInt(), input.readInt());
            colony.restoreFoodSite(foodSite, input.readLong());
        }
        if (version >= 3 && input.readBoolean()) {
            ColonyLocation brood = new ColonyLocation(input.readUTF(), input.readInt(), input.readInt(), input.readInt());
            Caste caste = enumValue(Caste.values(), input.readUnsignedByte(), "brood caste");
            colony.restoreBrood(brood, caste, input.readLong());
        }
        if (version >= 4) colony.restoreSpecializedRooms(input.readBoolean());
        int outpostCount = version >= 7 ? bounded(input.readUnsignedByte(), 0, 3, "outpost count")
                : version >= 5 && input.readBoolean() ? 1 : 0;
        for (int i = 0; i < outpostCount; i++) {
            ColonyLocation outpost = new ColonyLocation(input.readUTF(), input.readInt(), input.readInt(), input.readInt());
            colony.restoreOutpost(outpost, input.readBoolean());
        }
        if (version >= 6) colony.restoreUpkeepDay(input.readLong());
        if (version >= 9 && input.readBoolean()) colony.enableFacetedRooms();
        if (version >= 10 && input.readBoolean()) colony.enableVaultedRooms();
        if (version >= 11 && input.readBoolean()) colony.enableOrganicNest();
        return colony;
    }

    private static int bounded(int value, int min, int max, String field) throws IOException {
        if (value < min || value > max) throw new IOException("Invalid " + field + ": " + value);
        return value;
    }

    private static <T> T enumValue(T[] values, int ordinal, String field) throws IOException {
        if (ordinal < 0 || ordinal >= values.length) throw new IOException("Invalid " + field + " value: " + ordinal);
        return values[ordinal];
    }

    private static void writeUuid(DataOutputStream output, UUID id) throws IOException {
        output.writeLong(id.getMostSignificantBits());
        output.writeLong(id.getLeastSignificantBits());
    }

    private static UUID readUuid(DataInputStream input) throws IOException {
        return new UUID(input.readLong(), input.readLong());
    }
}
