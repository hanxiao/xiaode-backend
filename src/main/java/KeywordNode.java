import java.util.HashSet;

/**
 * Created by han on 9/25/15.
 */

public class KeywordNode {
    public String name;
    public String query;
    public HashSet<KeywordNode> children = new HashSet<KeywordNode>();

    public KeywordNode(String name, String query) {
        this.name = name;
        this.query = query;
    }

    public KeywordNode(String name) {
        this.name = name;
        this.query = name;
    }


    @Override
    public int hashCode() {
        return this.query.hashCode();
    }

    public void addChild(KeywordNode child) {
        if (!children.contains(child)) {
            child.query += query;
            children.add(child);
        }
    }

}
