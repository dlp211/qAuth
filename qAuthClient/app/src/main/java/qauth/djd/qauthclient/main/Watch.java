package qauth.djd.qauthclient.main;

import java.io.Serializable;

/**
 * Created by David on 3/6/15.
 */
public class Watch implements Serializable {

    static final long serialVersionUID = 42L;
    public String deviceId;
    public String model;
    public String privKey;
    public String pubKey;

    Watch(String deviceId, String model, String privKey, String pubKey){
        this.deviceId = deviceId;
        this.model = model;
        this.privKey = privKey;
        this.pubKey = pubKey;
    }

    @Override
    public int hashCode() {
        return deviceId.hashCode();
    }

    @Override
    public boolean equals(Object o){
        if(o instanceof Watch){
            String toCompare = ((Watch) o).deviceId;
            return deviceId.equals(toCompare);
        }
        return false;
    }

}