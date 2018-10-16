// IPushServiceCallback.aidl
package com.guc.activemqtest;

// Declare any non-default types here with import statements

interface IPushServiceCallback {
     void onConnectCallback(boolean success,String msg);
     void onSubscribeCallback(boolean success,String msg);
     void onUnSubscribeCallback(boolean success,String msg);
}
