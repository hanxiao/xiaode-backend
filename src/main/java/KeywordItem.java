import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.UpdateInterval;

import java.io.IOException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;


/**
 * Created by hxiao on 15/8/11.
 */
public class KeywordItem {
    private static final Logger LOG = LoggerFactory.getLogger(KeywordItem.class);

    public final String keyword;
    public final long createTime;
    public long lastUpdateTime;
    public final String creator;
    public final HashSet<String> followers;
    // this only saves id for items belong to this keywords
    public LinkedHashSet<PostItem> allItems;

    private URL feedUrl;

    public KeywordItem(String keyword, String creator) {
        this.creator = creator;
        this.lastUpdateTime = 0;
        this.createTime = System.currentTimeMillis();
        this.keyword = keyword;
        try {
            String urlPattern = "https://news.google.com/news/section?cf=all&ned=us&hl=en&q=#QUERY&output=rss";
            this.feedUrl = new URL(urlPattern.replace("#QUERY", URLEncoder.encode(keyword, "UTF-8")));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        this.followers = new HashSet<String>();
        this.followers.add(creator);
        this.allItems = new LinkedHashSet<PostItem>();

    }

    public void update() {
        long timeDiff = System.currentTimeMillis() - lastUpdateTime;
        if (lastUpdateTime == 0 || timeDiff > UpdateInterval.HOUR.getNumVal()) {
            try {
                SyndFeedInput input = new SyndFeedInput();
                SyndFeed feed = input.build(new XmlReader(feedUrl));
                List<SyndEntry> allFeeds = feed.getEntries();
                for (SyndEntry sf : allFeeds) {
                    PostItem postItem = new PostItem(keyword, sf);
                    if (postItem != null) {
                        allItems.add(postItem);
                    }
                    LOG.info("New post {} is added to {}!", postItem.title, keyword);
                }
            }
            catch (IOException exception) {
                LOG.error("Can not read from {}", feedUrl);
            }
            catch (FeedException exception) {
                LOG.error("Feed can not be built");
            }
            this.lastUpdateTime = System.currentTimeMillis();
        } else {
            LOG.info("This keyword has been updated {} mins ago.", timeDiff / UpdateInterval.HOUR.getNumVal());
        }


    }







}
