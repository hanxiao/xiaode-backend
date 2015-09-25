import java.io.Serializable;
import java.util.HashSet;

/**
 * Created by hxiao on 15/8/11.
 */
public class User implements Serializable {
    public String name;
    public long createTime;
    public long updateTime;
    public HashSet<String> keywords;


    public User(String name) {
        this.name = name;
        this.createTime = System.currentTimeMillis();
        this.keywords = new HashSet<String>();
    }



}
