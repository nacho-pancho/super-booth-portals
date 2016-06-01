package net.ddns.gongorg;

//import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.block.BlockFace;
import org.bukkit.ChatColor;
import org.bukkit.Material;
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
        
        //
        // may be a portal
        //
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

	Portal newPortal = seekPortal(doorLoc,facing);
	if (newPortal != null) {
	    plugin.addPortal(newPortal);
	}
    }

    /** portal may be 3x3, 5x5 or 7x7 */
    private void seekPortal(Location doorLoc, BlockFace facing) {
        Block doorSign = null;
	// for moving in directions relative to where the door is facing  
	Vector back, left, right, front;
	Vector up = new Vector(0,1,0);
	Vector down = new Vector(0,-1,0);
        switch (facing) {
        case NORTH: // facing north, so booth is towards positive z
	    back = new Vector(0,0,1);
	    left = new Vector(-1,0,0);
            break;
        case SOUTH:
	    back = new Vector(0,0,-1);
	    left = new Vector(1,0,0);
            break;
	case EAST:
	    back = new Vector(-1,0,0);
	    left = new Vector(0,0, 1);
            break;
        case WEST:
	    back = new Vector(0,0,1);
	    left = new Vector(0,0,-1);
            break;
            default:
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
        // check for inner sign, the one that points to the destination
        // it must be facing to the same side as the other sign
        //
	int boothRadius = 1; 
	Location innerSignLoc = doorLoc.clone();
	innerSignLoc.add(up).add(back); // first potential location 
	while (world.getBlockAt(innerSignLoc).getType() != Material.WALL_SIGN) {
	    boothRadius++;
	    innerSignLoc.add(back).add(back); // size increases by 2
	    if (boothSize > BoothPlugin.MAX_BOOTH_SIZE) {
		return null; // no sign found within maximum allowed booth size
	    }	    
	}
        Block innerSign = world.getBlockAt(innerSignLoc);
        Sign signState = (Sign) innerSign.getState();
        plugin.log.debug("just to check, inner sign reads:" + String.join("\n", signState.getLines()));
	//
	// found sign and potential booth radius. Now scan for booth structure
	//
        Location l2 = innerSignLoc.clone().add(back);
	Location l1 = l1.clone().add(down);
	Location l3 = l1.clone().add(up);
	Location r1 = l1.clone();
	Location r2 = l2.clone();
	Location r3 = l3.clone();
	for (int i = 0; i <= boothRadius; i++) {
	    
	} 
	// check to the left
	
        //
        // Name is obtained from door sign, and it must be unique!
        //
        String signName = String.join(" ", ((Sign)doorSign.getState()).getLines()).trim();
        if (plugin.getPortal(signName) != null) {
            plugin.log.debug("There is another sign with the name " + signName);
            player.sendMessage(ChatColor.YELLOW + "sign_exists" + signName);
            return;
        }
        //
        // check for columns
        //
        if (    plugin.isBooth(world.getBlockAt(cx+1,cy  ,cz+1)) &&
                plugin.isBooth(world.getBlockAt(cx+1,cy  ,cz-1)) &&
                plugin.isBooth(world.getBlockAt(cx-1,cy  ,cz+1)) &&
                plugin.isBooth(world.getBlockAt(cx-1,cy  ,cz-1)) &&
                plugin.isBooth(world.getBlockAt(cx+1,cy+1,cz+1)) &&
                plugin.isBooth(world.getBlockAt(cx+1,cy+1,cz-1)) &&
                plugin.isBooth(world.getBlockAt(cx-1,cy+1,cz+1)) &&
                plugin.isBooth(world.getBlockAt(cx-1,cy+1,cz-1))) {
            plugin.log.debug("Congratulations! It is a portal!");
            double tmp = 0.03125; // a little off centering so that so that roundings go towards correct place
	    Portal newPortal = new Portal(signName,
			     new Location(world, cx + 0.5 - tmp, cy+ tmp, cz + 0.5-tmp),
			     new Location(world, dx, dy, dz));
	    return newPortal;
        }
	return null;
    }

}
