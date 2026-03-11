package org.bonkmc.limitedmaces;

import org.bonkmc.limitedmaces.commands.ClearUntrackedMacesCommand;
import org.bonkmc.limitedmaces.commands.GetUntrackedMaceCommand;
import org.bonkmc.limitedmaces.commands.MacesCommand;
import org.bonkmc.limitedmaces.commands.RemoveMaceCommand;
import org.bonkmc.limitedmaces.listeners.ContainerBlockListener;
import org.bonkmc.limitedmaces.listeners.CraftingListener;
import org.bonkmc.limitedmaces.listeners.TrackingListener;
import org.bonkmc.limitedmaces.recipes.RecipeController;
import org.bonkmc.limitedmaces.storage.ConfigManager;
import org.bonkmc.limitedmaces.storage.ConfigMigrator;
import org.bonkmc.limitedmaces.storage.MaceRegistry;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class LimitedMaces extends JavaPlugin {

    private ConfigManager configManager;
    private MaceRegistry maceRegistry;
    private RecipeController recipeController;

    @Override
    public void onEnable() {
        ConfigMigrator migrator = new ConfigMigrator(this);
        migrator.migrateIfNeeded();

        this.configManager = new ConfigManager(this);
        this.configManager.load();

        this.maceRegistry = new MaceRegistry(this);
        this.maceRegistry.load();

        this.recipeController = new RecipeController(this);

        Bukkit.getPluginManager().registerEvents(new CraftingListener(this), this);
        Bukkit.getPluginManager().registerEvents(new ContainerBlockListener(this), this);
        Bukkit.getPluginManager().registerEvents(new TrackingListener(this), this);

        MacesCommand cmd = new MacesCommand(this);
        if (getCommand("maces") != null) {
            getCommand("maces").setExecutor(cmd);
            getCommand("maces").setTabCompleter(cmd);
        }

        GetUntrackedMaceCommand getUntrackedCmd = new GetUntrackedMaceCommand(this);
        if (getCommand("getuntrackedmace") != null) {
            getCommand("getuntrackedmace").setExecutor(getUntrackedCmd);
        }

        ClearUntrackedMacesCommand clearUntrackedCmd = new ClearUntrackedMacesCommand(this);
        if (getCommand("clearuntrackedmaces") != null) {
            getCommand("clearuntrackedmaces").setExecutor(clearUntrackedCmd);
        }

        RemoveMaceCommand removeMaceCmd = new RemoveMaceCommand(this);
        if (getCommand("removemace") != null) {
            getCommand("removemace").setExecutor(removeMaceCmd);
            getCommand("removemace").setTabCompleter(removeMaceCmd);
        }

        for (Player p : Bukkit.getOnlinePlayers()) {
            maceRegistry.scanAndNormalizePlayerInventory(p);
        }

        recipeController.syncWithLimit();

        getLogger().info("LimitedMaces enabled. Tracked maces: " + maceRegistry.getActiveCount() +
                " / " + configManager.getAllowedMaces());
    }

    @Override
    public void onDisable() {
        try {
            maceRegistry.save();
        } catch (Exception ignored) {}
        getLogger().info("LimitedMaces disabled.");
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
