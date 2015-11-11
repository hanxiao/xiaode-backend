import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.CollectionAdapter;
import utils.LZString;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by han on 9/25/15.
 */
public class JsonIO {
    private static transient final Logger LOG = LoggerFactory.getLogger(JsonIO.class);


    private static void writeNDaysBefore(File outFile, List<StoryItem> tmpStoriesUnique, int daysBefore) {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -daysBefore);
        long weekBefore = cal.getTime().getTime();
        List<StoryItem> day7Stories = tmpStoriesUnique.stream()
                .filter(p -> p.publishTime > weekBefore)
                .collect(Collectors.toList());

        Gson gson = new GsonBuilder()
                .registerTypeHierarchyAdapter(Collection.class, new CollectionAdapter()).create();
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

    public static void writeStoriesData(FeedDatabase feedDatabase) {
        List<StoryItem> tmpStories = feedDatabase.getAllFeeds().values().stream().flatMap(p -> p.allStories.stream())
                .sorted((e1, e2) -> Long.compare(e2.publishTime, e1.publishTime))
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

        double posAvg = posStat.getAverage();
        double negAvg = negStat.getAverage();

        double posStd = Math.sqrt(tmpStoriesUnique.stream().flatMap(p -> p.sourceArticles.stream())
                .mapToDouble(p -> (p.posFactor - posAvg) * (p.posFactor - posAvg)).sum() / posStat.getCount());

        double negStd = Math.sqrt(tmpStoriesUnique.stream().flatMap(p -> p.sourceArticles.stream())
                .mapToDouble(p -> (p.negFactor - negAvg) * (p.negFactor - negAvg)).sum() / negStat.getCount());

        tmpStoriesUnique.stream().flatMap(p -> p.sourceArticles.stream())
                .forEach(p -> p.normalizeScore(posAvg, negAvg, posStd, negStd));

//        Map<String, Map<String, Double>> posChart =  tmpStoriesUnique.stream()
//                .collect(Collectors.groupingBy(StoryItem::getKeyword ,
//                        Collectors.groupingBy(StoryItem::getPublishDate,
//                                Collectors.averagingDouble(p ->
//                                        p.sourceArticles.stream().mapToDouble(j -> j.posFactor)
//                                                .average().getAsDouble()))));
//
//
//        Map<String, Map<String, Double>> negChart =  tmpStoriesUnique.stream()
//                .collect(Collectors.groupingBy(StoryItem::getKeyword,
//                        Collectors.groupingBy(StoryItem::getPublishDate,
//                                Collectors.averagingDouble(p ->
//                                        p.sourceArticles.stream().mapToDouble(j -> j.negFactor)
//                                                .average().getAsDouble()))));

        writeNDaysBefore(new File("database-3days.json"), tmpStoriesUnique, 3);
        writeNDaysBefore(new File("database-week.json"), tmpStoriesUnique, 7);
        writeNDaysBefore(new File("database-month.json"), tmpStoriesUnique, 31);


        Map<Integer, List<StoryItem>> storyGroup =
        tmpStoriesUnique.stream().collect(Collectors.groupingBy(StoryItem::getIdByGroup));

//        List<String> sourceLinksUnique =
//                tmpStoriesUnique.stream().flatMap(p -> p.sourceArticles.stream())
//                .map(p -> p.sourceLink).collect(Collectors.toList());

//        HashMap<String, String> domainFavIcons = new HashMap<>();
//        for (String sourceLink : sourceLinksUnique) {
//            try {
//                URI tmpUri = new URI(sourceLink);
//                String hostName = tmpUri.getHost();
//                if (!domainFavIcons.containsKey(hostName)) {
//                    LOG.info("Downloading {} favicon ...", hostName );
//                    BufferedImage img = ImageIO.read(new URL("https://www.google.com/s2/favicons?domain_url=" + hostName));
//                    ByteArrayOutputStream os = new ByteArrayOutputStream();
//                    ImageIO.write(img, "png", Base64.getEncoder().wrap(os));
//                    domainFavIcons.put(hostName, os.toString("UTF-8"));
//                }
//            } catch (URISyntaxException ex) {
//                throw new RuntimeException(ex);
//            } catch (MalformedURLException ex) {
//                LOG.error("image favicon is not correct");
//            } catch (IOException ex) {
//                LOG.error("can not load favicon");
//            }
//        }

//        favIcon2Json(domainFavIcons, new File("favicons.json"));

        for (Map.Entry<Integer, List<StoryItem>> entry : storyGroup.entrySet()) {
            writeNDaysBefore(new File("database-" + entry.getKey().toString() + ".json"), entry.getValue(), 99);
        }

        //get all users from device server
        // send notification
        // to ios
        // to android

    }


    private static void favIcon2Json(HashMap<String, String> favIcons, File outFile) {
        Gson gson = new GsonBuilder()
                .registerTypeHierarchyAdapter(Collection.class, new CollectionAdapter()).create();
        String jsonOutput = gson.toJson(favIcons);

        writeToFile(outFile, jsonOutput);
    }

    public static void database2Json(FeedDatabase feedDatabase, File outFile) {
        Gson gson = new GsonBuilder()
                .registerTypeHierarchyAdapter(Collection.class, new CollectionAdapter()).create();
        String jsonOutput = gson.toJson(feedDatabase);

        writeToFile(outFile, jsonOutput);
        LOG.info("Compressing...");
        String jsonCompressed = LZString.compressToEncodedURIComponent(jsonOutput);

        writeToFile(new File(outFile.getName() + ".lz"), jsonCompressed);

    }

    public static void keywordTree2Json(KeywordNode keywordNode, File outFile) {
        Gson gson = new GsonBuilder()
                .registerTypeHierarchyAdapter(Collection.class, new CollectionAdapter()).create();
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
        Gson gson = new GsonBuilder()
                .registerTypeHierarchyAdapter(Collection.class, new CollectionAdapter()).create();
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
        Gson gson = new GsonBuilder()
                .registerTypeHierarchyAdapter(Collection.class, new CollectionAdapter()).create();
        FeedDatabase feedDatabase = null;
        try {
            String content = new Scanner(inFile).useDelimiter("\\Z").next();
            feedDatabase = gson.fromJson(content, FeedDatabase.class);
        } catch (IOException e) {
            LOG.error("Error {} whe reading files", e);
        }
        return feedDatabase;
    }

}
