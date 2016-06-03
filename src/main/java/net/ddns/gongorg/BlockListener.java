package net.ddns.gongorg;

//import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.BlockFace;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
//import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockPlaceEvent;
//import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.Location;
import org.bukkit.util.Vector;

/**
 * block listener
 * 
 * @author cppchriscpp
 */
public class BlockListener implements org.bukkit.event.Listener {
    private final BoothPortals plugin;


    /**
     * Constructor
     * 
     * @param plugin
     *            The plugin to attach to.
     */
    public BlockListener(final BoothPortals plugin) {
        this.plugin = plugin;
    }

    /**
     * 
     * @param event
     *            The event related to the block placement.
     * 
     *            A portal structure is identified by four things:
     *            1) a door
     *            2) a sign above the door
     *            3) a rectangular chamber which is entered through the door,  which must
     *            be made only of redstone blocks, have at least 3 blocks of height (without counting the roof)
     *            4) a sign, placed on the wall right behind the (closed) door
     *            Something like this:
     *             R | R | R 
     *            ---+-S-+--- 
     *             R |   | R 
     *            ---+---+--- 
     *             R | D | R
     * 
     *            Besides the four redstone columns, a sign must be placed on top of the door
     *            (which gives the unique name of the portal), and another one must be placed
     *            inside the portal  (where the S appears).
     * 
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onBlockPlace(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        Location doorLoc = block.getLocation();
        org.bukkit.World world = player.getWorld();

        if (event.isCancelled()) {
            return;
        }
        //
        // do not allow placing any blocks inside a portal
        //
        for (Portal p  : plugin.portals.values()) {
            if (p.isInterior(doorLoc)) {
                event.setCancelled(true);
                return;
            }
        }
        //
        // nothing else to check if object being placed is not a door
        //
        if (!plugin.isDoor(block))
            return;
        

	Portal newPortal = seekPortal(event);

	if (newPortal != null) {
	    plugin.addPortal(newPortal);
	}
    }

    private final void debugLoc(String prefix, Location l) {  // only for debugging
	java.text.DecimalFormat nf = new java.text.DecimalFormat("#####");
	StringBuffer sb = new StringBuffer(prefix);
	sb.append("=(");
	sb.append(nf.format(l.getX())).append(',');
	sb.append(nf.format(l.getY())).append(',');
	sb.append(nf.format(l.getY())).append(',');
	sb.append(')');
	plugin.log.debug(sb.toString());
    }

    /** portal may be 3x3, 5x5 or 7x7 */
    private Portal seekPortal(BlockPlaceEvent event) {
        Player player = event.getPlayer();
        Block block = event.getBlock();
        Location doorLoc = block.getLocation();
        World world = player.getWorld();

        // data unique to doors
        org.bukkit.material.Door doorData = (org.bukkit.material.Door) block.getState().getData(); 
        if (doorData.isTopHalf()) { // top half of door does not contain valid information
            doorData = (org.bukkit.material.Door) block.getRelative(0, -1, 0).getState().getData();
	    doorLoc.add(0,-1,0);
        }
        //
        // we are sure that (dx,dy,dz) corresponds to the lower block of the door
        //
        // 
        // now determine orientation of the portal
        // further checks are done depending on that
        //
        BlockFace facing =  doorData.getFacing();
        Block doorSign = null;
	// for moving in directions relative to where the door is facing  
	Vector back, left, right, front;
	Vector up = new Vector(0,1,0);
	Vector down = new Vector(0,-1,0);
	plugin.log.debug("Seeking portal facing " + facing);
        switch (facing) {
        case NORTH: // facing north, so booth is towards positive z, left is east (positive x)
	    back = new Vector(0,0,+1);
	    left = new Vector(+1,0,0);
            break;
        case SOUTH: // facing south, booth extends towards negative z, left is west (negative x)
	    back = new Vector(0,0,-1);
	    left = new Vector(-1,0,0);
            break;
	case EAST: // facing east, booth extends towards west, left is south (positive z)
	    back = new Vector(-1,0,0);
	    left = new Vector(0,0,+1);
            break;
        case WEST: // facing west, booth extends to the east, left is north (neg z)
	    back = new Vector(+1,0,0);
	    left = new Vector(0,0,-1);
            break;
            default:
		return null;
                // cannot be
        }
	right = left.clone();
	right.multiply(-1);
	front = back.clone();
	front.multiply(-1);

	Location doorSignLoc = doorLoc.clone();
	doorSignLoc.add(front).add(up).add(up);

	doorSign = world.getBlockAt(doorSignLoc);
        if (doorSign.getType() != Material.WALL_SIGN) {
            plugin.log.debug("No sign above door.");
            return null;
        }
        String signName = String.join(" ", ((Sign)doorSign.getState()).getLines()).trim();
        if (plugin.getPortal(signName) != null) {
            plugin.log.debug("There is another sign with the name " + signName);
            player.sendMessage(ChatColor.YELLOW + "sign_exists" + signName);
            return null;
        }
        // check for inner sign, the one that points to the destination
        // it must be facing to the same side as the other sign
        //
	int boothRadius = 1; 
	Location innerSignLoc = doorLoc.clone();
	innerSignLoc.add(up).add(back); // first potential location 
	while (world.getBlockAt(innerSignLoc).getType() != Material.WALL_SIGN) {
	    boothRadius++;
	    innerSignLoc.add(back).add(back); // size increases by 2
	    if (boothRadius > plugin.maxBoothRadius) {
		plugin.log.debug("No booth wall beyond maximum radius " + (boothRadius-1));
		return null; // no sign found within maximum allowed booth size
	    }
	}
	plugin.log.debug("Possible both with radius " + boothRadius);
        Block innerSign = world.getBlockAt(innerSignLoc);
        Sign signState = (Sign) innerSign.getState();
        plugin.log.debug("just to check, inner sign reads:" + String.join("\n", signState.getLines()));
	//
	// found sign and potential booth radius. Now scan for booth structure
	//
        Location l2 = innerSignLoc.clone().add(back);
	Location l1 = l2.clone().add(down);
	Location l3 = l2.clone().add(up);
	if (!plugin.isBooth(l1.getBlock()) ||
	    !plugin.isBooth(l2.getBlock()) ||
	    !plugin.isBooth(l3.getBlock())) {
	    debugLoc("No booth at l1=",l1);
	    return null;
	}
	Location r1 = l1.clone();
	Location r2 = l2.clone();
	Location r3 = l3.clone();
	// check back wall, determine radius
	int i;
	plugin.log.debug("Checking back wall starting at ");
	debugLoc(" l2=",l2);
	for (i = 1; i <= boothRadius; i++) {
	    l1.add(left); l2.add(left); l3.add(left);
	    r1.add(right); r2.add(right); r3.add(right);
	    if (!plugin.isBooth(l1.getBlock()) ||
		!plugin.isBooth(l2.getBlock()) ||
		!plugin.isBooth(l3.getBlock()) ||
		!plugin.isBooth(r1.getBlock()) ||
		!plugin.isBooth(r2.getBlock()) ||
		!plugin.isBooth(r3.getBlock())) {
		plugin.log.debug("i=" + i);
		debugLoc("No booth at l1=", l1);
		break;
	    }
	}
	if (i < boothRadius) {
	    return null; 
	}
	// check side walls
	// we are past the corner; go back one step
	for (i = 0; i <= (2*boothRadius); i++) {
	    l1.add(front); l2.add(front); l3.add(front);
	    r1.add(front); r2.add(front); r3.add(front);
	    if (!plugin.isBooth(l1.getBlock()) ||
		!plugin.isBooth(l2.getBlock()) ||
		!plugin.isBooth(l3.getBlock()) ||
		!plugin.isBooth(r1.getBlock()) ||
		!plugin.isBooth(r2.getBlock()) ||
		!plugin.isBooth(r3.getBlock())) {
		break;
	    }
	} 
	if (i < (2*boothRadius)) {
	    return null;
	}
	// check front wall
	for (i = 0; i <= (boothRadius-1); i++) {
	    l1.add(right); l2.add(right); l3.add(right);
	    r1.add(left); r2.add(left); r3.add(left);
	    if (!plugin.isBooth(l1.getBlock()) ||
		!plugin.isBooth(l2.getBlock()) ||
		!plugin.isBooth(l3.getBlock()) ||
		!plugin.isBooth(r1.getBlock()) ||
		!plugin.isBooth(r2.getBlock()) ||
		!plugin.isBooth(r3.getBlock())) {
		break;
	    }
	} 
	if (i < (boothRadius-1)) {
	    return null;
	}
	// 
	// we seem to have a complete wall for the booth
        //
	// now determine portal center
	//
	Location centerLoc = doorLoc.clone();
	back.multiply(boothRadius);
	centerLoc.add(back);
	plugin.log.debug("Portal walls check. Try adding it.");
	double tmp = 0.03125; // a little off centering so that so that roundings go towards correct place
	return new Portal(signName, centerLoc, doorLoc, boothRadius);
    }

}
