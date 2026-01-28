package com.regiontokens.listener;

import com.regiontokens.RegionTokensPlugin;
import com.regiontokens.model.CuboidZone;
import com.regiontokens.model.TokenTemplate;
import com.regiontokens.util.ColorUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class ItemDropListener implements Listener {
    private final RegionTokensPlugin plugin;

    public ItemDropListener(RegionTokensPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onItemDrop(PlayerDropItemEvent event) {
        Player player = event.getPlayer();
        ItemStack droppedItem = event.getItemDrop().getItemStack();

        if (!droppedItem.hasItemMeta() || !droppedItem.getItemMeta().hasDisplayName()) {
            return;
        }

        String displayName = droppedItem.getItemMeta().getDisplayName();

        // Check if the dropped item is a token
        TokenTemplate droppedToken = null;
        for (TokenTemplate token : plugin.getTokenManager().getAllTokens().values()) {
            if (displayName.contains(token.getName())) {
                droppedToken = token;
                break;
            }
        }

        if (droppedToken == null) {
            return;
        }

        // Check all zones that this token is assigned to
        for (String zoneId : droppedToken.getAssignedZoneIds()) {
            CuboidZone zone = plugin.getZoneManager().getZone(zoneId);
            if (zone == null) {
                continue;
            }

            if (!zone.getWorld().equals(player.getWorld().getName())) {
                continue;
            }

            // Check if player is in this zone
            if (!zone.contains(player.getLocation().getX(), player.getLocation().getY(), player.getLocation().getZ())) {
                continue;
            }

            // Player is in a zone and dropped a required token
            // Check if they have any other valid tokens for this zone
            boolean hasOtherToken = false;

            for (ItemStack item : player.getInventory().getContents()) {
                if (item != null && item != droppedItem && item.hasItemMeta() && item.getItemMeta().hasDisplayName()) {
                    String itemName = item.getItemMeta().getDisplayName();
                    for (TokenTemplate token : plugin.getTokenManager().getAllTokens().values()) {
                        if (token.isAssignedToZone(zone.getId()) && itemName.contains(token.getName())) {
                            hasOtherToken = true;
                            break;
                        }
                    }
                    if (hasOtherToken) break;
                }
            }

            // If no other valid tokens, kick the player
            if (!hasOtherToken) {
                kickPlayerFromZone(player, zone);
                player.sendMessage(ColorUtil.colorize("&c&lYou dropped your token! You have been kicked from &e" + zone.getName()));
                return;
            }
        }
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
        // Only remove the timer for the specific token that was dropped and caused the kick
        // (or for all tokens if you want to clear all on kick, but not on drop)
        // plugin.getDatabaseManager().removePlayerTokenSpecific(playerUUID, zone.getId(), droppedToken.getId());
    }
}
