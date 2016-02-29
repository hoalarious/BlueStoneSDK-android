# BlueStoneSDK-android

## Table of Contents

- [Overview](#overview)
- [Gradle via Jcenter](#gradle-via-jcenter)
- [Quick start](#quick-start)
- [Commands](#commands)

## Overview
Hello!

### Gradle via Jcenter

BlueStone Android SDK is available on [JCenter](http://jcenter.bintray.com/pickme/bluestone_sdk/bluestone-sdk/). Declare in your Gradle's `build.gradle` dependency to this library.

```gradle
dependencies {
  compile 'pickme.bluestone_sdk:bluestone-sdk:0.0.5'
}
```

### Quick start

Sample code

```java
  private BluestoneManager mBluestoneManager;
  
   @Override
    protected void onCreate(Bundle savedInstanceState) {
    ...
    long SCAN_PERIOD = 600000; //Scanning duration. Accepts values betwen 10000-6000000. Units in milliseconds. Defaults to 600000.
     int rssiIgnore = 55; //inRange filter. Between 10-110. Will default to 55 otherwise.
     int precision = 5; //precision. Enter values between 1-10. Will default to 5 otherwise.
     mBluestoneManager = new BluestoneManager(this, rssiIgnore, precision, SCAN_PERIOD);
     mBluestoneManager.setListener(mBlueStoneListener);
    ...
    }
 
 private BluestoneManager.BlueStoneListener mBlueStoneListener = new BluestoneManager.BlueStoneListener() {
        @Override
        public void onBlueStoneCallBack(String mac, boolean inRange, byte[] scanRecord, int rssi, String batt, String firmware, String days, String hours) {
            if (inRange) {
            //Do something when beacon is in range. Use the mac string to identify the beacon.
    
            } else {
            //Otherwise do this if beacon is outside of range.
            }
        }
    
        @Override
        public void onScanStart() {
            //Do something when scan starts
        }

        @Override
        public void onScanStop() {
            //Do something when scan stops
        }
    };
```

### Commands

Stop scan

```java
mBluestoneManager.stopScan();
```

Start scan

```java
mBluestoneManager.startScan();
```

Update RSSI ignore

```java
mBluestoneManager.updateRssiIgnore(55); //reverse dBM. 55 for -55dBm. Increase this value if the beacon is not picked up.
```

Update Scan timeout

```java
mBluestoneManager.updateScanTimeout(60000); //Time out in milliseconds. 60000 = 60 seconds.
```
