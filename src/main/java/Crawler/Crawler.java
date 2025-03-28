package Crawler;

import java.io.*;
import java.util.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class Crawler {
    private Queue<String> urlsToCrawl = new LinkedList<>(); // Use Queue for BFS crawling
    private Set<String> visited = new HashSet<>();

    public Crawler(String filename) {
        readStartLinks(filename);
        crawl();
    }

    private void readStartLinks(String fileName) {
        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) { // Try-with-resources
            String line;
            while ((line = br.readLine()) != null) {
                urlsToCrawl.add(line); // Use offer() for queue
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
                Document doc = Jsoup.connect(url).get();
                Elements links = doc.select("a[href]"); // Select all anchor tags with href attribute

                for (Element link : links) {
                    String newUrl = link.absUrl("href"); // Convert relative URLs to absolute URLs

                    if (!visited.contains(newUrl) && !newUrl.isEmpty() && newUrl.startsWith("https")) {
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
