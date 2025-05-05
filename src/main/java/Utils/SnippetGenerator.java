package Utils;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SnippetGenerator {

    /**
     * Extracts a relevant snippet from HTML content based on the query.
     *
     * @param content   HTML content to extract text from
     * @param query     Search query to find in the text
     * @param maxWords  Maximum number of words in the resulting snippet
     * @return A snippet of text containing the query, or the beginning of the text if query not found
     */
    public static String getSnippet(String content, String query, int maxWords) {
        // Parse HTML to plain text using Jsoup - make sure to remove all HTML elements
        String plainText = htmlToText(content);

        // Count words in the plain text
        String[] words = plainText.split("\\s+");

        // If the plain text has fewer words than maxWords, just return it
        if (words.length <= maxWords) {
            return plainText;
        }

        // If query is empty or null, return the beginning of the text
        if (query == null || query.trim().isEmpty()) {
            return truncateToWordLimit(plainText, maxWords);
        }

        // Split query into words and find the best snippet containing most words
        String[] queryWords = query.toLowerCase().split("\\s+");
        List<String> sentences = splitIntoSentences(plainText);

        // Find the sentence that contains the most query words
        int maxMatches = 0;
        int bestSentenceIndex = 0;

        for (int i = 0; i < sentences.size(); i++) {
            String sentence = sentences.get(i).toLowerCase();
            int matches = countQueryMatches(sentence, queryWords);

            if (matches > maxMatches) {
                maxMatches = matches;
                bestSentenceIndex = i;
            }
        }

        // If no matches found, return the beginning of the text
        if (maxMatches == 0) {
            return truncateToWordLimit(plainText, maxWords);
        }

        // Build snippet around the best matching sentence
        return constructSnippet(sentences, bestSentenceIndex, maxWords);
    }

    /**
     * Converts HTML to plain text using Jsoup
     * Ensures all HTML tags are completely removed
     */
    private static String htmlToText(String html) {
        try {
            Document doc = Jsoup.parse(html);
            // Use text() method which extracts text from all elements, removing all HTML tags
            return doc.text();
        } catch (Exception e) {
            // Fallback: If Jsoup parsing fails, manually strip tags (less ideal)
            return html.replaceAll("<[^>]*>", "");
        }
    }

    /**
     * Splits text into sentences
     */
    private static List<String> splitIntoSentences(String text) {
        List<String> sentences = new ArrayList<>();
        // Simple sentence splitting - can be improved for better results
        String[] roughSentences = text.split("[.!?]+");

        for (String sentence : roughSentences) {
            sentence = sentence.trim();
            if (!sentence.isEmpty()) {
                sentences.add(sentence);
            }
        }

        return sentences;
    }

    /**
     * Counts how many query words appear in the text
     */
    private static int countQueryMatches(String text, String[] queryWords) {
        int count = 0;
        for (String word : queryWords) {
            if (word.trim().isEmpty()) continue;

            // Use word boundary for more accurate matching
            Pattern pattern = Pattern.compile("\\b" + Pattern.quote(word) + "\\b", Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(text);

            if (matcher.find()) {
                count++;
            }
        }
        return count;
    }

    /**
     * Constructs a snippet centered around a specific sentence,
     * including context from surrounding sentences if possible
     * Now limits by word count instead of character length
     */
    private static String constructSnippet(List<String> sentences, int centerIndex, int maxWords) {
        StringBuilder snippet = new StringBuilder(sentences.get(centerIndex));
        int wordCount = countWords(snippet.toString());

        // Try to add surrounding context
        int leftIndex = centerIndex - 1;
        int rightIndex = centerIndex + 1;

        // Alternately add sentences from left and right until maxWords is reached
        while (wordCount < maxWords && (leftIndex >= 0 || rightIndex < sentences.size())) {
            // Try to add from right first to maintain reading flow
            if (rightIndex < sentences.size()) {
                String rightSentence = sentences.get(rightIndex);
                int rightWordCount = countWords(rightSentence);

                if (wordCount + rightWordCount + 1 <= maxWords) { // +1 for the period
                    snippet.append(". ").append(rightSentence);
                    wordCount += rightWordCount + 1;
                }
                rightIndex++;
            }

            // Then try from left
            if (leftIndex >= 0 && wordCount < maxWords) {
                String leftSentence = sentences.get(leftIndex);
                int leftWordCount = countWords(leftSentence);

                if (wordCount + leftWordCount + 1 <= maxWords) { // +1 for the period
                    snippet.insert(0, leftSentence + ". ");
                    wordCount += leftWordCount + 1;
                }
                leftIndex--;
            }
        }

        String result = snippet.toString();

        // If the result still has more words than maxWords, truncate it
        if (countWords(result) > maxWords) {
            result = truncateToWordLimit(result, maxWords);
        }

        return result;
    }

    /**
     * Counts the number of words in a string
     */
    private static int countWords(String text) {
        if (text == null || text.trim().isEmpty()) {
            return 0;
        }
        return text.split("\\s+").length;
    }

    /**
     * Truncates text to maxWords words and adds ellipsis
     */
    private static String truncateToWordLimit(String text, int maxWords) {
        String[] words = text.split("\\s+");

        if (words.length <= maxWords) {
            return text;
        }

        StringBuilder result = new StringBuilder();
        for (int i = 0; i < maxWords; i++) {
            result.append(words[i]).append(" ");
        }

        return result.toString().trim() + "...";
    }
}