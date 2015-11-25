import com.google.common.base.Strings;

import java.text.ParseException;
import java.util.Calendar;

/**
 * Created by han on 11/25/15.
 */
public class Device {
    private String timestamp;
    private String deviceid;
    private String deviceos;
    private String timezone;
    private String favtopic;
    private String pushinterval;
    private String syslang;
    private long timestamplong;
    private int pushintervalint;

    public long getTimestamp() {
        return timestamplong;
    }

    public String getDeviceID() {
        return deviceid;
    }

    public int getPushInterval() {
        return pushintervalint;
    }


    public boolean isAppleDevice() {
        return deviceid.length() == 64;
    }

    public boolean isTimeToPush() {
        Calendar rightNow = Calendar.getInstance();
        int hour = rightNow.get(Calendar.HOUR_OF_DAY);
        return hour % pushintervalint == 0;
    }

    public void reformat() {
        try {
            timestamplong = GlobalConfiguration.convertStr2Long(timestamp);
            if (Strings.isNullOrEmpty(pushinterval)) {
                pushintervalint = 1;
            } else {
                pushintervalint = Integer.parseInt(pushinterval);
            }
        } catch (ParseException ex) {
            ex.printStackTrace();
        }
    }


    @Override
    public String toString() {
        return String.format("%s\t%s\t%s\t%s\t%s",
                deviceid.substring(0, Math.min(deviceid.length(), 5)),
                timezone,
                favtopic,
                pushinterval,
                syslang
                );
    }


}
