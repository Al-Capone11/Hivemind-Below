package com.hivemind.core;

import java.util.Objects;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

public final class PlayerMemory {
    private final UUID playerId;
    private int encounters;
    private int colonyLosses;
    private int structuralDamage;
    private long lastEncounter;
    private final EnumMap<CombatTactic, Integer> tactics = new EnumMap<>(CombatTactic.class);
    private final EnumMap<CombatTactic, Long> tacticTimes = new EnumMap<>(CombatTactic.class);

    public PlayerMemory(UUID playerId) {
        this.playerId = Objects.requireNonNull(playerId, "playerId");
    }

    public UUID playerId() {
        return playerId;
    }

    public int encounters() {
        return encounters;
    }

    public int colonyLosses() {
        return colonyLosses;
    }

    public int structuralDamage() {
        return structuralDamage;
    }

    public long lastEncounter() {
        return lastEncounter;
    }

    public Map<CombatTactic, Integer> tactics() {
        return java.util.Collections.unmodifiableMap(new EnumMap<>(tactics));
    }

    public void recordEncounter(long gameTime) {
        encounters++;
        lastEncounter = Math.max(lastEncounter, gameTime);
    }

    public void recordColonyLoss(long gameTime) {
        recordEncounter(gameTime);
        colonyLosses++;
    }

    public void recordWitnessedLoss(CombatTactic tactic, long gameTime) {
        recordColonyLoss(gameTime);
        if (tactic != null) countTactic(tactic, gameTime);
    }

    public void recordStructuralDamage(int amount, long gameTime) {
        if (amount <= 0) {
            return;
        }
        recordEncounter(gameTime);
        structuralDamage = Math.addExact(structuralDamage, amount);
        countTactic(CombatTactic.STRUCTURE, gameTime);
    }

    public void recordTactic(CombatTactic tactic, long gameTime) {
        recordEncounter(gameTime);
        countTactic(tactic, gameTime);
    }

    private void countTactic(CombatTactic tactic, long gameTime) {
        tactics.merge(Objects.requireNonNull(tactic, "tactic"), 1, Math::addExact);
        tacticTimes.merge(tactic, Math.max(0L, gameTime), Math::max);
    }

    public long lastTactic(CombatTactic tactic) {
        return tacticTimes.getOrDefault(tactic, lastEncounter);
    }

    void restoreTacticTimes(Map<CombatTactic, Long> times) {
        times.forEach((tactic, time) -> {
            if (time < 0 || time > lastEncounter || tacticCount(tactic) == 0)
                throw new IllegalArgumentException("Invalid tactic time");
            tacticTimes.put(tactic, time);
        });
    }

    public int tacticCount(CombatTactic tactic) {
        return tactics.getOrDefault(tactic, 0);
    }

    public void restore(int encounters, int colonyLosses, int structuralDamage, long lastEncounter,
                        Map<CombatTactic, Integer> restoredTactics) {
        if (encounters < 0 || colonyLosses < 0 || structuralDamage < 0 || lastEncounter < 0) {
            throw new IllegalArgumentException("Player memory values cannot be negative");
        }
        this.encounters = encounters;
        this.colonyLosses = colonyLosses;
        this.structuralDamage = structuralDamage;
        this.lastEncounter = lastEncounter;
        this.tactics.clear();
        this.tacticTimes.clear();
        restoredTactics.forEach((tactic, count) -> {
            if (count < 0) throw new IllegalArgumentException("Tactic count cannot be negative");
            if (count > 0) {
                this.tactics.put(tactic, count);
                tacticTimes.put(tactic, lastEncounter);
            }
        });
    }

    public ThreatLevel threatLevel() {
        int incursions = tacticCount(CombatTactic.INCURSION);
        int fires = tacticCount(CombatTactic.FIRE);
        if (colonyLosses >= 8 || structuralDamage >= 100 || incursions >= 10 || fires >= 8) {
            return ThreatLevel.CRITICAL;
        }
        if (colonyLosses >= 4 || structuralDamage >= 40 || incursions >= 4 || fires >= 4) {
            return ThreatLevel.HOSTILE;
        }
        if (colonyLosses > 0 || structuralDamage > 0 || incursions > 0 || fires >= 2) {
            return ThreatLevel.THREAT;
        }
        return encounters > 0 ? ThreatLevel.DETECTED : ThreatLevel.UNKNOWN;
    }
}
