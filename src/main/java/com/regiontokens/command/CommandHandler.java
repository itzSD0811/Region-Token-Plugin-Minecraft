package com.regiontokens.command;

import com.regiontokens.RegionTokensPlugin;
import com.regiontokens.util.ColorUtil;
import com.regiontokens.util.ConfigUtil;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CommandHandler implements CommandExecutor {
    private final RegionTokensPlugin plugin;
    private final ZoneCommand zoneCommand;
    private final TokenCommand tokenCommand;
    private final ToolCommand toolCommand;

    public CommandHandler(RegionTokensPlugin plugin) {
        this.plugin = plugin;
        this.zoneCommand = new ZoneCommand(plugin);
        this.tokenCommand = new TokenCommand(plugin);
        this.toolCommand = new ToolCommand(plugin);
        
        ConfigUtil.setConfig(plugin.getConfig());
    }

    public void registerCommands() {
        var command = plugin.getCommand("regiontokens");
        if (command != null) {
            command.setExecutor(this);
            command.setTabCompleter(new CommandTabCompleter(plugin));
        } else {
            plugin.getLogger().warning("Failed to register regiontokens command - command not found in plugin.yml");
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 0) {
            if (sender instanceof Player) {
                sendHelp((Player) sender);
            } else {
                sender.sendMessage("§8========== §bRegionTokens Console §8==========");
                sender.sendMessage("§7Available: /rgtk token, /rgtk zone");
            }
            return true;
        }

        String subcommand = args[0].toLowerCase();

        switch (subcommand) {
            case "zone":
                if (sender instanceof Player) {
                    return zoneCommand.execute((Player) sender, args);
                } else {
                    return zoneCommand.executeConsole(sender, args);
                }
            case "token":
                if (sender instanceof Player) {
                    return tokenCommand.execute((Player) sender, args);
                } else {
                    return tokenCommand.executeConsole(sender, args);
                }
            case "tool":
                if (!(sender instanceof Player)) {
                    sender.sendMessage("§cTool command can only be used by players.");
                    return true;
                }
                return toolCommand.execute((Player) sender, args);
            case "reload":
                if (sender instanceof Player) {
                    return handleReload((Player) sender);
                } else {
                    return handleReloadConsole(sender);
                }
            case "help":
                if (sender instanceof Player) {
                    sendHelp((Player) sender);
                } else {
                    sender.sendMessage("§8========== §bRegionTokens Console §8==========");
                    sender.sendMessage("§7Available: /rgtk token, /rgtk zone");
                }
                return true;
            default:
                if (sender instanceof Player) {
                    ((Player) sender).sendMessage(ColorUtil.colorize("&cUnknown command. Type /rgtk help"));
                } else {
                    sender.sendMessage("§cUnknown command");
                }
                return true;
        }
    }

    private boolean handleReload(Player player) {
        if (!player.hasPermission("regiontokens.admin.reload")) {
            player.sendMessage(ConfigUtil.getMessage("error.no-permission"));
            return true;
        }

        try {
            plugin.reloadConfig();
            ConfigUtil.setConfig(plugin.getConfig());
            plugin.getZoneManager().reloadZones();
            plugin.getTokenManager().reloadTokens();
            player.sendMessage(ConfigUtil.getMessage("plugin.reloaded"));
            return true;
        } catch (Exception e) {
            player.sendMessage(ConfigUtil.getMessage("plugin.reload-error"));
            plugin.getLogger().severe("Failed to reload plugin: " + e.getMessage());
            e.printStackTrace();
            return true;
        }
    }

    private boolean handleReloadConsole(CommandSender sender) {
        try {
            plugin.reloadConfig();
            ConfigUtil.setConfig(plugin.getConfig());
            plugin.getZoneManager().reloadZones();
            plugin.getTokenManager().reloadTokens();
            sender.sendMessage("§a[RegionTokens] Plugin reloaded successfully!");
            return true;
        } catch (Exception e) {
            sender.sendMessage("§c[RegionTokens] Failed to reload plugin: " + e.getMessage());
            plugin.getLogger().severe("Failed to reload plugin: " + e.getMessage());
            e.printStackTrace();
            return true;
        }
    }

    private void sendHelp(Player player) {
        player.sendMessage(ColorUtil.colorize("&8========== &bRegionTokens Help &8=========="));
        player.sendMessage(ColorUtil.colorize("&7Zone Commands:"));
        player.sendMessage(ColorUtil.colorize("  &b/rgtk zone create <zoneName> &7- Create a zone"));
        player.sendMessage(ColorUtil.colorize("  &b/rgtk zone list &7- List all zones"));
        player.sendMessage(ColorUtil.colorize("  &b/rgtk zone rename <zoneID> <newName> &7- Rename a zone"));
        player.sendMessage(ColorUtil.colorize("  &b/rgtk zone kickto <x> <y> <z> &7- Set kick location"));
        player.sendMessage(ColorUtil.colorize("&7Token Commands:"));
        player.sendMessage(ColorUtil.colorize("  &b/rgtk token create <tokenName> [duration] &7- Create a token"));
        player.sendMessage(ColorUtil.colorize("  &b/rgtk token list &7- List all tokens"));
        player.sendMessage(ColorUtil.colorize("  &b/rgtk token assign <tokenID> <zoneID> &7- Assign token to zone"));
        player.sendMessage(ColorUtil.colorize("  &b/rgtk token duration <tokenID> <duration> &7- Change duration"));
        player.sendMessage(ColorUtil.colorize("  &b/rgtk token item <tokenID> [is-glowing|not-glowing] &7- Configure item"));
        player.sendMessage(ColorUtil.colorize("  &b/rgtk token rename <tokenID> <newName> &7- Rename a token"));
        player.sendMessage(ColorUtil.colorize("  &b/rgtk token lore add|set|remove &7- Manage lore"));
        player.sendMessage(ColorUtil.colorize("&7Other Commands:"));
        player.sendMessage(ColorUtil.colorize("  &b/rgtk tool &7- Get the zone tool"));
        player.sendMessage(ColorUtil.colorize("  &b/rgtk reload &7- Reload plugin configuration"));
        player.sendMessage(ColorUtil.colorize("&8========================================"));
    }
}
