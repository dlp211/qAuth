package qauth.djd.qauthclient;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.google.android.gms.gcm.GoogleCloudMessaging;

import org.json.JSONObject;

import qauth.djd.qauthclient.POST.LoginTwoFactor;

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


                //Log.i("GcmIntentService", "Completed work @ " + SystemClock.elapsedRealtime());
                // Post notification of received message.
                //sendNotification("Received: " + extras.toString());
                Log.i("GcmIntentService", "Received: " + extras.toString());

                String messageID = extras.getString("messageID");

                if (messageID.equals("0")) { //authRequest

                    String authRequest = extras.getString("authRequest");

                    Log.i("authRequest", authRequest);
                    //authRequest: { "bluetoothId", "package", "deviceid", "nonce", "nonceEnc", "hash" }

                    String nonceEnc = "";
                    String nonce = "";
                    String packageName = "";
                    String deviceId = "";
                    String hash = "";
                    int auth = 1;

                    try {
                        JSONObject json = new JSONObject(authRequest);
                        nonceEnc = json.getString("nonceEnc");
                        nonce = json.getString("nonce");
                        packageName = json.getString("package");
                        deviceId = json.getString("deviceid");
                        hash = json.getString("hash");
                    } catch (Exception e){}

                    try {
                        String nonceEnc2 = MainActivity.auth.decrypt(nonceEnc);
                        String nonceEnc3 = String.valueOf( Long.valueOf(nonceEnc2) + 1);

                        Log.i("authRequest", "decrypt(nonceEnc): " + nonceEnc2 );
                        Log.i("authRequest", "decrypt(nonceEnc) + 1: " + nonceEnc3 );

                        Log.i("authRequest", "packageName: " + packageName );
                        Log.i("authRequest", "deviceId: " + deviceId );
                        Log.i("authRequest", "nonceEnc: " + nonceEnc );
                        Log.i("authRequest", "hash: " + hash );

                        //turn nonceEnc2 in to Long
                        //nonceEnc2 + 1
                        //encrypt and send to clientAuthenticate

                        //if ( nonceEnc2.equals(nonce) ) {
                            //TODO: fix verifySignature function
                            //if ( Authenticate.verifySignature( hash, Authenticate.hash(packageName + deviceId + nonceEnc) ) ){
                                // return json: { nonce, encrypt(nonce), auth, hash(auth+nonce+nonceEnc) }
                                //TODO: reimplement: new ClientAuthenticate( nonce, MainActivity.auth.encrypt(nonceEnc3), auth,  MainActivity.auth.hashAndSign(auth + nonce + nonceEnc)).execute();
                            //} else {
                                //Log.i("authRequest", "verifySignature FALSE");
                            //}
                        //} else {
                        //    Log.i("authRequest", "decrypt(nonceEnc) != nonce");
                        //}
                    } catch (Exception e) {
                        Log.i("GcmIntentService", e.toString());
                    }
                    //call decode on nonceEnc
                    // -> compare to nonce
                    // if ==, cont.
                    //
                    //  call verifySig( hash(package+deviceId+nonceEnc), hash )
                    // -> if true



                } else if (messageID.equals("1")) { //tokenResult & callback

                    String token1 = "";
                    String token2 = "";

                    try {
                        JSONObject json = new JSONObject(extras.getString("tokenResult"));
                        String token1Enc = json.getString("token1");
                        String token2Enc = json.getString("token2");

                        token1 = MainActivity.auth.decrypt(token1Enc);
                        token2 = MainActivity.auth.decrypt(token2Enc);

                        Log.i("tokenResult", "token1: " + token1);
                        Log.i("tokenResult", "token2: " + token2);

                    } catch (Exception e){ Log.i("tokenResult&callback", e.toString());}

                    SharedPreferences prefs = getSharedPreferences("qauth.djd.qauthclient",Context.MODE_PRIVATE);
                    prefs.edit().putString("QStoken", token2).commit();

                    new LoginTwoFactor(token1, "1", this).execute();

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