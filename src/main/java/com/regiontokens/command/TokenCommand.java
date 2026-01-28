package com.regiontokens.command;

import com.regiontokens.RegionTokensPlugin;
import com.regiontokens.model.TokenTemplate;
import com.regiontokens.util.ColorUtil;
import com.regiontokens.util.ConfigUtil;
import com.regiontokens.util.DurationUtil;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class TokenCommand {
    private final RegionTokensPlugin plugin;

    public TokenCommand(RegionTokensPlugin plugin) {
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
            case "assign":
                return handleAssign(player, args);
            case "unassign":
                return handleUnassign(player, args);
            case "duration":
                return handleDuration(player, args);
            case "item":
                return handleItem(player, args);
            case "rename":
                return handleRename(player, args);
            case "lore":
                return handleLore(player, args);
            case "give":
                return handleGive(player, args);
            case "delete":
                return handleDelete(player, args);
            default:
                player.sendMessage(ConfigUtil.getMessage("error.invalid-usage"));
                return true;
        }
    }

    private boolean handleCreate(Player player, String[] args) {
        if (!player.hasPermission("regiontokens.admin.token.create")) {
            player.sendMessage(ConfigUtil.getMessage("error.no-permission"));
            return true;
        }

        if (args.length < 3) {
            player.sendMessage(ConfigUtil.getMessage("error.invalid-usage"));
            return true;
        }

        String tokenName = args[2];
        long duration = 60;

        if (args.length > 3) {
            duration = DurationUtil.parseDuration(args[3]);
            if (duration < 0) {
                player.sendMessage(ConfigUtil.getMessage("token.error.invalid-duration"));
                return true;
            }
        }

        TokenTemplate token = plugin.getTokenManager().createToken(tokenName, duration);
        player.sendMessage(ConfigUtil.getMessage("token.created", 
            "id", token.getId(), 
            "tokenName", token.getName(), 
            "duration", duration));

        return true;
    }

    private boolean handleList(Player player, String[] args) {
        if (!player.hasPermission("regiontokens.admin.token.create")) {
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

        var tokens = plugin.getTokenManager().getAllTokens();
        if (tokens.isEmpty()) {
            player.sendMessage(ConfigUtil.getMessage("token.list-empty"));
            return true;
        }

        int itemsPerPage = ConfigUtil.getItemsPerPage();
        int totalPages = (int) Math.ceil((double) tokens.size() / itemsPerPage);

        if (page < 1 || page > totalPages) {
            page = 1;
        }

        player.sendMessage(ConfigUtil.getMessage("token.list-header", "page", page, "total", totalPages));

        tokens.values().stream().skip((long) (page - 1) * itemsPerPage).limit(itemsPerPage).forEach(token -> {
            String zoneName = token.getAssignedZoneId() != null ? 
                plugin.getZoneManager().getZone(token.getAssignedZoneId()).getName() : "None";
            player.sendMessage(ConfigUtil.getMessage("token.list-item", 
                "id", token.getId(), 
                "tokenName", token.getName(),
                "zoneName", zoneName));
        });

        if (totalPages > 1) {
            player.sendMessage(buildPaginationButtons(page, totalPages));
        }

        return true;
    }

    private boolean handleAssign(Player player, String[] args) {
        if (!player.hasPermission("regiontokens.admin.token.create")) {
            player.sendMessage(ConfigUtil.getMessage("error.no-permission"));
            return true;
        }

        if (args.length < 4) {
            player.sendMessage(ConfigUtil.getMessage("error.invalid-usage"));
            return true;
        }

        String tokenId = args[2];
        String zoneId = args[3];

        TokenTemplate token = plugin.getTokenManager().getToken(tokenId);
        if (token == null) {
            player.sendMessage(ConfigUtil.getMessage("token.error.not-found"));
            return true;
        }

        if (plugin.getZoneManager().getZone(zoneId) == null) {
            player.sendMessage(ConfigUtil.getMessage("zone.error.not-found"));
            return true;
        }

        plugin.getTokenManager().assignTokenToZone(tokenId, zoneId);
        plugin.getZoneManager().getZone(zoneId).addAssignedToken(tokenId);
        plugin.getZoneManager().saveZone(plugin.getZoneManager().getZone(zoneId));
        player.sendMessage(ConfigUtil.getMessage("token.assigned", 
            "tokenName", token.getName(), 
            "zoneName", plugin.getZoneManager().getZone(zoneId).getName()));

        return true;
    }

    private boolean handleUnassign(Player player, String[] args) {
        if (!player.hasPermission("regiontokens.admin.token.create")) {
            player.sendMessage(ConfigUtil.getMessage("error.no-permission"));
            return true;
        }

        if (args.length < 4) {
            player.sendMessage(ConfigUtil.getMessage("error.invalid-usage",
                "usage", "/rgtk token unassign <tokenId> <zoneId>"));
            return true;
        }

        String tokenId = args[2];
        String zoneId = args[3];

        TokenTemplate token = plugin.getTokenManager().getToken(tokenId);
        if (token == null) {
            player.sendMessage(ConfigUtil.getMessage("token.error.not-found"));
            return true;
        }

        if (plugin.getZoneManager().getZone(zoneId) == null) {
            player.sendMessage(ConfigUtil.getMessage("zone.error.not-found"));
            return true;
        }

        if (!token.isAssignedToZone(zoneId)) {
            player.sendMessage(ConfigUtil.getMessage("error.invalid-usage",
                "usage", "Token is not assigned to this zone"));
            return true;
        }

        token.removeAssignedZone(zoneId);
        plugin.getTokenManager().saveToken(token);
        plugin.getZoneManager().getZone(zoneId).removeAssignedToken(tokenId);
        plugin.getZoneManager().saveZone(plugin.getZoneManager().getZone(zoneId));
        player.sendMessage(ConfigUtil.getMessage("token.unassigned", 
            "tokenName", token.getName(), 
            "zoneName", plugin.getZoneManager().getZone(zoneId).getName()));

        return true;
    }

    private boolean handleDuration(Player player, String[] args) {
        if (!player.hasPermission("regiontokens.admin.token.duration")) {
            player.sendMessage(ConfigUtil.getMessage("error.no-permission"));
            return true;
        }

        if (args.length < 4) {
            player.sendMessage(ConfigUtil.getMessage("error.invalid-usage"));
            return true;
        }

        String tokenId = args[2];
        long duration = DurationUtil.parseDuration(args[3]);

        if (duration < 0) {
            player.sendMessage(ConfigUtil.getMessage("token.error.invalid-duration"));
            return true;
        }

        TokenTemplate token = plugin.getTokenManager().getToken(tokenId);
        if (token == null) {
            player.sendMessage(ConfigUtil.getMessage("token.error.not-found"));
            return true;
        }

        plugin.getTokenManager().setTokenDuration(tokenId, duration);
        player.sendMessage(ConfigUtil.getMessage("token.duration-changed", 
            "tokenName", token.getName(), 
            "duration", duration));

        return true;
    }

    private boolean handleItem(Player player, String[] args) {
        if (!player.hasPermission("regiontokens.admin.token.create")) {
            player.sendMessage(ConfigUtil.getMessage("error.no-permission"));
            return true;
        }

        if (args.length < 3) {
            player.sendMessage(ConfigUtil.getMessage("error.invalid-usage"));
            return true;
        }

        String tokenId = args[2];
        ItemStack itemInHand = player.getInventory().getItemInMainHand();
        
        // Check if player is holding an item
        if (itemInHand == null || itemInHand.getType() == Material.AIR) {
            player.sendMessage(ConfigUtil.getMessage("token.error.no-item-in-hand"));
            return true;
        }

        String materialName = itemInHand.getType().name();
        boolean glowing = false;

        if (args.length > 3) {
            glowing = args[3].equalsIgnoreCase("is-glowing");
        }

        TokenTemplate token = plugin.getTokenManager().getToken(tokenId);
        if (token == null) {
            player.sendMessage(ConfigUtil.getMessage("token.error.not-found"));
            return true;
        }

        plugin.getTokenManager().setTokenItem(tokenId, materialName, glowing);
        player.sendMessage(ConfigUtil.getMessage("token.item-configured", 
            "tokenName", token.getName(),
            "itemMaterial", materialName,
            "glowing", glowing ? "Yes" : "No"));

        return true;
    }

    private boolean handleRename(Player player, String[] args) {
        if (!player.hasPermission("regiontokens.admin.tokens.rename")) {
            player.sendMessage(ConfigUtil.getMessage("error.no-permission"));
            return true;
        }

        if (args.length < 4) {
            player.sendMessage(ConfigUtil.getMessage("error.invalid-usage"));
            return true;
        }

        String tokenId = args[2];
        String newName = args[3];

        TokenTemplate token = plugin.getTokenManager().getToken(tokenId);
        if (token == null) {
            player.sendMessage(ConfigUtil.getMessage("token.error.not-found"));
            return true;
        }

        plugin.getTokenManager().renameToken(tokenId, newName);
        player.sendMessage(ConfigUtil.getMessage("token.renamed", "tokenName", newName));

        return true;
    }

    private boolean handleLore(Player player, String[] args) {
        if (!player.hasPermission("regiontokens.admin.tokens.lore")) {
            player.sendMessage(ConfigUtil.getMessage("error.no-permission"));
            return true;
        }

        if (args.length < 4) {
            player.sendMessage(ConfigUtil.getMessage("error.invalid-usage"));
            return true;
        }

        String action = args[2].toLowerCase();
        String tokenId = args[3];

        TokenTemplate token = plugin.getTokenManager().getToken(tokenId);
        if (token == null) {
            player.sendMessage(ConfigUtil.getMessage("token.error.not-found"));
            return true;
        }

        switch (action) {
            case "add":
                if (args.length < 5) {
                    player.sendMessage(ConfigUtil.getMessage("error.invalid-usage"));
                    return true;
                }
                String loreText = String.join(" ", java.util.Arrays.copyOfRange(args, 4, args.length));
                plugin.getTokenManager().addTokenLore(tokenId, loreText);
                player.sendMessage(ConfigUtil.getMessage("token.lore.added", "tokenName", token.getName()));
                break;

            case "set":
                if (args.length < 6) {
                    player.sendMessage(ConfigUtil.getMessage("error.invalid-usage"));
                    return true;
                }
                try {
                    int lineNumber = Integer.parseInt(args[4]);
                    String newText = String.join(" ", java.util.Arrays.copyOfRange(args, 5, args.length));
                    plugin.getTokenManager().setTokenLoreLine(tokenId, lineNumber, newText);
                    player.sendMessage(ConfigUtil.getMessage("token.lore.set", "lineNumber", lineNumber, "tokenName", token.getName()));
                } catch (NumberFormatException e) {
                    player.sendMessage(ConfigUtil.getMessage("token.error.invalid-lore-line"));
                }
                break;

            case "remove":
                if (args.length < 5) {
                    player.sendMessage(ConfigUtil.getMessage("error.invalid-usage"));
                    return true;
                }
                try {
                    int lineNumber = Integer.parseInt(args[4]);
                    plugin.getTokenManager().removeTokenLoreLine(tokenId, lineNumber);
                    player.sendMessage(ConfigUtil.getMessage("token.lore.removed", "lineNumber", lineNumber, "tokenName", token.getName()));
                } catch (NumberFormatException e) {
                    player.sendMessage(ConfigUtil.getMessage("token.error.invalid-lore-line"));
                }
                break;

            default:
                player.sendMessage(ConfigUtil.getMessage("error.invalid-usage"));
        }

        return true;
    }

    private String buildPaginationButtons(int currentPage, int totalPages) {
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

    private boolean handleGive(Player player, String[] args) {
        if (!player.hasPermission("regiontokens.admin.token.give")) {
            player.sendMessage(ConfigUtil.getMessage("error.no-permission"));
            return true;
        }

        if (args.length < 4) {
            player.sendMessage(ConfigUtil.getMessage("error.invalid-usage"));
            return true;
        }

        String tokenId = args[2];
        String targetPlayerName = args[3];

        TokenTemplate token = plugin.getTokenManager().getToken(tokenId);
        if (token == null) {
            player.sendMessage(ConfigUtil.getMessage("token.error.not-found", "tokenId", tokenId));
            return true;
        }

        String zoneId = token.getAssignedZoneId();
        if (zoneId == null || zoneId.isEmpty()) {
            player.sendMessage(ConfigUtil.getMessage("token.error.no-zone-assigned"));
            return true;
        }

        Player targetPlayer = Bukkit.getPlayer(targetPlayerName);
        if (targetPlayer == null) {
            player.sendMessage(ConfigUtil.getMessage("error.player-not-found", "playerName", targetPlayerName));
            return true;
        }

        long duration = token.getDurationSeconds();
        plugin.getDatabaseManager().setPlayerTokenTime(targetPlayer.getUniqueId().toString(), zoneId, tokenId, duration);

        // Create and give the token item to the player
        ItemStack tokenItem = createTokenItem(token);
        if (targetPlayer.getInventory().addItem(tokenItem).isEmpty()) {
            // Inventory had space
            player.sendMessage(ConfigUtil.getMessage("token.given",
                "tokenName", token.getName(),
                "playerName", targetPlayer.getName(),
                "duration", duration));

            targetPlayer.sendMessage(ConfigUtil.getMessage("token.received",
                "tokenName", token.getName(),
                "duration", duration));
        } else {
            // Inventory was full
            player.sendMessage(ConfigUtil.getMessage("token.inventory-full",
                "playerName", targetPlayer.getName()));
            targetPlayer.sendMessage(ConfigUtil.getMessage("token.inventory-full-self"));
        }

        return true;
    }

    private ItemStack createTokenItem(TokenTemplate token) {
        Material material;
        try {
            material = Material.valueOf(token.getItemMaterial());
        } catch (IllegalArgumentException e) {
            material = Material.PAPER; // Default to paper if material not found
        }

        ItemStack item = new ItemStack(material, 1);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.setDisplayName(ColorUtil.colorize("&e" + token.getName()));
            
            java.util.List<String> lore = new java.util.ArrayList<>();
            for (String loreLine : token.getLore()) {
                lore.add(ColorUtil.colorize(loreLine));
            }
            lore.add(ColorUtil.colorize("&7Zone: &f" + token.getAssignedZoneId()));
            lore.add(ColorUtil.colorize("&7Duration: &f" + DurationUtil.formatDuration(token.getDurationSeconds())));
            
            meta.setLore(lore);

            if (token.isGlowing()) {
                meta.addEnchant(org.bukkit.enchantments.Enchantment.SILK_TOUCH, 1, true);
                meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
            }

            item.setItemMeta(meta);
        }

        return item;
    }

    private boolean handleDelete(Player player, String[] args) {
        if (!player.hasPermission("regiontokens.admin.token.delete")) {
            player.sendMessage(ConfigUtil.getMessage("error.no-permission"));
            return true;
        }

        if (args.length < 3) {
            player.sendMessage(ConfigUtil.getMessage("error.invalid-usage"));
            return true;
        }

        String tokenId = args[2];
        TokenTemplate token = plugin.getTokenManager().getToken(tokenId);
        if (token == null) {
            player.sendMessage(ConfigUtil.getMessage("token.error.not-found"));
            return true;
        }

        plugin.getTokenManager().deleteToken(tokenId);
        player.sendMessage(ConfigUtil.getMessage("token.deleted",
            "tokenName", token.getName()));

        return true;
    }

    public boolean executeConsole(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage("§cUsage: /rgtk token <create|assign|delete|duration|rename|lore|give>");
            return true;
        }

        String action = args[1].toLowerCase();

        switch (action) {
            case "create":
                return handleCreateConsole(sender, args);
            case "assign":
                return handleAssignConsole(sender, args);
            case "delete":
                return handleDeleteConsole(sender, args);
            case "duration":
                return handleDurationConsole(sender, args);
            case "rename":
                return handleRenameConsole(sender, args);
            case "lore":
                sender.sendMessage("§cLore command requires a player (item in hand needed)");
                return true;
            case "give":
                return handleGiveConsole(sender, args);
            case "list":
                return handleListConsole(sender, args);
            default:
                sender.sendMessage("§cUnknown token action: " + action);
                return true;
        }
    }

    // Console versions of handlers
    private boolean handleCreateConsole(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§cUsage: /rgtk token create <tokenName> [duration]");
            return true;
        }

        String tokenName = args[2];
        long duration = 3600; // default 1 hour

        if (args.length > 3) {
            try {
                duration = DurationUtil.parseDuration(args[3]);
            } catch (Exception e) {
                sender.sendMessage("§cInvalid duration format. Use: 10s, 5m, 1h, 2d");
                return true;
            }
        }

        TokenTemplate token = plugin.getTokenManager().createToken(tokenName, duration);
        sender.sendMessage("§aToken created with ID: §e" + token.getId() + " §aName: §e" + tokenName + " §aDuration: §e" + duration + "s");
        return true;
    }

    private boolean handleAssignConsole(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage("§cUsage: /rgtk token assign <tokenId> <zoneId>");
            return true;
        }

        String tokenId = args[2];
        String zoneId = args[3];

        TokenTemplate token = plugin.getTokenManager().getToken(tokenId);
        if (token == null) {
            sender.sendMessage("§cToken not found.");
            return true;
        }

        plugin.getTokenManager().assignTokenToZone(tokenId, zoneId);
        sender.sendMessage("§aToken §e" + tokenId + " §aassigned to zone §e" + zoneId);
        return true;
    }

    private boolean handleDeleteConsole(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage("§cUsage: /rgtk token delete <tokenId>");
            return true;
        }

        String tokenId = args[2];
        TokenTemplate token = plugin.getTokenManager().getToken(tokenId);
        if (token == null) {
            sender.sendMessage("§cToken not found.");
            return true;
        }

        plugin.getTokenManager().deleteToken(tokenId);
        sender.sendMessage("§aToken §e" + tokenId + " §adeleted.");
        return true;
    }

    private boolean handleDurationConsole(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage("§cUsage: /rgtk token duration <tokenId> <duration>");
            return true;
        }

        String tokenId = args[2];
        TokenTemplate token = plugin.getTokenManager().getToken(tokenId);
        if (token == null) {
            sender.sendMessage("§cToken not found.");
            return true;
        }

        try {
            long duration = DurationUtil.parseDuration(args[3]);
            plugin.getTokenManager().setTokenDuration(tokenId, duration);
            sender.sendMessage("§aToken §e" + tokenId + " §aduration changed to §e" + duration + "s");
        } catch (Exception e) {
            sender.sendMessage("§cInvalid duration format. Use: 10s, 5m, 1h, 2d");
        }
        return true;
    }

    private boolean handleRenameConsole(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage("§cUsage: /rgtk token rename <tokenId> <newName>");
            return true;
        }

        String tokenId = args[2];
        String newName = args[3];
        TokenTemplate token = plugin.getTokenManager().getToken(tokenId);
        if (token == null) {
            sender.sendMessage("§cToken not found.");
            return true;
        }

        plugin.getTokenManager().renameToken(tokenId, newName);
        sender.sendMessage("§aToken §e" + tokenId + " §arenamed to §e" + newName);
        return true;
    }

    private boolean handleGiveConsole(CommandSender sender, String[] args) {
        if (args.length < 4) {
            sender.sendMessage("§cUsage: /rgtk token give <tokenId> <playerName>");
            return true;
        }

        String tokenId = args[2];
        String playerName = args[3];
        Player targetPlayer = Bukkit.getPlayer(playerName);

        if (targetPlayer == null) {
            sender.sendMessage("§cPlayer §e" + playerName + " §cnot found or offline.");
            return true;
        }

        TokenTemplate token = plugin.getTokenManager().getToken(tokenId);
        if (token == null) {
            sender.sendMessage("§cToken not found.");
            return true;
        }

        ItemStack tokenItem = createTokenItem(token);
        if (targetPlayer.getInventory().firstEmpty() == -1) {
            sender.sendMessage("§cPlayer inventory is full.");
            return true;
        }

        targetPlayer.getInventory().addItem(tokenItem);
        sender.sendMessage("§aToken §e" + tokenId + " §agiven to §e" + playerName);
        targetPlayer.sendMessage("§aYou received token §e" + token.getName() + " §avalid for §e" + token.getDurationSeconds() + "s");
        return true;
    }

    private boolean handleListConsole(CommandSender sender, String[] args) {
        var tokens = plugin.getTokenManager().getAllTokens();
        if (tokens.isEmpty()) {
            sender.sendMessage("§cNo tokens found.");
            return true;
        }

        sender.sendMessage("§8=== §bToken List §8===");
        for (var entry : tokens.entrySet()) {
            TokenTemplate token = entry.getValue();
            sender.sendMessage("§fID: §e" + token.getId() + " §f- Name: §e" + token.getName());
        }
        return true;
    }
}
