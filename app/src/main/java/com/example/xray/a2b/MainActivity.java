package com.example.xray.a2b;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.os.Vibrator;

import com.gelo.gelosdk.GeLoBeaconManager;
import com.gelo.gelosdk.Model.Beacons.GeLoBeacon;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;


public class MainActivity extends Activity {

    GeLoBeaconManager ml;
    ArrayList<GeLoBeacon> beacons;
    Vibrator v;
    int minRssi = -100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ml = GeLoBeaconManager.sharedInstance(getApplicationContext());
        ml.startScanningForBeacons();
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new UpdateBeacon(), 0, 1*400);
        v = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);
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

    class UpdateBeacon extends TimerTask {
        @Override
        public void run() {
            MainActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    beacons = ml.getKnownBeacons();
                    int rssi = 0;
                    if (beacons.isEmpty() != true) {
                        for (GeLoBeacon i : beacons) {
                            if (i.getBeaconId() == 551) {
                                rssi = i.getSignalStregth();
                            }
                        }
                    }
                    if (minRssi < rssi) {
                        minRssi = rssi;
                        v.vibrate(400);
                    }
                    TextView rssiView = (TextView)findViewById(R.id.rssiLabel);
                    rssiView.setText(Integer.toString(rssi));
                }
            });
        }
    }
}
