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

    /** The running service, so the eyes/lips counter can make the floating tasbih show a new count at once. */
    static ZikrService live;

    static void poke() {
        if (live != null) live.refreshTasbih();
    }

    static final String ACTION_REFRESH = "com.ghora.zikrbar.REFRESH";
    static final String ACTION_TOGGLE_TASBIH = "com.ghora.zikrbar.TOGGLE_TASBIH";
    static final String ACTION_SHOW_TASBIH = "com.ghora.zikrbar.SHOW_TASBIH";
    static final String ACTION_PREVIEW_UNLOCK = "com.ghora.zikrbar.PREVIEW_UNLOCK";
    static final String ACTION_PREVIEW_PERIODIC = "com.ghora.zikrbar.PREVIEW_PERIODIC";

    private static final String CHANNEL = "zikr_bar";
    private static final String CHANNEL_REMIND = "zikr_reminder";
    private static final int NOTIF_ID = 1;
    private static final int REMIND_ID = 2;


    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            Prefs.next(ZikrService.this);
            notifyMain();
            handler.postDelayed(this, Prefs.intervalMs(ZikrService.this));
        }
    };
    private final Runnable popupTick = new Runnable() {
        @Override
        public void run() {
            if (Prefs.popupOn(ZikrService.this)) {
                int hr = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
                if (hr >= 6 && hr < 21 && Settings.canDrawOverlays(ZikrService.this) && unlockPopup == null) showPeriodic();
                handler.postDelayed(this, Prefs.popupEvery(ZikrService.this) * 60_000L);
            }
        }
    };

    private void showPeriodic() {
        try {
            removeUnlockPopup();
            showShown(PeriodicStyles.build(this, Prefs.popupStyle(this), Prefs.periodicText(this), screen()));
        } catch (RuntimeException ignored) {
        }
    }

    private final Runnable reminderCheck = new Runnable() {
        @Override
        public void run() {
            checkReminder();
            registerUnlockReceiver(); // cheap insurance in case the phone dropped it
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

    private long lastShown;
    private boolean pollingUnlock;
    private boolean receiverOn;

    private final BroadcastReceiver unlockReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String a = intent.getAction();
            if (a == null) return;
            switch (a) {
                case Intent.ACTION_SCREEN_ON:
                    if (Prefs.unlockPopup(ZikrService.this)) startUnlockPolling();
                    break;
                case Intent.ACTION_SCREEN_OFF:
                    pollingUnlock = false;
                    handler.removeCallbacks(unlockPoll);
                    break;
                case Intent.ACTION_USER_PRESENT:
                    unlocked();
                    break;
                default:
                    break;
            }
        }
    };

    /**
     * Some phones send "unlocked" late or not at all, so besides USER_PRESENT we also watch the lock screen
     * ourselves for a while after the screen turns on and show the dhikr the moment it is gone.
     */
    private int pollTries;
    private final Runnable unlockPoll = new Runnable() {
        @Override
        public void run() {
            if (!pollingUnlock) return;
            boolean locked = true;
            try {
                android.app.KeyguardManager km = getSystemService(android.app.KeyguardManager.class);
                locked = km != null && km.isKeyguardLocked();
            } catch (RuntimeException ignored) {
                locked = false;
            }
            if (!locked) {
                pollingUnlock = false;
                unlocked();
                return;
            }
            if (++pollTries > 600) { // about a minute
                pollingUnlock = false;
                return;
            }
            handler.postDelayed(this, 100);
        }
    };

    private void startUnlockPolling() {
        pollingUnlock = true;
        pollTries = 0;
        handler.removeCallbacks(unlockPoll);
        handler.postDelayed(unlockPoll, 60);
        prepareUnlockPopup();
    }

    private void unlocked() {
        if (!Prefs.unlockPopup(this)) return;
        long now = android.os.SystemClock.elapsedRealtime();
        if (now - lastShown < 2500) return; // USER_PRESENT and our own check both fire: show once
        lastShown = now;
        pollingUnlock = false;
        handler.removeCallbacks(unlockPoll);
        handler.post(() -> {
            try {
                showUnlockPopup();
            } catch (RuntimeException e) {
                removeUnlockPopup();
            }
        });
    }

    private void registerUnlockReceiver() {
        if (receiverOn) {
            try {
                unregisterReceiver(unlockReceiver);
            } catch (RuntimeException ignored) {
            }
            receiverOn = false;
        }
        IntentFilter f = new IntentFilter(Intent.ACTION_USER_PRESENT);
        f.addAction(Intent.ACTION_SCREEN_ON);
        f.addAction(Intent.ACTION_SCREEN_OFF);
        try {
            if (Build.VERSION.SDK_INT >= 33) {
                registerReceiver(unlockReceiver, f, Context.RECEIVER_EXPORTED);
            } else {
                registerReceiver(unlockReceiver, f);
            }
            receiverOn = true;
        } catch (RuntimeException ignored) {
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        live = this;
        L.init(this);
        wm = getSystemService(WindowManager.class);
        if (Build.VERSION.SDK_INT >= 26) createChannels();

        registerUnlockReceiver();
    }

    /** Notification channels exist only on Android 8+. */
    @android.annotation.TargetApi(26)
    private void createChannels() {
        NotificationManager nm = getSystemService(NotificationManager.class);
        NotificationChannel ch = new NotificationChannel(CHANNEL, L.t("app_name"), NotificationManager.IMPORTANCE_LOW);
        ch.setShowBadge(false);
        ch.setSound(null, null);
        ch.enableVibration(false);
        nm.createNotificationChannel(ch);
        NotificationChannel rem = new NotificationChannel(CHANNEL_REMIND, L.t("c_rem_t"), NotificationManager.IMPORTANCE_DEFAULT);
        rem.setShowBadge(false);
        nm.createNotificationChannel(rem);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        L.init(this);
        String action = intent != null ? intent.getAction() : null;
        if (ACTION_TOGGLE_TASBIH.equals(action)) {
            boolean on = !Prefs.overlay(this);
            Prefs.setOverlay(this, on);
            if (on) Prefs.setMini(this, 0, Prefs.miniY(this));
        } else if (ACTION_PREVIEW_UNLOCK.equals(action)) {
            if (Settings.canDrawOverlays(this)) handler.postDelayed(this::showUnlockPopup, 120);
        } else if (ACTION_PREVIEW_PERIODIC.equals(action)) {
            if (Settings.canDrawOverlays(this)) handler.postDelayed(this::showPeriodic, 120);
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
        BootReceiver.scheduleWatchdog(this);
        registerUnlockReceiver();
        if (Prefs.lastZikr(this) == 0) Prefs.setLastZikr(this, System.currentTimeMillis());

        syncOverlay();
        handler.removeCallbacks(tick);
        handler.postDelayed(tick, Prefs.intervalMs(this));
        handler.removeCallbacks(reminderCheck);
        handler.postDelayed(reminderCheck, 60_000);
        handler.removeCallbacks(popupTick);
        if (Prefs.popupOn(this)) handler.postDelayed(popupTick, Prefs.popupEvery(this) * 60_000L);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        try {
            unregisterReceiver(unlockReceiver);
        } catch (RuntimeException ignored) {
        }
        receiverOn = false;
        if (Prefs.running(this)) BootReceiver.restartSoon(this, 3000);
        removeUnlockPopup();
        handler.removeCallbacksAndMessages(null);
        removeTasbih();
        removeTab();
        hideClose();
        Krr.release();
        if (live == this) live = null;
        super.onDestroy();
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        // swiping the app away must not stop the dhikr: ask to be started again right away
        if (Prefs.running(this)) BootReceiver.restartSoon(this, 1500);
        super.onTaskRemoved(rootIntent);
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
        String toggleLabel = Prefs.overlay(this) ? L.t("tas_hide") : L.t("tas_show");

        return Ui.notif(this, CHANNEL, true)
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
        int mins = Prefs.reminderMinutes(this);
        if (mins <= 0) return;
        int hours = mins / 60;
        long now = System.currentTimeMillis();
        long gap = mins * 60_000L;
        if (now - Prefs.lastZikr(this) < gap) return;
        if (now - Prefs.lastReminder(this) < gap) return;
        int hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
        if (hour >= 21 || hour < 6) return; // quiet at night (9pm – 6am)

        PendingIntent show = PendingIntent.getService(this, 2,
                new Intent(this, ZikrService.class).setAction(ACTION_SHOW_TASBIH),
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        String body = mins % 60 != 0 ? L.f("rem_nmin", L.num(mins))
                : hours == 1 ? L.t("rem_1h")
                : L.f("rem_nh", L.num(hours));
        Notification n = Ui.notif(this, CHANNEL_REMIND, false)
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
        return Ui.allahIcon(this);
    }

    // ---------------- unlock popup ----------------

    /**
     * Shows the dhikr for 2 seconds after the phone is unlocked: three glass panels at different depths
     * fly in from far away, drift gently in 3D, then fly past the viewer.
     */
    private UnlockStyles.Shown styled;

    private void showUnlockPopup() {
        removeUnlockPopup();
        int style;
        if (prebuiltStyle >= 0) {
            style = prebuiltStyle;
            prebuiltStyle = -1;
        } else {
            style = Prefs.unlockStyleNow(this);
        }
        if (style != UnlockStyles.LAYERS) {
            showStyled(style);
            return;
        }
        prebuilt = null;
        Kit k = new Kit(this);
        Point s = screen();
        int cw = Math.min(s.x - k.dp(64), k.dp(300));
        int ch = k.dp(250);
        int depth = k.dp(16);

        android.widget.FrameLayout root = new android.widget.FrameLayout(this);
        root.setClipChildren(false);
        root.setClipToPadding(false);
        root.setPadding(k.dp(44), k.dp(44), k.dp(44) + depth, k.dp(44) + depth);

        android.widget.FrameLayout stack = new android.widget.FrameLayout(this);
        stack.setClipChildren(false);
        root.addView(stack, new android.widget.FrameLayout.LayoutParams(cw + depth, ch + depth));

        // back layer (deepest)
        View l1 = new View(this);
        android.graphics.drawable.GradientDrawable g1 = new android.graphics.drawable.GradientDrawable();
        g1.setColor(0x991B5E20);
        g1.setCornerRadius(k.dp(26));
        g1.setStroke(k.dp(1), 0x55C9A227);
        l1.setBackground(g1);
        android.widget.FrameLayout.LayoutParams p1 = new android.widget.FrameLayout.LayoutParams(cw, ch);
        p1.leftMargin = depth;
        p1.topMargin = depth;
        stack.addView(l1, p1);

        // middle layer
        View l2 = new View(this);
        android.graphics.drawable.GradientDrawable g2 = new android.graphics.drawable.GradientDrawable();
        g2.setColor(0xB32E7D32);
        g2.setCornerRadius(k.dp(26));
        g2.setStroke(k.dp(1), 0x88C9A227);
        l2.setBackground(g2);
        android.widget.FrameLayout.LayoutParams p2 = new android.widget.FrameLayout.LayoutParams(cw, ch);
        p2.leftMargin = depth / 2;
        p2.topMargin = depth / 2;
        stack.addView(l2, p2);

        // front glass panel with the dhikr
        android.widget.LinearLayout front = new android.widget.LinearLayout(this);
        front.setOrientation(android.widget.LinearLayout.VERTICAL);
        front.setGravity(Gravity.CENTER);
        front.setPadding(k.dp(20), k.dp(16), k.dp(20), k.dp(18));
        android.graphics.drawable.GradientDrawable glass = new android.graphics.drawable.GradientDrawable(
                android.graphics.drawable.GradientDrawable.Orientation.TL_BR, new int[]{0xF0236B3A, 0xF00B3319});
        glass.setCornerRadius(k.dp(26));
        glass.setStroke(k.dp(1.5f), 0xCCF3E3A5);
        android.graphics.drawable.GradientDrawable shine = new android.graphics.drawable.GradientDrawable(
                android.graphics.drawable.GradientDrawable.Orientation.TL_BR, new int[]{0x30FFFFFF, 0x00FFFFFF, 0x00FFFFFF});
        shine.setCornerRadius(k.dp(26));
        front.setBackground(new android.graphics.drawable.LayerDrawable(new android.graphics.drawable.Drawable[]{glass, shine}));
        front.setElevation(k.dp(14));

        android.widget.TextView glyph = k.arabic("\uFDF2", 40, Kit.GOLD);
        glyph.setIncludeFontPadding(false);
        front.addView(glyph);
        android.widget.TextView zk = k.arabic(Prefs.nextUnlockText(this), 25, Color.WHITE);
        zk.setPadding(0, k.dp(10), 0, k.dp(6));
        zk.setLineSpacing(0, 1.12f);
        zk.setMaxLines(4);
        front.addView(zk);
        front.addView(k.arabic("فَاذْكُرُونِي أَذْكُرْكُمْ", 15, 0xD9F3E3A5));
        stack.addView(front, new android.widget.FrameLayout.LayoutParams(cw, ch));

        WindowManager.LayoutParams p = overlayParams(0);
        p.gravity = Gravity.CENTER;
        root.setOnClickListener(v -> removeUnlockPopup());

        float cam = 9000 * getResources().getDisplayMetrics().density;
        stack.setCameraDistance(cam);
        stack.setPivotX((cw + depth) / 2f);
        stack.setPivotY((ch + depth) / 2f);
        // start: far away, tilted (almost invisible first, so the first heavy draw is done before it moves)
        stack.setAlpha(0.01f);
        stack.setScaleX(0.35f);
        stack.setScaleY(0.35f);
        stack.setRotationX(30f);
        stack.setRotationY(-40f);
        l1.setTranslationX(k.dp(28));
        l1.setTranslationY(k.dp(28));
        l2.setTranslationX(k.dp(14));
        l2.setTranslationY(k.dp(14));
        front.setScaleX(0.9f);
        front.setScaleY(0.9f);

        try {
            wm.addView(root, p);
        } catch (RuntimeException e) {
            return;
        }
        unlockPopup = root;

        final android.view.animation.Interpolator out = new android.view.animation.DecelerateInterpolator(2.2f);
        handler.postDelayed(() -> {
            if (unlockPopup != root) return;
            // fly in, then keep drifting smoothly
            stack.animate().alpha(1f).scaleX(1f).scaleY(1f).rotationX(10f).rotationY(-18f).setDuration(520).setInterpolator(out)
                    .withEndAction(() -> stack.animate().rotationX(-6f).rotationY(16f).setDuration(1500)
                            .setInterpolator(new android.view.animation.AccelerateDecelerateInterpolator()).start())
                    .start();
            l1.animate().translationX(0).translationY(0).setDuration(620).setInterpolator(out).start();
            l2.animate().translationX(0).translationY(0).setDuration(560).setInterpolator(out).start();
            front.animate().scaleX(1f).scaleY(1f).setDuration(520).setInterpolator(out).start();
        }, 50);

        final View shown = root;
        handler.postDelayed(() -> {
            if (unlockPopup != shown) return;
            // fly past the viewer
            stack.animate().cancel();
            stack.animate().alpha(0f).scaleX(1.45f).scaleY(1.45f).rotationX(-10f).rotationY(30f).setDuration(320)
                    .setInterpolator(new android.view.animation.AccelerateInterpolator(1.6f))
                    .withEndAction(() -> {
                        if (unlockPopup == shown) removeUnlockPopup();
                    }).start();
        }, 2150);
    }

    /** One of the newer glass designs (see UnlockStyles). */
    private UnlockStyles.Shown prebuilt;
    private int prebuiltStyle = -1;

    /** Builds the popup while the phone is still locked, so it can appear the instant it is unlocked. */
    private void prepareUnlockPopup() {
        try {
            int style = Prefs.unlockStyleNow(this);
            prebuilt = null;
            prebuiltStyle = -1;
            if (style == UnlockStyles.LAYERS) {
                prebuiltStyle = style;
                return;
            }
            prebuilt = UnlockStyles.build(this, style, Prefs.nextUnlockText(this), screen(), Prefs.unlockSizeMul(this));
            prebuiltStyle = style;
        } catch (RuntimeException e) {
            prebuilt = null;
            prebuiltStyle = -1;
        }
    }

    private void showStyled(int style) {
        UnlockStyles.Shown sh = prebuilt;
        prebuilt = null;
        if (sh == null) sh = UnlockStyles.build(this, style, Prefs.nextUnlockText(this), screen(), Prefs.unlockSizeMul(this));
        showShown(sh);
    }

    private void showShown(UnlockStyles.Shown sh) {
        WindowManager.LayoutParams p = overlayParams(0);
        p.gravity = sh.gravity;
        p.y = sh.y;
        if (sh.fullWidth) p.width = WindowManager.LayoutParams.MATCH_PARENT;
        sh.root.setOnClickListener(v -> {
            if (unlockPopup == sh.root) sh.exit.run(this::removeUnlockPopup);
        });
        // nearly invisible at first so the heavy first draw happens before the animation starts
        sh.root.setAlpha(0.01f);
        try {
            wm.addView(sh.root, p);
        } catch (RuntimeException e) {
            return;
        }
        unlockPopup = sh.root;
        styled = sh;
        final View shown = sh.root;
        handler.postDelayed(() -> {
            if (unlockPopup != shown) return;
            shown.setAlpha(1f);
            sh.enter.run();
        }, 50);
        handler.postDelayed(() -> {
            if (unlockPopup == shown) sh.exit.run(() -> {
                if (unlockPopup == shown) removeUnlockPopup();
            });
        }, 950 + sh.hold);
    }

    private void removeUnlockPopup() {
        if (styled != null) {
            styled.stopLoops();
            styled = null;
        }
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
                Ui.overlayType(),
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
        tasbih.setOnNotch(n -> onRollNotches(n));
        tasbih.setSize(scale);
        if (TasbihView.isRollerStyle(Prefs.tasbihStyle(this))) Krr.prepare(this);
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
            float downX, downY, lastY;
            int startX, startY;
            boolean moved, longDone, pinching, onText, swiping, roller, onGrip, rolling;
            android.view.VelocityTracker vt;
            final Runnable longPress = () -> {
                longDone = true;
                Prefs.setCount(ZikrService.this, 0);
                refreshTasbih();
                vibrate(80);
                Toast.makeText(ZikrService.this, L.t("count_reset"), Toast.LENGTH_SHORT).show();
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
                        swiping = false;
                        onText = tasbih != null && tasbih.isTextHit(e.getX(), e.getY());
                        // side-view roller: rolling counts, and only the small handle under it moves it
                        roller = tasbih != null && tasbih.isRoller();
                        onGrip = roller && tasbih.isGripHit(e.getX(), e.getY());
                        rolling = roller && !onText && !onGrip && tasbih.isRollerHit(e.getX(), e.getY());
                        lastY = e.getRawY();
                        if (tasbih != null) tasbih.stopFling();
                        if (rolling) {
                            rollCounted = false;
                            if (vt != null) vt.recycle();
                            vt = android.view.VelocityTracker.obtain();
                            vt.addMovement(e);
                        }
                        if (!rolling) handler.postDelayed(longPress, 700);
                        return true;

                    case MotionEvent.ACTION_POINTER_DOWN:
                        pinching = true;
                        handler.removeCallbacks(longPress);
                        hideClose();
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        if (pinching || swiping) return true;
                        if (rolling) {
                            float step = e.getRawY() - lastY;
                            lastY = e.getRawY();
                            if (vt != null) vt.addMovement(e);
                            int n = tasbih != null ? tasbih.roll(step) : 0;
                            if (n > 0) onRollNotches(n);
                            return true;
                        }
                        float dx = e.getRawX() - downX, dy = e.getRawY() - downY;
                        // sideways swipe on the dhikr text changes the dhikr instead of moving the tasbih
                        if (!moved && onText && Math.abs(dx) > slop && Math.abs(dx) > Math.abs(dy) * 1.2f) {
                            swiping = true;
                            handler.removeCallbacks(longPress);
                            return true;
                        }
                        if (!moved && Math.hypot(dx, dy) > slop) {
                            if (roller && !onGrip) {
                                handler.removeCallbacks(longPress);
                                return true;
                            }
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
                        if (rolling) {
                            rolling = false;
                            if (vt != null) {
                                vt.addMovement(e);
                                vt.computeCurrentVelocity(1000);
                                float vy = vt.getYVelocity();
                                vt.recycle();
                                vt = null;
                                if (tasbih != null) tasbih.fling(vy);
                            }
                            return true;
                        }
                        if (swiping) {
                            swiping = false;
                            float sx = e.getRawX() - downX;
                            if (Math.abs(sx) > dp(30)) nextTasbihZikr(sx < 0 ? 1 : -1);
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
                        } else if (!longDone && !onGrip && Math.hypot(e.getRawX() - downX, e.getRawY() - downY) <= slop) {
                            countOne();
                        }
                        return true;

                    case MotionEvent.ACTION_CANCEL:
                        handler.removeCallbacks(longPress);
                        hideClose();
                        pinching = false;
                        rolling = false;
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
        Toast.makeText(this, L.t("tas_closed"), Toast.LENGTH_LONG).show();
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

    /** Moves the floating tasbih to the next (dir 1) or previous (dir -1) dhikr and starts a new count. */
    private void nextTasbihZikr(int dir) {
        int size = Prefs.list(this).size();
        Prefs.setTasbihIndex(this, Math.floorMod(Prefs.tasbihIndex(this) + dir, size));
        Prefs.setCount(this, 0);
        refreshTasbih();
        vibrate(30);
        Toast.makeText(this, Prefs.tasbihText(this), Toast.LENGTH_SHORT).show();
    }

    private boolean rollCounted;
    private long lastRollFb;

    /**
     * A drag or flick on the roller: every notch that passes gives its own "krrr" and a small buzz,
     * but the whole swipe adds just one to the count.
     */
    private void onRollNotches(int n) {
        if (!rollCounted) {
            rollCounted = true;
            countRoll();
        }
        for (int i = 0; i < Math.min(n, 6); i++) handler.postDelayed(this::rollFeedback, i * 40L);
    }

    private void rollFeedback() {
        long now = android.os.SystemClock.uptimeMillis();
        if (now - lastRollFb < 22) return;
        lastRollFb = now;
        if (Prefs.rollSound(this)) Krr.play(this);
        if (Prefs.rollVib(this)) vibrate(8);
    }

    /** One dhikr counted by one swipe on the roller. */
    private void countRoll() {
        int n = Prefs.count(this) + 1;
        Prefs.setCount(this, n);
        Prefs.setLastZikr(this, System.currentTimeMillis());
        refreshTasbih();
        int t = Prefs.target(this);
        if (t > 0 && n % t == 0) vibrate(350);
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
            if (Build.VERSION.SDK_INT >= 26) {
                v.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                v.vibrate(ms);
            }
        }
    }
}
