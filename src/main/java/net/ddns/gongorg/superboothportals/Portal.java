package net.ddns.gongorg.superboothportals;

import org.bukkit.Location;

public final class Portal implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    private boolean enabled = true;
    final Location sourceLocation;
    final Location doorLocation;
    final int radius;
    final String name;
    String destinationName = null;
    
    Portal(String name, Location src, Location door) {
        this.sourceLocation = src;
        this.doorLocation = door;
        this.name = name;
	this.radius = (int) distXZ(doorLocation);
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

    private static String printXYZ(Location l) {
        return (l == null) ? "nowhere" : "(" + l.getX() + ", " + l.getY()
                + ", " + l.getZ() + ")";
    }

    final double distXZ(Location l) {
	final double ax = Math.abs(l.getX() - sourceLocation.getX());
	final double az = Math.abs(l.getZ() - sourceLocation.getZ());
	return ax > az ? ax : az;
    }

    public String toString() {
        return "Portal at " + printXYZ(sourceLocation) + ", radius " + getRadius() + ", from " + name + " to " + destinationName ;

    }

    Location getSourceLocation() {
        return sourceLocation.clone();
    }

    String getName() {
        return name;
    }
    
    Location getDoorLocation() {
        return doorLocation.clone();
    }

    int getRadius() { return radius; }

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
        final int y = l.getBlockY();
        final int cy = this.sourceLocation.getBlockY();
        if ((y < cy) || (y - cy > 2))
            return false; // too high or low
	return distXZ(l) == radius;
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
        final int y = l.getBlockY();
        return  (y >= sourceLocation.getBlockY()) && (y <= (sourceLocation.getBlockY() + 2)) &&
	    distXZ(l) < radius;
    }

}
