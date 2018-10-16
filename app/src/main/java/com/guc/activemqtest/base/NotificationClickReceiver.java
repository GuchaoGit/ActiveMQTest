package com.guc.activemqtest.base;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Author: hardcattle
 * Time: 2018/9/18 下午10:08
 * Description:
 */
public class NotificationClickReceiver extends BroadcastReceiver {
    private static final String TAG = "NotificationClickReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
//        try {
//            boolean run = FrameworkUtils.isRun(context, "com.hnhy.ygzf");
//            Toast.makeText(context, "receive,app is run:" + run, Toast.LENGTH_LONG).show();
//            if (run) {
//                boolean hasMainActivity = SystemManager.getInstance().getSystem(SystemPage.class).hasActivity(ActivityMain.class.getName());
//                if (hasMainActivity) {
//                    toMainActivity(context, intent.getExtras());
//                } else {
//                    Activity currActivity = SystemManager.getInstance().getSystem(SystemPage.class).getCurrActivity();
//                    if (!currActivity.getClass().getName().equals(ActivityLogin.class.getName())) {
//                        toLoginActivity(context, intent.getExtras());
//                    }
//                }
//            } else {
//                toLoadingActivity(context, intent.getExtras());
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//            Logger.e(TAG, "handle receive error,msg=" + e.getMessage());
//        }
//    }
//
//    private void toLoadingActivity(Context context, Bundle extras) {
//        Intent intent = new Intent(context, ActivityLoading.class);
//        if (extras != null) {
//            intent.putExtras(extras);
//        }
//        context.startActivity(intent);
//    }
//
//    private void toMainActivity(Context context, Bundle extras) {
//        Intent intent = new Intent(context, ActivityMain.class);
//        if (extras != null) {
//            intent.putExtras(extras);
//        }
//        context.startActivity(intent);
//    }
//
//    private void toLoginActivity(Context context, Bundle extras) {
//        Intent intent = new Intent(context, ActivityLogin.class);
//        if (extras != null) {
//            intent.putExtras(extras);
//        }
//        context.startActivity(intent);
    }
}
