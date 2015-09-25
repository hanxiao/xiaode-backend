import com.sree.textbytes.readabilityBUNDLE.Article;
import com.sree.textbytes.readabilityBUNDLE.ContentExtractor;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.net.URL;
import java.net.URLConnection;
import java.util.Scanner;

/**
 * Created by hxiao on 15/9/24.
 */
public class ArticleItem implements Serializable {
    private static transient final Logger LOG = LoggerFactory.getLogger(ArticleItem.class);

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

        String html = null;
        URLConnection connection = null;
        try {
            connection =  new URL(sourceLink).openConnection();
            Scanner scanner = new Scanner(connection.getInputStream(), "utf-8");
            scanner.useDelimiter("\\Z");
            html = scanner.next();
        } catch ( Exception ex ) {
            ex.printStackTrace();
        }
        String header = html.substring(0,1000).toLowerCase();
        if (header.contains("charset=gbk")
                || header.contains("charset=\"gbk\"")
                || header.contains("charset='gbk'")) {
            LOG.info("{} is in GBK", sourceLink);
            try {
                connection =  new URL(sourceLink).openConnection();
                Scanner scanner = new Scanner(connection.getInputStream(), "gbk");
                scanner.useDelimiter("\\Z");
                html = scanner.next();
            } catch ( Exception ex ) {
                ex.printStackTrace();
            }
        } else if (header.contains("charset=gb2312")
                || header.contains("charset=\"gb2312\"")
                || header.contains("charset='gb2312'")) {
            LOG.info("{} is in GB2312", sourceLink);
            try {
                connection =  new URL(sourceLink).openConnection();
                Scanner scanner = new Scanner(connection.getInputStream(), "gb2312");
                scanner.useDelimiter("\\Z");
                html = scanner.next();
            } catch ( Exception ex ) {
                ex.printStackTrace();
            }
        }


        Article article = ce.extractContent(html, "ReadabilitySnack");
        this.imageUrl = article.getTopImage().getImageSrc();
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
