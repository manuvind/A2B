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
    ArrayList<Integer> visited = new ArrayList<Integer>();
    int bChoice = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ml = GeLoBeaconManager.sharedInstance(getApplicationContext());
        ml.startScanningForBeacons();
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new UpdateBeacon(), 0, 2*400);
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

    public void currProximity(ArrayList<GeLoBeacon> kBeacons) {
        if (bChoice == 0) {
            chooseNearest(kBeacons);
        }
        for (GeLoBeacon i : kBeacons) {
            if (i.getBeaconId() == bChoice) {
                int rssi = i.getSignalStregth();
                if (minRssi < rssi) {
                    minRssi = rssi;
                    v.vibrate(400);
                    TextView rssiText = (TextView)findViewById(R.id.textView);
                    rssiText.setText(Integer.toString(minRssi));
                }

                if (minRssi > -58) {
                    visited.add(i.getBeaconId());
                    chooseNearest(kBeacons);
                }
            }
        }

    }

    public void chooseNearest(ArrayList<GeLoBeacon> kBeacons) {
        int max = -100;
        int curr_id = 0;
        for (GeLoBeacon i : kBeacons) {
            if (!visited.contains(i.getBeaconId())) {
                if (i.getSignalStregth() > max) {
                    max = i.getSignalStregth();
                    curr_id = i.getBeaconId();
                }
            }
        }
        minRssi = max;
        bChoice = curr_id;
    }

    class UpdateBeacon extends TimerTask {
        @Override
        public void run() {
            MainActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    beacons = ml.getKnownBeacons();
                    int rssi = 0;
                    boolean notZero = true;
                    if (beacons.isEmpty() != true) {
                        for (GeLoBeacon i : beacons) {
                            if (i.getSignalStregth() == 0) {
                                notZero = false;
                            }
                        }
                    }
                    TextView rView = (TextView)findViewById(R.id.near);
                    if (notZero) {
                        if (visited.size() < 3) {
                            currProximity(beacons);
                        } else {
                            rView.setText("found all beacons");
                        }

                    }
//                    if (rssi != 0) {
//                        if (minRssi < rssi) {
//                            minRssi = rssi;
//                            v.vibrate(400);
//                            TextView rssiText = (TextView)findViewById(R.id.textView);
//                            rssiText.setText(Integer.toString(minRssi));
//                        }
//                    }
                    //near
                    rView.setText(Integer.toString(bChoice));

                    TextView rssiView = (TextView)findViewById(R.id.rssiLabel);
                    rssiView.setText(Integer.toString(rssi));
                }
            });
        }
    }
}
