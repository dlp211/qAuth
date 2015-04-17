package qauth.djd.qauthclient.POST;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Base64;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.HashMap;
import java.util.Map;

import qauth.djd.qauthclient.main.MainTabsActivity;

public class Register extends PostRequest {

    public String email;
    public String password;
    public String deviceId;
    public String gcmid;
    public static Context ctx;

    public Register(
            String email, String password, String deviceId, String gcmid, Context ctx ){
        this.email = email;
        this.password = password;
        this.deviceId = deviceId;
        this.gcmid = gcmid;
        this.ctx = ctx;
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

    protected void onPostExecute( String result ) {
        Log.i("Register result", "response: " + result);

        //202 if successfully registered

        if ( result.equals("202") ) {

            SharedPreferences prefs = ctx.getSharedPreferences(
                    "qauth.djd.qauthclient",
                    Context.MODE_PRIVATE);
            prefs.edit().putBoolean("loggedIn", true).commit();
            prefs.edit().putString("email", this.email).commit();
            prefs.edit().putString("password", this.password).commit();

            ctx.startActivity(new Intent(ctx, MainTabsActivity.class));

            //generate();
        } else if ( result.equals("409") ) {

        } else {
            Log.i("Register", "error");
        }

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

    static {
        Security.insertProviderAt(new org.spongycastle.jce.provider.BouncyCastleProvider(), 1);
    }

    public static String PUBKEY = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDhdTU+30M3X54TaEL2iiyfW/Is\n" +
            "empfRXwLAySf1flat5VJamHg7kIDOBmFEDLW9vNZ5H9C2Gt/bMsxjH6auZxr9lLM\n" +
            "cI+Ctfdh6eoACPbvKmBASUjDxml3Rd/vJFiMh9SWUYKevjnf2uqg8iG+RCvxUyfS\n" +
            "/6Dy30lQIm2bTF91UQIDAQAB";

    public static PrivateKey getPrivKeyFromString(String key) throws Exception {
        byte[] clear = Base64.decode(key, Base64.DEFAULT);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(clear);
        KeyFactory kf = KeyFactory.getInstance("RSA", "SC");
        PrivateKey pk = kf.generatePrivate(keySpec);
        return pk;
    }

    public static PublicKey getPubKeyFromString(String key) throws Exception {
        KeyFactory kf = KeyFactory.getInstance("RSA", "SC");
        byte[] publicKeyBytes = Base64.decode(key.getBytes("UTF-8"), Base64.DEFAULT);
        X509EncodedKeySpec x = new X509EncodedKeySpec(publicKeyBytes);
        return kf.generatePublic(x);
    }

}