package qauth.djd.dummyclient;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings.Secure;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;
import com.google.gson.Gson;

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
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.KeyStore;
import java.util.Collection;
import java.util.HashSet;

public class MainActivity extends ActionBarActivity implements SensorEventListener, MessageApi.MessageListener, com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks {

    static TextView tv3;
    private static String deviceID;

    public static Context ctx;

    private SensorManager mSensorManager;
    private Sensor mSensor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ctx = this;
        deviceID = Secure.getString(this.getContentResolver(), Secure.ANDROID_ID);
        tv3 = (TextView) findViewById(R.id.textView3);

        Intent intent = getIntent();
        String balance = intent.getStringExtra("balance");
        String level = intent.getStringExtra("level");
        String sessionid = intent.getStringExtra("sessionid");

        if ( balance != null ){

            Log.i("MainActivity", "found balance: " + balance + " with sessionid: " + sessionid);

            Intent intent2 = new Intent(this, GpaActivity.class);
            intent2.putExtra("balance", balance);
            intent2.putExtra("level", level);

            SharedPreferences prefs = ctx.getSharedPreferences("qauth.djd.dummyclient",Context.MODE_PRIVATE);
            prefs.edit().putString("sessionid", sessionid).commit();

            startActivity(intent2);

        }

        if (GCM.checkPlayServices()) {
            GCM.gcm = GoogleCloudMessaging.getInstance(this);
            GCM.regid = GCM.getRegistrationId(ctx);

            if (GCM.regid.isEmpty()) {
                GCM.registerInBackground();
            }
        } else {
            Log.i("GCM checkPlayerServerices()", "No valid Google Play Services APK found.");
        }

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(new com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(ConnectionResult result) {
                        Log.i("mGoogleApiClient", "onConnectionFailed: " + result);
                    }
                })
                        // Request access only to the Wearable API
                .addApi(Wearable.API)
                .build();
        mGoogleApiClient.connect();

        registerReceiver(remoteLogin, new IntentFilter("remoteLogin"));

        //mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        //mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);

    }

    private final BroadcastReceiver remoteLogin = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i("I SHOULD", "BE LOGGING IN!!!");



        }
    };

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(remoteLogin);
    }

    protected void onResume() {
        super.onResume();
        //mSensorManager.registerListener(this, mSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    protected void onPause() {
        super.onPause();
        //mSensorManager.unregisterListener(this);
    }

    // Create a constant to convert nanoseconds to seconds.
    private static final float NS2S = 1.0f / 1000000000.0f;
    private final float[] deltaRotationVector = new float[4];
    private float timestamp;

    public void onAccuracyChanged(Sensor sensor, int accuracy){
        return;
    }

    public void onSensorChanged(SensorEvent event) {
        // This timestep's delta rotation to be multiplied by the current rotation
        // after computing it from the gyro sample data.
        if (timestamp != 0) {
            final float dT = (event.timestamp - timestamp) * NS2S;
            // Axis of the rotation sample, not normalized yet.
            float axisX = event.values[0];
            float axisY = event.values[1];
            float axisZ = event.values[2];

            // Calculate the angular speed of the sample
            float omegaMagnitude = (float) Math.sqrt(axisX * axisX + axisY * axisY + axisZ * axisZ);

            // Normalize the rotation vector if it's big enough to get the axis
            // (that is, EPSILON should represent your maximum allowable margin of error)

            Log.i("EPSILON", "epsilon: " + omegaMagnitude);

            if (omegaMagnitude > 1) {
            axisX /= omegaMagnitude;
            axisY /= omegaMagnitude;
            axisZ /= omegaMagnitude;
            }

            // Integrate around this axis with the angular speed by the timestep
            // in order to get a delta rotation from this sample over the timestep
            // We will convert this axis-angle representation of the delta rotation
            // into a quaternion before turning it into the rotation matrix.
            float thetaOverTwo = omegaMagnitude * dT / 2.0f;
            float sinThetaOverTwo = (float) Math.sin(thetaOverTwo);
            float cosThetaOverTwo = (float) Math.cos(thetaOverTwo);
            deltaRotationVector[0] = sinThetaOverTwo * axisX;
            deltaRotationVector[1] = sinThetaOverTwo * axisY;
            deltaRotationVector[2] = sinThetaOverTwo * axisZ;
            deltaRotationVector[3] = cosThetaOverTwo;
        }
        timestamp = event.timestamp;
        float[] deltaRotationMatrix = new float[9];
        SensorManager.getRotationMatrixFromVector(deltaRotationMatrix, deltaRotationVector);
        // User code should concatenate the delta rotation we computed with the current rotation
        // in order to get the updated rotation.
        // rotationCurrent = rotationCurrent * deltaRotationMatrix;
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

    public static Handler handler;

    public void clickLogin( final View v ){
        String username = ((EditText)findViewById(R.id.userName)).getText().toString();
        String password = ((EditText)findViewById(R.id.password)).getText().toString();

        if ( username.equals("dlp")) {

            SharedPreferences prefs = ctx.getSharedPreferences("qauth.djd.dummyclient", Context.MODE_PRIVATE);
            String gcmId = prefs.getString("registration_id", "null reg id");

            new LoginTask(username, password, this.deviceID, gcmId, this).execute();

            new Thread(new Runnable() {
                public void run() {
                    if ( ! getNodes().isEmpty() ) {
                        Intent intent = getPackageManager().getLaunchIntentForPackage("qauth.djd.qauthclient");
                        intent.putExtra("packageName", "qauth.djd.dummyclient");
                        startActivityForResult(intent, 1);
                    }
                }
            }).start();

        } else if ( username.equals("dgk")) {

            SharedPreferences prefs = ctx.getSharedPreferences("qauth.djd.dummyclient", Context.MODE_PRIVATE);
            String gcmId = prefs.getString("registration_id", "null reg id");

            new LoginTask(username, password, this.deviceID, gcmId, this).execute();
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

    private class LoginTask extends AsyncTask<String, Void, String> {

        public String username;
        public String password;
        public String deviceId;
        public String gcmId;
        public Context ctx;

        private class LoginTaskJson {

            private String username;
            public String password;
            public String deviceId;
            public String gcmId;
            private int is2FA;

            public LoginTaskJson(String username, String password, String deviceId, String gcmId, int is2FA ) {
                this.username = username;
                this.password = password;
                this.deviceId = deviceId;
                this.gcmId = gcmId;
                this.is2FA = is2FA;
            }

            @Override
            public String toString() {
                return "LoginTaskJson [username=" + this.username + ", password=" + this.password + ", deviceId=" + this.deviceId + ", gcmId=" + this.gcmId + ", is2FA=" + this.is2FA + "]";
            }

        }

        LoginTask(
                String username,
                String password,
                String deviceId,
                String gcmId,
                Context ctx){
            this.username = username;
            this.password = password;
            this.deviceId = deviceId;
            this.gcmId = gcmId;
            this.ctx = ctx;
        }
        
        @Override
        protected String doInBackground(String... params) {

            LoginTaskJson ltJson;
            if ( getNodes().isEmpty() ) {
                ltJson = new LoginTaskJson(this.username, this.password, this.deviceId, this.gcmId, 0);
            } else {
                ltJson = new LoginTaskJson(this.username, this.password, this.deviceId, this.gcmId, 1);
            }

            Gson gson = new Gson();
            String json = gson.toJson(ltJson);

            Log.i("LoginTask", "json: " + json);
            try {
                return EntityUtils.toString(makeRequest("http://107.170.156.222:8081/login", json).getEntity());
            } catch (Exception e) {
                e.printStackTrace();
            }

            return null;
        }

        protected void onPostExecute(String result) {
            if ( result != null ){
                Log.i("Login Task", "results: " + result);

                try {
                    JSONObject json = new JSONObject(result);
                    String balance = json.getString("balance");
                    String sessionid = json.getString("sessionid");
                    String level = json.getString("level");

                    if ( sessionid.length() > 0 ) {
                        SharedPreferences prefs = ctx.getSharedPreferences("qauth.djd.dummyclient", Context.MODE_PRIVATE);
                        prefs.edit().putString("sessionid", sessionid).commit();
                    }

                    Intent intent2 = new Intent(ctx, GpaActivity.class);
                    intent2.putExtra("balance", balance);
                    intent2.putExtra("level", level);
                    startActivity(intent2);

                    //tv3.setText("GPA: " + foundGPA);
                } catch (Exception e){}

            } else {
                Log.i("Login Task", "No internet connection");
                //Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show();
            }
        }

    }

    public static GoogleApiClient mGoogleApiClient;

    public static Collection<String> getNodes() {
        HashSet<String> results = new HashSet<String>();
        NodeApi.GetConnectedNodesResult nodes =
                Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await();
        for (Node node : nodes.getNodes()) {
            results.add(node.getId());
            Log.i("test", "NODE ID: " + node.getId() + " NODE DISPLAYNAME:" + node.getDisplayName() );
        }
        return results;
    }

    @Override
    public void onConnectionSuspended(int i){
        Wearable.MessageApi.removeListener(mGoogleApiClient, this);
    }

    @Override
    public void onConnected(Bundle b){
        Log.i("mGoogleApiClient", "onConnected: " + b);

        Wearable.MessageApi.addListener(mGoogleApiClient, this);

    }

    public static boolean busy=false;

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        Log.i("DummyClient", "on message received23123123!!!!!");

        if (messageEvent.getPath().equals("SESSION_TRANSFER")) {

            if ( busy == false ) {

                busy = true;

                SharedPreferences prefs = ctx.getSharedPreferences("qauth.djd.dummyclient", Context.MODE_PRIVATE);
                new LoginSession(prefs.getString("sessionid", "null session id")).execute();

                try { Thread.sleep(3000); } catch (Exception e){}

                busy = false;

            }

        }

    }

}
