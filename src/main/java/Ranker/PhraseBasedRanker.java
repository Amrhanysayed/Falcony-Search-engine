package Ranker;
import Utils.Posting;
import Utils.WebDocument;
import dbManager.dbManager;

import java.util.*;
import java.util.stream.Collectors;

public class PhraseBasedRanker implements Ranker {

    private final int popularityAlpha ;
    private dbManager db;
    public PhraseBasedRanker(int popularityAlpha) {
        this.popularityAlpha = popularityAlpha;
        db = new dbManager();
    }

    @Override
    public Set<WebDocument> rank(List<String> tokens, Set<String> candidateDocsIds) {
        String text = tokens.getFirst();



        Map<String , WebDocument> candidateDocs = db.getDocumentsByIds(candidateDocsIds);


        candidateDocs.entrySet().removeIf(entry ->
                !entry.getValue().getSoupedContent().toLowerCase().contains(text));

        return new HashSet<>(candidateDocs.values());
    }



}


