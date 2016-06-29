package net.ddns.gongorg.superboothportals;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.ChatColor;

/**
 * Handle events for all Player related events
 * 
 * @author You
 */
public class CommandExecutor implements org.bukkit.command.CommandExecutor {
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
                    if (args.length == 3) { 
			String playerName = args[1];
			String portalName = args[2];
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

    private void printHelp() {
        StringBuffer sb = new StringBuffer("BoothPortals usage:\n");
        sb.append("\tbooth [command [args]]\n");
        sb.append("Where command can be: list, load, save, backup, disable, enable, tp.");
        plugin.log.info(sb.toString());
    }
}
