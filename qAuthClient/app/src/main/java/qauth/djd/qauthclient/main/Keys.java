package qauth.djd.qauthclient.main;

import java.io.Serializable;

/**
 * Created by David on 4/11/15.
 */
public class Keys implements Serializable {

    static final long serialVersionUID = 42L;
    public String privKey;
    public String pubKey;

    Keys(String privKey, String pubKey){
        this.privKey = privKey;
        this.pubKey = pubKey;
    }

}