import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.CollectionAdapter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Scanner;

/**
 * Created by han on 9/25/15.
 */
public class JsonIO {
    private static transient final Logger LOG = LoggerFactory.getLogger(JsonIO.class);

    public static void database2Json(FeedDatabase feedDatabase, File outFile) {
        Gson gson = new GsonBuilder().setPrettyPrinting()
                .registerTypeHierarchyAdapter(Collection.class, new CollectionAdapter()).create();
        String jsonOutput = gson.toJson(feedDatabase);

        try {
            PrintWriter writer = new PrintWriter(new FileOutputStream(outFile, false));
            writer.println(jsonOutput);
            writer.flush();
            writer.close();
        } catch (IOException ex) {
            LOG.error("Could not save Database {}", outFile, ex);
        }
    }

    public static void keywordTree2Json(KeywordNode keywordNode, File outFile) {
        Gson gson = new GsonBuilder().setPrettyPrinting()
                .registerTypeHierarchyAdapter(Collection.class, new CollectionAdapter()).create();
        String jsonOutput = gson.toJson(keywordNode);

        try {
            PrintWriter writer = new PrintWriter(new FileOutputStream(outFile, false));
            writer.println(jsonOutput);
            writer.flush();
            writer.close();
        } catch (IOException ex) {
            LOG.error("Could not save Database {}", outFile, ex);
        }
    }

    public static KeywordNode json2KeywordTree(File inFile) {
        Gson gson = new GsonBuilder().setPrettyPrinting()
                .registerTypeHierarchyAdapter(Collection.class, new CollectionAdapter()).create();
        KeywordNode keywordNode = null;
        try {
            String content = new Scanner(inFile).useDelimiter("\\Z").next();
            keywordNode = gson.fromJson(content, KeywordNode.class);
        } catch (IOException e) {
            LOG.error("Error {} whe reading files", e);
        }
        return keywordNode;
    }

}
