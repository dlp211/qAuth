package qauth.djd.qauthclient.POST;

import android.content.Context;
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
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAKeyGenParameterSpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.HashMap;
import java.util.Map;

import qauth.djd.qauthclient.login.LoginActivity;

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
            generate();
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

    public static void generate() {
        try {

            // Create the public and private keys
            SecureRandom random = new SecureRandom();
            RSAKeyGenParameterSpec spec = new RSAKeyGenParameterSpec(1024, RSAKeyGenParameterSpec.F4);
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA", "SC");
            generator.initialize(spec, random);
            KeyPair pair = generator.generateKeyPair();

            LoginActivity.pubKey = (RSAPublicKey) pair.getPublic();
            LoginActivity.privKey = (RSAPrivateKey) pair.getPrivate();

            SharedPreferences prefs = ctx.getSharedPreferences("qauth.djd.qauthclient", Context.MODE_PRIVATE);

            String bytes = Base64.encodeToString(LoginActivity.privKey.getEncoded(), Base64.DEFAULT);
            prefs.edit().putString("privKey", bytes ).commit();

            String pk = Base64.encodeToString(LoginActivity.pubKey.getEncoded(), Base64.DEFAULT);
            prefs.edit().putString("pubKey", pk ).commit();

            // { email, password, bluetoothId, N, E }
            // string N, int E
            String N = LoginActivity.pubKey.getModulus().toString(10); //N
            int E = LoginActivity.pubKey.getPublicExponent().intValue(); //E

            new RegisterBluetooth("dlp", "password", "1001", N, E).execute();

            //Log.d("RSA", pubKey.toString());
            //String pk = Base64.encodeToString(pubKey.getEncoded(), Base64.DEFAULT);
            //String pk2 = pubKey.getFormat();
            //Log.d("RSA", pk + "   " + pk2);
            //RSAPublicKey pubKey2 = (RSAPublicKey)getPubKeyFromString(PUBKEY);
            //Log.d("RSA", pubKey2.getModulus().toString(10));

            /* generate from string
            String bytes = Base64.encodeToString(privKey.getEncoded(), Base64.DEFAULT);
            PrivateKey privKey2 = getPrivKeyFromString(bytes);
            */

            //Log.d("RSA", privKey2.toString());


        }
        catch (Exception e) {
            Log.d("RSA", e.toString());
        }
    }

}