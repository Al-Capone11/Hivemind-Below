package com.hivemind.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** A corridor and chamber beyond the defensive room's existing exit. */
public final class GrowthLayout {
    public static final int ROOF = 5;
    private static final List<NestLayout.Position> SHELL = makeShell();
    private static final Set<NestLayout.Position> SHELL_SET = Set.copyOf(SHELL);
    private static final List<NestLayout.Position> EXCAVATION = makeExcavation();

    private GrowthLayout() {}

    public static List<NestLayout.Position> shell() { return SHELL; }

    public static List<NestLayout.Position> excavation() { return EXCAVATION; }

    public static boolean isShell(int x, int y, int z) {
        return SHELL_SET.contains(new NestLayout.Position(x, y, z));
    }

    public static boolean isFormerCeiling(NestLayout.Position position) {
        return position.y() == 4 && !isShell(position.x(), position.y(), position.z())
                && inFootprint(position.x(), position.z());
    }

    public static boolean inFootprint(int x, int z) {
        return z >= 14 && z <= 19 && Math.abs(x) <= 2
            || z >= 20 && z <= 26 && Math.abs(x) <= 3;
    }

    private static List<NestLayout.Position> makeShell() {
        List<NestLayout.Position> result = new ArrayList<>();
        for (int z = 14; z <= 26; z++) {
            int radius = z < 20 ? 2 : 3;
            for (int x = -radius; x <= radius; x++) for (int y = 0; y <= ROOF; y++) {
                boolean wall = Math.abs(x) == radius || (z == 20 || z == 26) && Math.abs(x) > 1;
                if (z == 26) wall = Math.abs(x) > 1 || y == ROOF - 1;
                if (y == 0 || y == ROOF || wall) result.add(new NestLayout.Position(x, y, z));
            }
        }
        return List.copyOf(result);
    }

    private static List<NestLayout.Position> makeExcavation() {
        List<NestLayout.Position> result = new ArrayList<>();
        int[] sides = {0, -1, 1, -2, 2, -3, 3};
        for (int z = 14; z <= 26; z++) for (int y = 1; y <= ROOF; y++) for (int x : sides) {
            if (inFootprint(x, z)) result.add(new NestLayout.Position(x, y, z));
        }
        return List.copyOf(result);
    }
}
