package com.hivemind.core;

import java.util.HashSet;
import java.util.ArrayDeque;
import java.util.Set;

public final class GrowthLayoutCheck {
    public static void main(String[] args) {
        var shell = GrowthLayout.shell();
        var excavation = GrowthLayout.excavation();
        if (excavation.size() != 395 || new HashSet<>(excavation).size() != excavation.size()) throw new AssertionError("invalid excavation volume");
        if (!excavation.get(0).equals(new NestLayout.Position(0, 1, 14))) throw new AssertionError("excavation must start at the entrance");
        for (var part : excavation) if (!GrowthLayout.inFootprint(part.x(), part.z()) || part.y() < 1 || part.y() > GrowthLayout.ROOF) throw new AssertionError("excavation outside planned room");
        if (shell.isEmpty() || shell.size() != new HashSet<>(shell).size()) throw new AssertionError("invalid shell");
        for (var part : shell) {
            if (!GrowthLayout.inFootprint(part.x(), part.z()) || part.y() < 0 || part.y() > GrowthLayout.ROOF
                    || !GrowthLayout.isShell(part.x(), part.y(), part.z())) throw new AssertionError(part);
        }
        for (int z = 14; z <= 25; z++) for (int x = -1; x <= 1; x++) for (int y = 1; y <= 3; y++) {
            if (GrowthLayout.isShell(x, y, z)) throw new AssertionError("blocked route");
        }
        for (int x = -3; x <= 3; x++) for (int y = 1; y <= 3; y++) {
            if (GrowthLayout.isShell(x, y, 26) != (Math.abs(x) > 1)) throw new AssertionError("incorrect exterior exit");
        }
        for (int z = 14; z <= 15; z++) {
            if (!GrowthLayout.isShell(0, 0, z) || !GrowthLayout.isShell(0, GrowthLayout.ROOF, z)) throw new AssertionError("disconnected entrance");
        }
        if (!GrowthLayout.isFormerCeiling(new NestLayout.Position(0, 4, 23))
                || GrowthLayout.isFormerCeiling(new NestLayout.Position(3, 4, 26))
                || !GrowthLayout.isShell(3, GrowthLayout.ROOF, 26)) throw new AssertionError("roof migration or corner");
        Set<NestLayout.Position> air = new HashSet<>();
        for (int x = -1; x <= 1; x++) for (int y = 1; y <= 3; y++) air.add(new NestLayout.Position(x, y, 13));
        Set<NestLayout.Position> remaining = new HashSet<>(excavation);
        int[][] sides = {{0,-1},{-1,0},{1,0},{0,1}};
        while (!remaining.isEmpty()) {
            Set<NestLayout.Position> reachable = new HashSet<>();
            var queue = new ArrayDeque<NestLayout.Position>();
            var entrance = new NestLayout.Position(0, 1, 13);
            reachable.add(entrance); queue.add(entrance);
            while (!queue.isEmpty()) {
                var pos = queue.remove();
                for (int[] side : sides) {
                    var next = new NestLayout.Position(pos.x() + side[0], 1, pos.z() + side[1]);
                    if (air.contains(next) && air.contains(new NestLayout.Position(next.x(), 2, next.z())) && reachable.add(next)) queue.add(next);
                }
            }
            NestLayout.Position chosen = null;
            for (var target : excavation) {
                if (!remaining.contains(target)) continue;
                for (int[] side : sides) {
                    var approach = new NestLayout.Position(target.x() + side[0], 1, target.z() + side[1]);
                    if (air.contains(approach) && air.contains(new NestLayout.Position(approach.x(), 2, approach.z()))) {
                        if (!reachable.contains(approach)) throw new AssertionError("selected approach is unreachable");
                        chosen = target; break;
                    }
                }
                if (chosen != null) break;
            }
            if (chosen == null) throw new AssertionError("excavation route becomes inaccessible with " + remaining.size() + " blocks left");
            remaining.remove(chosen); air.add(chosen);
        }
        System.out.println("Growth layout connects to the nest and preserves an exterior exit.");
    }
}
