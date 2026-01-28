package com.regiontokens.task;

import com.regiontokens.RegionTokensPlugin;
import com.regiontokens.model.CuboidZone;
import com.regiontokens.util.ColorUtil;
import com.regiontokens.util.ConfigUtil;
import com.regiontokens.util.DurationUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TokenTimerTask implements Runnable {
    private final RegionTokensPlugin plugin;
    private final Map<String, Long> playerZoneTime = new HashMap<>();

    public TokenTimerTask(RegionTokensPlugin plugin) {
        this.plugin = plugin;
    }

    public void start() {
        Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, this, 20L, 20L);
    }

    @Override
    public void run() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            updatePlayerTokens(player);
        }
    }

    private void updatePlayerTokens(Player player) {
        String playerUUID = player.getUniqueId().toString();

        for (CuboidZone zone : plugin.getZoneManager().getAllZones().values()) {
            if (!zone.getWorld().equals(player.getWorld().getName())) {
                continue;
            }

            boolean inZone = zone.contains(player.getLocation().getX(), player.getLocation().getY(), player.getLocation().getZ());

            if (!inZone) {
                continue;
            }

            if (player.hasPermission("regiontokens.zones.bypass.*") || 
                player.hasPermission("regiontokens.zones.bypass." + zone.getId())) {
                continue;
            }

            // Get all tokens assigned to this zone
            java.util.List<String> assignedTokenIds = zone.getAssignedTokenIds();
            boolean hasAnyToken = false;
            boolean hasValidTokenInInventory = false;

            // First, verify player actually has the tokens in inventory
            for (String tokenId : assignedTokenIds) {
                var token = plugin.getTokenManager().getToken(tokenId);
                if (token == null) continue;

                // Check if player has this token in inventory
                for (ItemStack item : player.getInventory().getContents()) {
                    if (item != null && item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
                        if (item.getItemMeta().getDisplayName().contains(token.getName())) {
                            hasValidTokenInInventory = true;
                            break;
                        }
                    }
                }
                if (hasValidTokenInInventory) break;
            }

            // If player is in zone with assigned tokens but has none in inventory, kick them
            if (!assignedTokenIds.isEmpty() && !hasValidTokenInInventory) {
                kickPlayerFromZone(player, zone);
                continue;
            }

            for (String tokenId : assignedTokenIds) {
                long timeRemaining = plugin.getDatabaseManager().getPlayerTokenTime(playerUUID, zone.getId(), tokenId);

                if (timeRemaining <= 0) {
                    continue;
                }

                hasAnyToken = true;

                if (timeRemaining <= ConfigUtil.getTimeWarningThreshold()) {
                    player.sendActionBar(ConfigUtil.getMessage("access.time-warning", 
                        "time", timeRemaining));
                }

                timeRemaining--;
                var token = plugin.getTokenManager().getToken(tokenId);
                if (token != null) {
                    plugin.getDatabaseManager().setPlayerTokenTime(playerUUID, zone.getId(), tokenId, timeRemaining);
                    
                    // Update token lore in real-time
                    updateTokenLore(player, token.getName(), timeRemaining);

                    if (timeRemaining == 0) {
                        plugin.getDatabaseManager().removePlayerTokenSpecific(playerUUID, zone.getId(), tokenId);
                        removeTokenFromInventory(player, token.getName());
                    }
                }
            }

            // If no tokens remaining, kick from zone
            if (!hasAnyToken && !assignedTokenIds.isEmpty()) {
                kickPlayerFromZone(player, zone);
            }
        }
    }

    private void updateTokenLore(Player player, String tokenName, long timeRemaining) {
        // Find the token item in player's inventory and update its lore
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item != null && item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
                String displayName = item.getItemMeta().getDisplayName();
                if (displayName.contains(tokenName)) {
                    ItemMeta meta = item.getItemMeta();
                    if (meta != null && meta.hasLore()) {
                        List<String> lore = new ArrayList<>(meta.getLore());
                        
                        // Update or add the time line
                        String timeLine = ColorUtil.colorize("&7Time Left: &f[" + formatTimeHMS(timeRemaining) + "]");
                        
                        boolean found = false;
                        for (int j = 0; j < lore.size(); j++) {
                            if (lore.get(j).contains("Time Left")) {
                                lore.set(j, timeLine);
                                found = true;
                                break;
                            }
                        }
                        
                        if (!found) {
                            lore.add(timeLine);
                        }
                        
                        meta.setLore(lore);
                        item.setItemMeta(meta);
                    }
                    break;
                }
            }
        }
    }

    private String formatTimeHMS(long seconds) {
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        
        return String.format("%d:%02d:%02d", hours, minutes, secs);
    }

    private void kickPlayerFromZone(Player player, CuboidZone zone) {
        String playerUUID = player.getUniqueId().toString();
        
        Location kickLocation = new Location(
            Bukkit.getWorld(zone.getKickWorld()),
            zone.getKickX() + 0.5,
            zone.getKickY() + 0.5,
            zone.getKickZ() + 0.5
        );

        if (kickLocation.getWorld() == null) {
            kickLocation = player.getWorld().getSpawnLocation();
        }

        player.teleport(kickLocation);
        player.sendMessage(ConfigUtil.getMessage("access.kicked", 
            "zoneName", zone.getName()));

        plugin.getDatabaseManager().removePlayerToken(playerUUID, zone.getId());

        removeTokenFromInventory(player, zone);
    }

    private void removeTokenFromInventory(Player player, CuboidZone zone) {
        var token = plugin.getTokenManager().getToken(zone.getId());
        if (token != null) {
            for (int i = 0; i < player.getInventory().getSize(); i++) {
                var item = player.getInventory().getItem(i);
                if (item != null && item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
                    if (item.getItemMeta().getDisplayName().contains(token.getName())) {
                        player.getInventory().removeItem(item);
                        break;
                    }
                }
            }
        }
    }

    private void removeTokenFromInventory(Player player, String tokenName) {
        for (int i = 0; i < player.getInventory().getSize(); i++) {
            ItemStack item = player.getInventory().getItem(i);
            if (item != null && item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
                if (item.getItemMeta().getDisplayName().contains(tokenName)) {
                    player.getInventory().removeItem(item);
                    break;
                }
            }
        }
    }
}
