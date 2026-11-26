package com.hivemind.core;
import java.io.DataOutputStream;
import java.nio.file.Files;
import java.util.*;
public final class NestLayoutCheck {
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
    public static void main(String[] args) throws Exception {
        Map<NestLayout.Position,NestLayout.Material> blocks = new HashMap<>();
        for (var p : NestLayout.placements()) check(blocks.put(p.position(), p.material()) == null, "duplicate placement");
        check(blocks.get(new NestLayout.Position(0,0,0)) == NestLayout.Material.CORE, "missing core");
        check(blocks.get(new NestLayout.Position(10,1,0)) == NestLayout.Material.STORAGE, "missing inventory");
        check(HiveInterior.nest(0, 1, 0) && HiveInterior.nest(7, 1, 0) && HiveInterior.nest(10, 1, 0),
                "spawn protection must cover the core, hallway and storage room");
        check(!HiveInterior.nest(0, NestLayout.MAX_ROOF, 0) && !HiveInterior.nest(14, 1, 12),
                "spawn protection must exclude the roof and exterior");
        check(HiveInterior.traditional(0, 1, 0, 3) && !HiveInterior.traditional(4, 1, 0, 3),
                "spawn protection must follow the compact nest footprint");
        check(HiveInterior.annex(0, 1, 18) && !HiveInterior.annex(2, 1, 18),
                "annex protection must stop at the corridor walls");
        check(HiveInterior.outpost(17, 1, 0, 24, 0) && HiveInterior.outpost(24, 1, 0, 24, 0)
                        && !HiveInterior.outpost(24, 1, -4, 24, 0),
                "outpost protection must follow the passage and room");
        var compact = NestLayout.compactPlacements().stream().collect(java.util.stream.Collectors.toMap(
                NestLayout.Placement::position, NestLayout.Placement::material));
        check(compact.size() == 245 && compact.get(new NestLayout.Position(0,0,0)) == NestLayout.Material.CORE,
                "compact nest needs a complete floor and shell");
        for (int z = 0; z <= 3; z++) for (int y = 1; y <= 3; y++)
            check(compact.get(new NestLayout.Position(0,y,z)) == NestLayout.Material.AIR, "compact entrance must be passable");
        for (int x=-1;x<=1;x++) for (int z=-1;z<=1;z++) for (int y=1;y<=3;y++)
            check(blocks.get(new NestLayout.Position(x,y,z)) == NestLayout.Material.AIR, "queen has no space");
        check(!blocks.containsKey(new NestLayout.Position(6,0,6)), "faceted cell has a square corner");
        check(NestLayout.cellShell(6,4,2,5), "diagonal wall missing from repair plan");
        check(!HiveInterior.nest(5,1,5,true,false) && HiveInterior.nest(5,1,5,false,false), "old nests retain their interior");
        for (var entry : blocks.entrySet()) if (entry.getKey().y() == 0) {
            var p = entry.getKey();
            check(NestLayout.repairMaterial(p.x(),0,p.z()) == entry.getValue(), "floor repair loses room material");
        }
        check(HiveInterior.nest(0,7,0) && !HiveInterior.nest(0,7,0,true,false), "vault height must match saved architecture");
        for (var cell : blocks.entrySet()) if (cell.getValue() == NestLayout.Material.RESERVE_CYST) {
            var p = cell.getKey();
            check(NestLayout.reserveCyst(p.x(), p.y(), p.z()) && blocks.get(new NestLayout.Position(p.x(),p.y()+1,p.z())) == NestLayout.Material.CHITIN, "reserve must hang from vault");
        }
        check(blocks.values().stream().filter(m -> m == NestLayout.Material.RESERVE_CYST).count() == 4, "reservoir clusters");
        Set<NestLayout.Position> visited = new HashSet<>();
        ArrayDeque<NestLayout.Position> queue = new ArrayDeque<>();
        queue.add(new NestLayout.Position(0,1,0));
        while (!queue.isEmpty()) {
            var a=queue.remove();
            if (!visited.add(a)) continue;
            for (int[] d : new int[][]{{1,0},{-1,0},{0,1},{0,-1}}) {
                var n=new NestLayout.Position(a.x()+d[0],1,a.z()+d[1]);
                if (blocks.get(n) == NestLayout.Material.AIR && !visited.contains(n)) queue.add(n);
            }
        }
        for (var room : NestLayout.Room.values()) {
            var c=NestLayout.center(room);
            var position=new NestLayout.Position(c.x()+(room==NestLayout.Room.STORAGE?1:0),1,c.z());
            check(visited.contains(position), "room is unreachable: "+room);
        }
        check(visited.contains(new NestLayout.Position(0,1,15)), "no route to exit");
        for (int y=1;y<=3;y++) for (int width=-1;width<=1;width++) for (int side : new int[]{-1,1}) {
            check(NestLayout.isCentralDoor(side*6,y,width) && NestLayout.isCentralDoor(width,y,side*6), "repair logic must keep the four doorways open");
            check(blocks.get(new NestLayout.Position(side*6,y,width)) == NestLayout.Material.AIR, "east/west door is obstructed");
            check(blocks.get(new NestLayout.Position(width,y,side*6)) == NestLayout.Material.AIR, "north/south door is obstructed");
        }
        for (NestLayout.Room room : NestLayout.Room.values()) {
            if (room == NestLayout.Room.CENTRAL) continue;
            var center=NestLayout.center(room);
            for (int y=1;y<=3;y++) for (int x=-3;x<=3;x++) for (int z=-3;z<=3;z++) {
                if (NestLayout.isRoomDoor(room,x,y,z)) {
                    check(blocks.get(new NestLayout.Position(center.x()+x,y,center.z()+z)) == NestLayout.Material.AIR,
                            "repair logic must preserve the room entrance: "+room);
                }
            }
        }
        check(OutpostLayout.shell().contains(new NestLayout.Position(0,0,0)), "outpost needs a floor");
        check(OutpostLayout.shell().contains(new NestLayout.Position(0,5,0)), "outpost needs a roof");
        check(!OutpostLayout.shell().contains(new NestLayout.Position(0,1,2)), "outpost entrance must stay open");
        for (int x = -1; x <= 1; x++) for (int y = 1; y <= 3; y++) {
            check(!OutpostLayout.shell().contains(new NestLayout.Position(x,y,3)), "outpost entrance must remain three blocks wide and high");
        }
        check(OutpostLayout.shell().contains(new NestLayout.Position(-3,1,3)), "outpost door must retain the corner");
        var file=Files.createTempFile("hivemind-v3-", ".dat");
        UUID colony=UUID.randomUUID(),queen=UUID.randomUUID();
        try {
            try (var d=new DataOutputStream(Files.newOutputStream(file))) {
                d.writeInt(0x48495645);d.writeInt(3);d.writeInt(1);
                d.writeLong(colony.getMostSignificantBits());d.writeLong(colony.getLeastSignificantBits());
                d.writeByte(ColonyState.ACTIVE.ordinal());d.writeBoolean(true);d.writeLong(queen.getMostSignificantBits());d.writeLong(queen.getLeastSignificantBits());
                d.writeBoolean(true);d.writeUTF("minecraft:overworld");d.writeInt(0);d.writeInt(20);d.writeInt(0);
                d.writeInt(36);d.writeInt(0);d.writeInt(1);d.writeLong(queen.getMostSignificantBits());d.writeLong(queen.getLeastSignificantBits());d.writeByte(Caste.QUEEN.ordinal());
                d.writeInt(0);d.writeInt(0);d.writeBoolean(false);d.writeBoolean(false);
            }
            ColonyRegistry old=ColonyRegistry.load(file);Colony migrated=old.get(colony).orElseThrow();
            check(!migrated.hasSpecializedRooms() && migrated.food()==36 && migrated.queenId().orElseThrow().equals(queen), "legacy colony migration changed state");
            check(migrated.populationLimit(true, 1) == 7, "legacy population limit");
            byte[] legacy = Files.readAllBytes(file);
            for (int version = 5; version <= 6; version++) {
                try (var output = new DataOutputStream(Files.newOutputStream(file))) {
                    java.nio.ByteBuffer.wrap(legacy).putInt(4, version);
                    output.write(legacy);
                    output.writeBoolean(true);
                    output.writeBoolean(true);
                    output.writeUTF("minecraft:overworld");
                    output.writeInt(24);
                    output.writeInt(20);
                    output.writeInt(0);
                    output.writeBoolean(true);
                    if (version == 6) output.writeLong(6);
                }
                Colony imported = ColonyRegistry.load(file).get(colony).orElseThrow();
                check(imported.outposts().equals(List.of(new Colony.Outpost(new ColonyLocation("minecraft:overworld", 24, 20, 0), true))),
                        "v" + version + " single outpost must migrate into the network");
                check(imported.upkeepDay() == (version == 6 ? 6 : -1), "legacy upkeep day");
            }
            migrated.enableSpecializedRooms();migrated.enableFacetedRooms();old.save(file);
            check(migrated.populationLimit(false, 0) == 12 && migrated.populationLimit(true, 3) == 24, "capacity must follow completed rooms");
            Colony restored=ColonyRegistry.load(file).get(colony).orElseThrow();
            check(restored.hasSpecializedRooms() && restored.hasFacetedRooms() && !restored.hasVaultedRooms(), "old plan changed on reload");
            byte[] facetedSave = Files.readAllBytes(file);
            byte[] versionNine = Arrays.copyOf(facetedSave, facetedSave.length - 2);
            java.nio.ByteBuffer.wrap(versionNine).putInt(4, 9);
            Files.write(file, versionNine);
            var previousPlan = ColonyRegistry.load(file).get(colony).orElseThrow();
            check(previousPlan.hasFacetedRooms() && !previousPlan.hasVaultedRooms(), "beta51 save must keep its faceted plan");
            restored.enableVaultedRooms();
            old.get(colony).orElseThrow().enableVaultedRooms();
            old.save(file);
            check(ColonyRegistry.load(file).get(colony).orElseThrow().hasVaultedRooms(), "vaulted plan lost on reload");
            byte[] vaulted = Files.readAllBytes(file);
            byte[] versionTen = Arrays.copyOf(vaulted, vaulted.length - 1);
            java.nio.ByteBuffer.wrap(versionTen).putInt(4, 10);
            Files.write(file, versionTen);
            var beta52 = ColonyRegistry.load(file).get(colony).orElseThrow();
            check(beta52.hasVaultedRooms() && !beta52.hasOrganicNest() && beta52.coreLocation().equals(beta52.home()),
                    "beta52 must retain its central core");
            migrated.enableOrganicNest();
            old.save(file);
            var royal = ColonyRegistry.load(file).get(colony).orElseThrow();
            check(royal.hasOrganicNest() && royal.coreLocation().x() == royal.home().x() + OrganicNest.FULL.throne.x()
                    && royal.coreLocation().z() == royal.home().z() + OrganicNest.FULL.throne.z(), "royal chamber lost after reload");
            var nursery = new ColonyLocation("minecraft:overworld", -11, 21, -7);
            royal.startBrood(nursery, Caste.WORKER, 1800L);
            royal.deferBrood(100L);
            var checkedRegistry = new ColonyRegistry();
            var checkedColony = checkedRegistry.create(royal.home());
            checkedColony.startBrood(nursery, royal.broodCaste(), royal.broodReadyTime());
            checkedRegistry.save(file);
            var resumedBrood = ColonyRegistry.load(file).get(checkedColony.id()).orElseThrow();
            check(nursery.equals(resumedBrood.broodSite()) && resumedBrood.broodReadyTime() == 1900L,
                    "paused brood must keep its remaining care time after reload");
            var site=new ColonyLocation("minecraft:overworld",24,20,0);
            restored.beginOutpost(site);
            ColonyRegistry ongoing=new ColonyRegistry();
            var isolated=ongoing.create(new ColonyLocation("minecraft:overworld",100,20,0));
            var east = new ColonyLocation("minecraft:overworld",124,20,0);
            var west = new ColonyLocation("minecraft:overworld",76,20,0);
            isolated.beginOutpost(east);
            ongoing.save(file);
            ColonyRegistry reloaded=ColonyRegistry.load(file);
            Colony loaded=reloaded.get(isolated.id()).orElseThrow();
            check(loaded.hasOutpostProject() && loaded.hasOutpost(east), "unfinished outpost did not persist");
            loaded.finishOutpost(east);
            loaded.beginOutpost(west);
            check(!loaded.beginUpkeep(1) && loaded.beginUpkeep(2) && !loaded.beginUpkeep(2), "upkeep must run once per day");
            reloaded.save(file);
            Colony persisted = ColonyRegistry.load(file).get(isolated.id()).orElseThrow();
            check(persisted.outposts().equals(java.util.List.of(new Colony.Outpost(east, true), new Colony.Outpost(west, false))),
                    "outpost network did not persist");
            check(!persisted.beginUpkeep(2) && persisted.beginUpkeep(3), "upkeep did not persist");
            loaded.beginCollapse();
            check(loaded.outposts().size() == 2 && loaded.state()==ColonyState.COLLAPSING, "abandoned shelters should remain recorded");
        } finally { Files.deleteIfExists(file); }
        System.out.println("Room layout, accessibility, outpost network and upkeep persistence passed.");
    }
}
