import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;


/**
 * Created by hxiao on 15/8/11.
 */
public class NewsFetcher {
    private static final Logger LOG = LoggerFactory.getLogger(NewsFetcher.class);

    public String keyword;
    public long createTime;
    public long lastUseTime;
    public User creator;
    public List<User> followers;
    public LinkedHashSet<NewsItem> allItems;

    private static String urlPattern = "https://news.google.com/news/section?cf=all&ned=us&hl=en&q=#QUERY&output=rss";
    private URL feedUrl;

    public NewsFetcher(String keyword, User creator) {
        this.creator = creator;
        this.lastUseTime = System.currentTimeMillis();
        this.createTime = System.currentTimeMillis();
        this.keyword = keyword;
        try {
            this.feedUrl = new URL(urlPattern.replace("#QUERY", URLEncoder.encode(keyword, "UTF-8")));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        this.followers = new ArrayList<User>();
        this.followers.add(creator);
        this.allItems = new LinkedHashSet<NewsItem>();
    }

    public void update() {
        this.lastUseTime = System.currentTimeMillis();
        try {
            SyndFeedInput input = new SyndFeedInput();
            SyndFeed feed = input.build(new XmlReader(feedUrl));
            List<SyndEntry> allFeeds = feed.getEntries();
            for (SyndEntry sf : allFeeds) {
                NewsItem newsItem = new NewsItem(sf);
                allItems.add(newsItem);
                LOG.info("added a post!");
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }







}
