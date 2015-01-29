package qauth.djd.qauthclient;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.google.android.gms.common.api.GoogleApiClient;
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
import java.security.KeyStore;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends ActionBarActivity {

    GifView gifView;
    private GoogleApiClient client;
    static Context ctx;
    static TextView tv2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ctx = this;

        GifView gif_view = (GifView) findViewById(R.id.testGifView );
        gif_view.setGifImageResourceID(R.raw.green);

        //new FindWatchTask().execute();


    /*

        new Runnable() {
            public void run() {
                TextView tv3 = (TextView) findViewById(R.id.bluetoothDevices);
                client = getGoogleApiClient(getApplicationContext());
                List<Node> connectedNodes =
                        Wearable.NodeApi.getConnectedNodes(client).await().getNodes();

                for ( Node node : connectedNodes ){
                    tv3.setText( tv3.getText() + " | " + node.getDisplayName() + " " + node.getId() );
                }
            }
        }.run();

    */
        tv2 = (TextView) findViewById(R.id.textView2);
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            public void run() {
                tv2.setText("Sending Watch Id");

            }
        }, 2000);

        handler.postDelayed(new Runnable() {
            public void run() {
                tv2.setText("Retrieving Token");
            }
        }, 3500);

        handler.postDelayed(new Runnable() {
            public void run() {
                new GetTokenTask("007").execute();
            }
        }, 4000);

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

    public void exit() {

        Intent intent = getPackageManager().getLaunchIntentForPackage("qauth.djd.dummyclient");
        intent.putExtra("qauth", "hi from the qauth client");
        startActivityForResult(intent, 1);
        finish();
        //setResult(Activity.RESULT_OK);
        //finish();
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
                Log.i("yo", "testing");
                TextView tv3 = (TextView) findViewById(R.id.bluetoothDevices);
                client = getGoogleApiClient(ctx);
                Log.i("yo", "testing2");
                List<Node> connectedNodes =
                        Wearable.NodeApi.getConnectedNodes(client).await().getNodes();
                Log.i("yo", "testing3");
                for (Node node : connectedNodes) {
                    tv3.setText(tv3.getText() + " | " + node.getDisplayName() + " " + node.getId());
                }
                Log.i("yo", "testing4");
            } catch (Exception e){
                Log.i("exceptionnn", "EXCEPTIONNNNN: " + e);
            }
            Log.i("yo", "sup");
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

    private class GetTokenTask extends AsyncTask<String, Void, String> {

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
                Log.i("GetTokenTask", "results: " + result);
                Intent intent = getPackageManager().getLaunchIntentForPackage("qauth.djd.dummyclient");
                intent.putExtra("qauthToken", result);
                startActivity(intent);
                finish();
            } else {
                Log.i("GetTokenTask", "No internet connection");
                //Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show();
            }
        }

    }

}
