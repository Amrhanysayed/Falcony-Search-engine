package Crawler;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class RobotsManager {

    private final ConcurrentHashMap<String, Set<String>> robots = new ConcurrentHashMap<>();

    public RobotsManager() {
    }

    public void parseRobots(String url) {
        try {
            URI uri = new URI(url);
            String domain = uri.getHost();
            if (domain == null) return;

            // Only parse if not already present, using computeIfAbsent for atomicity
            robots.computeIfAbsent(domain, k -> {
                try {
                    String robotsURL = uri.getScheme() + "://" + domain + "/robots.txt";
                    Document robotsFile = Jsoup.connect(robotsURL)
                            .timeout(5000) // 5s timeout
                            .ignoreHttpErrors(true) // Handle 404s gracefully
                            .get();
                    Set<String> disallowed = parseRobotsText(robotsFile);
                    // Return an unmodifiable set to prevent external modification
                    return Collections.unmodifiableSet(disallowed);
                } catch (IOException e) {
                    System.err.println("Failed to fetch robots.txt for " + domain + ": " + e.getMessage());
                    // Return empty set if robots.txt fails
                    return Collections.emptySet();
                }
            });
        } catch (URISyntaxException e) {
            System.err.println("Invalid URL for robots parsing: " + url + " - " + e.getMessage());
        }
    }

    private Set<String> parseRobotsText(Document doc) {
        Set<String> disallowedPaths = new HashSet<>();
        String robotsText = doc.text();
        boolean inUserAgentStar = false;
        String[] lines = robotsText.split("\n");

        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("#") || line.isEmpty()) {
                continue;
            }

            if (line.toLowerCase().startsWith("user-agent:")) {
                String agent = line.substring("user-agent:".length()).trim();
                inUserAgentStar = agent.equals("*");
                continue;
            }

            if (inUserAgentStar && line.toLowerCase().startsWith("disallow:")) {
                String path = line.substring("disallow:".length()).trim();
                if (!path.isEmpty()) {
                    if (!path.startsWith("/")) {
                        path = "/" + path;
                    }
                    disallowedPaths.add(path);
                }
            }
        }

        return disallowedPaths;
    }

    public Set<String> getDisallowed(String url) {
        try {
            URI uri = new URI(url);
            String domain = uri.getHost();
            if (domain == null) return Collections.emptySet();
            // Parse robots.txt if not already done
            parseRobots(url);
            return robots.getOrDefault(domain, Collections.emptySet());
        } catch (URISyntaxException e) {
            System.err.println("Invalid URL for getDisallowed: " + url + " - " + e.getMessage());
            return Collections.emptySet();
        }
    }

    public boolean canCrawl(String url) {
        try {
            URI uri = new URI(url);
            String domain = uri.getHost();
            String path = uri.getPath();
            if (domain == null) return false;
            if (path == null || path.isEmpty()) {
                path = "/";
            }

            // Parse robots.txt if not already done
            parseRobots(url);
            Set<String> disallowed = robots.getOrDefault(domain, Collections.emptySet());
            for (String dis : disallowed) {
                if (path.startsWith(dis)) {
                    return false;
                }
            }
            return true;
        } catch (URISyntaxException e) {
            System.err.println("Invalid URL for canCrawl: " + url + " - " + e.getMessage());
            return false;
        }
    }
}