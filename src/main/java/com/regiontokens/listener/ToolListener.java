package com.regiontokens.listener;

import com.regiontokens.RegionTokensPlugin;
import com.regiontokens.model.Position;
import com.regiontokens.util.ColorUtil;
import com.regiontokens.util.ConfigUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

public class ToolListener implements Listener {
    private final RegionTokensPlugin plugin;
    private final java.util.Map<String, BukkitTask> activeTasks = new java.util.HashMap<>();

    public ToolListener(RegionTokensPlugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerItemHeld(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        String uuid = player.getUniqueId().toString();

        // Only process if player has an active zone selection
        int[] selection = plugin.getDatabaseManager().getZoneSelection(uuid);
        if (selection == null) {
            return; // No active selection, ignore the event
        }

        // Check if player switched away from zone tool
        if (!isPlayerHoldingZoneTool(player)) {
            // Cancel selection and particles
            plugin.getDatabaseManager().clearZoneSelection(uuid);
            
            BukkitTask task = activeTasks.remove(uuid);
            if (task != null) {
                task.cancel();
            }
            
            player.sendMessage(ConfigUtil.getMessage("tool.error.not-selected"));
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (item == null || item.getType() != Material.SHEARS) {
            return;
        }

        if (!item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) {
            return;
        }

        if (!item.getItemMeta().getDisplayName().contains("Zone Tool")) {
            return;
        }

        event.setCancelled(true);

        if (!player.hasPermission("regiontokens.admin.tool")) {
            player.sendMessage(ConfigUtil.getMessage("tool.error.not-enough-perms"));
            return;
        }

        if (event.getAction() == Action.LEFT_CLICK_BLOCK) {
            int x = event.getClickedBlock().getX();
            int y = event.getClickedBlock().getY();
            int z = event.getClickedBlock().getZ();
            
            plugin.getDatabaseManager().setZoneSelection(player.getUniqueId().toString(), 
                player.getWorld().getName(), x, y, z, 0, 0, 0);
            
            player.sendMessage(ConfigUtil.getMessage("tool.position1", "x", x, "y", y, "z", z));
        } else if (event.getAction() == Action.RIGHT_CLICK_BLOCK) {
            int x = event.getClickedBlock().getX();
            int y = event.getClickedBlock().getY();
            int z = event.getClickedBlock().getZ();

            int[] selection = plugin.getDatabaseManager().getZoneSelection(player.getUniqueId().toString());
            if (selection == null) {
                player.sendMessage(ConfigUtil.getMessage("tool.error.not-enough-perms"));
                return;
            }

            String world = plugin.getDatabaseManager().getZoneSelectionWorld(player.getUniqueId().toString());
            if (world == null || !world.equals(player.getWorld().getName())) {
                player.sendMessage(ConfigUtil.getMessage("tool.error.different-world"));
                return;
            }

            plugin.getDatabaseManager().setZoneSelection(player.getUniqueId().toString(), 
                player.getWorld().getName(), selection[0], selection[1], selection[2], x, y, z);
            
            player.sendMessage(ConfigUtil.getMessage("tool.position2", "x", x, "y", y, "z", z));
            
            // Show zone creation suggestion
            player.sendMessage(ConfigUtil.getMessage("tool.zone-selected", "zoneName", ""));
            
            // Show particle effect while holding tool
            showZoneOutline(player, selection[0], selection[1], selection[2], x, y, z);
        }
    }

    private boolean isPlayerHoldingZoneTool(Player player) {
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType() != Material.SHEARS) {
            return false;
        }
        if (!item.hasItemMeta() || !item.getItemMeta().hasDisplayName()) {
            return false;
        }
        return item.getItemMeta().getDisplayName().contains("Zone Tool");
    }

    private void showZoneOutline(Player player, int x1, int y1, int z1, int x2, int y2, int z2) {
        // Normalize coordinates
        int minX = Math.min(x1, x2);
        int maxX = Math.max(x1, x2);
        int minY = Math.min(y1, y2);
        int maxY = Math.max(y1, y2);
        int minZ = Math.min(z1, z2);
        int maxZ = Math.max(z1, z2);

        String uuid = player.getUniqueId().toString();
        
        // Cancel previous task if any
        BukkitTask oldTask = activeTasks.remove(uuid);
        if (oldTask != null) {
            oldTask.cancel();
        }

        // Schedule particle effect task that runs while tool is held
        BukkitTask task = plugin.getServer().getScheduler().runTaskTimer(plugin, new Runnable() {
            @Override
            public void run() {
                if (!player.isOnline() || !isPlayerHoldingZoneTool(player)) {
                    BukkitTask currentTask = activeTasks.remove(uuid);
                    if (currentTask != null) {
                        currentTask.cancel();
                    }
                    return;
                }

                drawCuboidOutline(player, minX, minY, minZ, maxX, maxY, maxZ);
            }
        }, 0L, 2L); // Update every 2 ticks

        activeTasks.put(uuid, task);
    }

    public void stopParticleEffect(Player player) {
        String uuid = player.getUniqueId().toString();
        BukkitTask task = activeTasks.remove(uuid);
        if (task != null) {
            task.cancel();
        }
    }

    private void drawCuboidOutline(Player player, int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        Location playerLoc = player.getLocation();
        
        // Draw edges of the cuboid
        // Bottom face
        drawLine(player, minX, minY, minZ, maxX, minY, minZ);
        drawLine(player, maxX, minY, minZ, maxX, minY, maxZ);
        drawLine(player, maxX, minY, maxZ, minX, minY, maxZ);
        drawLine(player, minX, minY, maxZ, minX, minY, minZ);
        
        // Top face
        drawLine(player, minX, maxY, minZ, maxX, maxY, minZ);
        drawLine(player, maxX, maxY, minZ, maxX, maxY, maxZ);
        drawLine(player, maxX, maxY, maxZ, minX, maxY, maxZ);
        drawLine(player, minX, maxY, maxZ, minX, maxY, minZ);
        
        // Vertical edges
        drawLine(player, minX, minY, minZ, minX, maxY, minZ);
        drawLine(player, maxX, minY, minZ, maxX, maxY, minZ);
        drawLine(player, maxX, minY, maxZ, maxX, maxY, maxZ);
        drawLine(player, minX, minY, maxZ, minX, maxY, maxZ);
    }

    private void drawLine(Player player, int x1, int y1, int z1, int x2, int y2, int z2) {
        Location start = new Location(player.getWorld(), x1 + 0.5, y1 + 0.5, z1 + 0.5);
        Location end = new Location(player.getWorld(), x2 + 0.5, y2 + 0.5, z2 + 0.5);
        
        double distance = start.distance(end);
        int steps = (int) Math.max(1, distance * 2);
        
        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            double x = start.getX() + (end.getX() - start.getX()) * t;
            double y = start.getY() + (end.getY() - start.getY()) * t;
            double z = start.getZ() + (end.getZ() - start.getZ()) * t;
            
            Location particleLoc = new Location(player.getWorld(), x, y, z);
            
            // Use dust particle with red color
            player.spawnParticle(Particle.DUST, particleLoc, 1, 
                new Particle.DustOptions(org.bukkit.Color.fromRGB(255, 85, 85), 1.0f));
        }
    }
}
