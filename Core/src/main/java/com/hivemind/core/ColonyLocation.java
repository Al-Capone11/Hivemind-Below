package com.hivemind.core;

import java.util.Objects;

public record ColonyLocation(String dimension, int x, int y, int z) {
    public ColonyLocation {
        Objects.requireNonNull(dimension, "dimension");
    }
}
