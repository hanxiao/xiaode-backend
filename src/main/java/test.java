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
    }
}
