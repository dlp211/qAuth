package qauth.djd.dummyclient;

import android.util.Log;

import com.google.gson.Gson;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

public class LoginSession extends PostRequest {

    private static class LoginSessionJson {

        private String sessionid;

        public LoginSessionJson(String sessionid) {
            this.sessionid = sessionid;
        }

        @Override
        public String toString() {
            return "AccountUpdateJson [sessionid=" + this.sessionid + "]";
        }

    }

    private String session;

    public LoginSession(String session){
        this.session = session;
    }

    @Override
    protected String doInBackground(Void... params) {

        LoginSessionJson lsJson = new LoginSessionJson(this.session);

        Gson gson = new Gson();
        String json = gson.toJson(lsJson);

        try {
            // return json: { nonce, encrypt(nonce), auth, hash(auth+nonce+nonceEnc) }

            Log.i("LoginSession", "sent json: " + json );
            return EntityUtils.toString(makeRequest("http://107.170.156.222:8081/login/session", json).getEntity());
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return null;
    }

    protected void onPostExecute(String result) {
        Log.i("LoginSession result", "response: " + result);

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