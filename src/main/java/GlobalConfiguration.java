import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Set;
import java.util.concurrent.ForkJoinPool;

/**
 * Created by han on 11/17/15.
 */
public class GlobalConfiguration {
    public static ForkJoinPool forkJoinPool = new ForkJoinPool(20);
    public static File visitedFile = new File("visited-stories.json");
    public static Set<String> visitedStories =
            JsonIO.loadVisitedStories(visitedFile);

    public static int numThread = 20;

    public static SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss");

    public static long convertStr2Long(String timestamp) throws ParseException {
        return dateFormat.parse(timestamp).getTime();
    }

    public static String[] junkImageUrl = new String[] {
            "default",
            "rcom-default",
            "udn_baby",
            "logo",
            "l2009",
            "2015-06-18",
            "MDJ-FB",
            "moneyudn",
            "c372022486173e3",
            "Rq5b-fxnqrny6785575",
            "1f3a5f4fabd8dac"
    };

    public static boolean isValidImageUrl(String url) {
        String url1 = url.trim().toLowerCase();
        for (String s : junkImageUrl) {
            if (url1.contains(s)) return false;
        }
        return true;
    }

}
