package com.company.meetingcast;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(final Context context, Intent intent) {
        if (!Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) return;
        final Intent home = new Intent(context, MainActivity.class);
        home.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.N_MR1) {
            // Legacy MiTV launches its own home after BOOT_COMPLETED.
            new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
                @Override public void run() { context.startActivity(home); }
            }, 5000);
        } else {
            context.startActivity(home);
        }
    }
}
