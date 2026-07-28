package org.bonkmc.limitedmaces.commands;

import org.bonkmc.limitedmaces.LimitedMaces;
import org.bonkmc.limitedmaces.updates.UpdateService;
import org.bonkmc.limitedmaces.updates.UpdateStatus;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public final class LimitedMacesCommand implements CommandExecutor, TabCompleter {
    private final LimitedMaces plugin;
    private final UpdateService updateService;
    private final LimitedMacesInfoFormatter infoFormatter;
    private final AtomicBoolean isUpdateRunning;

    public LimitedMacesCommand(LimitedMaces plugin, UpdateService updateService) {
        this.plugin = plugin;
        this.updateService = updateService;
        this.infoFormatter = new LimitedMacesInfoFormatter();
        this.isUpdateRunning = new AtomicBoolean();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String subcommand = args.length == 0 ? "info" : args[0].toLowerCase(Locale.ROOT);
        if (args.length > 1) {
            sender.sendMessage(plugin.cfg().msg("limitedmaces-usage"));
            return true;
        }

        switch (subcommand) {
            case "info" -> showInfo(sender);
            case "reload" -> reloadConfig(sender);
            case "update" -> updatePlugin(sender);
            default -> sender.sendMessage(plugin.cfg().msg("limitedmaces-usage"));
        }
        return true;
    }

    private void showInfo(CommandSender sender) {
        if (!sender.hasPermission("limitedmaces.info")) {
            sender.sendMessage(plugin.cfg().msg("no-permission"));
            return;
        }

        sender.sendMessage(plugin.cfg().color("&7Checking Modrinth for the latest compatible version..."));
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                sendInfo(sender, infoFormatter.format(plugin, updateService.check()));
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                sendInfo(sender, infoFormatter.formatUnavailable(
                        plugin,
                        updateService.gameVersion(),
                        "Update check was interrupted"
                ));
            } catch (IOException | RuntimeException exception) {
                sendInfo(sender, infoFormatter.formatUnavailable(
                        plugin,
                        updateService.gameVersion(),
                        failureMessage(exception)
                ));
            }
        });
    }

    private void reloadConfig(CommandSender sender) {
        if (!sender.hasPermission("limitedmaces.reload")) {
            sender.sendMessage(plugin.cfg().msg("no-permission"));
            return;
        }

        plugin.cfg().reload();
        plugin.recipes().syncWithLimit();
        sender.sendMessage(plugin.cfg().msg("reload"));
    }

    private void updatePlugin(CommandSender sender) {
        if (!sender.hasPermission("limitedmaces.update")) {
            sender.sendMessage(plugin.cfg().msg("no-permission"));
            return;
        }
        if (!isUpdateRunning.compareAndSet(false, true)) {
            sender.sendMessage(plugin.cfg().msg("update-in-progress"));
            return;
        }

        sender.sendMessage(plugin.cfg().msg("update-checking"));
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> runUpdate(sender));
    }

    private void runUpdate(CommandSender sender) {
        try {
            UpdateStatus updateStatus = updateService.check();
            if (updateStatus.latestVersion().isEmpty()) {
                sendConfigured(sender, "update-no-compatible-version", Map.of(
                        "%game_version%", updateStatus.gameVersion()
                ));
                return;
            }
            if (!updateStatus.isUpdateAvailable()) {
                sendConfigured(sender, "update-up-to-date", Map.of(
                        "%version%", updateStatus.currentVersion()
                ));
                return;
            }

            String latestVersion = updateStatus.latestVersion().orElseThrow();
            sendConfigured(sender, "update-downloading", Map.of("%version%", latestVersion));
            updateService.installLatest(updateStatus);
            sendConfigured(sender, "update-success", Map.of("%version%", latestVersion));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            sendConfigured(sender, "update-failed", Map.of("%error%", "Update was interrupted"));
        } catch (IOException | RuntimeException exception) {
            sendConfigured(sender, "update-failed", Map.of("%error%", failureMessage(exception)));
        } finally {
            isUpdateRunning.set(false);
        }
    }

    private void sendConfigured(CommandSender sender, String messageKey, Map<String, String> placeholders) {
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (canReceiveMessage(sender)) {
                sender.sendMessage(plugin.cfg().msg(messageKey, placeholders));
            }
        });
    }

    private void sendInfo(CommandSender sender, List<String> messages) {
        plugin.getServer().getScheduler().runTask(plugin, () -> {
            if (!canReceiveMessage(sender)) {
                return;
            }
            for (String message : messages) {
                sender.sendMessage(plugin.cfg().color(message));
            }
        });
    }

    private boolean canReceiveMessage(CommandSender sender) {
        return !(sender instanceof Player player) || player.isOnline();
    }

    private String failureMessage(Exception exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank() ? exception.getClass().getSimpleName() : message;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length != 1) {
            return Collections.emptyList();
        }

        String partial = args[0].toLowerCase(Locale.ROOT);
        List<String> completions = new ArrayList<>();
        addCompletion(completions, partial, "info", sender.hasPermission("limitedmaces.info"));
        addCompletion(completions, partial, "reload", sender.hasPermission("limitedmaces.reload"));
        addCompletion(completions, partial, "update", sender.hasPermission("limitedmaces.update"));
        return completions;
    }

    private void addCompletion(List<String> completions, String partial, String option, boolean hasPermission) {
        if (hasPermission && option.startsWith(partial)) {
            completions.add(option);
        }
    }
}
