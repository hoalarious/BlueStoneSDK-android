package pickme.smartmozoexample;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import pickme.bluestone_sdk.BluestoneManager;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private static final String TAG = MainActivity.class.getClass().getSimpleName();

    private Handler beaconExpiry, rejectedBeaconExpiry, shakeExpiry;
    private boolean mScanning, hasRejected;

    private SharedPreferences mSharedPreferences;
    private TextView textViewUUID_RSSI, textViewUUID_ID, textViewBattery, textViewFirmware, textViewTime;
    private TextView textViewTitle, textViewPrice;
    private TextView textViewVersion, textViewLabelRange;

    protected PowerManager.WakeLock mWakeLock;

    private HashMap<String, Product> products = new HashMap<String, Product>();

    private HashMap<String, Date> BluestonesOut = new HashMap<String, Date>();

    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private float x1, x2, x3;
    private static final float ERROR = (float) 7.0;
    private boolean shaken, shakeEnabled, beepEnabled, sensorInit = false;

    private ToneGenerator toneGen1;

    private BluestoneManager mBluestoneManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        beaconExpiry = new Handler();
        rejectedBeaconExpiry = new Handler();
        shakeExpiry = new Handler();

        textViewUUID_RSSI = (TextView)findViewById(R.id.textViewUUID_RSSI);
        textViewUUID_ID  = (TextView)findViewById(R.id.textViewUUID_ID);
        textViewBattery = (TextView)findViewById(R.id.textViewBattery);
        textViewFirmware = (TextView)findViewById(R.id.textViewFirmware);
        textViewTime = (TextView)findViewById(R.id.textViewTime);
        textViewTitle  = (TextView)findViewById(R.id.textViewTitle);
        textViewPrice  = (TextView)findViewById(R.id.textViewPrice);
        textViewLabelRange = (TextView)findViewById(R.id.textViewLabelRange);

        textViewVersion  = (TextView)findViewById(R.id.textViewVersion);
        Date buildDate = new Date(BuildConfig.TIMESTAMP);
        textViewVersion.setText("Build date: " + buildDate.toString());

        PowerManager pm = (PowerManager) getApplicationContext().getSystemService(Context.POWER_SERVICE);
        mWakeLock = pm.newWakeLock((PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP), TAG);

        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        mSharedPreferences.registerOnSharedPreferenceChangeListener(prefernceListener);

        shakeEnabled = mSharedPreferences.getBoolean("enable_shake_to_pickup", false);
        beepEnabled = mSharedPreferences.getBoolean("enable_beep", true);

        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        if (shakeEnabled) mSensorManager.registerListener(this,mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);

        toneGen1 = new ToneGenerator(AudioManager.STREAM_MUSIC, 100);

        long SCAN_PERIOD = Long.parseLong(mSharedPreferences.getString("scan_timeout", "600000"));
        int rssiIgnore = Integer.parseInt(mSharedPreferences.getString("rssi_filter", "55"));
        int precision = Integer.parseInt(mSharedPreferences.getString("precision", "25"));
        mBluestoneManager = new BluestoneManager(this, rssiIgnore, SCAN_PERIOD);
        mBluestoneManager.setListener(mBlueStoneListener);

        getBlueStones();
    }

    private void getBluestoneID(final String mac){
        class GetBluestone extends AsyncTask<Void,Void,String>{

            @Override
            protected void onPostExecute(String s) {
                super.onPostExecute(s);
                try {
                    JSONObject jsonObject = new JSONObject(s);
                    JSONArray result = jsonObject.getJSONArray(ConfigSQL.TAG_JSON_ARRAY);
                    JSONObject c = result.getJSONObject(0);
                    String id = c.getString(ConfigSQL.TAG_ID);
                    String mac = c.getString(ConfigSQL.TAG_MAC);

                    Product p = products.get(mac);
                    if (p!=null) {
                        p.name = id;
                        textViewTitle.setText(id);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            protected String doInBackground(Void... params) {
                RequestHandlerSQL rh = new RequestHandlerSQL();
                String s = rh.sendGetRequestParam(ConfigSQL.URL_GET_BS, mac);
                return s;
            }
        }
        GetBluestone ge = new GetBluestone();
        ge.execute();
    }

    private void getBlueStones(){
        class GetBlueStones extends AsyncTask<Void,Void,String>{

            ProgressDialog loading;
            @Override
            protected void onPreExecute() {
                super.onPreExecute();
                loading = ProgressDialog.show(MainActivity.this,"Fetching Data","Wait...",false,false);
            }

            @Override
            protected void onPostExecute(String s) {
                super.onPostExecute(s);
                loading.dismiss();

                JSONObject jsonObject = null;
                ArrayList<HashMap<String, String>> list = new ArrayList<HashMap<String, String>>();
                try {
                    jsonObject = new JSONObject(s);
                    JSONArray result = jsonObject.getJSONArray(ConfigSQL.TAG_JSON_ARRAY);

                    for (int i = 0; i < result.length(); i++) {
                        JSONObject jo = result.getJSONObject(i);
                        String id = jo.getString(ConfigSQL.TAG_ID);
                        String mac = jo.getString(ConfigSQL.TAG_MAC);

                        Product e = new Product();
                        e.beacon = mac;
                        e.name = id;
                        e.image = "item_a";
                        products.put(e.beacon, e);
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            protected String doInBackground(Void... params) {
                RequestHandlerSQL rh = new RequestHandlerSQL();
                String s = rh.sendGetRequest(ConfigSQL.URL_GET_ALL_BS);
                return s;
            }
        }

        GetBlueStones gj = new GetBlueStones();
        gj.execute();
    }

    private BluestoneManager.BlueStoneListener mBlueStoneListener = new BluestoneManager.BlueStoneListener() {
        @Override
        public void onBlueStoneCallBack(String mac, boolean inRange, byte[] scanRecord, int rssi, String batt, String firmware, String days, String hours) {
            Product current = products.get(mac);
            if (current == null) {
                Product e = new Product();
                e.beacon = mac;
                e.image = "item_a";
                products.put(e.beacon, e);
                getBluestoneID(mac);
            }
            if (inRange) {
                if (beepEnabled) toneGen1.startTone(ToneGenerator.TONE_CDMA_PIP,150);
                if (current != null) textViewTitle.setText(current.name);

                if (textViewUUID_ID.getVisibility() == View.VISIBLE) {
                    String data = ByteArrayToString(scanRecord);
                    textViewUUID_RSSI.setText("RSSI: " + rssi + "dBm");
                    textViewUUID_ID.setText("ID: " + mac);
                    textViewBattery.setText("Battery: " + batt);
                    textViewFirmware.setText("Firmware version: " + firmware);
                    textViewTime.setText("Time alive: " + days.trim() + "d " + hours.trim() + "h");
                }
            } else{
                if (current != null) BluestonesOut.put(current.name, new Date());
                else BluestonesOut.put(mac, new Date());
                StringBuilder outList = new StringBuilder();
                for (String s: BluestonesOut.keySet()){
                    outList.append(s+"\n");
                }
                textViewLabelRange.setText("Outside range: \n" + outList.toString());
                if (!hasRejected) {
                    rejectedBeaconExpiry.removeCallbacksAndMessages(null);
                    rejectedBeaconExpiry.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            Iterator it = BluestonesOut.entrySet().iterator();
                            StringBuilder outList = new StringBuilder();
                            while (it.hasNext()) {
                                Map.Entry<String, Date> pair = (Map.Entry)it.next();
                                if (new Date().getTime() - pair.getValue().getTime() > 2000){
                                    it.remove();
                                } else outList.append(pair.getKey()+"\n");
                            }
                            textViewLabelRange.setText("Outside range: \n" + outList.toString());
                            if (BluestonesOut.isEmpty()) {
                                hasRejected = false;
                                rejectedBeaconExpiry.removeCallbacksAndMessages(null);
                            } else rejectedBeaconExpiry.postDelayed(this, 500);
                        }
                    }, 500);
                }
                hasRejected = true;
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