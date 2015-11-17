import org.apache.commons.lang.builder.EqualsBuilder;

import java.util.HashSet;

/**
 * Created by han on 9/25/15.
 */

public class KeywordNode {
    public String name;
    public String query;
    public HashSet<KeywordNode> children = new HashSet<>();

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
        return Math.abs(this.query.hashCode());
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof KeywordNode))
            return false;
        if (obj == this)
            return true;

        KeywordNode rhs = (KeywordNode) obj;
        return new EqualsBuilder().
                append(hashCode(), rhs.hashCode()).
                isEquals();
    }

    public void addChild(KeywordNode child) {
        if (!children.contains(child)) {
            child.query += query;
            children.add(child);
        }
    }

}
