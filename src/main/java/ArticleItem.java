import com.sree.textbytes.network.HtmlFetcher;
import com.sree.textbytes.readabilityBUNDLE.Article;
import com.sree.textbytes.readabilityBUNDLE.ContentExtractor;
import utils.FixEncoder;

import java.io.Serializable;

/**
 * Created by hxiao on 15/9/24.
 */
public class ArticleItem implements Serializable {

    String imageUrl;
    String mainContent;
    String sourceLink;


    public ArticleItem(String mainContent, String sourceLink) {
        this.imageUrl = "";
        this.mainContent = mainContent;
        this.sourceLink = sourceLink;
    }

    public ArticleItem(String sourceLink) throws Exception {

        this.sourceLink = sourceLink;

        ContentExtractor ce = new ContentExtractor();
        HtmlFetcher htmlFetcher = new HtmlFetcher();

        String html = htmlFetcher.getHtml(sourceLink, 0);
        Article article = ce.extractContent(html, "ReadabilitySnack");
        this.imageUrl = FixEncoder.fixEncoding(article.getTopImage().getImageSrc());
        this.mainContent = FixEncoder.fixEncoding(article.getCleanedArticleText());
    }

    @Override
    public int hashCode() {
        return this.sourceLink.hashCode();
    }


}
