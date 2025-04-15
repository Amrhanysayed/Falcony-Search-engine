package Crawler;
import java.io.*;
import java.net.URISyntaxException;
import java.util.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import java.net.URI;


public class RobotsManager {

                // url   disallowed
     private final HashMap<String,Set<String>> robots ;

    RobotsManager(){
        robots = new HashMap<>();
    }

    public  void  parseRobots(String url){
        //
        try {
            URI uri = new URI(url);

            if(robots.containsKey(uri.getHost())) return ;

            // get the robots.txt
            String robotsURL = uri.getScheme() + "://" + uri.getHost() + "/robots.txt";

            try {
                Document robotsFile = Jsoup.connect(robotsURL).get();
               Set<String> disallowed = parseRobotsText(robotsFile);
               String domain = uri.getHost();
                robots.put(domain,disallowed);

            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }


    }

    private   Set<String> parseRobotsText (Document doc){

        String robotsText = doc.text();
        Set<String> disallowedPaths = new HashSet<>();
        boolean inUserAgentStar = false;
        String[] lines = robotsText.split("\n");

        for(String line : lines){
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
                    // Ensure path starts with "/" for consistency
                    if (!path.startsWith("/")) {
                        path = "/" + path;
                    }
                    disallowedPaths.add(path);
                }
            }

        }

        return disallowedPaths;
    }


    public  Set<String> getDisallowed(String url ){
            try {
                URI uri = new URI(url);
               String domain = uri.getHost();
               return robots.get(domain);
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
    }

    public  boolean canCrawl(String url){
        try {
            URI uri = new URI(url);
            String domain = uri.getHost();
            String path = uri.getPath();
            if (path == null || path.isEmpty()) {
                path = "/";
            }

            Set<String> disallowed =  robots.get(domain);
            for (String dis : disallowed) {

                if (path.startsWith(dis)) {
                    return false;
                }
            }
            return true;

        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

    }
}
