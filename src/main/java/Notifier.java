import com.google.android.gcm.server.Message;
import com.google.android.gcm.server.Result;
import com.google.android.gcm.server.Sender;
import com.google.gson.*;
import com.notnoop.apns.APNS;
import com.notnoop.apns.ApnsService;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import utils.CollectionAdapter;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by hxiao on 15/11/4.
 */
public class Notifier {
    private static transient final Logger LOG = LoggerFactory.getLogger(Notifier.class);

    private static String deviceServerGoogle = "https://spreadsheets.google.com/feeds/list/1Qe_3I7ijdDPp5dFU6ho9eD-5w0gWkVla4nxlGhUGL-I/1/public/basic?alt=json";
    private static String deviceServerNodeJS = "http://news-api.ojins.com:8080/getallusers";
    private static Gson gson = new GsonBuilder()
            .registerTypeHierarchyAdapter(Collection.class, new CollectionAdapter()).create();


    public static void pushStories2Device(List<StoryItem> storyItem, int numUpdate) {
        LOG.info("Start pushing...");
        String text = storyItem.stream().map(p -> String.format("[%s] %s", p.keyword, p.title))
                .collect(Collectors.joining("; "));
        String title = String.format("欧金所 - %d条新闻", numUpdate);
        List<Device> deviceIdList = getDeviceList();
        sendNotiIOS(deviceIdList, title, text, numUpdate);
        sendNotiAndroid(deviceIdList, title, text, numUpdate);
        storyItem.stream().forEach(StoryItem::setPushed);
    }


    private static void sendNotiAndroid(List<Device> deviceIdList,
                                        String title,
                                        String text, int numUpdate) {
        Message message =  new Message.Builder()
                .addData("message", text)
                .addData("title", title)
                .build();

        Sender sender = new Sender("AIzaSyB8lPfKHZZto9EzMNWROWCbbVrt7v-HMC0");


        deviceIdList.stream()
                .distinct()
                .filter(Device::isAndroidDevice)
                .filter(Device::isTimeToPush)
                .forEach(p -> {
                    try {
                        Result result = sender.send(message, p.getDeviceID(), 3);
                        LOG.info(p.toString());
                    } catch (Exception error) {
                        LOG.error("Unable to push to Android devices");
                        error.printStackTrace();
                    }
                });
    }


    private static void sendNotiIOS(List<Device> deviceIdList,
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
                    service.push(p.getDeviceID(), iosPayload);
                    LOG.info(p.toString());
                });


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
                String[] keyVal = rowCol.split(": ");
                rowObj.addProperty(keyVal[0].trim(), keyVal[1].trim());
            }
            jsonArray.add(rowObj);
        }

        return jsonArray;
    }


    public static List<Device> getDeviceList() {
        HttpGet postDeviceId = new HttpGet(deviceServerGoogle);
        HttpGet postDeviceId2 = new HttpGet(deviceServerNodeJS);
        HttpClient httpClient  = new DefaultHttpClient();
        List<Device> deviceIdList = new ArrayList<>();

        try {
            //From google Sheet
            HttpResponse response = httpClient.execute(postDeviceId);
            String json = EntityUtils.toString(response.getEntity());
            JsonArray jsonArray = parseGoogleJson(json);
            Device[] devicesGoogle = gson.fromJson(jsonArray, Device[].class);
            deviceIdList.addAll(Arrays.stream(devicesGoogle)
                    .collect(Collectors.toList())
            );

            //From nodejs Server
            response = httpClient.execute(postDeviceId2);
            json = EntityUtils.toString(response.getEntity())
                    .replace("favTopic", "favtopic")
                    .replace("deviceId", "deviceid")
                    .replace("deviceOS", "deviceos")
                    .replace("sysLang", "syslang")
                    .replace("pushInterval", "pushinterval");
            JsonParser jsonParser = new JsonParser();
            JsonElement jsonElement = jsonParser.parse(json);
            Device[] devicesAWS = gson.fromJson(jsonElement, Device[].class);

            deviceIdList.addAll(Arrays.stream(devicesAWS)
                    .collect(Collectors.toList()));
        } catch (IOException ex) {
            ex.printStackTrace();
            LOG.error("Unable to fetch all users");
        } finally {
            deviceIdList.stream().forEach(Device::reformat);
            JsonIO.writeDeviceIDs(deviceIdList, new File("device-list.json"));
        }
        HashMap<String, Device> uniqueDevices = new HashMap<>();
        deviceIdList.stream().forEach(p -> {
            if (uniqueDevices.containsKey(p.getDeviceID())) {
                if (p.getTimestamp()
                        > uniqueDevices.get(p.getDeviceID()).getTimestamp()) {
                    uniqueDevices.replace(p.getDeviceID(), p);
                }
            } else {
                uniqueDevices.put(p.getDeviceID(), p);
            }
        });

        deviceIdList = uniqueDevices.values().stream()
                .collect(Collectors.toList());
        return deviceIdList;
    }

    public static void main(final String[] args) throws IOException {
        List<Device> devices = getDeviceList();
//        sendNotiIOS(devices.stream()
//                        .filter(Device::isAppleDevice)
//                        .collect(Collectors.toList()),
//                "测试", "测试两下，看到麻烦微信我", 5);
        sendNotiAndroid(devices.stream()
                        .filter(Device::isAndroidDevice)
                        .collect(Collectors.toList()),
                "测试", "测试两下，看到麻烦微信我", 5);
    }

}
