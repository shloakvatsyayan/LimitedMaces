package org.bonkmc.limitedmaces.updates;

import java.net.URI;
import java.time.Instant;
import java.util.Objects;

record ModrinthRelease(
        String versionNumber,
        URI downloadUri,
        String filename,
        long expectedSize,
        String sha512,
        Instant publishedAt
) {
    ModrinthRelease {
        Objects.requireNonNull(versionNumber, "versionNumber");
        Objects.requireNonNull(downloadUri, "downloadUri");
        Objects.requireNonNull(filename, "filename");
        Objects.requireNonNull(sha512, "sha512");
        Objects.requireNonNull(publishedAt, "publishedAt");

        if (versionNumber.isBlank()) {
            throw new IllegalArgumentException("Version number cannot be blank");
        }
        if (!"https".equalsIgnoreCase(downloadUri.getScheme())) {
            throw new IllegalArgumentException("Download URL must use HTTPS");
        }
        if (!filename.toLowerCase().endsWith(".jar")) {
            throw new IllegalArgumentException("Download file must be a jar");
        }
        if (expectedSize <= 0) {
            throw new IllegalArgumentException("Download size must be positive");
        }
        if (!sha512.matches("[0-9a-fA-F]{128}")) {
            throw new IllegalArgumentException("Invalid SHA-512 checksum");
        }
    }
}
