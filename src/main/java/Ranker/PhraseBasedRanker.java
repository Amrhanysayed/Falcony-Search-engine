package Ranker;

import Utils.Posting;
import Utils.WebDocument;
import dbManager.dbManager;
import java.util.*;
import java.util.regex.*;

public class PhraseBasedRanker implements Ranker {
    private final double popularityAlpha;
    private final Helpers.WeightConfig weightConfig;
    private dbManager db;

    public PhraseBasedRanker(double popularityAlpha) throws Exception {
        this(popularityAlpha, new Helpers.WeightConfig(1.0, 1.5, 1.3, 1.2)); // Default weights
    }

    public PhraseBasedRanker(double popularityAlpha, Helpers.WeightConfig weightConfig) throws Exception {
        this.popularityAlpha = popularityAlpha;
        this.weightConfig = weightConfig;
        this.db = new dbManager();
    }

    @Override
    public List<WebDocument> rank(List<String> queryTexts, List<String> tokensFirst, List<String> tokensSecond,
                                  Set<String> candidateDocsIds, String logicalOperator) {
        // sort return
        String firstPhrase = queryTexts.get(0).toLowerCase();
        String secondPhrase = logicalOperator.isEmpty() ? "" : queryTexts.get(1).toLowerCase();

        // First Filter Operators and Not existing
        Set<String> filteredCandidateIds = FilterCandidates(firstPhrase, logicalOperator, secondPhrase, candidateDocsIds);

        Integer totalDocCount = db.getTotalDocCount();
        if (totalDocCount == 0) {
            return new ArrayList<>();
        }

        // Combine tokens without duplicates
        Set<String> combinedTokensSet = new HashSet<>(tokensFirst);
        if (tokensSecond != null) {
            combinedTokensSet.addAll(tokensSecond);
        }
        List<String> combinedTokens = new ArrayList<>(combinedTokensSet);

        // Apply TF_IDF
        Map<String, List<Posting>> tokenToPostings = db.getPostingsForTokens(combinedTokens, filteredCandidateIds);
        Map<String, WebDocument> filteredDocs = db.getDocumentsByIds(filteredCandidateIds);

        // Get relevance scores - pass weightConfig
        Map<String, Double> docScores = Helpers.RelevanceScore(combinedTokens, tokenToPostings, totalDocCount, filteredDocs, weightConfig);

        // Apply phrase boost for titles
        for (WebDocument doc : filteredDocs.values()) {
            double titleCount = countPhraseOccurrences(firstPhrase, doc.getTitle()) +
                    (secondPhrase.isEmpty() ? 0 : countPhraseOccurrences(secondPhrase, doc.getTitle()));

            // Use the same weight from weightConfig for title boost
            docScores.put(doc.getId(), docScores.getOrDefault(doc.getId(), 0.0) +
                    titleCount * weightConfig.titleWeight);
        }

        // Apply popularity adjustment
        docScores = Helpers.ApplyPopularityScore(docScores, filteredDocs, popularityAlpha);

        return docScores.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .map(entry -> filteredDocs.get(entry.getKey()))
                .toList();
    }

    Set<String> FilterCandidates(String firstPhrase, String logicalOperator, String secondPhrase, Set<String> candidateDocsIds) {
        Set<String> filteredDocsIds = db.FilterDocsIdsByPhrase(candidateDocsIds, firstPhrase);
        if (filteredDocsIds.size() == 0 && !logicalOperator.equals("or"))
            return new HashSet<String>();
        else if (logicalOperator.equals("or"))
            filteredDocsIds.addAll(db.FilterDocsIdsByPhrase(candidateDocsIds, secondPhrase));
        else if (logicalOperator.equals("and"))
            filteredDocsIds.retainAll(db.FilterDocsIdsByPhrase(candidateDocsIds, secondPhrase));
        else if (logicalOperator.equals("not"))
            filteredDocsIds.removeAll(db.FilterDocsIdsByPhrase(candidateDocsIds, secondPhrase));

        return filteredDocsIds;
    }

    private int countPhraseOccurrences(String phrase, String content) {
        if (phrase == null || phrase.isEmpty() || content == null) {
            return 0;
        }

        int count = 0;
        String regex = "(^|\\b)" + Pattern.quote(phrase) + "(?=$|\\b)";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(content);

        while (matcher.find()) {
            count++;
        }
        return count;
    }
}