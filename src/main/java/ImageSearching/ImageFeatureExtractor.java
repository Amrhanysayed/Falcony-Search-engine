package ImageSearching;

import ai.onnxruntime.*;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Collections;


public class ImageFeatureExtractor {

    private OrtEnvironment env;
    private OrtSession session;

    private static final int WIDTH = 224;
    private static final int HEIGHT = 224;

    public ImageFeatureExtractor() throws OrtException, IOException {
        env = OrtEnvironment.getEnvironment();
        String modelPath = "src/main/resources/models/model.onnx"; // Adjust to your actual path
        session = env.createSession(modelPath, new OrtSession.SessionOptions());
        System.out.println("✅ DINOv2 ViT-B/16 model loaded.");
    }

    public float[] extractFeatures(byte[] imageBytes) throws IOException, OrtException {
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageBytes));
        if (image == null) throw new IOException("Invalid image");

        float[][][][] input = preprocess(image); // [1, 3, 224, 224]
        OnnxTensor inputTensor = OnnxTensor.createTensor(env, input);

        // IMPORTANT: DINOv2 ONNX expects "input" and returns "output"
        OrtSession.Result result = session.run(Collections.singletonMap("input", inputTensor));

        float[][] pooled = (float[][]) result.get("pooler_output").get().getValue(); // [1][768]
        return normalizeVector(pooled[0]);
    }

    private float[][][][] preprocess(BufferedImage original) {
        BufferedImage resized = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = resized.createGraphics();
        g.drawImage(original, 0, 0, WIDTH, HEIGHT, null);
        g.dispose();

        float[][][][] input = new float[1][3][HEIGHT][WIDTH]; // CHW

        for (int y = 0; y < HEIGHT; y++) {
            for (int x = 0; x < WIDTH; x++) {
                Color color = new Color(resized.getRGB(x, y));
                // ImageNet normalization (same as CLIP)
                input[0][0][y][x] = ((color.getRed() / 255.0f) - 0.485f) / 0.229f;
                input[0][1][y][x] = ((color.getGreen() / 255.0f) - 0.456f) / 0.224f;
                input[0][2][y][x] = ((color.getBlue() / 255.0f) - 0.406f) / 0.225f;
            }
        }

        return input;
    }

    private float[] normalizeVector(float[] vector) {
        float norm = 0f;
        for (float v : vector) norm += v * v;
        norm = (float) Math.sqrt(norm);
        if (norm == 0f) return vector;

        float[] out = new float[vector.length];
        for (int i = 0; i < vector.length; i++) {
            out[i] = vector[i] / norm;
        }
        return out;
    }
}
