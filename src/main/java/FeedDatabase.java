import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by han on 8/15/15.
 */
public class FeedDatabase implements Serializable {
    private static transient final Logger LOG = LoggerFactory.getLogger(FeedDatabase.class);
    private HashMap<String, User> allUsers = new HashMap<String, User>();

    public HashMap<String, FeedItem> getAllFeeds() {
        return allFeeds;
    }

    private HashMap<String, FeedItem> allFeeds = new HashMap<String, FeedItem>();
    private int nrOfThreads = 4 * Runtime.getRuntime().availableProcessors();




    public void saveFile(final File model) {
        try {
            ObjectOutputStream out = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(model)));
            out.writeObject(this);
            out.close();
        } catch (IOException e) {
            LOG.error("Could not save Database {}", model, e);
        }
    }


    public static FeedDatabase loadFile(final File model) {
        try {
            LOG.info("Loading database...");

            ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(new FileInputStream(model)));
            FeedDatabase mat = (FeedDatabase) in.readObject();
            mat.nrOfThreads = Runtime.getRuntime().availableProcessors();
            in.close();
            return mat;

        } catch (Exception e) {
            LOG.error("Could not load database {}", model, e);
        }
        return null;
    }


    public void touchUser(String usr) {
        allUsers.get(usr).updateTime = System.currentTimeMillis();
    }

    private void addUser(User user) {
        allUsers.put(user.name, user);
    }


    public void addKeyword(KeywordNode keywordNode, String user) {
        if (!allUsers.containsKey(user)) {
            allUsers.put(user, new User(user));
        }
        allUsers.get(user).keywordNodes.add(keywordNode);

        if (!allFeeds.containsKey(keywordNode.query)) {
            allFeeds.put(keywordNode.query, new FeedItem(keywordNode, user));
        }
        allFeeds.get(keywordNode.query).followers.add(user);
    }

    public void traverseKeyword(KeywordNode keywordNode, String user) {
        addKeyword(keywordNode, user);
        LOG.info("Keyword: {} with query {} is added by user {}", keywordNode.name, keywordNode.query, user);
        if (keywordNode.children != null) {
            for (KeywordNode child : keywordNode.children) {
                traverseKeyword(child, user);
            }
        }
    }

    public void updateAll() {
        ExecutorService executor = Executors.newFixedThreadPool(nrOfThreads);
        final AtomicInteger nrOfJobs = new AtomicInteger(0);
        for (final FeedItem feedItem : allFeeds.values()) {

            while (nrOfJobs.get() > nrOfThreads) {
                try {
                    Thread.sleep(5);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            executor.execute(new Runnable() {
                public void run() {
                    feedItem.update();
                    for (String usr: feedItem.followers) {
                        touchUser(usr);
                    }
                    LOG.info("{} is updated", feedItem.feedName);
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
    }



}
