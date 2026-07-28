package org.bonkmc.limitedmaces.updates;

import org.bonkmc.limitedmaces.updates.json.JsonParser;

import java.math.BigDecimal;
import java.net.URI;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

final class ModrinthReleaseParser {
    Optional<ModrinthRelease> parseLatest(String responseBody, String gameVersion) {
        Object parsedResponse = JsonParser.parse(responseBody);
        if (!(parsedResponse instanceof List<?> versions)) {
            throw new IllegalArgumentException("Modrinth response must be an array");
        }

        return versions.stream()
                .map(version -> parseRelease(version, gameVersion))
                .flatMap(Optional::stream)
                .max(Comparator.comparing(ModrinthRelease::publishedAt));
    }

    private Optional<ModrinthRelease> parseRelease(Object rawVersion, String gameVersion) {
        Map<?, ?> version = requireMap(rawVersion, "version");
        if (!"listed".equals(requireString(version, "status"))) {
            return Optional.empty();
        }
        if (!containsString(requireList(version, "game_versions"), gameVersion)) {
            return Optional.empty();
        }

        Map<?, ?> file = selectFile(requireList(version, "files"));
        Map<?, ?> hashes = requireMap(file.get("hashes"), "file hashes");
        String downloadUrl = requireString(file, "url");

        return Optional.of(new ModrinthRelease(
                requireString(version, "version_number"),
                URI.create(downloadUrl),
                requireString(file, "filename"),
                requireLong(file, "size"),
                requireString(hashes, "sha512"),
                Instant.parse(requireString(version, "date_published"))
        ));
    }

    private Map<?, ?> selectFile(List<?> files) {
        Map<?, ?> firstJar = null;
        for (Object rawFile : files) {
            Map<?, ?> file = requireMap(rawFile, "version file");
            if (Boolean.TRUE.equals(file.get("primary"))) {
                return file;
            }
            if (firstJar == null && requireString(file, "filename").toLowerCase().endsWith(".jar")) {
                firstJar = file;
            }
        }
        if (firstJar == null) {
            throw new IllegalArgumentException("Modrinth version has no jar file");
        }
        return firstJar;
    }

    private boolean containsString(List<?> strings, String expected) {
        return strings.stream().anyMatch(expected::equals);
    }

    private Map<?, ?> requireMap(Object rawMap, String fieldName) {
        if (rawMap instanceof Map<?, ?> parsedMap) {
            return parsedMap;
        }
        throw new IllegalArgumentException("Modrinth " + fieldName + " must be an object");
    }

    private List<?> requireList(Map<?, ?> object, String fieldName) {
        Object rawList = object.get(fieldName);
        if (rawList instanceof List<?> parsedList) {
            return parsedList;
        }
        throw new IllegalArgumentException("Modrinth field '" + fieldName + "' must be an array");
    }

    private String requireString(Map<?, ?> object, String fieldName) {
        Object rawString = object.get(fieldName);
        if (rawString instanceof String parsedString && !parsedString.isBlank()) {
            return parsedString;
        }
        throw new IllegalArgumentException("Modrinth field '" + fieldName + "' must be a string");
    }

    private long requireLong(Map<?, ?> object, String fieldName) {
        Object rawNumber = object.get(fieldName);
        if (rawNumber instanceof BigDecimal parsedNumber) {
            return parsedNumber.longValueExact();
        }
        throw new IllegalArgumentException("Modrinth field '" + fieldName + "' must be a number");
    }
}
