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

    public static byte[] downloadImage(String url) throws IOException, InterruptedException {
        // Configure HttpClient with redirect handling and timeout
        HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.ALWAYS) // Follow redirects
                .connectTimeout(Duration.ofSeconds(10)) // Set connection timeout
                .build();

        // Build request with User-Agent header
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36")
                .timeout(Duration.ofSeconds(10)) // Set request timeout
                .build();

        // Send request and get response
        HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());

        // Check response status
        if (response.statusCode() != HttpURLConnection.HTTP_OK) {
            throw new IOException("Failed to download image. HTTP status: " + response.statusCode());
        }

        return response.body();
    }

}
