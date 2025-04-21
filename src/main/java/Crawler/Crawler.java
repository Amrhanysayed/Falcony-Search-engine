package Crawler;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.Map;
import java.util.Arrays;
import java.util.stream.Collectors;

import dbManager.dbManager;

public class Crawler {
    private final ConcurrentLinkedQueue<String> urlsToCrawl = new ConcurrentLinkedQueue<>();
    private final Set<String> visited = ConcurrentHashMap.newKeySet();
    private final AtomicInteger pageCount = new AtomicInteger(0);
    private final int maxPages = 6000;
    private final RobotsManager robotsM;
    private final ExecutorService executor;
    private final int numThreads = 8;
    private final dbManager mongo ;
    public Crawler() {
        mongo =  new dbManager();
        this.robotsM = new RobotsManager();
        executor = Executors.newFixedThreadPool(numThreads);
    }

    public void startCrawl(String filename) throws Exception {
        readStartLinks(filename);
        crawl();
    }

    protected static String normalizeUrl(String url, String baseUrl) throws Exception {
        if (!url.startsWith("http") && baseUrl != null) {
            Document doc = Jsoup.parse("<a href=\"" + url + "\"></a>", baseUrl);
            url = doc.select("a").first().absUrl("href");
        }
        URI uri = new URI(url).normalize();
        String scheme = uri.getScheme().toLowerCase();
        String host = uri.getHost().toLowerCase();
        int port = uri.getPort();
        if ((port == 80 && "http".equals(scheme)) || (port == 443 && "https".equals(scheme))) {
            port = -1;
        }
        String query = uri.getQuery();
        String newQuery = query;
        if (query != null) {
            Map<String, String> params = Arrays.stream(query.split("&"))
                    .map(p -> p.split("="))
                    .filter(p -> p.length == 2 && !p[0].equals("session"))
                    .collect(Collectors.toMap(p -> p[0], p -> p[1], (v1, v2) -> v1));
            newQuery = params.entrySet().stream()
                    .map(e -> e.getKey() + "=" + e.getValue())
                    .collect(Collectors.joining("&"));
            if (newQuery.isEmpty()) newQuery = null;
        }
        return new URI(scheme, host, uri.getPath(), newQuery, null).toString();
    }

    private void readStartLinks(String fileName) {
        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            String line;
            while ((line = br.readLine()) != null) {
                try {
                    String normalized = normalizeUrl(line, null);
                    if (normalized != null && !normalized.isEmpty()) {
                        robotsM.parseRobots(normalized);
                        urlsToCrawl.add(normalized);
                    }
                } catch (Exception e) {
                    System.err.println("Failed to normalize seed URL: " + line + " - " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading seed file: " + e.getMessage());
        }
        System.out.println("Seed URLs loaded: " + urlsToCrawl.size());
    }

    private void crawl() {
        for (int i = 0; i < numThreads; i++) {
            executor.submit(new CrawlerWorker(urlsToCrawl, visited, pageCount, maxPages, robotsM,mongo));
        }
        executor.shutdown();
        try {
            if (!executor.awaitTermination(1, TimeUnit.HOURS)) {
                executor.shutdownNow();
                System.err.println("Crawl timed out after 1 hour");
            } else {
                System.out.println("Crawl completed successfully");
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            System.err.println("Crawl interrupted: " + e.getMessage());
            Thread.currentThread().interrupt();
        }
    }

    public static void main(String[] args) {
        String fileName = "src/seed.txt";


        Crawler cr = new Crawler();
        try {
            cr.startCrawl(fileName);
            System.out.println("Finished crawling.");
        } catch (Exception e) {
            System.err.println("Crawling failed: " + e.getMessage());
        }
    }

}