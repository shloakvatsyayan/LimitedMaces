package org.bonkmc.multiMace;

import org.bonkmc.multiMace.commands.MacesCommand;
import org.bonkmc.multiMace.listeners.ContainerBlockListener;
import org.bonkmc.multiMace.listeners.CraftingListener;
import org.bonkmc.multiMace.listeners.TrackingListener;
import org.bonkmc.multiMace.recipes.RecipeController;
import org.bonkmc.multiMace.storage.ConfigManager;
import org.bonkmc.multiMace.storage.MaceRegistry;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class MultiMace extends JavaPlugin {

    private ConfigManager configManager;
    private MaceRegistry maceRegistry;
    private RecipeController recipeController;

    @Override
    public void onEnable() {
        this.configManager = new ConfigManager(this);
        this.configManager.load();

        this.maceRegistry = new MaceRegistry(this);
        this.maceRegistry.load();

        this.recipeController = new RecipeController(this);

        // Listeners
        Bukkit.getPluginManager().registerEvents(new CraftingListener(this), this);
        Bukkit.getPluginManager().registerEvents(new ContainerBlockListener(this), this);
        Bukkit.getPluginManager().registerEvents(new TrackingListener(this), this);

        // Command
        MacesCommand cmd = new MacesCommand(this);
        if (getCommand("maces") != null) {
            getCommand("maces").setExecutor(cmd);
            getCommand("maces").setTabCompleter(cmd);
        }

        // Re-scan online players (useful on /reload or plugin reloads)
        for (Player p : Bukkit.getOnlinePlayers()) {
            maceRegistry.scanAndNormalizePlayerInventory(p);
        }

        // Sync recipes based on current count vs limit
        recipeController.syncWithLimit();

        getLogger().info("MultiMace enabled. Tracked maces: " + maceRegistry.getActiveCount() +
                " / " + configManager.getAllowedMaces());
    }

    @Override
    public void onDisable() {
        try {
            maceRegistry.save();
        } catch (Exception ignored) {}
        getLogger().info("MultiMace disabled.");
    }

    public ConfigManager cfg() {
        return configManager;
    }

    public MaceRegistry registry() {
        return maceRegistry;
    }

    public RecipeController recipes() {
        return recipeController;
    }
}
