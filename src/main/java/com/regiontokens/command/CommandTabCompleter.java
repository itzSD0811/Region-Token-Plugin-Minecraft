package com.regiontokens.command;

import com.regiontokens.RegionTokensPlugin;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class CommandTabCompleter implements TabCompleter {
    private final RegionTokensPlugin plugin;

    public CommandTabCompleter(RegionTokensPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 0) {
            return Collections.emptyList();
        }

        if (args.length == 1) {
            List<String> subcommands = new ArrayList<>();
            if (sender.hasPermission("regiontokens.admin.zones.create") ||
                sender.hasPermission("regiontokens.admin.zones.list") ||
                sender.hasPermission("regiontokens.admin.zones.rename") ||
                sender.hasPermission("regiontokens.admin.zones.kickto")) {
                subcommands.add("zone");
            }
            if (sender.hasPermission("regiontokens.admin.token.create") ||
                sender.hasPermission("regiontokens.admin.tokens.rename")) {
                subcommands.add("token");
            }
            if (sender.hasPermission("regiontokens.admin.tool")) {
                subcommands.add("tool");
            }
            subcommands.add("help");

            return StringUtil.copyPartialMatches(args[0], subcommands, new ArrayList<>());
        }

        if (args[0].equalsIgnoreCase("zone")) {
            return handleZoneTabComplete(sender, args);
        } else if (args[0].equalsIgnoreCase("token")) {
            return handleTokenTabComplete(sender, args);
        } else if (args[0].equalsIgnoreCase("tool")) {
            return Collections.emptyList();
        }

        return Collections.emptyList();
    }

    private List<String> handleZoneTabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            List<String> actions = new ArrayList<>();
            if (sender.hasPermission("regiontokens.admin.zones.create")) {
                actions.add("create");
            }
            if (sender.hasPermission("regiontokens.admin.zones.list")) {
                actions.add("list");
            }
            if (sender.hasPermission("regiontokens.admin.zones.rename")) {
                actions.add("rename");
            }
            if (sender.hasPermission("regiontokens.admin.zones.kickto")) {
                actions.add("kickto");
            }
            if (sender.hasPermission("regiontokens.admin.zones.delete")) {
                actions.add("delete");
            }
            if (sender.hasPermission("regiontokens.admin.zones.show")) {
                actions.add("show");
            }
            return StringUtil.copyPartialMatches(args[1], actions, new ArrayList<>());
        }

        if (args.length >= 3) {
            String action = args[1].toLowerCase();

            if (action.equals("rename") || action.equals("delete") || action.equals("show")) {
                if (args.length == 3) {
                    List<String> zoneIds = plugin.getZoneManager().getAllZones().keySet().stream()
                        .collect(Collectors.toList());
                    return StringUtil.copyPartialMatches(args[2], zoneIds, new ArrayList<>());
                }
            }

            if (action.equals("kickto")) {
                if (args.length == 3) {
                    // Suggest zone IDs
                    List<String> zoneIds = plugin.getZoneManager().getAllZones().keySet().stream()
                        .collect(Collectors.toList());
                    return StringUtil.copyPartialMatches(args[2], zoneIds, new ArrayList<>());
                } else if (args.length == 4) {
                    // Suggest "facing" or coordinate
                    return StringUtil.copyPartialMatches(args[3], 
                        List.of("facing"), new ArrayList<>());
                }
            }

            if (action.equals("list") && args.length == 3) {
                return StringUtil.copyPartialMatches(args[2], 
                    List.of("1", "2", "3", "4", "5"), new ArrayList<>());
            }
        }

        return Collections.emptyList();
    }

    private List<String> handleTokenTabComplete(CommandSender sender, String[] args) {
        if (args.length == 2) {
            List<String> actions = new ArrayList<>();
            if (sender.hasPermission("regiontokens.admin.token.create")) {
                actions.add("create");
                actions.add("list");
                actions.add("assign");
                actions.add("duration");
                actions.add("item");
            }
            if (sender.hasPermission("regiontokens.admin.tokens.rename")) {
                actions.add("rename");
            }
            if (sender.hasPermission("regiontokens.admin.tokens.lore")) {
                actions.add("lore");
            }
            if (sender.hasPermission("regiontokens.admin.token.give")) {
                actions.add("give");
            }
            if (sender.hasPermission("regiontokens.admin.token.delete")) {
                actions.add("delete");
            }
            return StringUtil.copyPartialMatches(args[1], actions, new ArrayList<>());
        }

        if (args.length >= 3) {
            String action = args[1].toLowerCase();

            if (action.equals("list") && args.length == 3) {
                return StringUtil.copyPartialMatches(args[2], 
                    List.of("1", "2", "3", "4", "5"), new ArrayList<>());
            }

            if ((action.equals("assign") || action.equals("duration") || 
                 action.equals("item") || action.equals("rename")) && args.length == 3) {
                List<String> tokenIds = plugin.getTokenManager().getAllTokens().keySet().stream()
                    .collect(Collectors.toList());
                return StringUtil.copyPartialMatches(args[2], tokenIds, new ArrayList<>());
            }

            if (action.equals("delete") && args.length == 3) {
                List<String> tokenIds = plugin.getTokenManager().getAllTokens().keySet().stream()
                    .collect(Collectors.toList());
                return StringUtil.copyPartialMatches(args[2], tokenIds, new ArrayList<>());
            }

            if (action.equals("give") && args.length == 3) {
                List<String> tokenIds = plugin.getTokenManager().getAllTokens().keySet().stream()
                    .collect(Collectors.toList());
                return StringUtil.copyPartialMatches(args[2], tokenIds, new ArrayList<>());
            }

            if (action.equals("give") && args.length == 4) {
                List<String> playerNames = Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .collect(Collectors.toList());
                return StringUtil.copyPartialMatches(args[3], playerNames, new ArrayList<>());
            }

            if (action.equals("assign") && args.length == 4) {
                List<String> zoneIds = plugin.getZoneManager().getAllZones().keySet().stream()
                    .collect(Collectors.toList());
                return StringUtil.copyPartialMatches(args[3], zoneIds, new ArrayList<>());
            }

            if (action.equals("duration") && args.length == 4) {
                List<String> durations = List.of("1h", "30m", "15m", "10m", "5m", "1m", "30s", "1s");
                return StringUtil.copyPartialMatches(args[3], durations, new ArrayList<>());
            }

            if (action.equals("item") && args.length == 4) {
                List<String> options = List.of("is-glowing", "not-glowing");
                return StringUtil.copyPartialMatches(args[3], options, new ArrayList<>());
            }

            if (action.equals("lore") && args.length == 3) {
                List<String> loreActions = List.of("add", "set", "remove");
                return StringUtil.copyPartialMatches(args[2], loreActions, new ArrayList<>());
            }

            if (action.equals("lore") && args.length == 4) {
                List<String> tokenIds = plugin.getTokenManager().getAllTokens().keySet().stream()
                    .collect(Collectors.toList());
                return StringUtil.copyPartialMatches(args[3], tokenIds, new ArrayList<>());
            }
        }

        return Collections.emptyList();
    }
}
