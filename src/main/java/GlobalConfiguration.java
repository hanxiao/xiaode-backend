import java.io.File;
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
}
