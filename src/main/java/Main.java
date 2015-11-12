import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by han on 8/16/15.
 */
public class Main {

    private static transient final Logger LOG = LoggerFactory.getLogger(Main.class);

    @Option(name = "--json", required = false, usage = "Filename for saving json file")
    File dbJson;

    @Option(name = "--kw", required = true, usage = "Keywords in json")
    File kwJson;

    @Option(name = "--help", usage = "Print this help message")
    boolean help = false;

    private static void printHelp(final CmdLineParser parser) {
        System.out.print("java -jar xiaode.jar");
        parser.printSingleLineUsage(System.out);
        System.out.println();
        parser.printUsage(System.out);
    }

    public static void main(final String[] args) throws IOException {
        Main runner = new Main();

        CmdLineParser parser = new CmdLineParser(runner);
        try {
            parser.parseArgument(args);
        } catch (CmdLineException e) {
            if (runner.help) {
                printHelp(parser);
            }

            // handling of wrong arguments
            System.err.println(e.getMessage());
            parser.printUsage(System.err);
            return;
        }

        if (runner.help) {
            printHelp(parser);
            return;
        }

        runner.run();
    }


    public void run() {
        KeywordNode keywordNode;
        if (kwJson.exists() && !kwJson.isDirectory()) {
            keywordNode = JsonIO.json2KeywordTree(kwJson);
        } else {
            LOG.info("No feedName list found! Generate from scratch!");
            keywordNode = generateKeywordTree();
        }

        FeedDatabase feedDatabase;
        if(dbJson.exists() && !dbJson.isDirectory()) {
            feedDatabase = JsonIO.json2Database(dbJson);
        } else {
            LOG.info("No feeds database found! Generate from scratch!");
            feedDatabase = new FeedDatabase();
        }
        // put write here only for debug
//        JsonIO.writeStoriesData(feedDatabase);

        feedDatabase.traverseKeyword(keywordNode, "Han");

        long startTime = System.currentTimeMillis() - 10000L;
        feedDatabase.updateAll();
        List<StoryItem> uniqueStories = JsonIO.writeStoriesData(feedDatabase);
        JsonIO.database2Json(feedDatabase, dbJson);

        List<StoryItem> newStories = uniqueStories.stream()
                .filter(p -> p.fetchTime > startTime)
                .collect(Collectors.toList());

        if (newStories.size() > 0) {
            List<StoryItem> pushStory = newStories.stream()
                    .sorted(Comparator.comparingLong(StoryItem::getPublishTime))
                    .limit(3)
                    .collect(Collectors.toList());

            Notifier.pushStories2Device(pushStory, newStories.size());
        }

    }


    public KeywordNode generateKeywordTree() {
        KeywordNode categoryNode = new KeywordNode("全球", "经济");
        categoryNode.addChild(new KeywordNode("英国"));
        categoryNode.addChild(new KeywordNode("德国"));
        categoryNode.addChild(new KeywordNode("芬兰"));
        categoryNode.addChild(new KeywordNode("丹麦"));
        categoryNode.addChild(new KeywordNode("土耳其"));
        categoryNode.addChild(new KeywordNode("挪威"));
        categoryNode.addChild(new KeywordNode("法国"));
        categoryNode.addChild(new KeywordNode("荷兰"));
        categoryNode.addChild(new KeywordNode("西班牙"));
        categoryNode.addChild(new KeywordNode("希腊"));
        categoryNode.addChild(new KeywordNode("意大利"));
        categoryNode.addChild(new KeywordNode("俄罗斯"));
        categoryNode.addChild(new KeywordNode("爱尔兰"));
        categoryNode.addChild(new KeywordNode("瑞典"));
        categoryNode.addChild(new KeywordNode("瑞士"));
        categoryNode.addChild(new KeywordNode("乌克兰"));
        categoryNode.addChild(new KeywordNode("比利时"));
        categoryNode.addChild(new KeywordNode("葡萄牙"));
        categoryNode.addChild(new KeywordNode("欧元区"));
        categoryNode.addChild(new KeywordNode("欧洲央行"));
        categoryNode.addChild(new KeywordNode("美国"));
        JsonIO.keywordTree2Json(categoryNode, new File("keywords.json"));
        return categoryNode;
    }

}
