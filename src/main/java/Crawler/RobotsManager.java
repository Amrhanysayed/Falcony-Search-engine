package Crawler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class RobotsManager {
    private final ConcurrentHashMap<String, RobotsInfo> robotsCache = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private static final String USER_AGENT = "MySearchBot/1.0";
    private static final int CACHE_EXPIRY_HOURS = 24;
    private static final int ROBOTS_TIMEOUT_MS = 5000;
    private static final int MAX_ROBOTS_SIZE = 1000000; // 1MB max for robots.txt

    public RobotsManager() {
        // Schedule cleanup of old cache entries
        scheduler.scheduleAtFixedRate(
                this::cleanupCache,
                CACHE_EXPIRY_HOURS,
                CACHE_EXPIRY_HOURS,
                TimeUnit.HOURS
        );
    }

    private void cleanupCache() {
        long now = System.currentTimeMillis();
        robotsCache.entrySet().removeIf(entry ->
                (now - entry.getValue().timestamp) > TimeUnit.HOURS.toMillis(CACHE_EXPIRY_HOURS));
    }

    public void parseRobots(String url) {
        try {
            URI uri = new URI(url);
            String host = uri.getHost();
            if (host == null || host.isEmpty()) {
                return;
            }

            String scheme = uri.getScheme();
            if (scheme == null) {
                scheme = "http";
            }

            String robotsUrl = scheme + "://" + host;
            if (uri.getPort() > 0 && uri.getPort() != 80 && uri.getPort() != 443) {
                robotsUrl += ":" + uri.getPort();
            }
            robotsUrl += "/robots.txt";

            // Skip if we already have this robots.txt
            if (robotsCache.containsKey(host)) {
                return;
            }

            fetchRobotsTxt(robotsUrl, host);
        } catch (Exception e) {
            // Silently ignore errors with robots.txt - just proceed with crawling
            RobotsInfo info = new RobotsInfo(new HashMap<>(), System.currentTimeMillis());
            robotsCache.put(getDomainFromUrl(url), info);
        }
    }

    private void fetchRobotsTxt(String robotsUrl, String host) {
        try {
            URL url = new URL(robotsUrl);
            URLConnection connection = url.openConnection();
            connection.setConnectTimeout(ROBOTS_TIMEOUT_MS);
            connection.setReadTimeout(ROBOTS_TIMEOUT_MS);
            connection.setRequestProperty("User-Agent", USER_AGENT);

            StringBuilder content = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (content.length() > MAX_ROBOTS_SIZE) {
                        break; // Prevent huge robots.txt files from being fully read
                    }
                    content.append(line).append("\n");
                }
            }

            Map<String, Boolean> disallowedPaths = parseRobotsTxt(content.toString());
            RobotsInfo info = new RobotsInfo(disallowedPaths, System.currentTimeMillis());
            robotsCache.put(host, info);
        } catch (Exception e) {
            // Default to empty rules on error
            RobotsInfo info = new RobotsInfo(new HashMap<>(), System.currentTimeMillis());
            robotsCache.put(host, info);
        }
    }

    private Map<String, Boolean> parseRobotsTxt(String content) {
        Map<String, Boolean> disallowedPaths = new HashMap<>();
        boolean inRelevantUserAgent = false;

        for (String line : content.split("\n")) {
            line = line.trim();

            // Skip comments and empty lines
            if (line.isEmpty() || line.startsWith("#")) {
                continue;
            }

            // Check for user agent
            if (line.toLowerCase().startsWith("user-agent:")) {
                String agent = line.substring("user-agent:".length()).trim().toLowerCase();
                inRelevantUserAgent = agent.equals("*") || agent.contains(USER_AGENT.toLowerCase());
                continue;
            }

            // Check for disallow if in relevant user agent section
            if (inRelevantUserAgent && line.toLowerCase().startsWith("disallow:")) {
                String path = line.substring("disallow:".length()).trim();
                if (!path.isEmpty()) {
                    disallowedPaths.put(path, true);
                }
            }

            // Check for allow if in relevant user agent section (allow overrides disallow)
            if (inRelevantUserAgent && line.toLowerCase().startsWith("allow:")) {
                String path = line.substring("allow:".length()).trim();
                if (!path.isEmpty()) {
                    disallowedPaths.put(path, false);
                }
            }
        }

        return disallowedPaths;
    }

    public boolean canCrawl(String url) {
        try {
            URI uri = new URI(url);
            String host = uri.getHost();
            String path = uri.getPath();
            if (path == null || path.isEmpty()) {
                path = "/";
            }

            // Check if we have robots info for this host
            RobotsInfo info = robotsCache.get(host);
            if (info == null) {
                // If not, try to parse robots.txt
                parseRobots(url);
                info = robotsCache.get(host);

                // If still null, allow crawling
                if (info == null) {
                    return true;
                }
            }

            // Check if path is disallowed
            for (Map.Entry<String, Boolean> entry : info.disallowedPaths.entrySet()) {
                String disallowPath = entry.getKey();
                boolean isDisallowed = entry.getValue();

                // Skip if this is an allow rule
                if (!isDisallowed) {
                    continue;
                }

                // Check if path matches disallow pattern
                if (disallowPath.endsWith("*")) {
                    String prefix = disallowPath.substring(0, disallowPath.length() - 1);
                    if (path.startsWith(prefix)) {
                        return false;
                    }
                } else if (path.equals(disallowPath) || path.startsWith(disallowPath + "/")) {
                    return false;
                }
            }

            return true;
        } catch (Exception e) {
            return true; // Allow on error
        }
    }

    private String getDomainFromUrl(String url) {
        try {
            URI uri = new URI(url);
            return uri.getHost();
        } catch (Exception e) {
            return "";
        }
    }

    private static class RobotsInfo {
        final Map<String, Boolean> disallowedPaths;
        final long timestamp;

        RobotsInfo(Map<String, Boolean> disallowedPaths, long timestamp) {
            this.disallowedPaths = disallowedPaths;
            this.timestamp = timestamp;
        }
    }

    public void shutdown() {
        scheduler.shutdown();
    }
}