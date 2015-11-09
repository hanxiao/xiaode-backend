/**
 * Created by hxiao on 15/11/3.
 */
public class GPNNotification {
    String to;
    Notification notification;

    public GPNNotification(String to, String body, String title) {
        this.to = to;
        this.notification =  new Notification();
        this.notification.body = body;
        this.notification.title = title;
    }

    class Notification {
        String body;
        String title;
    }


}
