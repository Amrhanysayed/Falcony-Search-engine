package Indexer;

import Utils.Document;
import Utils.StopWords;
import opennlp.tools.stemmer.PorterStemmer;
import opennlp.tools.tokenize.TokenizerME;
import org.jsoup.Jsoup;

import java.io.InputStream;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Pattern;

public class Indexer {
    private static ConcurrentHashMap<String, List<TermInfo>> invertedIndex;
    private static ConcurrentHashMap<Integer, Document> indexedDocuments;
    private static ConcurrentHashMap<Integer, Document> unindexedDocs;
    private static Tokenizer tokenizer;
    private ExecutorService executor;
    private final int numThreads = 10;

    private static final Set<String> STOP_WORDS = StopWords.getStopWords();
    private static final Pattern CLEAN_PATTERN = Pattern.compile("[^a-zA-Z0-9 ]");
    public Indexer() throws Exception {
        invertedIndex = new ConcurrentHashMap<>();
        indexedDocuments = new ConcurrentHashMap<>();
        unindexedDocs = new ConcurrentHashMap<>();
        tokenizer = new Tokenizer();

    }

    public static void indexDocument(Document document, TokenizerME tokenizer) {
        indexedDocuments.put(document.getId(), document);

        String[] tokens = tokenizer.tokenize(document.content);

        Map<String, Integer> termFreq = new HashMap<>();

        PorterStemmer stemmer = new PorterStemmer();

        Map<String, List<Integer>> positions = new HashMap<>();
        int lastPosition = 0;
        for (String token : tokens) {
            // Clean token using regex
            // TODO: check this regex
            String cleaned = CLEAN_PATTERN.matcher(token.toLowerCase()).replaceAll("");
            System.out.println("Original: " + token + " Cleaned: " + cleaned);
            if (cleaned.isEmpty() || STOP_WORDS.contains(cleaned)) {
                continue;
            }

            // Stem token
            String stemmed = stemmer.stem(cleaned);

            termFreq.put(stemmed, termFreq.getOrDefault(stemmed, 0) + 1);
            positions.computeIfAbsent(stemmed, k -> new ArrayList<>()).add(lastPosition++);
        }

        // Update inverted index
        for (Map.Entry<String, Integer> entry : termFreq.entrySet()) {
            String term = entry.getKey();
            int freq = entry.getValue();
            TermInfo tInfo = new TermInfo(term, freq, document.getId(), positions.computeIfAbsent(term, k -> new ArrayList<>()));
            invertedIndex.computeIfAbsent(term, k -> new ArrayList<>());
            boolean dup = false;
            for (TermInfo termInfo : invertedIndex.get(term)) {
                if(termInfo.getDocId() == document.getId()) {
                    termInfo.setTermInfo(tInfo);
                    dup = true;
                    break;
                }
            }
            if(!dup) {
                invertedIndex.get(term).add(tInfo);
            }
        }
    }

    public void runIndexer() throws Exception {
        executor = Executors.newFixedThreadPool(numThreads);
        try {
            while (!unindexedDocs.isEmpty()) {
                for (Document doc : unindexedDocs.values()) {
                    executor.submit(new IndexerWorker(doc, tokenizer));
                }
                unindexedDocs.clear();
            }
        } finally {
            // Shutdown executor and wait for tasks to complete
            executor.shutdown();
            try {
                if (!executor.awaitTermination(10, TimeUnit.MINUTES)) {
                    executor.shutdownNow(); // Force shutdown if tasks don't finish
                }
                // unindexedDocs = get again
                // if not empty -> call runIndexer AGAIN
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    // For debugging
    public void printIndex() {
        for (Map.Entry<String, List<TermInfo>> entry : invertedIndex.entrySet()) {
            System.out.println("Term: " + entry.getKey());
            for (TermInfo p : entry.getValue()) {
                System.out.println("  DocID: " + p.docId + ", Freq: " + p.getFrequency());
                System.out.println("  Pos: " + p.getPositions());
            }
        }
    }

    public static void main(String[] args) throws Exception {
        Indexer indexer = new Indexer();
        unindexedDocs.put(1, new Document(1, "http://example.com", "Introduction to Java Programming",
                "<html><body>Java Java is a versatile Java programming language used for web and mobile apps. Learn Java basics today!</body></html>"));
        unindexedDocs.put(2, new Document(2, "http://techblog.com", "Building Search Engines",
                "<html><body>Search engines like Google use crawlers and indexers to rank pages. Java is great for such systems.</body></html>"));
        unindexedDocs.put(3, new Document(3, "http://codinghub.com", "Python vs Java",
                "<html><body>Python is concise, but Java’s performance shines in enterprise apps. Compare their syntax!</body></html>"));
        unindexedDocs.put(4, new Document(4, "http://aiworld.com", "Machine Learning Basics",
                "<html><body>AI and machine learning power modern search engines. Train models with Python or Java.</body></html>"));
        unindexedDocs.put(5, new Document(5, "http://webdev.org", "HTML and CSS Guide",
                "<html><body>HTML structures web pages, while CSS styles them. Build responsive sites easily.</body></html>"));
        unindexedDocs.put(6, new Document(6, "http://cloudtech.com", "Cloud Computing Trends",
                "<html><body>Cloud platforms like AWS and Azure support scalable Java applications.</body></html>"));
        unindexedDocs.put(7, new Document(7, "http://datascience.io", "Data Science with Python",
                "<html><body>Analyze data with Pandas and NumPy. Python excels in data science tasks.</body></html>"));
        unindexedDocs.put(8, new Document(8, "http://mobiledev.com", "Android App Development",
                "<html><body>Build Android apps using Java or Kotlin. Start with Android Studio.</body></html>"));
        unindexedDocs.put(9, new Document(9, "http://securitytech.com", "Cybersecurity Essentials",
                "<html><body>Secure your Java applications with encryption and authentication.</body></html>"));
        unindexedDocs.put(10, new Document(10, "http://gamedev.net", "Game Development with Unity",
                "<html><body>Create games using C# or Java. Unity is a popular engine for beginners.</body></html>"));
        unindexedDocs.put(11, new Document(11, "http://iotworld.com", "Internet of Things Overview",
                "<html><body>IoT devices use Java for real-time data processing. Explore smart homes!</body></html>"));
        unindexedDocs.put(12, new Document(12, "http://blockchain.info", "Blockchain Technology",
                "<html><body>Blockchain powers cryptocurrencies. Write smart contracts with Java.</body></html>"));
        unindexedDocs.put(13, new Document(13, "http://devops.io", "DevOps Best Practices",
                "<html><body>Automate deployments with Jenkins and Docker. Java integrates well.</body></html>"));
        unindexedDocs.put(14, new Document(14, "http://bigdata.com", "Big Data Analytics",
                "<html><body>Hadoop and Spark process big data. Java is key for distributed systems.</body></html>"));
        unindexedDocs.put(15, new Document(15, "http://frontend.dev", "React for Web Development",
                "<html><body>React builds dynamic UIs. Pair it with Java backends for full-stack apps.</body></html>"));
        unindexedDocs.put(16, new Document(16, "http://backend.guru", "Spring Boot Tutorial",
                "<html><body>Spring Boot simplifies Java web development. Build REST APIs fast.</body></html>"));
        unindexedDocs.put(17, new Document(17, "http://database.info", "SQL vs NoSQL",
                "<html><body>SQL databases like MySQL work with Java. NoSQL like MongoDB scales well.</body></html>"));
        unindexedDocs.put(18, new Document(18, "http://testing.io", "Software Testing Guide",
                "<html><body>Test Java apps with JUnit and Selenium for reliable code.</body></html>"));
        unindexedDocs.put(19, new Document(19, "http://microservices.com", "Microservices Architecture",
                "<html><body>Build scalable systems with Java and Spring Cloud.</body></html>"));
        unindexedDocs.put(20, new Document(20, "http://apis.dev", "REST API Design",
                "<html><body>Design REST APIs using Java and Jersey for web services.</body></html>"));
        unindexedDocs.put(21, new Document(21, "http://quantum.com", "Quantum Computing Intro",
                "<html><body>Quantum computing is the future. Simulate algorithms with Java.</body></html>"));
        unindexedDocs.put(22, new Document(22, "http://arvr.world", "Augmented Reality Apps",
                "<html><body>Develop AR apps with Java and ARCore for Android.</body></html>"));
        unindexedDocs.put(23, new Document(23, "http://robotics.io", "Robotics Programming",
                "<html><body>Program robots using Java for control systems and AI.</body></html>"));
        unindexedDocs.put(24, new Document(24, "http://fintech.news", "Fintech Innovations",
                "<html><body>Java powers secure financial apps for banking and trading.</body></html>"));
        unindexedDocs.put(25, new Document(25, "http://healthtech.com", "Healthcare IT Solutions",
                "<html><body>Build healthcare apps with Java for patient data management.</body></html>"));
        unindexedDocs.put(26, new Document(26, "http://ecommerce.store", "E-commerce Platforms",
                "<html><body>Create online stores with Java and Magento.</body></html>"));
        unindexedDocs.put(27, new Document(27, "http://education.tech", "EdTech Tools",
                "<html><body>Develop learning platforms with Java for online education.</body></html>"));
        unindexedDocs.put(28, new Document(28, "http://gaming.io", "Multiplayer Game Servers",
                "<html><body>Build game servers with Java for real-time multiplayer.</body></html>"));
        unindexedDocs.put(29, new Document(29, "http://travel.app", "Travel Booking Systems",
                "<html><body>Java powers travel apps for flights and hotels.</body></html>"));
        unindexedDocs.put(30, new Document(30, "http://social.network", "Social Media Platforms",
                "<html><body>Create social networks with Java backends and GraphQL.</body></html>"));
        unindexedDocs.put(31, new Document(31, "http://logistics.io", "Supply Chain Management",
                "<html><body>Optimize logistics with Java-based tracking systems.</body></html>"));
        unindexedDocs.put(32, new Document(32, "http://energy.tech", "Smart Energy Solutions",
                "<html><body>Java monitors energy usage in smart grids.</body></html>"));
        unindexedDocs.put(33, new Document(33, "http://automotive.com", "Automotive Software",
                "<html><body>Develop car systems with Java for infotainment.</body></html>"));
        unindexedDocs.put(34, new Document(34, "http://agritech.farm", "Agricultural Technology",
                "<html><body>Use Java for precision farming and IoT sensors.</body></html>"));
        unindexedDocs.put(35, new Document(35, "http://retail.io", "Retail Management Systems",
                "<html><body>Build POS systems with Java for retail stores.</body></html>"));
        unindexedDocs.put(36, new Document(36, "http://fashion.app", "Fashion E-commerce",
                "<html><body>Create fashion apps with Java and Spring.</body></html>"));
        unindexedDocs.put(37, new Document(37, "http://music.stream", "Music Streaming Services",
                "<html><body>Stream music with Java-based servers.</body></html>"));
        unindexedDocs.put(38, new Document(38, "http://video.platform", "Video Streaming Guide",
                "<html><body>Build video platforms with Java and HLS.</body></html>"));
        unindexedDocs.put(39, new Document(39, "http://news.site", "Online News Portals",
                "<html><body>Deliver news with Java-powered CMS systems.</body></html>"));
        unindexedDocs.put(40, new Document(40, "http://sports.app", "Sports Apps Development",
                "<html><body>Create sports apps with Java for live scores.</body></html>"));
        unindexedDocs.put(41, new Document(41, "http://fitness.guru", "Fitness Tracking Apps",
                "<html><body>Track workouts with Java and wearable APIs.</body></html>"));
        unindexedDocs.put(42, new Document(42, "http://food.delivery", "Food Delivery Systems",
                "<html><body>Build delivery apps with Java and GPS.</body></html>"));
        unindexedDocs.put(43, new Document(43, "http://realestate.io", "Real Estate Platforms",
                "<html><body>Develop property apps with Java and GIS.</body></html>"));
        unindexedDocs.put(44, new Document(44, "http://jobs.site", "Job Search Portals",
                "<html><body>Create job boards with Java and Elasticsearch.</body></html>"));
        unindexedDocs.put(45, new Document(45, "http://events.app", "Event Management Tools",
                "<html><body>Organize events with Java-based ticketing.</body></html>"));
        unindexedDocs.put(46, new Document(46, "http://photography.site", "Photography Portfolios",
                "<html><body>Showcase photos with Java-powered galleries.</body></html>"));
        unindexedDocs.put(47, new Document(47, "http://art.gallery", "Online Art Galleries",
                "<html><body>Display art with Java and WebGL.</body></html>"));
        unindexedDocs.put(48, new Document(48, "http://charity.org", "Charity Platforms",
                "<html><body>Build donation systems with Java and Stripe.</body></html>"));
        unindexedDocs.put(49, new Document(49, "http://pets.app", "Pet Care Applications",
                "<html><body>Manage pet care with Java-based apps.</body></html>"));
        unindexedDocs.put(50, new Document(50, "http://weather.io", "Weather Forecasting Tools",
                "<html><body>Predict weather with Java and APIs.</body></html>"));

        indexer.runIndexer();
        indexer.printIndex();
    }
}
