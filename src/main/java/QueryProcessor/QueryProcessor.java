package QueryProcessor;

import Ranker.TokenBasedRanker;
import Ranker.PhraseBasedRanker;
import Ranker.RankerContext;
import Utils.Tokenizer;
import Utils.Utils;
import Utils.WebDocument;
import dbManager.dbManager;

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

    public List<WebDocument> process(String query) throws Exception {
        query = query.trim().toLowerCase(); // ALL COMING LOGIC IS BASED ON LOWERCASE
        List<String> queryTexts = new ArrayList<>();
        Pattern pattern = Pattern.compile("\"([^\"]*)\"\\s*(AND|OR|NOT)\\s*\"([^\"]*)\"", Pattern.CASE_INSENSITIVE);
        boolean isUsingOperator = false;
        boolean isUsingPhrase = false;
        String operator = "";
        Matcher matcher = pattern.matcher(query);

        // Check if the query contains a logical operator
        if (matcher.matches()) {
            // Add the first phrase to the query texts
            queryTexts.add(matcher.group(1).trim());

            // Extract and validate the operator
            operator = matcher.group(2).trim();
            if (!Set.of("and", "or", "not").contains(operator)) {
                String errorMessage = "Invalid logical operator: " + operator;
                System.out.println("ERROR: " + errorMessage);
                throw new IllegalArgumentException(errorMessage);
            }

            // Add the second phrase to the query texts
            queryTexts.add(matcher.group(3).trim());
            isUsingOperator = true;
            System.out.println("Operator with 2 Phrases");
        } else {
            // Check if the query is a single phrase enclosed in quotes
            pattern = Pattern.compile("^\"(.*)\"$");
            matcher = pattern.matcher(query);

            if (matcher.matches()) {
                queryTexts.add(matcher.group(1).trim());
                isUsingPhrase = true;
                System.out.println("Single Phrase");
            } else {
                // Treat the query as token-based
                queryTexts.add(query.trim());
            }
        }


        Set<String> candidateDocIds = new HashSet<>();
        RankerContext rankerContext = new RankerContext();
        List<String> tokensFirst = tokenizer.Tokenize(queryTexts.get(0));
        List<String> tokensSecond = isUsingOperator ? tokenizer.Tokenize(queryTexts.get(1)) : new ArrayList<>();

        System.out.println(tokensFirst);
        System.out.println(tokensSecond);

        // Ranker Context
        List<String> queryTerms = new ArrayList<>();
        if (isUsingOperator || isUsingPhrase) {

            candidateDocIds = db.getDocIdsForTokens(tokensFirst, true);
            Set<String> candidateDocIdsSecond = db.getDocIdsForTokens(tokensSecond, true);

            if (operator.equals("and")) {
                candidateDocIds.retainAll(candidateDocIdsSecond);
            } else if (operator.equals("or")) {
                candidateDocIds.addAll(candidateDocIdsSecond);
            }

            rankerContext.setRanker(new PhraseBasedRanker(0));
//            queryTerms = queryTexts;
        }
        else {
            candidateDocIds = db.getDocIdsForTokens(tokensFirst , false);
            rankerContext.setRanker(new TokenBasedRanker(0));
//            queryTerms = tokensFirst;
            System.out.println("Token Based");
        }

        System.out.println("WILL RANK");

        double startTime = System.currentTimeMillis();
        List<WebDocument> Results = rankerContext.rank(queryTexts , tokensFirst, tokensSecond, candidateDocIds, operator);
        double endTime = System.currentTimeMillis();
        double duration = (endTime - startTime) / 1000;
        System.out.println("Anas Ranker took " + duration + " seconds");

//        System.out.println(queryTerms);
        System.out.println(candidateDocIds);
        System.out.println(operator);

        System.out.println("Found " + Results.size() + " results");

        int counter = 0;
        for (WebDocument doc : Results) {
            System.out.println();
            doc.Print();
            counter++;
            if (counter > 22)
                break;
        }

        return Results;

    }


    public static void main(String[] args) throws Exception {
        QueryProcessor qp = new QueryProcessor();
        qp.process("\"Autism\"");

    }


}