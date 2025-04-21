package Utils;

public class WebDocument {
    public String docId, url, title, html, content;

    public WebDocument(String docId, String url, String title, String content) {
        this.docId = docId;
        this.url = url;
        this.title = title;
        this.content = content;
        //System.out.println(this.content);
    }
    public String getId() {
        return docId;
    }
}
