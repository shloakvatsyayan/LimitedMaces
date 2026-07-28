package org.bonkmc.limitedmaces.updates;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class GameVersionResolver {
    private static final Pattern VERSION_PREFIX = Pattern.compile(
            "^(\\d+(?:\\.\\d+){1,2})(?=-|\\.build\\.|$)"
    );

    String resolve(String bukkitVersion) {
        if (bukkitVersion == null || bukkitVersion.isBlank()) {
            throw new IllegalStateException("Unable to resolve the Minecraft version");
        }

        Matcher versionMatcher = VERSION_PREFIX.matcher(bukkitVersion);
        if (!versionMatcher.find()) {
            throw new IllegalStateException(
                    "Unable to resolve the Minecraft version from Bukkit version " + bukkitVersion
            );
        }
        return versionMatcher.group(1);
    }
}
