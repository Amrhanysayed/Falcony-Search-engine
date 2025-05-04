package Utils;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Set;
import java.util.regex.Pattern;

public class Utils {
    public static final Pattern CLEAN_PATTERN = Pattern.compile("[^a-zA-Z0-9 ]");
    public static final Set<String> STOP_WORDS = StopWords.getStopWords();
    private static final long MAX_IMAGE_SIZE = 2_097_152; // 2MB in bytes

    public static byte[] downloadImage(String url) throws IOException, InterruptedException {
        // Configure HttpClient with redirect handling and timeout
        HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.ALWAYS)
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        // Step 1: Send HEAD request to check Content-Length
        HttpRequest headRequest = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .method("HEAD", HttpRequest.BodyPublishers.noBody())
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                .timeout(Duration.ofSeconds(10))
                .build();

        try {
            HttpResponse<Void> headResponse = client.send(headRequest, HttpResponse.BodyHandlers.discarding());
            if (headResponse.statusCode() == HttpURLConnection.HTTP_OK) {
                String contentLength = headResponse.headers().firstValue("Content-Length").orElse("");
                if (!contentLength.isEmpty()) {
                    try {
                        long size = Long.parseLong(contentLength);
                        if (size > MAX_IMAGE_SIZE) {
                            throw new IOException("Image size (" + size + " bytes) exceeds 2MB limit");
                        }
                    } catch (NumberFormatException e) {
                        // Content-Length header is invalid, proceed with download
                    }
                }
            }
        } catch (IOException | InterruptedException e) {
            // HEAD request failed, proceed with full download as fallback
        }

        // Step 2: Send GET request to download image
        HttpRequest getRequest = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                .timeout(Duration.ofSeconds(10))
                .build();

        HttpResponse<byte[]> response = client.send(getRequest, HttpResponse.BodyHandlers.ofByteArray());

        // Check response status
        if (response.statusCode() != HttpURLConnection.HTTP_OK) {
            throw new IOException("Failed to download image. HTTP status: " + response.statusCode());
        }

        // Optional: Double-check size after download
        byte[] imageData = response.body();
        if (imageData.length > MAX_IMAGE_SIZE) {
            throw new IOException("Downloaded image size (" + imageData.length + " bytes) exceeds 2MB limit");
        }

        return imageData;
    }

}
