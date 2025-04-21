package QueryProcessor;

import Utils.Tokenizer;
import Utils.WebDocument;
import Utils.Utils;
import dbManager.dbManager;
import opennlp.tools.stemmer.PorterStemmer;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.stemmer.PorterStemmer;

import java.util.*;

public class QueryProcessor {

    dbManager db;
    public QueryProcessor() {
        dbManager db = new dbManager();
    }

    public void processs(String query) throws Exception {
        List <String> queryTerms = new ArrayList<>();
        List <WebDocument> candidateDocs = new ArrayList<>();

        // Tokenatzeion get queryterms ready
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
        // check if " "
        if (true) // isPhrase(query)
        {
            candidateDocs = db.getDocsForTokens(queryTerms, true);
        }
        else {
            candidateDocs = db.getDocsForTokens(queryTerms, false);
        }

            // Candidiate docs


            for (WebDocument doc : candidateDocs)
                doc.Print();

        }

    }

    // This module receives search queries, performs necessary preprocessing and searches the index for relevant
    //documents. Retrieve documents containing words that share the same stem with those in the search query. For
    //example, the search query “travel” should match (with lower degree) the words “traveler”, “traveling” … etc.

    // call ranker with candidate docs + tokens or phrases

    public static void main(String[] args) throws Exception {
        QueryProcessor qp = new QueryProcessor();
        qp.processs("reddeadredemption");

    }


}