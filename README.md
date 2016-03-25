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
  compile 'pickme.bluestone_sdk:bluestone-sdk:0.0.11'
}
```

### Quick start

Sample code

```java
  private BluestoneManager mBluestoneManager;
  
   @Override
    protected void onCreate(Bundle savedInstanceState) {
    ...
     mBluestoneManager = new BluestoneManager(this);
     mBluestoneManager.setListener(mBlueStoneListener);
    ...
    }
 
 private BluestoneManager.BlueStoneListener mBlueStoneListener = new BluestoneManager.BlueStoneListener() {
        @Override
        public void onBlueStoneCallBack(BlueStone blueStone, boolean inRange, String UUID, int major, int minor) {
            if (inRange) {
              //Do something when beacon is in range. Use the mac string to identify the beacon.
              Log.i("BlueStone","Detected a BlueStone in range with ID: " + blueStone.mac);
              String motion = blueStone.motion; //"0" if not in motion and "1" if in motion. Will convert to bool in next version.
            } else {
              //Otherwise do this if beacon is outside of range.
              Log.i("BlueStone","Detected a BlueStone out of range with ID: " + mac);
            }
        }
    
        @Override
        public void onScanStart() {
            //Do something when scan starts
            Log.i("BlueStone","Scanning has started");
        }

        @Override
        public void onScanStop() {
            //Do something when scan stops
            Log.i("BlueStone","Scanning has stopped");
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
mBluestoneManager.updateRange(-55); //Default: -55dBm. Range: -10 to -110 Increase this value if the beacon is not detected in range.
```

Update Scan timeout

```java
mBluestoneManager.updateScanTimeout(60000); //Default: 60000000. Range: 10000 to 600000000. Time out in milliseconds.
```

