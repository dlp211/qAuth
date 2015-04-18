package qauth.djd.dummyclient;

import android.util.Log;

import com.google.gson.Gson;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

public class AccountUpdate extends PostRequest {

    private static class AccountUpdateJson {

        private String session;
        private int amount;

        public AccountUpdateJson(String session, int amount ) {
            this.session = session;
            this.amount = amount;
        }

        @Override
        public String toString() {
            return "AccountUpdateJson [session=" + this.session + ", " +
                    "amount=" + this.amount + "]";
        }

    }

    private String session;
    private int amount;

    public AccountUpdate(String session, int amount){
        this.session = session;
        this.amount = amount;
    }

    @Override
    protected String doInBackground(Void... params) {

        AccountUpdateJson auJson = new AccountUpdateJson(this.session, this.amount);

        Gson gson = new Gson();
        String json = gson.toJson(auJson);

        try {
            // return json: { nonce, encrypt(nonce), auth, hash(auth+nonce+nonceEnc) }

            Log.i("AccountUpdate", "sent json: " + json );
            return EntityUtils.toString(makeRequest("http://107.170.156.222:8081/account/update", json).getEntity());
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return null;
    }

    protected void onPostExecute(String result) {
        Log.i("AccountUpdate result", "response: " + result);

        try {
            JSONObject json = new JSONObject(result);
            String balance = json.getString("balance");
            String sessionid = json.getString("sessionid");

            GpaActivity.gpaTV.setText("balance: " + balance);

        } catch (Exception e){}

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