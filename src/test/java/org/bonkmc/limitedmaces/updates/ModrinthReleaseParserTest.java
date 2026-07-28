package org.bonkmc.limitedmaces.updates;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ModrinthReleaseParserTest {
    private final ModrinthReleaseParser releaseParser = new ModrinthReleaseParser();

    @Test
    void selectsNewestPublishedReleaseSupportingExactGameVersion() {
        String responseBody = "["
                + release("9.0.0", "2026-07-30T00:00:00Z", "[\"26.3\"]")
                + ","
                + release("2.0.2", "2026-07-28T00:00:00Z", "[\"26.2\"]")
                + ","
                + release("2.1.0", "2026-07-29T00:00:00Z", "[\"1.21.11\", \"26.2\"]")
                + "]";

        Optional<ModrinthRelease> latestRelease = releaseParser.parseLatest(responseBody, "26.2");

        assertEquals("2.1.0", latestRelease.orElseThrow().versionNumber());
    }

    @Test
    void rejectsVersionsThatOnlyPartiallyMatchGameVersion() {
        String responseBody = "[" + release(
                "2.0.2",
                "2026-07-28T00:00:00Z",
                "[\"1.21.10\"]"
        ) + "]";

        assertTrue(releaseParser.parseLatest(responseBody, "1.21.1").isEmpty());
    }

    private String release(String versionNumber, String publishedAt, String gameVersions) {
        return """
                {
                  "status": "listed",
                  "version_number": "%s",
                  "date_published": "%s",
                  "game_versions": %s,
                  "files": [{
                    "url": "https://cdn.modrinth.com/LimitedMaces-%s.jar",
                    "filename": "LimitedMaces-%s.jar",
                    "primary": true,
                    "size": 1024,
                    "hashes": {"sha512": "%s"}
                  }]
                }
                """.formatted(
                versionNumber,
                publishedAt,
                gameVersions,
                versionNumber,
                versionNumber,
                "a".repeat(128)
        );
    }
}
