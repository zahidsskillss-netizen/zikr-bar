package com.ghora.zikrbar;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.graphics.PointF;
import android.graphics.SurfaceTexture;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.Icon;
import android.hardware.Camera;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.Settings;
import android.view.Gravity;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceContour;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;

import java.util.List;

/**
 * Counts dhikr without touching the phone: the front camera watches the face and each blink
 * (or each dhikr said with the lips) adds one to the count.
 * The camera picture is never shown, saved or sent. All that appears is a tiny light and the count
 * at the top of the screen, under the camera, with a small × that stops it.
 */
@SuppressWarnings("deprecation") // android.hardware.Camera keeps this working back to Android 6 with no extra libraries
public class FaceService extends Service {

    static final String ACTION_STOP = "com.ghora.zikrbar.FACE_STOP";
    private static final int NOTIF_ID = 77;
    private static final String CHANNEL = "zikr_face";
    private static final int BLINK = 0, MOUTH = 1;

    private static final int DOT_IDLE = 0xFF3FBF86, DOT_NO_FACE = 0xFFE0A030, DOT_FLASH = 0xFFFFFFFF;

    /** True while the counter runs (the settings switch reads this). */
    static volatile boolean running;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private WindowManager wm;

    // the tiny light + number
    private View pill, dot;
    private TextView num;

    // camera
    private Camera cam;
    private SurfaceTexture dummy;
    private int camOrient, pw, ph;
    private boolean busy, screenOn = true;
    private FaceDetector detector;
    private int mode;

    // blink
    private boolean eyesOpen = true;
    private long closedAt;
    // lips
    private float baseline = -1, smooth = -1, lastGap;
    private boolean talking;
    private long talkStart, lastActive;

    private long lastCount;
    private boolean faceSeen = true;

    private final BroadcastReceiver screenReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context c, Intent i) {
            if (Intent.ACTION_SCREEN_OFF.equals(i.getAction())) {
                screenOn = false;
                closeCamera();
            } else {
                screenOn = true;
                openCamera();
            }
        }
    };

    // ---------------------------------------------------------------- lifecycle
    static void start(Context c) {
        Ui.startZikrService(c, new Intent(c, FaceService.class));
    }

    static void stop(Context c) {
        c.stopService(new Intent(c, FaceService.class));
    }

    @Override
    public void onCreate() {
        super.onCreate();
        L.init(this);
        wm = getSystemService(WindowManager.class);
        if (Build.VERSION.SDK_INT >= 26) createChannel();
        IntentFilter f = new IntentFilter(Intent.ACTION_SCREEN_OFF);
        f.addAction(Intent.ACTION_SCREEN_ON);
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(screenReceiver, f, Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(screenReceiver, f);
        }
    }

    @android.annotation.TargetApi(26)
    private void createChannel() {
        NotificationChannel ch = new NotificationChannel(CHANNEL, L.t("face_t"), NotificationManager.IMPORTANCE_LOW);
        ch.setShowBadge(false);
        ch.setSound(null, null);
        ch.enableVibration(false);
        getSystemService(NotificationManager.class).createNotificationChannel(ch);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        L.init(this);
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            stopSelf();
            return START_NOT_STICKY;
        }
        if (running) return START_NOT_STICKY;

        Notification n = notification();
        try {
            if (Build.VERSION.SDK_INT >= 30) {
                startForeground(NOTIF_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA);
            } else {
                startForeground(NOTIF_ID, n);
            }
        } catch (RuntimeException e) {
            stopSelf();
            return START_NOT_STICKY;
        }
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
                || !Settings.canDrawOverlays(this)) {
            stopSelf();
            return START_NOT_STICKY;
        }

        running = true;
        Prefs.setFaceOn(this, true);
        mode = Prefs.faceMode(this) == MOUTH ? MOUTH : BLINK;
        if (Prefs.faceSound(this)) Krr.prepare(this);
        showPill();
        newDetector();
        openCamera();
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        running = false;
        Prefs.setFaceOn(this, false);
        try {
            unregisterReceiver(screenReceiver);
        } catch (RuntimeException ignored) {
        }
        closeCamera();
        if (detector != null) detector.close();
        detector = null;
        handler.removeCallbacksAndMessages(null);
        removePill();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private Notification notification() {
        PendingIntent open = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class),
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent off = PendingIntent.getService(this, 3,
                new Intent(this, FaceService.class).setAction(ACTION_STOP),
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        Icon icon = Icon.createWithBitmap(Ui.allahIcon(this));
        return Ui.notif(this, CHANNEL, true)
                .setSmallIcon(icon)
                .setContentTitle(L.t("face_t"))
                .setContentText(L.t(Prefs.faceMode(this) == MOUTH ? "face_ok_mouth" : "face_ok_blink"))
                .setColor(0xFF1B5E20)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setShowWhen(false)
                .setContentIntent(open)
                .addAction(new Notification.Action.Builder(icon, L.t("face_stop"), off).build())
                .build();
    }

    // ---------------------------------------------------------------- the tiny light and number
    private int dp(float v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }

    private void showPill() {
        if (pill != null) return;
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(6), 0, 0, 0);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0x99000000);
        bg.setCornerRadius(dp(9));
        row.setBackground(bg);

        dot = new View(this);
        GradientDrawable od = new GradientDrawable();
        od.setShape(GradientDrawable.OVAL);
        od.setColor(0xFFFFFFFF);
        dot.setBackground(od);
        dot.setBackgroundTintList(android.content.res.ColorStateList.valueOf(DOT_IDLE));
        LinearLayout.LayoutParams dl = new LinearLayout.LayoutParams(dp(6), dp(6));
        dl.setMarginEnd(dp(4));
        row.addView(dot, dl);

        num = new TextView(this);
        num.setTextSize(9);
        num.setTextColor(0xFFFFFFFF);
        num.setTypeface(Typeface.MONOSPACE);
        num.setIncludeFontPadding(false);
        num.setText(L.num(Prefs.count(this)));
        row.addView(num);

        TextView x = new TextView(this);
        x.setText("×");
        x.setTextSize(12);
        x.setTextColor(0xFFDDDDDD);
        x.setIncludeFontPadding(false);
        x.setGravity(Gravity.CENTER);
        x.setPadding(dp(7), dp(4), dp(8), dp(4));
        x.setOnClickListener(v -> stopSelf());
        row.addView(x);

        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT,
                Ui.overlayType(),
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        lp.y = Math.max(0, statusBarHeight() - dp(3)); // just under the front camera
        try {
            wm.addView(row, lp);
            pill = row;
        } catch (RuntimeException e) {
            pill = null;
            stopSelf();
        }
    }

    private int statusBarHeight() {
        int id = getResources().getIdentifier("status_bar_height", "dimen", "android");
        return id > 0 ? getResources().getDimensionPixelSize(id) : dp(24);
    }

    private void removePill() {
        if (pill != null) {
            try {
                wm.removeView(pill);
            } catch (RuntimeException ignored) {
            }
            pill = null;
        }
    }

    private void setDot(int color) {
        if (dot != null) dot.setBackgroundTintList(android.content.res.ColorStateList.valueOf(color));
    }

    /** The light blinks white with each counted dhikr, then goes back to green. */
    private void flash() {
        setDot(DOT_FLASH);
        dot.setScaleX(1.9f);
        dot.setScaleY(1.9f);
        dot.animate().cancel();
        dot.animate().scaleX(1f).scaleY(1f).setDuration(260).withEndAction(() -> setDot(faceSeen ? DOT_IDLE : DOT_NO_FACE)).start();
    }

    // ---------------------------------------------------------------- camera
    private void newDetector() {
        if (detector != null) detector.close();
        FaceDetectorOptions.Builder b = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .setMinFaceSize(0.15f);
        if (mode == BLINK) {
            b.setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL);
        } else {
            b.setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL);
        }
        detector = FaceDetection.getClient(b.build());
    }

    private void openCamera() {
        if (cam != null || !running || !screenOn) return;
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            stopSelf();
            return;
        }
        int id = -1;
        Camera.CameraInfo info = new Camera.CameraInfo();
        for (int i = 0; i < Camera.getNumberOfCameras(); i++) {
            Camera.getCameraInfo(i, info);
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                id = i;
                camOrient = info.orientation;
                break;
            }
        }
        if (id < 0) {
            Toast.makeText(this, L.t("face_no_cam"), Toast.LENGTH_LONG).show();
            stopSelf();
            return;
        }
        try {
            cam = Camera.open(id);
            Camera.Parameters pr = cam.getParameters();
            Camera.Size best = null;
            for (Camera.Size s : pr.getSupportedPreviewSizes()) {
                int score = Math.abs(s.width * s.height - 640 * 480) + (s.width * 3 == s.height * 4 ? 0 : 200000);
                if (best == null || score < Math.abs(best.width * best.height - 640 * 480) + (best.width * 3 == best.height * 4 ? 0 : 200000)) {
                    best = s;
                }
            }
            if (best != null) pr.setPreviewSize(best.width, best.height);
            pr.setPreviewFormat(ImageFormat.NV21);
            List<String> fm = pr.getSupportedFocusModes();
            if (fm != null && fm.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
                pr.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
            }
            cam.setParameters(pr);
            Camera.Size s = cam.getParameters().getPreviewSize();
            pw = s.width;
            ph = s.height;
            // the picture goes to an off-screen texture nobody sees; only the frames are looked at
            dummy = new SurfaceTexture(11);
            cam.setPreviewTexture(dummy);
            int size = pw * ph * 3 / 2;
            cam.addCallbackBuffer(new byte[size]);
            cam.addCallbackBuffer(new byte[size]);
            cam.setPreviewCallbackWithBuffer((data, c) -> onFrame(data));
            cam.startPreview();
        } catch (Exception e) {
            closeCamera();
            Toast.makeText(this, L.t("face_no_cam"), Toast.LENGTH_LONG).show();
            stopSelf();
        }
    }

    private void closeCamera() {
        if (cam != null) {
            try {
                cam.setPreviewCallbackWithBuffer(null);
                cam.stopPreview();
            } catch (RuntimeException ignored) {
            }
            cam.release();
            cam = null;
        }
        if (dummy != null) {
            dummy.release();
            dummy = null;
        }
        busy = false;
    }

    /** How far the picture must be turned so the face is upright, whichever way the phone is held. */
    private int rotation() {
        int deg = 0;
        try {
            int r = wm.getDefaultDisplay().getRotation();
            deg = r == Surface.ROTATION_90 ? 90 : r == Surface.ROTATION_180 ? 180 : r == Surface.ROTATION_270 ? 270 : 0;
        } catch (RuntimeException ignored) {
        }
        return (camOrient + deg) % 360;
    }

    private void onFrame(byte[] data) {
        final Camera c = cam;
        if (c == null || data == null) return;
        if (busy || detector == null) {
            c.addCallbackBuffer(data);
            return;
        }
        busy = true;
        InputImage img;
        try {
            img = InputImage.fromByteArray(data, pw, ph, rotation(), InputImage.IMAGE_FORMAT_NV21);
        } catch (RuntimeException e) {
            busy = false;
            c.addCallbackBuffer(data);
            return;
        }
        final int forMode = mode;
        detector.process(img)
                .addOnSuccessListener(faces -> {
                    if (forMode == mode && running) onFaces(faces);
                })
                .addOnCompleteListener(t -> {
                    busy = false;
                    if (cam == c) c.addCallbackBuffer(data);
                });
    }

    // ---------------------------------------------------------------- counting
    private void onFaces(List<Face> faces) {
        long now = SystemClock.uptimeMillis();
        boolean seen = faces != null && !faces.isEmpty();
        if (seen != faceSeen) {
            faceSeen = seen;
            setDot(seen ? DOT_IDLE : DOT_NO_FACE);
        }
        if (!seen) {
            finishTalk(now, true);
            return;
        }
        Face f = faces.get(0);
        for (Face o : faces) {
            if (o.getBoundingBox().width() > f.getBoundingBox().width()) f = o;
        }
        if (mode == BLINK) blink(f, now);
        else lips(f, now);
    }

    private void blink(Face f, long now) {
        Float l = f.getLeftEyeOpenProbability(), r = f.getRightEyeOpenProbability();
        if (l == null || r == null) return;
        float open = (l + r) / 2f;
        if (eyesOpen && open < 0.25f) {
            eyesOpen = false;
            closedAt = now;
        } else if (!eyesOpen && open > 0.6f) {
            eyesOpen = true;
            long dur = now - closedAt;
            // a real blink is short; keeping the eyes shut for long does not count
            if (dur >= 30 && dur <= 900 && now - lastCount > 250) count(now);
        }
    }

    /**
     * Lips: very easy to trigger. Any small opening (the front teeth showing) or ordinary lip movement
     * counts as saying the dhikr; one dhikr is added when the lips settle again for a moment.
     */
    private void lips(Face f, long now) {
        FaceContour up = f.getContour(FaceContour.UPPER_LIP_BOTTOM), lo = f.getContour(FaceContour.LOWER_LIP_TOP);
        if (up == null || lo == null) return;
        List<PointF> a = up.getPoints(), b = lo.getPoints();
        if (a.isEmpty() || b.isEmpty()) return;
        // average the gap over the middle of the lips (steadier than a single point)
        int n = Math.min(a.size(), b.size());
        int from = Math.max(0, n / 2 - 1), to = Math.min(n - 1, n / 2 + 1);
        float sum = 0;
        for (int i = from; i <= to; i++) sum += Math.abs(b.get(i).y - a.get(i).y);
        float gap = (sum / (to - from + 1)) / Math.max(1f, f.getBoundingBox().height());
        smooth = smooth < 0 ? gap : smooth + (gap - smooth) * 0.6f;

        if (baseline < 0 || smooth < baseline) baseline = smooth;
        else baseline += (smooth - baseline) * 0.015f;

        boolean open = smooth > baseline + 0.008f;
        boolean moving = Math.abs(smooth - lastGap) > 0.004f;
        lastGap = smooth;
        if (open || moving) {
            if (!talking) {
                talking = true;
                talkStart = now;
            }
            lastActive = now;
        } else {
            finishTalk(now, false);
        }
    }

    /** One dhikr is counted when the lips settle after moving or opening. */
    private void finishTalk(long now, boolean faceLost) {
        if (!talking) return;
        if (faceLost || now - lastActive > 400) {
            talking = false;
            if (lastActive - talkStart >= 90 && now - lastCount > 300) count(now);
        }
    }

    private void count(long now) {
        lastCount = now;
        int n = Prefs.count(this) + 1;
        Prefs.setCount(this, n);
        Prefs.setLastZikr(this, System.currentTimeMillis());
        int t = Prefs.target(this);
        boolean reached = t > 0 && n % t == 0;
        if (Prefs.faceSound(this)) Krr.play(this);
        vibrate(reached ? 350 : 15);
        if (num != null) num.setText(L.num(n));
        if (dot != null) flash();
        ZikrService.poke();
    }

    private void vibrate(long ms) {
        Vibrator v = getSystemService(Vibrator.class);
        if (v == null || !v.hasVibrator()) return;
        if (Build.VERSION.SDK_INT >= 26) {
            v.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            v.vibrate(ms);
        }
    }
}
