package com.hivemind.core;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Routes from specialized rooms to their outposts. */
public final class OutpostTunnelLayout {
    public static final int MIN_DISTANCE = 24;
    public static final int MAX_DISTANCE = 40;
    public static final int LATERAL_LIMIT = 4;
    private record Route(int dx, int dz) {}
    private record Plan(List<NestLayout.Position> corridor, List<NestLayout.Position> chamber,
                        List<NestLayout.Position> cover, List<NestLayout.Position> chamberAccess,
                        Set<NestLayout.Position> interior) {}
    private static final java.util.Map<Route, Plan> PLANS = new java.util.concurrent.ConcurrentHashMap<>();
    private static final Plan UNSUPPORTED = new Plan(List.of(), List.of(), List.of(), List.of(), Set.of());
    private static final List<NestLayout.Position> EAST_OBSOLETE = obsoleteCover(24);
    private static final List<NestLayout.Position> WEST_OBSOLETE = obsoleteCover(-24);
    private OutpostTunnelLayout() {}

    public static boolean supported(int dx, int dz) {
        return (dx >= MIN_DISTANCE && dx <= MAX_DISTANCE || dx <= -MIN_DISTANCE && dx >= -MAX_DISTANCE)
                && dz >= -LATERAL_LIMIT && dz <= LATERAL_LIMIT
                || dz <= -MIN_DISTANCE && dz >= -MAX_DISTANCE && dx >= -LATERAL_LIMIT && dx <= LATERAL_LIMIT
                || dx == 0 && dz == 24;
    }

    public static int side(int dx, int dz) {
        return Math.abs(dz) <= LATERAL_LIMIT && supported(dx, dz) ? Integer.signum(dx) : 0;
    }

    public static NestLayout.Room entranceRoom(int dx, int dz) {
        if (!supported(dx, dz)) throw new IllegalArgumentException("Unsupported outpost route");
        int side = side(dx, dz);
        if (side != 0) return side > 0 ? NestLayout.Room.STORAGE : NestLayout.Room.INCUBATION;
        return dz < 0 ? NestLayout.Room.REST : NestLayout.Room.DEFENSE;
    }

    public static List<NestLayout.Position> candidateOffsets() {
        return Candidates.OFFSETS;
    }

    private static final class Candidates {
        private static final List<NestLayout.Position> OFFSETS = create();

        private static List<NestLayout.Position> create() {
            List<NestLayout.Position> offsets = new ArrayList<>();
            for (int lateral : new int[]{0, -2, 2, -4, 4}) {
                for (int distance = MIN_DISTANCE; distance <= MAX_DISTANCE; distance += 2) {
                    offsets.add(new NestLayout.Position(distance, 0, lateral));
                    offsets.add(new NestLayout.Position(-distance, 0, lateral));
                    offsets.add(new NestLayout.Position(lateral, 0, -distance));
                }
            }
            return List.copyOf(offsets);
        }
    }

    public static boolean isRoomExit(NestLayout.Room room, int x, int y, int z, int dx, int dz) {
        if (!supported(dx, dz) || y < 1 || y > 3) return false;
        int side = side(dx, dz);
        if (side > 0) return room == NestLayout.Room.STORAGE && x == 3 && Math.abs(z) <= 1;
        if (side < 0) return room == NestLayout.Room.INCUBATION && x == -3 && Math.abs(z) <= 1;
        if (dz < 0) return room == NestLayout.Room.REST && z == -3 && Math.abs(x) <= 1;
        return room == NestLayout.Room.DEFENSE && z == 3 && Math.abs(x) <= 1;
    }

    public static boolean isNestExit(int x, int y, int z, int dx, int dz) {
        if (!supported(dx, dz) || y < 1 || y > 3) return false;
        int side = side(dx, dz);
        if (side != 0) return x == side * 13 && Math.abs(z) <= 1;
        return z == Integer.signum(dz) * 13 && Math.abs(x) <= 1;
    }

    public static boolean isPostDoor(int x, int y, int z, int dx, int dz) {
        if (y < 1 || y > 3) return false;
        return z == dz + OutpostLayout.RADIUS && Math.abs(x - dx) <= 1
                || OutpostLayout.isFacingDoor(x - dx, y, z - dz, side(dx, dz));
    }

    public static NestLayout.Position connectingDoor(int dx, int dz) {
        if (!supported(dx, dz)) throw new IllegalArgumentException("Unsupported outpost route");
        int side = side(dx, dz);
        int radius = OutpostLayout.RADIUS;
        return new NestLayout.Position(dx - side * radius, 1, side == 0 ? dz + radius : dz);
    }

    public static boolean isConnectingDoor(NestLayout.Position cell, int dx, int dz) {
        if (cell.y() < 1 || cell.y() > 3) return false;
        var door = connectingDoor(dx, dz);
        return side(dx, dz) == 0 ? cell.z() == door.z() && Math.abs(cell.x() - door.x()) <= 1
                : cell.x() == door.x() && Math.abs(cell.z() - door.z()) <= 1;
    }

    public static boolean accessClear(int dx, int dz, java.util.function.Predicate<NestLayout.Position> floor,
                                      java.util.function.Predicate<NestLayout.Position> clear) {
        if (!supported(dx, dz)) return false;
        var plan = plan(dx, dz);
        for (var cell : plan.corridor()) if (!(cell.y() == 0 ? floor : clear).test(cell)) return false;
        for (var cell : plan.chamberAccess()) if (!clear.test(cell)) return false;
        return true;
    }

    public static boolean interior(NestLayout.Position cell, int dx, int dz) {
        return plan(dx, dz).interior().contains(cell);
    }

    public static List<NestLayout.Position> obsoleteCover(int dx, int dz) {
        if (dz != 0) return List.of();
        if (dx == 24) return EAST_OBSOLETE;
        if (dx == -24) return WEST_OBSOLETE;
        return List.of();
    }

    public static List<NestLayout.Position> corridor(int dx, int dz) {
        return plan(dx, dz).corridor();
    }

    private static Plan plan(int dx, int dz) {
        if (!supported(dx, dz)) return UNSUPPORTED;
        return PLANS.computeIfAbsent(new Route(dx, dz), route -> {
            var corridor = makeCorridor(route.dx(), route.dz(), false);
            var cover = route.dz() == 24 ? List.<NestLayout.Position>of() : makeCover(route.dx(), route.dz(), corridor);
            var chamber = makeChamber(route.dx(), route.dz());
            int side = side(route.dx(), route.dz());
            var chamberAccess = chamber.stream().filter(cell ->
                    !OutpostLayout.isShell(cell.x() - route.dx(), cell.y(), cell.z() - route.dz(), side)).toList();
            var interior = new java.util.HashSet<>(corridor);
            interior.addAll(chamberAccess);
            return new Plan(corridor, chamber, cover, chamberAccess, Set.copyOf(interior));
        });
    }

    private static List<NestLayout.Position> makeCorridor(int dx, int dz, boolean former) {
        if (!supported(dx, dz)) return List.of();
        int[][] waypoints;
        int sign = side(dx, dz);
        if (sign != 0) {
            waypoints = former ? new int[][]{{sign * 13, 0}, {sign * 19, 0}, {sign * 19, 4}, {dx, 4}}
                    : new int[][]{{sign * 13, 0}, {sign * 16, 0}, {sign * 16, dz}, {dx - sign * 4, dz}};
        } else if (dz < 0) {
            waypoints = new int[][]{{0, -13}, {0, -16}, {dx, -16}, {dx, dz + 4}};
        } else {
            waypoints = new int[][]{{0, 13}, {0, 15}, {5, 15}, {5, 28}, {0, 28}};
        }
        Set<NestLayout.Position> cells = new LinkedHashSet<>();
        for (int i = 0; i < waypoints.length - 1; i++) {
            int x = waypoints[i][0], z = waypoints[i][1];
            int endX = waypoints[i + 1][0], endZ = waypoints[i + 1][1];
            if (x != endX && z != endZ) throw new IllegalStateException("non-orthogonal route");
            while (true) {
                for (int ox = -1; ox <= 1; ox++) for (int oz = -1; oz <= 1; oz++) {
                    int px = x + ox, pz = z + oz;
                    if (Math.abs(px - dx) <= OutpostLayout.RADIUS && Math.abs(pz - dz) <= OutpostLayout.RADIUS) continue;
                    for (int y = 0; y <= 3; y++) cells.add(new NestLayout.Position(px, y, pz));
                    if (dz != 24 && outsideBuildings(px, pz, dx, dz))
                        cells.add(new NestLayout.Position(px, 4, pz));
                }
                if (x == endX && z == endZ) break;
                x += Integer.compare(endX, x); z += Integer.compare(endZ, z);
            }
        }
        return List.copyOf(cells);
    }

    public static List<NestLayout.Position> chamber(int dx, int dz) {
        return plan(dx, dz).chamber();
    }

    public static List<NestLayout.Position> cover(int dx, int dz) {
        return plan(dx, dz).cover();
    }

    private static List<NestLayout.Position> obsoleteCover(int dx) {
        var former = new LinkedHashSet<>(makeCover(dx, 0, makeCorridor(dx, 0, true)));
        former.removeAll(cover(dx, 0));
        return List.copyOf(former);
    }

    private static List<NestLayout.Position> makeCover(int dx, int dz, List<NestLayout.Position> corridor) {
        Set<NestLayout.Position> floor = new LinkedHashSet<>();
        for (var part : corridor) if (part.y() == 0) floor.add(part);
        Set<NestLayout.Position> result = new LinkedHashSet<>();
        for (var part : floor) {
            int x = part.x(), z = part.z();
            if (outsideBuildings(x, z, dx, dz)) result.add(new NestLayout.Position(x, OutpostLayout.ROOF, z));
            for (int sx = -1; sx <= 1; sx++) for (int sz = -1; sz <= 1; sz++) {
                int wallX = x + sx, wallZ = z + sz;
                if (!outsideBuildings(wallX, wallZ, dx, dz)
                        || floor.contains(new NestLayout.Position(wallX, 0, wallZ))) continue;
                for (int y = 1; y <= OutpostLayout.ROOF; y++) result.add(new NestLayout.Position(wallX, y, wallZ));
            }
        }
        return List.copyOf(result);
    }

    private static boolean outsideBuildings(int x, int z, int dx, int dz) {
        return NestLayout.roomAt(x, z) == null
                && (Math.abs(x - dx) > OutpostLayout.RADIUS || Math.abs(z - dz) > OutpostLayout.RADIUS);
    }

    private static List<NestLayout.Position> makeChamber(int dx, int dz) {
        if (!supported(dx, dz)) return List.of();
        List<NestLayout.Position> cells = new ArrayList<>();
        int radius = OutpostLayout.RADIUS;
        for (int z = radius; z >= -radius; z--) for (int y = 1; y <= OutpostLayout.ROOF; y++) for (int x = -radius; x <= radius; x++)
            cells.add(new NestLayout.Position(dx + x, y, dz + z));
        for (int z = radius; z >= -radius; z--) for (int x = -radius; x <= radius; x++)
            cells.add(new NestLayout.Position(dx + x, 0, dz + z));
        return List.copyOf(cells);
    }
}
