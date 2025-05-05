package Backend;

import Utils.WebDocument;

import java.util.List;

public class ResultsResponse {
    private int total;
    private List<WebDocument> docs;

    public ResultsResponse(int total, List<WebDocument> docs) {
        this.total = total;
        this.docs = docs;
    }

    // Add getters and setters
    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public List<WebDocument> getDocs() {
        return docs;
    }

    public void setDocs(List<WebDocument> docs) {
        this.docs = docs;
    }
}