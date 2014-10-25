package com.example.xray.a2b;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.os.Vibrator;
import android.widget.Toast;

import com.gelo.gelosdk.GeLoBeaconManager;
import com.gelo.gelosdk.Model.Beacons.GeLoBeacon;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.Constants;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.DeckOfCardsEventListener;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.card.ListCard;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.card.NotificationTextCard;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.card.SimpleTextCard;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.remote.DeckOfCardsManager;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.remote.RemoteDeckOfCards;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.remote.RemoteDeckOfCardsException;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.remote.RemoteResourceStore;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.remote.RemoteToqNotification;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.resource.CardImage;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.resource.DeckOfCardsLauncherIcon;
import com.qualcomm.toq.smartwatch.api.v1.deckofcards.util.ParcelableUtil;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;


public class MainActivity extends Activity {

    GeLoBeaconManager ml;
    ArrayList<GeLoBeacon> beacons;
    Vibrator v;
    int minRssi = -100;
    ArrayList<Integer> visited = new ArrayList<Integer>();
    int bChoice = 0;

    private final static String PREFS_FILE= "prefs_file";
    private final static String DECK_OF_CARDS_KEY= "deck_of_cards_key";
    private final static String DECK_OF_CARDS_VERSION_KEY= "deck_of_cards_version_key";

    private MainActivityBroadcastReceiver toqReceiver;
    private DeckOfCardsManager mDeckOfCardsManager;
    private RemoteDeckOfCards mRemoteDeckOfCards;
    private RemoteResourceStore mRemoteResourceStore;
    private CardImage[] mCardImages;
    private DeckOfCardsEventListener deckOfCardsEventListener;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ml = GeLoBeaconManager.sharedInstance(getApplicationContext());
        ml.startScanningForBeacons();
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new UpdateBeacon(), 0, 2*400);
        v = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);
        mDeckOfCardsManager = DeckOfCardsManager.getInstance(getApplicationContext());
        toqReceiver = new MainActivityBroadcastReceiver();
        init();
    }

    protected void onStart(){
        super.onStart();

        Log.d(Constants.TAG, "ToqApiDemo.onStart");
        // If not connected, try to connect
        if (!mDeckOfCardsManager.isConnected()){
            try{
                mDeckOfCardsManager.connect();
            }
            catch (RemoteDeckOfCardsException e){
                e.printStackTrace();
            }
        }
    }

    public void sendNotification(int currBeacon) {
        Random randomGenerator = new Random();
        int cardNum = randomGenerator.nextInt(6);

        String[] message = new String[3];
        message[0] = "Proximity Alert";
        message[1] = "You are at beacon";
        message[2] = Integer.toString(currBeacon);
        // Create a NotificationTextCard
        NotificationTextCard notificationCard = new NotificationTextCard(System.currentTimeMillis(),
            "Notification Title", message);

        // Draw divider between lines of text
        notificationCard.setShowDivider(true);
        // Vibrate to alert user when showing the notification
        notificationCard.setVibeAlert(true);
        // Create a notification with the NotificationTextCard we made
        RemoteToqNotification notification = new RemoteToqNotification(this, notificationCard);

        try {
            // Send the notification
            mDeckOfCardsManager.sendNotification(notification);
            Toast.makeText(this, "Sent Notification", Toast.LENGTH_SHORT).show();
        } catch (RemoteDeckOfCardsException e) {
            e.printStackTrace();
            Toast.makeText(this, "Failed to send Notification", Toast.LENGTH_SHORT).show();
        }
    }

    public void installToq(View view) {
        boolean isInstalled = true;
        mRemoteDeckOfCards = createDeckOfCards();
        try {
            isInstalled = mDeckOfCardsManager.isInstalled();
        }
        catch (RemoteDeckOfCardsException e) {
            e.printStackTrace();
            Toast.makeText(this, "Error: Can't determine if app is installed", Toast.LENGTH_SHORT).show();
        }

        if (!isInstalled) {
            try {
                mDeckOfCardsManager.installDeckOfCards(mRemoteDeckOfCards, mRemoteResourceStore);
            } catch (RemoteDeckOfCardsException e) {
                e.printStackTrace();
                Toast.makeText(this, "Error: Cannot install application", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "App is already installed!", Toast.LENGTH_SHORT).show();
        }

        try{
            storeDeckOfCards();
        }
        catch (Exception e){
            e.printStackTrace();
        }
    }

    private void storeDeckOfCards() throws Exception{
        // Retrieve and hold the contents of PREFS_FILE, or create one when you retrieve an editor (SharedPreferences.edit())
        SharedPreferences prefs = getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE);
        // Create new editor with preferences above
        SharedPreferences.Editor editor = prefs.edit();
        // Store an encoded string of the deck of cards with key DECK_OF_CARDS_KEY
        editor.putString(DECK_OF_CARDS_KEY, ParcelableUtil.marshall(mRemoteDeckOfCards));
        // Store the version code with key DECK_OF_CARDS_VERSION_KEY
        editor.putInt(DECK_OF_CARDS_VERSION_KEY, Constants.VERSION_CODE);
        // Commit these changes
        editor.commit();
    }

    // Read an image from assets and return as a bitmap
    private Bitmap getBitmap(String fileName) throws Exception{

        try{
            InputStream is= getAssets().open(fileName);
            return BitmapFactory.decodeStream(is);
        }
        catch (Exception e){
            throw new Exception("An error occurred getting the bitmap: " + fileName, e);
        }
    }

    private void init(){

        // Create the resource store for icons and images
        mRemoteResourceStore= new RemoteResourceStore();

        DeckOfCardsLauncherIcon whiteIcon = null;
        DeckOfCardsLauncherIcon colorIcon = null;

        // Get the launcher icons
        try{
            whiteIcon= new DeckOfCardsLauncherIcon("white.launcher.icon", getBitmap("bw.png"), DeckOfCardsLauncherIcon.WHITE);
            colorIcon= new DeckOfCardsLauncherIcon("color.launcher.icon", getBitmap("color.png"), DeckOfCardsLauncherIcon.COLOR);
        }
        catch (Exception e){
            e.printStackTrace();
            System.out.println("Can't get launcher icon");
            return;
        }

//        mCardImages = new CardImage[6];
//        try{
//            mCardImages[0]= new CardImage("card.image.1", getBitmap("art_goldberg_toq.png"));
//            mCardImages[1]= new CardImage("card.image.2", getBitmap("jack_weinberg_toq.png"));
//            mCardImages[2]= new CardImage("card.image.3", getBitmap("jackie_goldberg_toq.png"));
//            mCardImages[3]= new CardImage("card.image.4", getBitmap("joan_baez_toq.png"));
//            mCardImages[4]= new CardImage("card.image.5", getBitmap("mario_savio_toq.png"));
//            mCardImages[5]= new CardImage("card.image.6", getBitmap("michael_rossman_toq.png"));
//        }
//        catch (Exception e){
//            e.printStackTrace();
//            System.out.println("Can't get picture icon");
//            Toast.makeText(this, "Hola", Toast.LENGTH_SHORT).show();
//            return;
//        }

        // Try to retrieve a stored deck of cards
        try {
            // If there is no stored deck of cards or it is unusable, then create new and store
            if ((mRemoteDeckOfCards = getStoredDeckOfCards()) == null){
                mRemoteDeckOfCards = createDeckOfCards();
                storeDeckOfCards();
            }
        }
        catch (Throwable th){
            th.printStackTrace();
            mRemoteDeckOfCards = null; // Reset to force recreate
        }

        // Make sure in usable state
        if (mRemoteDeckOfCards == null){
            mRemoteDeckOfCards = createDeckOfCards();
        }
        // Set the custom launcher icons, adding them to the resource store
        mRemoteDeckOfCards.setLauncherIcons(mRemoteResourceStore, new DeckOfCardsLauncherIcon[]{whiteIcon, colorIcon});
    }

    private RemoteDeckOfCards getStoredDeckOfCards() throws Exception{

        if (!isValidDeckOfCards()){
            Log.w(Constants.TAG, "Stored deck of cards not valid for this version of the demo, recreating...");
            return null;
        }

        SharedPreferences prefs= getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE);
        String deckOfCardsStr= prefs.getString(DECK_OF_CARDS_KEY, null);

        if (deckOfCardsStr == null){
            return null;
        }
        else{
            return ParcelableUtil.unmarshall(deckOfCardsStr, RemoteDeckOfCards.CREATOR);
        }

    }

    private boolean isValidDeckOfCards(){

        SharedPreferences prefs= getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE);
        // Return 0 if DECK_OF_CARDS_VERSION_KEY isn't found
        int deckOfCardsVersion= prefs.getInt(DECK_OF_CARDS_VERSION_KEY, 0);

        return deckOfCardsVersion >= Constants.VERSION_CODE;
    }

    // Create some cards with example content
    private RemoteDeckOfCards createDeckOfCards() {

        ListCard listCard= new ListCard();

        SimpleTextCard simpleTextCard= new SimpleTextCard("card0");
        simpleTextCard.setHeaderText("Art Goldberg");
        String[] messages = {"Draw 'Now'"};
        simpleTextCard.setMessageText(messages);
        simpleTextCard.setReceivingEvents(true);
        listCard.add(simpleTextCard);

        return new RemoteDeckOfCards(this, listCard);
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
                    sendNotification(bChoice);
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

    public void resetBeacons(View view) {
        visited.clear();
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
                    TextView rssiView = (TextView)findViewById(R.id.rssiLabel);
                    if (notZero) {
                        if (visited.size() < 3) {
                            currProximity(beacons);
                            rssiView.setText(Integer.toString(visited.size()));
                        } else {
                            rssiView.setText("found all beacons");
                        }

                    }
                    rView.setText(Integer.toString(bChoice));


                }
            });
        }
    }
}
