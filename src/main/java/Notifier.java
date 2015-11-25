import com.google.gson.*;
import com.notnoop.apns.APNS;
import com.notnoop.apns.ApnsService;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.CollectionAdapter;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by hxiao on 15/11/4.
 */
public class Notifier {
    private static transient final Logger LOG = LoggerFactory.getLogger(Notifier.class);

    private static String deviceServerGoogle = "https://spreadsheets.google.com/feeds/list/1Qe_3I7ijdDPp5dFU6ho9eD-5w0gWkVla4nxlGhUGL-I/1/public/basic?alt=json";
    private static String deviceServerNodeJS = "http://52.192.121.54:8080/getallusers";
    private static Gson gson = new GsonBuilder()
            .registerTypeHierarchyAdapter(Collection.class, new CollectionAdapter()).create();


    public static void pushStories2Device(List<StoryItem> storyItem, int numUpdate) {
        LOG.info("Start pushing...");
        String text = storyItem.stream().map(p -> String.format("[%s] %s", p.keyword, p.title))
                .collect(Collectors.joining("; "));
        String title = String.format("欧金所 - %d条新闻", numUpdate);
        List<Device> deviceIdList = getDeviceList();
        sendNotification(deviceIdList, title, text, numUpdate);
        storyItem.stream().forEach(StoryItem::setPushed);
    }

    private static void sendNotification(List<Device> deviceIdList,
                                         String title,
                                         String text, int numUpdate) {

        String iosPayload = APNS.newPayload()
                .badge(numUpdate)
                .alertBody(text)
                .alertTitle(title)
                .shrinkBody("查看详情")
                .build();

        ApnsService service =
                APNS.newService()
                        .withCert("certificate/ojins_prod_ck.p12", "xh0531")
                        .withProductionDestination()
                        .build();

            deviceIdList.stream()
                    .distinct()
                    .filter(Device::isAppleDevice)
                    .filter(Device::isTimeToPush)
                    .forEach(p-> {
                        LOG.info(p.toString());
                        service.push(p.getDeviceID(), iosPayload);
                    });

//            for (int j = 0; j < jsonArray.size(); j++) {
//                String deviceId = jsonArray.get(j).getAsJsonObject()
//                        .get("deviceid").getAsString();
//                LOG.info("push {}", deviceId);
//                try {
//                    if (isIOSDevice(deviceId)) {
//                        service.push(deviceId, iosPayload);
//                    } else if (isAndroidDevice(deviceId)) {
//                        GPNNotification gpnNotification = new GPNNotification(
//                                deviceId, title, text);
//                        StringEntity postingString = new StringEntity(gson.toJson(gpnNotification));//convert your pojo to   json
//                        postGoogle.setEntity(postingString);
//                        postGoogle.setHeader("Content-type", "application/json");
//                        postGoogle.setHeader("Authorization", "key=AIzaSyB8lPfKHZZto9EzMNWROWCbbVrt7v-HMC0");
//                        HttpResponse responseFromGoogle = httpClient.execute(postGoogle);
//                    } else {
//                        LOG.warn("{} device id is not recognized!");
//                    }
//                } catch (Exception ex) {
//                    LOG.warn("Unable push to {}", deviceId);
//                }
//
//            }
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
            for (String rowCol : rowCols) {
                String[] keyVal = rowCol.split(":");
                rowObj.addProperty(keyVal[0].trim(), keyVal[1].trim());
            }
            jsonArray.add(rowObj);
        }

        return jsonArray;
    }
    public static List<Device> getDeviceList() {
        HttpPost postGoogle = new HttpPost("https://gcm-http.googleapis.com/gcm/send");// put in your url
        HttpGet postDeviceId = new HttpGet(deviceServerGoogle);
        HttpGet postDeviceId2 = new HttpGet(deviceServerNodeJS);
        HttpClient httpClient  = new DefaultHttpClient();
        List<Device> deviceIdList = new ArrayList<>();

        try {
            HttpResponse response = httpClient.execute(postDeviceId);
            String json = EntityUtils.toString(response.getEntity());
            JsonArray jsonArray = parseGoogleJson(json);
            Device[] devicesGoogle = gson.fromJson(jsonArray, Device[].class);
            deviceIdList.addAll(Arrays.stream(devicesGoogle)
                    .filter(Device::isAppleDevice)
                    .collect(Collectors.toList())
            );
            deviceIdList.stream().forEach(Device::reformat);
            JsonIO.writeDeviceIDs(deviceIdList, new File("device-list.json"));
            response = httpClient.execute(postDeviceId2);
            json = EntityUtils.toString(response.getEntity());
            JsonParser jsonParser = new JsonParser();
            JsonElement jsonElement = jsonParser.parse(json);
            Device[] devicesAWS = gson.fromJson(jsonElement, Device[].class);



            deviceIdList.addAll(Arrays.stream(devicesAWS).collect(Collectors.toList()));
        } catch (IOException ex) {
            ex.printStackTrace();
            LOG.error("Unable to fetch all users");
        }
        deviceIdList.stream().forEach(Device::reformat);

        return deviceIdList;
    }

    public static void main(final String[] args) throws IOException {
        List<String> ss = new ArrayList<>();
        ss.add("ca959d3c188b732a1b2e444eae1f6c0d81f079d5277ab2c62b5ec07680e69f27");
        sendNotification(getDeviceList(),
                "测试", "测试两下，看到麻烦微信我", 5);
    }

}
