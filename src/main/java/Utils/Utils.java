package Utils;

import java.util.Set;
import java.util.regex.Pattern;

public class Utils {
    public static final Pattern CLEAN_PATTERN = Pattern.compile("[^a-zA-Z0-9 ]");
    public static final Set<String> STOP_WORDS = StopWords.getStopWords();


}
