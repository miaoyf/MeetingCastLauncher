package com.company.meetingcast;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.media.tv.TvContract;
import android.media.tv.TvInputInfo;
import android.media.tv.TvInputManager;
import android.media.tv.TvView;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private TvInputManager inputManager;
    private TvView hdmiPreview;
    private View hdmiIllustration;
    private TextView hdmiStatus;
    private String connectedInputId;
    private String connectedInputLabel;
    private boolean idleWithoutFocus = true;

    private final Runnable refreshTask = new Runnable() {
        @Override public void run() {
            refreshHdmi();
            hdmiPreview.postDelayed(this, 300);
        }
    };

    private final TvInputManager.TvInputCallback inputCallback = new TvInputManager.TvInputCallback() {
        @Override public void onInputStateChanged(String inputId, int state) {
            if (state == TvInputManager.INPUT_STATE_CONNECTED && hdmiPreview != null) {
                hdmiPreview.post(new Runnable() {
                    @Override public void run() { refreshHdmi(); }
                });
            }
        }
    };

    private final Runnable screensaverTask = new Runnable() {
        @Override public void run() {
            try {
                startActivity(new Intent().setComponent(new ComponentName(
                        "com.mitv.screensaver", "com.mitv.screensaver.RenderActivity")));
            } catch (ActivityNotFoundException | SecurityException error) {
                Toast.makeText(MainActivity.this, "此型号没有系统屏保", Toast.LENGTH_SHORT).show();
            }
        }
    };

    private void scheduleScreensaver() {
        hdmiPreview.removeCallbacks(screensaverTask);
        hdmiPreview.postDelayed(screensaverTask, 5 * 60 * 1000L);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);

        View hdmiCard = findViewById(R.id.hdmiCard);
        View macCard = findViewById(R.id.macCard);
        View windowsCard = findViewById(R.id.windowsCard);
        View settingsButton = findViewById(R.id.settingsButton);
        hdmiPreview = findViewById(R.id.hdmiPreview);
        hdmiIllustration = findViewById(R.id.hdmiIllustration);
        hdmiStatus = findViewById(R.id.hdmiStatus);
        inputManager = (TvInputManager) getSystemService(TV_INPUT_SERVICE);

        String name = Settings.System.getString(getContentResolver(), "dk_device_name");
        if (name == null || name.trim().isEmpty()) {
            name = Settings.Global.getString(getContentResolver(), Settings.Global.DEVICE_NAME);
        }
        if (name == null || name.trim().isEmpty()) {
            name = Settings.Secure.getString(getContentResolver(), "bluetooth_name");
        }
        if (name == null || name.trim().isEmpty()) name = android.os.Build.MODEL;
        final TextView deviceNameHeader = findViewById(R.id.deviceNameHeader);
        final String deviceName = name;
        updateDisplayedName(deviceName);
        hdmiPreview.post(new Runnable() {
            @Override public void run() { syncCastingName(deviceName); }
        });
        deviceNameHeader.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) { showDeviceNameEditor(); }
        });

        hdmiCard.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) { openHdmi(); }
        });
        View.OnClickListener castListener = new View.OnClickListener() {
            @Override public void onClick(View view) { openCasting(); }
        };
        macCard.setOnClickListener(castListener);
        windowsCard.setOnClickListener(castListener);
        settingsButton.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View view) { openSettings(); }
        });

        installFocusEffect(hdmiCard);
        installFocusEffect(macCard);
        installFocusEffect(windowsCard);
        installFocusEffect(settingsButton);
    }

    private void clearCardFocus() {
        idleWithoutFocus = true;
        findViewById(android.R.id.content).clearFocus();
        findViewById(R.id.deviceNameHeader).clearFocus();
        findViewById(R.id.hdmiCard).clearFocus();
        findViewById(R.id.macCard).clearFocus();
        findViewById(R.id.windowsCard).clearFocus();
        findViewById(R.id.settingsButton).clearFocus();
    }

    @Override protected void onResume() {
        super.onResume();
        clearCardFocus();
        setInputCallbackRegistered(true);
        if (hdmiPreview != null) {
            hdmiPreview.post(new Runnable() {
                @Override public void run() { clearCardFocus(); }
            });
            hdmiPreview.removeCallbacks(refreshTask);
            hdmiPreview.post(refreshTask);
            scheduleScreensaver();
        }
    }

    @Override public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus && idleWithoutFocus && hdmiPreview != null) {
            hdmiPreview.post(new Runnable() {
                @Override public void run() { clearCardFocus(); }
            });
        }
    }

    @Override protected void onPause() {
        setInputCallbackRegistered(false);
        if (hdmiPreview != null) {
            hdmiPreview.removeCallbacks(refreshTask);
            hdmiPreview.removeCallbacks(screensaverTask);
            hdmiPreview.reset();
        }
        super.onPause();
    }

    @Override public void onUserInteraction() {
        super.onUserInteraction();
        if (hdmiPreview != null) scheduleScreensaver();
    }

    private void setInputCallbackRegistered(boolean register) {
        try {
            if (register) {
                TvInputManager.class.getMethod("addCallback",
                        TvInputManager.TvInputCallback.class, Handler.class)
                        .invoke(inputManager, inputCallback, new Handler(getMainLooper()));
            } else {
                TvInputManager.class.getMethod("removeCallback", TvInputManager.TvInputCallback.class)
                        .invoke(inputManager, inputCallback);
            }
        } catch (Exception error) {
            android.util.Log.w("MeetingCast", "HDMI state callback unavailable, polling only", error);
        }
    }

    private void refreshHdmi() {
        String foundId = null;
        String foundLabel = null;
        int hdmiNumber = 0;
        for (TvInputInfo info : inputManager.getTvInputList()) {
            if (info.getType() != TvInputInfo.TYPE_HDMI) continue;
            hdmiNumber++;
            CharSequence label = info.loadLabel(this);
            String inputLabel = label == null ? "HDMI " + hdmiNumber : label.toString();
            if (inputManager.getInputState(info.getId()) == TvInputManager.INPUT_STATE_CONNECTED && foundId == null) {
                foundId = info.getId();
                foundLabel = inputLabel;
            }
        }
        boolean changed = foundId == null ? connectedInputId != null : !foundId.equals(connectedInputId);
        connectedInputId = foundId;
        connectedInputLabel = foundLabel;
        if (foundId == null) {
            hdmiPreview.reset();
            hdmiPreview.setVisibility(View.GONE);
            hdmiIllustration.setVisibility(View.VISIBLE);
            hdmiStatus.setText("●  HDMI  ·  等待信号");
            hdmiStatus.setTextColor(android.graphics.Color.parseColor("#85858A"));
        } else {
            hdmiStatus.setText("●  " + foundLabel + "  ·  已连接，正在打开投屏");
            hdmiStatus.setTextColor(android.graphics.Color.parseColor("#30D158"));
            hdmiPreview.reset();
            hdmiPreview.setVisibility(View.GONE);
            hdmiIllustration.setVisibility(View.VISIBLE);
            if (changed) openHdmi();
        }
    }

    private void showDeviceNameEditor() {
        final android.widget.EditText input = new android.widget.EditText(this);
        input.setSingleLine(true);
        input.setText(((TextView) findViewById(R.id.deviceNameHeader)).getText());
        input.setSelection(input.getText().length());
        int padding = (int) (24 * getResources().getDisplayMetrics().density);
        input.setPadding(padding, padding / 2, padding, padding / 2);
        new android.app.AlertDialog.Builder(this)
                .setTitle("设备名称")
                .setMessage("保存后同步到 Mac、Windows 无线投屏列表")
                .setView(input)
                .setNegativeButton("取消", null)
                .setPositiveButton("保存", new android.content.DialogInterface.OnClickListener() {
                    @Override public void onClick(android.content.DialogInterface dialog, int which) {
                        saveDeviceName(input.getText().toString());
                    }
                }).show();
    }

    private void saveDeviceName(String value) {
        String name = value == null ? "" : value.trim();
        if (name.isEmpty()) {
            Toast.makeText(this, "设备名称不能为空", Toast.LENGTH_SHORT).show();
            return;
        }
        if (name.length() > 32) name = name.substring(0, 32);
        try {
            Settings.Global.putString(getContentResolver(), Settings.Global.DEVICE_NAME, name);
            syncCastingName(name);
            updateDisplayedName(name);
            Toast.makeText(this, "设备名称已统一更新", Toast.LENGTH_SHORT).show();
        } catch (SecurityException error) {
            Toast.makeText(this, "没有修改系统名称的权限", Toast.LENGTH_LONG).show();
        }
    }

    private void syncCastingName(String name) {
        String previousName = Settings.System.getString(getContentResolver(), "dk_device_name");
        try {
            try {
                Settings.System.putInt(getContentResolver(), "devicename_change_state", 2);
                Settings.System.putString(getContentResolver(), "dk_device_name", name);
            } catch (IllegalArgumentException ignored) {
                // Newer firmware redirects this key to the secure table.
            }
            Settings.Secure.putString(getContentResolver(), "dk_device_name", name);
            Settings.Secure.putString(getContentResolver(), "bluetooth_name", name);
            android.bluetooth.BluetoothAdapter adapter = android.bluetooth.BluetoothAdapter.getDefaultAdapter();
            if (adapter != null && !name.equals(adapter.getName())) adapter.setName(name);
            String oldP2pName = Settings.Global.getString(getContentResolver(), "wifi_p2p_device_name");
            String suffix = "";
            if (oldP2pName != null) {
                int separator = oldP2pName.lastIndexOf('_');
                if (separator >= 0) suffix = oldP2pName.substring(separator);
            }
            Settings.Global.putString(getContentResolver(), "wifi_p2p_device_name", name + suffix);
            updateWifiDirectName(name + suffix);
            if (!name.equals(previousName)) {
                android.app.ActivityManager activityManager =
                        (android.app.ActivityManager) getSystemService(ACTIVITY_SERVICE);
                activityManager.killBackgroundProcesses("com.xiaomi.mitv.smartshare");
                Intent airplay = new Intent().setComponent(new ComponentName(
                        "com.xiaomi.mitv.smartshare", "com.xiaomi.mitv.smartshare.mairplay.AirPlayService"));
                startService(airplay);
                sendBroadcast(new Intent("com.duokan.duokantv.DEVICE_NAME_CHANGED"));
            }
        } catch (SecurityException | ActivityNotFoundException ignored) {
            // Permissions and component availability vary by firmware.
        }
    }

    private void updateWifiDirectName(String name) {
        try {
            android.net.wifi.p2p.WifiP2pManager manager = (android.net.wifi.p2p.WifiP2pManager)
                    getSystemService(WIFI_P2P_SERVICE);
            android.net.wifi.p2p.WifiP2pManager.Channel channel =
                    manager.initialize(this, getMainLooper(), null);
            java.lang.reflect.Method method = android.net.wifi.p2p.WifiP2pManager.class
                    .getDeclaredMethod("setDeviceName", android.net.wifi.p2p.WifiP2pManager.Channel.class,
                            String.class, android.net.wifi.p2p.WifiP2pManager.ActionListener.class);
            method.invoke(manager, channel, name, null);
        } catch (Exception error) {
            android.util.Log.w("MeetingCast", "Wi-Fi Direct name update failed", error);
        }
    }

    private void updateDisplayedName(String name) {
        ((TextView) findViewById(R.id.deviceNameHeader)).setText(name);
        ((TextView) findViewById(R.id.deviceNameFooter)).setText(name);
        ((TextView) findViewById(R.id.wirelessDeviceHint)).setText("无线投屏请选择「" + name + "」");
    }

    private void installFocusEffect(final View view) {
        view.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus && idleWithoutFocus) {
                    v.clearFocus();
                    return;
                }
                v.animate().scaleX(hasFocus ? 1.03f : 1.0f)
                        .scaleY(hasFocus ? 1.03f : 1.0f).setDuration(150).start();
                v.setElevation(hasFocus ? 12f : 0f);
            }
        });
    }

    private void openHdmi() {
        if (connectedInputId == null) {
            Toast.makeText(this, "请先连接 HDMI 线", Toast.LENGTH_SHORT).show();
            return;
        }
        Intent fullscreen = new Intent(this, HdmiFullscreenActivity.class);
        fullscreen.putExtra("input_id", connectedInputId);
        fullscreen.putExtra("input_label", connectedInputLabel);
        startActivity(fullscreen);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    private void openSettings() {
        try {
            startActivity(new Intent().setComponent(new ComponentName(
                    "com.xiaomi.mitv.settings", "com.xiaomi.mitv.settings.entry.MainActivity")));
            return;
        } catch (ActivityNotFoundException | SecurityException ignored) {
            // Non-Xiaomi firmware may not ship the MiTV settings activity.
        }
        try {
            startActivity(new Intent(Settings.ACTION_SETTINGS));
        } catch (ActivityNotFoundException | SecurityException error) {
            Toast.makeText(this, "未找到系统设置入口", Toast.LENGTH_LONG).show();
        }
    }

    private void openCasting() {
        Intent nativeCast = new Intent();
        nativeCast.setComponent(new ComponentName(
                "com.xiaomi.mitv.smartshare",
                "com.xiaomi.mitv.smartshare.wifidisplay.WfdActivity"));
        nativeCast.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            startActivity(nativeCast);
            return;
        } catch (ActivityNotFoundException | SecurityException ignored) {
            // Fall back to the public wireless-display settings screen on other firmware.
        }
        try {
            startActivity(new Intent("android.settings.WIFI_DISPLAY_SETTINGS"));
        } catch (ActivityNotFoundException | SecurityException error) {
            Toast.makeText(this, "未找到系统投屏入口，请检查投屏服务", Toast.LENGTH_LONG).show();
        }
    }

    @Override public void onBackPressed() {
        // BACK first leaves navigation mode; the launcher itself stays on screen.
        if (!idleWithoutFocus) clearCardFocus();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_HOME) return true;
        if (idleWithoutFocus
                && (keyCode == KeyEvent.KEYCODE_DPAD_UP || keyCode == KeyEvent.KEYCODE_DPAD_DOWN
                || keyCode == KeyEvent.KEYCODE_DPAD_LEFT || keyCode == KeyEvent.KEYCODE_DPAD_RIGHT)) {
            idleWithoutFocus = false;
            findViewById(R.id.macCard).requestFocus();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
