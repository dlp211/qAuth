package qauth.djd.qauthclient;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.Settings.Secure;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.KeyFactory;
import java.security.KeyStore;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class MainActivity extends Activity {

    GifView gifView;
    private GoogleApiClient client;
    static Context ctx;
    static Activity act;
    static TextView tv2;

    public static final String EXTRA_MESSAGE = "message";
    public static final String PROPERTY_REG_ID = "registration_id";
    private static final String PROPERTY_APP_VERSION = "appVersion";
    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;

    /**
     * Substitute you own sender ID here. This is the project number you got
     * from the API Console, as described in "Getting Started."
     */
    String SENDER_ID = "156110196668";

    /**
     * Tag used on log messages.
     */
    static final String TAG = "qAuthClient";

    TextView mDisplay;
    GoogleCloudMessaging gcm;
    AtomicInteger msgId = new AtomicInteger();
    SharedPreferences prefs;
    Context context;

    String regid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ctx = this;
        act = this;

        tv2 = (TextView) findViewById(R.id.textView2);
        mDisplay = tv2;

        GifView gif_view = (GifView) findViewById(R.id.testGifView );
        gif_view.setGifImageResourceID(R.raw.green);

        String deviceId = Secure.getString(this.getContentResolver(), Secure.ANDROID_ID);
        Log.i(TAG, "deviceId: " + deviceId);

        /*try {

            Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
            byte[] input = "abc".getBytes();
            Cipher cipher = Cipher.getInstance("RSA/None/OAEPWithSHA1AndMGF1Padding", "BC");
            SecureRandom random = new SecureRandom();
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA", "BC");

            generator.initialize(1024, random);

            KeyPair pair = generator.generateKeyPair();
            Key pubKey = pair.getPublic();

            Key privKey = pair.getPrivate();

            //Log.i(TAG, "pubKey: " + pubKey.toString() );
            //Log.i(TAG, "privKey: " + privKey.toString() );

            cipher.init(Cipher.ENCRYPT_MODE, pubKey, random);
            byte[] cipherText = cipher.doFinal(input);
            //Log.i(TAG, "cipher: " + new String(cipherText));

            cipher.init(Cipher.DECRYPT_MODE, privKey);
            byte[] plainText = cipher.doFinal(cipherText);
            //Log.i(TAG, "plain : " + new String(plainText));

        } catch (Exception e) {

        }*/

        /*
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            public void run() {
                tv2.setText("Sending Watch Id");

            }
        }, 2000);*/

        /*handler.postDelayed(new Runnable() {
            public void run() {
                tv2.setText("Retrieving Token");

            }
        }, 3500);*/

        Log.i(TAG, "logcat test");

        //new GetTokenTask("007").execute();

        /*
        handler.postDelayed(new Runnable() {
            public void run() {
                new GetTokenTask("007").execute();
            }
        }, 4000);*/

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    /**
     * Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     */
    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, this,
                        PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                Log.i(TAG, "This device is not supported.");
                finish();
            }
            return false;
        }
        return true;
    }

    /**
     * Gets the current registration ID for application on GCM service.
     * <p>
     * If result is empty, the app needs to register.
     *
     * @return registration ID, or empty string if there is no existing
     *         registration ID.
     */
    private String getRegistrationId(Context context) {
        final SharedPreferences prefs = getGCMPreferences(context);
        String registrationId = prefs.getString(PROPERTY_REG_ID, "");
        if (registrationId.isEmpty()) {
            Log.i(TAG, "Registration not found.");
            return "";
        }
        // Check if app was updated; if so, it must clear the registration ID
        // since the existing regID is not guaranteed to work with the new
        // app version.
        /*int registeredVersion = prefs.getInt(PROPERTY_APP_VERSION, Integer.MIN_VALUE);
        int currentVersion = getAppVersion(context);
        if (registeredVersion != currentVersion) {
            Log.i(TAG, "App version changed.");
            return "";
        }*/
        return registrationId;
    }

    /**
     * @return Application's {@code SharedPreferences}.
     */
    private SharedPreferences getGCMPreferences(Context context) {
        // This sample app persists the registration ID in shared preferences, but
        // how you store the regID in your app is up to you.
        return getSharedPreferences("qauth.djd.qauthclient",
                Context.MODE_PRIVATE);
    }

    /**
     * Registers the application with GCM servers asynchronously.
     * <p>
     * Stores the registration ID and app versionCode in the application's
     * shared preferences.
     */
    private void registerInBackground() {
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... params) {
                String msg = "";
                try {
                    if (gcm == null) {
                        gcm = GoogleCloudMessaging.getInstance(context);
                    }
                    regid = gcm.register(SENDER_ID);
                    msg = "Device registered, registration ID=" + regid;
                    Log.i(TAG, msg);

                    sendRegistrationIdToBackend();
                    storeRegistrationId(context, regid);
                } catch (IOException ex) {
                    msg = "Error :" + ex.getMessage();
                }
                return msg;
            }

            @Override
            protected void onPostExecute(String msg) {
                mDisplay.append(msg + "\n");
            }
        }.execute(null, null, null);

    }

    /**
     * Sends the registration ID to your server over HTTP, so it can use GCM/HTTP
     * or CCS to send messages to your app. Not needed for this demo since the
     * device sends upstream messages to a server that echoes back the message
     * using the 'from' address in the message.
     */
    private void sendRegistrationIdToBackend() {
        // Your implementation here.
    }

    /**
     * Stores the registration ID and app versionCode in the application's
     * {@code SharedPreferences}.
     *
     * @param context application's context.
     * @param regId registration ID
     */
    private void storeRegistrationId(Context context, String regId) {
        final SharedPreferences prefs = getGCMPreferences(context);
        //int appVersion = getAppVersion(context);
        //Log.i(TAG, "Saving regId on app version " + appVersion);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PROPERTY_REG_ID, regId);
        //editor.putInt(PROPERTY_APP_VERSION, appVersion);
        editor.commit();
    }

    public static void exit(String token1) {

        Intent intent = ctx.getPackageManager().getLaunchIntentForPackage("qauth.djd.dummyclient");
        intent.putExtra("qauthToken", token1);
        ctx.startActivity(intent);

    }

    public static void compareTokens() {

        SharedPreferences prefs = ctx.getSharedPreferences("qauth.djd.qauthclient",Context.MODE_PRIVATE);
        String QStoken = prefs.getString("QStoken", "QStoken");
        String DStoken = prefs.getString("DStoken", "DStoken");

        if ( QStoken.equals(DStoken) ){
            tv2.setText("QStoken == DStoken");

            Intent intent = ctx.getPackageManager().getLaunchIntentForPackage("qauth.djd.dummyclient");
            ctx.startActivity(intent);

        } else {
            tv2.setText("QStoken != DStoken");
        }

    }

    private GoogleApiClient getGoogleApiClient(Context context) {
        if (client == null)
            client = new GoogleApiClient.Builder(context)
                    .addApi(Wearable.API)
                    .build();
        return client;
    }

    private class FindWatchTask extends AsyncTask<String, Void, String> {

        FindWatchTask(){
        }
        @Override
        protected String doInBackground(String... params) {

            try {
                TextView tv3 = (TextView) findViewById(R.id.bluetoothDevices);
                client = getGoogleApiClient(ctx);
                List<Node> connectedNodes =
                        Wearable.NodeApi.getConnectedNodes(client).await().getNodes();
                for (Node node : connectedNodes) {
                    tv3.setText(tv3.getText() + " | " + node.getDisplayName() + " " + node.getId());
                }
            } catch (Exception e){
                Log.i("exceptionnn", "E: " + e);
            }
            return null;
        }

        protected void onPostExecute(String result) {
            Log.i("im done", "doneee");

            if ( result != null ){

            }
        }

    }

    public static HttpResponse makeRequest(String uri, String json) {
        try {
            HttpPost httpPost = new HttpPost(uri);
            httpPost.setEntity(new StringEntity(json));
            httpPost.setHeader("Accept", "application/json");
            httpPost.setHeader("Content-type", "application/json");
            return getNewHttpClient().execute(httpPost);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static HttpClient getNewHttpClient() {
        try {
            KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
            trustStore.load(null, null);

            SSLSocketFactory sf = new MySSLSocketFactory(trustStore);
            sf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

            HttpParams params = new BasicHttpParams();
            HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
            HttpProtocolParams.setContentCharset(params, HTTP.UTF_8);

            SchemeRegistry registry = new SchemeRegistry();
            registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
            registry.register(new Scheme("https", sf, 443));

            ClientConnectionManager ccm = new ThreadSafeClientConnManager(params, registry);

            return new DefaultHttpClient(ccm, params);
        } catch (Exception e) {
            return new DefaultHttpClient();
        }
    }

    public static class GetTokenTask extends AsyncTask<String, Void, String> {

        public String watchId;

        GetTokenTask(
                String watchId ){
            this.watchId = watchId;
        }
        @Override
        protected String doInBackground(String... params) {

            Gson gson = new GsonBuilder().create();
            Map<String, String> newLoop = new HashMap<String, String>();
            newLoop.put("watchId", this.watchId);

            String json = gson.toJson(newLoop, Map.class);

            //encrypt json

            Log.i("LoginTask", "json: " + json);
            try {
                return EntityUtils.toString(makeRequest("http://107.170.156.222:8080/gettoken", json).getEntity());
            } catch (Exception e) {
                e.printStackTrace();
            }

            return null;
        }

        protected void onPostExecute(String result) {
            if ( result != null ){

                byte[] plainText = "test".getBytes();

                try {

                    /*//decrypt result
                    Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
                    byte[] input = "abc".getBytes();
                    Cipher cipher = Cipher.getInstance("RSA/None/OAEPWithSHA1AndMGF1Padding", "BC");
                    //SecureRandom random = new SecureRandom();
                    //KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA", "BC");

                    //generator.initialize(1024, random);

                    String modulusString = "bd96a14af1dc72b14bd2864b4cde1023d3755fa071f68ac4619dfa7d9117985f4a1355069dffc100093945d945b4c1791a906034feeef95ebfc26b98161d0aecc6ceb785bec4a1ecd707db9b8877fef3f54b0b2d31cfe3e6053cb016dce28f7beee56d3d90da7bdee9668ce7ec3b635abe63b5c3594db500d3d157b7c399371f";
                    String exponentString = "9380d75ac4d41c13df071b5f089e18f696b5d241b588f8ac13bae2c1c11a177dc3d748a6ce54c6a72d85f6d735898da1984e4ddbcda0c639b67e2052029a73fd2d5e9c09af9b730e0bb61042f28c2443ca69fb42c4839f858a8b6622ebaf04e946d5e8016043bf6e0c2389c4a1b3b1cd9d74107a52fd53ee5ed5ac9c2c422429";

                    //BigInteger modulus2 = new BigInteger(hexStringToByteArray(modulusString));
                    //BigInteger exponent2 = new BigInteger(hexStringToByteArray(exponentString));
                    BigInteger modulus = new BigInteger(modulusString, 10);
                    BigInteger exponent = new BigInteger(exponentString, 10);

                    //RSAPrivateKeySpec privateSpec = new RSAPrivateKeySpec(modulus, exponent);
                    //KeyFactory factory = KeyFactory.getInstance("RSA");

                    //PrivateKey priv = factory.generatePrivate(privateSpec);

                    //byte[] encodedKey     = Base64.decode(modulusString, Base64.DEFAULT);
                    //SecretKey originalKey = new SecretKeySpec(encodedKey, 0, encodedKey.length, "AES");
                    PrivateKey privKey = KeyFactory.getInstance("RSA").generatePrivate(new RSAPrivateKeySpec(modulus, exponent));

                    cipher.init(Cipher.DECRYPT_MODE, privKey);
                    plainText = cipher.doFinal(result.getBytes());*/


/*
                    KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
                    keyGen.initialize(1024);
                    KeyPair keypair = keyGen.genKeyPair();
                    PrivateKey privateKey = keypair.getPrivate();
                    PublicKey publicKey = keypair.getPublic();

                    KeyFactory fact = KeyFactory.getInstance("RSA");
                    RSAPublicKeySpec pub = fact.getKeySpec(publicKey, RSAPublicKeySpec.class);
                    RSAPrivateKeySpec priv = fact.getKeySpec(privateKey, RSAPrivateKeySpec.class);

                    BigInteger pubModulus = pub.getModulus();
                    BigInteger pubExponent = pub.getPublicExponent();

                    BigInteger privModulus = priv.getModulus();
                    BigInteger privExponent = priv.getPrivateExponent();

                    //Log.i("private", "privModulus: " + pub. );
                    //Log.i("private", "privExponent: " + privExponent.toByteArray() );
                    //Log.i("public", "pubModulus: " + pubModulus.toByteArray() );
                    //Log.i("public", "pubExponent: " + pubExponent.toByteArray() );



                    //X509EncodedKeySpec x509ks = new X509EncodedKeySpec(
                      //      publicKey.getEncoded());


                    //RSAPublicKeySpec keySpec = new RSAPublicKeySpec(modulus, exponent);
                    //KeyFactory fact = KeyFactory.getInstance("RSA");
                    //PublicKey pubKey = fact.generatePublic(keySpec);

                    RSAPrivateKeySpec privKeySpec = new RSAPrivateKeySpec(privModulus, privExponent);
                   // KeyFactory fact = KeyFactory.getInstance("RSA");
                    PrivateKey privKey = fact.generatePrivate(privKeySpec);


                    Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
                    byte[] input = "testing my message".getBytes();
                    Cipher cipher = Cipher.getInstance("RSA/None/OAEPWithSHA1AndMGF1Padding", "BC");

                    cipher.init(Cipher.ENCRYPT_MODE, publicKey);
                    byte[] cipherText = cipher.doFinal(input);

                    Log.i("encryption", "encrypted2: " + new String(cipherText) );

                    cipher.init(Cipher.DECRYPT_MODE, privKey);
                    byte[] cipherText2 = cipher.doFinal(cipherText);

                    Log.i("decryption", "decrypted2: " + new String(cipherText2) );
*/

                    //RSAPublicKeySpec keySpec = new RSAPublicKeySpec(modulus, exponent);
                    //KeyFactory fact = KeyFactory.getInstance("RSA");
                    //PublicKey pubKey = fact.generatePublic(keySpec);

                } catch (Exception e) {
                    Log.i("GetTokenTask", "exception: " + e);
                    e.printStackTrace();
                }

                Log.i("GetTokenTask", "results: " + result);
                if ( plainText != null ) {
                    Log.i("GetTokenTask", "results2: " + new String(plainText) );
                } else {
                    Log.i("GetTokenTask", "results2: error");
                }
                Intent intent = ctx.getPackageManager().getLaunchIntentForPackage("qauth.djd.dummyclient");
                intent.putExtra("qauthToken", result);
                ctx.startActivity(intent);
                act.finish();
            } else {
                Log.i("GetTokenTask", "No internet connection");
                //Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show();
            }
        }

    }

    public static PublicKey getKey(String key){
        try{
            byte[] byteKey = Base64.decode(key.getBytes(), Base64.DEFAULT);
            X509EncodedKeySpec X509publicKey = new X509EncodedKeySpec(byteKey);
            KeyFactory kf = KeyFactory.getInstance("RSA");

            return kf.generatePublic(X509publicKey);
        }
        catch(Exception e){
            e.printStackTrace();
        }

        return null;
    }

}
