import com.rometools.rome.feed.synd.SyndEntry;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import utils.ChineseTrans;

import java.util.HashMap;

/**
 * Created by hxiao on 15/8/11.
 */
public class PostItem {
    public int id;
    public String keyword;
    public String title;
    public String subtitle;
    public String text;
    public long publishTime;
    public HashMap<String, String> linkUrl;
    public String imageUrl;
    public String author;
    public int numViews;
    public ChineseTrans chineseTrans = new ChineseTrans();

    public PostItem(String keyword, SyndEntry sf) {
        this.keyword =  keyword;
        this.publishTime = sf.getPublishedDate().getTime();
        this.title = cleanTitle(sf.getTitle());
        this.author = sf.getAuthor();
        this.text = cleanContent(sf.getDescription().getValue()).replace(this.title, "")
                .replace(this.author, "").trim();
        this.numViews = 0;
        this.linkUrl =  new HashMap<String, String>();
        this.id = this.hashCode();
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
        org_content = chineseTrans.normalizeCAP(chineseTrans.toSimp(org_content), true);
        Document doc = Jsoup.parse(org_content);
        Elements links = doc.select("a[href]");
        result = doc.text()
                .replaceAll("\\.\\.\\..*$", "")
                .replaceAll("\"", "").trim();


        for (Element link : links) {
            if (link.attr("abs:href").matches(".*url=.*")) {
                String this_link = link.attr("abs:href").replaceAll(".*?url=", "");
                if (this_link.length() > 0) {
                    linkUrl.put(link.text().trim(), this_link);
                }
            }
        }
        return result;
    }

}
