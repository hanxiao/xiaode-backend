import com.google.common.base.Strings;

import java.text.ParseException;
import java.util.Calendar;

/**
 * Created by han on 11/25/15.
 */
public class Device {
    private String timestamp;
    private String deviceid;
    private String deviceos = "ios";
    private String timezone = "+0";
    private String favtopic = "all";
    private String pushinterval = "3";
    private String syslang = "zh-cn";
    private long timestamplong = 0;
    private int pushintervalint = 3;

    public int getTimezone() {
        return timezoneint;
    }

    private int timezoneint = 0;

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
        return deviceos.equals("ios") && deviceid.length() == 64;
    }

    public boolean isAndroidDevice() {
        return deviceos.equals("android") && deviceid.length() == 152;
    }

    public boolean isTimeToPush() {
        Calendar rightNow = Calendar.getInstance();
        int hour = rightNow.get(Calendar.HOUR_OF_DAY);
        return hour % pushintervalint == 0;
    }

    public void reformat() {


        if (!Strings.isNullOrEmpty(pushinterval)
                && !pushinterval.trim().equals("undefined")) {
            pushintervalint = Integer.parseInt(pushinterval);
        }
        if (!Strings.isNullOrEmpty(timezone)
                && !timezone.trim().equals("undefined")) {
            if (timezone.contains(":")) {
                timezone = timezone.split(":")[0];
            }
            timezoneint = Integer.parseInt(timezone);
        }
        if (favtopic.equals("undefined")) {
            favtopic = "all";
        }

        if (Strings.isNullOrEmpty(syslang) || syslang.trim().equals("undefined")) {
            syslang = "zh-cn";
        }

        try {
            timestamplong = Long.parseLong(timestamp);
        } catch (NumberFormatException ex) {
            try {
                timestamplong = GlobalConfiguration.convertStr2Long(timestamp);
            } catch (ParseException ex2) {
                timestamplong = 0;
                ex2.printStackTrace();
            }
        }
    }


    @Override
    public String toString() {
        return String.format("%s\t%s\t%s\t%s\t%s",
                deviceid.substring(0, Math.min(deviceid.length(), 5)),
                getTimezone(),
                favtopic,
                getPushInterval(),
                syslang
        );
    }


}
