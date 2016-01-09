# BlueStoneSDK-android

## Table of Contents

- [Overview](#overview)
- [Gradle via Jcenter](#gradle-via-jcenter)
- [Quick start](#quick-start)

## Overview
Hello!

### Gradle via Jcenter

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
  
  private void run()
    {
     long SCAN_PERIOD = 600000
     int rssiIgnore = 55
     int sampleSize = 25
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
