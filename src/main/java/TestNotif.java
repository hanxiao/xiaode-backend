import com.google.gson.*;
import com.notnoop.apns.APNS;
import com.notnoop.apns.ApnsService;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.CollectionAdapter;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Created by hxiao on 15/11/4.
 */
public class TestNotif {
    private static transient final Logger LOG = LoggerFactory.getLogger(TestNotif.class);

    private static void sendNotification(String deviceServer, String title,
                                         String text, int numUpdate) {

        Gson gson = new GsonBuilder()
                .registerTypeHierarchyAdapter(Collection.class, new CollectionAdapter()).create();

        String iosPayload = APNS.newPayload()
                .badge(numUpdate)
                .alertBody(text)
                .alertTitle(title)
                .shrinkBody("查看详情")
                .build();

        ApnsService service =
                APNS.newService()
                        .withCert("ojins-cert-dev.p12", "xh0531")
                        .withSandboxDestination()
                        .build();

        HttpPost postGoogle = new HttpPost("https://gcm-http.googleapis.com/gcm/send");// put in your url
        HttpPost postDeviceId = new HttpPost(deviceServer);
        HttpClient httpClient  = new DefaultHttpClient();
        try {
            HttpResponse response = httpClient.execute(postDeviceId);
            String json = EntityUtils.toString(response.getEntity());
            JsonParser jsonParser = new JsonParser();
            JsonElement jsonElement = jsonParser.parse(json);
            JsonArray jsonArray = jsonElement.getAsJsonArray();
            for (int j = 0; j < jsonArray.size(); j++) {
                String deviceId = jsonArray.get(j).getAsString().replace("\"","");
                LOG.info("push {}", deviceId);
                try {

                    if (isIOSDevice(deviceId)) {
                        service.push(deviceId, iosPayload);
                    } else if (isAndroidDevice(deviceId)) {
                        GPNNotification gpnNotification = new GPNNotification(
                                deviceId, title, text);
                        StringEntity postingString = new StringEntity(gson.toJson(gpnNotification));//convert your pojo to   json
                        postGoogle.setEntity(postingString);
                        postGoogle.setHeader("Content-type", "application/json");
                        postGoogle.setHeader("Authorization", "key=AIzaSyB8lPfKHZZto9EzMNWROWCbbVrt7v-HMC0");
                        HttpResponse responseFromGoogle = httpClient.execute(postGoogle);
                    } else {
                        LOG.warn("{} device id is not recognized!");
                    }
                } catch (Exception ex) {
                    LOG.warn("Unable push to {}", deviceId);
                }

            }
        } catch (IOException ex) {
            ex.printStackTrace();
            LOG.error("Unable to fetch all users");
        }
    }

    public static boolean isIOSDevice(String deviceId) {
        return true;
    }

    public static boolean isAndroidDevice(String deviceId) {
        return true;
    }

    public static void main(final String[] args) throws IOException {
        sendNotification("http://localhost:8080/getallusers",
                "测试", "测试一下", 5);
    }

}
