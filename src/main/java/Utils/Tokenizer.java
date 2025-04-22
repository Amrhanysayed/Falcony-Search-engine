package Utils;

import opennlp.tools.stemmer.PorterStemmer;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Tokenizer {
    private static TokenizerModel tmodel;

    public Tokenizer() throws Exception {
        try {
            InputStream modelInput = Tokenizer.class.getResourceAsStream("/en-token.bin");
            assert modelInput != null;
            tmodel = new TokenizerModel(modelInput);
            modelInput.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Threading Safe Solution
    public TokenizerME getTokenizerME() {
        return new TokenizerME(tmodel);
    }
    public List<String> Tokenize(String text) {
        TokenizerME tokenizerMe = getTokenizerME();
        String[] tokens = tokenizerMe.tokenize(text);
        PorterStemmer stemmer = new PorterStemmer();
        List<String> stemmedList = new ArrayList<>();

        for (String token : tokens) {
            String cleaned = Utils.CLEAN_PATTERN.matcher(token.toLowerCase()).replaceAll("");
            if (cleaned.isEmpty() || Utils.STOP_WORDS.contains(cleaned)) {
                continue;
            }

            String stemmed = stemmer.stem(cleaned);
            stemmedList.add(stemmed);
        }
        return stemmedList;
    }
}
