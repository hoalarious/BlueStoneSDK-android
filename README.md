# BlueStoneSDK-android

## Table of Contents

- [Overview](#overview)
- [Gradle via Jcenter](#gradle-via-jcenter)
- [Quick start](#quick-start)

## Overview
Hello!

### Gradle via Jcenter

BlueStone Android SDK is available on [JCenter](http://jcenter.bintray.com/pickme/bluestone_sdk/bluestone-sdk/). Declare in your Gradle's `build.gradle` dependency to this library.

```gradle
dependencies {
  compile 'pickme.bluestone_sdk:bluestone-sdk:0.0.2'
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
    public void onBlueStoneCallBack(String mac, boolean inRange, byte[] scanRecord, int rssi) {
        if (inRange) {
        //Do something when beacon is in range. Use the mac string to identify the beacon.

        } else {
        //Otherwise do this if beacon is outside of range.
        }
    }
```
