package pickme.smartmozoexample;

import android.app.ProgressDialog;
import android.bluetooth.BluetoothDevice;
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

import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Switch;
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

import pickme.bluestone_sdk.BlueStone;
import pickme.bluestone_sdk.BluestoneManager;
import pickme.bluestone_sdk.ConfigBS;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private static final String TAG = MainActivity.class.getClass().getSimpleName();

    private Handler beaconExpiry, rejectedBeaconExpiry, shakeExpiry;
    private boolean mScanning, hasRejected;

    private SharedPreferences mSharedPreferences;
    private TextView textViewUUID_RSSI, textViewUUID_ID, textViewBattery, textViewFirmware, textViewTime;
    private TextView textViewTitle, textViewSeekBarValue, textViewUUID, textViewMajor, textViewMinor;
    private TextView textViewVersion, textViewLabelRange;
    private TextView textViewSeekBarIntervalValue, textViewSeekBarIntervalPowerValue, textViewSeekBarMotionPowerValue;
    private SeekBar seekbarRSSI, seekbarInterval, seekbarIntervalPower, seekbarMotionPower;
    private Switch switchSound, switchBS_led, switchBS_motion, switchBS_motion_led;

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

    private BluetoothDevice lastSeenDevice;

    // TODO: 22/03/2016
    // AUTO DFU
    // Demo application
    // Binding

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        beaconExpiry = new Handler();
        rejectedBeaconExpiry = new Handler();
        shakeExpiry = new Handler();

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

        int rssiIgnore = Integer.parseInt(mSharedPreferences.getString("rssi_filter", "55"));
        mBluestoneManager = new BluestoneManager(this);
        mBluestoneManager.setListener(mBlueStoneListener);
        mBluestoneManager.updateRange(-rssiIgnore);
        setGUI();
        getBlueStones();

    }

    private void setGUI(){
        textViewUUID_RSSI = (TextView)findViewById(R.id.textViewUUID_RSSI);
        textViewUUID_ID  = (TextView)findViewById(R.id.textViewUUID_ID);
        textViewBattery = (TextView)findViewById(R.id.textViewBattery);
        textViewFirmware = (TextView)findViewById(R.id.textViewFirmware);
        textViewTime = (TextView)findViewById(R.id.textViewTime);
        textViewTitle  = (TextView)findViewById(R.id.textViewTitle);
        textViewLabelRange = (TextView)findViewById(R.id.textViewLabelRange);
        textViewSeekBarValue = (TextView)findViewById(R.id.textViewSeekBarValue);
        textViewSeekBarIntervalValue = (TextView)findViewById(R.id.textViewSeekBarIntervalValue);
        textViewSeekBarIntervalPowerValue = (TextView)findViewById(R.id.textViewSeekBarIntervalPowerValue);
        textViewSeekBarMotionPowerValue = (TextView)findViewById(R.id.textViewSeekBarMotionPowerValue);

        seekbarRSSI = (SeekBar)findViewById(R.id.seekbarRSSI);
        seekbarInterval = (SeekBar)findViewById(R.id.seekbarInterval);
        seekbarIntervalPower = (SeekBar)findViewById(R.id.seekbarIntervalPower);
        seekbarMotionPower = (SeekBar)findViewById(R.id.seekbarMotionPower);

        switchSound = (Switch)findViewById(R.id.switchSound);
        switchBS_led = (Switch)findViewById(R.id.switchBS_led);
        switchBS_motion = (Switch)findViewById(R.id.switchBS_motion);
        switchBS_motion_led = (Switch)findViewById(R.id.switchBS_motion_led);

        textViewUUID = (TextView)findViewById(R.id.textViewUUID);
        textViewMajor = (TextView)findViewById(R.id.textViewMajor);
        textViewMinor = (TextView)findViewById(R.id.textViewMinor);

        textViewVersion  = (TextView)findViewById(R.id.textViewVersion);
        Date buildDate = new Date(BuildConfig.TIMESTAMP);
        textViewVersion.setText("Build date: " + buildDate.toString());

        switchSound.setChecked(beepEnabled);
        switchSound.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                beepEnabled = isChecked;
                mSharedPreferences.edit().putBoolean("enable_beep", isChecked).apply();
            }
        });
        switchBS_led.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) mBluestoneManager.configBS(ConfigBS.CMD_LED, ConfigBS.CODE_LED_ON);
                else mBluestoneManager.configBS(ConfigBS.CMD_LED, ConfigBS.CODE_LED_OFF);
            }
        });
        switchBS_motion.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked)
                    mBluestoneManager.configBS(ConfigBS.CMD_MOTION, ConfigBS.CODE_MOTION_ON);
                else mBluestoneManager.configBS(ConfigBS.CMD_MOTION, ConfigBS.CODE_MOTION_OFF);
            }
        });
        switchBS_motion_led.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) mBluestoneManager.configBS(ConfigBS.CMD_MOTION_LED,ConfigBS.CODE_MOTION_LED_ON);
                else mBluestoneManager.configBS(ConfigBS.CMD_MOTION_LED,ConfigBS.CODE_MOTION_LED_OFF);
            }
        });


        int rssiIgnore = Integer.parseInt(mSharedPreferences.getString("rssi_filter", "55"));

        seekbarRSSI.setProgress(rssiIgnore);
        textViewSeekBarValue.setText("-" + rssiIgnore + "dBm");
        seekbarRSSI.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                mBluestoneManager.updateRange(-progress);
                textViewSeekBarValue.setText(-progress + "dBm");
                mSharedPreferences.edit().putString("rssi_filter", "" + progress).apply();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        seekbarInterval.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                switch (progress){
                    case 0:
                        textViewSeekBarIntervalValue.setText("OFF");
                        mBluestoneManager.configBS(ConfigBS.CMD_INTERVAL, ConfigBS.CODE_INTERVAL_OFF);
                        break;
                    case 1:
                        textViewSeekBarIntervalValue.setText(500 + "ms");
                        mBluestoneManager.configBS(ConfigBS.CMD_INTERVAL, ConfigBS.CODE_INTERVAL_BROADCAST_500MS);
                        break;
                    case 2:
                        textViewSeekBarIntervalValue.setText(1000 + "ms");
                        mBluestoneManager.configBS(ConfigBS.CMD_INTERVAL, ConfigBS.CODE_INTERVAL_BROADCAST_1000MS);
                        break;
                    case 3:
                        textViewSeekBarIntervalValue.setText(2000 + "ms");
                        mBluestoneManager.configBS(ConfigBS.CMD_INTERVAL, ConfigBS.CODE_INTERVAL_BROADCAST_2000MS);
                        break;
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        seekbarIntervalPower.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                switch (progress){
                    case 0:
                        textViewSeekBarIntervalPowerValue.setText(-40 + "dBm");
                        mBluestoneManager.configBS(ConfigBS.CMD_INTERVAL_PW, ConfigBS.CODE_INTERVAL_ADV_POWER_N40DBM);
                        break;
                    case 1:
                        textViewSeekBarIntervalPowerValue.setText(-30 + "dBm");
                        mBluestoneManager.configBS(ConfigBS.CMD_INTERVAL_PW, ConfigBS.CODE_INTERVAL_ADV_POWER_N30DBM);
                        break;
                    case 2:
                        textViewSeekBarIntervalPowerValue.setText(-20 + "dBm");
                        mBluestoneManager.configBS(ConfigBS.CMD_INTERVAL_PW, ConfigBS.CODE_INTERVAL_ADV_POWER_N20DBM);
                        break;
                    case 3:
                        textViewSeekBarIntervalPowerValue.setText(-16 + "dBm");
                        mBluestoneManager.configBS(ConfigBS.CMD_INTERVAL_PW, ConfigBS.CODE_INTERVAL_ADV_POWER_N16DBM);
                        break;
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

        seekbarMotionPower.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                switch (progress){
                    case 0:
                        textViewSeekBarMotionPowerValue.setText(4 + "dBm");
                        mBluestoneManager.configBS(ConfigBS.CMD_MOTION_PW, ConfigBS.CODE_MOTION_ADV_POWER_P4DBM);
                        break;
                    case 1:
                        textViewSeekBarMotionPowerValue.setText(0 + "dBm");
                        mBluestoneManager.configBS(ConfigBS.CMD_MOTION_PW, ConfigBS.CODE_MOTION_ADV_POWER_0DBM);
                        break;
                    case 2:
                        textViewSeekBarMotionPowerValue.setText(-4 + "dBm");
                        mBluestoneManager.configBS(ConfigBS.CMD_MOTION_PW, ConfigBS.CODE_MOTION_ADV_POWER_N4DBM);
                        break;
                    case 3:
                        textViewSeekBarMotionPowerValue.setText(-8 + "dBm");
                        mBluestoneManager.configBS(ConfigBS.CMD_MOTION_PW, ConfigBS.CODE_MOTION_ADV_POWER_N8DBM);
                        break;
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
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
        public void onBlueStoneCallBack(BlueStone blueStone, boolean inRange, String UUID, int major, int minor) {
            Product current = products.get(blueStone.id);
            if (current == null) {
                Product e = new Product();
                e.beacon = blueStone.id;
                e.image = "item_a";
                products.put(e.beacon, e);
                getBluestoneID(blueStone.id);
            } else if (current.name == null)
            {
                getBluestoneID(blueStone.id);
            }
            if (inRange) {
                if (beepEnabled) toneGen1.startTone(ToneGenerator.TONE_CDMA_PIP,150);
                if (current != null) textViewTitle.setText(current.name);
                textViewUUID_RSSI.setText("RSSI: " + blueStone.average_RSSI + "dBm");
                textViewUUID_ID.setText("ID: " + blueStone.id);
                textViewBattery.setText("Battery: " + blueStone.batt_cur);
                textViewFirmware.setText("Firmware version: " + blueStone.firmware);
                textViewTime.setText("Time alive: " + blueStone.rtc2 + "d " + blueStone.rtc1 + "h");
                textViewUUID.setText("Config: " + blueStone.config);
                textViewMajor.setText("Motion: " + blueStone.motion);
                textViewMinor.setText("Configurable: " + blueStone.configurable);
            } else{
                if (current != null) BluestonesOut.put(current.name, new Date());
                else BluestonesOut.put(blueStone.id, new Date());
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
                mBluestoneManager.updateRange(-Integer.parseInt(sharedPreferences.getString("rssi_filter", "55")));
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

}