package Utils;

import java.util.List;
import java.util.Map;

public class Posting {
    private Map<String, Integer> freqs;
    public String docId;

    /*
        count in title
        count in headers (h1, h2)
        count in meta
        count in body
     */

    public Posting(String token, String docId, Map<String, Integer> freqs) {
        this.freqs = freqs;
        this.docId = docId;
    }

    public Map<String, Integer> getFrequencies() {
        return freqs;
    }
    public void setFrequency(String key, int frequency) {
        freqs.putIfAbsent(key, frequency);
    }
    public int getFrequency(String key) {
        return freqs.getOrDefault(key, 0);
    }

    public String getDocId() {
        return docId;
    }

    public void setTokenInfo(Posting tInfo) {
        this.freqs = tInfo.freqs;
    }
}
