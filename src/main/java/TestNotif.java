import com.google.gson.*;
import com.notnoop.apns.APNS;
import com.notnoop.apns.ApnsService;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
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
import java.util.List;

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
        HttpGet postDeviceId = new HttpGet(deviceServer);
        HttpClient httpClient  = new DefaultHttpClient();
        try {
            HttpResponse response = httpClient.execute(postDeviceId);
            String json = EntityUtils.toString(response.getEntity());
            JsonArray jsonArray = parseGoogleJson(json);
            for (int j = 0; j < jsonArray.size(); j++) {
                String deviceId = jsonArray.get(j).getAsJsonObject()
                        .get("deviceid").getAsString();
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

    private static JsonArray parseGoogleJson(String json) {
        JsonArray jsonArray = new JsonArray();
        JsonParser jsonParser = new JsonParser();
        JsonElement jsonElement = jsonParser.parse(json);

        JsonArray array = ((JsonObject) jsonElement).get("feed").getAsJsonObject().get("entry").getAsJsonArray();

        for (int i = 0; i < array.size(); i++){
            JsonObject rowObj = new JsonObject();
            rowObj.addProperty("timestamp", array.get(i)
                    .getAsJsonObject()
                    .get("title")
                    .getAsJsonObject()
                    .get("$t").getAsString());
            String [] rowCols = array.get(i)
                    .getAsJsonObject()
                    .get("content")
                    .getAsJsonObject()
                    .get("$t").getAsString().split(",");
            for (int j = 0; j < rowCols.length; j++) {
                String [] keyVal = rowCols[j].split(":");
                rowObj.addProperty(keyVal[0].trim(), keyVal[1].trim());
            }
            jsonArray.add(rowObj);
        }

        return jsonArray;
    }

    public static boolean isIOSDevice(String deviceId) {
        return true;
    }

    public static boolean isAndroidDevice(String deviceId) {
        return true;
    }

    public static void main(final String[] args) throws IOException {
        sendNotification("https://spreadsheets.google.com/feeds/list/1Qe_3I7ijdDPp5dFU6ho9eD-5w0gWkVla4nxlGhUGL-I/1/public/basic?alt=json",
                "测试", "测试一下", 5);
    }

}
