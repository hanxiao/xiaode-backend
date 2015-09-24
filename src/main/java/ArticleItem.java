import com.sree.textbytes.network.HtmlFetcher;
import com.sree.textbytes.readabilityBUNDLE.Article;
import com.sree.textbytes.readabilityBUNDLE.ContentExtractor;

/**
 * Created by hxiao on 15/9/24.
 */
public class ArticleItem {

    String imageUrl;
    String mainContent;

    public ArticleItem(String mainContent, String sourceLink) {
        this.imageUrl = "";
        this.mainContent = mainContent;
        this.sourceLink = sourceLink;
    }

    String sourceLink;

    public ArticleItem(String sourceLink) throws Exception {
        this.sourceLink = sourceLink;

        ContentExtractor ce = new ContentExtractor();
        HtmlFetcher htmlFetcher = new HtmlFetcher();

        String html = htmlFetcher.getHtml(sourceLink, 0);
        Article article = ce.extractContent(html, "ReadabilitySnack");
        this.imageUrl = article.getTopImage().getImageSrc();
        this.mainContent = article.getCleanedArticleText();
    }




}
