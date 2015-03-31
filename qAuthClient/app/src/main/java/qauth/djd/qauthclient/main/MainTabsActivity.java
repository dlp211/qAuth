package qauth.djd.qauthclient.main;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import qauth.djd.qauthclient.MainActivity;
import qauth.djd.qauthclient.R;
import qauth.djd.qauthclient.login.LoginActivity;
import qauth.djd.qauthclient.main.common.activities.SampleActivityBase;


public class MainTabsActivity extends SampleActivityBase {


    public static final String TAG = "MainTabsActivity";

    // Whether the Log Fragment is currently shown
    private boolean mLogShown;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //requestWindowFeature(Window.FEATURE_NO_TITLE);

        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        String packageName = intent.getStringExtra("packageName");

        if ( packageName != null ){
            Log.i("packageName", packageName);
            if ( packageName.equals("qauth.djd.dummyclient") ){
                startActivity(new Intent(this, MainActivity.class));
                finish();
                return;
            }
        }

        if ( ! loggedIn() ){
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_maintabs);

        if (savedInstanceState == null) {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            SlidingTabsColorsFragment fragment = new SlidingTabsColorsFragment();
            transaction.replace(R.id.sample_content_fragment, fragment);
            transaction.commit();
        }
    }

    private boolean loggedIn(){
        SharedPreferences prefs = getSharedPreferences(
                "qauth.djd.qauthclient",
                Context.MODE_PRIVATE);
        return prefs.getBoolean("loggedIn",false);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        //getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        /*MenuItem logToggle = menu.findItem(R.id.menu_toggle_log);
        logToggle.setVisible(findViewById(R.id.sample_output) instanceof ViewAnimator);
        logToggle.setTitle(mLogShown ? R.string.sample_hide_log : R.string.sample_show_log);*/

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        /*switch(item.getItemId()) {
            case R.id.menu_toggle_log:
                mLogShown = !mLogShown;
                ViewAnimator output = (ViewAnimator) findViewById(R.id.sample_output);
                if (mLogShown) {
                    output.setDisplayedChild(1);
                } else {
                    output.setDisplayedChild(0);
                }
                supportInvalidateOptionsMenu();
                return true;
        }*/
        return super.onOptionsItemSelected(item);
    }
}
