package com.regiontokens.listener;

import com.regiontokens.RegionTokensPlugin;
import com.regiontokens.model.CuboidZone;
import com.regiontokens.model.TokenTemplate;
import com.regiontokens.util.ColorUtil;
import com.regiontokens.util.ConfigUtil;
import com.regiontokens.util.DurationUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class ZoneAccessListener implements Listener {
    private final RegionTokensPlugin plugin;
    private final Map<String, String> playerZones = new HashMap<>();

    public ZoneAccessListener(RegionTokensPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Location to = event.getTo();
        Location from = event.getFrom();

        if (to == null || from == null) {
            return;
        }

        if (to.getX() == from.getX() && to.getY() == from.getY() && to.getZ() == from.getZ()) {
            return;
        }

        for (CuboidZone zone : plugin.getZoneManager().getAllZones().values()) {
            if (!zone.getWorld().equals(player.getWorld().getName())) {
                continue;
            }

            boolean wasInZone = zone.contains(from.getX(), from.getY(), from.getZ());
            boolean isInZone = zone.contains(to.getX(), to.getY(), to.getZ());

            if (!wasInZone && isInZone) {
                handleZoneEntry(event, player, zone);
            } else if (wasInZone && !isInZone) {
                playerZones.remove(player.getUniqueId().toString());
            }
        }
    }

    private void handleZoneEntry(PlayerMoveEvent event, Player player, CuboidZone zone) {
        if (player.hasPermission("regiontokens.zones.bypass.*") || 
            player.hasPermission("regiontokens.zones.bypass." + zone.getId())) {
            playerZones.put(player.getUniqueId().toString(), zone.getId());
            return;
        }

        if (!player.hasPermission("regiontokens.zones.enter")) {
            player.sendMessage(ConfigUtil.getMessage("access.denied"));
            return;
        }

        // Get all tokens assigned to this zone
        java.util.List<TokenTemplate> zoneTokens = new java.util.ArrayList<>();
        for (TokenTemplate token : plugin.getTokenManager().getAllTokens().values()) {
            if (token.isAssignedToZone(zone.getId())) {
                zoneTokens.add(token);
            }
        }

        if (zoneTokens.isEmpty()) {
            playerZones.put(player.getUniqueId().toString(), zone.getId());
            return;
        }

        // Find ALL tokens in player inventory that match zone tokens
        java.util.List<TokenTemplate> foundTokens = new java.util.ArrayList<>();
        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
                String displayName = item.getItemMeta().getDisplayName();
                for (TokenTemplate token : zoneTokens) {
                    if (displayName.contains(token.getName()) && !foundTokens.contains(token)) {
                        foundTokens.add(token);
                    }
                }
            }
        }

        if (foundTokens.isEmpty()) {
            // Player has no tokens - allow entry but will be kicked by timer task fallback
            player.sendMessage(ConfigUtil.getMessage("access.no-token", "zoneName", ColorUtil.colorize(zone.getName())));
            playerZones.put(player.getUniqueId().toString(), zone.getId());
            return;
        }

        // Verify permissions for all found tokens
        for (TokenTemplate token : foundTokens) {
            if (!player.hasPermission("regiontokens.token.use." + token.getId()) &&
                !player.hasPermission("regiontokens.token.use.*")) {
                player.sendMessage(ConfigUtil.getMessage("access.denied"));
                return;
            }

            // Track each token separately
            long timeRemaining = plugin.getDatabaseManager().getPlayerTokenTime(player.getUniqueId().toString(), zone.getId(), token.getId());
            
            if (timeRemaining <= 0) {
                timeRemaining = token.getDurationSeconds();
            }

            plugin.getDatabaseManager().setPlayerTokenTime(player.getUniqueId().toString(), zone.getId(), token.getId(), timeRemaining);
            
            player.sendMessage(ConfigUtil.getMessage("access.entry-success", 
                "zoneName", ColorUtil.colorize(zone.getName()), 
                "time", timeRemaining));
        }

        playerZones.put(player.getUniqueId().toString(), zone.getId());
    }
}

