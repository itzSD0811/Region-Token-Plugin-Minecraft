package com.regiontokens.manager;

import com.regiontokens.RegionTokensPlugin;
import com.regiontokens.model.TokenTemplate;
import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class TokenManager {
    private final RegionTokensPlugin plugin;
    private final Map<String, TokenTemplate> tokens = new HashMap<>();
    private int tokenIdCounter = 1;

    public TokenManager(RegionTokensPlugin plugin) {
        this.plugin = plugin;
        loadTokens();
    }

    private void loadTokens() {
        File tokensDir = new File(plugin.getDataFolder(), "tokens");
        if (!tokensDir.exists()) {
            tokensDir.mkdirs();
        }

        File[] files = tokensDir.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files != null) {
            for (File file : files) {
                try {
                    FileConfiguration config = YamlConfiguration.loadConfiguration(file);
                    String tokenName = config.getString("name", file.getName().replace(".yml", ""));
                    long duration = config.getLong("duration", 60);
                    String assignedZoneId = config.getString("assigned-zone", null);
                    String itemMaterial = config.getString("item-material", "PAPER");
                    boolean glowing = config.getBoolean("glowing", false);

                    String id = config.getString("id");
                    if (id == null) {
                        id = String.valueOf(tokenIdCounter++);
                        config.set("id", id);
                        config.save(file);
                    }

                    TokenTemplate token = new TokenTemplate(id, tokenName, duration, assignedZoneId, itemMaterial, glowing);
                    
                    // Load multiple assigned zones if they exist
                    List<String> assignedZones = config.getStringList("assigned-zones");
                    for (String zone : assignedZones) {
                        token.addAssignedZone(zone);
                    }
                    
                    // Also load single assigned zone for backward compatibility
                    if (assignedZoneId != null && !assignedZoneId.isEmpty() && !assignedZones.contains(assignedZoneId)) {
                        token.addAssignedZone(assignedZoneId);
                    }
                    
                    List<String> lore = config.getStringList("lore");
                    for (String line : lore) {
                        token.addLoreLine(line);
                    }

                    tokens.put(id, token);

                    if (Integer.parseInt(id) >= tokenIdCounter) {
                        tokenIdCounter = Integer.parseInt(id) + 1;
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Failed to load token from " + file.getName());
                    e.printStackTrace();
                }
            }
        }
    }

    public TokenTemplate createToken(String name, long durationSeconds) {
        String id = String.valueOf(tokenIdCounter++);
        TokenTemplate token = new TokenTemplate(id, name, durationSeconds, null, "PAPER", false);
        tokens.put(id, token);
        
        saveToken(token);
        return token;
    }

    public void saveToken(TokenTemplate token) {
        File tokensDir = new File(plugin.getDataFolder(), "tokens");
        if (!tokensDir.exists()) {
            tokensDir.mkdirs();
        }

        File tokenFile = new File(tokensDir, token.getId() + ".yml");
        FileConfiguration config = new YamlConfiguration();
        
        config.set("id", token.getId());
        config.set("name", token.getName());
        config.set("duration", token.getDurationSeconds());
        config.set("assigned-zone", token.getAssignedZoneId());
        config.set("assigned-zones", token.getAssignedZoneIds());  // Save multiple zones
        config.set("item-material", token.getItemMaterial());
        config.set("glowing", token.isGlowing());
        config.set("lore", token.getLore());

        try {
            config.save(tokenFile);
        } catch (IOException e) {
            plugin.getLogger().warning("Failed to save token: " + token.getId());
            e.printStackTrace();
        }
    }

    public void deleteToken(String tokenId) {
        tokens.remove(tokenId);
        File tokensDir = new File(plugin.getDataFolder(), "tokens");
        File tokenFile = new File(tokensDir, tokenId + ".yml");
        if (tokenFile.exists()) {
            tokenFile.delete();
        }
    }

    public TokenTemplate getToken(String tokenId) {
        return tokens.get(tokenId);
    }

    public TokenTemplate getTokenByName(String name) {
        for (TokenTemplate token : tokens.values()) {
            if (token.getName().equalsIgnoreCase(name)) {
                return token;
            }
        }
        return null;
    }

    public Map<String, TokenTemplate> getAllTokens() {
        return new HashMap<>(tokens);
    }

    public void renameToken(String tokenId, String newName) {
        TokenTemplate token = tokens.get(tokenId);
        if (token != null) {
            TokenTemplate newToken = new TokenTemplate(token.getId(), newName, token.getDurationSeconds(), 
                                                       token.getAssignedZoneId(), token.getItemMaterial(), token.isGlowing());
            for (String line : token.getLore()) {
                newToken.addLoreLine(line);
            }
            tokens.put(tokenId, newToken);
            saveToken(newToken);
        }
    }

    public void assignTokenToZone(String tokenId, String zoneId) {
        TokenTemplate token = tokens.get(tokenId);
        if (token != null) {
            TokenTemplate updatedToken = new TokenTemplate(token.getId(), token.getName(), token.getDurationSeconds(), 
                                                          zoneId, token.getItemMaterial(), token.isGlowing());
            for (String line : token.getLore()) {
                updatedToken.addLoreLine(line);
            }
            tokens.put(tokenId, updatedToken);
            saveToken(updatedToken);
        }
    }

    public void setTokenDuration(String tokenId, long durationSeconds) {
        TokenTemplate token = tokens.get(tokenId);
        if (token != null) {
            TokenTemplate updatedToken = new TokenTemplate(token.getId(), token.getName(), durationSeconds, 
                                                          token.getAssignedZoneId(), token.getItemMaterial(), token.isGlowing());
            for (String line : token.getLore()) {
                updatedToken.addLoreLine(line);
            }
            tokens.put(tokenId, updatedToken);
            saveToken(updatedToken);
        }
    }

    public void setTokenItem(String tokenId, String material, boolean glowing) {
        TokenTemplate token = tokens.get(tokenId);
        if (token != null) {
            TokenTemplate updatedToken = new TokenTemplate(token.getId(), token.getName(), token.getDurationSeconds(), 
                                                          token.getAssignedZoneId(), material, glowing);
            for (String line : token.getLore()) {
                updatedToken.addLoreLine(line);
            }
            tokens.put(tokenId, updatedToken);
            saveToken(updatedToken);
        }
    }

    public void addTokenLore(String tokenId, String line) {
        TokenTemplate token = tokens.get(tokenId);
        if (token != null) {
            token.addLoreLine(line);
            saveToken(token);
        }
    }

    public void setTokenLoreLine(String tokenId, int lineNumber, String newText) {
        TokenTemplate token = tokens.get(tokenId);
        if (token != null) {
            token.setLoreLine(lineNumber, newText);
            saveToken(token);
        }
    }

    public void removeTokenLoreLine(String tokenId, int lineNumber) {
        TokenTemplate token = tokens.get(tokenId);
        if (token != null) {
            token.removeLoreLine(lineNumber);
            saveToken(token);
        }
    }

    public void reloadTokens() {
        tokens.clear();
        tokenIdCounter = 1;
        loadTokens();
    }
}
