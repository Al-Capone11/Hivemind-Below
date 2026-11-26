package com.hivemind.core;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** In-memory retry delay for a worker that cannot reach any repair position. */
public final class RepairBackoff {
    private static final long DELAY = 400L;
    private final Map<UUID, Long> until = new HashMap<>();

    public void defer(UUID worker, long tick) {
        until.put(worker, tick > Long.MAX_VALUE - DELAY ? Long.MAX_VALUE : tick + DELAY);
    }

    public boolean ready(UUID worker, long tick) {
        Long deadline = until.get(worker);
        if (deadline == null) return true;
        if (tick < deadline) return false;
        until.remove(worker);
        return true;
    }

    public void remove(UUID worker) { until.remove(worker); }

    public void clear() { until.clear(); }
}
