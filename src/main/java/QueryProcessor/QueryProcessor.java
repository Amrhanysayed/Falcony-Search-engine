package QueryProcessor;

import Ranker.Ranker;
import Ranker.TokenBasedRanker;
import Ranker.PhraseBasedRanker;
import Ranker.RankerContext;
import Utils.Tokenizer;
import Utils.Utils;
import Utils.WebDocument;
import dbManager.dbManager;
import opennlp.tools.stemmer.PorterStemmer;
import opennlp.tools.tokenize.TokenizerME;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class QueryProcessor {

    dbManager db;
    Tokenizer tokenizer;

    public QueryProcessor() throws Exception {
        db = new dbManager();  // Fixed: Assign to instance variable, not local variable
        tokenizer = new Tokenizer();
    }

    public void process(String query) throws Exception {
        boolean isUsingOperator = false;
        boolean isSinglePhrase = false;
        query = query.trim().toLowerCase();

        String text1 = "", operator = "", text2 = "";
        // Is using operator
        Pattern pattern = Pattern.compile("\"([^\"]*)\"\\s*(AND|OR|NOT)\\s*\"([^\"]*)\"", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(query);
        if (matcher.matches()) {
            text1 = matcher.group(1).trim();
            operator = matcher.group(2).toUpperCase();
            text2 = matcher.group(3).trim();
            isUsingOperator = true;
        }
        // Is single phrase
        if (!isUsingOperator) {
            pattern  = Pattern.compile("^\"(.*)\"$");
            matcher = pattern.matcher(query);
            if (matcher.matches()) {
                text1 = matcher.group(1).trim();
                isSinglePhrase = true;
            }
        }
        if(!isSinglePhrase && !isUsingOperator) {
            text1 = query;
        }

        // Tokenization get query terms ready

        List<String> queryTerms = tokenizer.Tokenize(text1);
        Set<String> docsIds1 = new HashSet<>(), docsIds2 = new HashSet<>();

        docsIds1 = db.getDocIdsForTokens(queryTerms, isSinglePhrase || isUsingOperator);
        if(isUsingOperator) {
            List<String> queryTerms2 = tokenizer.Tokenize(text2);
            docsIds2 = db.getDocIdsForTokens(queryTerms2, true);
        }

        Set<WebDocument> Results = new HashSet<>();
        RankerContext rankerContext = new RankerContext();
        if(!isSinglePhrase && !isUsingOperator) {
            Ranker TOKENBASED = new TokenBasedRanker(0);
            rankerContext.setRanker(TOKENBASED);
        }
        else {
            Ranker PHRASEBASED = new PhraseBasedRanker(0);
            rankerContext.setRanker(PHRASEBASED);
        }

        queryTerms = isSinglePhrase || isUsingOperator ? List.of(text1) : queryTerms;

        Results = rankerContext.rank(queryTerms, docsIds1);

        if(isUsingOperator) {
            Set<WebDocument> Results2 = rankerContext.rank(List.of(text2), docsIds2);
            if(operator.equals("AND")) {
                Results.retainAll(Results2);
            }
            else if(operator.equals("OR")) {
                Results.addAll(Results2);
            }
            else if(operator.equals("NOT")) {
                Results.removeAll(Results2);
            }
        }

        System.out.println("Found " + Results.size() + " results");
        int count = 0;
        for (WebDocument doc : Results) {
            System.out.println();
            doc.Print();
            count++;
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
        qp.process("\"saving private ryan\" AND \"godfather\"");

    }


}