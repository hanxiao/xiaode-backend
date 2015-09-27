import com.rometools.rome.feed.synd.SyndEntry;
import org.apache.commons.lang.builder.EqualsBuilder;
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
        this.author = setAuthor(sf.getTitle());
        this.title = cleanTitle(sf.getTitle());
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
                ArticleItem articleItem = new ArticleItem(null, null, this_link);
                if (this_link.length() > 0 && !sourceArticles.contains(articleItem)) {

                    try {
                        articleItem = new ArticleItem(this_link);
                        LOG.info("Extracted content from {} for feedName {}", this_link, keyword);
                    } catch (Exception ex) {
                        LOG.error("Smart extraction failed on {} Fallback to link", this_link);
                        articleItem = new ArticleItem(null, null, this_link);
                    }

                    if (articleItem.imageUrl != null
                            && !articleItem.imageUrl.trim().isEmpty()
                            && !articleItem.imageUrl.contains("logo")) {
                        images.add(articleItem.imageUrl);
                    }

                    if (articleItem.mainContent != null) {
                        articleItem.mainContent = chineseTrans.normalizeCAP(
                                chineseTrans.toSimp(articleItem.mainContent), false);
                    }

                    sourceArticles.add(articleItem);


                }
            }
        }
    }


    private String setAuthor(String org_title) {
        return org_title.substring(org_title.lastIndexOf('-') + 1, org_title.length()).trim();
    }

    private String cleanTitle(String org_title) {
        // do something cleaning work for the title
        String result;
        org_title = chineseTrans.normalizeCAP(chineseTrans.toSimp(org_title), true);

        result = org_title
                .replaceAll("-\\s.*?$", "")
                .replaceAll(".*[\\(【《].*?[】\\)》]", "")
                .trim();
//                .replaceAll("^.*[：:]", "")
//                .replaceAll(".*[\\(【《].*?[】\\)》]", "")
//                .replaceAll("\"", "").trim();
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
                .replaceAll("\"", "")
                .trim();
        return result;
    }

    @Override
    public int hashCode() {
        return this.title.hashCode();
    }


    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof StoryItem))
            return false;
        if (obj == this)
            return true;

        StoryItem rhs = (StoryItem) obj;
        return new EqualsBuilder().
                append(hashCode(), rhs.hashCode()).
                isEquals();
    }

}
