package Ranker;
import Utils.WebDocument;
import dbManager.dbManager;
import org.slf4j.Logger;
import java.util.regex.*;
import org.slf4j.LoggerFactory;

import java.util.*;

public class PhraseBasedRanker implements Ranker {

    private static final Logger log = LoggerFactory.getLogger(PhraseBasedRanker.class);
    private final int popularityAlpha ;
    private dbManager db;
    private final Map<String, Double> sectionWeights; // TODO use it parsing HTML content

    public PhraseBasedRanker(int popularityAlpha) {
        this.popularityAlpha = popularityAlpha;
        db = new dbManager();
        this.sectionWeights = Map.of( // TODO cinder using it instead of defualt weight 1.0
        "title", 2.0,
        "h1", 1.5,
        "body", 1.0,
        "meta", 1.2
        );
    }


    @Override
    public List<WebDocument> rank(List<String> queryTexts, Set<String> candidateDocsIds , String logicalOperator) {

        Map<String, Double> docScores = new HashMap<>();
        Map<String, WebDocument> candidateDocs = db.getDocumentsByIds(candidateDocsIds);


        String firstPhrase = queryTexts.get(0);
        String secondPhrase = logicalOperator.isEmpty() ? "" : queryTexts.get(1);

        if (queryTexts.size() < 1 || (!logicalOperator.isEmpty() && queryTexts.size() != 2)) {
            throw new IllegalArgumentException("Invalid query format: expected one or two quoted phrases depending on the logical operator.");
        }

        for (WebDocument doc : candidateDocs.values()) {

            String content = doc.getSoupedContent().toLowerCase();
            int countFirst = countPhraseOccurrences(firstPhrase.toLowerCase(), content);
            if (countFirst == 0 && !logicalOperator.equals("or")) {continue;} // early exit

            double phraseScoreFirst = 1.0 * countFirst + 2.0 * countPhraseOccurrences(firstPhrase.toLowerCase() , doc.getTitle().toLowerCase());
            double finalScore = phraseScoreFirst ;

            if (!logicalOperator.isEmpty())
            {
                if (logicalOperator.equals("not"))
                {
                    if (content.contains(secondPhrase.toLowerCase()))
                    {
                        System.out.println(doc.getId());
                        continue;
                    }
                }
                else
                {
                    int countSecond = countPhraseOccurrences(secondPhrase.toLowerCase(), content);
                    double phraseScoreSecond = 1.0 * countSecond +  2.0 * countPhraseOccurrences(secondPhrase.toLowerCase() , doc.getTitle().toLowerCase());

                    if (logicalOperator.equals("and") && countSecond == 0)
                        continue;

                    if (logicalOperator.equals("or") && (countSecond + countFirst) == 0)
                        continue;

                    finalScore = phraseScoreFirst + phraseScoreSecond; // ASSUME AND , OR both sum scores

                }
            }

            finalScore += popularityAlpha * doc.getPopularity() ;
            String docId = doc.getId();
            docScores.put(docId, docScores.getOrDefault(docId, 0.0) + finalScore);

        }

        return docScores.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .map(entry -> candidateDocs.get(entry.getKey()))
                .toList();
    }


    private int countPhraseOccurrences(String phrase, String content) {
        int count = 0;
        String regex = "(^|\\b)" + Pattern.quote(phrase) + "(?=$|\\b)"; // handle punctuation , start , end , ..etc
        // TODO consider assumption for hyphen seperated ?
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(content);

        while (matcher.find()) {
            count++;
        }
        return count;
    }


}