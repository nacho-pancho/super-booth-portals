package net.ddns.gongorg.superboothportals;

import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.block.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.util.Vector;
import org.bukkit.Material;
import org.bukkit.Chunk;
import java.util.List;

/**
 * Handle events for all Player related events
 * 
 * @author Ignacio Ramirez <nacho@fing.edu.uy>
 */
public class PlayerListener implements Listener {
    private final SuperBoothPortals plugin;

    /**
     * Constructor.
     * 
     * @param instance
     *            The plugin to attach to.
     */
    public PlayerListener(SuperBoothPortals instance) {
        plugin = instance;
    }

    /**
     * The elegant way would be to trigger custom events (Doorevent, SignEvent,
     * whatever) and use the Bukkit mechanism, but I'm a little lazy for that.
     */
    @EventHandler
    public void onEvent(PlayerInteractEvent event) {
        if (event.isCancelled()) {
            return;
        }
        Block block = event.getClickedBlock();
        Material m = block.getType();
        if (plugin.isDoor(block)) {
            handleDoorEvent(event);
        } else if (m == Material.WALL_SIGN) {
            handleSignEvent(event);
        } else if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            handleBreakEvent(event);
        }
    }

    private void handleDoorEvent(PlayerInteractEvent event) {
        //plugin.log.debug("door event");
        if (plugin.isSuspended()) {
            return;
        }

        Player player = event.getPlayer();
        Block block = event.getClickedBlock();

        BlockState doorState = block.getState();
        if (!doorState.isPlaced()) {
            return;
        }

        org.bukkit.material.MaterialData data = doorState.getData();
        if (!(data instanceof org.bukkit.material.Door)) {
            //plugin.log.debug("Not a door??");
	    return;
        }
        org.bukkit.material.Door doorData = (org.bukkit.material.Door) data;
        if (doorData.isTopHalf()) {
            // isOpen is undefined for the top half, so we need to access the
            // bottom half first
            block = block.getRelative(org.bukkit.block.BlockFace.DOWN);
            doorState = block.getState();
            doorData = (org.bukkit.material.Door) doorState.getData();
        }
        if (!doorData.isOpen()) {
            return;
        }
        //
        // 1. check whether player is inside a booth
        //
        Location location = player.getLocation();
        org.bukkit.World world = location.getWorld();
	Portal srcPortal = plugin.getPortalAt(location);
        if (srcPortal == null) {
            //plugin.log.debug("Not within a portal.");
            return;
        }
        
        // check if it is enabled
        if (!srcPortal.isEnabled()) { 
            //plugin.log.debug("Portal not enabled.");
            return; 
        }

        //
        // * determine warp destination; check whether it is empty (must!)
        //
	Portal destPortal = plugin.getPortal(srcPortal.getDestinationName());
        Location dest = destPortal.getSourceLocation();
	plugin.log.debugLoc("Portal dest ",dest);
        if (dest == null) {
            plugin.log.debug("Portal leads nowhere");
            return;
        }
	
        plugin.log.info("Teleporting player to \"" + srcPortal.getDestinationName() + "\":" + dest);
        // player.setFlying(true); // to avoid falling down to unloaded chunk
        // pieces!
        final Chunk chunk = dest.getChunk();
        if (!chunk.isLoaded()) {
            chunk.load();
        }
	// see who is inside the booth. The one who closes the door must still be inside the booth,
	// so the distance from the user to another entity within the booth is at most twice the radius
	// of the booth
	int radius = srcPortal.getRadius()*2;
	List<Entity> nearbyEntities = player.getNearbyEntities(radius,1,radius);
	Location srcLoc = srcPortal.getSourceLocation();
	plugin.log.debug("Source at " + srcLoc);
	plugin.log.debug("Found " + nearbyEntities.size() + " entities near the player.");
	final double scale = (double) destPortal.getRadius() /(double) srcPortal.getRadius();
        try {
            while (!chunk.isLoaded()) {
                player.setVelocity(new Vector(0, 0, 0));
                Thread.sleep(200);
            }
	    for (Entity e: nearbyEntities) {
		Location eloc = e.getLocation();
		if (srcPortal.isInterior(eloc)) {
		    // add identical offset
		    plugin.log.debug("Teleporting entity");
		    plugin.log.debugLoc("From ", eloc);
		    Location offset = eloc.subtract(srcLoc);
		    plugin.log.debugLoc("Offset ",offset);
		    Location edest = dest.clone();
		    if (scale < 1.0) { 
			offset.setX(offset.getX()*scale);
			offset.setY(offset.getY()*scale);
		    } 
		    offset.setWorld(edest.getWorld());
		    edest.add(offset);
		    plugin.log.debugLoc("To ",edest);
		    e.teleport(edest);
		}
	    }
	    // shrink relative locations if destination portal is smaller
	    Location ploc = player.getLocation();
	    plugin.log.debug("Teleporting player");
	    plugin.log.debugLoc("From ", ploc);
	    Location offset = ploc.subtract(srcLoc);
	    if (scale < 1.0) { 
		offset.setX(offset.getX()*scale);
		offset.setY(offset.getY()*scale);
	    } 
	    plugin.log.debugLoc("Offset ",offset);
	    offset.setWorld(dest.getWorld());
	    dest.add(offset);
	    plugin.log.debugLoc("To ", dest);
            player.teleport(dest);
	    //
	    // thunder effect!
	    //	    
	    //dest.getWorld().strikeLightningEffect(srcLoc);
	    dest.getWorld().playEffect(srcLoc,org.bukkit.Effect.VILLAGER_PLANT_GROW,2);
	    //dest.getWorld().strikeLightningEffect(dest);
	    dest.getWorld().playEffect(dest,org.bukkit.Effect.VILLAGER_PLANT_GROW,2);
        } catch (InterruptedException ex) {
            plugin.log.info("Interrupted while sleeping.");
        }
    }

    /**
     * Portals can only be removed by breaking the door. While a portal is
     * active, all other blocks are not modifyable. When the door is broke, the
     * portal is automatically removed and all other portals that point to it
     * are disabled.
     */
    private void handleBreakEvent(PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        Location bl = block.getLocation();
        //
        // if it is another part of an active portal,
        // disallow this
        //
        for (Portal p : plugin.portals.values()) {
            if (p.isBoothBlock(bl)) {
                plugin.log.debug("Can't touch this!");
                event.setCancelled(true);
                return;
            }
        }
    }

    /**
     * Clicking on an inner portal sign rotates the portal's destination.
     * Clicking on an outer portal sign does nothing.
     */
    private void handleSignEvent(PlayerInteractEvent event) {
        Block block = event.getClickedBlock();
        Location location = block.getLocation().add(0.0,-1.0,0.0);
        Portal srcPortal = plugin.getPortalAt(location);
        // check whether it is an inner booth sign
        if (srcPortal  == null) return;
        // this is safe: we checked for a sign here earlier
        Sign sign = (Sign) block.getState();
        // get current key from map
        String currDestName = sign.getLine(1).trim();
	
        plugin.log.debug("Current portal destination is \""  + currDestName + "\"");
        String newDest = currDestName;
        Action action = event.getAction();
	int srcRadius = srcPortal.getRadius();
        if (action == Action.RIGHT_CLICK_BLOCK) { // go one destination up
	    newDest = plugin.portals.higherKey(newDest);
	    if (newDest == null) {
		newDest = plugin.portals.firstKey();
	    }
	    Portal cand = plugin.portals.get(newDest);
        } else if (action == Action.LEFT_CLICK_BLOCK) {
            event.setCancelled(true); // we don't want to break the sign
	    newDest = plugin.portals.lowerKey(newDest);
	    if (newDest == null) {
		newDest = plugin.portals.lastKey();
	    }
        }
        if (newDest != null) {
            plugin.log.debug("Change destination to \"" + newDest + "\"");
            sign.setLine(0, "Portal to");
            sign.setLine(1, newDest == null ? "NOWHERE" : newDest);
            sign.setLine(2, "");
            boolean res = sign.update(true);
	    if (res == false) { 
		plugin.log.debug("Failed to update sign text!"); 
	    }
	}
	srcPortal.setDestinationName(newDest);
    }
}
