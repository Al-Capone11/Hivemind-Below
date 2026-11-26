package com.hivemind.core;

import java.util.List;
import java.util.UUID;

public record RaidPlan(UUID target, List<UUID> members, Phase phase, long since) {
    public enum Phase { GATHERING, ADVANCING, WITHDRAWING, COOLDOWN }

    public RaidPlan {
        members = List.copyOf(members);
    }

    public RaidPlan advance(long now, boolean validTarget, List<UUID> ready) {
        long elapsed = now - since;
        return switch (phase) {
            case GATHERING -> {
                var gathered = members.stream().filter(ready::contains).toList();
                if (!validTarget || elapsed >= 600 && gathered.size() < 2) yield withdraw(now);
                if (elapsed >= 200 && gathered.size() >= 2) yield new RaidPlan(target, gathered, Phase.ADVANCING, now);
                yield this;
            }
            case ADVANCING -> !validTarget || elapsed >= 1600 ? withdraw(now) : this;
            case WITHDRAWING -> elapsed >= 600 ? new RaidPlan(target, members, Phase.COOLDOWN, now) : this;
            case COOLDOWN -> this;
        };
    }

    private RaidPlan withdraw(long now) { return new RaidPlan(target, members, Phase.WITHDRAWING, now); }
}
