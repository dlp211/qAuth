package qauth.djd.dummyclient;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

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
import java.util.Map;


public class MainActivity extends ActionBarActivity {

    static TextView tv3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent intent = getIntent();
        tv3 = (TextView) findViewById(R.id.textView3);

        try {
            if ( intent.getExtras().getString("qauthToken") != null ){
                tv3.setText( "qauthToken: " + intent.getExtras().getString("qauthToken") );
                new AuthMeTask( intent.getExtras().getString("qauthToken"), "1" ).execute();
            }
        } catch (Exception e){
            Log.i("exception", "exception: " + e);
        }
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

    /*
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            Log.i("result", "WAS OKKKKKKKKKKKKK");
        }
    }*/

    public void clickLogin( View v ){
        new LoginTask( "davemayne", "test" ).execute();

        Intent intent = getPackageManager().getLaunchIntentForPackage("qauth.djd.qauthclient");
        intent.putExtra("dummy", "hi from the dummy");
        startActivityForResult(intent, 1);
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

        LoginTask(
                String username,
                String password ){
            this.username = username;
            this.password = password;
        }
        @Override
        protected String doInBackground(String... params) {

            Gson gson = new GsonBuilder().create();
            Map<String, String> newLoop = new HashMap<String, String>();
            newLoop.put("username", this.username);
            newLoop.put("password", this.password);

            String json = gson.toJson(newLoop, Map.class);
            Log.i("LoginTask", "json: " + json);
            try {
                return EntityUtils.toString(makeRequest("http://107.170.156.222:8081/login/tokenhere", json).getEntity());
            } catch (Exception e) {
                e.printStackTrace();
            }

            return null;
        }

        protected void onPostExecute(String result) {
            if ( result != null ){
                Log.i("Login Task", "results: " + result);
            } else {
                Log.i("Login Task", "No internet connection");
                //Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show();
            }
        }

    }

    private class AuthMeTask extends AsyncTask<String, Void, String> {

        public String token;
        public String userId;

        AuthMeTask(
                String token,
                String userId ){
            this.token = token;
            this.userId = userId;
        }
        @Override
        protected String doInBackground(String... params) {

            Gson gson = new GsonBuilder().create();
            Map<String, String> newLoop = new HashMap<String, String>();
            newLoop.put("token", this.token);
            newLoop.put("userId", this.userId);

            String json = gson.toJson(newLoop, Map.class);
            Log.i("LoginTask", "json: " + json);
            try {
                return EntityUtils.toString(makeRequest("http://107.170.156.222:8081/authme", json).getEntity());
            } catch (Exception e) {
                e.printStackTrace();
            }

            return null;
        }

        protected void onPostExecute(String result) {
            if ( result != null ){
                Log.i("Login Task", "results: " + result);
                tv3.setText( tv3.getText() + "\nAuthenticated successfully!");
            } else {
                Log.i("Login Task", "No internet connection");
                //Toast.makeText(this, "No internet connection", Toast.LENGTH_SHORT).show();
            }
        }

    }

}
