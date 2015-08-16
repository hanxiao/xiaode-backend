import org.junit.Test;

/**
 * Created by han on 8/16/15.
 */
public class test {
    @Test
    public void run() {
        AllDatabase allDatabase = new AllDatabase();
        allDatabase.addKeyword("德国", "han");
        allDatabase.addKeyword("美国", "han");
        allDatabase.addKeyword("德国", "xiao");
        allDatabase.addKeyword("美国", "xiao");
        allDatabase.updateAll();


        int a = 1;

    }
}
