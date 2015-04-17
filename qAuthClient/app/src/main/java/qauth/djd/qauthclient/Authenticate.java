package qauth.djd.qauthclient;

import android.content.Context;
import android.os.Bundle;
import android.util.Base64;
import android.util.Log;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import org.spongycastle.util.encoders.Hex;

import java.io.ByteArrayInputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.Signature;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAKeyGenParameterSpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Collection;
import java.util.HashSet;

import javax.crypto.Cipher;

import qauth.djd.qauthclient.main.MainTabsActivity;
import qauth.djd.qauthclient.main.Watch;

/**
 * Created by David on 3/30/15.
 */
public class Authenticate implements MessageApi.MessageListener, com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks {

    public static Context ctx;
    public static GoogleApiClient mGoogleApiClient;
    public static String tempWatchPrivKey;
    public static String tempWatchPubKey;


    Authenticate(Context ctx) {
        if ( this.ctx == null ) {
            this.ctx = ctx;
            mGoogleApiClient = new GoogleApiClient.Builder(ctx)
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
        }
    }

    //server's public key
    static String PUBKEY = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDhdTU+30M3X54TaEL2iiyfW/Is\n" +
            "empfRXwLAySf1flat5VJamHg7kIDOBmFEDLW9vNZ5H9C2Gt/bMsxjH6auZxr9lLM\n" +
            "cI+Ctfdh6eoACPbvKmBASUjDxml3Rd/vJFiMh9SWUYKevjnf2uqg8iG+RCvxUyfS\n" +
            "/6Dy30lQIm2bTF91UQIDAQAB";

    static {
        Security.insertProviderAt(new org.spongycastle.jce.provider.BouncyCastleProvider(), 1);
    }

    public static String encrypt(String message) throws Exception {
        Cipher rsaCipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA1AndMGF1Padding", "SC");
        rsaCipher.init(Cipher.ENCRYPT_MODE, getPubKey());
        return Hex.toHexString(rsaCipher.doFinal(message.getBytes()));
    }

    public static String decrypt(String message) throws Exception {
        byte[] msg = Hex.decode(message);
        Cipher rsaCipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA1AndMGF1Padding", "SC");
        rsaCipher.init(Cipher.DECRYPT_MODE, getPrivKey());
        return new String(rsaCipher.doFinal(msg), "UTF-8");
    }

    public static byte[] hash(String message) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-1", "SC");
        byte[] passByte = message.getBytes();
        passByte = md.digest(passByte);
        Log.i("hash test", "passByte: " + Hex.toHexString(passByte) );
        return passByte;
    }

    public static String sign(byte[] message) throws Exception {
        Signature signature = Signature.getInstance("SHA1withRSA", "SC");
        signature.initSign(getPrivKey());
        signature.update(message);
        return Hex.toHexString(signature.sign());
    }

    public static boolean verifySignature(String sig, byte[] hash) throws Exception {

        //Authenticate.verifySignature( hash, Authenticate.hash(packageName + deviceId + nonceEnc) )

        Signature verifier = Signature.getInstance("SHA1withRSA", "SC");
        verifier.initVerify(getPubKey());
        verifier.update(hash);

        Log.i("verifySignature", "sig: " + sig);
        Log.i("verifySignature", verifier.toString() );
        Log.i("verifySignature", getPubKey().getAlgorithm() );

        String decryptedHash = decrypt(sig);
        Log.i("verifySignature", "decryptedHash: " + decryptedHash);

        return verifier.verify( Hex.decode(sig) );
    }

    public static String hashAndSign(String message) throws Exception {
        return sign(hash(message));
    }

    public static RSAPrivateKey getPrivKey() throws Exception {

        RSAPrivateKey pk = null;

        if ( tempWatchPrivKey == null ){
            getWatch();

            Thread.sleep(3000);

            Log.i("Authenticate", "tempWatchPrivKey after sleep: " + tempWatchPrivKey);

            byte[] clear = Base64.decode(tempWatchPrivKey, Base64.DEFAULT);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(clear);
            KeyFactory kf = KeyFactory.getInstance("RSA", "SC");
            pk = (RSAPrivateKey) kf.generatePrivate(keySpec);

        } else {

            byte[] clear = Base64.decode(tempWatchPrivKey, Base64.DEFAULT);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(clear);
            KeyFactory kf = KeyFactory.getInstance("RSA", "SC");
            pk = (RSAPrivateKey) kf.generatePrivate(keySpec);

        }
        return pk;
    }

    public static RSAPublicKey getPubKey() throws Exception {
        KeyFactory kf = KeyFactory.getInstance("RSA", "SC");
        byte[] publicKeyBytes = Base64.decode(PUBKEY.getBytes("UTF-8"), Base64.DEFAULT);
        X509EncodedKeySpec x = new X509EncodedKeySpec(publicKeyBytes);
        return (RSAPublicKey)kf.generatePublic(x);
    }

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

            MainTabsActivity.pubKey = (RSAPublicKey) pair.getPublic();
            MainTabsActivity.privKey = (RSAPrivateKey) pair.getPrivate();

            //SharedPreferences prefs = ctx.getSharedPreferences("qauth.djd.qauthclient", Context.MODE_PRIVATE);

            String bytes = Base64.encodeToString(MainTabsActivity.privKey.getEncoded(), Base64.DEFAULT);
            //prefs.edit().putString("privKey", bytes ).commit();

            String pk = Base64.encodeToString(MainTabsActivity.pubKey.getEncoded(), Base64.DEFAULT);
            //prefs.edit().putString("pubKey", pk ).commit();

            // { email, password, bluetoothId, N, E }
            // string N, int E
            String N = MainTabsActivity.pubKey.getModulus().toString(10); //N
            int E = MainTabsActivity.pubKey.getPublicExponent().intValue(); //E

            //ctx.startActivity(new Intent(ctx, MainTabsActivity.class));

            //new RegisterBluetooth("dlp", "password", "1001", N, E).execute();

        }
        catch (Exception e) {
            Log.d("RSA", e.toString());
        }
    }

    private static Collection<String> getNodes() {
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

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        Log.i("qAuthWear", "on message received23123123!!!!!");

        if (messageEvent.getPath().equals("GET_WATCH")) {

            //Log.i("GET_WATCH", "HAS BEEN CALLED.");

            Watch watch = null;
            ByteArrayInputStream bis = new ByteArrayInputStream(messageEvent.getData());
            ObjectInput in = null;
            try { in = new ObjectInputStream(bis); } catch (Exception e) { Log.i("exception1", "e: " + e); }
            try { watch = (Watch) in.readObject(); } catch (Exception e) { Log.i("exception2", "e: " + e); }

            if ( watch != null ){
                tempWatchPrivKey = watch.privKey;
                tempWatchPubKey = watch.pubKey;
            }

        }
    }

    public static void getWatch() {
        //Log.i("Authenticate", "getWatch() being called");
        for ( String nodeId : getNodes() ){
            Wearable.MessageApi.sendMessage(
                    mGoogleApiClient, nodeId, "GET_WATCH", new byte[0]).setResultCallback(
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
    }

}
