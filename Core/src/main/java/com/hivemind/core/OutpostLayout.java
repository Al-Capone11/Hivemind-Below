package com.hivemind.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** A small queenless shelter, built a block at a time by workers. */
public final class OutpostLayout {
    public static final int RADIUS = 3;
    public static final int ROOF = 5;
    private static final List<NestLayout.Position> SHELL = createShell(RADIUS, ROOF);
    private static final List<NestLayout.Position> EAST_SHELL = createSideShell(1);
    private static final List<NestLayout.Position> WEST_SHELL = createSideShell(-1);
    private static final Set<NestLayout.Position> SHELL_SET = Set.copyOf(SHELL);
    private static final Set<NestLayout.Position> LEGACY_SHELL = Set.copyOf(createShell(2, 4));

    private OutpostLayout() {}

    public static List<NestLayout.Position> shell() { return SHELL; }

    public static List<NestLayout.Position> shell(int dx) {
        return dx > 0 ? EAST_SHELL : dx < 0 ? WEST_SHELL : SHELL;
    }

    public static boolean isFacingDoor(int x, int y, int z, int dx) {
        return dx != 0 && x == -Integer.signum(dx) * RADIUS && Math.abs(z) <= 1 && y >= 1 && y <= 3;
    }

    private static List<NestLayout.Position> createSideShell(int dx) {
        return SHELL.stream().filter(part -> !isFacingDoor(part.x(), part.y(), part.z(), dx)).toList();
    }

    public static boolean isShell(int x, int y, int z) {
        return SHELL_SET.contains(new NestLayout.Position(x, y, z));
    }

    public static boolean isShell(int x, int y, int z, int dx) {
        return isShell(x, y, z) && !isFacingDoor(x, y, z, dx);
    }

    public static boolean isLegacyInterior(int x, int y, int z) {
        var position = new NestLayout.Position(x, y, z);
        return LEGACY_SHELL.contains(position) && !SHELL_SET.contains(position);
    }

    private static List<NestLayout.Position> createShell(int radius, int roof) {
        List<NestLayout.Position> result = new ArrayList<>();
        for (int y = 0; y <= roof; y++) for (int x = -radius; x <= radius; x++) for (int z = -radius; z <= radius; z++) {
            if (y != 0 && y != roof && Math.abs(x) != radius && Math.abs(z) != radius) continue;
            if (z == radius && Math.abs(x) <= 1 && y >= 1 && y <= 3) continue;
            result.add(new NestLayout.Position(x, y, z));
        }
        return List.copyOf(result);
    }
}
