/**
 * Created by han on 8/16/15.
 */
public class test {
    public static void main(String[] args) throws Exception {
        AllDatabase allDatabase = new AllDatabase();
        allDatabase.addKeyword("德国", "han");
        allDatabase.addKeyword("美国", "han");
        allDatabase.addKeyword("德国", "xiao");
        allDatabase.addKeyword("美国", "xiao");
        allDatabase.updateAll();
        allDatabase.updateAll();
    }

}
