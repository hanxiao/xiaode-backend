import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.UpdateInterval;

import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.concurrent.*;


/**
 * Created by hxiao on 15/8/11.
 */
public class FeedItem implements Serializable {
    private static transient final Logger LOG = LoggerFactory.getLogger(FeedItem.class);

    public final String feedName;
    public final String query;
    public final long createTime;
    public long lastUpdateTime;
    public final String creator;
    public final HashSet<String> followers;
    // this only saves id for items belong to this keywords
    public HashSet<StoryItem> allStories;

    private URL feedUrl;

    public FeedItem(KeywordNode keywordNode, String creator) {
        this.creator = creator;
        this.lastUpdateTime = 0;
        this.createTime = System.currentTimeMillis();
        this.feedName = keywordNode.name;
        this.query = keywordNode.query;
        try {
            String urlPattern = "https://news.google.com/news/section?cf=all&ned=us&hl=en&q=#QUERY&output=rss";
            this.feedUrl = new URL(urlPattern.replace("#QUERY", URLEncoder.encode(this.query, "UTF-8")));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        this.followers = new HashSet<String>();
        this.followers.add(creator);
        this.allStories = new LinkedHashSet<StoryItem>();

    }

    public void update() {
        long timeDiff = System.currentTimeMillis() - lastUpdateTime;
        if (lastUpdateTime == 0 || timeDiff > (UpdateInterval.HOUR.getNumVal() / 2)) {
            try {
                ExecutorService executorService = Executors.newFixedThreadPool(GlobalConfiguration.numThread);

                SyndFeedInput input = new SyndFeedInput();
                SyndFeed feed = input.build(new XmlReader(feedUrl));

                feed.getEntries().stream().map(p -> {

                    Future<StoryItem> task = executorService.submit(() ->
                            new StoryItem(feedName, p));

                    try {
                        return task.get(1, TimeUnit.MINUTES);
                    } catch (InterruptedException | ExecutionException ex) {
                        LOG.error("Thread pool is error");
                        return null;
                    } catch (TimeoutException e) {
                        LOG.error("Thread timeout when {}->{}", feedName, p);
                        return null;
                    }
                })
                        .filter(p-> p!=null)
                        .forEach(p -> {
                            allStories.add(p);
                            LOG.info("New post {} is added to {}!", p.title, feedName);
                        });


//                GlobalConfiguration.forkJoinPool.submit(() ->
//                        feed.getEntries().parallelStream().map(p ->
//                                new StoryItem(feedName, p))
//                                .filter(p-> p!=null)
//                                .forEach(p -> {
//                                    allStories.add(p);
//                                    LOG.info("New post {} is added to {}!", p.title, feedName);
//                                })).get(10, TimeUnit.SECONDS);
            }
            catch (IOException | FeedException exception) {
                LOG.error("Can not read from {}", feedUrl);
            }
//            catch (InterruptedException | ExecutionException ex) {
//                LOG.error("Thread pool is error");
//            } catch (TimeoutException e) {
//                LOG.error("Thread timeout when ");
////                e.printStackTrace();
//            }
            this.lastUpdateTime = System.currentTimeMillis();
        } else {
            LOG.info("Feed {} has been updated {} hour ago.",
                    feedName,
                    (double)timeDiff / UpdateInterval.HOUR.getNumVal());
        }


    }
}
