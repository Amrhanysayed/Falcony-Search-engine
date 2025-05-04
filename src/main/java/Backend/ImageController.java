package Backend;

import ImageSearching.ImageFeatureExtractor;
import ai.onnxruntime.OrtException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.List;

@RestController
public class ImageController {
    private ImageFeatureExtractor featureExtractor;
    @Autowired
    private ImageSearchService imageSearchService;

    @PostConstruct
    public void init() throws IOException, OrtException {
        featureExtractor = new ImageFeatureExtractor();
    }

    @CrossOrigin(origins = "*")
    @PostMapping("/extract-features")
    public float[] extractFeatures(@RequestParam("file") MultipartFile file) throws Exception {
        return imageSearchService.extractFeatures(file);
    }

    @CrossOrigin(origins = "*")
    @PostMapping("/upload")
    public void uploadImage(@RequestParam("file") MultipartFile file) throws Exception {
        imageSearchService.saveImage(file);
    }

    @CrossOrigin(origins = "*")
    @PostMapping("/upload-from-url")
    public void uploadImageFromUrl(@RequestParam("url") String url) throws Exception {
        imageSearchService.processImageFromUrl(url);
    }

    @CrossOrigin(origins = "*")
    @PostMapping("/search_images")
    public List<Image> searchSimilarImages(@RequestParam("file") MultipartFile file) throws Exception {
        return imageSearchService.searchSimilarImages(file);
    }
}