package Crawler;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RobotsManager {

    //  Used to cache parsed robots.txt rules per domain (host)
    // Format: domain -> map of [path -> true (disallow) or false (allow)]
    private final ConcurrentHashMap<String, Map<String, Boolean>> robotsRules = new ConcurrentHashMap<>();


    private static final String USER_AGENT = "Falcony/1.0";


    private static final int ROBOTS_TIMEOUT_MS = 15000; // 15 seconds


    private static final int MAX_ROBOTS_SIZE = 1000000; // 1MB

    // ✅ Fetches and parses robots.txt for the given URL's host
    public void parseRobots(String url) {
        try {

            URI uri = new URI(url);
            String host = uri.getHost();
            if (host == null || host.isEmpty()) return;


            String scheme = uri.getScheme() == null ? "http" : uri.getScheme();


            String robotsUrl = scheme + "://" + host;
            if (uri.getPort() > 0 && uri.getPort() != 80 && uri.getPort() != 443) {
                robotsUrl += ":" + uri.getPort(); // include non-standard ports
            }
            robotsUrl += "/robots.txt";

            // Fetch and parse the robots.txt file
            Map<String, Boolean> rules = fetchRobotsTxt(robotsUrl);

            // Save the rules to cache
            robotsRules.put(host, rules);

        } catch (Exception ignored) {
            // If error happens, treat the host as fully allowed (no rules)
            try {
                URI uri = new URI(url);
                String host = uri.getHost();
                if (host != null) {
                    robotsRules.put(host, new HashMap<>());
                }
            } catch (Exception e) {

            }
        }
    }

    //  Downloads robots.txt content and parses it into disallowed/allowed paths
    private Map<String, Boolean> fetchRobotsTxt(String robotsUrl) {
        Map<String, Boolean> disallowedPaths = new HashMap<>();
        try {

            URL url = new URL(robotsUrl);
            URLConnection connection = url.openConnection();
            connection.setConnectTimeout(ROBOTS_TIMEOUT_MS);
            connection.setReadTimeout(ROBOTS_TIMEOUT_MS);
            connection.setRequestProperty("User-Agent", USER_AGENT);

            // read
            StringBuilder content = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null && content.length() < MAX_ROBOTS_SIZE) {
                    content.append(line).append("\n");
                }
            }

            // Parse the content and extract rules
            disallowedPaths = parseRobotsTxt(content.toString());

        } catch (Exception ignored) {
            // If failed to download robots.txt, treat as fully allowed
        }

        return disallowedPaths;
    }

    //  Parses raw robots.txt content and returns a map of path rules
    private Map<String, Boolean> parseRobotsTxt(String content) {
        Map<String, Boolean> rules = new HashMap<>();
        boolean inRelevantUserAgent = false;

        // Process file line by line
        for (String line : content.split("\n")) {
            line = line.trim();

            // Ignore empty lines and comments
            if (line.isEmpty() || line.startsWith("#")) continue;

            // Track if current rules belong to our User-Agent or wildcard '*'
            if (line.toLowerCase().startsWith("user-agent:")) {
                String agent = line.substring("user-agent:".length()).trim().toLowerCase();
                inRelevantUserAgent = agent.equals("*") || agent.contains(USER_AGENT.toLowerCase());
            }

            // If the section is for us, read allow/disallow rules
            if (inRelevantUserAgent && line.toLowerCase().startsWith("disallow:")) {
                String path = line.substring("disallow:".length()).trim();
                if (!path.isEmpty()) rules.put(path, true);
                else rules.put(path, false);
            }

            if (inRelevantUserAgent && line.toLowerCase().startsWith("allow:")) {
                String path = line.substring("allow:".length()).trim();
                if (!path.isEmpty()) rules.put(path, false);
            }
        }

        return rules;
    }

    //  Main function to check if a given URL is allowed to be crawled
    public boolean canCrawl(String url) {
        try {
            URI uri = new URI(url);
            String host = uri.getHost();
            String path = uri.getPath() == null ? "/" : uri.getPath();

            // If rules for the host are not loaded yet, fetch them
            if (!robotsRules.containsKey(host)) {
                parseRobots(url);
            }

            // Get the rules (or empty map if none)
            Map<String, Boolean> rules = robotsRules.getOrDefault(host, new HashMap<>());

            // Check path against all disallowed rules
            for (Map.Entry<String, Boolean> entry : rules.entrySet()) {
                String rulePath = entry.getKey();
                boolean isDisallowed = entry.getValue();

                if (!isDisallowed) continue; // skip allowed rules

                // Match wildcard rules (ending with *)
                if (rulePath.endsWith("*")) {
                    String prefix = rulePath.substring(0, rulePath.length() - 1);
                    if (path.startsWith(prefix)) return false;
                }
                // Match exact or prefix match
                else if (path.equals(rulePath) || path.startsWith(rulePath + "/")) {
                    return false;
                }
            }

            return true;

        } catch (Exception e) {
            return true;
        }
    }
}
