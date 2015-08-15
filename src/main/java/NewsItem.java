import com.rometools.rome.feed.synd.SyndEntry;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

/**
 * Created by hxiao on 15/8/11.
 */
public class NewsItem {
    public int id;
    public String title;
    public String subtitle;
    public String text;
    public long publishTime;
    public String textUrl;
    public String imageUrl;
    public String author;
    public int numViews;



    public NewsItem (SyndEntry sf) {
        this.publishTime = sf.getPublishedDate().getTime();
        this.title = cleanTitle(sf.getTitle());
        this.author = sf.getAuthor();
        this.text = sf.getDescription().getValue().replace(this.title, "")
                .replace(this.author, "").trim();
        this.numViews = 0;
    }

    private String cleanTitle(String org_title) {
        // do something cleaning work for the title
        String result;
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
        result = doc.text()
                .replaceAll("\\.\\.\\..*$", "")
                .replaceAll("\"", "").trim();


        for (Element link : links) {
            if (link.attr("abs:href").matches(".*url=.*")) {
                String this_link = link.attr("abs:href").replaceAll(".*?url=", "");
                if (this_link.length() > 0) {
                    linkUrl += String.format("<p><a href=\"%s\" target=\"_blank\">[%s]</a></p>", this_link, link.text().trim());
                    curLink = this_link;
                    numLinks ++;
                }
            }
        }
        result = chineseTrans.normalizeCAP(result, true);
        return result;
    }

}
