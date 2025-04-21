package QueryProcessor;

import Ranker.Ranker;
import Ranker.TokenBasedRanker;
import Ranker.RankerContext;
import Utils.Tokenizer;
import Utils.Utils;
import Utils.WebDocument;
import dbManager.dbManager;
import opennlp.tools.stemmer.PorterStemmer;
import opennlp.tools.tokenize.TokenizerME;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public class QueryProcessor {

    dbManager db;

    public QueryProcessor() {
        db = new dbManager();  // Fixed: Assign to instance variable, not local variable
    }

    public void process(String query) throws Exception {
        List<String> queryTerms = new ArrayList<>();
        Set<String> candidateDocsIds = new HashSet<>();

        // Tokenization get query terms ready
        Tokenizer tokenizer = new Tokenizer();
        TokenizerME tokenizerMe = tokenizer.getTokenizerME();
        String[] tokens = tokenizerMe.tokenize(query);
        PorterStemmer stemmer = new PorterStemmer();

        for (String token : tokens) {
            String cleaned = Utils.CLEAN_PATTERN.matcher(token.toLowerCase()).replaceAll("");
            if (cleaned.isEmpty() || Utils.STOP_WORDS.contains(cleaned)) {
                continue;
            }

            String stemmed = stemmer.stem(cleaned);
            queryTerms.add(stemmed);
        }

        if (isFullyQuoted(query))
        {
            System.out.println("Fully quoted query: " + query);
            candidateDocsIds = db.getDocIdsForTokens(queryTerms, true);
        } else
        {
            candidateDocsIds = db.getDocIdsForTokens(queryTerms, false);
        }

        List<WebDocument> Results = new ArrayList<>();
        RankerContext rankerContext = new RankerContext();
        Ranker TOKENBASED = new TokenBasedRanker(0);
        rankerContext.setRanker(TOKENBASED);
        Results = rankerContext.rank(queryTerms, candidateDocsIds);

        int count = 0;
        for (WebDocument doc : Results) {
            System.out.println();
            doc.Print();
            count++;
            if (count == 5) break;
        }

    }


    public static boolean isFullyQuoted(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }

        String regex = "^\"(.*)\"$";
        return Pattern.matches(regex, text);
    }

    public static void main(String[] args) throws Exception {
        QueryProcessor qp = new QueryProcessor();
        qp.process("saving private ryan");

    }


}