package org.bonkmc.limitedmaces.storage;

final class VersionComparator {
    private VersionComparator() {
    }

    static boolean isOlderOrEqual(String version, String targetVersion) {
        return compare(version, targetVersion) <= 0;
    }

    static boolean isNewer(String version, String baselineVersion) {
        return compare(version, baselineVersion) > 0;
    }

    private static int compare(String version, String otherVersion) {
        String[] versionParts = splitVersion(version);
        String[] otherVersionParts = splitVersion(otherVersion);
        int longestLength = Math.max(versionParts.length, otherVersionParts.length);

        for (int index = 0; index < longestLength; index++) {
            int versionPart = index < versionParts.length ? parseVersionPart(versionParts[index]) : 0;
            int otherVersionPart = index < otherVersionParts.length ? parseVersionPart(otherVersionParts[index]) : 0;

            if (versionPart != otherVersionPart) {
                return Integer.compare(versionPart, otherVersionPart);
            }
        }

        return 0;
    }

    private static String[] splitVersion(String version) {
        if (version == null || version.isBlank()) {
            return new String[0];
        }
        return version.split("\\.");
    }

    private static int parseVersionPart(String versionPart) {
        int parsedPart = 0;
        for (int index = 0; index < versionPart.length(); index++) {
            char character = versionPart.charAt(index);
            if (!Character.isDigit(character)) {
                return 0;
            }
            parsedPart = parsedPart * 10 + Character.digit(character, 10);
        }
        return parsedPart;
    }
}
