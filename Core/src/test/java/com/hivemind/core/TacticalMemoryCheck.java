package com.hivemind.core;

import java.nio.file.Files;
import java.util.UUID;

public final class TacticalMemoryCheck {
    private static void require(boolean value, String reason) {
        if (!value) throw new AssertionError(reason);
    }

    public static void main(String[] args) throws Exception {
        UUID attacker = UUID.randomUUID();
        PlayerMemory observer = new PlayerMemory(UUID.randomUUID());
        for (int i = 0; i < 40; i++) observer.recordEncounter(i);
        require(observer.threatLevel() == ThreatLevel.DETECTED, "passive sightings must not make an enemy");
        UUID victim = UUID.randomUUID();
        var ranged = HiveIdentity.lossReport(HiveIdentity.lossReportTag(attacker, victim, CombatTactic.RANGED)).orElseThrow();
        require(ranged.attacker().equals(attacker) && ranged.victim().equals(victim) && ranged.tactic() == CombatTactic.RANGED, "report must retain the attacker's identity and tactic");
        var old = HiveIdentity.lossReport("hivemind_loss_" + attacker + "_" + victim).orElseThrow();
        require(old.tactic() == null, "old witness reports remain readable");
        require(HiveIdentity.lossReport("hivemind_loss_not-a-uuid").isEmpty(), "malformed reports must not reach the colony");

        ColonyRegistry registry = new ColonyRegistry();
        Colony first = registry.create(new ColonyLocation("minecraft:overworld", 0, 20, 0));
        Colony second = registry.create(new ColonyLocation("minecraft:overworld", 200, 20, 0));
        first.addMember(UUID.randomUUID(), Caste.QUEEN);
        first.recordWitnessedLoss(attacker, ranged.tactic(), 10);
        require(!first.adaptations().contains("anti_ranged"), "one observation must not unlock an adaptation");
        require(second.memorySnapshot().isEmpty(), "other colonies must not receive the report");
        first.recordWitnessedLoss(attacker, CombatTactic.RANGED, 20);
        first.recordWitnessedLoss(attacker, CombatTactic.MELEE, 30);
        first.recordWitnessedLoss(attacker, CombatTactic.MELEE, 40);
        var memory = first.memorySnapshot().get(attacker);
        require(memory.encounters() == 4 && memory.colonyLosses() == 4, "a report must count the loss once");
        require(memory.tacticCount(CombatTactic.RANGED) == 2 && memory.tacticCount(CombatTactic.MELEE) == 2, "attack methods remain distinct");
        require(first.adaptations().contains("anti_ranged") && first.adaptations().contains("anti_melee"), "both adaptations should unlock after repeated reports");
        UUID builder = UUID.randomUUID();
        String damage = HiveIdentity.damageReportTag(builder, UUID.randomUUID());
        require(HiveIdentity.damageReportAttacker(damage).orElseThrow().equals(builder), "damage report lost attacker identity");
        require(HiveIdentity.damageReportAttacker("hivemind_damage_invalid").isEmpty(), "invalid damage report was accepted");
        first.recordWitnessedDamage(builder, 1, 50);
        first.recordWitnessedDamage(builder, 1, 60);
        first.recordWitnessedDamage(builder, 1, 70);
        var damageMemory = first.memorySnapshot().get(builder);
        require(damageMemory.structuralDamage() == 3 && damageMemory.encounters() == 3, "damage must be recorded once per report");
        require(damageMemory.tacticCount(CombatTactic.STRUCTURE) == 3, "structural tactic was not tracked");
        UUID firePlayer = UUID.randomUUID();
        require(HiveIdentity.fireReportPlayer(HiveIdentity.fireReportTag(firePlayer)).orElseThrow().equals(firePlayer), "fire report lost player identity");
        first.recordWitnessedFire(firePlayer, 1000);
        first.recordWitnessedFire(firePlayer, 1100);
        require(first.memorySnapshot().get(firePlayer).tacticCount(CombatTactic.FIRE) == 1, "one fire must not be counted repeatedly");
        first.recordWitnessedFire(firePlayer, 2300);
        require(first.adaptations().contains("avoid_fire"), "repeated reports should trigger fire avoidance");
        UUID intruder = UUID.randomUUID();
        require(HiveIdentity.incursionReportPlayer(HiveIdentity.incursionReportTag(intruder)).orElseThrow().equals(intruder), "incursion report lost player identity");
        first.recordWitnessedIncursion(intruder, 3000);
        first.recordWitnessedIncursion(intruder, 3100);
        require(first.memorySnapshot().get(intruder).tacticCount(CombatTactic.INCURSION) == 1, "one incursion was counted repeatedly");
        first.memoryFor(intruder).recordEncounter(4250);
        first.recordWitnessedIncursion(intruder, 4300);
        require(first.adaptations().contains("guard_core"), "repeated incursions should reinforce the core");
        require(first.memorySnapshot().get(intruder).tacticCount(CombatTactic.INCURSION) == 2,
                "a recent passive sighting must not suppress an incursion report");
        var file = Files.createTempFile("hivemind-tactical-memory-", ".dat");
        try {
            registry.save(file);
            Colony restored = ColonyRegistry.load(file).get(first.id()).orElseThrow();
            var remembered = restored.memorySnapshot().get(attacker);
            require(remembered.tacticCount(CombatTactic.RANGED) == 2 && remembered.tacticCount(CombatTactic.MELEE) == 2, "observations must survive save and load");
            require(restored.adaptations().containsAll(first.adaptations()), "adaptations must survive save and load");
            require(restored.memorySnapshot().get(builder).structuralDamage() == 3, "structural damage must survive save and load");
            require(restored.memorySnapshot().get(intruder).lastTactic(CombatTactic.INCURSION) == 4300L,
                    "incursion cooldown must persist separately");
            restored.beginCollapse();
            require(restored.memorySnapshot().isEmpty(), "a collapsing colony must forget its players");
        } finally {
            Files.deleteIfExists(file);
        }
        System.out.println("Tactical memory, reports, colony isolation and persistence passed.");

        UUID one = UUID.randomUUID(), two = UUID.randomUUID(), three = UUID.randomUUID();
        var squad = java.util.List.of(one, two, three);
        var gathering = new RaidPlan(attacker, squad, RaidPlan.Phase.GATHERING, 0);
        require(gathering.advance(200, true, java.util.List.of(one)).phase() == RaidPlan.Phase.GATHERING,
                "one arrival must never launch a raid");
        require(gathering.advance(600, true, java.util.List.of(one)).phase() == RaidPlan.Phase.WITHDRAWING,
                "an incomplete squad must withdraw after the deadline");
        var advancing = gathering.advance(200, true, java.util.List.of(one, two));
        require(advancing.phase() == RaidPlan.Phase.ADVANCING && advancing.members().equals(java.util.List.of(one, two)),
                "only gathered combatants should advance");
        var retreat = advancing.advance(300, false, java.util.List.of());
        require(retreat.phase() == RaidPlan.Phase.WITHDRAWING
                && retreat.advance(900, false, java.util.List.of()).phase() == RaidPlan.Phase.COOLDOWN,
                "losing the target must trigger a withdrawal before cooldown");
    }
}
