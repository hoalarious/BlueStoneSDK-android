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
    long SCAN_PERIOD = 600000;
     int rssiIgnore = 55;
     int sampleSize = 25;
     mBluestoneManager = new BluestoneManager(this, rssiIgnore, sampleSize, SCAN_PERIOD);
     mBluestoneManager.setListener(mBlueStoneListener);
    ...
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
