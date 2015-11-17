import com.sree.textbytes.network.HtmlFetcher;
import com.sree.textbytes.readabilityBUNDLE.Article;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.print.Doc;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;

/**
 * Created by han on 11/16/15.
 */
public class test {

    public static class pairObj {

        int a = 1;
        int b = 2;

        public void update() {
            a = 3;
        }
    }

    public static void main(final String[] args) {
        HashMap<Integer, pairObj> ss = new HashMap<>();
        ss.put(1, new pairObj());
        ss.put(2, new pairObj());
        ss.values().stream().forEach(pairObj::update);
        int a = 1;

        String sourceLink = "http://finance.ifeng.com/a/20151117/14073429_0.shtml";
        HtmlFetcher htmlFetcher = new HtmlFetcher();




        try {
            String ua = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_10_5) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/46.0.2490.86 Safari/537.36";

            Document doc = Jsoup.connect("http://finance.ifeng.com/a/20151117/14073429_0.shtml")
                    .ignoreContentType(true)
                    .userAgent("Mozilla/5.0 (Windows NT 6.1; Win64; x64; rv:25.0) Gecko/20100101 Firefox/25.0")
                    .referrer("http://www.google.com")
                    .timeout(12000)
                    .followRedirects(true)
                    .get();

            Elements img = doc.getElementsByTag("img");
            for (Element el : img) {
                String src = el.absUrl("src");
                System.out.println("Image Found!");
                System.out.println("src attribute is : "+src);
            }

            ArticleItem articleItem = new ArticleItem(sourceLink);

            String html = htmlFetcher.getHtml(sourceLink, 10000);
            ArticleItem aaa = new ArticleItem(sourceLink);
            int b =1;

            final URL url = new URL("http://i0.sinaimg.cn/cj/2015/1026/U12760P31DT20151026013739.jpgs");
            HttpURLConnection huc = (HttpURLConnection) url.openConnection();
            huc.setRequestMethod("HEAD");
            int responseCode = huc.getResponseCode();

            if (responseCode == 200) {
                System.out.println("GOOD");
            } else {
                System.out.println("BAD");
            }
            int asda=1;
        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }
}
