package org.bonkmc.limitedmaces;

import org.bonkmc.limitedmaces.commands.ClearUntrackedMacesCommand;
import org.bonkmc.limitedmaces.commands.GetUntrackedMaceCommand;
import org.bonkmc.limitedmaces.commands.LimitedMacesCommand;
import org.bonkmc.limitedmaces.commands.MacesCommand;
import org.bonkmc.limitedmaces.commands.RemoveMaceCommand;
import org.bonkmc.limitedmaces.listeners.ContainerBlockListener;
import org.bonkmc.limitedmaces.listeners.CraftingListener;
import org.bonkmc.limitedmaces.listeners.InventoryTrackingListener;
import org.bonkmc.limitedmaces.listeners.TrackingListener;
import org.bonkmc.limitedmaces.recipes.RecipeController;
import org.bonkmc.limitedmaces.storage.ConfigManager;
import org.bonkmc.limitedmaces.storage.ConfigMigrator;
import org.bonkmc.limitedmaces.storage.MaceRegistry;
import org.bonkmc.limitedmaces.updates.StartupUpdateNotifier;
import org.bonkmc.limitedmaces.updates.UpdateService;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public final class LimitedMaces extends JavaPlugin {

    private ConfigManager configManager;
    private MaceRegistry maceRegistry;
    private RecipeController recipeController;
    private UpdateService updateService;

    @Override
    public void onEnable() {
        ConfigMigrator migrator = new ConfigMigrator(this);
        migrator.migrateIfNeeded();

        this.configManager = new ConfigManager(this);
        this.configManager.load();

        this.maceRegistry = new MaceRegistry(this);
        this.maceRegistry.load();

        this.recipeController = new RecipeController(this);
        this.updateService = new UpdateService(this);

        Bukkit.getPluginManager().registerEvents(new CraftingListener(this), this);
        Bukkit.getPluginManager().registerEvents(new ContainerBlockListener(this), this);
        Bukkit.getPluginManager().registerEvents(new InventoryTrackingListener(this), this);
        Bukkit.getPluginManager().registerEvents(new TrackingListener(this), this);
        Bukkit.getPluginManager().registerEvents(new StartupUpdateNotifier(this, updateService), this);

        LimitedMacesCommand limitedMacesCommand = new LimitedMacesCommand(this, updateService);
        registerTabCommand("limitedmaces", limitedMacesCommand, limitedMacesCommand);

        MacesCommand macesCommand = new MacesCommand(this);
        registerTabCommand("maces", macesCommand, macesCommand);

        GetUntrackedMaceCommand getUntrackedCommand = new GetUntrackedMaceCommand(this);
        registerCommand("getuntrackedmace", getUntrackedCommand);

        ClearUntrackedMacesCommand clearUntrackedCommand = new ClearUntrackedMacesCommand(this);
        registerCommand("clearuntrackedmaces", clearUntrackedCommand);

        RemoveMaceCommand removeMaceCommand = new RemoveMaceCommand(this);
        registerTabCommand("removemace", removeMaceCommand, removeMaceCommand);

        for (Player player : Bukkit.getOnlinePlayers()) {
            maceRegistry.scanAndNormalizePlayerInventory(player);
        }

        recipeController.syncWithLimit();

        getLogger().info("LimitedMaces enabled. Tracked maces: " + maceRegistry.getActiveCount() +
                " / " + configManager.getAllowedMaces());
    }

    @Override
    public void onDisable() {
        try {
            if (maceRegistry != null) {
                maceRegistry.save();
            }
        } catch (RuntimeException exception) {
            getLogger().warning("Failed to save tracked maces during shutdown: " + exception.getMessage());
        }
        getLogger().info("LimitedMaces disabled.");
    }

    private void registerCommand(String commandName, CommandExecutor executor) {
        PluginCommand command = getCommand(commandName);
        if (command == null) {
            getLogger().warning("Command is missing from plugin.yml: " + commandName);
            return;
        }

        command.setExecutor(executor);
    }

    private void registerTabCommand(String commandName, CommandExecutor executor, TabCompleter completer) {
        PluginCommand command = getCommand(commandName);
        if (command == null) {
            getLogger().warning("Command is missing from plugin.yml: " + commandName);
            return;
        }

        command.setExecutor(executor);
        command.setTabCompleter(completer);
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
