package com.hivemind.core;

public enum Caste {
    QUEEN,
    SCOUT,
    WARRIOR,
    WORKER;

    public static Caste fromId(String id) {
        return valueOf(id.toUpperCase(java.util.Locale.ROOT));
    }
}
