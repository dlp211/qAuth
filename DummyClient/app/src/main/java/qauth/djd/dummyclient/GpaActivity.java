package qauth.djd.dummyclient;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;


public class GpaActivity extends ActionBarActivity {

    public static TextView gpaTV;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gpa);

        Intent intent = getIntent();
        String balance = intent.getStringExtra("balance");
        String level = intent.getStringExtra("level");

        gpaTV = (TextView) findViewById(R.id.gpaTextView);
        Button withdrawal = (Button) findViewById(R.id.withdrawal);
        Button deposit = (Button) findViewById(R.id.deposit);

        if ( balance != null ){

            gpaTV.setText("balance: " + balance);

            if ( level.equals("0") ){
                withdrawal.setEnabled(true);
                deposit.setEnabled(true);
            } else {
                withdrawal.setEnabled(false);
                deposit.setEnabled(false);
            }

        }

        registerReceiver(remoteFinish, new IntentFilter("remoteFinish"));

    }

    private final BroadcastReceiver remoteFinish = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            finish();
        }
    };

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(remoteFinish);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_gpa, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void withdrawal(View v){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Withdrawal Amount");

        final EditText input = new EditText(this);

        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        builder.setView(input);

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Log.i("test", input.getText().toString());

                SharedPreferences prefs = getSharedPreferences("qauth.djd.dummyclient", Context.MODE_PRIVATE);
                String sessionid = prefs.getString("sessionid", "null session id");

                new AccountUpdate( sessionid, Integer.valueOf(input.getText().toString()) * -1 ).execute();

            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    public void deposit(View v){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Deposit Amount");

        final EditText input = new EditText(this);

        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        builder.setView(input);

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                Log.i("test", input.getText().toString());

                SharedPreferences prefs = getSharedPreferences("qauth.djd.dummyclient", Context.MODE_PRIVATE);
                String sessionid = prefs.getString("sessionid", "null session id");

                new AccountUpdate( sessionid, Integer.valueOf(input.getText().toString()) ).execute();

            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }
}
