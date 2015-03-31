package qauth.djd.qauthclient;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import android.util.Log;

import org.spongycastle.util.encoders.Hex;

import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.Signature;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;

import javax.crypto.Cipher;

/**
 * Created by David on 3/30/15.
 */
public class Authenticate {

    //server's public key
    static String PUBKEY = "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDhdTU+30M3X54TaEL2iiyfW/Is\n" +
            "empfRXwLAySf1flat5VJamHg7kIDOBmFEDLW9vNZ5H9C2Gt/bMsxjH6auZxr9lLM\n" +
            "cI+Ctfdh6eoACPbvKmBASUjDxml3Rd/vJFiMh9SWUYKevjnf2uqg8iG+RCvxUyfS\n" +
            "/6Dy30lQIm2bTF91UQIDAQAB";

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
        SharedPreferences prefs = MainActivity.ctx.getSharedPreferences("qauth.djd.qauthclient", Context.MODE_PRIVATE);
        byte[] clear = Base64.decode(prefs.getString("privKey", "null"), Base64.DEFAULT);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(clear);
        KeyFactory kf = KeyFactory.getInstance("RSA", "SC");
        RSAPrivateKey pk = (RSAPrivateKey) kf.generatePrivate(keySpec);
        return pk;
    }

    public static RSAPublicKey getPubKey() throws Exception {
        KeyFactory kf = KeyFactory.getInstance("RSA", "SC");
        byte[] publicKeyBytes = Base64.decode(PUBKEY.getBytes("UTF-8"), Base64.DEFAULT);
        X509EncodedKeySpec x = new X509EncodedKeySpec(publicKeyBytes);
        return (RSAPublicKey)kf.generatePublic(x);
    }

}
