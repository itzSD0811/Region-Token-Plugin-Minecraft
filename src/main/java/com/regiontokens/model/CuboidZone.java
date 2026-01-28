package com.regiontokens.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CuboidZone {
    private final String id;
    private final String name;
    private final int x1, y1, z1;
    private final int x2, y2, z2;
    private final String world;
    private int kickX = 0;
    private int kickY = 64;
    private int kickZ = 0;
    private String kickWorld;
    private final List<String> assignedTokenIds = new ArrayList<>();

    public CuboidZone(String id, String name, int x1, int y1, int z1, int x2, int y2, int z2, String world, String kickWorld) {
        this.id = id;
        this.name = name;
        this.x1 = Math.min(x1, x2);
        this.y1 = Math.min(y1, y2);
        this.z1 = Math.min(z1, z2);
        this.x2 = Math.max(x1, x2);
        this.y2 = Math.max(y1, y2);
        this.z2 = Math.max(z1, z2);
        this.world = world;
        this.kickWorld = kickWorld;
    }

    public boolean contains(double x, double y, double z) {
        return x >= x1 && x <= x2 && y >= y1 && y <= y2 && z >= z1 && z <= z2;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getWorld() { return world; }
    public int getX1() { return x1; }
    public int getY1() { return y1; }
    public int getZ1() { return z1; }
    public int getX2() { return x2; }
    public int getY2() { return y2; }
    public int getZ2() { return z2; }
    public int getKickX() { return kickX; }
    public int getKickY() { return kickY; }
    public int getKickZ() { return kickZ; }
    public String getKickWorld() { return kickWorld; }

    public void setKickLocation(int x, int y, int z, String world) {
        this.kickX = x;
        this.kickY = y;
        this.kickZ = z;
        this.kickWorld = world;
    }

    public List<String> getAssignedTokenIds() {
        return new ArrayList<>(assignedTokenIds);
    }

    public void addAssignedToken(String tokenId) {
        if (tokenId != null && !tokenId.isEmpty() && !assignedTokenIds.contains(tokenId)) {
            assignedTokenIds.add(tokenId);
        }
    }

    public void removeAssignedToken(String tokenId) {
        assignedTokenIds.remove(tokenId);
    }

    public boolean isTokenAssigned(String tokenId) {
        return assignedTokenIds.contains(tokenId);
    }
}
