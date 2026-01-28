package com.regiontokens;

import org.bukkit.plugin.java.JavaPlugin;
import com.regiontokens.database.DatabaseManager;
import com.regiontokens.manager.ZoneManager;
import com.regiontokens.manager.TokenManager;
import com.regiontokens.listener.ZoneAccessListener;
import com.regiontokens.listener.ToolListener;
import com.regiontokens.listener.ItemDropListener;
import com.regiontokens.listener.TeleportListener;
import com.regiontokens.command.CommandHandler;
import com.regiontokens.placeholder.PlaceholderIntegration;
import com.regiontokens.task.TokenTimerTask;
import com.regiontokens.util.ConfigUtil;

public class RegionTokensPlugin extends JavaPlugin {
    
    private static RegionTokensPlugin instance;
    private DatabaseManager databaseManager;
    private ZoneManager zoneManager;
    private TokenManager tokenManager;

    @Override
    public void onEnable() {
        instance = this;
        
        // Display startup banner
        getLogger().info("╔════════════════════════════════════════╗");
        getLogger().info("║        RegionTokens Plugin v1.0.0       ║");
        getLogger().info("║                                        ║");
        getLogger().info("║     by Magneticx Developments          ║");
        getLogger().info("║     Developed by ItzSD                 ║");
        getLogger().info("║     GitHub: github.com/itzSD0811       ║");
        getLogger().info("╚════════════════════════════════════════╝");
        
        saveDefaultConfig();
        ConfigUtil.setConfig(getConfig());
        
        databaseManager = new DatabaseManager(this);
        zoneManager = new ZoneManager(this);
        tokenManager = new TokenManager(this);
        
        getServer().getPluginManager().registerEvents(new ZoneAccessListener(this), this);
        getServer().getPluginManager().registerEvents(new ToolListener(this), this);
        getServer().getPluginManager().registerEvents(new ItemDropListener(this), this);
        getServer().getPluginManager().registerEvents(new TeleportListener(this), this);
        
        new CommandHandler(this).registerCommands();
        
        TokenTimerTask timerTask = new TokenTimerTask(this);
        timerTask.start();
        
        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new PlaceholderIntegration(this).register();
            getLogger().info("✓ PlaceholderAPI integration enabled!");
        }
        
        getLogger().info("✓ RegionTokens loaded and ready!");
    }

    @Override
    public void onDisable() {
        if (databaseManager != null) {
            databaseManager.close();
        }
        getLogger().info("✗ RegionTokens disabled!");
    }

    public static RegionTokensPlugin getInstance() {
        return instance;
    }

    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }

    public ZoneManager getZoneManager() {
        return zoneManager;
    }

    public TokenManager getTokenManager() {
        return tokenManager;
    }
}
