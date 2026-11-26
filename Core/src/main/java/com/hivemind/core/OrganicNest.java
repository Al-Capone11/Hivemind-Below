package com.hivemind.core;

import java.util.*;
import static com.hivemind.core.NestLayout.Material;
import static com.hivemind.core.NestLayout.Position;
import static com.hivemind.core.NestLayout.Placement;

/** Each column is the union of the free volumes of the chambers and galleries. */
public final class OrganicNest {
    private static final int[][] SIDES = {{1,0},{-1,0},{0,1},{0,-1}};
    public static final OrganicNest FULL = new OrganicNest(false);
    public static final OrganicNest SMALL = new OrganicNest(true);
    public final Position throne, nursery, rest, gate;
    public final List<Placement> plan, repairs;
    private final Map<Position, Material> cells;
    private final Map<Position, Integer> heights = new LinkedHashMap<>();
    private final boolean compact;
    public final int extent, height;

    private OrganicNest(boolean compact) {
        this.compact = compact;
        throne = compact ? new Position(-7,0,-2) : new Position(-9,0,-8);
        nursery = compact ? new Position(-8,0,-1) : new Position(-11,0,-7);
        rest = new Position(-9,0,1);
        gate = new Position(0,0,9);
        if (compact) {
            lobe(-7,-2,3.5,2.8,0.45,4);
            lobe(0,0,2.8,2.7,-0.2,4);
            gallery(0,0, 1,-4, -2,-6, -4,-5);
            gallery(-4,-5, -5,-5, -7,-4, -7,-2);
            gallery(2,0, 1,0, 0,1, 0,2);
        } else {
            lobe(-9,-8,5.6,4.1,-0.34,7);
            lobe(-11,-6.5,2.6,2.3,0.2,5);
            lobe(-9,1,4.4,3.2,-0.38,5);
            lobe(9,0,4.3,3.8,0.52,6);
            lobe(7,2.5,2.6,2.9,0.5,4);
            lobe(-1,8.7,4.8,3.8,-0.6,5);
            lobe(2,-1,3.8,4.6,-0.55,6);
            gallery(0,12, -1,11, -2,10, -2,8);
            gallery(-2,8, -7,5, 0,4, 2,0);
            gallery(-1,8, -5,6, -6,2, -9,1);
            gallery(2,0, 4,-4, 8,-3, 10,0);
            gallery(2,0, 4,-5, 6,-9, 2,-10);
            gallery(2,-10, 0,-11, 0,-11, 0,-12);
            gallery(3,-4, 1,-8, -2,-4, -5,-6);
            gallery(-5,-6, -6,-8, -8,-8, -9,-8);
            gallery(-9,1, -10,1, -11,0, -12,0);
            gallery(10,0, 11,0, 11,0, 12,0);
        }
        Map<Position, Material> result = new LinkedHashMap<>();
        for (var entry : heights.entrySet()) {
            int x=entry.getKey().x(), z=entry.getKey().z(), top=entry.getValue();
            result.put(new Position(x,0,z), Material.CHITIN);
            result.put(new Position(x,top+1,z), Material.CHITIN);
            for (int y=1; y<=top; y++) {
                result.put(new Position(x,y,z), Material.AIR);
                for (int[] d : SIDES) {
                    if (heights.getOrDefault(new Position(x+d[0],0,z+d[1]),0) < y)
                        result.put(new Position(x+d[0],y,z+d[1]), Material.CHITIN);
                }
            }
        }
        int door = compact ? 3 : 13;
        for (int x=-1;x<=1;x++) for (int z=door;z<=door+(compact?0:2);z++) {
            result.put(new Position(x,0,z), Material.CHITIN);
            for (int y=1;y<=3;y++) result.put(new Position(x,y,z), Material.AIR);
        }
        if (compact) {
            for(int y=1;y<=2;y++) result.put(new Position(3,y,0),Material.AIR);
        } else {
            for(int x=-1;x<=1;x++) {
                result.put(new Position(x,0,11),Material.ALARM_RIDGE);
                for(int z=-1;z<=1;z++) if(x!=0) result.put(new Position(rest.x()+x,0,rest.z()+z),Material.MOLT_BED);
            }
            result.put(new Position(10,1,0),Material.STORAGE);
            for(int x : new int[]{8,10}) for(int z : new int[]{-2,2}) {
                Position column = new Position(x,0,z);
                result.put(new Position(x,heights.get(column),z),Material.RESERVE_CYST);
            }
        }
        for(int dx=-1;dx<=1;dx++) for(int dz=-1;dz<=1;dz++) if(Math.abs(dx)+Math.abs(dz)<=1) {
            var p=new Position(nursery.x()+dx,0,nursery.z()+dz);
            if(heights.containsKey(p)) result.put(p,Material.BROOD_COMB);
        }
        result.put(throne,Material.CORE);
        cells=Map.copyOf(result);
        plan=result.entrySet().stream().map(e->new Placement(e.getKey(),e.getValue())).toList();
        repairs=plan.stream().filter(p->p.material()!=Material.AIR && p.material()!=Material.CORE && p.material()!=Material.STORAGE)
                .sorted(Comparator.comparingInt(p->p.position().y())).toList();
        extent=plan.stream().mapToInt(p->Math.max(Math.abs(p.position().x()),Math.abs(p.position().z()))).max().orElseThrow();
        height=plan.stream().mapToInt(p->p.position().y()).max().orElseThrow();
    }

    private void lobe(double cx,double cz,double rx,double rz,double turn,int crown) {
        for(int x=-17;x<=16;x++) for(int z=-16;z<=14;z++) {
            double dx=x-cx,dz=z-cz;
            double u=(dx*Math.cos(turn)+dz*Math.sin(turn))/rx, v=(dz*Math.cos(turn)-dx*Math.sin(turn))/rz;
            double d=u*u+v*v;
            if(d<=1) column(x,z,3+(int)Math.round((crown-3)*Math.sqrt(1-d)));
        }
    }

    private void gallery(double x0,double z0,double x1,double z1,double x2,double z2,double x3,double z3) {
        for(int i=0;i<=32;i++) {
            double t=i/32.0,s=1-t,x=s*s*s*x0+3*s*s*t*x1+3*s*t*t*x2+t*t*t*x3,
                    z=s*s*s*z0+3*s*s*t*z1+3*s*t*t*z2+t*t*t*z3;
            for(int a=(int)Math.floor(x)-2;a<=x+2;a++) for(int b=(int)Math.floor(z)-2;b<=z+2;b++)
                if(Math.hypot(a-x,b-z)<=1.85) column(a,b,3);
        }
    }

    private void column(int x,int z,int airHeight) {
        int limit=compact?2:12;
        if(z>limit || x>limit || !compact && (Math.abs(z)<=2 && x < -12 || Math.abs(x)<=2 && z < -12)) return;
        heights.merge(new Position(x,0,z), airHeight, Math::max);
    }

    public static OrganicNest of(Colony colony) { return colony.hasSpecializedRooms()?FULL:SMALL; }
    public Material at(int x,int y,int z) { return cells.get(new Position(x,y,z)); }
    public boolean inside(int x,int y,int z) {
        var m=at(x,y,z);
        return m==Material.AIR || m==Material.STORAGE || m==Material.RESERVE_CYST;
    }
    public boolean intersects(double minX,double minY,double minZ,double maxX,double maxY,double maxZ) {
        for (int x=(int)Math.floor(minX); x<maxX; x++) for(int z=(int)Math.floor(minZ); z<maxZ; z++)
            for(int y=(int)Math.floor(minY); y<maxY; y++) if(at(x,y,z)!=null) return true;
        return false;
    }

    public boolean footprint(int x,int z) { return at(x,0,z)!=null || at(x,1,z)!=null; }
}
