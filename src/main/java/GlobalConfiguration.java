import java.util.concurrent.ForkJoinPool;

/**
 * Created by han on 11/17/15.
 */
public class GlobalConfiguration {
    public static ForkJoinPool forkJoinPool = new ForkJoinPool(20);
}
