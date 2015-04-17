/*
 * Copyright 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package qauth.djd.qauthclient.main;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Base64;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;
import com.melnykov.fab.FloatingActionButton;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.Security;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAKeyGenParameterSpec;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import qauth.djd.qauthclient.Authenticate;
import qauth.djd.qauthclient.POST.RegisterBluetooth;
import qauth.djd.qauthclient.R;

/**
 * Simple Fragment used to display some meaningful content for each page in the sample's
 * {@link android.support.v4.view.ViewPager}.
 */
public class ContentFragment extends Fragment implements MessageApi.MessageListener, com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks {

    private static final String TAG = "RecyclerViewFragment";
    private static final String KEY_LAYOUT_MANAGER = "layoutManager";
    //private static final int SPAN_COUNT = 2;
    private static final int DATASET_COUNT = 15;

    private enum LayoutManagerType {
        //GRID_LAYOUT_MANAGER,
        LINEAR_LAYOUT_MANAGER
    }

    static {
        Security.insertProviderAt(new org.spongycastle.jce.provider.BouncyCastleProvider(), 1);
    }

    protected LayoutManagerType mCurrentLayoutManagerType;

    public RecyclerView mRecyclerView;
    public static WatchAdapter wAdapter;
    public static ProviderAdapter pAdapter;
    protected RecyclerView.LayoutManager mLayoutManager;
    public static List<Watch> wDataset;
    public static List<Provider> pDataset;

    private static final String KEY_TITLE = "title";
    private static final String KEY_INDICATOR_COLOR = "indicator_color";
    private static final String KEY_DIVIDER_COLOR = "divider_color";

    /**
     * @return a new instance of {@link qauth.djd.qauthclient.main.ContentFragment}, adding the parameters into a bundle and
     * setting them as arguments.
     */
    public static ContentFragment newInstance(CharSequence title, int indicatorColor,
            int dividerColor) {
        Bundle bundle = new Bundle();
        bundle.putCharSequence(KEY_TITLE, title);
        bundle.putInt(KEY_INDICATOR_COLOR, indicatorColor);
        bundle.putInt(KEY_DIVIDER_COLOR, dividerColor);

        ContentFragment fragment = new ContentFragment();
        fragment.setArguments(bundle);

        initDataset();

        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {

        Bundle args = getArguments();
        if ( args.getCharSequence(KEY_TITLE).toString().equals("Providers") ) {

            View rootView = inflater.inflate(R.layout.providers_view_frag, container, false);

            mRecyclerView = (RecyclerView) rootView.findViewById(R.id.recyclerView);
            mLayoutManager = new LinearLayoutManager(getActivity());
            mCurrentLayoutManagerType = LayoutManagerType.LINEAR_LAYOUT_MANAGER;

            if (savedInstanceState != null) {
                // Restore saved layout manager type.
                mCurrentLayoutManagerType = (LayoutManagerType) savedInstanceState
                        .getSerializable(KEY_LAYOUT_MANAGER);
            }
            setRecyclerViewLayoutManager(mCurrentLayoutManagerType);

            pAdapter = new ProviderAdapter(pDataset);
            mRecyclerView.setAdapter(pAdapter);

            final PackageManager pm = getActivity().getPackageManager();
            List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);

            for (ApplicationInfo packageInfo : packages) {
                //Log.i(TAG, "Installed package :" + packageInfo.packageName);
                //Log.i(TAG, "Source dir : " + packageInfo.sourceDir);
                //Log.i(TAG, "Launch Activity :" + pm.getLaunchIntentForPackage(packageInfo.packageName));

                if ( packageInfo.packageName.equals("qauth.djd.dummyclient")){
                    Provider provider = new Provider("DummyClient", packageInfo.packageName);
                    pDataset.add(provider);
                    pAdapter.notifyDataSetChanged();
                }

            }

            //get local package names and cross reference with providers on server ("/provider/available")
            //display package names in listview
            //allow user to click on item to activate or deactivate
            // '-> have check box with progress bar indicating status

            return rootView;

        } else {

            View rootView = inflater.inflate(R.layout.recycler_view_frag, container, false);
            mRecyclerView = (RecyclerView) rootView.findViewById(R.id.recyclerView);
            mLayoutManager = new LinearLayoutManager(getActivity());
            mCurrentLayoutManagerType = LayoutManagerType.LINEAR_LAYOUT_MANAGER;

            if (savedInstanceState != null) {
                // Restore saved layout manager type.
                mCurrentLayoutManagerType = (LayoutManagerType) savedInstanceState
                        .getSerializable(KEY_LAYOUT_MANAGER);
            }
            setRecyclerViewLayoutManager(mCurrentLayoutManagerType);

            wAdapter = new WatchAdapter(wDataset);
            mRecyclerView.setAdapter(wAdapter);

            FloatingActionButton fab = (FloatingActionButton) rootView.findViewById(R.id.fab);
            fab.attachToRecyclerView(mRecyclerView);

            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.i("test", "clicked!");

                    AlertDialog.Builder builderSingle = new AlertDialog.Builder(
                            getActivity());
                    builderSingle.setIcon(R.drawable.ic_launcher);
                    builderSingle.setTitle("Select Bluetooth Device");
                    final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<String>(
                            getActivity(),
                            android.R.layout.select_dialog_singlechoice);
                    new Thread(new Runnable() {
                        public void run() {
                            for (String s : getNodes()) {
                                arrayAdapter.add(s);
                            }
                        }
                    }).start();
                    builderSingle.setNegativeButton("Cancel",
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            });
                    builderSingle.setAdapter(arrayAdapter,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {

                                    String nodeId = arrayAdapter.getItem(which);
                                    String privKey = null;
                                    String pubKey = null;

                                    try {
                                        SecureRandom random = new SecureRandom();
                                        RSAKeyGenParameterSpec spec = new RSAKeyGenParameterSpec(1024, RSAKeyGenParameterSpec.F4);
                                        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA", "SC");
                                        generator.initialize(spec, random);
                                        KeyPair pair = generator.generateKeyPair();
                                        privKey = Base64.encodeToString(pair.getPrivate().getEncoded(), Base64.DEFAULT);
                                        pubKey = Base64.encodeToString(pair.getPublic().getEncoded(), Base64.DEFAULT);
                                    } catch (Exception e){ Log.i("generate", "error: " + e);}

                                    //Log.i("keys", "priv key : " + privKey);

                                    //String privKey = Base64.encodeToString(MainTabsActivity.privKey.getEncoded(), Base64.DEFAULT);
                                    //String pubKey = Base64.encodeToString(MainTabsActivity.pubKey.getEncoded(), Base64.DEFAULT);

                                    Keys keys = new Keys(privKey, pubKey);
                                    ByteArrayOutputStream bos = new ByteArrayOutputStream();
                                    ObjectOutput out = null;
                                    try { out = new ObjectOutputStream(bos); } catch (Exception e){}
                                    try { out.writeObject(keys); } catch (Exception e){}
                                    byte b[] = bos.toByteArray();
                                    try { out.close(); } catch (Exception e){}
                                    try { bos.close(); } catch (Exception e){}

                                    Wearable.MessageApi.sendMessage(
                                            mGoogleApiClient, nodeId, "REGISTER", b).setResultCallback(
                                            new ResultCallback<MessageApi.SendMessageResult>() {
                                                @Override
                                                public void onResult(MessageApi.SendMessageResult sendMessageResult) {
                                                    if (!sendMessageResult.getStatus().isSuccess()) {
                                                        Log.i("MessageApi", "Failed to send message with status code: "
                                                                + sendMessageResult.getStatus().getStatusCode());
                                                    } else if (sendMessageResult.getStatus().isSuccess()) {
                                                        Log.i("MessageApi", "onResult successful!");
                                                    }
                                                }
                                            }
                                    );

                                }
                            });
                    builderSingle.show();


                }
            });

            mGoogleApiClient = new GoogleApiClient.Builder(getActivity())
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

            /*BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();

            for(BluetoothDevice bt : pairedDevices)
                Log.i("BluetoothDevice", "pairedDevice: " + bt.toString());*/

            return rootView;

        }

    }

    public static GoogleApiClient mGoogleApiClient;

    private Collection<String> getNodes() {
        HashSet<String> results = new HashSet<String>();
        NodeApi.GetConnectedNodesResult nodes =
                Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await();
        for (Node node : nodes.getNodes()) {
            results.add(node.getId());
            Log.i("test", "NODE ID: " + node.getId() + " NODE DISPLAYNAME:" + node.getDisplayName() );
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

    }

    static Watch watch;

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        Log.i("qAuthWear", "on message received23123123!!!!!");

        if (messageEvent.getPath().equals("REGISTER")) {
            //Log.i("test", "device id:" + messageEvent.getData().toString());

            watch = null;
            ByteArrayInputStream bis = new ByteArrayInputStream(messageEvent.getData());
            ObjectInput in = null;
            try { in = new ObjectInputStream(bis); } catch (Exception e) { Log.i("exception1", "e: " + e); }
            try { watch = (Watch) in.readObject(); } catch (Exception e) { Log.i("exception2", "e: " + e); }

            if ( watch != null ){
                Log.i("WATCH SERIALIZABLE", "deviceId:" + watch.deviceId + " model:" + watch.model);
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (!wDataset.contains(watch)) {
                            wDataset.add(watch);
                            wAdapter.notifyDataSetChanged();
                        }
                    }
                });

                RSAPrivateKey rsaPrivKey = null;
                RSAPublicKey rsaPubKey = null;

                try {
                    rsaPrivKey = (RSAPrivateKey) Authenticate.getPrivKeyFromString(watch.privKey);
                    rsaPubKey = (RSAPublicKey) Authenticate.getPubKeyFromString(watch.pubKey);
                } catch (Exception e) {}


                String N = rsaPubKey.getModulus().toString(10); //N
                int E = rsaPubKey.getPublicExponent().intValue(); //E

                for (String nodeId : getNodes()) {

                    SharedPreferences prefs = getActivity().getSharedPreferences("qauth.djd.qauthclient", Context.MODE_PRIVATE);
                    String email = prefs.getString("email", "email");
                    String password = prefs.getString("password", "password");
                    new RegisterBluetooth(email, password, watch.deviceId, N, E, nodeId).execute();

                }

            } else {
                Log.i("WATCH SERIALIZABLE", "watch = null");
            }

        }

    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Bundle args = getArguments();

        if (args != null) {
            /*
            TextView title = (TextView) view.findViewById(R.id.item_title);
            title.setText("Title: " + args.getCharSequence(KEY_TITLE));

            int indicatorColor = args.getInt(KEY_INDICATOR_COLOR);
            TextView indicatorColorView = (TextView) view.findViewById(R.id.item_indicator_color);
            indicatorColorView.setText("Indicator: #" + Integer.toHexString(indicatorColor));
            indicatorColorView.setTextColor(indicatorColor);

            int dividerColor = args.getInt(KEY_DIVIDER_COLOR);
            TextView dividerColorView = (TextView) view.findViewById(R.id.item_divider_color);
            dividerColorView.setText("Divider: #" + Integer.toHexString(dividerColor));
            dividerColorView.setTextColor(dividerColor);
            */
        }
    }

    /**
     * Set RecyclerView's LayoutManager to the one given.
     *
     * @param layoutManagerType Type of layout manager to switch to.
     */
    public void setRecyclerViewLayoutManager(LayoutManagerType layoutManagerType) {
        int scrollPosition = 0;

        // If a layout manager has already been set, get current scroll position.
        if (mRecyclerView.getLayoutManager() != null) {
            scrollPosition = ((LinearLayoutManager) mRecyclerView.getLayoutManager())
                    .findFirstCompletelyVisibleItemPosition();
        }

        switch (layoutManagerType) {
            /*case GRID_LAYOUT_MANAGER:
                mLayoutManager = new GridLayoutManager(getActivity(), SPAN_COUNT);
                mCurrentLayoutManagerType = LayoutManagerType.GRID_LAYOUT_MANAGER;
                break;*/
            case LINEAR_LAYOUT_MANAGER:
                mLayoutManager = new LinearLayoutManager(getActivity());
                mCurrentLayoutManagerType = LayoutManagerType.LINEAR_LAYOUT_MANAGER;
                break;
            default:
                mLayoutManager = new LinearLayoutManager(getActivity());
                mCurrentLayoutManagerType = LayoutManagerType.LINEAR_LAYOUT_MANAGER;
        }

        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.scrollToPosition(scrollPosition);
    }

    /**
     * Generates Strings for RecyclerView's adapter. This data would usually come
     * from a local content provider or remote server.
     */
    private static void initDataset() {
        wDataset = new ArrayList<Watch>();
        pDataset = new ArrayList<Provider>();
        //for (int i = 0; i < DATASET_COUNT; i++) {
            //mDataset.add("Login item #" + i);
       // }
    }

}
