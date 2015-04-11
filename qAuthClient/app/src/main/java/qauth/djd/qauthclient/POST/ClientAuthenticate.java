package qauth.djd.qauthclient.POST;

import android.util.Log;

import com.google.gson.Gson;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

public class ClientAuthenticate extends PostRequest {

    private static class ClientAuthenticateJson {

        private String nonce;
        private String nonceEnc;
        private int auth;
        private String hash;

        public ClientAuthenticateJson( String nonce, String nonceEnc, int auth, String hash ) {
            this.nonce = nonce;
            this.nonceEnc = nonceEnc;
            this.auth = auth;
            this.hash = hash;
        }

        @Override
        public String toString() {
            return "ClientAuthenticateJson [nonce=" + this.nonce + ", nonceEnc=" + this.nonceEnc + ", " +
                    "auth=" + this.auth + ", hash=" + this.hash + "]";
        }

    }

    private String nonce;
    private String nonceEnc;
    private int auth;
    private String hash;

    public ClientAuthenticate(String nonce, String nonceEnc, int auth, String hash){
        this.nonce = nonce;
        this.nonceEnc = nonceEnc;
        this.auth = auth;
        this.hash = hash;
    }

    @Override
    protected String doInBackground(Void... params) {

        ClientAuthenticateJson caJson = new ClientAuthenticateJson(this.nonce, this.nonceEnc, this.auth, this.hash);

        Gson gson = new Gson();
        String json = gson.toJson(caJson);

        try {
            // return json: { nonce, encrypt(nonce), auth, hash(auth+nonce+nonceEnc) }

            Log.i("ClientAuthenticate", "sent json: " + json );
            return String.valueOf(makeRequestForStatusCode("http://107.170.156.222:8080/client/authenticate", json));
        } catch (IllegalStateException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return null;
    }

    protected void onPostExecute(String result) {
        Log.i("ClientAuthenticate result", "response: " + result);

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