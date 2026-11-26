package com.hivemind.core;

import java.util.Set;
import java.util.stream.Collectors;

public final class HiveInterior {
    private static final Set<NestLayout.Position> NEST = NestLayout.placements().stream()
            .filter(part -> part.material() == NestLayout.Material.AIR || part.material() == NestLayout.Material.STORAGE || part.material() == NestLayout.Material.RESERVE_CYST)
            .map(NestLayout.Placement::position).collect(Collectors.toUnmodifiableSet());

    private static final Set<NestLayout.Position> LEGACY_NEST = NestLayout.createPlacements(false, false).stream()
            .filter(part -> part.material() == NestLayout.Material.AIR || part.material() == NestLayout.Material.STORAGE)
            .map(NestLayout.Placement::position).collect(Collectors.toUnmodifiableSet());

    private static final Set<NestLayout.Position> FACETED = NestLayout.createPlacements(true, false).stream()
            .filter(part -> part.material() == NestLayout.Material.AIR || part.material() == NestLayout.Material.STORAGE)
            .map(NestLayout.Placement::position).collect(Collectors.toUnmodifiableSet());

    private HiveInterior() {}

    public static boolean nest(int x, int y, int z) {
        return nest(x, y, z, true, true);
    }

    public static boolean nest(int x, int y, int z, boolean faceted, boolean vaulted) {
        return y > 0 && (vaulted ? NEST : faceted ? FACETED : LEGACY_NEST).contains(new NestLayout.Position(x, y, z));
    }

    public static boolean traditional(int x, int y, int z, int radius) {
        if (y < 1 || y > (radius == 3 ? 3 : 4)) return false;
        return Math.abs(x) < radius && Math.abs(z) < radius
                || y <= 3 && z == radius && Math.abs(x) <= 1
                || y <= 2 && x == radius && z == 0;
    }

    public static boolean annex(int x, int y, int z) {
        return y >= 1 && y < GrowthLayout.ROOF && GrowthLayout.inFootprint(x, z) && !GrowthLayout.isShell(x, y, z);
    }

    public static boolean outpost(int x, int y, int z, int dx, int dz) {
        return y >= 1 && y < OutpostLayout.ROOF && OutpostTunnelLayout.interior(new NestLayout.Position(x, y, z), dx, dz);
    }
}
