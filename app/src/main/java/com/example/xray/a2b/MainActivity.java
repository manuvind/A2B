package com.example.xray.a2b;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;
import android.os.Vibrator;
import android.util.Log;


import com.gelo.gelosdk.GeLoBeaconManager;
import com.gelo.gelosdk.Model.Beacons.GeLoBeacon;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends Activity{
    public final static String EXTRA_MESSAGE = "com.example.myfirstapp.MESSAGE";
    static final int CONTACT_REQUEST = 1;  // The request code
    static ArrayList<String> shop_list;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final String TAG = MainActivity.class.getSimpleName();
        setContentView(R.layout.activity_main);
        shop_list = new ArrayList<String>();
        String[] values = new String[] { "Bananas", "Milk", "Eggs"};
        for (int i = 0; i < values.length; ++i) {
            shop_list.add(values[i]);
        }
        Log.d(TAG, "Creating Layout");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        ArrayList<String> shop_list = data.getStringArrayListExtra("SHOP_LIST");
        TextView rssiView = (TextView)findViewById(R.id.rssiLabel);
        rssiView.setText("ENTERED" );
        // Check which request we're responding to
        if (requestCode == 1) {
            // Make sure the request was successful
            if (resultCode == RESULT_OK) {
                rssiView.setText("SHOP_LEN: " + shop_list.size());
                // The user picked a contact.
                // The Intent's data Uri identifies which contact was selected.

                // Do something with the contact here (bigger example below)
            }
        }
    }
    /** Called when the user clicks the create List button */
    public void createList(View view) {
        Intent intent = new Intent(this, ListActivity.class);
        intent.putStringArrayListExtra(EXTRA_MESSAGE, shop_list);
        startActivityForResult(intent, CONTACT_REQUEST);
    }

    /** Called when the user clicks the create List button */
    public void startShop(View view) {
        Intent intent = new Intent(this, ShopActivity.class);
        intent.putStringArrayListExtra(EXTRA_MESSAGE, shop_list);
        startActivityForResult(intent, CONTACT_REQUEST);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }


}
