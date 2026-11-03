package com.company.meetingcast;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.content.Intent;
import android.media.tv.TvContract;
import android.media.tv.TvInputManager;
import android.media.tv.TvView;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

public class HdmiFullscreenActivity extends Activity {
    private TvView tvView;
    private TvInputManager inputManager;
    private String inputId;
    private View loadingOverlay;
    private int dialogDismissCount;

    private final Runnable dialogDismissTask = new Runnable() {
        @Override public void run() {
            dismissSystemDialogs();
            if (++dialogDismissCount < 12 && loadingOverlay != null) {
                loadingOverlay.postDelayed(this, 500);
            }
        }
    };

    private final Runnable connectionCheck = new Runnable() {
        @Override public void run() {
            if (inputManager.getInputState(inputId) != TvInputManager.INPUT_STATE_CONNECTED) {
                finish();
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                return;
            }
            tvView.postDelayed(this, 1000);
        }
    };

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        inputId = getIntent().getStringExtra("input_id");
        if (inputId == null) { finish(); return; }
        inputManager = (TvInputManager) getSystemService(TV_INPUT_SERVICE);
        FrameLayout root = new FrameLayout(this);
        tvView = new TvView(this);
        tvView.setBackgroundColor(Color.BLACK);
        root.addView(tvView, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        loadingOverlay = createLoadingOverlay();
        root.addView(loadingOverlay, new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        setContentView(root);
    }

    private View createLoadingOverlay() {
        FrameLayout overlay = new FrameLayout(this);
        overlay.setBackgroundColor(Color.parseColor("#0B1420"));
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setGravity(Gravity.CENTER);
        LinearLayout dots = new LinearLayout(this);
        dots.setOrientation(LinearLayout.HORIZONTAL);
        dots.setGravity(Gravity.CENTER);
        for (int i = 0; i < 3; i++) {
            View dot = new View(this);
            GradientDrawable shape = new GradientDrawable();
            shape.setShape(GradientDrawable.OVAL);
            shape.setColor(Color.parseColor("#0A84FF"));
            dot.setBackground(shape);
            LinearLayout.LayoutParams dotParams = new LinearLayout.LayoutParams(dp(18), dp(18));
            dotParams.leftMargin = dp(10);
            dotParams.rightMargin = dp(10);
            dots.addView(dot, dotParams);
            animateJelly(dot, i * 140);
        }
        TextView title = new TextView(this);
        title.setText("正在连接 HDMI");
        title.setTextColor(Color.WHITE);
        title.setTextSize(28);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, dp(32), 0, 0);
        TextView subtitle = new TextView(this);
        subtitle.setText("即将显示电脑画面");
        subtitle.setTextColor(Color.parseColor("#A1A1AA"));
        subtitle.setTextSize(17);
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setPadding(0, dp(12), 0, 0);
        content.addView(dots);
        content.addView(title);
        content.addView(subtitle);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER);
        overlay.addView(content, params);
        return overlay;
    }

    private void animateJelly(View view, int delay) {
        ObjectAnimator x = ObjectAnimator.ofFloat(view, View.SCALE_X, 0.8f, 1.3f);
        ObjectAnimator y = ObjectAnimator.ofFloat(view, View.SCALE_Y, 1.2f, 0.8f);
        for (ObjectAnimator animator : new ObjectAnimator[] { x, y }) {
            animator.setDuration(650);
            animator.setStartDelay(delay);
            animator.setRepeatCount(ValueAnimator.INFINITE);
            animator.setRepeatMode(ValueAnimator.REVERSE);
            animator.setInterpolator(new AccelerateDecelerateInterpolator());
            animator.start();
        }
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density);
    }

    private void dismissSystemDialogs() {
        // MiTV SystemUI shows an "input source connected" alert on HDMI plug;
        // it listens for this broadcast and dismisses itself.
        sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
    }

    @Override protected void onResume() {
        super.onResume();
        if (inputId != null) {
            dialogDismissCount = 0;
            loadingOverlay.post(dialogDismissTask);
            loadingOverlay.setVisibility(View.VISIBLE);
            loadingOverlay.setAlpha(1f);
            Uri inputUri = TvContract.buildChannelUriForPassthroughInput(inputId);
            tvView.tune(inputId, inputUri);
            tvView.post(connectionCheck);
            loadingOverlay.postDelayed(new Runnable() {
                @Override public void run() {
                    loadingOverlay.animate().alpha(0f).setDuration(400).withEndAction(new Runnable() {
                        @Override public void run() { loadingOverlay.setVisibility(View.GONE); }
                    }).start();
                }
            }, 2200);
        }
    }

    @Override protected void onPause() {
        if (loadingOverlay != null) loadingOverlay.removeCallbacks(dialogDismissTask);
        if (tvView != null) {
            tvView.removeCallbacks(connectionCheck);
            tvView.reset();
        }
        super.onPause();
    }
}
