package com.hivemind.core;

import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.EnumSet;
import java.util.Set;

/** Mutable colony model. Callers are expected to access it from the game server thread. */
public final class Colony {
    private final UUID id;
    private final EnumMap<Caste, Integer> population = new EnumMap<>(Caste.class);
    private final Map<UUID, PlayerMemory> playerMemory = new HashMap<>();
    private final Map<UUID, Caste> members = new HashMap<>();
    private final Set<String> adaptations = new java.util.HashSet<>();
    private UUID queenId;
    private ColonyState state = ColonyState.ACTIVE;
    private ColonyLocation home;
    private ColonyLocation foodSite;
    private long foodSiteTime;
    private ColonyLocation broodSite;
    private Caste broodCaste;
    private long broodReadyTime;
    private int food;
    private int generation;
    private boolean specializedRooms;
    private boolean facetedRooms;
    private boolean vaultedRooms;
    private boolean organicNest;
    public boolean hasOrganicNest() { return organicNest; }
    public void enableOrganicNest() { organicNest = true; }
    public ColonyLocation coreLocation() {
        if (!organicNest || home == null) return home;
        var throne = OrganicNest.of(this).throne;
        return new ColonyLocation(home.dimension(), home.x()+throne.x(), home.y(), home.z()+throne.z());
    }

    public boolean hasVaultedRooms() { return vaultedRooms; }
    public void enableVaultedRooms() { enableFacetedRooms(); vaultedRooms = true; }

    public boolean hasFacetedRooms() { return facetedRooms; }
    public void enableFacetedRooms() { facetedRooms = true; }
    public record Outpost(ColonyLocation location, boolean complete) {}
    private final Map<ColonyLocation, Outpost> outposts = new java.util.LinkedHashMap<>();
    private long upkeepDay = -1;

    public Colony(UUID id) {
        this.id = Objects.requireNonNull(id, "id");
        for (Caste caste : Caste.values()) {
            population.put(caste, 0);
        }
    }

    public UUID id() {
        return id;
    }

    public ColonyLocation home() {
        return home;
    }

    public int food() {
        return food;
    }

    public long upkeepDay() { return upkeepDay; }

    public boolean beginUpkeep(long day) {
        requireActive();
        if (day < 0 || day <= upkeepDay) return false;
        boolean due = upkeepDay >= 0;
        upkeepDay = day;
        return due;
    }

    void restoreUpkeepDay(long day) {
        if (day < -1) throw new IllegalArgumentException("Invalid upkeep day");
        upkeepDay = day;
    }

    public boolean hasSpecializedRooms() { return specializedRooms; }

    public void enableSpecializedRooms() {
        requireActive();
        specializedRooms = true;
    }

    void restoreSpecializedRooms(boolean value) { specializedRooms = value; }

    public java.util.List<Outpost> outposts() { return java.util.List.copyOf(outposts.values()); }

    public boolean hasOutpost(ColonyLocation location) { return outposts.containsKey(location); }

    public boolean hasOutpostBranch(NestLayout.Room room) {
        if (home == null) return false;
        for (var location : outposts.keySet()) {
            int dx = location.x() - home.x(), dz = location.z() - home.z();
            if (OutpostTunnelLayout.supported(dx, dz) && OutpostTunnelLayout.entranceRoom(dx, dz) == room) return true;
        }
        return false;
    }

    public boolean hasOutposts() { return !outposts.isEmpty(); }

    public boolean hasOutpostProject() { return outposts.values().stream().anyMatch(post -> !post.complete()); }

    public void beginOutpost(ColonyLocation location) {
        requireActive();
        if (hasOutpostProject() || outposts.size() >= 3 || hasOutpost(location) || home == null
                || !home.dimension().equals(location.dimension()) || home.y() != location.y()
                || !OutpostTunnelLayout.supported(location.x() - home.x(), location.z() - home.z())
                || hasOutpostBranch(OutpostTunnelLayout.entranceRoom(location.x() - home.x(), location.z() - home.z()))) {
            throw new IllegalStateException("Cannot start outpost");
        }
        outposts.put(location, new Outpost(location, false));
    }

    public void finishOutpost(ColonyLocation location) {
        requireActive();
        if (!hasOutpost(location)) throw new IllegalStateException("No planned outpost");
        outposts.put(location, new Outpost(location, true));
    }

    void restoreOutpost(ColonyLocation location, boolean complete) {
        if (location == null || outposts.size() >= 3 || outposts.putIfAbsent(location, new Outpost(location, complete)) != null)
            throw new IllegalArgumentException("Invalid outpost location");
    }

    public ColonyLocation foodSite() { return foodSite; }

    public long foodSiteTime() { return foodSiteTime; }

    public void recordFoodSite(ColonyLocation site, long time) {
        requireActive();
        foodSite = Objects.requireNonNull(site, "site");
        foodSiteTime = Math.max(0L, time);
    }

    public void clearFoodSite() { foodSite = null; foodSiteTime = 0L; }

    void restoreFoodSite(ColonyLocation site, long time) {
        if (time < 0) throw new IllegalArgumentException("Invalid food site time");
        foodSite = site;
        foodSiteTime = time;
    }

    public ColonyLocation broodSite() { return broodSite; }

    public Caste broodCaste() { return broodCaste; }

    public void deferBrood(long ticks) { if (broodSite != null) broodReadyTime += ticks; }
    public long broodReadyTime() { return broodReadyTime; }

    public void startBrood(ColonyLocation site, Caste caste, long readyTime) {
        requireActive();
        if (broodSite != null || caste == Caste.QUEEN || readyTime < 0) throw new IllegalStateException("Invalid brood");
        broodSite = Objects.requireNonNull(site, "site");
        broodCaste = Objects.requireNonNull(caste, "caste");
        broodReadyTime = readyTime;
    }

    public void clearBrood() { broodSite = null; broodCaste = null; broodReadyTime = 0L; }

    void restoreBrood(ColonyLocation site, Caste caste, long readyTime) {
        if (site == null || caste == null || caste == Caste.QUEEN || readyTime < 0) throw new IllegalArgumentException("Invalid brood data");
        broodSite = site;
        broodCaste = caste;
        broodReadyTime = readyTime;
    }

    public int generation() {
        return generation;
    }

    public int territoryRadius() {
        return Math.min(128, 32 + generation * 16);
    }

    public String developmentStage() {
        int total = totalPopulation();
        if (total >= 19 && outposts.values().stream().filter(Outpost::complete).count() >= 2) return "DOMINANT";
        if (total >= 16) return "DEVELOPED";
        if (total >= 7) return "ESTABLISHED";
        return "NASCENT";
    }

    public Set<String> adaptations() {
        return Collections.unmodifiableSet(new java.util.HashSet<>(adaptations));
    }

    public Map<UUID, Caste> membersSnapshot() {
        return Collections.unmodifiableMap(new HashMap<>(members));
    }

    public void setHome(ColonyLocation home) {
        requireActive();
        this.home = Objects.requireNonNull(home, "home");
    }

    public void addFood(int amount) {
        requireActive();
        if (amount < 0) throw new IllegalArgumentException("Food amount cannot be negative");
        food = Math.addExact(food, amount);
    }

    public boolean consumeFood(int amount) {
        requireActive();
        if (amount < 0) throw new IllegalArgumentException("Food amount cannot be negative");
        if (food < amount) return false;
        food -= amount;
        return true;
    }

    public void learnAdaptation(String adaptation) {
        requireActive();
        if (adaptations.size() < 5) adaptations.add(Objects.requireNonNull(adaptation, "adaptation"));
    }

    public void advanceGeneration() {
        requireActive();
        generation = Math.min(6, generation + 1);
    }

    public ColonyState state() {
        return state;
    }

    public Optional<UUID> queenId() {
        return Optional.ofNullable(queenId);
    }

    public int population(Caste caste) {
        return population.get(Objects.requireNonNull(caste, "caste"));
    }

    public int totalPopulation() {
        return population.values().stream().mapToInt(Integer::intValue).sum();
    }

    public int populationLimit(boolean completedAnnex, int completedOutposts) {
        if (!specializedRooms) return 7;
        return 12 + (completedAnnex ? 3 : 0) + Math.max(0, Math.min(3, completedOutposts)) * 3;
    }

    public Map<Caste, Integer> populationSnapshot() {
        return Collections.unmodifiableMap(new EnumMap<>(population));
    }

    public Map<UUID, PlayerMemory> memorySnapshot() {
        return Collections.unmodifiableMap(new HashMap<>(playerMemory));
    }

    public void addMember(UUID entityId, Caste caste) {
        requireActive();
        Objects.requireNonNull(entityId, "entityId");
        Objects.requireNonNull(caste, "caste");
        if (caste == Caste.QUEEN && queenId != null && !queenId.equals(entityId)) {
            throw new IllegalStateException("A colony can have only one queen");
        }
        Caste previous = members.putIfAbsent(entityId, caste);
        if (previous != null) {
            if (previous != caste) throw new IllegalStateException("Entity already belongs to this colony as " + previous);
            return;
        }
        population.merge(caste, 1, Math::addExact);
        if (caste == Caste.QUEEN) {
            queenId = entityId;
        }
    }

    public void removeMember(UUID entityId, Caste caste) {
        Objects.requireNonNull(entityId, "entityId");
        Objects.requireNonNull(caste, "caste");
        Caste registeredCaste = members.remove(entityId);
        if (registeredCaste == null || registeredCaste != caste) {
            if (registeredCaste != null) members.put(entityId, registeredCaste);
            return;
        }
        int count = population.get(caste);
        if (count == 0) {
            return;
        }
        population.put(caste, count - 1);
        if (caste == Caste.QUEEN && entityId.equals(queenId)) {
            queenId = null;
            beginCollapse();
        }
    }

    public PlayerMemory memoryFor(UUID playerId) {
        requireActive();
        return playerMemory.computeIfAbsent(Objects.requireNonNull(playerId, "playerId"), PlayerMemory::new);
    }

    public void recordWitnessedLoss(UUID attacker, CombatTactic tactic, long gameTime) {
        PlayerMemory memory = memoryFor(attacker);
        memory.recordWitnessedLoss(tactic, gameTime);
        learnAdaptation("focus_defense");
        if (tactic == CombatTactic.RANGED && memory.tacticCount(tactic) >= 2) learnAdaptation("anti_ranged");
        if (tactic == CombatTactic.MELEE && memory.tacticCount(tactic) >= 2) learnAdaptation("anti_melee");
        if (tactic == CombatTactic.FIRE && memory.tacticCount(tactic) >= 2) learnAdaptation("avoid_fire");
    }

    public void recordWitnessedDamage(UUID attacker, int amount, long gameTime) {
        PlayerMemory memory = memoryFor(attacker);
        memory.recordStructuralDamage(amount, gameTime);
        if (memory.structuralDamage() >= 3) learnAdaptation("focus_defense");
    }

    public void recordWitnessedFire(UUID player, long gameTime) {
        PlayerMemory memory = memoryFor(player);
        if (memory.tacticCount(CombatTactic.FIRE) > 0 && gameTime - memory.lastTactic(CombatTactic.FIRE) < 1200L) return;
        memory.recordTactic(CombatTactic.FIRE, gameTime);
        if (memory.tacticCount(CombatTactic.FIRE) >= 2) learnAdaptation("avoid_fire");
    }

    public void recordWitnessedIncursion(UUID player, long gameTime) {
        PlayerMemory memory = memoryFor(player);
        if (memory.tacticCount(CombatTactic.INCURSION) > 0 && gameTime - memory.lastTactic(CombatTactic.INCURSION) < 1200L) return;
        memory.recordTactic(CombatTactic.INCURSION, gameTime);
        learnAdaptation("focus_defense");
        if (memory.tacticCount(CombatTactic.INCURSION) >= 2) learnAdaptation("guard_core");
    }

    public void restoreData(ColonyState state, UUID queenId, ColonyLocation home, int food, int generation,
                           Map<UUID, Caste> members, Map<UUID, PlayerMemory> memory, Set<String> adaptations) {
        this.state = Objects.requireNonNull(state, "state");
        this.queenId = queenId;
        this.home = home;
        if (food < 0 || generation < 0) throw new IllegalArgumentException("Colony values cannot be negative");
        this.food = food;
        this.generation = Math.min(6, generation);
        this.members.clear();
        this.population.replaceAll((caste, ignored) -> 0);
        members.forEach((entityId, caste) -> {
            this.members.put(entityId, caste);
            this.population.merge(caste, 1, Math::addExact);
        });
        this.playerMemory.clear();
        this.playerMemory.putAll(memory);
        this.adaptations.clear();
        this.adaptations.addAll(adaptations.stream().limit(5).toList());
    }

    public boolean beginCollapse() {
        if (state != ColonyState.ACTIVE) {
            return false;
        }
        state = ColonyState.COLLAPSING;
        playerMemory.clear();
        clearFoodSite();
        clearBrood();
        return true;
    }

    public boolean finishExtermination() {
        if (state != ColonyState.COLLAPSING) {
            return false;
        }
        state = ColonyState.EXTERMINATED;
        members.clear();
        for (Caste caste : Caste.values()) {
            population.put(caste, 0);
        }
        queenId = null;
        playerMemory.clear();
        return true;
    }

    private void requireActive() {
        if (state != ColonyState.ACTIVE) {
            throw new IllegalStateException("Colony " + id + " is " + state);
        }
    }
}
