package Utils;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SnippetGenerator {

    /**
     * Generates a basic snippet from HTML content
     */
    public static String generateSnippet(String htmlContent, int maxWords) {
        Document document = Jsoup.parse(htmlContent);
        String plainText = document.text();

        String[] words = plainText.split("\\s+");
        if (words.length <= maxWords) {
            return plainText;
        }

        StringBuilder snippet = new StringBuilder();
        for (int i = 0; i < maxWords; i++) {
            snippet.append(words[i]).append(" ");
        }

        return snippet.toString().trim() + "...";
    }

    /**
     * Generates a snippet based on a search query, retrieving the whole paragraph
     * containing the query up to maxWords
     */
    public static String getSnippet(String htmlContent, String query, int maxWords) {
        Document document = Jsoup.parse(htmlContent);

        // Process the query
        String processedQuery = query;
        String operator = null;
        String secondTerm = null;

        // Check for quoted terms with boolean operators
        Pattern pattern = Pattern.compile("\"([^\"]*)\"\\s*(AND|OR|NOT)\\s*\"([^\"]*)\"", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(query);

        if (matcher.find()) {
            // Extract the first term without quotes
            processedQuery = matcher.group(1);
            // Get the operator
            operator = matcher.group(2).toUpperCase();
            // Get the second term without quotes
            secondTerm = matcher.group(3);
        } else {
            // Check for simple quoted term
            Pattern quotePattern = Pattern.compile("\"([^\"]*)\"");
            Matcher quoteMatcher = quotePattern.matcher(query);
            if (quoteMatcher.find()) {
                processedQuery = quoteMatcher.group(1);
            }
        }

        // Convert to lowercase for case-insensitive matching
        String lowerProcessedQuery = processedQuery.toLowerCase();
        String lowerSecondTerm = secondTerm != null ? secondTerm.toLowerCase() : null;

        Elements paragraphs = document.select("p, li, div, h1, h2, h3, h4, h5, h6");

        for (Element paragraph : paragraphs) {
            String paragraphText = paragraph.text();
            String lowerParagraphText = paragraphText.toLowerCase();

            boolean matches = false;

            if (operator == null) {
                // Simple query - just check if the paragraph contains the term
                matches = lowerParagraphText.contains(lowerProcessedQuery);
            } else {
                // Apply boolean logic
                boolean firstTermMatch = lowerParagraphText.contains(lowerProcessedQuery);
                boolean secondTermMatch = lowerParagraphText.contains(lowerSecondTerm);

                switch (operator) {
                    case "AND":
                        matches = firstTermMatch && secondTermMatch;
                        break;
                    case "OR":
                        matches = firstTermMatch || secondTermMatch;
                        break;
                    case "NOT":
                        matches = firstTermMatch && !secondTermMatch;
                        break;
                }
            }

            if (matches) {
                String[] words = paragraphText.split("\\s+");

                if (words.length <= maxWords) {
                    return paragraphText;
                }

                StringBuilder snippet = new StringBuilder();
                for (int i = 0; i < maxWords; i++) {
                    snippet.append(words[i]).append(" ");
                }

                return snippet.toString().trim() + "...";
            }
        }

        return generateSnippet(htmlContent, maxWords);
    }

    public static void main(String[] args) {
        String htmlContent = "<html><head><title>Test Page</title></head>" +
                "<body>" +
                "<h1>Welcome to the Test</h1>" +
                "<div class='section'>" +
                "<p>This is the <b>first paragraph</b> with some <i>formatted text</i>.</p>" +
                "<p>Another paragraph with <a href='https://example.com'>links</a> and special characters: &amp; &lt; &gt;</p>" +
                "</div>" +
                "<div class='highlight'>" +
                "<p>This section contains the target keyword phrase that should be found.</p>" +
                "<ul><li>List item 1</li><li>Important information here</li><li>List item 3</li></ul>" +
                "</div>" +
                "<footer>Copyright 2025</footer>" +
                "</body></html>";

        // Test different query formats
        String[] queries = {
                "target keyword",
                "\"target keyword\"",
                "\"target\" AND \"keyword\"",
                "\"target\" OR \"nonexistent\"",
                "\"target\" NOT \"nonexistent\"",
                "\"messi\" NOT \"ronaldo\""
        };

        int maxWords = 10;

        for (String query : queries) {
            String querySnippet = getSnippet(htmlContent, query, maxWords);
            System.out.println("Query: " + query);
            System.out.println("Snippet: " + querySnippet + "\n");
        }
    }
}