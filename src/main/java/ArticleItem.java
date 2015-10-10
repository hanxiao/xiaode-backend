import com.sree.textbytes.network.HtmlFetcher;
import com.sree.textbytes.readabilityBUNDLE.Article;
import com.sree.textbytes.readabilityBUNDLE.ContentExtractor;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.Scanner;

/**
 * Created by hxiao on 15/9/24.
 */
public class ArticleItem implements Serializable {
    private static transient final Logger LOG = LoggerFactory.getLogger(ArticleItem.class);

    String imageUrl;
    String mainContent;
    String sourceLink;
    double posFactor = 0;
    double negFactor = 0;

    public void sentimentAnalysis(List<String> posWords, List<String> negWords) {
        posFactor = 0;
        negFactor = 0;
        posWords.stream().filter(pos -> mainContent != null && mainContent.length() > 0).forEach(pos -> {
            posFactor += mainContent.contains(pos) ? 1 : 0;
        });
        negWords.stream().filter(neg -> mainContent != null && mainContent.length() > 0).forEach(neg -> {
            negFactor += mainContent.contains(neg) ? 1 : 0;
        });

    }

    public void normalizeScore(double posAvg, double negAvg, double posStd, double negStd) {
        posFactor = (posFactor - posAvg) / posStd;
        negFactor = (negFactor - negAvg) / negStd;
    }

    public ArticleItem(ArticleItem articleItem) {
        this.imageUrl = articleItem.imageUrl;
        this.mainContent = articleItem.mainContent;
        this.sourceLink = articleItem.sourceLink;
        this.posFactor = articleItem.posFactor;
        this.negFactor = articleItem.negFactor;
    }

    public ArticleItem copy(){
        return new ArticleItem(this);
    }

    public ArticleItem(String imageUrl, String mainContent, String sourceLink) {
        this.imageUrl = imageUrl;
        this.mainContent = mainContent;
        this.sourceLink = sourceLink;
    }

    public ArticleItem(String sourceLink) throws Exception {

        this.sourceLink = sourceLink;

        ContentExtractor ce = new ContentExtractor();
        HtmlFetcher htmlFetcher = new HtmlFetcher();
        String html = htmlFetcher.getHtml(sourceLink, 5000);

        Article article = ce.extractContent(html, "ReadabilitySnack");
        this.imageUrl = article.getTopImage().getImageSrc();
        if (this.imageUrl != null) {
            this.imageUrl =
                    this.imageUrl.startsWith("http") ? this.imageUrl : null;
        }
        this.mainContent = article.getCleanedArticleText();
    }

    @Override
    public int hashCode() {
        return this.sourceLink.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ArticleItem))
            return false;
        if (obj == this)
            return true;

        ArticleItem rhs = (ArticleItem) obj;
        return new EqualsBuilder().
                append(hashCode(), rhs.hashCode()).
                isEquals();
    }

}
