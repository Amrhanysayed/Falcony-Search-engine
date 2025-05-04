package Backend;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "images")
public class Image  {
    @Id
    private String id;
    private float[] features;
    private String url;
    private String docUrl;

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public float[] getFeatures() { return features; }
    public void setFeatures(float[] features) { this.features = features; }
    public void setUrl(String url) { this.url = url; }
    public String getUrl() { return url; }
    public void setDocUrl(String docUrl) { this.docUrl = docUrl; }
    public String getDocUrl() { return docUrl; }
}