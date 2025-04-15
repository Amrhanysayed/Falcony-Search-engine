package Crawler;

import java.io.*;
import java.util.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.net.URI;
import java.util.stream.Collectors;

import Crawler.RobotsManager;
public class Crawler {
    private final Queue<String> urlsToCrawl = new LinkedList<>(); // Use Queue for BFS crawling
    private final Set<String> visited = new HashSet<>();
    RobotsManager RobotsM;
    public Crawler(String filename) {
        readStartLinks(filename);
        crawl();
        RobotsM = new RobotsManager();
    }
    private static String normalizeUrl(String url, String baseUrl) throws Exception{

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
            if (newQuery.isEmpty()) newQuery = null;
        }

        // Step 6: Remove fragment and rebuild
        return new URI(scheme, host, uri.getPath(), newQuery, null).toString();

    }

    private void readStartLinks(String fileName) {
        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) { // Try-with-resources
            String line;
            while ((line = br.readLine()) != null) {

                try{
                    line=normalizeUrl(line,null);
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
        while (!urlsToCrawl.isEmpty()) { // Keep crawling while there are URLs in queue
            String url = urlsToCrawl.poll(); // Get and remove the first URL

            if (url == null || visited.contains(url)) {
                continue; // Skip if URL is null or already visited
            }

            System.out.println("Crawling: " + url);
            visited.add(url); // Mark as visited

            try {
                Document doc = Jsoup.connect(url).get(); //  returned as HTML
                Elements links = doc.select("a[href]"); // Select all anchor tags with href attribute

                for (Element link : links) {

                    String newUrl = link.absUrl("href"); // Convert relative URLs to absolute URLs
                    try{
                        newUrl=normalizeUrl(newUrl,url);
                        RobotsM.parseRobots(newUrl);
                    }catch (Exception e){
                        System.err.println("Failed to normalize : " + url);
                    }

                    if (!visited.contains(newUrl) && !newUrl.isEmpty()) {
                        System.out.println("Found: " + newUrl);
                        urlsToCrawl.add(newUrl); // Add to queue for further crawling
                    }
                }
            } catch (IOException e) {
                System.err.println("Failed to fetch: " + url);
            }
        }
    }
}
