package com.guc.activemqtest.push;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.media.RingtoneManager;
import android.os.Build;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;
import android.util.Log;

import com.google.gson.Gson;
import com.guc.activemqtest.IPushService;
import com.guc.activemqtest.IPushServiceCallback;
import com.guc.activemqtest.R;
import com.guc.activemqtest.base.Constant;
import com.guc.activemqtest.datastructure.LinkQueue;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;

/**
 * Created by guc on 2018/9/10.
 * 描述：
 */
public class PushService extends Service {
    private static final String TAG = "PushService";
    private static final String NOTIFICATION_CHANNEL_ID = "notification_channel_id";
    //    private static final String HOST = "tcp://35.233.186.244:1883";
    private static final String HOST = "tcp://192.168.20.158:1883";
    private static final String USERNAME = "admin";
    private static final String PASSWORD = "admin";

    private IPushService.Stub mBinder;
    private IPushServiceCallback mPushServiceCallback;

    private MqttClient mMqttClient;
    private MqttConnectOptions mMqttConnectOptions;
    private NotificationManager mNotificationManager;
    private boolean mIsReconnecting;
    private boolean mIsRunningTimer;

    private Gson mGson;
    private List<TopicWrapper> mSubscribedTopics;
    private LinkQueue<TopicWrapper> mSubscribeQueue;
    private int mNotificationId;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.e(TAG, "onCreate");
        init();
        mBinder = new IPushService.Stub() {
            @Override
            public void registerCallback(IPushServiceCallback callback) throws RemoteException {
                mPushServiceCallback = callback;
            }

            @Override
            public void unRegisterCallback() throws RemoteException {
                mPushServiceCallback = null;
            }

            @Override
            public boolean isConnect() throws RemoteException {
                return mMqttClient != null && mMqttClient.isConnected();
            }

            @Override
            public void subscribe(String topic, int qos) throws RemoteException {
                Log.e(TAG, String.format("subscribe topic=%s,qos=%s", topic, qos));
                subscribeTopic(new TopicWrapper(topic, qos));
            }

            @Override
            public void unSubscribe(String topic) throws RemoteException {

            }
        };

        mMqttClient = new MqttClient(HOST, "hello");
        mMqttClient.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable throwable) {
                Log.e(TAG, "connectionLost");
                mSubscribeQueue.enQueue(mSubscribedTopics);
                mSubscribedTopics.clear();
                tryReconnect();
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) {
                String msg = new String(message.getPayload());
                Log.e(TAG, "messageArrived,topic=" + topic + ",msg=" + msg);
                sendNotice(getPushMsg(msg));
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
                Log.e(TAG, "deliveryComplete");
            }
        });

        connect();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.e(TAG, "onStartCommand");
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        Log.e(TAG, "onBind");
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        Log.e(TAG, "onUnbind");
        mPushServiceCallback = null;
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.e(TAG, "onDestroy");
        mBinder = null;
        mPushServiceCallback = null;
        mMqttConnectOptions = null;
        mNotificationManager = null;
    }

    private void connect() {
        mMqttClient.connect(mMqttConnectOptions, new IMqttActionListener() {
            @Override
            public void onSuccess(IMqttToken iMqttToken) {
                Log.e(TAG, "connect success");
                try {
                    mPushServiceCallback.onConnectCallback(true, "connect success");
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
                mIsReconnecting = false;
                subscribeFromQueue();
            }

            @Override
            public void onFailure(IMqttToken iMqttToken, Throwable throwable) {
                Log.w(TAG, "connect error,msg=" + throwable.getMessage());
                try {
                    mPushServiceCallback.onConnectCallback(false, "connect error,msg=" + throwable.getMessage());
                } catch (RemoteException e) {
                    e.printStackTrace();
                }

                mIsReconnecting = false;
                tryReconnect();
            }
        });
    }

    private void subscribeTopic(final TopicWrapper topicWrapper) {
        if (mMqttClient != null && mMqttClient.isConnected()) {
            Log.e(TAG, "subscribeTopic ed size=" + mSubscribedTopics.size());
            if (mSubscribedTopics.contains(topicWrapper)) {
                return;
            }
            mMqttClient.subscribe(topicWrapper.mTopic, topicWrapper.mQos, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken iMqttToken) {
                    Log.e(TAG, String.format("subscribeTopic callback topic=%s:%s", topicWrapper.mTopic, true));
                    if (mPushServiceCallback != null) {
                        try {
                            mPushServiceCallback.onSubscribeCallback(true, "");
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    }

                    mSubscribedTopics.add(topicWrapper);
                }

                @Override
                public void onFailure(IMqttToken iMqttToken, Throwable throwable) {
                    Log.w(TAG, String.format("subscribeTopic callback topic=%s:%s,msg=%s", topicWrapper.mTopic, false, throwable.getMessage()));
                    if (mPushServiceCallback != null) {
                        try {
                            mPushServiceCallback.onSubscribeCallback(false, throwable.getMessage());
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
        } else {
            Log.e(TAG, "subscribeTopic,mqtt is not connect, into queue first.");
            mSubscribeQueue.enQueue(topicWrapper);
        }
    }

    private void init() {
        mGson = new Gson();
        mSubscribedTopics = new ArrayList<>();
        mSubscribeQueue = new LinkQueue<>();

        initNotification();

        mMqttConnectOptions = new MqttConnectOptions();
        mMqttConnectOptions.setAutomaticReconnect(true);
        mMqttConnectOptions.setKeepAliveInterval(30);
        mMqttConnectOptions.setConnectionTimeout(60);
        mMqttConnectOptions.setCleanSession(false);
        mMqttConnectOptions.setUserName(USERNAME);
        mMqttConnectOptions.setPassword(PASSWORD.toCharArray());
    }

    private void tryReconnect() {
        if (!mIsReconnecting && !mIsRunningTimer) {
            mIsReconnecting = true;
            mIsRunningTimer = true;
            Observable.interval(0, 60, TimeUnit.SECONDS)
                    .take(60).subscribe(new Observer<Long>() {
                Disposable mDisposable;

                @Override
                public void onSubscribe(Disposable d) {
                    mDisposable = d;
                }

                @Override
                public void onNext(Long aLong) {
                    if (mMqttClient.isConnected()) {
                        mDisposable.dispose();
                        mIsRunningTimer = false;
                    } else {
                        connect();
                    }
                }

                @Override
                public void onError(Throwable e) {
                    e.printStackTrace();
                }

                @Override
                public void onComplete() {
                    mIsRunningTimer = false;
                }
            });
        }
    }

    private void subscribeFromQueue() {
        while (!mSubscribeQueue.isEmpty()) {
            subscribeTopic(mSubscribeQueue.deQueue());
        }
    }

    private void initNotification() {
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, "My Notifications", NotificationManager.IMPORTANCE_DEFAULT);
            notificationChannel.setDescription("Channel description");
            notificationChannel.enableLights(true);
            notificationChannel.setLightColor(Color.RED);
            notificationChannel.setVibrationPattern(new long[]{0, 1000, 500, 1000});
            notificationChannel.enableVibration(true);
            mNotificationManager.createNotificationChannel(notificationChannel);
        }
    }

    private void sendNotice(PushMessage pushMsg) {
        mNotificationId++;
        if (mNotificationId >= 99999) {
            mNotificationId = 0;
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setVibrate(new long[]{0, 100, 100, 100, 100, 100})
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(pushMsg.title)
                .setContentText(pushMsg.content);

        Intent clickIntent = new Intent(Constant.INTENT_NOTICE);
        clickIntent.putExtra("type", pushMsg.type);
        ComponentName componentName = new ComponentName("com.guc.activemqtest", "com.guc.activemqtest.base.NotificationClickReceiver");
        clickIntent.setComponent(componentName);
        PendingIntent contentIntent = PendingIntent.getBroadcast(this, mNotificationId, clickIntent, PendingIntent.FLAG_CANCEL_CURRENT);
        builder.setContentIntent(contentIntent);
        builder.setAutoCancel(true);
        Notification notification = builder.build();
        notification.flags |= Notification.FLAG_AUTO_CANCEL;

        mNotificationManager.notify(mNotificationId, builder.build());
    }

    private PushMessage getPushMsg(String msg) {
        if (!TextUtils.isEmpty(msg)) {
            msg = msg.substring(0, msg.lastIndexOf("@"));
            return mGson.fromJson(msg, PushMessage.class);
        }
        return null;
    }

    private static class TopicWrapper {
        private String mTopic;
        private int mQos;

        private TopicWrapper(String topic, int qos) {
            this.mTopic = topic;
            this.mQos = qos;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj instanceof TopicWrapper) {
                TopicWrapper t = (TopicWrapper) obj;
                return t.mTopic.equals(mTopic);
            }
            return false;
        }
    }

    private static class PushMessage {
        public String title;
        public String content;
        public String type;
    }
}
