package com.guc.activemqtest.push;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.guc.activemqtest.IPushService;
import com.guc.activemqtest.IPushServiceCallback;
import com.guc.activemqtest.base.MyApplication;
import com.guc.activemqtest.utils.Utils;

import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;

/**
 * Created by guc on 2018/10/12.
 * 描述：
 */
public class SystemPushClient {
    private static final String TAG = "SystemPushClient";
    private static SystemPushClient mClient;
    private IPushService mPushService;
    private IPushServiceCallback mPushServiceCallback;
    private ServiceConnection mServiceConnection;

    private boolean mIsBind;
    private boolean mServiceIsRunning;
    private boolean mIsMqttConnected;

    private Context mAppContext;
    
    public static SystemPushClient getPushClient(){
        if (mClient == null){
            synchronized (SystemPushClient.class){
                if (mClient == null){
                    mClient = new SystemPushClient();
                }
            }
        }
       return mClient;
    }
    
    public void destory(){
        stopService();
        mClient = null;
    }
    private void startService() {
        Log.e(TAG, "startService");
        if (!mServiceIsRunning) {
            Intent intent = new Intent(mAppContext, PushService.class);
            mAppContext.startService(intent);
            mServiceIsRunning = true;
        }

        if (!mIsBind) {
            bindService();
        }
    }

    public void stopService() {
        Log.e(TAG, "stopService");
        Intent intent = new Intent(mAppContext, PushService.class);
        mAppContext.stopService(intent);
        unBindService();
    }

    public boolean mqttIsConnect() {
        try {
            return mPushService.isConnect();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return false;
    }

    public void subscribe(final String topic, final int qos) {
        Log.e(TAG, "subscribe,topic=" + topic + ",qos=" + qos);
        if (mIsBind && mIsMqttConnected) {
            sub(topic, qos);
        } else {
            Log.w(TAG, "subscribe,the service not bind or mqtt not connect,retry...");
            Observable.interval(0, 1, TimeUnit.SECONDS)
                    .take(10).subscribe(new Observer<Long>() {
                Disposable mDisposable;

                @Override
                public void onSubscribe(Disposable d) {
                    mDisposable = d;
                }

                @Override
                public void onNext(Long aLong) {
                    if (mIsBind && mIsMqttConnected) {
                        mDisposable.dispose();
                        sub(topic, qos);
                    } else {
                        if (aLong == 10 - 1) {
                            Log.e(TAG, "can't subcribe,mIsBind=" + mIsBind + ",mIsMqttConnected=" + mIsMqttConnected);
                        }
                    }
                }

                @Override
                public void onError(Throwable e) {
                    e.printStackTrace();
                }

                @Override
                public void onComplete() {
                }
            });
        }
    }

    public void unSubscribe(final String topic) {
        Log.e(TAG, "stopService,topic=" + topic);
        if (mIsBind && mIsMqttConnected) {
            unSub(topic);
        } else {
            Log.w(TAG, "unSubscribe,the service not bind or mqtt not connect,retry...");
            Observable.interval(0, 1, TimeUnit.SECONDS)
                    .take(10).subscribe(new Observer<Long>() {
                Disposable mDisposable;

                @Override
                public void onSubscribe(Disposable d) {
                    mDisposable = d;
                }

                @Override
                public void onNext(Long aLong) {
                    if (mIsBind && mIsMqttConnected) {
                        mDisposable.dispose();
                        unSub(topic);
                    } else {
                        if (aLong == 10 - 1) {
                            Log.e(TAG, "can't unSubscribe,mIsBind=" + mIsBind + ",mIsMqttConnected=" + mIsMqttConnected);
                        }
                    }
                }

                @Override
                public void onError(Throwable e) {
                    e.printStackTrace();
                }

                @Override
                public void onComplete() {
                }
            });
        }
    }

    private SystemPushClient(){
        Log.e(TAG,"createInstance");
        mAppContext = MyApplication.getApplication();
        mServiceIsRunning = Utils.isServiceRunning(mAppContext,PushService.class.getName());
        mPushServiceCallback = new IPushServiceCallback.Stub() {
            @Override
            public void onConnectCallback(boolean success, String msg) throws RemoteException {
                Log.e(TAG, "onConnectCallback = " + success + ",msg=" + msg);
                mIsMqttConnected = success;
            }

            @Override
            public void onSubscribeCallback(boolean success, String msg) throws RemoteException {
                Log.e(TAG, "onSubscribeCallback = " + success + ",msg=" + msg);
            }

            @Override
            public void onUnSubscribeCallback(boolean success, String msg) throws RemoteException {
                Log.e(TAG, "onUnSubscribeCallback = " + success + ",msg=" + msg);
            }
        };
        mServiceConnection = new ServiceConnection() {
            @Override
            public void onServiceConnected(ComponentName componentName, IBinder service) {
                Log.e(TAG, "onServiceConnected");
                mIsBind = true;
                mPushService = IPushService.Stub.asInterface(service);
                try {
                    mPushService.registerCallback(mPushServiceCallback);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onServiceDisconnected(ComponentName componentName) {
                Log.e(TAG, "onServiceConnected");
                mIsBind = false;
            }
        };
        startService();
        bindService();
    }
    private void sub(String topic, int qos) {
        try {
            mPushService.subscribe(topic, qos);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    private void unSub(String topic) {
        try {
            mPushService.unSubscribe(topic);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
    private void bindService() {
        Log.e(TAG, "bindService");
        Intent intent = new Intent(mAppContext, PushService.class);
        mAppContext.bindService(intent, mServiceConnection, Context.BIND_AUTO_CREATE);
    }

    private void unBindService() {
        Log.e(TAG, "unBindService");
        mAppContext.unbindService(mServiceConnection);
    }
}
