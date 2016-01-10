package pickme.smartmozoexample;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Handler;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.Field;
import java.util.Date;
import java.util.HashMap;

import pickme.bluestone_sdk.BluestoneManager;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private static final String TAG = MainActivity.class.getClass().getSimpleName();

    private Handler beaconExpiry, rejectedBeaconExpiry, shakeExpiry;
    private boolean mScanning;

    private SharedPreferences mSharedPreferences;
    private TextView textViewUUID_RSSI, textViewUUID_ID;
    private TextView textViewTitle, textViewPrice;
    private TextView textViewVersion;

    private ImageView boundImage, boundImageRejected;

    protected PowerManager.WakeLock mWakeLock;

    private HashMap<String, Product> products = new HashMap<String, Product>();

    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private float x1, x2, x3;
    private static final float ERROR = (float) 7.0;
    private boolean shaken, shakeEnabled, beepEnabled, sensorInit = false;

    private ToneGenerator toneGen1;

    private BluestoneManager mBluestoneManager;

    private boolean imagePreviousFree = true;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        beaconExpiry = new Handler();
        rejectedBeaconExpiry = new Handler();
        shakeExpiry = new Handler();

        textViewUUID_RSSI = (TextView)findViewById(R.id.textViewUUID_RSSI);
        textViewUUID_ID  = (TextView)findViewById(R.id.textViewUUID_ID);
        textViewTitle  = (TextView)findViewById(R.id.textViewTitle);
        textViewPrice  = (TextView)findViewById(R.id.textViewPrice);

        textViewVersion  = (TextView)findViewById(R.id.textViewVersion);
        Date buildDate = new Date(BuildConfig.TIMESTAMP);
        textViewVersion.setText("Build date: " + buildDate.toString());

        boundImage=(ImageView)findViewById(R.id.imageViewBoundImage);
        boundImageRejected=(ImageView)findViewById(R.id.imageViewBoundImageRejected);
        boundImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                textViewUUID_RSSI.setText("Type: " + "N/A");
                textViewUUID_ID.setText("ID: " + 0);
                boundImage.setImageBitmap(null);
            }
        });

        PowerManager pm = (PowerManager) getApplicationContext().getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock((PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP), TAG);

        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        mSharedPreferences.registerOnSharedPreferenceChangeListener(prefernceListener);

        shakeEnabled = mSharedPreferences.getBoolean("enable_shake_to_pickup", false);
        beepEnabled = mSharedPreferences.getBoolean("enable_beep", true);

        createProducts();

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        if (shakeEnabled) mSensorManager.registerListener(this,mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);

        toneGen1 = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);

        long SCAN_PERIOD = Long.parseLong(mSharedPreferences.getString("scan_timeout", "600000"));
        int rssiIgnore = Integer.parseInt(mSharedPreferences.getString("rssi_filter", "55"));
        int precision = Integer.parseInt(mSharedPreferences.getString("precision", "25"));
        mBluestoneManager = new BluestoneManager(this, rssiIgnore, precision, SCAN_PERIOD);
        mBluestoneManager.setListener(mBlueStoneListener);
    }

    private BluestoneManager.BlueStoneListener mBlueStoneListener = new BluestoneManager.BlueStoneListener() {
        @Override
        public void onBlueStoneCallBack(String mac, boolean inRange, byte[] scanRecord, int rssi) {
            if (inRange) {
                displayProduct(mac);
                if (textViewUUID_ID.getVisibility() == View.VISIBLE) {
                    String data = ByteArrayToString(scanRecord);
                    textViewUUID_RSSI.setText("RSSI: " + rssi + "dBm");
                    textViewUUID_ID.setText("ID: " + mac);
                    //textViewUUID_ID.setText("Major: " + getMajorMinors(Arrays.copyOfRange(scanRecord, 25, 25 + 2)) + " Minor: " + getMajorMinors(Arrays.copyOfRange(scanRecord, 27, 27 + 2)));
                    clearTarget(3000);
                }
            } else if (!shaken) {
                if (imagePreviousFree) displayProductRejected(mac);
                return;
            } else {
                displayProduct(mac);
                if (textViewUUID_ID.getVisibility() == View.VISIBLE) {
                    String data = ByteArrayToString(scanRecord);
                    textViewUUID_RSSI.setText("RSSI: " + rssi + "dBm");
                    textViewUUID_ID.setText("ID: " + mac);
                    //textViewUUID_ID.setText("Major: " + getMajorMinors(Arrays.copyOfRange(scanRecord, 25, 25 + 2)) + " Minor: " + getMajorMinors(Arrays.copyOfRange(scanRecord, 27, 27 + 2)));
                    clearTarget(3000);
                }
                shakeExpiry.removeCallbacksAndMessages(null);
                shaken = false;
            }
        }

        @Override
        public void onScanStart() {
            mScanning = true;
            invalidateOptionsMenu();
        }

        @Override
        public void onScanStop() {
            mScanning = false;
            invalidateOptionsMenu();
        }
    };

    @Override
    public void onAccuracyChanged(Sensor arg0, int arg1) {

    }

    @Override
    public void onSensorChanged(SensorEvent e) {
        //Get x,y and z values
        float x,y,z;
        x = e.values[0];
        y = e.values[1];
        z = e.values[2];


        if (!sensorInit) {
            x1 = x;
            x2 = y;
            x3 = z;
            sensorInit = true;
        } else {

            float diffX = Math.abs(x1 - x);
            float diffY = Math.abs(x2 - y);
            float diffZ = Math.abs(x3 - z);

            //Handling ACCELEROMETER Noise
            if (diffX < ERROR) {

                diffX = (float) 0.0;
            }
            if (diffY < ERROR) {
                diffY = (float) 0.0;
            }
            if (diffZ < ERROR) {

                diffZ = (float) 0.0;
            }


            x1 = x;
            x2 = y;
            x3 = z;


            //Horizontal Shake Detected!
            if (diffX > diffY) {
                shaken = true;
                shakeExpiry.removeCallbacksAndMessages(null);
                shakeExpiry.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        shaken = false;
                    }
                }, 1000);
                Toast.makeText(MainActivity.this, "Shake Detected!", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        if (!mScanning) {
            menu.findItem(R.id.menu_stop).setVisible(false);
            menu.findItem(R.id.menu_scan).setVisible(true);
        } else {
            menu.findItem(R.id.menu_stop).setVisible(true);
            menu.findItem(R.id.menu_scan).setVisible(false);
        }
        if (textViewUUID_ID.getVisibility() == View.GONE) {
            menu.findItem(R.id.menu_bindings).setVisible(false);
            menu.findItem(R.id.menu_settings).setVisible(false);
            menu.findItem(R.id.menu_clear).setVisible(false);
            menu.findItem(R.id.menu_stop).setVisible(false);
        } else {
            menu.findItem(R.id.menu_bindings).setVisible(false);
            menu.findItem(R.id.menu_settings).setVisible(true);
            menu.findItem(R.id.menu_clear).setVisible(false);
            //menu.findItem(R.id.menu_admin).setVisible(false);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_scan:
                mBluestoneManager.startScan();
                break;
            case R.id.menu_stop:
                mBluestoneManager.stopScan();
                break;
            case R.id.menu_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                break;
            case R.id.menu_clear:
                String temp = mSharedPreferences.getString("scan_timeout","10000");
                mSharedPreferences.edit().clear().apply(); //Uncomment to clear sharedpreferences.
                mSharedPreferences.edit().putString("scan_timeout",temp).apply();
                break;
            case R.id.menu_admin:
                if (textViewUUID_ID.getVisibility() == View.GONE) {
                    textViewUUID_ID.setVisibility(View.VISIBLE);
                    textViewUUID_RSSI.setVisibility(View.VISIBLE);
                } else {
                    textViewUUID_ID.setVisibility(View.GONE);
                    textViewUUID_RSSI.setVisibility(View.GONE);
                }
                invalidateOptionsMenu();
                break;
        }
        return true;
    }

    SharedPreferences.OnSharedPreferenceChangeListener prefernceListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
        @Override
        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            if (key.equals("scan_timeout")){
                mBluestoneManager.updateScanTimeout(Long.parseLong(sharedPreferences.getString("scan_timeout", "600000")));
            }
            else if (key.equals("rssi_filter")){
                mBluestoneManager.updateRssiIgnore(Integer.parseInt(sharedPreferences.getString("rssi_filter", "55")));
            }
            else if (key.equals("precision")){
                mBluestoneManager.updatePrecision(Integer.parseInt(sharedPreferences.getString("precision","25")));
            }
            else if (key.equals("enable_shake_to_pickup")) {
                shakeEnabled = mSharedPreferences.getBoolean("enable_shake_to_pickup", false);
                updateShakePreference();
            }
            else if (key.equals("enable_beep")){
                beepEnabled = mSharedPreferences.getBoolean("enable_beep", true);
            }
        }
    };

    private void updateShakePreference(){
        if (shakeEnabled)mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        else mSensorManager.unregisterListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!mWakeLock.isHeld()) mWakeLock.acquire();
        if (shakeEnabled) mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mWakeLock.isHeld()) mWakeLock.release();
        if (shakeEnabled) mSensorManager.unregisterListener(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        mBluestoneManager.stopScan();
        mSharedPreferences.unregisterOnSharedPreferenceChangeListener(prefernceListener);
        beaconExpiry.removeCallbacksAndMessages(null);

        super.onDestroy();
    }

    private void createProducts(){
        Product e = new Product();
        e.name = "New Balance SZ";
        e.beacon = "CE:D2:F4:03:73:8B ";
        e.code = "9382764293846";
        e.price = 60;
        e.colours = "red,blue";
        e.sizes = "6,7";
        e.comments = "Wow this is so cool,What is that?,I want this for my birthday!!!";
        e.image = "item_a";
        products.put(e.beacon, e);

        e = new Product();
        e.name = "361 Neon Glow";
        e.beacon = "0 2 0 0 ";
        e.code = "9382764293846";
        e.price = 60;
        e.colours = "red,green";
        e.sizes = "5,6,7";
        e.comments = "Wow this is so cool,What is that?,I want this for my birthday!!!";
        e.image = "item_b";
        products.put(e.beacon, e);

        e = new Product();
        e.name = "361 Minnie Polka";
        e.beacon = "0 3 0 0 ";
        e.code = "9382764293846";
        e.price = 60;
        e.colours = "red,blue,green";
        e.sizes = "4,5,6";
        e.comments = "Wow this is so cool,What is that?,I want this for my birthday!!!";
        e.image = "item_c";
        products.put(e.beacon, e);

        e = new Product();
        e.name = "Super fabulous sneakers";
        e.beacon = "0 4 0 0 ";
        e.code = "9382764293846";
        e.price = 60;
        e.colours = "blue,green";
        e.sizes = "6,7,8";
        e.comments = "Wow this is so cool,What is that?,I want this for my birthday!!!";
        e.image = "item_d";
        products.put(e.beacon, e);

        e = new Product();
        e.name = "Super fabulous sneakers";
        e.beacon = "0 5 0 0 ";
        e.code = "9382764293846";
        e.price = 60;
        e.colours = "blue,green";
        e.sizes = "6,7,8";
        e.comments = "Wow this is so cool,What is that?,I want this for my birthday!!!";
        e.image = "item_e";
        products.put(e.beacon, e);

    }

    private void displayProduct(String beaconID){
        Product e = products.get(beaconID);
        Bitmap bp;
        if (e==null) {
            textViewTitle.setText("Product not in database");
            textViewPrice.setText("$N/A");
            LinearLayout layout = (LinearLayout)findViewById(R.id.layoutColors);
            if((layout).getChildCount() > 0) (layout).removeAllViews();
            layout = (LinearLayout)findViewById(R.id.layoutSizes);
            if((layout).getChildCount() > 0) (layout).removeAllViews();
            bp = BitmapFactory.decodeResource(getResources(), getResId("none", R.raw.class));
        } else {
            textViewTitle.setText(e.name);
            bp = BitmapFactory.decodeResource(getResources(), getResId(e.image, R.raw.class));
            textViewPrice.setText("$"+e.price);
            String[] colors = e.colours.split("[,]");
            LinearLayout layout = (LinearLayout)findViewById(R.id.layoutColors);
            if((layout).getChildCount() > 0) (layout).removeAllViews();
            for (int i = 0; i < colors.length;i++) {
                Button colorBox = new Button(this);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                params.setMargins(2, 2, 2, 2);
                colorBox.setLayoutParams(params);
                colorBox.setHeight(20);
                colorBox.setWidth(20);
                colorBox.setBackgroundResource(getResId(colors[i], R.color.class));
                // Adds the view to the layout
                layout.addView(colorBox);
            }
            String[] sizes = e.sizes.split("[,]");
            layout = (LinearLayout)findViewById(R.id.layoutSizes);
            if((layout).getChildCount() > 0) (layout).removeAllViews();
            for (int i = 0; i < sizes.length;i++) {
                Button sizeBox = new Button(this);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
                params.setMargins(2, 2, 2, 2);
                sizeBox.setLayoutParams(params);
                sizeBox.setHeight(20);
                sizeBox.setWidth(20);
                sizeBox.setText(sizes[i]);
                sizeBox.setBackgroundResource(R.drawable.size_shape);
                if (i==0) sizeBox.setEnabled(false);
                // Adds the view to the layout
                layout.addView(sizeBox);
            }
        }

        bp = Bitmap.createScaledBitmap(bp, 400, 400, true);
        boundImage.setImageBitmap(bp);
        if (beepEnabled) toneGen1.startTone(ToneGenerator.TONE_CDMA_PIP,150);

    }


    private void displayProductRejected(String beaconID){
        Product e = products.get(beaconID);

        if (e==null) {
            Bitmap bp = BitmapFactory.decodeResource(getResources(), getResId("none", R.raw.class));
            bp = Bitmap.createScaledBitmap(bp, 200, 200, true);
            boundImageRejected.setImageBitmap(bp);
        } else {
            Bitmap bp = BitmapFactory.decodeResource(getResources(), getResId(e.image, R.raw.class));
            bp = Bitmap.createScaledBitmap(bp, 200, 200, true);
            boundImageRejected.setImageBitmap(bp);
        }
        imagePreviousFree = false;
        rejectedBeaconExpiry.removeCallbacksAndMessages(null);
        rejectedBeaconExpiry.postDelayed(new Runnable() {
            @Override
            public void run() {
                boundImageRejected.setImageBitmap(null);
                imagePreviousFree = true;
            }
        }, 500);
    }

    public static int getResId(String resName, Class<?> c) {

        try {
            Field idField = c.getDeclaredField(resName);
            return idField.getInt(idField);
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    private void clearTarget(int delay){
        beaconExpiry.removeCallbacksAndMessages(null);
        beaconExpiry.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (textViewUUID_ID.getVisibility() == View.VISIBLE) {
                    textViewUUID_RSSI.setText("RSSI: " + "N/A");
                    textViewUUID_ID.setText("ID: " + 0);
                    boundImage.setImageBitmap(null);
                    //currentProduct = null;
                }
            }
        }, delay);
    }

    public String ByteArrayToString(byte[] ba)
    {
        StringBuilder hex = new StringBuilder(ba.length * 2);
        for (byte b : ba)
            hex.append(b + " ");
        return hex.toString();
    }

    public String getMajorMinors(byte[] ba)
    {
        if (ba.length < 2) return "Invalid byte length";
        int val = ((ba[0] & 0xff) << 8) | (ba[1] & 0xff);
        return Integer.toString(val);
    }

}