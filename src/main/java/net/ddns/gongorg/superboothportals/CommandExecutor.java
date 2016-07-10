package net.ddns.gongorg.superboothportals;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.ChatColor;
import java.util.List;
import java.util.ArrayList;

/**
 * Handle events for all Player related events
 * 
 * @author You
 */
public class CommandExecutor implements org.bukkit.command.CommandExecutor, org.bukkit.command.TabCompleter {
    private final SuperBoothPortals plugin;

    public CommandExecutor(SuperBoothPortals instance) {
        plugin = instance;
    }

    /** MUST return boolean */
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label,
            String[] args) {
        String basename = cmd.getName();
        if (basename.equalsIgnoreCase("booth")) {
            if (args.length == 0) {
                printHelp();
                return true;
            }
            String subcmd = args[0];
            if (subcmd.equalsIgnoreCase("list")) {
                if (sender.hasPermission("boothportals.list")) {
                    String portalList = plugin.listPortals();
                    sender.sendMessage(ChatColor.GREEN + portalList);
                }
            } else if (subcmd.equalsIgnoreCase("save")) {
                if (sender.hasPermission("boothportals.save")) {
                    plugin.storage.saveCSV();
                    sender.sendMessage(ChatColor.GREEN
                            + "Saved all existing portals.");
                }
            } else if (subcmd.equalsIgnoreCase("backup")) {
                if (sender.hasPermission("boothportals.backup")) {
                    plugin.storage.backupCSV();
                    sender.sendMessage(ChatColor.GREEN + "Backup created.");
                }
            } else if (subcmd.equalsIgnoreCase("restore")) {
                if (sender.hasPermission("boothportals.restore")) {
                    plugin.storage.restoreCSV();
                    sender.sendMessage(ChatColor.GREEN + "Restored from backup.");
                }
            } else if (subcmd.equalsIgnoreCase("reload")) {
                if (sender.hasPermission("boothportals.reload")) {
                    plugin.storage.loadCSV();
                    sender.sendMessage(ChatColor.GREEN + "Portals reloaded. ");
                }
            } else if (subcmd.equalsIgnoreCase("suspend")) {
                if (sender.hasPermission("boothportals.suspend")) {
                    sender.sendMessage(ChatColor.GREEN + "Portals suspended. ");
                    plugin.suspendPortals();
                }
            } else if (subcmd.equalsIgnoreCase("resume")) {
                if (sender.hasPermission("boothportals.resume")) {
                    plugin.resumePortals();
                    sender.sendMessage(ChatColor.GREEN + "Portals resumed. ");
                }
	    }	else if (subcmd.equalsIgnoreCase("tp")) {
                if (sender.hasPermission("boothportals.tp")) {
                    if (args.length >= 3) { 
			String playerName = args[1];
			StringBuffer sb = new StringBuffer();
			for (int i = 2; i < args.length; i++) {
			    sb.append(args[i]);
			    if (i < (args.length-1)) {
				sb.append(' ');
			    }
			}
			/*
			if ( portalName.charAt(0) == '\"') { // detect multi word argument
			    StringBuffer pnb = new StringBuffer(portalName.substring(1));
			    int i = 3;
			    while ((i < args.length) && (args[i].charAt(args[i].length()-1) != '\"')) {
				pnb.append(' ').append(args[i]);
			    }  
			    if (i < args.length) {
				pnb.append(' ').append(args[i].substring(0,args[i].length()-1));
			    }
			    portalName = pnb.toString();
			}
			*/
			String portalName = sb.toString();
			Player player = plugin.getServer().getPlayer(playerName);
			if (player == null) {
			    sender.sendMessage(ChatColor.RED + "Player " + playerName + " not found.");
			    return true;
			}
			Portal portal = plugin.getPortal(portalName);
			if (portal == null) {
			    sender.sendMessage(ChatColor.RED + "Portal " + portalName + " not found.");
			    return true;
			}
			sender.sendMessage(ChatColor.GREEN + "Teleporting user " + playerName  + " to portal " + portalName);
			player.teleport(portal.getSourceLocation());
		    } else {
			sender.sendMessage(ChatColor.RED + "Wrong command syntax. See help.");
		    }
                }
            } else {
                plugin.log
		    .info("Unknown BoothPortals command. Type booth <enter> for a list.");
	    }
            return true;
        } else {
            return false;
        }
    }

    /**
     * @todo Return a list of possible destinations for the command "booth tp user dest" 
     */
    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
	plugin.log.debug("Tab complete. label=" + label + "args.length=" + args.length + " args[0] = " + args[0]);
	List<String> sug =  new ArrayList<String>();
	if (!label.equalsIgnoreCase("booth")) {
	    return sug;
	}	
	if ((args.length < 2) || (args.length > 4)) {
	    return sug;
	}
	if (!args[0].equalsIgnoreCase("tp")) {
	    return sug;
	}
	// user didn't bother to give hint, we return all possible destinations
	if (args.length == 2 || (args[2].length() == 0)) {
	    sug.addAll(plugin.portals.keySet());
	    return sug;
	}
	//ok, we've got booth tp someuser xxx<TAB>
	String hint = args[2];
	for (String key: plugin.portals.keySet()) {
	    if (key.startsWith(hint)) sug.add(key);
	}
	return sug;
    }

    private void printHelp() {
        StringBuffer sb = new StringBuffer("BoothPortals usage:\n");
        sb.append("\tbooth [command [args]]\n");
        sb.append("Where command can be: list, reload, save, backup, restore, disable, enable, tp.");
        plugin.log.info(sb.toString());
    }
}
