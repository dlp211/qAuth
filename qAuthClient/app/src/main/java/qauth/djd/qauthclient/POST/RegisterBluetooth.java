package qauth.djd.qauthclient.POST;

import android.util.Log;

import com.google.gson.Gson;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

public class RegisterBluetooth extends PostRequest {

    private static class RegisterBluetoothJson {

        private String email;
        private String password;
        private String bluetoothId;
        private String N;
        private int E;

        public RegisterBluetoothJson( String email, String password, String bluetoothId, String N, int E ) {
            this.email = email;
            this.password = password;
            this.bluetoothId = bluetoothId;
            this.N = N;
            this.E = E;
        }

        @Override
        public String toString() {
            return "RegisterBluetoothJson [email=" + this.email + ", password=" + this.password + ", " +
                    "bluetoothId=" + this.bluetoothId + ", N=" + this.N + ", E=" + this.E + "]";
        }

    }

    public String email;
    public String password;
    public String bluetoothId;
    public String N;
    public int E;

    public RegisterBluetooth(
            String email, String password, String bluetoothId, String N, int E){
        this.email = email;
        this.password = password;
        this.bluetoothId = bluetoothId;
        this.N = N;
        this.E = E;
    }

    @Override
    protected String doInBackground(Void... params) {

        //Gson gson = new GsonBuilder().create();
        /*Map<String, String> mapJson = new HashMap<String, String>();
        mapJson.put("email", this.email);
        mapJson.put("password", this.password);
        mapJson.put("bluetoothId", this.bluetoothId);
        mapJson.put("N", this.N);
        mapJson("E", this.E);*/

        RegisterBluetoothJson rbJson = new RegisterBluetoothJson(this.email, this.password, this.bluetoothId, this.N, this.E);

        Gson gson = new Gson();
        String json = gson.toJson(rbJson);

        Log.i("RegisterBluetooth", "json: " + json);
        try {
            Log.i("RegisterBluetooth", "email: " + this.email + ", password: " + this.password + ", bluetoothId: " + bluetoothId);
            return String.valueOf(makeRequestForStatusCode("http://107.170.156.222:8080/register/bluetooth", json));
        } catch (IllegalStateException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return null;
    }

    protected void onPostExecute(String result) {
        Log.i("RegisterBluetooth result", "response: " + result);

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