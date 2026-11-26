package com.hivemind.core;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class OutpostExcavationCheck {
    private enum Material { AIR, STONE, HIVE, STORAGE }
    private static final int[][] SIDES = {{0,-1},{-1,0},{1,0},{0,1}};

    public static void main(String[] args) {
        for (int[] direction : new int[][]{{24,0},{-24,0},{0,-24},{0,24}}) {
            simulate(direction[0], direction[1], false, NestLayout.placements());
            simulate(direction[0], direction[1], false, OrganicNest.FULL.plan);
            simulate(direction[0], direction[1], true, NestLayout.placements());
            simulate(direction[0], direction[1], true, OrganicNest.FULL.plan);
        }
        System.out.println("Excavation reaches all four outposts through reachable work positions.");
    }

    private static void simulate(int dx, int dz, boolean surface, java.util.List<NestLayout.Placement> plan) {
        Map<NestLayout.Position, Material> world = new HashMap<>();
        for (var placement : plan) world.put(placement.position(), switch (placement.material()) {
            case AIR -> Material.AIR;
            case RESERVE_CYST, BROOD_COMB, MOLT_BED, ALARM_RIDGE, CHITIN, CORE -> Material.HIVE;
            case STORAGE -> Material.STORAGE;
        });
        var start = new NestLayout.Position(0,1,2);
        var route = OutpostTunnelLayout.corridor(dx, dz);
        var room = OutpostTunnelLayout.chamber(dx, dz);
        var obsolete = OutpostTunnelLayout.obsoleteCover(dx, dz);
        if (surface) {
            for (var cell : obsolete) world.put(cell, Material.HIVE);
            for (var cell : room) {
                if (OutpostLayout.isFacingDoor(cell.x() - dx, cell.y(), cell.z() - dz, dx)) world.put(cell, Material.HIVE);
                if (OutpostLayout.isLegacyInterior(cell.x() - dx, cell.y(), cell.z() - dz))
                    world.put(cell, Material.HIVE);
                if (cell.y() == 0 && Math.abs(cell.x() - dx) <= 2 && Math.abs(cell.z() - dz) <= 2)
                    world.put(cell, Material.HIVE);
            }
        }
        int removed = 0;
        while (true) {
            Set<NestLayout.Position> accessible = reachable(world, start, surface);
            NestLayout.Position job = null;
            boolean pendingRoute = false;
            for (var cell : route) {
                Material block = material(world, cell, surface);
                if (cell.y() == 0 && block != Material.AIR || cell.y() > 0 && block == Material.AIR) continue;
                pendingRoute = true;
                if (block == Material.STORAGE) throw new AssertionError("storage in route");
                if (cell.y() > 0 && block == Material.HIVE && !OutpostTunnelLayout.isNestExit(cell.x(),cell.y(),cell.z(),dx,dz))
                    throw new AssertionError("protected hive in route " + cell);
                if (hasApproach(cell, accessible)) { job = cell; break; }
            }
            boolean pendingRoom = false;
            if (!pendingRoute) for (var cell : room) {
                Material block = material(world, cell, surface);
                if (cell.y() == 0 ? block == Material.HIVE : block == Material.AIR) continue;
                pendingRoom = true;
                if (hasApproach(cell, accessible)) { job = cell; break; }
            }
            if (!pendingRoute && !pendingRoom) break;
            if (job == null) throw new AssertionError("excavation stuck " + dx + "," + dz + " after " + removed);
            world.put(job, job.y() == 0 ? Material.HIVE : Material.AIR);
            if (++removed > 2000) throw new AssertionError("excavation did not terminate");
        }
        if (!reachable(world, start, surface).contains(new NestLayout.Position(dx, 1, dz)))
            throw new AssertionError("excavated room is unreachable " + dx + "," + dz);
    }

    private static boolean hasApproach(NestLayout.Position target, Set<NestLayout.Position> accessible) {
        for (int[] side : SIDES) if (accessible.contains(new NestLayout.Position(target.x()+side[0],1,target.z()+side[1]))) return true;
        return false;
    }

    private static Set<NestLayout.Position> reachable(Map<NestLayout.Position, Material> world, NestLayout.Position start, boolean surface) {
        Set<NestLayout.Position> result = new HashSet<>();
        var queue = new ArrayDeque<NestLayout.Position>();
        result.add(start); queue.add(start);
        while (!queue.isEmpty()) {
            var at = queue.remove();
            for (int[] side : SIDES) {
                var next = new NestLayout.Position(at.x()+side[0],1,at.z()+side[1]);
                if (Math.abs(next.x()) > 36 || Math.abs(next.z()) > 36) continue;
                var above = new NestLayout.Position(next.x(),2,next.z());
                var floor = new NestLayout.Position(next.x(),0,next.z());
                if (material(world,next,surface) == Material.AIR && material(world,above,surface) == Material.AIR
                        && material(world,floor,surface) != Material.AIR && result.add(next)) queue.add(next);
            }
        }
        return result;
    }

    private static Material material(Map<NestLayout.Position, Material> world, NestLayout.Position pos, boolean surface) {
        return world.getOrDefault(pos, surface && pos.y() >= 0 ? Material.AIR : Material.STONE);
    }
}
