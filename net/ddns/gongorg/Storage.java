package net.ddns.gongorg;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import org.bukkit.Location;

final class Storage {

    private final BoothPortals plugin;
    private final String dataFile;
    private final String backupFile;

    Storage(BoothPortals plugin) {
        this.plugin = plugin;
        File dataFolder = plugin.getDataFolder();
        if (!dataFolder.exists()) {
            try {
                Files.createDirectory(Paths.get(dataFolder.getPath()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        dataFile = plugin.getDataFolder() + "/portals.txt";
        backupFile = plugin.getDataFolder() + "/portals.bak";
    }

    void save() {
        try {
            ObjectOutputStream out = new ObjectOutputStream(
                    new FileOutputStream(dataFile));
            out.writeObject(plugin.portals);
            out.close();
        } catch (IOException e) {
            plugin.log.severe("Failed to save portals: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    void load() {
        try {
            ObjectInputStream in = new ObjectInputStream(new FileInputStream(
                    dataFile));
            plugin.portals = (TreeMap<String,Portal>) in.readObject();
            in.close();
        } catch (ClassNotFoundException cnfe) {
            plugin.log.severe("Class not found? This is weird.");
        } catch (IOException e) {
            plugin.log.severe("Failed to load portals: " + e.getMessage());
        }
    }

    void saveCSV() {
        saveCSV(dataFile);
    }

    void backupCSV() {
        saveCSV(backupFile);
    }

    /*
     * srcworld,srcx, srcy, srcz, doorx, doory, doorz, destworld, destx, desty,
     * destz, enabled
     */
    void saveCSV(String fname) {
        plugin.log.debug("Saving " + plugin.portals.size() + " portals.");
        try {
            PrintWriter out = new PrintWriter(new FileOutputStream(fname));
            for (String n : plugin.portals.keySet()) {
                Portal p = plugin.portals.get(n);
                Location src = p.getSourceLocation();
                Location door = p.getDoorLocation();
                String dest = p.hasDestination()? p.getDestinationName() : "null";
                StringBuffer line = new StringBuffer(n);
                line.append(',');
                line.append(src.getWorld().getUID()).append(',');
                line.append(src.getX()).append(',').append(src.getY())
                        .append(',').append(src.getZ()).append(',');
                line.append(door.getX()).append(',').append(door.getY())
                        .append(',').append(door.getZ()).append(',');
                line.append(dest).append(',');
                line.append(p.isEnabled());
                out.println(line);
            }
            out.close();
        } catch (IOException e) {
            plugin.log.severe("Failed to save portals: " + e.getMessage());
        }
    }

    void loadCSV() {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(
                    new FileInputStream(dataFile)));
            String line;
            while ((line = in.readLine()) != null) {
                Location src;
                Location door;
                String name;

                StringTokenizer tokenizer = new StringTokenizer(line, ", ");
                String tok;
                java.util.UUID w;
                double x, y, z;
                boolean enabled;

                tok = tokenizer.nextToken();
                name = tok;
                tok = tokenizer.nextToken();
                w = java.util.UUID.fromString(tok);
                tok = tokenizer.nextToken();
                x = Double.parseDouble(tok);
                tok = tokenizer.nextToken();
                y = Double.parseDouble(tok);
                tok = tokenizer.nextToken();
                z = Double.parseDouble(tok);
                src = new Location(plugin.getServer().getWorld(w), x, y, z);

                tok = tokenizer.nextToken();
                x = Double.parseDouble(tok);
                tok = tokenizer.nextToken();
                y = Double.parseDouble(tok);
                tok = tokenizer.nextToken();
                z = Double.parseDouble(tok);
                door = new Location(plugin.getServer().getWorld(w), x, y, z);

                Portal p = new Portal(name, src, door);

                tok = tokenizer.nextToken();
                if (tok.equals("null")) {
                    enabled = false;
                } else {
                    p.setDestinationName(tok);
                    enabled = true;
                }
                if (enabled)
                    p.enable();
                plugin.log.debug("Adding portal " + p);
                plugin.portals.put(name, p);
            }
            in.close();
        } catch (NumberFormatException nfe) {
            plugin.log.severe("Portals file format is wrong: " + nfe);
        } catch (java.util.NoSuchElementException nse) {
            plugin.log.severe("Portals file format is wrong: " + nse);
        } catch (IOException e) {
            plugin.log.severe("Failed to load portals: " + e.getMessage());
        }
    }

}
