package com.company.meetingcast;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.IBinder;

/** Returns to the meeting-room launcher when the HDMI cable is unplugged. */
public class HdmiReturnService extends Service {
    private static final String CHANNEL_ID = "hdmi_session";
    private final BroadcastReceiver hdmiReceiver = new BroadcastReceiver() {
        @Override public void onReceive(Context context, Intent intent) {
            if (isInitialStickyBroadcast()) return;
            if (!"android.intent.action.HDMI_PLUGGED".equals(intent.getAction())) return;
            Intent home = new Intent(context, MainActivity.class);
            home.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP
                    | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            context.startActivity(home);
        }
    };

    @Override public void onCreate() {
        super.onCreate();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(new NotificationChannel(
                    CHANNEL_ID, "HDMI 连接", NotificationManager.IMPORTANCE_MIN));
            Notification notification = new Notification.Builder(this, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_launcher)
                    .setContentTitle("HDMI 投屏中")
                    .setContentText("拔出 HDMI 线后自动返回会议桌面")
                    .build();
            startForeground(2, notification);
        }
        registerReceiver(hdmiReceiver, new IntentFilter("android.intent.action.HDMI_PLUGGED"));
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        return START_NOT_STICKY;
    }

    @Override public void onDestroy() {
        unregisterReceiver(hdmiReceiver);
        super.onDestroy();
    }

    @Override public IBinder onBind(Intent intent) { return null; }
}
