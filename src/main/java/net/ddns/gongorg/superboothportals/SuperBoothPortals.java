package net.ddns.gongorg.superboothportals;

import java.util.*;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

public class SuperBoothPortals extends org.bukkit.plugin.java.JavaPlugin {

    /**
     * Logging component.
     */
    public Logger log;
    private String pluginName;
    private static final String permissionNode = "superboothportals.";

    private String pluginVersion;
    private ResourceBundle i18nResource;

    int maxBoothRadius = 5;
    private Material boothMaterial = Material.REDSTONE_BLOCK;
    TreeMap<String,Portal> portals = new TreeMap<String,Portal>();
    Storage storage;

    private boolean suspended = false;

    public void onLoad() {
        org.bukkit.plugin.PluginDescriptionFile pdfFile = this.getDescription();
        pluginName = pdfFile.getName();
        pluginVersion = pdfFile.getVersion();
    }

    void configurePlugin() {
        FileConfiguration conf = getConfig();
        boolean debugMode = conf.getBoolean("debug", false);
        log = new Logger(pluginName + " v" + pluginVersion, debugMode);
        log.info("Debugging is set to " + debugMode);
        conf.get("locale_country", "UY");
        final String language = conf.getString("locale_language", "es");
        final String country = conf.getString("locale_country", "");
        log.info("Locale set to " + language + " " + country);
        final Locale locale = new Locale(language, country);
        i18nResource = ResourceBundle.getBundle("Messages", locale);
		maxBoothRadius = conf.getInt("max_booth_radius",5);
		String materialName = conf.getString("booth_material","QUARTZ_BLOCK");
		boothMaterial = Material.getMaterial(materialName);
		if (boothMaterial == null) {
		    boothMaterial = Material.QUARTZ_BLOCK;
		}
	log.debug("Booth material is " + boothMaterial);
    }

    public void onEnable() {
        configurePlugin();
        storage = new Storage(this);
        storage.loadCSV();
        log.info("Loaded " + portals.size() + " portals.");
        org.bukkit.plugin.PluginManager pm = getServer().getPluginManager();
        pm.registerEvents(new BlockListener(this), this);
        pm.registerEvents(new PlayerListener(this), this);
	final CommandExecutor ex = new CommandExecutor(this);
        getCommand("booth").setExecutor(ex);
        getCommand("booth").setTabCompleter(ex);
    }

    public void onDisable() {
        storage.saveCSV();
    }

    boolean isSuspended() {
        return suspended;
    }

    void suspendPortals() {
        suspended = true;
    }

    void resumePortals() {
        suspended = false;
    }

    boolean isDoor(Block b) {
        if (b == null)
            return false;
        Material m = b.getType();
        return (m == Material.DARK_OAK_DOOR) || (m == Material.WOOD_DOOR)
                || (m == Material.WOODEN_DOOR) || (m == Material.ACACIA_DOOR)
	    || (m == Material.SPRUCE_DOOR) || (m == Material.BIRCH_DOOR) || (m == Material.JUNGLE_DOOR);
    }

    boolean isBooth(Block b) {
        return b.getType() == boothMaterial;
    }

    Portal getPortalAt(org.bukkit.Location l) {
        for (Portal p: portals.values()) {
            if (p.isInterior(l)) { 
		log.debug("Inside " + p);
		return p;
	    }
        }
        return null;
    }

    Portal getPortal(String name) {
        if (portals.containsKey(name)) {
            return portals.get(name);
        } else {
            return null;
        }
    }
    
    Location getDestination(String name) {
        Portal destPortal = getPortal(name);
        return destPortal != null ? 
                destPortal.getSourceLocation() : null;
    }
    
    boolean hasPermission(Player player, String permission) {
        return player.hasPermission(permissionNode + permission);
    }

    void addPortal(Portal p) {
	String text = i18nResource.getString("added_portal");
        org.bukkit.Bukkit.broadcastMessage(ChatColor.YELLOW  + text + " " + p.getName());
        this.portals.put(p.getName(),p);
    }

    void removePortal(String name) {
        log.info("Removing portal " + name);
	String text = i18nResource.getString("removed_portal");
        org.bukkit.Bukkit.broadcastMessage(ChatColor.RED + text + " " + name);
        portals.remove(name);
    }

    String listPortals() {
	String text = i18nResource.getString("list_portals");
        StringBuffer s = new StringBuffer(text);
	text = i18nResource.getString("portals");
	s.append(" ").append(portals.size()).append(text).append('\n');
        java.util.Iterator<Portal> it = portals.values().iterator();
        while (it.hasNext()) {
            s.append("-").append(it.next()).append('\n');
        }
        return s.toString();
    }
}
