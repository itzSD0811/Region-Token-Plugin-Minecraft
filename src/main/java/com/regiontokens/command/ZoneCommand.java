package com.regiontokens.command;

import com.regiontokens.RegionTokensPlugin;
import com.regiontokens.listener.ToolListener;
import com.regiontokens.model.CuboidZone;
import com.regiontokens.util.ColorUtil;
import com.regiontokens.util.ConfigUtil;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class ZoneCommand {
    private final RegionTokensPlugin plugin;

    public ZoneCommand(RegionTokensPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean execute(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ConfigUtil.getMessage("error.invalid-usage"));
            return true;
        }

        String action = args[1].toLowerCase();

        switch (action) {
            case "create":
                return handleCreate(player, args);
            case "list":
                return handleList(player, args);
            case "rename":
                return handleRename(player, args);
            case "kickto":
                return handleKickTo(player, args);
            case "delete":
                return handleDelete(player, args);
            case "show":
                return handleShow(player, args);
            default:
                player.sendMessage(ConfigUtil.getMessage("error.invalid-usage"));
                return true;
        }
    }

    private boolean handleCreate(Player player, String[] args) {
        if (!player.hasPermission("regiontokens.admin.zones.create")) {
            player.sendMessage(ConfigUtil.getMessage("error.no-permission"));
            return true;
        }

        if (args.length < 3) {
            player.sendMessage(ConfigUtil.getMessage("error.invalid-usage"));
            return true;
        }

        // Check if player is holding zone tool
        ItemStack item = player.getInventory().getItemInMainHand();
        if (item == null || item.getType() != Material.SHEARS || 
            !item.hasItemMeta() || !item.getItemMeta().hasDisplayName() ||
            !item.getItemMeta().getDisplayName().contains("Zone Tool")) {
            player.sendMessage(ConfigUtil.getMessage("zone.error.not-selected"));
            return true;
        }

        String zoneName = args[2];
        
        int[] selection = plugin.getDatabaseManager().getZoneSelection(player.getUniqueId().toString());
        if (selection == null) {
            player.sendMessage(ConfigUtil.getMessage("zone.error.not-selected"));
            return true;
        }

        String world = plugin.getDatabaseManager().getZoneSelectionWorld(player.getUniqueId().toString());
        if (world == null) {
            player.sendMessage(ConfigUtil.getMessage("zone.error.not-selected"));
            return true;
        }

        CuboidZone zone = plugin.getZoneManager().createZone(zoneName, selection[0], selection[1], selection[2],
                                                            selection[3], selection[4], selection[5], world);

        player.sendTitle(
            ConfigUtil.getMessage("zone.created.title"),
            ColorUtil.colorize(zoneName)
        );

        plugin.getDatabaseManager().clearZoneSelection(player.getUniqueId().toString());

        // Stop particle effect after 3 seconds
        plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, () -> {
            // Particles will auto-stop when player switches tool or stops holding it
        }, 60L); // 3 seconds = 60 ticks

        plugin.getLogger().info("Zone created by " + player.getName() + " with ID: " + zone.getId());
        return true;
    }

    private boolean handleList(Player player, String[] args) {
        if (!player.hasPermission("regiontokens.admin.zones.list")) {
            player.sendMessage(ConfigUtil.getMessage("error.no-permission"));
            return true;
        }

        int page = 1;
        if (args.length > 2) {
            try {
                page = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                page = 1;
            }
        }

        var zones = plugin.getZoneManager().getAllZones();
        if (zones.isEmpty()) {
            player.sendMessage(ConfigUtil.getMessage("zone.list-empty"));
            return true;
        }

        int itemsPerPage = ConfigUtil.getItemsPerPage();
        int totalPages = (int) Math.ceil((double) zones.size() / itemsPerPage);

        if (page < 1 || page > totalPages) {
            page = 1;
        }

        int startIndex = (page - 1) * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, zones.size());

        player.sendMessage(ConfigUtil.getMessage("zone.list-header", "page", page, "total", totalPages));

        zones.values().stream().skip(startIndex).limit(itemsPerPage).forEach(zone ->
            player.sendMessage(ConfigUtil.getMessage("zone.list-item", 
                "id", zone.getId(), 
                "zoneName", ColorUtil.colorize(zone.getName())))
        );

        if (totalPages > 1) {
            player.sendMessage(buildPaginationButtons(page, totalPages, "zone"));
        }

        return true;
    }

    private boolean handleRename(Player player, String[] args) {
        if (!player.hasPermission("regiontokens.admin.zones.rename")) {
            player.sendMessage(ConfigUtil.getMessage("error.no-permission"));
            return true;
        }

        if (args.length < 4) {
            player.sendMessage(ConfigUtil.getMessage("error.invalid-usage"));
            return true;
        }

        String zoneId = args[2];
        String newName = args[3];

        CuboidZone zone = plugin.getZoneManager().getZone(zoneId);
        if (zone == null) {
            player.sendMessage(ConfigUtil.getMessage("zone.error.not-found"));
            return true;
        }

        plugin.getZoneManager().renameZone(zoneId, newName);
        player.sendMessage(ConfigUtil.getMessage("zone.renamed.title"));
        return true;
    }

    private boolean handleKickTo(Player player, String[] args) {
        if (!player.hasPermission("regiontokens.admin.zones.kickto")) {
            player.sendMessage(ConfigUtil.getMessage("error.no-permission"));
            return true;
        }

        if (args.length < 3) {
            player.sendMessage(ConfigUtil.getMessage("error.invalid-usage",
                "usage", "/rgtk zone kickto <zoneId> <x> <y> <z> OR /rgtk zone kickto <zoneId> facing"));
            return true;
        }

        String zoneId = args[2];
        CuboidZone zone = plugin.getZoneManager().getZone(zoneId);
        if (zone == null) {
            player.sendMessage(ConfigUtil.getMessage("zone.error.not-found"));
            return true;
        }

        // Check if using "facing" keyword
        if (args.length == 4 && args[3].equalsIgnoreCase("facing")) {
            org.bukkit.block.Block targetBlock = player.getTargetBlock(null, 100);
            if (targetBlock == null) {
                player.sendMessage(ConfigUtil.getMessage("error.invalid-usage",
                    "usage", "You must be looking at a block"));
                return true;
            }

            int x = targetBlock.getX();
            int y = targetBlock.getY();
            int z = targetBlock.getZ();

            // Check if position is inside the zone
            if (zone.contains(x, y, z)) {
                player.sendMessage(ConfigUtil.getMessage("zone.error.kick-inside-zone"));
                return true;
            }

            zone.setKickLocation(x, y, z, player.getWorld().getName());
            plugin.getZoneManager().saveZone(zone);
            player.sendMessage(ConfigUtil.getMessage("zone.kick-location-set", "x", String.valueOf(x), "y", String.valueOf(y), "z", String.valueOf(z)));
            return true;
        }

        // Parse coordinates
        if (args.length < 6) {
            player.sendMessage(ConfigUtil.getMessage("error.invalid-usage",
                "usage", "/rgtk zone kickto <zoneId> <x> <y> <z> OR /rgtk zone kickto <zoneId> facing"));
            return true;
        }

        try {
            int x = Integer.parseInt(args[3]);
            int y = Integer.parseInt(args[4]);
            int z = Integer.parseInt(args[5]);

            // Check if position is inside the zone
            if (zone.contains(x, y, z)) {
                player.sendMessage(ConfigUtil.getMessage("zone.error.kick-inside-zone"));
                return true;
            }

            zone.setKickLocation(x, y, z, player.getWorld().getName());
            plugin.getZoneManager().saveZone(zone);
            player.sendMessage(ConfigUtil.getMessage("zone.kick-location-set", "x", String.valueOf(x), "y", String.valueOf(y), "z", String.valueOf(z)));
        } catch (NumberFormatException e) {
            player.sendMessage(ConfigUtil.getMessage("error.invalid-usage",
                "usage", "/rgtk zone kickto <zoneId> <x> <y> <z> OR /rgtk zone kickto <zoneId> facing"));
        }

        return true;
    }

    private String buildPaginationButtons(int currentPage, int totalPages, String type) {
        StringBuilder sb = new StringBuilder();
        sb.append("&8<<< ");
        
        if (currentPage > 1) {
            sb.append("&b[Previous]&8 ");
        } else {
            sb.append("&7[Previous]&8 ");
        }
        
        if (currentPage < totalPages) {
            sb.append("&b[Next]&8 ");
        } else {
            sb.append("&7[Next]&8 ");
        }
        
        sb.append(">>>");
        
        return ColorUtil.colorize(sb.toString());
    }

    private boolean handleDelete(Player player, String[] args) {
        if (!player.hasPermission("regiontokens.admin.zones.delete")) {
            player.sendMessage(ConfigUtil.getMessage("error.no-permission"));
            return true;
        }

        if (args.length < 3) {
            player.sendMessage(ConfigUtil.getMessage("error.invalid-usage"));
            return true;
        }

        String zoneId = args[2];
        CuboidZone zone = plugin.getZoneManager().getZone(zoneId);
        if (zone == null) {
            player.sendMessage(ConfigUtil.getMessage("zone.error.not-found"));
            return true;
        }

        plugin.getZoneManager().deleteZone(zoneId);
        player.sendMessage(ConfigUtil.getMessage("zone.deleted",
            "zoneName", zone.getName()));

        return true;
    }

    private boolean handleShow(Player player, String[] args) {
        if (!player.hasPermission("regiontokens.admin.zones.show")) {
            player.sendMessage(ConfigUtil.getMessage("error.no-permission"));
            return true;
        }

        if (args.length < 3) {
            player.sendMessage(ConfigUtil.getMessage("error.invalid-usage",
                "usage", "/rgtk zone show <zoneId>"));
            return true;
        }

        String zoneId = args[2];
        CuboidZone zone = plugin.getZoneManager().getZone(zoneId);

        if (zone == null) {
            player.sendMessage(ConfigUtil.getMessage("zone.error.not-found"));
            return true;
        }

        // Send success message
        player.sendMessage(ConfigUtil.getMessage("zone.show",
            "zoneName", zone.getName()));

        // Display particles for 10 seconds (200 ticks)
        displayZoneOutline(zone, 200);

        return true;
    }

    private void displayZoneOutline(CuboidZone zone, int duration) {
        // Get min and max coordinates
        int minX = Math.min(zone.getX1(), zone.getX2());
        int maxX = Math.max(zone.getX1(), zone.getX2());
        int minY = Math.min(zone.getY1(), zone.getY2());
        int maxY = Math.max(zone.getY1(), zone.getY2());
        int minZ = Math.min(zone.getZ1(), zone.getZ2());
        int maxZ = Math.max(zone.getZ1(), zone.getZ2());

        org.bukkit.World world = Bukkit.getWorld(zone.getWorld());
        if (world == null) {
            return; // World doesn't exist
        }

        Location center = new Location(world, (minX + maxX) / 2.0, (minY + maxY) / 2.0, (minZ + maxZ) / 2.0);

        // Counter for tracking elapsed ticks
        final int[] tickCounter = {0};
        final int[] taskId = {-1};

        // Display cuboid outline every 2 ticks until duration expires
        taskId[0] = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            // Draw the 12 edges of the cuboid
            drawLine(center, minX, minY, minZ, maxX, minY, minZ, world); // bottom front
            drawLine(center, minX, minY, maxZ, maxX, minY, maxZ, world); // bottom back
            drawLine(center, minX, minY, minZ, minX, minY, maxZ, world); // bottom left
            drawLine(center, maxX, minY, minZ, maxX, minY, maxZ, world); // bottom right

            drawLine(center, minX, maxY, minZ, maxX, maxY, minZ, world); // top front
            drawLine(center, minX, maxY, maxZ, maxX, maxY, maxZ, world); // top back
            drawLine(center, minX, maxY, minZ, minX, maxY, maxZ, world); // top left
            drawLine(center, maxX, maxY, minZ, maxX, maxY, maxZ, world); // top right

            drawLine(center, minX, minY, minZ, minX, maxY, minZ, world); // front left vertical
            drawLine(center, maxX, minY, minZ, maxX, maxY, minZ, world); // front right vertical
            drawLine(center, minX, minY, maxZ, minX, maxY, maxZ, world); // back left vertical
            drawLine(center, maxX, minY, maxZ, maxX, maxY, maxZ, world); // back right vertical

            tickCounter[0] += 2;

            // Cancel if duration exceeded
            if (tickCounter[0] >= duration) {
                Bukkit.getScheduler().cancelTask(taskId[0]);
            }
        }, 0, 2);
    }

    private void drawLine(Location center, int x1, int y1, int z1, int x2, int y2, int z2, org.bukkit.World world) {
        // Calculate distance and steps
        double distance = Math.sqrt(
            Math.pow(x2 - x1, 2) +
            Math.pow(y2 - y1, 2) +
            Math.pow(z2 - z1, 2)
        );

        int steps = Math.max(1, (int) (distance * 4)); // 4 particles per block

        for (int i = 0; i <= steps; i++) {
            double progress = (double) i / steps;
            double x = x1 + (x2 - x1) * progress + 0.5;
            double y = y1 + (y2 - y1) * progress + 0.5;
            double z = z1 + (z2 - z1) * progress + 0.5;

            Location particleLocation = new Location(world, x, y, z);
            world.spawnParticle(Particle.DUST, particleLocation, 1, 
                new Particle.DustOptions(org.bukkit.Color.fromRGB(255, 85, 85), 1.0f));
        }
    }

    public boolean executeConsole(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /rgtk zone <create|delete|rename|kickto>");
            return true;
        }

        String action = args[1].toLowerCase();

        switch (action) {
            case "create":
                return handleCreateConsole(sender, args);
            case "delete":
                return handleDeleteConsole(sender, args);
            case "rename":
                return handleRenameConsole(sender, args);
            case "kickto":
                return handleKickToConsole(sender, args);
            case "list":
                return handleListConsole(sender, args);
            case "show":
                sender.sendMessage("§cZone show requires a player (display in player view)");
                return true;
            default:
                sender.sendMessage("§cUnknown zone action: " + action);
                return true;
        }
    }

    private boolean handleCreateConsole(CommandSender sender, String[] args) {
        if (args.length < 5) {
            sender.sendMessage("§cUsage: /rgtk zone create <zoneName> <x1> <y1> <z1> <x2> <y2> <z2>");
            return true;
        }

        String zoneName = args[2];
        try {
            int x1 = Integer.parseInt(args[3]);
            int y1 = Integer.parseInt(args[4]);
            int z1 = Integer.parseInt(args[5]);
            int x2 = Integer.parseInt(args[6]);
            int y2 = Integer.parseInt(args[7]);
            int z2 = Integer.parseInt(args[8]);

            // Need to specify world from console - use "world" as default or from args
            String worldName = (args.length > 9) ? args[9] : "world";
            org.bukkit.World world = Bukkit.getWorld(worldName);
            if (world == null) {
                sender.sendMessage("§cWorld '" + worldName + "' not found");
                return true;
            }

            CuboidZone zone = plugin.getZoneManager().createZone(zoneName, x1, y1, z1, x2, y2, z2, worldName);
            sender.sendMessage("§aZone '§e" + zoneName + "§a' created with ID: §e" + zone.getId());
            return true;
        } catch (NumberFormatException e) {
            sender.sendMessage("§cInvalid coordinates. Usage: /rgtk zone create <zoneName> <x1> <y1> <z1> <x2> <y2> <z2> [world]");
            return true;
        }
    }

    private boolean handleDeleteConsole(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§cUsage: /rgtk zone delete <zoneId>");
            return true;
        }

        String zoneId = args[2];
        CuboidZone zone = plugin.getZoneManager().getZone(zoneId);
        if (zone == null) {
            sender.sendMessage("§cZone not found.");
            return true;
        }

        plugin.getZoneManager().deleteZone(zoneId);
        sender.sendMessage("§aZone '§e" + zone.getName() + "§a' deleted.");
        return true;
    }

    private boolean handleRenameConsole(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage("§cUsage: /rgtk zone rename <zoneId> <newName>");
            return true;
        }

        String zoneId = args[2];
        String newName = args[3];
        CuboidZone zone = plugin.getZoneManager().getZone(zoneId);
        if (zone == null) {
            sender.sendMessage("§cZone not found.");
            return true;
        }

        plugin.getZoneManager().renameZone(zoneId, newName);
        sender.sendMessage("§aZone renamed to '§e" + newName + "§a'");
        return true;
    }

    private boolean handleKickToConsole(CommandSender sender, String[] args) {
        if (args.length < 6) {
            sender.sendMessage("§cUsage: /rgtk zone kickto <zoneId> <x> <y> <z>");
            return true;
        }

        String zoneId = args[2];
        CuboidZone zone = plugin.getZoneManager().getZone(zoneId);
        if (zone == null) {
            sender.sendMessage("§cZone not found.");
            return true;
        }

        try {
            int x = Integer.parseInt(args[3]);
            int y = Integer.parseInt(args[4]);
            int z = Integer.parseInt(args[5]);

            // Check if position is inside the zone
            if (zone.contains(x, y, z)) {
                sender.sendMessage("§cKick position must be outside the zone!");
                return true;
            }

            zone.setKickLocation(x, y, z, zone.getWorld());
            plugin.getZoneManager().saveZone(zone);
            sender.sendMessage("§aZone kick location set to §7X: §a" + x + " §7Y: §a" + y + " §7Z: §a" + z);
        } catch (NumberFormatException e) {
            sender.sendMessage("§cInvalid coordinates. Usage: /rgtk zone kickto <zoneId> <x> <y> <z>");
        }
        return true;
    }

    private boolean handleListConsole(CommandSender sender, String[] args) {
        var zones = plugin.getZoneManager().getAllZones();
        if (zones.isEmpty()) {
            sender.sendMessage("§cNo zones found.");
            return true;
        }

        sender.sendMessage("§8=== §bZone List §8===" );
        for (var entry : zones.entrySet()) {
            CuboidZone zone = entry.getValue();
            sender.sendMessage("§fID: §e" + zone.getId() + " §f- §e" + zone.getName());
        }
        return true;
    }
}
