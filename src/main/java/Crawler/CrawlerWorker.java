package Crawler;

import java.io.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class CrawlerWorker
    extends Crawler implements Runnable {

  public CrawlerWorker() {
    super();
  }

  @Override
  public void run() {
    System.out.println("Queue size: " + urlsToCrawl.size());
    while (!urlsToCrawl.isEmpty()) {

      System.out.println("Queue size: " + urlsToCrawl.size());

      String url = urlsToCrawl.poll();

      if (url == null || visited.contains(url)) {
        continue;
      }

      System.out.println("Crawling: " + url);
      if (!RobotsM.canCrawl(url)) {
        // Optionally log to MongoDB: { url: url, reason: "Disallowed" }
        System.out.println("Blocked by robots.txt: " + url);
        continue;
      }
      // visited.add(url); // Mark as visited

      try {
        Document doc = Jsoup.connect(url).get(); // returned as HTML
        Elements links = doc.select("a[href]");

        for (Element link : links) {

          String newUrl = link.absUrl("href"); // Get absolute URL

          try {
            newUrl = normalizeUrl(newUrl, url);
            if (newUrl != null && !newUrl.isEmpty() && !visited.contains(newUrl) && RobotsM.canCrawl(newUrl)) {
              urlsToCrawl.add(newUrl); // Add to queue for BFS
              visited.add(newUrl); // Mark as visited
              System.out.println("Added to queue: " + newUrl);
            } else {
              System.out.println("Skipped: " + newUrl);
            }

          } catch (Exception e) {
            System.out.println("Invalid URL: " + newUrl);
          }

        }
      } catch (IOException e) {
        System.err.println("Failed to fetch: " + url);
      }
    }
  }
}
