package Ranker;

import Utils.Posting;
import Utils.WebDocument;
import dbManager.dbManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class TokenBasedRanker implements Ranker {

    private final double k1 = 1.25;// TODO check values
    private final double b = 0.75;
    private dbManager db;

    private final int popularityAlpha ;
    public TokenBasedRanker(int popularityAlpha )
    {
        this.popularityAlpha = popularityAlpha;
        db = new dbManager();
    }


    @Override
    public Set<WebDocument> rank(List<String> tokens, Set<String> candidateDocsIds) {

        Map<String, Double> docScores = new HashMap<>();
        Integer totalDocCount = candidateDocsIds.size();

        Map<String, List<Posting>> tokenToPostings =  db.getPostingsForTokens(tokens , candidateDocsIds);
        Map<String , WebDocument> candidateDocs = db.getDocumentsByIds(candidateDocsIds);
        double avgdoclen = 0;
        Integer sumDocLen = 0;

        for (WebDocument doc : candidateDocs.values()) {
            sumDocLen += doc.getSoupedContent().length();
        }

        avgdoclen = (double) sumDocLen / totalDocCount;

        for (String token : tokens) {
            List<Posting> postings = tokenToPostings.get(token);
            int df = postings.size();
            if (df == 0) continue; // Safe Check

            double idf = Math.log(1 + (totalDocCount - df + 0.5) / (df + 0.5));

            for (Posting post : postings)
            {
                String docId = post.getDocId();
                int tf = post.getFrequency();
                int doclen = candidateDocs.get(docId).getSoupedContent().length();


                double bm25Score = BM25Calculator(tf , k1 , idf , doclen , avgdoclen );
                double popScore = 0; // TODO get actual pop
                double finalScore = bm25Score + popularityAlpha * popScore;

                docScores.put(docId , docScores.getOrDefault(docId , 0.0) + finalScore);

            }

        }

        return docScores.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue())) // descending score
                .map(entry -> candidateDocs.get(entry.getKey())).collect(Collectors.toSet());

    }

    private double BM25Calculator(int tf , double k1 , double idf , int docLen , double avgDocLen)
    {
        double numerator = tf * (k1 + 1);
        double denominator = tf + k1 * (1 - b + b * docLen / avgDocLen);
        return idf * (numerator / denominator);
    }


}