package com.hivemind.core;

import java.util.Optional;
import java.util.UUID;

public final class HiveIdentity {
    private static final String COLONY_PREFIX = "hivemind_colony=";
    private static final String CASTE_PREFIX = "hivemind_caste=";
    private static final String LOSS_PREFIX = "hivemind_loss_";
    private static final String DAMAGE_PREFIX = "hivemind_damage_";
    private static final String FIRE_PREFIX = "hivemind_fire_";
    private static final String INCURSION_PREFIX = "hivemind_incursion_";

    public record LossReport(UUID attacker, UUID victim, CombatTactic tactic) {}

    private HiveIdentity() {}

    public static String colonyTag(UUID colonyId) {
        return COLONY_PREFIX + colonyId;
    }

    public static String casteTag(Caste caste) {
        return CASTE_PREFIX + caste.name().toLowerCase(java.util.Locale.ROOT);
    }

    public static String lossReportTag(UUID attacker, UUID victim, CombatTactic tactic) {
        java.util.Objects.requireNonNull(attacker, "attacker");
        java.util.Objects.requireNonNull(victim, "victim");
        if (tactic != null && tactic != CombatTactic.MELEE && tactic != CombatTactic.RANGED && tactic != CombatTactic.FIRE) {
            throw new IllegalArgumentException("Unsupported witnessed attack type");
        }
        return LOSS_PREFIX + attacker + "_" + victim + (tactic == null ? "" : "_" + tactic.name());
    }

    public static String damageReportTag(UUID attacker, UUID eventId) {
        return DAMAGE_PREFIX + java.util.Objects.requireNonNull(attacker, "attacker") + "_"
                + java.util.Objects.requireNonNull(eventId, "eventId");
    }

    public static Optional<UUID> damageReportAttacker(String tag) {
        if (tag == null || !tag.startsWith(DAMAGE_PREFIX)) return Optional.empty();
        String[] parts = tag.substring(DAMAGE_PREFIX.length()).split("_", -1);
        if (parts.length != 2) return Optional.empty();
        try {
            UUID.fromString(parts[1]);
            return Optional.of(UUID.fromString(parts[0]));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    public static String fireReportTag(UUID player) {
        return FIRE_PREFIX + java.util.Objects.requireNonNull(player, "player");
    }

    public static Optional<UUID> fireReportPlayer(String tag) {
        if (tag == null || !tag.startsWith(FIRE_PREFIX)) return Optional.empty();
        try {
            return Optional.of(UUID.fromString(tag.substring(FIRE_PREFIX.length())));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    public static String incursionReportTag(UUID player) {
        return INCURSION_PREFIX + java.util.Objects.requireNonNull(player, "player");
    }

    public static Optional<UUID> incursionReportPlayer(String tag) {
        if (tag == null || !tag.startsWith(INCURSION_PREFIX)) return Optional.empty();
        try {
            return Optional.of(UUID.fromString(tag.substring(INCURSION_PREFIX.length())));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    public static Optional<LossReport> lossReport(String tag) {
        if (tag == null || !tag.startsWith(LOSS_PREFIX)) return Optional.empty();
        String[] parts = tag.substring(LOSS_PREFIX.length()).split("_", -1);
        if (parts.length < 1 || parts.length > 3) return Optional.empty();
        try {
            UUID attacker = UUID.fromString(parts[0]);
            UUID victim = parts.length > 1 ? UUID.fromString(parts[1]) : null;
            CombatTactic tactic = parts.length > 2 ? CombatTactic.valueOf(parts[2]) : null;
            if (tactic != null && tactic != CombatTactic.MELEE && tactic != CombatTactic.RANGED && tactic != CombatTactic.FIRE) return Optional.empty();
            return Optional.of(new LossReport(attacker, victim, tactic));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }

    public static Optional<UUID> colonyId(Iterable<String> tags) {
        for (String tag : tags) {
            if (tag.startsWith(COLONY_PREFIX)) {
                try {
                    return Optional.of(UUID.fromString(tag.substring(COLONY_PREFIX.length())));
                } catch (IllegalArgumentException ignored) {
                    return Optional.empty();
                }
            }
        }
        return Optional.empty();
    }

    public static Optional<Caste> caste(Iterable<String> tags) {
        for (String tag : tags) {
            if (tag.startsWith(CASTE_PREFIX)) {
                try {
                    return Optional.of(Caste.fromId(tag.substring(CASTE_PREFIX.length())));
                } catch (IllegalArgumentException ignored) {
                    return Optional.empty();
                }
            }
        }
        return Optional.empty();
    }
}
