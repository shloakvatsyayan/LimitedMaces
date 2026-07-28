package org.bonkmc.limitedmaces.updates;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Optional;

final class ModrinthClient {
    private static final URI PROJECT_VERSIONS_URI =
            URI.create("https://api.modrinth.com/v2/project/limitedmaces/version");
    private static final int MAX_RESPONSE_SIZE = 2 * 1024 * 1024;

    private final HttpClient httpClient;
    private final ModrinthReleaseParser releaseParser;
    private final String userAgent;

    ModrinthClient(String pluginVersion) {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
        this.releaseParser = new ModrinthReleaseParser();
        this.userAgent = "LimitedMaces/" + pluginVersion + " (https://modrinth.com/plugin/limitedmaces)";
    }

    Optional<ModrinthRelease> fetchLatest(String gameVersion) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(buildVersionsUri(gameVersion))
                .header("Accept", "application/json")
                .header("User-Agent", userAgent)
                .timeout(Duration.ofSeconds(20))
                .GET()
                .build();

        HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
        String responseBody = readResponse(response, MAX_RESPONSE_SIZE, "Modrinth version lookup");
        return releaseParser.parseLatest(responseBody, gameVersion);
    }

    void download(ModrinthRelease release, Path targetFile) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(release.downloadUri())
                .header("Accept", "application/java-archive")
                .header("User-Agent", userAgent)
                .timeout(Duration.ofMinutes(2))
                .GET()
                .build();

        HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() != 200) {
            response.body().close();
            throw new IOException("Modrinth download returned HTTP " + response.statusCode());
        }

        MessageDigest digest = sha512();
        long downloadedSize;
        try (InputStream responseBody = response.body();
             DigestInputStream digestStream = new DigestInputStream(responseBody, digest);
             OutputStream fileOutput = Files.newOutputStream(
                     targetFile,
                     StandardOpenOption.TRUNCATE_EXISTING,
                     StandardOpenOption.WRITE
             )) {
            downloadedSize = digestStream.transferTo(fileOutput);
        }

        if (downloadedSize != release.expectedSize()) {
            throw new IOException("Downloaded size mismatch: expected "
                    + release.expectedSize() + " bytes but received " + downloadedSize);
        }

        String downloadedHash = HexFormat.of().formatHex(digest.digest());
        if (!downloadedHash.equalsIgnoreCase(release.sha512())) {
            throw new IOException("Downloaded SHA-512 checksum does not match Modrinth");
        }
    }

    private URI buildVersionsUri(String gameVersion) {
        String gameVersionFilter = URLEncoder.encode(
                "[\"" + gameVersion + "\"]",
                StandardCharsets.UTF_8
        );
        return URI.create(PROJECT_VERSIONS_URI
                + "?game_versions=" + gameVersionFilter
                + "&include_changelog=false");
    }

    private String readResponse(
            HttpResponse<InputStream> response,
            int maximumBytes,
            String requestName
    ) throws IOException {
        if (response.statusCode() != 200) {
            response.body().close();
            throw new IOException(requestName + " returned HTTP " + response.statusCode());
        }

        byte[] responseBytes;
        try (InputStream responseBody = response.body()) {
            responseBytes = responseBody.readNBytes(maximumBytes + 1);
        }
        if (responseBytes.length > maximumBytes) {
            throw new IOException(requestName + " response exceeded " + maximumBytes + " bytes");
        }
        return new String(responseBytes, StandardCharsets.UTF_8);
    }

    private MessageDigest sha512() {
        try {
            return MessageDigest.getInstance("SHA-512");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-512 is unavailable", exception);
        }
    }
}
