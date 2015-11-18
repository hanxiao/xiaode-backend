import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.utils.Strings;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.imgscalr.Scalr;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.ChineseTrans;
import utils.EditDistance;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.Serializable;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.HashSet;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Created by hxiao on 15/8/11.
 */
public class StoryItem implements Serializable {

    private static transient final Logger LOG = LoggerFactory.getLogger(StoryItem.class);

    public int getIdByGroup() {
        return Math.abs(id % 10);
    }

    public int id;

    public String keyword;
    public String title;
    public String summary;
    public HashSet<String> images;
    public String mainImage;



    public long publishTime;
    public long fetchTime;

    public String getPublishDate() {
        return publishDate;
    }

    public String publishDate;
    public HashSet<ArticleItem> sourceArticles;
    public String author;
    public int numViews;
    public transient ChineseTrans chineseTrans;

    public boolean hasPushed;


    public int getNumSource() {
        return sourceArticles.size();
    }

    public long getPublishTime() {
        return publishTime;
    }

    public String getKeyword() {
        return keyword;
    }

    public StoryItem copy() {
        return new StoryItem(this);
    }

    public StoryItem(StoryItem storyItem) {
        this.id = storyItem.id;
        this.keyword = storyItem.keyword;
        this.title = storyItem.title;
        this.summary = storyItem.summary;
        if (storyItem.images == null) {
            this.images = null;
        } else {
            this.images = new HashSet<String>();
            this.images.addAll(storyItem.images);
        }
        this.publishDate = storyItem.publishDate;
        this.publishTime = storyItem.publishTime;
        this.author = storyItem.author;
        this.numViews = storyItem.numViews;
        this.chineseTrans = storyItem.chineseTrans;
        this.fetchTime = storyItem.fetchTime;
        this.hasPushed = storyItem.hasPushed;
        this.sourceArticles = new HashSet<ArticleItem>();
        for (ArticleItem articleItem : storyItem.sourceArticles) {
            this.sourceArticles.add(articleItem.copy());
        }
        this.mainImage = storyItem.mainImage;
    }


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
        this.fetchTime = System.currentTimeMillis();
        this.hasPushed = false;

        this.summary = cleanContent(sf.getDescription().getValue())
                .replace(this.title, "")
                .replace(cleanTitle(this.title), "")
                .replace(this.author, "")
                .trim();

        setSourceArticles(sf);

    }

    private void setSourceArticles(SyndEntry sf) {
         new HashSet<ArticleItem>();
        String org_content = sf.getDescription().getValue();
        Document doc = Jsoup.parse(org_content);
        Elements links = doc.select("a[href]");

        double maxSize = 0;
        sourceArticles = links.stream()
                .filter(p -> p.attr("abs:href").matches(".*url=.*"))
                .map(p -> p.attr("abs:href").replaceAll(".*?url=", "").trim())
                .filter(p -> p.length() > 0 && !GlobalConfiguration.visitedStories.contains(p))
                .map(p -> {
                    try {
                        LOG.info("Extracting [{}] {}", keyword, p);
                        return new ArticleItem(p);
                    } catch (Exception ex) {
                        LOG.error("Failed on [{}] {}", keyword, p);
                        ex.printStackTrace();
                        return new ArticleItem(null, null, p);
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(HashSet::new));


        sourceArticles.forEach(articleItem -> {

            if (articleItem.imageUrl != null) {
                images.add(articleItem.imageUrl);
                if (articleItem.imgSize > maxSize) {
                    mainImage = articleItem.imageUrl;
                }
            }

            if (mainImage != null && !Strings.isEmpty(mainImage)) {
                try {
                    File thumbFile = new File(String.format("thumbnail/%d.jpg", id));
                    if (!thumbFile.exists()) {
                        URL url = new URL(mainImage);
                        BufferedImage image = (BufferedImage) new ImageIcon(url).getImage();
                        BufferedImage thumbnail =
                                Scalr.resize(image, Scalr.Method.SPEED, Scalr.Mode.FIT_TO_WIDTH,
                                        300, 300, Scalr.OP_ANTIALIAS);
                        ImageIO.write(thumbnail, "jpg", thumbFile);
                    }
                }  catch (Exception ignored) {
                }
            }

            if (articleItem.mainContent != null) {
                articleItem.mainContent = chineseTrans.normalizeCAP(
                        chineseTrans.toSimp(articleItem.mainContent), false);
            }

            sourceArticles.add(articleItem);
            GlobalConfiguration.visitedStories.add(articleItem.sourceLink);
        });
    }

    public void setPushed() {
        this.hasPushed = true;
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
        return Math.abs(this.title.hashCode());
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

    public static boolean isSimilar(StoryItem a, StoryItem b) {
        if (a.equals(b)) {
            return true;
        } else {
            if (EditDistance.sim(a.title, b.title) > 0.55) {
                LOG.info("{} and {} are very similar", a.title, b.title);
                return true;
            }
        }
        return false;
    }

    public void mergeWith(StoryItem storyItem) {
        this.title = storyItem.title.length() > this.title.length()
                ? storyItem.title : this.title;
        this.id = this.hashCode();
        this.summary = storyItem.summary.length() > this.summary.length()
                ? storyItem.summary : this.summary;
        if (storyItem.images != null) {
            if (this.images == null) {
                this.images = new HashSet<String>();
            }
            this.images.addAll(storyItem.images);
        }


        this.publishTime = storyItem.publishTime < this.publishTime
                ? storyItem.publishTime : this.publishTime;
        DateFormat df = new SimpleDateFormat("yyyyMMdd");
        this.publishDate = df.format(this.publishTime);
        this.author = storyItem.author.length() > this.author.length()
                ? storyItem.author : this.author;
        if (Objects.isNull(sourceArticles)) {
            this.sourceArticles =  new HashSet<>();
        }
        if (Objects.isNull(storyItem.sourceArticles)) {
            storyItem.sourceArticles =  new HashSet<>();
        }
        this.sourceArticles.addAll(storyItem.sourceArticles.stream()
                .filter(Objects::nonNull)
                .map(ArticleItem::copy)
                .collect(Collectors.toList()));

        if (Objects.isNull(this.mainImage) && Objects.nonNull(storyItem.mainImage)) {
            this.mainImage = storyItem.mainImage;
        }

        this.hasPushed = this.hasPushed || storyItem.hasPushed;

    }

}
