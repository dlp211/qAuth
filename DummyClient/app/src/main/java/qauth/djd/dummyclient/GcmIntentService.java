package qauth.djd.dummyclient;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Vibrator;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Wearable;

import org.json.JSONObject;

/**
 * Created by David on 1/29/15.
 */
public class GcmIntentService extends IntentService {
    public static final int NOTIFICATION_ID = 1;
    private NotificationManager mNotificationManager;
    NotificationCompat.Builder builder;

    public GcmIntentService() {
        super("GcmIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Bundle extras = intent.getExtras();
        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(this);
        // The getMessageType() intent parameter must be the intent you received
        // in your BroadcastReceiver.
        String messageType = gcm.getMessageType(intent);

        if (!extras.isEmpty()) {  // has effect of unparcelling Bundle
            /*
             * Filter messages based on message type. Since it is likely that GCM
             * will be extended in the future with new message types, just ignore
             * any message types you're not interested in, or that you don't
             * recognize.
             */

            //Log.i("GcmIntentService", "Received: " + extras.toString());

            if (GoogleCloudMessaging.
                    MESSAGE_TYPE_SEND_ERROR.equals(messageType)) {
                sendNotification("Send error: " + extras.toString());
            } else if (GoogleCloudMessaging.
                    MESSAGE_TYPE_DELETED.equals(messageType)) {
                sendNotification("Deleted messages on server: " +
                        extras.toString());
                // If it's a regular GCM message, do some work.
            } else if (GoogleCloudMessaging.
                    MESSAGE_TYPE_MESSAGE.equals(messageType)) {

                Log.i("GcmIntentService", "Received: " + extras.toString());

                String messageID = extras.getString("messageID");

                if ( messageID.equals("0")) {

                    sendBroadcast(new Intent("remoteFinish"));

                } else if ( messageID.equals("1")) {

                    //sendBroadcast(new Intent("remoteLogin"));

                    String authRequest = extras.getString("data");

                    String balance="";
                    String level="";
                    String sessionid="";

                    try {
                        JSONObject json = new JSONObject(authRequest);
                        balance = json.getString("balance");
                        level = json.getString("level");
                        sessionid = json.getString("sessionid");
                    } catch (Exception e){}

                    Intent intent2 = new Intent(this, GpaActivity.class);
                    intent2.putExtra("balance", balance);
                    intent2.putExtra("level", level);
                    intent2.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                    SharedPreferences prefs = getSharedPreferences("qauth.djd.dummyclient",Context.MODE_PRIVATE);
                    prefs.edit().putString("sessionid", sessionid).commit();

                    startActivity(intent2);

                } else if (messageID.equals("99")) { //warning

                    Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
                    long[] vibrationPattern = {0, 500, 50, 300};
                    //-1 - don't repeat
                    final int indexInPatternToRepeat = -1;
                    vibrator.vibrate(vibrationPattern, indexInPatternToRepeat);

                    if (! MainActivity.getNodes().isEmpty() ){

                        Log.i("test", "we have a watch!!!");

                        int notificationId = 001;

                        // Create a WearableExtender to add functionality for wearables
                        NotificationCompat.WearableExtender wearableExtender =
                                new NotificationCompat.WearableExtender()
                                        .setHintHideIcon(false);

                        NotificationCompat.BigTextStyle bigStyle = new NotificationCompat.BigTextStyle();
                        bigStyle.bigText("description?");

                        // Create a NotificationCompat.Builder to build a standard notification
                        // then extend it with the WearableExtender
                        Notification notif = new NotificationCompat.Builder(this)
                                .setContentTitle("WARNING")
                                .setContentText("Login with 1FA")
                                .extend(wearableExtender)
                                .setStyle(bigStyle)
                                .build();

                        // Get an instance of the NotificationManager service
                        NotificationManagerCompat notificationManager =
                                NotificationManagerCompat.from(this);

                        notificationManager.notify(notificationId, notif);

                        for ( String s : MainActivity.getNodes() ){

                            Wearable.MessageApi.sendMessage(
                                    MainActivity.mGoogleApiClient, s, "WARNING", new byte[0]).setResultCallback(
                                    new ResultCallback<MessageApi.SendMessageResult>() {
                                        @Override
                                        public void onResult(MessageApi.SendMessageResult sendMessageResult) {
                                            if (!sendMessageResult.getStatus().isSuccess()) {
                                                Log.i("MessageApi", "Failed to send message with status code: "
                                                        + sendMessageResult.getStatus().getStatusCode());
                                            } else if ( sendMessageResult.getStatus().isSuccess() ){
                                                Log.i("MessageApi", "onResult successful!");
                                            }
                                        }
                                    }
                            );

                        }


                    }
                } else if (messageID.equals("1")) { //tokenResult & callback

                }

            }

        }
        // Release the wake lock provided by the WakefulBroadcastReceiver.
        GcmBroadcastReceiver.completeWakefulIntent(intent);
    }

    // Put the message into a notification and post it.
    // This is just one simple example of what you might choose to do with
    // a GCM message.
    private void sendNotification(String msg) {
        mNotificationManager = (NotificationManager)
                this.getSystemService(Context.NOTIFICATION_SERVICE);

        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class), 0);

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.drawable.ic_launcher)
                        .setContentTitle("GCM Notification")
                        .setStyle(new NotificationCompat.BigTextStyle()
                                .bigText(msg))
                        .setContentText(msg);

        mBuilder.setContentIntent(contentIntent);
        mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
    }
}