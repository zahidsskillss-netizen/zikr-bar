package com.ghora.zikrbar;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.Outline;
import android.graphics.PointF;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.Gravity;
import android.view.TextureView;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
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
 * (or each dhikr said with the lips, followed by a short pause) adds one to the tasbih count.
 * Frames are only looked at on the phone; nothing is recorded, saved or sent.
 */
@SuppressWarnings("deprecation") // android.hardware.Camera keeps this working back to Android 6 with no extra libraries
public class FaceZikrActivity extends Activity implements TextureView.SurfaceTextureListener {

    private static final int REQ_CAM = 7;
    private static final int BLINK = 0, MOUTH = 1;

    private Kit k;
    private TextureView preview;
    private FrameLayout previewBox;
    private TextView dhikr, countView, goalView, status, level, modeBlink, modeMouth, soundBtn;

    private Camera cam;
    private int camOrient, pw, ph;
    private boolean busy;
    private FaceDetector detector;
    private int mode;

    // blink
    private boolean eyesOpen = true;
    private long closedAt;
    // lips
    private float baseline = -1, lastGap;
    private boolean talking;
    private long talkStart, lastActive;

    private long lastCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        L.init(this);
        k = new Kit(this);
        Theme.darkWindow(this);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mode = Prefs.faceMode(this) == MOUTH ? MOUTH : BLINK;

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutDirection(L.rtl() ? View.LAYOUT_DIRECTION_RTL : View.LAYOUT_DIRECTION_LTR);
        root.addView(header());

        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setGravity(Gravity.CENTER_HORIZONTAL);
        body.setPadding(k.dp(16), k.dp(12), k.dp(16), k.dp(24));

        LinearLayout zr = k.row();
        zr.setGravity(Gravity.CENTER);
        zr.addView(arrow("‹", -1));
        dhikr = k.arabic("", 24, TasbihActivity.D_GOLD);
        dhikr.setGravity(Gravity.CENTER);
        zr.addView(dhikr, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        zr.addView(arrow("›", 1));
        body.addView(zr, k.matchWrap());

        previewBox = new FrameLayout(this);
        previewBox.setBackground(k.rounded(0xFF000000, 24, 0));
        previewBox.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View v, Outline o) {
                o.setRoundRect(0, 0, v.getWidth(), v.getHeight(), k.dp(24));
            }
        });
        previewBox.setClipToOutline(true);
        preview = new TextureView(this);
        preview.setSurfaceTextureListener(this);
        previewBox.addView(preview, new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        int w = Math.round(getResources().getDisplayMetrics().widthPixels * 0.58f);
        LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(w, Math.round(w * 4f / 3f));
        plp.topMargin = k.dp(12);
        body.addView(previewBox, plp);

        status = k.text("", 14, TasbihActivity.D_MUTED);
        status.setGravity(Gravity.CENTER);
        status.setPadding(0, k.dp(10), 0, 0);
        body.addView(status, k.matchWrap());

        level = k.text("", 13, TasbihActivity.D_ACCENT);
        level.setGravity(Gravity.CENTER);
        level.setPadding(0, k.dp(4), 0, 0);
        body.addView(level, k.matchWrap());

        countView = k.title("0", 64, TasbihActivity.D_TEXT);
        countView.setGravity(Gravity.CENTER);
        body.addView(countView, k.matchWrap());
        goalView = k.text("", 14, TasbihActivity.D_MUTED);
        goalView.setGravity(Gravity.CENTER);
        body.addView(goalView, k.matchWrap());

        LinearLayout modes = k.row();
        modes.setGravity(Gravity.CENTER);
        modeBlink = chip(L.t("face_blink"), v -> setMode(BLINK));
        modeMouth = chip(L.t("face_mouth"), v -> setMode(MOUTH));
        modes.addView(modeBlink);
        modes.addView(modeMouth);
        body.addView(modes, k.topMargin(14));

        ScrollView sv = new ScrollView(this);
        sv.addView(body);
        root.addView(sv, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));
        setContentView(root);

        paint();
        newDetector();
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, L.t("face_cam_perm"), Toast.LENGTH_LONG).show();
            requestPermissions(new String[]{Manifest.permission.CAMERA}, REQ_CAM);
        }
        if (Prefs.faceSound(this)) Krr.prepare(this);
    }

    // ---------------------------------------------------------------- views
    private View header() {
        LinearLayout h = k.row();
        h.setBackgroundColor(TasbihActivity.D_HEAD);
        h.setPadding(k.dp(8), k.dp(10), k.dp(8), k.dp(10));
        h.addView(icon(L.rtl() ? "→" : "←", v -> finish()));
        TextView t = k.title(L.t("face_t"), 20, TasbihActivity.D_TEXT);
        t.setPadding(k.dp(14), 0, k.dp(14), 0);
        h.addView(t, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        h.addView(icon("↻", v -> {
            Prefs.setCount(this, 0);
            paint();
            refreshService();
        }));
        soundBtn = icon("🔊", v -> {
            Prefs.setFaceSound(this, !Prefs.faceSound(this));
            paint();
        });
        h.addView(soundBtn);
        return h;
    }

    private TextView icon(String glyph, View.OnClickListener l) {
        TextView b = new TextView(this);
        b.setText(glyph);
        b.setTextSize(22);
        b.setTextColor(TasbihActivity.D_TEXT);
        b.setGravity(Gravity.CENTER);
        b.setBackground(k.rounded(Theme.ov(0x14), 14, 0));
        b.setOnClickListener(l);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(k.dp(44), k.dp(44));
        lp.setMarginStart(k.dp(6));
        b.setLayoutParams(lp);
        return b;
    }

    private TextView arrow(String s, int dir) {
        TextView a = k.title(s, 30, TasbihActivity.D_TEXT);
        a.setGravity(Gravity.CENTER);
        a.setPadding(k.dp(14), 0, k.dp(14), k.dp(4));
        a.setOnClickListener(v -> {
            int size = Prefs.list(this).size();
            int step = L.rtl() ? -dir : dir;
            Prefs.setTasbihIndex(this, Math.floorMod(Prefs.tasbihIndex(this) + step, size));
            Prefs.setCount(this, 0);
            paint();
            refreshService();
        });
        return a;
    }

    private TextView chip(String s, View.OnClickListener l) {
        TextView c = k.strong(s, 15, TasbihActivity.D_TEXT);
        c.setGravity(Gravity.CENTER);
        c.setPadding(k.dp(16), k.dp(10), k.dp(16), k.dp(10));
        c.setOnClickListener(l);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(k.dp(5), 0, k.dp(5), 0);
        c.setLayoutParams(lp);
        return c;
    }

    private void paint() {
        dhikr.setText(Prefs.tasbihText(this));
        countView.setText(L.num(Prefs.count(this)));
        int t = Prefs.target(this);
        goalView.setText(t > 0 ? "/ " + L.num(t) : "∞");
        modeBlink.setBackground(k.rounded(mode == BLINK ? 0x5563D3A0 : Theme.ov(0x14), 18, 0));
        modeMouth.setBackground(k.rounded(mode == MOUTH ? 0x5563D3A0 : Theme.ov(0x14), 18, 0));
        soundBtn.setText(Prefs.faceSound(this) ? "🔊" : "🔇");
        if (cam == null) status.setText(L.t("face_none"));
    }

    private void setMode(int m) {
        if (m == mode) return;
        mode = m;
        Prefs.setFaceMode(this, m);
        eyesOpen = true;
        talking = false;
        baseline = -1;
        newDetector();
        paint();
    }

    private void newDetector() {
        if (detector != null) detector.close();
        FaceDetectorOptions.Builder b = new FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .setMinFaceSize(0.2f);
        if (mode == BLINK) {
            b.setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL);
        } else {
            b.setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL);
        }
        detector = FaceDetection.getClient(b.build());
    }

    // ---------------------------------------------------------------- camera
    @Override
    protected void onResume() {
        super.onResume();
        paint();
        if (preview.isAvailable()) openCamera(preview.getSurfaceTexture());
    }

    @Override
    protected void onPause() {
        closeCamera();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (detector != null) detector.close();
        detector = null;
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_CAM && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED
                && preview.isAvailable()) {
            openCamera(preview.getSurfaceTexture());
        }
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture st, int w, int h) {
        openCamera(st);
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture st, int w, int h) {}

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture st) {
        closeCamera();
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture st) {}

    private void openCamera(SurfaceTexture st) {
        if (cam != null || st == null) return;
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) return;
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
            cam.setDisplayOrientation((360 - camOrient % 360) % 360);
            cam.setPreviewTexture(st);
            int size = pw * ph * 3 / 2;
            cam.addCallbackBuffer(new byte[size]);
            cam.addCallbackBuffer(new byte[size]);
            cam.setPreviewCallbackWithBuffer((data, c) -> onFrame(data));
            cam.startPreview();
            fitPreview();
        } catch (Exception e) {
            closeCamera();
            Toast.makeText(this, L.t("face_no_cam"), Toast.LENGTH_LONG).show();
        }
    }

    /** Keeps the picture's real shape (it is turned upright, so width and height swap). */
    private void fitPreview() {
        boolean sideways = camOrient % 180 != 0;
        float ratio = sideways ? pw / (float) ph : ph / (float) pw;
        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) previewBox.getLayoutParams();
        lp.height = Math.round(lp.width * ratio);
        previewBox.setLayoutParams(lp);
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
        busy = false;
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
            img = InputImage.fromByteArray(data, pw, ph, camOrient, InputImage.IMAGE_FORMAT_NV21);
        } catch (RuntimeException e) {
            busy = false;
            c.addCallbackBuffer(data);
            return;
        }
        final int forMode = mode;
        detector.process(img)
                .addOnSuccessListener(faces -> {
                    if (forMode == mode) onFaces(faces);
                })
                .addOnCompleteListener(t -> {
                    busy = false;
                    if (cam == c) c.addCallbackBuffer(data);
                });
    }

    // ---------------------------------------------------------------- counting
    private void onFaces(List<Face> faces) {
        long now = SystemClock.uptimeMillis();
        if (faces == null || faces.isEmpty()) {
            status.setText(L.t("face_none"));
            level.setText("");
            finishTalk(now, true);
            return;
        }
        Face f = faces.get(0);
        for (Face o : faces) {
            if (o.getBoundingBox().width() > f.getBoundingBox().width()) f = o;
        }
        status.setText(L.t(mode == BLINK ? "face_ok_blink" : "face_ok_mouth"));
        if (mode == BLINK) blink(f, now);
        else lips(f, now);
    }

    private void blink(Face f, long now) {
        Float l = f.getLeftEyeOpenProbability(), r = f.getRightEyeOpenProbability();
        if (l == null || r == null) return;
        float open = (l + r) / 2f;
        level.setText(L.t("face_eyes") + "  " + bar(open));
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

    private void lips(Face f, long now) {
        FaceContour up = f.getContour(FaceContour.UPPER_LIP_BOTTOM), lo = f.getContour(FaceContour.LOWER_LIP_TOP);
        if (up == null || lo == null || up.getPoints().isEmpty() || lo.getPoints().isEmpty()) return;
        PointF a = up.getPoints().get(up.getPoints().size() / 2);
        PointF b = lo.getPoints().get(lo.getPoints().size() / 2);
        float gap = Math.abs(b.y - a.y) / Math.max(1f, f.getBoundingBox().height());
        if (baseline < 0 || gap < baseline) baseline = gap;
        else baseline += (gap - baseline) * 0.01f;
        boolean open = gap > baseline + 0.03f;
        boolean moving = Math.abs(gap - lastGap) > 0.015f;
        lastGap = gap;
        level.setText(L.t("face_lips") + "  " + bar(Math.min(1f, (gap - baseline) / 0.08f)));
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

    /** One dhikr is counted when the lips stop after moving (a short pause between two dhikr). */
    private void finishTalk(long now, boolean faceLost) {
        if (!talking) return;
        if (faceLost || now - lastActive > 650) {
            talking = false;
            if (lastActive - talkStart >= 200 && now - lastCount > 300) count(now);
        }
    }

    private static String bar(float v) {
        int n = Math.max(0, Math.min(10, Math.round(v * 10)));
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < 10; i++) s.append(i < n ? '■' : '□');
        return s.toString();
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
        if (reached) Toast.makeText(this, L.t("goal_done"), Toast.LENGTH_SHORT).show();
        countView.setText(L.num(n));
        countView.animate().cancel();
        countView.setScaleX(1.15f);
        countView.setScaleY(1.15f);
        countView.animate().scaleX(1f).scaleY(1f).setDuration(180).start();
        refreshService();
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

    /** Lets the floating tasbih show the new count. */
    private void refreshService() {
        if (Prefs.running(this)) {
            Ui.startZikrService(this, new Intent(this, ZikrService.class).setAction(ZikrService.ACTION_REFRESH));
        }
    }
}
