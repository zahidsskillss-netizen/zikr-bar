package com.ghora.zikrbar;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;

/**
 * Shows a ringing task as a big animated 3D card over every app. It stays until the task is marked done or moved to
 * another time. If several tasks are waiting they are shown one after the other.
 */
public class TaskOverlayService extends Service {

    static TaskOverlayService live;
    private static final int FG = 470;
    private static final int GOLD = 0xFFF3C24A;

    private WindowManager wm;
    private FrameLayout card;
    private FrameLayout body;
    private long shownId = -1;
    private int shownTotal = -1;
    private final ArrayList<Animator> loops = new ArrayList<>();
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    public void onCreate() {
        super.onCreate();
        L.init(this);
        wm = getSystemService(WindowManager.class);
        live = this;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        L.init(this);
        live = this;
        ArrayList<Tasks.Task> due = Tasks.due(this);
        Notification n = Tasks.build(this, due.isEmpty() ? null : due.get(0));
        try {
            if (Build.VERSION.SDK_INT >= 34) startForeground(FG, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
            else startForeground(FG, n);
        } catch (RuntimeException e) {
            for (Tasks.Task t : due) Tasks.notifyDue(this, t);
            stopSelf();
            return START_NOT_STICKY;
        }
        refresh();
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        removeCard();
        if (live == this) live = null;
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private int dp(float v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    /** Shows the first waiting task, or finishes when nothing is waiting. */
    void refresh() {
        ArrayList<Tasks.Task> due = Tasks.due(this);
        if (due.isEmpty()) {
            finish();
            return;
        }
        Tasks.Task t = due.get(0);
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm != null) nm.notify(FG, Tasks.build(this, t));
        if (card != null && shownId == t.id && shownTotal == due.size()) return;
        removeCard();
        if (!Settings.canDrawOverlays(this) || !addCard(t, due.size())) {
            for (Tasks.Task d : due) Tasks.notifyDue(this, d);
            finish();
        }
    }

    private void finish() {
        removeCard();
        if (Build.VERSION.SDK_INT >= 24) stopForeground(STOP_FOREGROUND_REMOVE); else stopForeground(true);
        stopSelf();
    }

    private void removeCard() {
        handler.removeCallbacksAndMessages(null);
        for (Animator a : loops) a.cancel();
        loops.clear();
        if (card != null) {
            try {
                wm.removeView(card);
            } catch (RuntimeException ignored) {
            }
            card = null;
            body = null;
        }
        shownId = -1;
        shownTotal = -1;
    }

    // ------------------------------------------------------------------ the card

    private TextView bigButton(Kit k, String s, int c1, int c2, int textColor) {
        TextView tv = k.title(s, 16, textColor);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(dp(10), dp(13), dp(10), dp(15));
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{c1, c2});
        g.setCornerRadius(dp(18));
        g.setStroke(dp(1), 0x77FFFFFF);
        tv.setBackground(g);
        tv.setElevation(dp(5));
        tv.setClickable(true);
        press(tv);
        return tv;
    }

    private TextView chip(Kit k, String s) {
        TextView tv = k.strong(s, 14, Color.WHITE);
        tv.setGravity(Gravity.CENTER);
        tv.setPadding(dp(8), dp(11), dp(8), dp(12));
        tv.setBackground(k.rounded(0x26FFFFFF, 14, 0x55FFFFFF));
        tv.setClickable(true);
        press(tv);
        return tv;
    }

    @SuppressWarnings("ClickableViewAccessibility")
    private void press(View v) {
        v.setOnTouchListener((x, e) -> {
            int a = e.getActionMasked();
            if (a == MotionEvent.ACTION_DOWN) x.animate().scaleX(0.94f).scaleY(0.94f).setDuration(80).start();
            else if (a == MotionEvent.ACTION_UP || a == MotionEvent.ACTION_CANCEL) x.animate().scaleX(1f).scaleY(1f).setDuration(120).start();
            return false;
        });
    }

    private LinearLayout.LayoutParams weightLp(int marginDp) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        p.setMargins(dp(marginDp), dp(marginDp), dp(marginDp), dp(marginDp));
        return p;
    }

    @SuppressWarnings("deprecation")
    private boolean addCard(Tasks.Task t, int total) {
        Kit k = new Kit(this);
        FrameLayout wrap = new FrameLayout(this);
        wrap.setLayoutDirection(L.rtl() ? View.LAYOUT_DIRECTION_RTL : View.LAYOUT_DIRECTION_LTR);
        wrap.setClipChildren(false);
        wrap.setClipToPadding(false);
        wrap.setPadding(dp(12), dp(6), dp(12), dp(18));

        FrameLayout b = new FrameLayout(this);
        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{0xFF22346E, 0xFF0D1838, 0xFF080F26});
        bg.setCornerRadius(dp(28));
        bg.setStroke(dp(2), GOLD);
        b.setBackground(bg);
        b.setClipToOutline(true);
        b.setElevation(dp(16));

        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(dp(18), dp(16), dp(18), dp(14));

        // top: a golden bell, the title with the time, and a counter when more tasks are waiting
        LinearLayout top = new LinearLayout(this);
        top.setGravity(Gravity.CENTER_VERTICAL);
        TextView bell = new TextView(this);
        bell.setText("⏰");
        bell.setTextSize(26);
        bell.setGravity(Gravity.CENTER);
        GradientDrawable bb = new GradientDrawable();
        bb.setShape(GradientDrawable.OVAL);
        bb.setGradientType(GradientDrawable.RADIAL_GRADIENT);
        bb.setGradientCenter(0.35f, 0.3f);
        bb.setGradientRadius(dp(46));
        bb.setColors(new int[]{0xFFFFE9A6, 0xFFE9A91C, 0xFF8A5A05});
        bb.setStroke(dp(2), 0xFFFFF3C4);
        bell.setBackground(bb);
        bell.setElevation(dp(6));
        top.addView(bell, new LinearLayout.LayoutParams(dp(56), dp(56)));

        LinearLayout tx = new LinearLayout(this);
        tx.setOrientation(LinearLayout.VERTICAL);
        tx.setPadding(dp(12), 0, dp(12), 0);
        tx.addView(k.title(L.t("tk_notif"), 13, GOLD));
        tx.addView(k.text(Tasks.when(this, t.when), 14.5f, 0xFFC9D6FF));
        top.addView(tx, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        if (total > 1) {
            TextView cnt = k.strong(L.num(1) + "/" + L.num(total), 13, 0xFF2B1C02);
            cnt.setPadding(dp(11), dp(4), dp(11), dp(6));
            cnt.setBackground(k.rounded(GOLD, 14, 0));
            top.addView(cnt);
        }
        col.addView(top);

        TextView msg = k.title(t.text, 21, Color.WHITE);
        msg.setLineSpacing(0, 1.25f);
        msg.setMaxLines(6);
        msg.setEllipsize(TextUtils.TruncateAt.END);
        msg.setPadding(dp(2), dp(14), dp(2), dp(16));
        col.addView(msg);

        FrameLayout actions = new FrameLayout(this);
        col.addView(actions);
        showMain(k, actions, t);

        b.addView(col, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        View shine = new View(this);
        shine.setBackground(new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, new int[]{0x00FFFFFF, 0x38FFFFFF, 0x00FFFFFF}));
        shine.setRotation(16);
        shine.setTranslationX(-dp(300));
        b.addView(shine, new FrameLayout.LayoutParams(dp(70), ViewGroup.LayoutParams.MATCH_PARENT));

        wrap.addView(b, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        b.setAlpha(0f);

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT, Ui.overlayType(),
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                        | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED,
                PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.TOP;
        lp.y = dp(36);
        try {
            wm.addView(wrap, lp);
        } catch (RuntimeException e) {
            return false;
        }
        card = wrap;
        body = b;
        shownId = t.id;
        shownTotal = total;
        buzz();

        b.post(() -> {
            if (body != b) return;
            b.setPivotX(b.getWidth() / 2f);
            b.setPivotY(0f);
            b.setCameraDistance(getResources().getDisplayMetrics().density * 9000f);
            b.setRotationX(-85f);
            b.setTranslationY(-dp(60));
            b.animate().rotationX(0f).translationY(0f).alpha(1f).setDuration(780)
                    .setInterpolator(new OvershootInterpolator(1.1f))
                    .withEndAction(() -> startLoops(b, bb, bg, bell, shine)).start();
        });
        return true;
    }

    private void startLoops(FrameLayout b, GradientDrawable bellBg, GradientDrawable bg, TextView bell, View shine) {
        if (body != b) return;
        ObjectAnimator floatY = ObjectAnimator.ofFloat(b, "translationY", 0f, dp(5));
        floatY.setDuration(2400);
        floatY.setRepeatCount(ValueAnimator.INFINITE);
        floatY.setRepeatMode(ValueAnimator.REVERSE);
        floatY.setInterpolator(new AccelerateDecelerateInterpolator());
        ObjectAnimator tilt = ObjectAnimator.ofFloat(b, "rotationY", -3f, 3f);
        tilt.setDuration(3600);
        tilt.setRepeatCount(ValueAnimator.INFINITE);
        tilt.setRepeatMode(ValueAnimator.REVERSE);
        tilt.setInterpolator(new AccelerateDecelerateInterpolator());
        ValueAnimator glow = ValueAnimator.ofInt(90, 255);
        glow.setDuration(1300);
        glow.setRepeatCount(ValueAnimator.INFINITE);
        glow.setRepeatMode(ValueAnimator.REVERSE);
        glow.addUpdateListener(a -> bg.setStroke(dp(2), Color.argb((int) a.getAnimatedValue(), 243, 194, 74)));
        for (Animator a : new Animator[]{floatY, tilt, glow}) {
            loops.add(a);
            a.start();
        }
        bell.setPivotX(bell.getWidth() / 2f);
        bell.setPivotY(0f);
        Runnable show = new Runnable() {
            @Override
            public void run() {
                if (body != b) return;
                ObjectAnimator ring = ObjectAnimator.ofFloat(bell, "rotation", 0f, -20f, 17f, -13f, 10f, -6f, 0f);
                ring.setDuration(1100);
                ring.start();
                shine.setTranslationX(-dp(120));
                shine.animate().translationX(b.getWidth() + dp(60)).setDuration(1400).start();
                handler.postDelayed(this, 3600);
            }
        };
        handler.postDelayed(show, 150);
    }

    private void buzz() {
        try {
            Vibrator v = getSystemService(Vibrator.class);
            if (v == null) return;
            long[] pattern = {0, 350, 180, 350, 180, 500};
            if (Build.VERSION.SDK_INT >= 26) v.vibrate(VibrationEffect.createWaveform(pattern, -1));
            else v.vibrate(pattern, -1);
        } catch (RuntimeException ignored) {
        }
    }

    // ------------------------------------------------------------------ buttons

    private void showMain(Kit k, FrameLayout actions, Tasks.Task t) {
        actions.removeAllViews();
        LinearLayout row = new LinearLayout(this);
        TextView done = bigButton(k, "✔  " + L.t("tk_done"), 0xFF3EE08F, 0xFF0E8F55, 0xFF03281A);
        TextView later = bigButton(k, "⏱  " + L.t("tk_snooze"), 0xFFFFD166, 0xFFE09A12, 0xFF3A2500);
        row.addView(done, weightLp(4));
        row.addView(later, weightLp(4));
        actions.addView(row);
        done.setOnClickListener(v -> leave(() -> Tasks.setDone(this, t.id)));
        later.setOnClickListener(v -> showSnooze(k, actions, t));
    }

    private void showSnooze(Kit k, FrameLayout actions, Tasks.Task t) {
        actions.removeAllViews();
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        TextView head = k.title(L.t("tk_later"), 15, GOLD);
        head.setPadding(dp(4), 0, dp(4), dp(6));
        col.addView(head);

        LinearLayout r1 = new LinearLayout(this);
        TextView a = chip(k, L.f("tk_plus_min", L.num(10)));
        TextView b = chip(k, L.f("tk_plus_h", L.num(1)));
        r1.addView(a, weightLp(3));
        r1.addView(b, weightLp(3));
        col.addView(r1);

        LinearLayout r2 = new LinearLayout(this);
        TextView c = chip(k, L.t("tk_tonight"));
        TextView d = chip(k, L.t("tk_tomorrow8"));
        r2.addView(c, weightLp(3));
        r2.addView(d, weightLp(3));
        col.addView(r2);

        LinearLayout r3 = new LinearLayout(this);
        TextView e = chip(k, L.t("tk_custom"));
        TextView back = chip(k, (L.rtl() ? "→  " : "←  ") + L.t("tk_back"));
        r3.addView(e, weightLp(3));
        r3.addView(back, weightLp(3));
        col.addView(r3);
        actions.addView(col);

        a.setOnClickListener(v -> snoozeTo(t, Tasks.in(10)));
        b.setOnClickListener(v -> snoozeTo(t, Tasks.in(60)));
        c.setOnClickListener(v -> snoozeTo(t, Tasks.tonight()));
        d.setOnClickListener(v -> snoozeTo(t, Tasks.tomorrowMorning()));
        e.setOnClickListener(v -> {
            try {
                startActivity(new Intent(this, TasksActivity.class).putExtra(TasksActivity.EXTRA_CHANGE, t.id)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP));
            } catch (RuntimeException ignored) {
            }
        });
        back.setOnClickListener(v -> showMain(k, actions, t));
    }

    private void snoozeTo(Tasks.Task t, long when) {
        String msg = L.t("tk_moved") + "  " + Tasks.when(this, when);
        leave(() -> {
            Tasks.reschedule(this, t.id, when);
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
        });
    }

    /** Swings the card away, then runs what the button does. */
    private void leave(Runnable then) {
        if (body == null) {
            then.run();
            return;
        }
        for (Animator a : loops) a.cancel();
        loops.clear();
        handler.removeCallbacksAndMessages(null);
        body.animate().cancel();
        body.animate().rotationX(55f).translationY(-dp(40)).scaleX(0.92f).scaleY(0.92f).alpha(0f).setDuration(320)
                .withEndAction(then).start();
    }
}
