package org.bonkmc.limitedmaces.commands;

import org.bonkmc.limitedmaces.LimitedMaces;
import org.bonkmc.limitedmaces.updates.UpdateStatus;

import java.util.ArrayList;
import java.util.List;

final class LimitedMacesInfoFormatter {
    List<String> format(LimitedMaces plugin, UpdateStatus updateStatus) {
        List<String> messages = new ArrayList<>();
        String latestVersion = updateStatus.latestVersion().orElse("None available");

        messages.add("&6&lLimitedMaces");
        messages.add("&ePlugin: &f" + plugin.getName());
        messages.add("&eCurrent version: &f" + updateStatus.currentVersion());
        messages.add("&eLatest version for Minecraft " + updateStatus.gameVersion() + ": &f" + latestVersion);
        messages.add("&eUpdate available: " + (updateStatus.isUpdateAvailable() ? "&aYes" : "&cNo"));

        if (updateStatus.isUpdateAvailable()) {
            messages.add("&eA new LimitedMaces version is available.");
            messages.add("&7New versions often contain bug fixes and new features.");
            messages.add("&7Update with &f/limitedmaces update&7 or get the latest version from Modrinth:");
            messages.add("&fhttps://modrinth.com/plugin/limitedmaces");
        }

        return messages;
    }

    List<String> formatUnavailable(LimitedMaces plugin, String gameVersion, String failureMessage) {
        return List.of(
                "&6&lLimitedMaces",
                "&ePlugin: &f" + plugin.getName(),
                "&eCurrent version: &f" + plugin.getDescription().getVersion(),
                "&eLatest version for Minecraft " + gameVersion + ": &cUnavailable",
                "&eUpdate available: &7Unknown",
                "&cUnable to check Modrinth: " + failureMessage
        );
    }
}
