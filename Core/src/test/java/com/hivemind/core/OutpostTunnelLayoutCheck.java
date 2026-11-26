package com.hivemind.core;

import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Set;
import java.util.Map;
import java.util.stream.Collectors;

public final class OutpostTunnelLayoutCheck {
    public static void main(String[] args) {
        Map<NestLayout.Position, NestLayout.Material> nest = NestLayout.placements().stream()
                .collect(Collectors.toMap(NestLayout.Placement::position, NestLayout.Placement::material));
        int[][] directions = {{24,0},{-24,0},{0,-24},{0,24}};
        for (int[] d : directions) {
            int dx = d[0], dz = d[1];
            var corridor = OutpostTunnelLayout.corridor(dx, dz);
            if (corridor.isEmpty() || corridor.size() != new HashSet<>(corridor).size()) throw new AssertionError("invalid corridor");
            for (var cell : corridor) {
                if (Math.abs(cell.x() - dx) <= 3 && Math.abs(cell.z() - dz) <= 3) throw new AssertionError("corridor overwrites outpost");
                if (cell.y() < 0 || cell.y() > 4) throw new AssertionError("invalid tunnel height");
                NestLayout.Material existing = nest.get(cell);
                if (cell.y() > 0 && existing != null && existing != NestLayout.Material.AIR
                        && !(existing == NestLayout.Material.CHITIN && OutpostTunnelLayout.isNestExit(cell.x(),cell.y(),cell.z(),dx,dz)))
                    throw new AssertionError("tunnel crosses protected nest at " + cell);
            }
            Set<NestLayout.Position> floor = new HashSet<>();
            for (var cell : corridor) if (cell.y() == 0) floor.add(cell);
            int startX = dx == 0 ? 0 : Integer.signum(dx) * 13;
            int startZ = dz == 0 ? 0 : Integer.signum(dz) * 13;
            var start = new NestLayout.Position(startX, 0, startZ);
            var outsideDoor = dx == 0 ? new NestLayout.Position(dx, 0, dz + 4)
                    : new NestLayout.Position(dx - Integer.signum(dx) * 4, 0, dz);
            var queue = new ArrayDeque<NestLayout.Position>();
            Set<NestLayout.Position> seen = new HashSet<>();
            queue.add(start); seen.add(start);
            while (!queue.isEmpty()) {
                var at = queue.remove();
                for (int[] dir : new int[][]{{1,0},{-1,0},{0,1},{0,-1}}) {
                    var next = new NestLayout.Position(at.x()+dir[0], 0, at.z()+dir[1]);
                    if (floor.contains(next) && seen.add(next)) queue.add(next);
                }
            }
            if (!seen.contains(outsideDoor)) throw new AssertionError("route does not reach door: " + dx + "," + dz);
            NestLayout.Room room = dx > 0 ? NestLayout.Room.STORAGE : dx < 0 ? NestLayout.Room.INCUBATION
                    : dz < 0 ? NestLayout.Room.REST : NestLayout.Room.DEFENSE;
            for (int y = 1; y <= 3; y++) for (int side = -1; side <= 1; side++) {
                int roomX = dx == 0 ? side : Integer.signum(dx) * 3;
                int roomZ = dz == 0 ? side : Integer.signum(dz) * 3;
                if (!OutpostTunnelLayout.isRoomExit(room, roomX, y, roomZ, dx, dz)
                        || !OutpostTunnelLayout.isNestExit(startX + (dx == 0 ? side : 0), y, startZ + (dz == 0 ? side : 0), dx, dz))
                    throw new AssertionError("room exit does not align with the route");
            }
            var cover = OutpostTunnelLayout.cover(dx, dz);
            if (dz != 24 && cover.isEmpty()) throw new AssertionError("missing cover");
            for (var cell : cover) {
                if (corridor.contains(cell) || NestLayout.roomAt(cell.x(), cell.z()) != null
                        || OutpostTunnelLayout.chamber(dx,dz).contains(cell)) throw new AssertionError("cover blocks route or building");
            }
            if (dx != 0) {
                int sign = Integer.signum(dx);
                var shell = OutpostLayout.shell(dx);
                if (shell.size() != OutpostLayout.shell().size() - 9) throw new AssertionError("wrong doorway size");
                for (int y = 1; y <= 3; y++) for (int z = -1; z <= 1; z++) {
                    if (shell.contains(new NestLayout.Position(-3 * sign, y, z))
                            || !OutpostTunnelLayout.isPostDoor(dx - 3 * sign, y, z, dx, dz))
                        throw new AssertionError("doorway would be sealed");
                    for (int step = 13; step <= 20; step++) {
                        if (!corridor.contains(new NestLayout.Position(step * sign, y, z)))
                            throw new AssertionError("interrupted lateral corridor");
                    }
                }
                var obsolete = OutpostTunnelLayout.obsoleteCover(dx, dz);
                if (obsolete.isEmpty()) throw new AssertionError("missing old cover to remove");
                for (var cell : obsolete) {
                    if (cell.y() <= 0 || corridor.contains(cell) || cover.contains(cell)
                            || OutpostTunnelLayout.chamber(dx, dz).contains(cell))
                        throw new AssertionError("old cover overlaps retained structure");
                }
            }
            if (OutpostTunnelLayout.chamber(dx,dz).size() != 294) throw new AssertionError("invalid chamber");
            for (var cell : OutpostTunnelLayout.chamber(dx, dz)) {
                var relative = new NestLayout.Position(cell.x() - dx, cell.y(), cell.z() - dz);
                boolean isShell = OutpostLayout.isShell(relative.x(), relative.y(), relative.z(), dx);
                if (isShell != OutpostLayout.shell(dx).contains(relative))
                    throw new AssertionError("construction and inspection disagree at " + cell);
                if (isShell && OutpostTunnelLayout.isPostDoor(cell.x(), cell.y(), cell.z(), dx, dz))
                    throw new AssertionError("door counted as shelter wall at " + cell);
            }
            if (!OutpostTunnelLayout.isPostDoor(dx,1,dz+3,dx,dz)) throw new AssertionError("post entrance lost");
            var entrance = OutpostTunnelLayout.connectingDoor(dx, dz);
            if (!OutpostTunnelLayout.isConnectingDoor(entrance, dx, dz)
                    || OutpostTunnelLayout.accessClear(dx, dz, cell -> true, cell -> !cell.equals(entrance))
                    || !OutpostTunnelLayout.accessClear(dx, dz, cell -> true, cell -> true))
                throw new AssertionError("blocking and reopening the connecting door must change outpost capacity");
            long doorCells = OutpostTunnelLayout.chamber(dx, dz).stream()
                    .filter(cell -> OutpostTunnelLayout.isConnectingDoor(cell, dx, dz)).count();
            if (doorCells != 9) throw new AssertionError("wrong connecting door size");
        }
        if (!OutpostLayout.isLegacyInterior(0, 4, 0) || OutpostLayout.isLegacyInterior(0, 0, 0)
                || !OutpostLayout.isShell(3, 1, 0)) throw new AssertionError("legacy conversion");
        if (OutpostTunnelLayout.supported(12, 12) || !OutpostTunnelLayout.corridor(12, 12).isEmpty()) throw new AssertionError("unsupported route");
        for (var offset : OutpostTunnelLayout.candidateOffsets()) {
            int dx = offset.x(), dz = offset.z();
            int side = OutpostTunnelLayout.side(dx, dz);
            var corridor = Set.copyOf(OutpostTunnelLayout.corridor(dx, dz));
            var cover = Set.copyOf(OutpostTunnelLayout.cover(dx, dz));
            var start = new NestLayout.Position(side * 13, 0, side == 0 ? -13 : 0);
            var end = new NestLayout.Position(dx - side * 4, 0, dz + (side == 0 ? 4 : 0));
            var queue = new ArrayDeque<NestLayout.Position>();
            Set<NestLayout.Position> visited = new HashSet<>();
            queue.add(start);
            visited.add(start);
            while (!queue.isEmpty()) {
                var cell = queue.remove();
                for (int[] direction : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
                    var next = new NestLayout.Position(cell.x() + direction[0], 0, cell.z() + direction[1]);
                    if (corridor.contains(next) && visited.add(next)) queue.add(next);
                }
            }
            if (!visited.contains(end) || !java.util.Collections.disjoint(corridor, cover))
                throw new AssertionError("disconnected or obstructed route to " + offset);
            for (var cell : corridor) {
                var material = nest.get(cell);
                if (cell.y() > 0 && material != null && material != NestLayout.Material.AIR
                        && !OutpostTunnelLayout.isNestExit(cell.x(), cell.y(), cell.z(), dx, dz))
                    throw new AssertionError("route crosses a protected nest block at " + cell);
            }
        }
        System.out.println("Four outpost tunnels connect to the correct entrance without overwriting the shell.");
    }
}
