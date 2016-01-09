# BlueStoneSDK-android

## Table of Contents

- [Overview](#overview)
- [Gradle via Maven Central](#gradle-via-maven-central)
- [Quick start](#quick-start-for-eddystone)

## Overview

### Gradle via Maven Central

BlueStone Android SDK is available on [JCenter](http://). Declare in your Gradle's `build.gradle` dependency to this library.

```gradle
dependencies {
  compile 'com.pickme:sdk:0.0.2@aar'
}
```

### Quick start

Following code snippet shows you how you can start discovering nearby Estimote beacons broadcasting Eddystone packet.

```java
  private BluestoneManager mBluestoneManager;
  
  ...
  private void run()
  {
 long SCAN_PERIOD = Long.parseLong(mSharedPreferences.getString("scan_timeout", "600000"));
 int rssiIgnore = Integer.parseInt(mSharedPreferences.getString("rssi_filter", "55"));
 int sampleSize = Integer.parseInt(mSharedPreferences.getString("sample_size", "25"));
 mBluestoneManager = new BluestoneManager(this, rssiIgnore, sampleSize, SCAN_PERIOD);
 mBluestoneManager.setListener(mBlueStoneListener);
 }
 
     private BluestoneManager.BlueStoneListener mBlueStoneListener = new BluestoneManager.BlueStoneListener() {
        @Override
        public void onBlueStoneCallBack(String mac, boolean inRange, byte[] scanRecord, int rssi) {
            if (inRange) {
            //Do something when beacon is in range.

            } else {
            //Otherwise do this if beacon is outside of range.
            }
        }
```
