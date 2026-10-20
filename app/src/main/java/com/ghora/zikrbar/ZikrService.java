package com.ghora.zikrbar;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ServiceInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.WindowManager;
import android.widget.Toast;

import java.util.Calendar;

/**
 * Keeps the name of Allah Ta'ala in the status bar, rotates the dhikr in the notification,
 * shows the floating tasbih over other apps, and reminds the user when no dhikr was made for a while.
 */
public class ZikrService extends Service {

    static final String ACTION_REFRESH = "com.ghora.zikrbar.REFRESH";
    static final String ACTION_TOGGLE_TASBIH = "com.ghora.zikrbar.TOGGLE_TASBIH";
    static final String ACTION_SHOW_TASBIH = "com.ghora.zikrbar.SHOW_TASBIH";

    private static final String CHANNEL = "zikr_bar";
    private static final String CHANNEL_REMIND = "zikr_reminder";
    private static final int NOTIF_ID = 1;
    private static final int REMIND_ID = 2;

    private static Bitmap iconCache;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            Prefs.next(ZikrService.this);
            notifyMain();
            handler.postDelayed(this, Prefs.intervalMs(ZikrService.this));
        }
    };
    private final Runnable reminderCheck = new Runnable() {
        @Override
        public void run() {
            checkReminder();
            handler.postDelayed(this, 60_000);
        }
    };

    private WindowManager wm;
    private TasbihView tasbih;
    private WindowManager.LayoutParams lp;
    private OverlayParts.EdgeTab tab;
    private WindowManager.LayoutParams tabLp;
    private OverlayParts.CloseTarget closeTarget;
    private float scale = 1f;
    private View unlockPopup;

    private final BroadcastReceiver unlockReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_USER_PRESENT.equals(intent.getAction())
                    && Prefs.unlockPopup(ZikrService.this)
                    && Settings.canDrawOverlays(ZikrService.this)) {
                showUnlockPopup();
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        wm = getSystemService(WindowManager.class);
        NotificationManager nm = getSystemService(NotificationManager.class);
        NotificationChannel ch = new NotificationChannel(CHANNEL, "ذکر بار", NotificationManager.IMPORTANCE_LOW);
        ch.setShowBadge(false);
        ch.setSound(null, null);
        ch.enableVibration(false);
        nm.createNotificationChannel(ch);
        NotificationChannel rem = new NotificationChannel(CHANNEL_REMIND, "د ذکر یادونه", NotificationManager.IMPORTANCE_DEFAULT);
        rem.setShowBadge(false);
        nm.createNotificationChannel(rem);

        IntentFilter f = new IntentFilter(Intent.ACTION_USER_PRESENT);
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(unlockReceiver, f, Context.RECEIVER_EXPORTED);
        } else {
            registerReceiver(unlockReceiver, f);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent != null ? intent.getAction() : null;
        if (ACTION_TOGGLE_TASBIH.equals(action)) {
            boolean on = !Prefs.overlay(this);
            Prefs.setOverlay(this, on);
            if (on) Prefs.setMini(this, 0, Prefs.miniY(this));
        } else if (ACTION_SHOW_TASBIH.equals(action)) {
            getSystemService(NotificationManager.class).cancel(REMIND_ID);
            if (Settings.canDrawOverlays(this)) {
                Prefs.setOverlay(this, true);
                Prefs.setMini(this, 0, Prefs.miniY(this));
            } else {
                startActivity(new Intent(this, MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
            }
        }

        Notification n = buildNotification();
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIF_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        } else {
            startForeground(NOTIF_ID, n);
        }
        Prefs.setRunning(this, true);
        if (Prefs.lastZikr(this) == 0) Prefs.setLastZikr(this, System.currentTimeMillis());

        syncOverlay();
        handler.removeCallbacks(tick);
        handler.postDelayed(tick, Prefs.intervalMs(this));
        handler.removeCallbacks(reminderCheck);
        handler.postDelayed(reminderCheck, 60_000);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        try {
            unregisterReceiver(unlockReceiver);
        } catch (RuntimeException ignored) {
        }
        removeUnlockPopup();
        handler.removeCallbacksAndMessages(null);
        removeTasbih();
        removeTab();
        hideClose();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    // ---------------- notifications ----------------

    private void notifyMain() {
        getSystemService(NotificationManager.class).notify(NOTIF_ID, buildNotification());
    }

    private Notification buildNotification() {
        String text = Prefs.current(this);
        PendingIntent open = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class),
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent toggle = PendingIntent.getService(this, 1,
                new Intent(this, ZikrService.class).setAction(ACTION_TOGGLE_TASBIH),
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        Icon icon = Icon.createWithBitmap(allahIcon());
        String toggleLabel = Prefs.overlay(this) ? "تسبیح پټول" : "تسبیح ښکاره کول";

        return new Notification.Builder(this, CHANNEL)
                .setSmallIcon(icon)
                .setContentTitle(text)
                .setStyle(new Notification.BigTextStyle().bigText(text))
                .setColor(0xFF1B5E20)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setShowWhen(false)
                .setContentIntent(open)
                .addAction(new Notification.Action.Builder(icon, toggleLabel, toggle).build())
                .build();
    }

    /** Sends «فَاذْكُرُونِي أَذْكُرْكُمْ» when no dhikr was counted for the chosen number of hours. */
    private void checkReminder() {
        int hours = Prefs.reminderHours(this);
        if (hours <= 0) return;
        long now = System.currentTimeMillis();
        long gap = hours * 3_600_000L;
        if (now - Prefs.lastZikr(this) < gap) return;
        if (now - Prefs.lastReminder(this) < gap) return;
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (hour >= 21 || hour < 6) return; // quiet at night (9pm – 6am)

        PendingIntent show = PendingIntent.getService(this, 2,
                new Intent(this, ZikrService.class).setAction(ACTION_SHOW_TASBIH),
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        String body = hours == 1
                ? "یو ساعت کیږي چې ذکر مو نه دی کړی. راځئ د الله تعالی ذکر وکړو."
                : Ui.fa(hours) + " ساعته کیږي چې ذکر مو نه دی کړی. راځئ د الله تعالی ذکر وکړو.";
        Notification n = new Notification.Builder(this, CHANNEL_REMIND)
                .setSmallIcon(Icon.createWithBitmap(allahIcon()))
                .setContentTitle("فَاذْكُرُونِي أَذْكُرْكُمْ")
                .setContentText(body)
                .setStyle(new Notification.BigTextStyle().bigText(body))
                .setColor(0xFF1B5E20)
                .setAutoCancel(true)
                .setContentIntent(show)
                .build();
        getSystemService(NotificationManager.class).notify(REMIND_ID, n);
        Prefs.setLastReminder(this, now);
    }

    /** The name of Allah Ta'ala (ﷲ in the Amiri font), white on transparent, for the status bar. */
    private Bitmap allahIcon() {
        if (iconCache != null) return iconCache;
        int size = 128;
        Bitmap bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(Color.WHITE);
        p.setTypeface(Ui.amiriBold(this));
        String word = "ﷲ";
        Rect b = new Rect();
        p.setTextSize(100f);
        p.getTextBounds(word, 0, word.length(), b);
        float s = Math.min((size - 6f) / b.width(), (size - 6f) / b.height());
        p.setTextSize(100f * s);
        p.getTextBounds(word, 0, word.length(), b);
        canvas.drawText(word, size / 2f - b.exactCenterX(), size / 2f - b.exactCenterY(), p);
        iconCache = bmp;
        return bmp;
    }

    // ---------------- unlock popup ----------------

    /** Shows a dhikr card in the middle of the screen for 2 seconds after the phone is unlocked. */
    private void showUnlockPopup() {
        removeUnlockPopup();
        Kit k = new Kit(this);
        Point s = screen();

        android.widget.LinearLayout card = new android.widget.LinearLayout(this);
        card.setOrientation(android.widget.LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER_HORIZONTAL);
        card.setPadding(k.dp(24), k.dp(22), k.dp(24), k.dp(24));
        android.graphics.drawable.GradientDrawable bg = new android.graphics.drawable.GradientDrawable(
                android.graphics.drawable.GradientDrawable.Orientation.TL_BR, new int[]{0xF22E7D32, 0xF20B3319});
        bg.setCornerRadius(k.dp(28));
        bg.setStroke(k.dp(1.5f), 0xCCC9A227);
        card.setBackground(bg);
        card.setElevation(k.dp(10));

        android.widget.TextView glyph = k.arabic("\uFDF2", 34, Kit.GOLD);
        glyph.setIncludeFontPadding(false);
        android.graphics.drawable.GradientDrawable ring = new android.graphics.drawable.GradientDrawable();
        ring.setShape(android.graphics.drawable.GradientDrawable.OVAL);
        ring.setColor(0x22000000);
        ring.setStroke(k.dp(1.5f), 0x99F3E3A5);
        glyph.setBackground(ring);
        card.addView(glyph, new android.widget.LinearLayout.LayoutParams(k.dp(70), k.dp(70)));

        android.widget.TextView zk = k.arabic(Prefs.nextUnlockText(this), 25, Color.WHITE);
        zk.setPadding(0, k.dp(14), 0, k.dp(6));
        zk.setLineSpacing(0, 1.15f);
        card.addView(zk);

        android.widget.TextView ayah = k.arabic("فَاذْكُرُونِي أَذْكُرْكُمْ", 15, 0xCCF3E3A5);
        card.addView(ayah);

        WindowManager.LayoutParams p = overlayParams(0);
        p.width = Math.min(s.x - k.dp(48), k.dp(380));
        p.gravity = Gravity.CENTER;
        card.setOnClickListener(v -> removeUnlockPopup());
        card.setAlpha(0f);
        card.setScaleX(0.92f);
        card.setScaleY(0.92f);
        try {
            wm.addView(card, p);
        } catch (RuntimeException e) {
            return;
        }
        unlockPopup = card;
        card.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(220).start();
        final View shown = card;
        handler.postDelayed(() -> {
            if (unlockPopup != shown) return;
            shown.animate().alpha(0f).scaleX(0.96f).scaleY(0.96f).setDuration(250)
                    .withEndAction(() -> {
                        if (unlockPopup == shown) removeUnlockPopup();
                    }).start();
        }, 2000);
    }

    private void removeUnlockPopup() {
        if (unlockPopup != null) {
            try {
                wm.removeView(unlockPopup);
            } catch (RuntimeException ignored) {
            }
            unlockPopup = null;
        }
    }

    // ---------------- overlay ----------------

    private void syncOverlay() {
        boolean want = Prefs.overlay(this) && Settings.canDrawOverlays(this);
        if (!want) {
            removeTasbih();
            removeTab();
            return;
        }
        if (Prefs.mini(this) != 0) {
            removeTasbih();
            showTab();
        } else {
            removeTab();
            if (tasbih == null) addTasbih();
            refreshTasbih();
        }
    }

    private void refreshTasbih() {
        if (tasbih != null) {
            scale = Prefs.scale(this);
            tasbih.setSize(scale);
            tasbih.setState(Prefs.tasbihStyle(this), Prefs.tasbihText(this), Prefs.count(this), Prefs.target(this));
        }
    }

    private WindowManager.LayoutParams overlayParams(int flags) {
        WindowManager.LayoutParams p = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | flags,
                PixelFormat.TRANSLUCENT);
        p.gravity = Gravity.TOP | Gravity.START;
        return p;
    }

    private Point screen() {
        Point pt = new Point();
        if (Build.VERSION.SDK_INT >= 30) {
            Rect b = wm.getCurrentWindowMetrics().getBounds();
            pt.set(b.width(), b.height());
        } else {
            wm.getDefaultDisplay().getRealSize(pt);
        }
        return pt;
    }

    private int dp(float v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void addTasbih() {
        scale = Prefs.scale(this);
        tasbih = new TasbihView(this);
        tasbih.setSize(scale);
        lp = overlayParams(0);
        lp.x = Prefs.posX(this);
        lp.y = Prefs.posY(this);

        final int slop = ViewConfiguration.get(this).getScaledTouchSlop();
        final ScaleGestureDetector pinch = new ScaleGestureDetector(this,
                new ScaleGestureDetector.SimpleOnScaleGestureListener() {
                    @Override
                    public boolean onScale(ScaleGestureDetector g) {
                        scale = Math.max(0.5f, Math.min(2f, scale * g.getScaleFactor()));
                        if (tasbih != null) tasbih.setSize(scale);
                        return true;
                    }

                    @Override
                    public void onScaleEnd(ScaleGestureDetector g) {
                        Prefs.setScale(ZikrService.this, scale);
                    }
                });

        tasbih.setOnTouchListener(new View.OnTouchListener() {
            float downX, downY;
            int startX, startY;
            boolean moved, longDone, pinching;
            final Runnable longPress = () -> {
                longDone = true;
                Prefs.setCount(ZikrService.this, 0);
                refreshTasbih();
                vibrate(80);
                Toast.makeText(ZikrService.this, "شمېر صفر شو", Toast.LENGTH_SHORT).show();
            };

            @Override
            public boolean onTouch(View v, MotionEvent e) {
                pinch.onTouchEvent(e);
                switch (e.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        downX = e.getRawX();
                        downY = e.getRawY();
                        startX = lp.x;
                        startY = lp.y;
                        moved = false;
                        longDone = false;
                        pinching = false;
                        handler.postDelayed(longPress, 700);
                        return true;

                    case MotionEvent.ACTION_POINTER_DOWN:
                        pinching = true;
                        handler.removeCallbacks(longPress);
                        hideClose();
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        if (pinching) return true;
                        float dx = e.getRawX() - downX, dy = e.getRawY() - downY;
                        if (!moved && Math.hypot(dx, dy) > slop) {
                            moved = true;
                            handler.removeCallbacks(longPress);
                            showClose();
                        }
                        if (moved) {
                            lp.x = startX + (int) dx;
                            lp.y = startY + (int) dy;
                            if (tasbih != null) wm.updateViewLayout(tasbih, lp);
                            if (closeTarget != null) closeTarget.setActive(inCloseZone(e));
                        }
                        return true;

                    case MotionEvent.ACTION_UP:
                        handler.removeCallbacks(longPress);
                        hideClose();
                        if (pinching) {
                            pinching = false;
                            return true;
                        }
                        if (moved) {
                            Point s = screen();
                            int edge = dp(28);
                            if (inCloseZone(e)) {
                                dismissTasbih();
                            } else if (e.getRawX() < edge) {
                                minimize(1, (int) e.getRawY());
                            } else if (e.getRawX() > s.x - edge) {
                                minimize(2, (int) e.getRawY());
                            } else {
                                Prefs.setPos(ZikrService.this, lp.x, lp.y);
                            }
                        } else if (!longDone) {
                            countOne();
                        }
                        return true;

                    case MotionEvent.ACTION_CANCEL:
                        handler.removeCallbacks(longPress);
                        hideClose();
                        pinching = false;
                        return true;
                }
                return false;
            }
        });
        try {
            wm.addView(tasbih, lp);
        } catch (RuntimeException e) {
            tasbih = null;
        }
    }

    private boolean inCloseZone(MotionEvent e) {
        Point s = screen();
        return e.getRawY() > s.y - dp(190) && Math.abs(e.getRawX() - s.x / 2f) < dp(90);
    }

    private void showClose() {
        if (closeTarget != null) return;
        closeTarget = new OverlayParts.CloseTarget(this);
        WindowManager.LayoutParams p = overlayParams(WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        p.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        p.y = dp(56);
        try {
            wm.addView(closeTarget, p);
        } catch (RuntimeException e) {
            closeTarget = null;
        }
    }

    private void hideClose() {
        if (closeTarget != null) {
            try {
                wm.removeView(closeTarget);
            } catch (RuntimeException ignored) {
            }
            closeTarget = null;
        }
    }

    private void dismissTasbih() {
        vibrate(40);
        Prefs.setOverlay(this, false);
        removeTasbih();
        notifyMain();
        Toast.makeText(this, "تسبیح بند شو. له خبرتیا یا اپ څخه یې بیا روښانه کولی شئ.", Toast.LENGTH_LONG).show();
    }

    private void minimize(int side, int y) {
        vibrate(25);
        Prefs.setPos(this, lp.x, lp.y);
        Prefs.setMini(this, side, y);
        removeTasbih();
        showTab();
    }

    private void restore() {
        int side = Prefs.mini(this);
        Prefs.setMini(this, 0, Prefs.miniY(this));
        removeTab();
        Point s = screen();
        Prefs.setPos(this, side == 1 ? dp(12) : Prefs.posX(this), Math.max(0, Prefs.miniY(this) - dp(80)));
        addTasbih();
        refreshTasbih();
        if (side == 2 && tasbih != null) {
            tasbih.post(() -> {
                if (tasbih == null) return;
                lp.x = Math.max(0, s.x - tasbih.getWidth() - dp(12));
                wm.updateViewLayout(tasbih, lp);
                Prefs.setPos(this, lp.x, lp.y);
            });
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private void showTab() {
        if (tab != null) return;
        int side = Prefs.mini(this);
        tab = new OverlayParts.EdgeTab(this, side);
        tabLp = overlayParams(0);
        tabLp.gravity = Gravity.TOP | (side == 1 ? Gravity.START : Gravity.END);
        tabLp.x = 0;
        tabLp.y = Math.max(0, Prefs.miniY(this) - dp(38));

        final int slop = ViewConfiguration.get(this).getScaledTouchSlop();
        tab.setOnTouchListener(new View.OnTouchListener() {
            float downY;
            int startY;
            boolean moved;

            @Override
            public boolean onTouch(View v, MotionEvent e) {
                switch (e.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        downY = e.getRawY();
                        startY = tabLp.y;
                        moved = false;
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        float dy = e.getRawY() - downY;
                        if (Math.abs(dy) > slop) moved = true;
                        if (moved && tab != null) {
                            tabLp.y = startY + (int) dy;
                            wm.updateViewLayout(tab, tabLp);
                        }
                        return true;
                    case MotionEvent.ACTION_UP:
                        if (moved) {
                            Prefs.setMini(ZikrService.this, Prefs.mini(ZikrService.this), tabLp.y + dp(38));
                        } else {
                            vibrate(20);
                            restore();
                        }
                        return true;
                }
                return false;
            }
        });
        try {
            wm.addView(tab, tabLp);
        } catch (RuntimeException e) {
            tab = null;
        }
    }

    private void removeTab() {
        if (tab != null) {
            try {
                wm.removeView(tab);
            } catch (RuntimeException ignored) {
            }
            tab = null;
        }
    }

    private void countOne() {
        int n = Prefs.count(this) + 1;
        Prefs.setCount(this, n);
        Prefs.setLastZikr(this, System.currentTimeMillis());
        refreshTasbih();
        int t = Prefs.target(this);
        vibrate(t > 0 && n % t == 0 ? 350 : 18);
    }

    private void removeTasbih() {
        if (tasbih != null) {
            try {
                wm.removeView(tasbih);
            } catch (RuntimeException ignored) {
            }
            tasbih = null;
        }
    }

    private void vibrate(long ms) {
        Vibrator v = getSystemService(Vibrator.class);
        if (v != null && v.hasVibrator()) {
            v.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE));
        }
    }
}
