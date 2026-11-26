package com.hivemind.core;

import java.util.*;
import com.hivemind.core.NestLayout.Position;
import com.hivemind.core.NestLayout.Material;

public final class RoyalChamberCheck {
    private static void require(boolean pass, String message) {
        if (!pass) throw new AssertionError(message);
    }
    public static void main(String[] args) {
        for (var nest : List.of(OrganicNest.SMALL, OrganicNest.FULL)) {
            int entrance = nest == OrganicNest.SMALL ? 3 : 13;
            Set<Position> reached = new HashSet<>();
            ArrayDeque<Position> queue = new ArrayDeque<>();
            queue.add(new Position(0,1,entrance));
            while (!queue.isEmpty()) {
                var p = queue.removeFirst();
                if (!reached.add(p)) continue;
                for (int[] d : new int[][]{{1,0},{-1,0},{0,1},{0,-1}}) {
                    var q = new Position(p.x()+d[0],1,p.z()+d[1]);
                    if (!reached.contains(q) && nest.at(q.x(),1,q.z()) == Material.AIR && nest.inside(q.x(),2,q.z())) queue.add(q);
                }
            }
            var royal = nest.throne;
            require(nest.at(royal.x(),0,royal.z())==Material.CORE, "misplaced core");
            require(reached.contains(new Position(royal.x(),1,royal.z())), "royal chamber unreachable");
            for (var cell : nest.plan) if (cell.material()==Material.BROOD_COMB) {
                var p=cell.position();
                require(reached.contains(new Position(p.x(),1,p.z())), "nurse cannot reach egg");
                require(Math.hypot(p.x()-royal.x(), p.z()-royal.z()) < 4, "egg too far from queen");
            }
            for(int startX=-1; startX<=1; startX++) {
                boolean blocked=false;
                for(int i=1;i<150;i++) {
                    double t=i/150.0;
                    int x=(int)Math.floor(startX+0.5+t*(royal.x()-startX));
                    int z=(int)Math.floor(entrance+0.5+t*(royal.z()-entrance));
                    if(nest.at(x,2,z)==Material.CHITIN) blocked=true;
                }
                require(blocked, "queen visible from entrance at "+startX+" for door "+entrance);
            }
            for(int x=-1;x<=1;x++) for(int z=-1;z<=1;z++) for(int y=1;y<=3;y++)
                require(nest.at(royal.x()+x,y,royal.z()+z)==Material.AIR, "queen spawn box clipped");
        }
        System.out.println("Passages, screened royal chambers, nurse access and queen clearance passed.");
    }
}
