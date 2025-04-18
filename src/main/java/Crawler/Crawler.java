package Crawler;

import java.io.*;
import java.util.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.net.URI;

import java.util.stream.Collectors;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Crawler {
    protected final ConcurrentLinkedQueue<String> urlsToCrawl = new ConcurrentLinkedQueue<>(); // Use Queue for BFS
    protected static final Set<String> visited = ConcurrentHashMap.<String>newKeySet();
    RobotsManager RobotsM;
    ExecutorService executor;
    int numThreads = 8;

    public Crawler(String filename) {
        RobotsM = new RobotsManager();
        readStartLinks(filename);
        executor = Executors.newFixedThreadPool(numThreads); // thread pool
        crawl();

    }

    public Crawler() {
    }

    protected static String normalizeUrl(String url, String baseUrl) throws Exception {

        // Step 1: Resolve relative URL (if needed)
        if (!url.startsWith("http")) {
            Document doc = Jsoup.parse("<a href=\"" + url + "\"></a>", baseUrl);
            url = doc.select("a").first().absUrl("href");
        }

        // Step 2: Parse and normalize path
        URI uri = new URI(url).normalize();

        // Step 3: Lowercase scheme and host
        String scheme = uri.getScheme().toLowerCase();
        String host = uri.getHost().toLowerCase();

        // Step 4: Remove default ports
        int port = uri.getPort();
        if ((port == 80 && "http".equals(scheme)) || (port == 443 && "https".equals(scheme))) {
            port = -1;
        }

        // Step 5: Handle query parameters (example: remove "session")
        String query = uri.getQuery();
        String newQuery = query;
        if (query != null) {
            Map<String, String> params = Arrays.stream(query.split("&"))
                    .map(p -> p.split("="))
                    .filter(p -> !p[0].equals("session"))
                    .collect(Collectors.toMap(p -> p[0], p -> p[1]));
            newQuery = params.entrySet().stream()
                    .map(e -> e.getKey() + "=" + e.getValue())
                    .collect(Collectors.joining("&"));
            if (newQuery.isEmpty())
                newQuery = null;
        }

        // Step 6: Remove fragment and rebuild
        return new URI(scheme, host, uri.getPath(), newQuery, null).toString();

    }

    private void readStartLinks(String fileName) {
        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) { // Try-with-resources
            String line;
            while ((line = br.readLine()) != null) {

                try {
                    line = normalizeUrl(line, null);
                    RobotsM.parseRobots(line);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

                urlsToCrawl.add(line); // add to the queue
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void crawl() {

        for (int i = 0; i < numThreads; i++) {
            executor.submit(new CrawlerWorker());
        }
    }

}
