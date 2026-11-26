package com.hivemind.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Geometry shared by the world adapters; positions are relative to the core. */
public final class NestLayout {
    public enum Room { CENTRAL, INCUBATION, STORAGE, REST, DEFENSE }
    public enum Material { CHITIN, AIR, CORE, STORAGE, BROOD_COMB, MOLT_BED, ALARM_RIDGE, RESERVE_CYST }
    public record Position(int x, int y, int z) {}
    public record Placement(Position position, Material material) {}

    private static final List<Placement> PLACEMENTS = createPlacements(true, true);
    private static final Map<Position, Material> FLOOR = PLACEMENTS.stream()
            .filter(p -> p.position().y() == 0)
            .collect(java.util.stream.Collectors.toUnmodifiableMap(Placement::position, Placement::material));

    public static Material repairMaterial(int x, int y, int z) {
        if (y != 0) return Material.CHITIN;
        return FLOOR.getOrDefault(new Position(x, y, z), Material.CHITIN);
    }

    private static final List<Placement> COMPACT = createCompactPlacements();

    private NestLayout() {}

    public static Position center(Room room) {
        return switch (room) {
            case CENTRAL -> new Position(0, 0, 0);
            case INCUBATION -> new Position(-10, 0, 0);
            case STORAGE -> new Position(10, 0, 0);
            case REST -> new Position(0, 0, -10);
            case DEFENSE -> new Position(0, 0, 10);
        };
    }

    public static Room roomAt(int x, int z) {
        for (Room room : Room.values()) {
            Position center = center(room);
            int radius = room == Room.CENTRAL ? 6 : 3;
            if (Math.abs(x - center.x()) <= radius && Math.abs(z - center.z()) <= radius) return room;
        }
        return null;
    }

    public static List<Placement> placements() { return PLACEMENTS; }
    public static List<Placement> compactPlacements() { return COMPACT; }

    public static Position guardOffset(java.util.UUID memberId) {
        int[][] posts = {{-2,-2},{0,-2},{2,-2},{-2,0},{2,0},{-2,2},{0,2},{2,2}};
        int[] post = posts[Math.floorMod(memberId.hashCode(), posts.length)];
        return new Position(post[0], 0, post[1]);
    }

    public static boolean isCentralDoor(int x, int y, int z) {
        return y >= 1 && y <= 3 && ((Math.abs(x) == 6 && Math.abs(z) <= 1)
                || (Math.abs(z) == 6 && Math.abs(x) <= 1));
    }

    public static boolean isRoomDoor(Room room, int x, int y, int z) {
        if (room == Room.CENTRAL) return isCentralDoor(x, y, z);
        if (y < 1 || y > 3) return false;
        return switch (room) {
            case INCUBATION -> x == 3 && Math.abs(z) <= 1;
            case STORAGE -> x == -3 && Math.abs(z) <= 1;
            case REST -> z == 3 && Math.abs(x) <= 1;
            case DEFENSE -> Math.abs(z) == 3 && Math.abs(x) <= 1;
            case CENTRAL -> false;
        };
    }

    static List<Placement> createPlacements(boolean faceted, boolean vaulted) {
        Map<Position, Material> cells = new LinkedHashMap<>();
        room(cells, center(Room.CENTRAL), 6, faceted, vaulted);
        for (Room room : Room.values()) if (room != Room.CENTRAL) room(cells, center(room), 3, faceted, vaulted);
        for (int side : new int[]{-1, 1}) {
            for (int step = 6; step <= 7; step++) for (int width = -1; width <= 1; width++) for (int y = 1; y <= 3; y++) {
                cells.put(new Position(side * step, y, width), Material.AIR);
                cells.put(new Position(width, y, side * step), Material.AIR);
            }
        }
        for (int z = 13; z <= 15; z++) for (int x = -1; x <= 1; x++) {
            for (int y = 1; y <= 3; y++) cells.put(new Position(x, y, z), Material.AIR);
            if (z > 13) cells.put(new Position(x, 0, z), Material.CHITIN);
        }
        if (faceted) {
            for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) {
                if (Math.abs(x) + Math.abs(z) <= 1) cells.put(new Position(-10 + x, 0, z), Material.BROOD_COMB);
                if (x != 0) cells.put(new Position(x, 0, -10 + z), Material.MOLT_BED);
                if (z == 0) cells.put(new Position(x, 0, 11), Material.ALARM_RIDGE);
            }
        }
        if (vaulted) {
            Position store = center(Room.STORAGE);
            for (int x : new int[]{-1, 1}) for (int z : new int[]{-2, 2}) {
                cells.put(new Position(store.x() + x, roofHeight(3, x, z) - 1, z), Material.RESERVE_CYST);
            }
        }
        cells.put(new Position(0, 0, 0), Material.CORE);
        cells.put(new Position(10, 1, 0), Material.STORAGE);
        List<Placement> result = new ArrayList<>(cells.size());
        cells.forEach((position, material) -> result.add(new Placement(position, material)));
        return Collections.unmodifiableList(result);
    }

    private static void room(Map<Position, Material> cells, Position center, int radius, boolean faceted, boolean vaulted) {
        for (int x = -radius; x <= radius; x++) for (int z = -radius; z <= radius; z++) for (int y = 0; y <= (vaulted ? roofHeight(radius, x, z) : 5); y++) {
            if (faceted && !inCell(radius, x, z)) continue;
            boolean shell = vaulted ? vaultedShell(radius, x, y, z) : faceted ? cellShell(radius, x, y, z) : Math.abs(x) == radius || Math.abs(z) == radius || y == 0 || y == 5;
            cells.put(new Position(center.x() + x, y, center.z() + z), shell ? Material.CHITIN : Material.AIR);
        }
    }

    private static boolean inCell(int radius, int x, int z) {
        return Math.abs(x) <= radius && Math.abs(z) <= radius && Math.abs(x) + Math.abs(z) <= radius + (radius + 1) / 2;
    }

    public static final int MAX_ROOF = 8;

    static int roofHeight(int radius, int x, int z) {
        int edge = Math.min(radius - Math.max(Math.abs(x), Math.abs(z)),
                radius + (radius + 1) / 2 - Math.abs(x) - Math.abs(z));
        return 4 + Math.min(radius == 6 ? 4 : 2, Math.max(0, edge));
    }

    public static boolean vaultedShell(int radius, int x, int y, int z) {
        if (!inCell(radius, x, z)) return false;
        int roof = roofHeight(radius, x, z);
        return y >= 0 && y <= roof && (y == 0 || y == roof || roof == 4);
    }

    public static boolean reserveCyst(int x, int y, int z) {
        return Math.abs(x - 10) == 1 && Math.abs(z) == 2 && y == roofHeight(3, x - 10, z) - 1;
    }

    public static boolean cellShell(int radius, int x, int y, int z) {
        if (!inCell(radius, x, z)) return false;
        int diagonal = Math.abs(x) + Math.abs(z);
        int edge = radius + (radius + 1) / 2;
        return y == 0 || y == 5 || Math.abs(x) == radius || Math.abs(z) == radius
                || diagonal == edge || y == 4 && diagonal == edge - 1;
    }

    private static List<Placement> createCompactPlacements() {
        List<Placement> result = new ArrayList<>();
        for (int y = 0; y <= 4; y++) for (int x = -3; x <= 3; x++) for (int z = -3; z <= 3; z++) {
            boolean door = y >= 1 && y <= 3 && (z == 3 && Math.abs(x) <= 1 || x == 3 && z == 0 && y <= 2);
            boolean shell = y == 0 || y == 4 || Math.abs(x) == 3 || Math.abs(z) == 3;
            Material material = y == 0 && x == 0 && z == 0 ? Material.CORE : shell && !door ? Material.CHITIN : Material.AIR;
            result.add(new Placement(new Position(x, y, z), material));
        }
        return List.copyOf(result);
    }
}
