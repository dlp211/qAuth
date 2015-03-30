package qauth.djd.qauthclient.POST;

import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

public class Register extends PostRequest {

    public String email;
    public String password;
    public String deviceId;
    public String gcmid;

    public Register(
            String email, String password, String deviceId, String gcmid ){
        this.email = email;
        this.password = password;
        this.deviceId = deviceId;
        this.gcmid = gcmid;
    }

    @Override
    protected String doInBackground(Void... params) {

        Gson gson = new GsonBuilder().create();
        Map<String, String> mapJson = new HashMap<String, String>();
        mapJson.put("email", this.email);
        mapJson.put("password", this.password);
        mapJson.put("deviceId", this.deviceId);
        mapJson.put("gcmid", this.gcmid);
        String json = gson.toJson(mapJson, Map.class);
        try {
            Log.i("Register", "email: " + this.email + ", password: " + this.password + ", deviceId: " + deviceId + ", gcmid: " + gcmid );
            return String.valueOf(makeRequestForStatusCode("http://107.170.156.222:8080/register", json));
        } catch (IllegalStateException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return null;
    }

    protected void onPostExecute(String result) {
        Log.i("Register result", "response: " + result);

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