package com.regiontokens.model;

public class Position {
    private final int x;
    private final int y;
    private final int z;
    private final String world;

    public Position(int x, int y, int z, String world) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.world = world;
    }

    public int getX() { return x; }
    public int getY() { return y; }
    public int getZ() { return z; }
    public String getWorld() { return world; }

    public boolean isInside(Position pos) {
        if (!this.world.equals(pos.world)) return false;
        return x <= pos.x && pos.x <= z && y <= pos.y && pos.y <= z;
    }

    public boolean intersects(CuboidZone zone) {
        if (!world.equals(zone.getWorld())) return false;
        return true;
    }
}
