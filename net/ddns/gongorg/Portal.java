package net.ddns.gongorg;

import org.bukkit.Location;

public final class Portal implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    private boolean enabled = true;
    Location sourceLocation;
    Location doorLocation;
    String name;
    String destinationName = null;
    
    Portal(String name, Location src, Location door) {
        this.sourceLocation = src;
        this.doorLocation = door;
        this.name = name;
        this.destinationName = null;
    }

    public boolean equals(Object o) {
        if (!(o instanceof Portal))
            return false;
        final Portal p = (Portal) o;
        final double ax = this.sourceLocation.getX();
        final double ay = this.sourceLocation.getY();
        final double az = this.sourceLocation.getZ();

        final double bx = p.sourceLocation.getX();
        final double by = p.sourceLocation.getY();
        final double bz = p.sourceLocation.getZ();

        return Math.abs(ax - bx) < 1.0 && Math.abs(ay - by) < 1.0
                && Math.abs(az - bz) < 1.0;
    }

    String printXYZ(Location l) {
        return (l == null) ? "nowhere" : "(" + l.getX() + ", " + l.getY()
                + ", " + l.getZ() + ")";
    }

    public String toString() {
        return "Portal at " + printXYZ(sourceLocation) + ". goes from " + name + " to " + destinationName;

    }

    Location getSourceLocation() {
        return sourceLocation;
    }

    String getName() {
        return name;
    }
    
    Location getDoorLocation() {
        return doorLocation;
    }

    String getDestinationName() {
        return destinationName;
    }

    boolean hasDestination() {
        return destinationName != null;
    }

    void setDestinationName(String dest) {
        this.destinationName = dest;
    }

    boolean isEnabled() {
        return enabled;
    }

    void enable() {
        this.enabled = true;
    }

    void disable() {
        //this.enabled = false;
    }

    boolean isBoothBlock(Location l) {
        final int x = l.getBlockX();
        final int y = l.getBlockY();
        final int z = l.getBlockZ();
        final int cx = this.sourceLocation.getBlockX();
        final int cy = this.sourceLocation.getBlockY();
        final int cz = this.sourceLocation.getBlockZ();
        if ((y < cy) || (y - cy > 1))
            return false; // too high or low
        // if ((x == doorLocation.getBlockX()) && (z ==
        // doorLocation.getBlockZ())) return true; // it might be door
        final int dx = (int) Math.abs(x - cx);
        final int dz = (int) Math.abs(z - cz);
        if (dx >= 2 || dz >= 2)
            return false; // too far
        if (dx + dz <= 1)
            return false; // now only remaining option is 2 (a column)
        return true;
    }

    boolean isDoorBlock(Location l) {
        final int x = l.getBlockX();
        final int y = l.getBlockY();
        final int z = l.getBlockZ();
        return (x == doorLocation.getBlockX())
                && (z == doorLocation.getBlockZ())
                && (y >= doorLocation.getBlockY())
                && (y <= doorLocation.getBlockY() + 1);
    }

    boolean isInterior(Location l) {
        final int x = l.getBlockX();
        final int y = l.getBlockY();
        final int z = l.getBlockZ();
        return (x == sourceLocation.getBlockX()) && (z == sourceLocation.getBlockZ())
                && (y >= sourceLocation.getBlockY()) && (y <= (sourceLocation.getBlockY() + 1));
    }
}
