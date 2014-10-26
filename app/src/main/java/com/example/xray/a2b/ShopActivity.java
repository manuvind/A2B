package com.example.xray.a2b;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.Vibrator;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;


import com.gelo.gelosdk.GeLoBeaconManager;
import com.gelo.gelosdk.Model.Beacons.GeLoBeacon;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
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

import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;


public class ShopActivity extends Activity {

    GeLoBeaconManager ml;
    ArrayList<GeLoBeacon> beacons;
    Vibrator v;
    int minRssi = -100;
    ArrayList<Integer> visited = new ArrayList<Integer>();
    int bChoice = 0;
    static final int REQUEST_IMAGE_CAPTURE = 1;


    private static HashMap<String, Integer> foodMap = new HashMap<String, Integer>();
    private static ArrayList<String> shop_list;
    private final static String PREFS_FILE= "prefs_file";
    private final static String DECK_OF_CARDS_KEY= "deck_of_cards_key";
    private final static String DECK_OF_CARDS_VERSION_KEY= "deck_of_cards_version_key";

    private MainActivityBroadcastReceiver toqReceiver;
    private DeckOfCardsManager mDeckOfCardsManager;
    private RemoteDeckOfCards mRemoteDeckOfCards;
    private RemoteResourceStore mRemoteResourceStore;
    private CardImage[] mCardImages;
    private DeckOfCardsEventListener deckOfCardsEventListener;
    ArrayAdapter adapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shop);
        ml = GeLoBeaconManager.sharedInstance(getApplicationContext());
        ml.startScanningForBeacons();
        Timer timer = new Timer();

        foodMap.put("Bananas",554);
        foodMap.put("Milk",551);
        foodMap.put("Eggs",680);
        Intent intent = getIntent();
        shop_list = intent.getStringArrayListExtra(MainActivity.EXTRA_MESSAGE);

        final ListView listview = (ListView) findViewById(R.id.shopList);
        adapter = new ArrayAdapter(this,
                android.R.layout.simple_list_item_1, shop_list);


        listview.setAdapter(adapter);
        listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, final View view,
                                    int position, long id) {
                final String item = (String) parent.getItemAtPosition(position);
                view.animate().setDuration(1000).alpha(0)
                        .withEndAction(new Runnable() {
                            @Override
                            public void run() {
                                shop_list.remove(item);
                                adapter.notifyDataSetChanged();
                                view.setAlpha(1);
                            }
                        });
            }

        });
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

    public void sendNotification(int currBeacon, String foundItem) {
        Random randomGenerator = new Random();
        int cardNum = randomGenerator.nextInt(6);

        String[] message = new String[3];
        message[0] = "Proximity Alert";
        message[1] = "You are at beacon: " + currBeacon;
        message[2] = "You have found " + foundItem;
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
                    String foundItem = shop_list.remove(0);
                    adapter.notifyDataSetChanged();
                    sendNotification(bChoice,foundItem);
                    ocrImage();
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

    public void ocrImage() {
        dispatchTakePictureIntent();
    }

    public void resetBeacons(View view) {
        visited.clear();
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");
            ImageView imageView = (ImageView) findViewById(R.id.imageView);
            imageView.setImageBitmap(imageBitmap);
            processPictureWhenReady(imageBitmap);
        }
    }

    public void toastResult(String result){
        Toast.makeText(getApplicationContext(), result,
                Toast.LENGTH_LONG).show();
    }

    private static class IODOCRTask extends AsyncTask<File,Void,String> {
        private ShopActivity activity;
        protected IODOCRTask(ShopActivity activity) {
            this.activity = activity;
        }
        @Override
        protected String doInBackground(File... params) {
            File file = params[0];
            String result="";
            try {
                HttpResponse<JsonNode> response = Unirest
                        .post("http://api.idolondemand.com/1/api/sync/ocrdocument/v1")
                        .field("file",file)
                        .field("mode", "scene_photo")
                        .field("apikey","<yourapikey>")
                        .asJson();
                JSONObject textblock =(JSONObject) response.getBody().getObject().getJSONArray("text_block").get(0);
                result=textblock.getString("text");
            } catch (Exception e) {
// keeping error handling simple
                e.printStackTrace();
            }
            Log.i("MARTIN"+file.getName(),result);
            return result;
        }
        @Override
        protected void onPostExecute(String result) {
// TODO Auto-generated method stub
            activity.toastResult(result);
        }
    }

    public void processPictureWhenReady(Bitmap bm) {
        Toast.makeText(getApplicationContext(), "FILE IS WRITTEN",
                Toast.LENGTH_SHORT).show();
        File dir = Environment
                .getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
        Bitmap out = Bitmap.createScaledBitmap(bm, 640, 960, false);
        File file = new File(dir, "resize.png");
        FileOutputStream fOut;
        try {
            fOut = new FileOutputStream(file);
            out.compress(Bitmap.CompressFormat.PNG, 100, fOut);
            fOut.flush();
            fOut.close();
            new IODOCRTask(this).execute(file);
        } catch (Exception e) { // TODO
            e.printStackTrace();
        }
    }

    class UpdateBeacon extends TimerTask {
        @Override
        public void run() {
            ShopActivity.this.runOnUiThread(new Runnable() {
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
                    TextView rView = (TextView) findViewById(R.id.near);
                    TextView rssiView = (TextView) findViewById(R.id.rssiLabel);
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
