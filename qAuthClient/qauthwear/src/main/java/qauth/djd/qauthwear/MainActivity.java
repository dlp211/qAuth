package qauth.djd.qauthwear;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.provider.Settings.Secure;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.Collection;
import java.util.HashSet;

import qauth.djd.qauthclient.main.Watch;

public class MainActivity extends Activity implements MessageApi.MessageListener, com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks {

    private static TextView mTextView;
    private GoogleApiClient mGoogleApiClient;
    public static Context ctx;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ctx = this;
        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                mTextView = (TextView) stub.findViewById(R.id.text);
            }
        });

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(new com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(ConnectionResult result) {
                        Log.i("mGoogleApiClient", "onConnectionFailed: " + result);
                    }
                })
                        // Request access only to the Wearable API
                .addApi(Wearable.API)
                .build();
        mGoogleApiClient.connect();

                /*
        String filename = "privatekey";
        String string = "this should be my private key";
        FileOutputStream outputStream;

        try {
            outputStream = openFileOutput(filename, Context.MODE_PRIVATE);
            outputStream.write(string.getBytes());
            outputStream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        Log.i("test", "Created");
        */

        String privateKey = "this should be my private key";
        String filename = "privatekey";
        FileInputStream inputStream;

        Log.i("DEVICE NAME", android.os.Build.MODEL);

        try {
            byte[] b = new byte[privateKey.length()];
            inputStream = openFileInput(filename);
            inputStream.read(b);
            inputStream.close();
            Log.i("inputStream", "byte: " + new String(b) );
        } catch (Exception e){
            Log.i("test", "e: " + e);
        }

    }

    private Collection<String> getNodes() {
        HashSet<String> results = new HashSet<String>();
        NodeApi.GetConnectedNodesResult nodes =
                Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await();
        for (Node node : nodes.getNodes()) {
            results.add(node.getId());
        }
        return results;
    }

    @Override
    public void onConnectionSuspended(int i){

    }

    @Override
    public void onConnected(Bundle b){
        Log.i("mGoogleApiClient", "onConnected: " + b);

        Wearable.MessageApi.addListener(mGoogleApiClient, this);

        /*
        new Thread(new Runnable() {
            public void run() {
                for ( String s : getNodes() ){
                    Log.i("test", s);

                    Wearable.MessageApi.sendMessage(
                            mGoogleApiClient, s, "testing", new byte[0]).setResultCallback(
                            new ResultCallback<MessageApi.SendMessageResult>() {
                                @Override
                                public void onResult(MessageApi.SendMessageResult sendMessageResult) {
                                    if (!sendMessageResult.getStatus().isSuccess()) {
                                        Log.i("MessageApi", "Failed to send message with status code: "
                                                + sendMessageResult.getStatus().getStatusCode());
                                    } else if ( sendMessageResult.getStatus().isSuccess() ){
                                        Log.i("MessageApi", "onResult successful!");
                                    }
                                }
                            }
                    );

                }
            }
        }).start();
        */

    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        if (messageEvent.getPath().equals("REGISTER")) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mTextView.setText("Registering watch...");
                }
            });

            new Thread(new Runnable() {
                public void run() {
                    String android_id = Secure.getString(ctx.getContentResolver(),Secure.ANDROID_ID);

                    Watch watch = new Watch(android_id, android.os.Build.MODEL);
                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                    ObjectOutput out = null;
                    try { out = new ObjectOutputStream(bos); } catch (Exception e){}
                    try { out.writeObject(watch); } catch (Exception e){}
                    byte b[] = bos.toByteArray();
                    try { out.close(); } catch (Exception e){}
                    try { bos.close(); } catch (Exception e){}

                    for ( String s : getNodes() ){

                        Wearable.MessageApi.sendMessage(
                                mGoogleApiClient, s, "REGISTER", b).setResultCallback(
                                new ResultCallback<MessageApi.SendMessageResult>() {
                                    @Override
                                    public void onResult(MessageApi.SendMessageResult sendMessageResult) {
                                        if (!sendMessageResult.getStatus().isSuccess()) {
                                            Log.i("MessageApi", "Failed to send message with status code: "
                                                    + sendMessageResult.getStatus().getStatusCode());
                                        } else if ( sendMessageResult.getStatus().isSuccess() ){
                                            Log.i("MessageApi", "onResult successful!");
                                        }
                                    }
                                }
                        );

                    }
                }
            }).start();

            //save private key

        } else if (messageEvent.getPath().equals("REGISTER_COMPLETE")) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try { Thread.sleep(1000); } catch (Exception e){}
                    mTextView.setText("Registered successfully!");
                }
            });
        } else if (messageEvent.getPath().equals("AUTHORIZE")) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mTextView.setText("Authorizing watch...");
                }
            });
        } else if (messageEvent.getPath().equals("AUTHORIZE_COMPLETE")) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mTextView.setText("Authorized successfully!");
                }
            });
        }
    }
}
