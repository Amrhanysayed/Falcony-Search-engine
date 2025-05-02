package ImageSearching;

import ai.onnxruntime.OrtException;
import com.mongodb.client.AggregateIterable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.bson.Document;
import javax.annotation.PostConstruct;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class ImageSearchService {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private ImageFeatureExtractor featureExtractor;

    @PostConstruct
    public void createIndexes() {
        // Create indexes for efficient search
        mongoTemplate.indexOps("images").ensureIndex(
                new Index().on("features", Sort.Direction.ASC)
        );
    }

    public void saveImage(MultipartFile file) throws Exception {
        float[] features = featureExtractor.extractFeatures(file.getInputStream().readAllBytes());
        Image image = new Image();
        image.setFeatures(features);
        imageRepository.save(image);
    }

    public void processImageFromUrl(String url) throws Exception {
        System.out.println("Processing image from " + url);

        // Validate URL
        if (url == null || url.trim().isEmpty()) {
            throw new IllegalArgumentException("Image URL cannot be empty");
        }

        // Fetch image
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("User-Agent", "Mozilla/5.0")
                .GET()
                .build();
        HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
        byte[] imageData = response.body();

        // Verify it's an image
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(imageData)) {
            BufferedImage image = ImageIO.read(inputStream);
            if (image == null) {
                throw new IOException("URL does not point to a valid image: " + url);
            }
        }

        // Extract features
        float[] features = featureExtractor.extractFeatures(imageData);

        // Create and save Image object
        Image image = new Image();
        // TODO: save unique image ID (hash)
        image.setId(UUID.randomUUID().toString());
        image.setFeatures(features);
        image.setUrl(url);
        mongoTemplate.save(image, "images");
        System.out.println("Saved image with ID: " + image.getId() + " and url: " + url);
    }

    public float[] extractFeatures(MultipartFile file) throws Exception {
        try {
            return featureExtractor.extractFeatures(file.getBytes());
        } catch (IOException e) {
            throw new RuntimeException("Failed to extract features", e);
        }
    }

    public List<Image> searchSimilarImages(MultipartFile file) throws Exception {
        try {
            // Extract features of the input image
            float[] queryFeatures = featureExtractor.extractFeatures(file.getBytes());
            List<Double> vectorList = IntStream.range(0, queryFeatures.length)
                    .mapToObj(i -> (double) queryFeatures[i])
                    .toList();

            List<Document> pipeline = List.of(
                    new Document("$search", new Document()
                            .append("index", "features_idx")  // Or your index name
                            .append("knnBeta", new Document()
                                    .append("vector", vectorList)
                                    .append("path", "features")
                                    .append("k", 10)  // Top 10 results
                            )
                    )
            );

            AggregateIterable<Document> result = mongoTemplate.getCollection("images").aggregate(pipeline);
            List<Image> resultList = new ArrayList<>();
            for (Document doc : result) {
                Image image = new Image();
                image.setId(doc.getString("_id"));
                image.setUrl(doc.getString("url"));

                List<Double> featuresList = (List<Double>) doc.get("features");
                float[] features = new float[featuresList.size()];
                for (int i = 0; i < featuresList.size(); i++) {
                    features[i] = featuresList.get(i).floatValue();
                }

                // 🔥 Calculate similarity score (confidence)
                float similarity = calculateCosineSimilarity(queryFeatures, features);
                System.out.println("Similarity for image " + image.getUrl() + ": " + similarity);
                if(similarity > 0.3) {
                    resultList.add(image);
                }
            }
            // Return top 10 similar images
            return resultList;
        } catch (IOException e) {
            throw new RuntimeException("Failed to search images", e);
        }
    }

    float calculateCosineSimilarity(float[] v1, float[] v2) {
        float dot = 0, norm1 = 0, norm2 = 0;
        for (int i = 0; i < v1.length; i++) {
            dot += v1[i] * v2[i];
            norm1 += v1[i] * v1[i];
            norm2 += v2[i] * v2[i];
        }
        return dot / ((float)Math.sqrt(norm1) * (float)Math.sqrt(norm2));
    }

}