package Utils;

public class WebDocument {
    int docId;
    public String url, title, html, content;

    public WebDocument(int docId, String url, String title, String content) {
        this.docId = docId;
        this.url = url;
        this.title = title;
        this.content = content;
        //System.out.println(this.content);
    }
    public int getId() {
        return docId;
    }
}
