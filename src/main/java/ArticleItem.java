import com.sree.textbytes.network.HtmlFetcher;
import com.sree.textbytes.readabilityBUNDLE.Article;
import com.sree.textbytes.readabilityBUNDLE.ContentExtractor;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.image.RenderedImage;
import java.io.Serializable;
import java.net.URL;
import java.util.List;

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
    double neutralFactor = 0;
    double imgSize;
    double imgRatio;

    public void sentimentAnalysis(List<String> posWords, List<String> negWords) {
        posFactor = 0;
        negFactor = 0;
        posWords.stream().filter(pos -> mainContent != null && mainContent.length() > 0).forEach(pos -> {
            posFactor += mainContent.contains(pos) ? 1 : 0;
        });
        negWords.stream().filter(neg -> mainContent != null && mainContent.length() > 0).forEach(neg -> {
            negFactor += mainContent.contains(neg) ? 1 : 0;
        });
        neutralFactor = posFactor - negFactor;

    }

    public void normalizeScore(double posAvg, double negAvg, double neuAvg,
                               double posStd, double negStd, double neuStd) {
        posFactor = (posFactor - posAvg) / posStd;
        negFactor = (negFactor - negAvg) / negStd;
        neutralFactor = (neutralFactor - neuAvg) / neuStd;
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


    private String completeImageUrl(String imageUrlSuffix) {
        String urlSuffix = imageUrlSuffix.startsWith("/") ?
                imageUrlSuffix.substring(1, imageUrlSuffix.length()) :
                imageUrlSuffix;
        String [] info = sourceLink.split("://");
        String [] subInfo = info[1].split("/");
        String middleUrl = "";

        for (String aSubInfo : subInfo) {
            middleUrl += aSubInfo + "/";
            String candidateUrl = info[0] +"://" + middleUrl + urlSuffix;
            try {
                URL url = new URL(candidateUrl);
                java.awt.Image img = new ImageIcon(url).getImage();
                if (img != null) {
                    return candidateUrl;
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    public ArticleItem(String sourceLink) throws Exception {

        this.sourceLink = sourceLink;

        ContentExtractor ce = new ContentExtractor();
        HtmlFetcher htmlFetcher = new HtmlFetcher();
        String html = htmlFetcher.getHtml(sourceLink, 5000);

        Article article = ce.extractContent(html, "ReadabilitySnack");
        imageUrl = article.getTopImage().getImageSrc();
        if (imageUrl != null) {
            if (!imageUrl.startsWith("http") && !imageUrl.contains("logo")) {
                imageUrl = completeImageUrl(imageUrl);
            } else {
                imageUrl = (!imageUrl.contains("logo")
                        && imageUrl.trim().length() > 0) ?
                        this.imageUrl : null;
            }

        }
        if (this.imageUrl != null) {
            URL url = new URL(this.imageUrl);
            RenderedImage img = (RenderedImage) new ImageIcon(url).getImage();
            imgRatio = (double)img.getWidth()/ img.getHeight();
            if (imgRatio < 1.1 && imgRatio > 0.9) {
                LOG.warn("Image is probably a logo or QR code!");
                imageUrl = null;
            } else {
                imgSize = img.getWidth() *  img.getHeight();
            }
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
