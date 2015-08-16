import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by han on 8/15/15.
 */
public class AllDatabase {
    private static final Logger LOG = LoggerFactory.getLogger(AllDatabase.class);
    public HashMap<String, User> allUsers = new HashMap<String, User>();
    public HashMap<String, KeywordItem> allKeywords = new HashMap<String, KeywordItem>();
    int nrOfThreads = Runtime.getRuntime().availableProcessors();
    ExecutorService executor = Executors.newFixedThreadPool(nrOfThreads);

    private void addUser(User user) {
        allUsers.put(user.name, user);
    }


    private void addKeyword(KeywordItem keywordItem) {
        allKeywords.put(keywordItem.keyword, keywordItem);
    }


    public void addKeyword(String keyword, String user) {
        if (!allUsers.containsKey(user)) {
            allUsers.put(user, new User(user));
        }
        allUsers.get(user).keywords.add(keyword);

        if (!allKeywords.containsKey(keyword)) {
            allKeywords.put(keyword, new KeywordItem(keyword, user));
        }
        allKeywords.get(keyword).followers.add(user);
    }

    public void updateAll() {

        final AtomicInteger nrOfJobs = new AtomicInteger(0);
        for (final KeywordItem keywordItem : allKeywords.values()) {

            while (nrOfJobs.get() > nrOfThreads) {
                try {
                    Thread.sleep(5);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            executor.execute(new Runnable() {
                public void run() {
                    keywordItem.update();
                    LOG.info("{} is updated", keywordItem.keyword);
                    nrOfJobs.decrementAndGet();
                }
            });

            executor.shutdown();
            try {
                executor.awaitTermination(Long.MAX_VALUE, TimeUnit.NANOSECONDS);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }



}
