package com.regiontokens.command;

import com.regiontokens.RegionTokensPlugin;
import com.regiontokens.util.ColorUtil;
import com.regiontokens.util.ConfigUtil;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class ToolCommand {
    private final RegionTokensPlugin plugin;

    public ToolCommand(RegionTokensPlugin plugin) {
        this.plugin = plugin;
    }

    public boolean execute(Player player, String[] args) {
        if (!player.hasPermission("regiontokens.admin.tool")) {
            player.sendMessage(ConfigUtil.getMessage("error.no-permission"));
            return true;
        }

        ItemStack tool = createZoneTool();
        player.getInventory().addItem(tool);
        player.sendMessage(ConfigUtil.getMessage("tool.given"));

        return true;
    }

    private ItemStack createZoneTool() {
        ItemStack tool = new ItemStack(Material.SHEARS);
        ItemMeta meta = tool.getItemMeta();
        
        if (meta != null) {
            meta.setDisplayName(ColorUtil.colorize("&bZone Tool"));
            meta.setCustomModelData(1);
            tool.setItemMeta(meta);
        }

        return tool;
    }
}
