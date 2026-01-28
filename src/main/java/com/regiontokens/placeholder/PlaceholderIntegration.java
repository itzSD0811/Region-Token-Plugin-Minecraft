package com.regiontokens.placeholder;

import com.regiontokens.RegionTokensPlugin;
import org.bukkit.entity.Player;

/**
 * PlaceholderAPI Integration for RegionTokens
 * Provides placeholders for zone and token information
 * 
 * Available placeholders (when PlaceholderAPI is installed):
 * - %regiontokens_current_zone% - Name of the zone the player is in
 * - %regiontokens_in_zone% - Whether player is in a zone (yes/no)
 * - %regiontokens_token_name% - Name of token player is holding
 * - %regiontokens_token_time_left% - Time remaining on held token (H:M:S format)
 * - %regiontokens_token_duration% - Total duration of held token (seconds)
 * - %regiontokens_zone_time_left% - Time remaining for zone player is in (seconds)
 * - %regiontokens_zone_name% - Name of current zone
 */
public class PlaceholderIntegration {
    private final RegionTokensPlugin plugin;

    public PlaceholderIntegration(RegionTokensPlugin plugin) {
        this.plugin = plugin;
    }

    public void register() {
        try {
            // Try to use reflection to extend PlaceholderExpansion
            Class<?> expansionClass = Class.forName("me.clip.placeholderapi.expansion.PlaceholderExpansion");
            Class<?> apiClass = Class.forName("me.clip.placeholderapi.PlaceholderAPI");
            
            // Create an expansion instance using dynamic proxy or reflection
            Object expansion = createExpansion();
            
            // Register the expansion
            var registerMethod = apiClass.getMethod("registerExpansion", expansionClass);
            registerMethod.invoke(null, expansion);
            
            plugin.getLogger().info("✓ PlaceholderAPI expansion registered with placeholders!");
        } catch (ClassNotFoundException e) {
            plugin.getLogger().info("PlaceholderAPI not found on this server, placeholders disabled");
        } catch (Exception e) {
            plugin.getLogger().warning("Failed to register PlaceholderAPI expansion: " + e.getMessage());
        }
    }

    private Object createExpansion() {
        return new Object() {
            public String getIdentifier() { return "regiontokens"; }
            public String getAuthor() { return "RegionTokens"; }
            public String getVersion() { return "1.0.0"; }
            public boolean persist() { return true; }
            
            public String onPlaceholderRequest(Player player, String identifier) {
                if (player == null) return "";
                
                switch (identifier.toLowerCase()) {
                    case "current_zone":
                    case "zone_name":
                        return getCurrentZone(player);
                    case "in_zone":
                        return isInZone(player) ? "yes" : "no";
                    case "token_name":
                        return getHeldTokenName(player);
                    case "token_time_left":
                        return getTokenTimeLeft(player);
                    case "token_duration":
                        return getTokenDuration(player);
                    case "zone_time_left":
                        return getZoneTimeLeft(player);
                    default:
                        return null;
                }
            }

            private String getCurrentZone(Player player) {
                for (var zone : plugin.getZoneManager().getAllZones().values()) {
                    int x = player.getLocation().getBlockX();
                    int y = player.getLocation().getBlockY();
                    int z = player.getLocation().getBlockZ();
                    if (zone.contains(x, y, z)) {
                        return zone.getName();
                    }
                }
                return "None";
            }

            private boolean isInZone(Player player) {
                for (var zone : plugin.getZoneManager().getAllZones().values()) {
                    int x = player.getLocation().getBlockX();
                    int y = player.getLocation().getBlockY();
                    int z = player.getLocation().getBlockZ();
                    if (zone.contains(x, y, z)) {
                        return true;
                    }
                }
                return false;
            }

            private String getHeldTokenName(Player player) {
                if (player.getInventory().getItemInMainHand() == null) {
                    return "None";
                }
                
                var item = player.getInventory().getItemInMainHand();
                if (item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
                    String displayName = item.getItemMeta().getDisplayName();
                    if (displayName.contains("Token:")) {
                        return displayName.replaceAll("§[0-9a-fk-orA-FK-OR]", "").replace("Token:", "").trim();
                    }
                }
                return "None";
            }

            private String getTokenTimeLeft(Player player) {
                if (player.getInventory().getItemInMainHand() == null) {
                    return "0";
                }
                
                var item = player.getInventory().getItemInMainHand();
                if (item.hasItemMeta() && item.getItemMeta().hasLore()) {
                    for (String line : item.getItemMeta().getLore()) {
                        if (line.contains("Time Left:")) {
                            String time = line.replaceAll("§[0-9a-fk-orA-FK-OR]", "").replaceAll("[^0-9:\\[\\]]", "").replace("[", "").replace("]", "");
                            return time.isEmpty() ? "0" : time;
                        }
                    }
                }
                return "0";
            }

            private String getTokenDuration(Player player) {
                String tokenName = getHeldTokenName(player);
                if (tokenName.equals("None")) {
                    return "0";
                }
                
                for (var token : plugin.getTokenManager().getAllTokens().values()) {
                    if (token.getName().equalsIgnoreCase(tokenName)) {
                        return String.valueOf(token.getDurationSeconds());
                    }
                }
                return "0";
            }

            private String getZoneTimeLeft(Player player) {
                for (var zone : plugin.getZoneManager().getAllZones().values()) {
                    int x = player.getLocation().getBlockX();
                    int y = player.getLocation().getBlockY();
                    int z = player.getLocation().getBlockZ();
                    if (zone.contains(x, y, z)) {
                        long time = plugin.getDatabaseManager().getPlayerTokenTime(player.getUniqueId().toString(), zone.getId());
                        return String.valueOf(Math.max(0, time));
                    }
                }
                return "0";
            }
        };
    }
}
