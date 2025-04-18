package Utils;

public class Document {
    int docId;
    public String url, title, content;

    public Document(int docId, String url, String title, String content) {
        this.docId = docId;
        this.url = url;
        this.title = title;
        this.content = content;
    }
    public int getId() {
        return docId;
    }
}
