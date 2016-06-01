package net.ddns.gongorg;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.block.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.util.Vector;
import org.bukkit.Material;
import org.bukkit.Chunk;

/**
 * Handle events for all Player related events
 * 
 * @author Ignacio Ramirez <nacho@fing.edu.uy>
 */
public class PlayerListener implements Listener {
    private final BoothPortals plugin;

    /**
     * Constructor.
     * 
     * @param instance
     *            The plugin to attach to.
     */
    public PlayerListener(BoothPortals instance) {
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
        //plugin.log.debug("onEvent");
        Block block = event.getClickedBlock();
        Material m = block.getType();
        //plugin.log.debug("Type: " + m.getClass().getName() + ":" + m);
        if (plugin.isDoor(block)) {
            handleDoorEvent(event);
        } else if (m == Material.WALL_SIGN) {
            handleSignEvent(event);
        } else if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            handleBreakEvent(event);
        }
    }

    private void handleDoorEvent(PlayerInteractEvent event) {
        plugin.log.debug("door event");
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
            // plugin.log.debug("Not a door??");
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
            // plugin.log.debug("Door needs to be closed.");
            return;
        }
        //
        // 1. check whether player is inside a booth
        //
        Location location = player.getLocation();
        org.bukkit.World world = location.getWorld();
        Portal srcPortal = plugin.getPortalAt(location);
        if (srcPortal == null) {
            plugin.log.debug("Not a portal door.");
            return;
        }
        
        // check if it is enabled
        if (!srcPortal.isEnabled()) { 
            plugin.log.debug("Portal not enabled.");
            return; 
        }

        //
        // * determine warp destination; check whether it is empty (must!)
        //
        Location dest = plugin.getDestination(srcPortal.getDestinationName());
        if (dest == null) {
            plugin.log.debug("Portal leads nowhere");
            return;
        }
        if (world.getBlockAt(dest).getType() != Material.AIR) {
            plugin.log.debug("Portal destination is not empty (." + world.getBlockAt(dest).getType() + ").");
            return;
        }
        //
        // safe teleportation. Tries to cope with Spigot
        // issues when teleporting (falling out onto unloaded blocks)
        //
        plugin.log.info("Teleporting player to \"" + srcPortal.getDestinationName() + "\":" + dest);
        // player.setFlying(true); // to avoid falling down to unloaded chunk
        // pieces!
        final Chunk chunk = dest.getChunk();
        if (!world.isChunkLoaded(chunk)) {
            chunk.load();
        }
        // player.setFlying(true);
        try {
            while (!world.isChunkLoaded(chunk)) {
                player.setVelocity(new Vector(0, 0, 0));
                Thread.sleep(200);
            }
            player.teleport(dest);
            Thread.sleep(200);
            // player.setFlying(false);
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
        plugin.log.debug("break event");
        Block block = event.getClickedBlock();
        Location bl = block.getLocation();
        //
        // check if we are breaking the door of a portal
        // in which case the portal is removed
        // we also notify the user of this
        //
        for (Portal p : plugin.portals.values()) {
            if (p.isDoorBlock(bl)) {
                plugin.removePortal(p.getName());
                return;
            }
        }
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
        plugin.log.debug("sign event");
        Block block = event.getClickedBlock();
        // sign must be 1 block above center of booth
        Location location = block.getLocation().add(0.0,-1.0,0.0);
        Portal srcPortal = plugin.getPortalAt(location);
        // check whether it is an inner booth sign
        if (srcPortal  == null) return;
        // this is safe: we checked for a sign here earlier
        Sign sign = (Sign) block.getState();
        // get current key from map
        String currDestName = sign.getLine(1).trim();
        plugin.log.debug("Current portal destination is \""  + currDestName + "\"");
        String newDest = null;
        Action action = event.getAction();
        if (action == Action.RIGHT_CLICK_BLOCK) { // go one destination up
            newDest = plugin.portals.higherKey(currDestName);
            if (newDest == null) {
                newDest = plugin.portals.firstKey();
            }
            if (newDest == null) { newDest = "NOWHERE"; }
        } else if (action == Action.LEFT_CLICK_BLOCK) {
            event.setCancelled(true); // we don't want to break the sign
            newDest = plugin.portals.lowerKey(currDestName);
            if (newDest == null) {
                newDest = plugin.portals.lastKey();
            }
            if (newDest == null) { newDest = "NOWHERE"; }
        }
        if (newDest != null) {
            plugin.log.debug("Change destination to \"" + newDest + "\"");
            sign.setLine(0, "Portal to");
            sign.setLine(1, newDest == null ? "NOWHERE" : newDest);
            sign.setLine(2, null);
            sign.update();
            }
            srcPortal.setDestinationName(newDest);
        }
}
