package com.regiontokens.manager;

import com.regiontokens.RegionTokensPlugin;
import com.regiontokens.model.CuboidZone;
import com.regiontokens.util.ConfigUtil;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class ZoneManager {
    private final RegionTokensPlugin plugin;
    private final Map<String, CuboidZone> zones = new HashMap<>();
    private int zoneIdCounter = 1;

    public ZoneManager(RegionTokensPlugin plugin) {
        this.plugin = plugin;
        loadZones();
    }

    private void loadZones() {
        File zonesDir = new File(plugin.getDataFolder(), "zones");
        if (!zonesDir.exists()) {
            zonesDir.mkdirs();
        }

        File[] files = zonesDir.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files != null) {
            for (File file : files) {
                try {
                    FileConfiguration config = YamlConfiguration.loadConfiguration(file);
                    String zoneName = config.getString("name", file.getName().replace(".yml", ""));
                    int x1 = config.getInt("x1");
                    int y1 = config.getInt("y1");
                    int z1 = config.getInt("z1");
                    int x2 = config.getInt("x2");
                    int y2 = config.getInt("y2");
                    int z2 = config.getInt("z2");
                    String world = config.getString("world", "world");
                    String kickWorld = config.getString("kick-world", world);
                    int kickX = config.getInt("kick-x", 0);
                    int kickY = config.getInt("kick-y", 64);
                    int kickZ = config.getInt("kick-z", 0);

                    String id = config.getString("id");
                    if (id == null) {
                        id = String.valueOf(zoneIdCounter++);
                        config.set("id", id);
                        config.save(file);
                    }

                    CuboidZone zone = new CuboidZone(id, zoneName, x1, y1, z1, x2, y2, z2, world, kickWorld);
                    zone.setKickLocation(kickX, kickY, kickZ, kickWorld);
                    
                    // Load assigned tokens
                    List<String> assignedTokenIds = config.getStringList("assigned-tokens");
                    for (String tokenId : assignedTokenIds) {
                        zone.addAssignedToken(tokenId);
                    }
                    
                    zones.put(id, zone);

                    if (Integer.parseInt(id) >= zoneIdCounter) {
                        zoneIdCounter = Integer.parseInt(id) + 1;
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to load zone from " + file.getName());
                    e.printStackTrace();
                }
            }
        }
    }

    public CuboidZone createZone(String name, int x1, int y1, int z1, int x2, int y2, int z2, String world) {
        String id = String.valueOf(zoneIdCounter++);
        CuboidZone zone = new CuboidZone(id, name, x1, y1, z1, x2, y2, z2, world, world);
        zones.put(id, zone);
        
        saveZone(zone);
        return zone;
    }

    public void saveZone(CuboidZone zone) {
        File zonesDir = new File(plugin.getDataFolder(), "zones");
        if (!zonesDir.exists()) {
            zonesDir.mkdirs();
        }

        File zoneFile = new File(zonesDir, zone.getId() + ".yml");
        FileConfiguration config = new YamlConfiguration();
        
        config.set("id", zone.getId());
        config.set("name", zone.getName());
        config.set("x1", zone.getX1());
        config.set("y1", zone.getY1());
        config.set("z1", zone.getZ1());
        config.set("x2", zone.getX2());
        config.set("y2", zone.getY2());
        config.set("z2", zone.getZ2());
        config.set("world", zone.getWorld());
        config.set("kick-x", zone.getKickX());
        config.set("kick-y", zone.getKickY());
        config.set("kick-z", zone.getKickZ());
        config.set("kick-world", zone.getKickWorld());
        config.set("assigned-tokens", zone.getAssignedTokenIds());

        try {
            config.save(zoneFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save zone: " + zone.getId());
            e.printStackTrace();
        }
    }

    public void deleteZone(String zoneId) {
        zones.remove(zoneId);
        File zonesDir = new File(plugin.getDataFolder(), "zones");
        File zoneFile = new File(zonesDir, zoneId + ".yml");
        if (zoneFile.exists()) {
            zoneFile.delete();
        }
    }

    public CuboidZone getZone(String zoneId) {
        return zones.get(zoneId);
    }

    public CuboidZone getZoneByName(String name) {
        for (CuboidZone zone : zones.values()) {
            if (zone.getName().equalsIgnoreCase(name)) {
                return zone;
            }
        }
        return null;
    }

    public Map<String, CuboidZone> getAllZones() {
        return new HashMap<>(zones);
    }

    public void renameZone(String zoneId, String newName) {
        CuboidZone zone = zones.get(zoneId);
        if (zone != null) {
            zones.put(zoneId, new CuboidZone(zone.getId(), newName, zone.getX1(), zone.getY1(), zone.getZ1(), 
                                             zone.getX2(), zone.getY2(), zone.getZ2(), zone.getWorld(), zone.getKickWorld()));
            zone = zones.get(zoneId);
            zone.setKickLocation(zone.getKickX(), zone.getKickY(), zone.getKickZ(), zone.getKickWorld());
            saveZone(zone);
        }
    }

    public void reloadZones() {
        zones.clear();
        zoneIdCounter = 1;
        loadZones();
    }
}
