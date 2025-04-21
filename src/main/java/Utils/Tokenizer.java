package Utils;

import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;

import java.io.InputStream;

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
}
