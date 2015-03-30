package qauth.djd.qauthclient.main;

/**
 * Created by David on 3/10/15.
 */
public class Provider {

    public String appName;
    public String packageName;

    Provider(String appName, String packageName){
        this.appName = appName;
        this.packageName = packageName;
    }

}