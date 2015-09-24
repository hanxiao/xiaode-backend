import com.rometools.rome.feed.synd.SyndEntry;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.ChineseTrans;

import java.util.HashMap;

/**
 * Created by hxiao on 15/8/11.
 */
public class PostItem {

    private static final Logger LOG = LoggerFactory.getLogger(PostItem.class);

    public int id;
    public String keyword;
    public String title;
    public long publishTime;
    public HashMap<String, ArticleItem> linkedArticles;
    public String author;
    public int numViews;
    public ChineseTrans chineseTrans = new ChineseTrans();

    public PostItem(String keyword, SyndEntry sf) {

        this.keyword =  keyword;
        this.publishTime = sf.getPublishedDate().getTime();
        this.author = sf.getAuthor();
        this.title = sf.getTitle();
        this.numViews = 0;
        this.id = this.hashCode();

        setLinkedArticles(sf);
    }

    private void setLinkedArticles(SyndEntry sf) {
        linkedArticles = new HashMap<String, ArticleItem>();
        String org_content = sf.getDescription().getValue();
        Document doc = Jsoup.parse(org_content);
        Elements links = doc.select("a[href]");

        for (Element link : links) {
            if (link.attr("abs:href").matches(".*url=.*")) {
                String this_link = link.attr("abs:href").replaceAll(".*?url=", "").trim();
                if (this_link.length() > 0 &&
                        !linkedArticles.containsKey(this_link)) {
                    ArticleItem articleItem;

                    try {
                        articleItem = new ArticleItem(this_link);
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
                    linkedArticles.put(this_link, articleItem);
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
