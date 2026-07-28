package org.bonkmc.limitedmaces.updates;

import org.bonkmc.limitedmaces.storage.VersionComparator;

import java.util.Optional;

public final class UpdateStatus {
    private final String currentVersion;
    private final String gameVersion;
    private final ModrinthRelease latestRelease;

    UpdateStatus(String currentVersion, String gameVersion, ModrinthRelease latestRelease) {
        this.currentVersion = currentVersion;
        this.gameVersion = gameVersion;
        this.latestRelease = latestRelease;
    }

    public String currentVersion() {
        return currentVersion;
    }

    public String gameVersion() {
        return gameVersion;
    }

    public Optional<String> latestVersion() {
        return Optional.ofNullable(latestRelease).map(ModrinthRelease::versionNumber);
    }

    public boolean isUpdateAvailable() {
        return latestRelease != null
                && VersionComparator.isNewer(latestRelease.versionNumber(), currentVersion);
    }

    ModrinthRelease latestRelease() {
        if (latestRelease == null) {
            throw new IllegalStateException("No compatible Modrinth release is available");
        }
        return latestRelease;
    }
}
