package com.hivemind.core;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/** Selection rules for workers already carrying a repair material. */
public final class WorkerPolicy {
    public record Carrier(UUID id, boolean available) {}
    public record Candidate(UUID id, boolean available) {}
    public record Duties(List<UUID> outpostBuilders, List<UUID> growthExcavators) {}

    private WorkerPolicy() {}

    public static List<UUID> repairCarriers(Collection<Carrier> workers) {
        return workers.stream().filter(Carrier::available)
                .sorted(Comparator.comparing(Carrier::id))
                .limit(2).map(Carrier::id).toList();
    }

    public static Duties duties(Collection<Candidate> workers, boolean reserveOutpost) {
        List<UUID> available = workers.stream().filter(Candidate::available).map(Candidate::id)
                .sorted().toList();
        List<UUID> builders = available.stream().limit(2).toList();
        List<UUID> excavators = available.stream().skip(reserveOutpost ? 2 : 0).limit(2).toList();
        return new Duties(builders, excavators);
    }
}
