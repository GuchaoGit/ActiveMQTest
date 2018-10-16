// IPushService.aidl
package com.guc.activemqtest;
import com.guc.activemqtest.IPushServiceCallback;
// Declare any non-default types here with import statements

interface IPushService {
       void registerCallback(IPushServiceCallback callback);
       void unRegisterCallback();

       boolean isConnect();

       void subscribe(String topic,int qos);
       void unSubscribe(String topic);
}
