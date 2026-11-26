package com.hivemind.core;

import java.util.List;
import java.util.UUID;

public final class WorkerPolicyCheck {
    public static void main(String[] args) {
        UUID near = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID tied = UUID.fromString("00000000-0000-0000-0000-000000000002");
        UUID far = UUID.fromString("00000000-0000-0000-0000-000000000003");
        UUID trapped = UUID.fromString("00000000-0000-0000-0000-000000000004");
        var candidates = List.of(new WorkerPolicy.Carrier(far, true), new WorkerPolicy.Carrier(tied, true),
                new WorkerPolicy.Carrier(trapped, false), new WorkerPolicy.Carrier(near, true));
        if (!WorkerPolicy.repairCarriers(candidates).equals(List.of(near, tied))) throw new AssertionError("unstable or overfull repair assignment");
        if (!WorkerPolicy.repairCarriers(List.of()).isEmpty()) throw new AssertionError("empty roster");
        var roster = List.of(new WorkerPolicy.Candidate(far, true), new WorkerPolicy.Candidate(tied, true),
                new WorkerPolicy.Candidate(trapped, false), new WorkerPolicy.Candidate(near, true),
                new WorkerPolicy.Candidate(new UUID(0, 5), true), new WorkerPolicy.Candidate(new UUID(0, 6), true));
        var duties = WorkerPolicy.duties(roster, true);
        if (!duties.outpostBuilders().equals(List.of(near, tied))
                || !duties.growthExcavators().equals(List.of(far, new UUID(0, 5)))) throw new AssertionError("tasks overlap or unavailable worker assigned");
        var backoff = new RepairBackoff();
        if (!backoff.ready(near, 0)) throw new AssertionError("new worker should be ready");
        backoff.defer(near, 100);
        if (backoff.ready(near, 499) || !backoff.ready(near, 500)) throw new AssertionError("incorrect repair retry delay");
        backoff.defer(near, 600); backoff.clear();
        if (!backoff.ready(near, 600)) throw new AssertionError("backoff did not reset");
        if (NestLayout.roomAt(-10, 0) != NestLayout.Room.INCUBATION || NestLayout.roomAt(0, 0) != NestLayout.Room.CENTRAL
                || NestLayout.roomAt(-14, 0) != null) throw new AssertionError("incorrect room footprint");
        for (int i = 1; i <= 20; i++) {
            var post = NestLayout.guardOffset(new UUID(0, i));
            if (Math.abs(post.x()) > 2 || Math.abs(post.z()) > 2) throw new AssertionError("guard post outside room");
        }
        System.out.println("Repair roster limits workers and excludes unavailable carriers.");
    }
}
