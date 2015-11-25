import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.rometools.utils.Strings;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.imgscalr.Scalr;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.CollectionAdapter;
import utils.LZString;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Created by han on 9/25/15.
 */
public class JsonIO {
    private static transient final Logger LOG = LoggerFactory.getLogger(JsonIO.class);
    private static Gson gson = new GsonBuilder()
            .registerTypeHierarchyAdapter(Collection.class, new CollectionAdapter()).create();


    private static void writeNDaysBefore(File outFile, List<StoryItem> tmpStoriesUnique, int daysBefore) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -daysBefore);
        long weekBefore = cal.getTime().getTime();
        List<StoryItem> day7Stories = tmpStoriesUnique.stream()
                .filter(p -> p.publishTime > weekBefore)
                .collect(Collectors.toList());

        String jsonOutput = gson.toJson(day7Stories);

        writeToFile(outFile, jsonOutput);

        LOG.info("Compressing...");
        String jsonCompressed = LZString.compressToEncodedURIComponent(jsonOutput);

        writeToFile(new File(outFile.getName() + ".lz"), jsonCompressed);

    }

    private static void writeToFile (File outFile, String jsonOutput) {
        try {
            PrintWriter writer = new PrintWriter(new FileOutputStream(outFile, false));
            writer.println(jsonOutput);
            writer.flush();
            writer.close();
        } catch (IOException ex) {
            LOG.error("Could not save Database {}", outFile, ex);
        }
    }

    public static List<StoryItem> writeStoriesData(FeedDatabase feedDatabase) {
        List<StoryItem> tmpStories = feedDatabase.getAllFeeds()
                .values().stream().flatMap(p -> p.allStories.stream())
                .sorted((e1, e2) -> Long.compare(e2.publishTime, e1.publishTime))
                .filter(Objects::nonNull)
                .filter(p -> Objects.nonNull(p.sourceArticles))
                .filter(p -> Objects.nonNull(p.summary)
                        && p.summary.trim().length() > 0)
                .collect(Collectors.toList());

        ArrayList<StoryItem> tmpStoriesUnique = new ArrayList<StoryItem>();
        for (StoryItem storyItem1 : tmpStories) {
            boolean hasSimilar = false;
            for (StoryItem storyItem2 : tmpStoriesUnique) {
                hasSimilar = StoryItem.isSimilar(storyItem1, storyItem2);
                if (hasSimilar) {
                    storyItem2.mergeWith(storyItem1);
                    break;
                }
            }
            if (!hasSimilar) {
                tmpStoriesUnique.add(storyItem1.copy());
            }
        }
        try {
            List<String> posDict = FileUtils.readLines(new File("chinese-pos.txt"), "utf-8");
            List<String> negDict = FileUtils.readLines(new File("chinese-neg.txt"), "utf-8");

            tmpStoriesUnique.stream().flatMap(p -> p.sourceArticles.stream())
                    .forEach(p -> p.sentimentAnalysis(posDict, negDict));
        } catch (IOException ex) {
            LOG.info("Error when reading sentiment words!");
        }


        DoubleSummaryStatistics posStat =  tmpStoriesUnique.stream().flatMap(p -> p.sourceArticles.stream())
                .mapToDouble(p -> p.posFactor).summaryStatistics();
        DoubleSummaryStatistics negStat =  tmpStoriesUnique.stream().flatMap(p -> p.sourceArticles.stream())
                .mapToDouble(p -> p.negFactor).summaryStatistics();
        DoubleSummaryStatistics neuStat =  tmpStoriesUnique.stream().flatMap(p -> p.sourceArticles.stream())
                .mapToDouble(p -> p.neutralFactor).summaryStatistics();

        double posAvg = posStat.getAverage();
        double negAvg = negStat.getAverage();
        double neuAvg = neuStat.getAverage();

        double posStd = Math.sqrt(tmpStoriesUnique.stream().flatMap(p -> p.sourceArticles.stream())
                .mapToDouble(p -> (p.posFactor - posAvg) * (p.posFactor - posAvg)).sum() / posStat.getCount());

        double negStd = Math.sqrt(tmpStoriesUnique.stream().flatMap(p -> p.sourceArticles.stream())
                .mapToDouble(p -> (p.negFactor - negAvg) * (p.negFactor - negAvg)).sum() / negStat.getCount());

        double neuStd = Math.sqrt(tmpStoriesUnique.stream().flatMap(p -> p.sourceArticles.stream())
                .mapToDouble(p -> (p.neutralFactor - neuAvg) * (p.neutralFactor - neuAvg)).sum() / neuStat.getCount());

        tmpStoriesUnique.stream().flatMap(p -> p.sourceArticles.stream())
                .forEach(p -> p.normalizeScore(posAvg, negAvg, neuAvg, posStd, negStd, neuStd));

        Map<String, Map<String, Double>> posChart =  tmpStoriesUnique.stream()
                .collect(Collectors.groupingBy(StoryItem::getKeyword ,
                        Collectors.groupingBy(StoryItem::getPublishDate,
                                Collectors.averagingDouble(p ->
                                        p.sourceArticles.stream().mapToDouble(j -> j.posFactor)
                                                .average().orElse(0)))));


        Map<String, Map<String, Double>> negChart =  tmpStoriesUnique.stream()
                .collect(Collectors.groupingBy(StoryItem::getKeyword,
                        Collectors.groupingBy(StoryItem::getPublishDate,
                                Collectors.averagingDouble(p ->
                                        p.sourceArticles.stream().mapToDouble(j -> j.negFactor)
                                                .average().orElse(0)))));

        Map<String, Map<String, Double>> neuChart =  tmpStoriesUnique.stream()
                .collect(Collectors.groupingBy(StoryItem::getKeyword,
                        Collectors.groupingBy(StoryItem::getPublishDate,
                                Collectors.averagingDouble(p ->
                                        p.sourceArticles.stream().mapToDouble(j -> j.neutralFactor)
                                                .average().orElse(0)))));

        writeNDaysBefore(new File("database-3days.json"), tmpStoriesUnique, 3);
        writeNDaysBefore(new File("database-week.json"), tmpStoriesUnique, 7);
        writeNDaysBefore(new File("database-month.json"), tmpStoriesUnique, 31);

        writeSentiment(neuChart, new File("senti-neutral.json"));
        writeSentiment(posChart, new File("senti-positive.json"));
        writeSentiment(posChart, new File("senti-negative.json"));

        Map<Integer, List<StoryItem>> storyGroup =
                tmpStoriesUnique.stream().collect(Collectors.groupingBy(StoryItem::getIdByGroup));


        for (Map.Entry<Integer, List<StoryItem>> entry : storyGroup.entrySet()) {
            writeNDaysBefore(new File("database-" + entry.getKey().toString() + ".json"), entry.getValue(), 99);
        }

        return tmpStoriesUnique;

    }

    public static void writeSentiment(Map<String, Map<String, Double>> sentiData, File outFile) {

        String jsonOutput = gson.toJson(sentiData);

        writeToFile(outFile, jsonOutput);
        LOG.info("Compressing...");
        String jsonCompressed = LZString.compressToEncodedURIComponent(jsonOutput);

        writeToFile(new File(outFile.getName() + ".lz"), jsonCompressed);
    }


    public static void downloadImg4Stories(List<StoryItem> tmpStoriesUnique) {

        tmpStoriesUnique.stream()
                .filter(p -> Objects.nonNull(p.mainImage)
                        && !Strings.isEmpty(p.mainImage))
                .parallel()
                .forEach(p -> {
                    try {
                        File thumbFile = new File(String.format("thumbnail/%d.jpg", p.id));
                        if (!thumbFile.exists()) {
                            URL url = new URL(p.mainImage);
                            ImageIcon icon = new ImageIcon(url);
                            BufferedImage image = new BufferedImage(
                                    icon.getIconWidth(),
                                    icon.getIconHeight(),
                                    BufferedImage.TYPE_INT_RGB);

                            Graphics g = image.createGraphics();
                            icon.paintIcon(null, g, 0, 0);
                            g.dispose();

                            BufferedImage thumbnail =
                                    Scalr.resize(image, Scalr.Method.SPEED, Scalr.Mode.FIT_TO_WIDTH,
                                            300, 300, Scalr.OP_ANTIALIAS);
                            ImageIO.write(thumbnail, "jpg", thumbFile);
                            LOG.info("write thumbnail {}...", thumbFile.getName());
                        }
                    } catch (MalformedURLException ex) {
                        LOG.error("URL is not incorrect format");
                        ex.printStackTrace();
                    } catch (IOException ex) {
                        LOG.error("Can not write image file to disk");
                        ex.printStackTrace();
                    }
                });
    }


    public static void favIcon2Json(List<StoryItem> tmpStoriesUnique, File outFile) {

        int nrOfThreads = 8;
        ExecutorService executor = Executors.newFixedThreadPool(nrOfThreads);
        final AtomicInteger nrOfJobs = new AtomicInteger(0);

        List<String> sourceLinksUnique =
                tmpStoriesUnique.stream().flatMap(p -> p.sourceArticles.stream())
                        .map(p -> p.sourceLink).collect(Collectors.toList());

        HashMap<String, String> domainFavIcons = new HashMap<>();
        for (String sourceLink : sourceLinksUnique) {
            while (nrOfJobs.get() > nrOfThreads) {
                try {
                    Thread.sleep(5);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            executor.execute(new Runnable() {
                public void run() {
                    try {
                        URI tmpUri = new URI(sourceLink);
                        String hostName = tmpUri.getHost();
                        if (!domainFavIcons.containsKey(hostName)) {
                            LOG.info("Downloading {} favicon ...", hostName );
                            BufferedImage img = (BufferedImage) new ImageIcon(new URL("https://www.google.com/s2/favicons?domain_url=" + hostName)).getImage();
                            ByteArrayOutputStream os = new ByteArrayOutputStream();
                            ImageIO.write(img, "png", Base64.getEncoder().wrap(os));
                            domainFavIcons.put(hostName, os.toString("UTF-8"));
                        }
                    } catch (URISyntaxException ex) {
                        throw new RuntimeException(ex);
                    } catch (MalformedURLException ex) {
                        LOG.error("image favicon is not correct");
                    } catch (IOException ex) {
                        LOG.error("can not load favicon");
                    }
                    nrOfJobs.decrementAndGet();
                }
            });

        }

        executor.shutdown();
        try {
            executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }


        String jsonOutput = gson.toJson(domainFavIcons);

        writeToFile(outFile, jsonOutput);
    }

    public static void database2Json(FeedDatabase feedDatabase, File outFile) {

        String jsonOutput = gson.toJson(feedDatabase);

        writeToFile(outFile, jsonOutput);
        LOG.info("Compressing...");
        String jsonCompressed = LZString.compressToEncodedURIComponent(jsonOutput);

        writeToFile(new File(outFile.getName() + ".lz"), jsonCompressed);

    }

    public static void keywordTree2Json(KeywordNode keywordNode, File outFile) {
        String jsonOutput = gson.toJson(keywordNode);

        try {
            PrintWriter writer = new PrintWriter(new FileOutputStream(outFile, false));
            writer.println(jsonOutput);
            writer.flush();
            writer.close();
        } catch (IOException ex) {
            LOG.error("Could not save Database {}", outFile, ex);
        }
    }

    public static KeywordNode json2KeywordTree(File inFile) {

        KeywordNode keywordNode = null;
        try {
            String content = new Scanner(inFile).useDelimiter("\\Z").next();
            keywordNode = gson.fromJson(content, KeywordNode.class);
        } catch (IOException e) {
            LOG.error("Error {} whe reading files", e);
        }
        return keywordNode;
    }

    public static FeedDatabase json2Database(File inFile) {

        FeedDatabase feedDatabase = null;
        try {
            String content = new Scanner(inFile).useDelimiter("\\Z").next();
            feedDatabase = gson.fromJson(content, FeedDatabase.class);
        } catch (IOException e) {
            LOG.error("Error {} whe reading files", e);
        }
        return feedDatabase;
    }

    public static HashSet<Integer> loadPushId(File inFile) {
        HashSet<Integer> ids = null;
        if (inFile.exists() && !inFile.isDirectory()) {
            try {
                String content = new Scanner(inFile).useDelimiter("\\Z").next();
                ids = gson.fromJson(content, HashSet.class);
            } catch (IOException e) {
                LOG.error("Error {} whe reading files", e);
            }
        } else {
            LOG.warn("No pushed list found! Generate from scratch!");
            ids =  new HashSet<Integer>();
        }

        return ids;
    }

    public static void pushId2Json(HashSet<Integer> pushId, File outFile) {
        String jsonOutput = gson.toJson(pushId);

        try {
            PrintWriter writer = new PrintWriter(new FileOutputStream(outFile, false));
            writer.println(jsonOutput);
            writer.flush();
            writer.close();
        } catch (IOException ex) {
            LOG.error("Could not save Database {}", outFile, ex);
        }
    }


    public static Set<String> loadVisitedStories(File inFile) {
        Set<String> ids = null;
        if (inFile.exists() && !inFile.isDirectory()) {
            try {
                String content = new Scanner(inFile).useDelimiter("\\Z").next();
                ids = gson.fromJson(content, HashSet.class);
            } catch (IOException e) {
                LOG.error("Error {} whe reading files", e);
            }
        } else {
            LOG.warn("No visited stories found! Generate from scratch!");
            ids = Sets.newConcurrentHashSet();
        }

        return ids;
    }

    public static void writeVisitedStories(Set<String> pushId, File outFile) {
        String jsonOutput = gson.toJson(pushId);

        try {
            PrintWriter writer = new PrintWriter(new FileOutputStream(outFile, false));
            writer.println(jsonOutput);
            writer.flush();
            writer.close();
        } catch (IOException ex) {
            LOG.error("Could not save Database {}", outFile, ex);
        }
    }

    public static void writeDeviceIDs(List<Device> deviceList, File outFile) {
        String jsonOutput = gson.toJson(deviceList.toArray());

        try {
            PrintWriter writer = new PrintWriter(new FileOutputStream(outFile, false));
            writer.println(jsonOutput);
            writer.flush();
            writer.close();
        } catch (IOException ex) {
            LOG.error("Could not save Database {}", outFile, ex);
        }
    }
}
