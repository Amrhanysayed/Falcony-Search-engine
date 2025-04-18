package Crawler;

import java.io.*;
import java.util.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.net.URI;

public class CrawlerWorker
    extends Crawler implements Runnable {

  public CrawlerWorker() {
    super();
  }

  @Override
  public void run() {
    while (!urlsToCrawl.isEmpty()) {

      String url = urlsToCrawl.poll();

      if (url == null || visited.contains(url)) {
        continue;
      }

      System.out.println("Crawling: " + url);
      visited.add(url); // Mark as visited

      try {
        Document doc = Jsoup.connect(url).get(); // returned as HTML
        Elements links = doc.select("a[href]");

        for (Element link : links) {

          String newUrl = link.absUrl("href"); // Get absolute URL

          try {
            newUrl = normalizeUrl(newUrl, url);
            RobotsM.parseRobots(newUrl);
          } catch (Exception e) {
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
