package com.guc.activemqtest;

import android.nfc.Tag;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.guc.activemqtest.push.SystemPushClient;

import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "main";
    private Button mBtnOpenService;
    private static final String HOST = "tcp://192.168.20.158:1883";
    private static final String USERNAME = "admin";
    private static final String PASSWORD = "admin";
    private MqttConnectOptions mMqttConnectOptions;
    private MqttAsyncClient mMqttAsyncClient;
    private IMqttActionListener mMqttActionListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
//        initMqtt();
    }

//    private void initMqtt() {
//        mMqttConnectOptions = new MqttConnectOptions();
//        mMqttConnectOptions.setAutomaticReconnect(true);
//        mMqttConnectOptions.setKeepAliveInterval(30);
//        mMqttConnectOptions.setConnectionTimeout(60);
//        mMqttConnectOptions.setCleanSession(true);
//        mMqttConnectOptions.setUserName(USERNAME);
//        mMqttConnectOptions.setPassword(PASSWORD.toCharArray());
//        mMqttActionListener =new IMqttActionListener() {
//            @Override
//            public void onSuccess(IMqttToken iMqttToken) {
//                Log.e(TAG, "connect success");
//                DisconnectedBufferOptions disconnectedBufferOptions = new DisconnectedBufferOptions();
//                disconnectedBufferOptions.setBufferEnabled(true);
//                disconnectedBufferOptions.setBufferSize(100);
//                disconnectedBufferOptions.setPersistBuffer(false);
//                disconnectedBufferOptions.setDeleteOldestMessages(false);
//                mMqttAsyncClient.setBufferOpts(disconnectedBufferOptions);
//
//                subscribe("hello",1,null);
//            }
//
//            @Override
//            public void onFailure(IMqttToken iMqttToken, Throwable throwable) {
//                Log.e(TAG, "connect failure"+throwable.getMessage());
//            }
//        };
//        try {
//            mMqttAsyncClient = new MqttAsyncClient(HOST, "android", new MemoryPersistence());
//        } catch (MqttException e) {
//            e.printStackTrace();
//            Log.e(TAG, "init error,msg=" + e.getMessage());
//        }
//        try {
//            mMqttAsyncClient.connect(mMqttConnectOptions, null,mMqttActionListener );
//        } catch (MqttException e) {
//            e.printStackTrace();
//            Log.e(TAG, "connect error,msg=" + e.getMessage());
//        }
//
//
//        mMqttAsyncClient.setCallback(new MqttCallbackExtended() {
//            @Override
//            public void connectionLost(Throwable throwable) {
//                Log.e(TAG,"connectionLost");
//            }
//
//            @Override
//            public void messageArrived(String topic, MqttMessage mqttMessage) throws Exception {
//                Log.e(TAG,"主题:"+ topic+"内容:"+ mqttMessage);
//            }
//
//            @Override
//            public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
//                Log.e(TAG,"deliveryComplete");
//            }
//
//            @Override
//            public void connectComplete(boolean reconnect, String s) {
//                Log.e(TAG,"connectComplete，是否重连"+ reconnect);
//                if (reconnect){
//                    subscribe("hello",1,mMqttActionListener);
//                }
//            }
//        });
//    }

    private void initView() {
        mBtnOpenService = findViewById(R.id.btn_open_service);
        mBtnOpenService.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                SystemPushClient.getPushClient().subscribe("hello",1);
            }
        });
    }

    public void subscribe(String topicFilter, int qos, IMqttActionListener callback) {
        this.subscribe(new String[]{topicFilter}, new int[]{qos}, callback);
    }
    public void subscribe(String[] topicFilters, int[] qos, IMqttActionListener callback) {
        try {
            mMqttAsyncClient.subscribe(topicFilters, qos, null, callback);
        } catch (MqttException e) {
            e.printStackTrace();
            Log.e(TAG, "subscribe error,msg=" + e.getMessage());
            if (callback != null) {
                callback.onFailure(null, e);
            }
        }
    }
}
