package com.guc.activemqtest.base;

import android.app.Application;
import android.content.Context;

/**
 * Created by guc on 2018/10/12.
 * 描述：
 */
public class MyApplication extends Application {
    private static MyApplication mApplication;

    public static MyApplication getApplication() {
        return mApplication;
    }

    public Context getContext() {
        return (Context) mApplication;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mApplication = this;
    }
}
