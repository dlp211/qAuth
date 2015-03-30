package qauth.djd.qauthclient.main;

import java.io.Serializable;

/**
 * Created by David on 3/6/15.
 */
public class Watch implements Serializable {

    static final long serialVersionUID = 42L;
    public String deviceId;
    public String model;

    public Watch(String deviceId, String model){
        this.deviceId = deviceId;
        this.model = model;
    }

}
