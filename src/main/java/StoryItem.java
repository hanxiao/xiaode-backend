import com.rometools.rome.feed.synd.SyndEntry;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.ChineseTrans;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.HashSet;

/**
 * Created by hxiao on 15/8/11.
 */
public class StoryItem implements Serializable {

    private static transient final Logger LOG = LoggerFactory.getLogger(StoryItem.class);

    public int id;
    public String keyword;
    public String title;
    public String summary;
    public HashSet<String> images;
    public long publishTime;
    public String publishDate;
    public HashSet<ArticleItem> sourceArticles;
    public String author;
    public int numViews;
    public transient ChineseTrans chineseTrans;


    public StoryItem(String keyword, SyndEntry sf) {
        DateFormat df = new SimpleDateFormat("yyyyMMdd");

        this.images = new HashSet<String>();
        this.chineseTrans = new ChineseTrans();
        this.keyword =  keyword;
        this.publishTime = sf.getPublishedDate().getTime();
        this.publishDate = df.format(publishTime);
        this.author = sf.getAuthor();
        this.title = sf.getTitle();
        this.numViews = 0;
        this.id = this.hashCode();

        this.summary = cleanContent(sf.getDescription().getValue())
                .replace(this.title, "")
                .replace(cleanTitle(this.title), "")
                .replace(this.author, "")
                .trim();

        setSourceArticles(sf);

    }

    private void setSourceArticles(SyndEntry sf) {
        sourceArticles = new HashSet<ArticleItem>();
        String org_content = sf.getDescription().getValue();
        Document doc = Jsoup.parse(org_content);
        Elements links = doc.select("a[href]");

        for (Element link : links) {
            if (link.attr("abs:href").matches(".*url=.*")) {
                String this_link = link.attr("abs:href").replaceAll(".*?url=", "").trim();
                if (this_link.length() > 0) {
                    ArticleItem articleItem;
                    try {
                        articleItem = new ArticleItem(this_link);
                        if (articleItem.imageUrl != null && !articleItem.imageUrl.trim().isEmpty()) {
                            images.add(articleItem.imageUrl);
                        }
                    } catch (Exception ex) {
                        LOG.error("Smart extraction failed on {}", this_link);
                        LOG.info("Fallback to regex extractor!");
                        articleItem = new ArticleItem(
                                cleanContent(sf.getDescription().getValue())
                                        .replace(this.title, "")
                                        .replace(this.author, "")
                                        .trim(),
                                this_link);
                    }
                    sourceArticles.add(articleItem);
                    LOG.info("Extracted content from {} for keyword {}", this_link, keyword);
                }
            }
        }
    }

    private String cleanTitle(String org_title) {
        // do something cleaning work for the title
        String result;
        org_title = chineseTrans.normalizeCAP(chineseTrans.toSimp(org_title), true);

        author = org_title.substring(org_title.lastIndexOf('-') + 1, org_title.length()).trim();
        result = org_title
                .replaceAll("-\\s.*?$", "")
                .replaceAll("^.*[：:]", "")
                .replaceAll(".*[\\(【《].*?[】\\)》]", "")
                .replaceAll("\"", "").trim();
        //result = org_title;
        return result;
    }


    private String cleanContent(String org_content) {
        String result;

        Document doc = Jsoup.parse(org_content);
        Elements links = doc.select("a[href]");

        doc = Jsoup.parse(chineseTrans.normalizeCAP(chineseTrans.toSimp(org_content), true));
        result = doc.text()
                .replaceAll("\\.\\.\\..*$", "")
                .replaceAll("\"", "").trim();
        return result;
    }

    @Override
    public int hashCode() {
        return this.title.hashCode();
    }

}
