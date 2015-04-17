package qauth.djd.qauthclient.POST;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import qauth.djd.qauthclient.MainActivity;

public class LoginTwoFactor extends PostRequest {

    public String token;
    public String userId;
    public Context ctx;

    public LoginTwoFactor(
            String token,
            String userId,
            Context ctx ){
        this.token = token;
        this.userId = userId;
        this.ctx = ctx;
    }
    @Override
    protected String doInBackground(Void... params) {

        Gson gson = new GsonBuilder().create();
        Map<String, String> newLoop = new HashMap<String, String>();
        newLoop.put("token", this.token);
        String json = gson.toJson(newLoop, Map.class);

        try {
            //Log.i("Register", "auth: " + this.auth );
            Log.i("LoginTwoFactor", "sent json: " + json );
            return EntityUtils.toString(makeRequest("http://107.170.156.222:8081/login/twofactor", json).getEntity());
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return null;
    }

    protected void onPostExecute(String result) {

        Log.i("LoginTwoFactor result", "response: " + result);

        String returnedToken;

        try {
            JSONObject json = new JSONObject(result);
            returnedToken = json.getString("token");

            JSONObject json2 = new JSONObject(json.getString("data"));
            String balance = json2.getString("balance");

            SharedPreferences prefs = ctx.getSharedPreferences("qauth.djd.qauthclient",Context.MODE_PRIVATE);
            prefs.edit().putString("DStoken", returnedToken).commit();
            prefs.edit().putString("balance", balance).commit();

        } catch (Exception e){}

        MainActivity.compareTokens();

        //returns { "token": "654321" }

        //202 if successfully registered
        //409 is already registered

    }

    public static int makeRequestForStatusCode(String uri, String json) {
        try {
            HttpPost httpPost = new HttpPost(uri);
            httpPost.setEntity(new StringEntity(json));
            httpPost.setHeader("Accept", "application/json");
            httpPost.setHeader("Content-type", "application/json");
            return getNewHttpClient().execute(httpPost).getStatusLine().getStatusCode();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return 0;
    }

}