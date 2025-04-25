package Ranker;

import Utils.Posting;
import Utils.WebDocument;
import dbManager.dbManager;

import Utils.Tokenizer;
import java.util.*;

public class TokenBasedRanker implements Ranker {


    private dbManager db;
    Tokenizer tokenizer;


    private final int popularityAlpha ;
    public TokenBasedRanker(int popularityAlpha ) throws Exception {
        this.popularityAlpha = popularityAlpha;
        db = new dbManager();
        tokenizer = new Tokenizer();
    }


    @Override
    public List<WebDocument> rank(List<String> tokens, Set<String> candidateDocsIds , String logicalOperator) {

        Map<String, Double> docScores = new HashMap<>();
        Integer totalDocCount = candidateDocsIds.size();
        if (totalDocCount == 0) {
            return new ArrayList<>();
        }

        Map<String, List<Posting>> tokenToPostings =  db.getPostingsForTokens(tokens , candidateDocsIds);
        Map<String , WebDocument> candidateDocs = db.getDocumentsByIds(candidateDocsIds);

        for (String token : tokens) {
            List<Posting> postings = tokenToPostings.get(token);
            int df = postings.size();
            if (df == 0) continue; // Safe Check

            double idf = Math.log(totalDocCount  / df);

            for (Posting post : postings)
            {
                String docId = post.getDocId();
                WebDocument doc = candidateDocs.get(docId);
                int tf = post.getFrequency();
                double score = TF_IDF(tf , idf );

                if (doc != null && titleContainsToken(doc.getTitle(), token)) {
                    score *= 1.5;
                }

                double popScore = 0; // TODO get actual pop
                double finalScore = score + popularityAlpha * popScore;

                docScores.put(docId , docScores.getOrDefault(docId , 0.0) + finalScore);

            }

        }

        return docScores.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .map(entry -> candidateDocs.get(entry.getKey()))
                .toList();

    }

    private double TF_IDF(int tf , double idf)
    {
        // Logged tf / idf
        double tfLog = Math.log(tf + 1);  // Apply log to term frequency (log(tf + 1))
        double result = tfLog * idf;       // Multiply by idf, which is typically log(N / df)
        return result;
    }

    private boolean titleContainsToken(String title, String token) {
        if (title == null) return false;
        title = title.toLowerCase().trim();
        List<String> titleTokens = tokenizer.Tokenize(title);
        token = token.toLowerCase();
        for (String t : titleTokens) {
            if (t.equals(token)) {
                return true;
            }
        }
        return false;
    }


}