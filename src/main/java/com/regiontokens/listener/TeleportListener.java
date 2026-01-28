package com.regiontokens.listener;

import com.regiontokens.RegionTokensPlugin;
import com.regiontokens.model.CuboidZone;
import com.regiontokens.model.TokenTemplate;
import com.regiontokens.util.ColorUtil;
import com.regiontokens.util.ConfigUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.ItemStack;

public class TeleportListener implements Listener {
    private final RegionTokensPlugin plugin;

    public TeleportListener(RegionTokensPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        Player player = event.getPlayer();
        
        // Check if player has bypass permission
        if (player.hasPermission("regiontokens.zones.bypass.*")) {
            return;
        }

        // Check destination location
        if (event.getTo() == null) {
            return;
        }

        String worldName = event.getTo().getWorld().getName();
        double x = event.getTo().getX();
        double y = event.getTo().getY();
        double z = event.getTo().getZ();

        // Check if destination is in any protected zone
        for (CuboidZone zone : plugin.getZoneManager().getAllZones().values()) {
            if (!zone.getWorld().equals(worldName)) {
                continue;
            }

            if (!zone.contains(x, y, z)) {
                continue;
            }

            // Check if player has bypass for this specific zone
            if (player.hasPermission("regiontokens.zones.bypass." + zone.getId())) {
                continue;
            }

            // Check if player has enter permission
            if (!player.hasPermission("regiontokens.zones.enter")) {
                player.sendMessage(ConfigUtil.getMessage("access.denied"));
                event.setCancelled(true);
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
                // No tokens required for this zone, allow teleport
                continue;
            }

            // Check if player has any of the required tokens
            boolean hasToken = false;
            for (ItemStack item : player.getInventory().getContents()) {
                if (item != null && item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
                    String displayName = item.getItemMeta().getDisplayName();
                    for (TokenTemplate token : zoneTokens) {
                        if (displayName.contains(token.getName())) {
                            hasToken = true;
                            break;
                        }
                    }
                    if (hasToken) break;
                }
            }

            if (!hasToken) {
                player.sendMessage(ConfigUtil.getMessage("access.no-token", "zoneName", ColorUtil.colorize(zone.getName())));
                event.setCancelled(true);
                return;
            }

            // Verify token permissions
            for (ItemStack item : player.getInventory().getContents()) {
                if (item != null && item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
                    String displayName = item.getItemMeta().getDisplayName();
                    for (TokenTemplate token : zoneTokens) {
                        if (displayName.contains(token.getName())) {
                            if (!player.hasPermission("regiontokens.token.use." + token.getId()) &&
                                !player.hasPermission("regiontokens.token.use.*")) {
                                player.sendMessage(ConfigUtil.getMessage("access.denied"));
                                event.setCancelled(true);
                                return;
                            }
                        }
                    }
                }
            }

            // Token is valid, allow teleport
            return;
        }
    }
}
