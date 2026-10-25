package com.ghora.zikrbar;

import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

import static com.ghora.zikrbar.Kit.*;

/**
 * The tasbih inside the app: the chosen dhikr on top (swipe left or right to change it), a row of all
 * adhkar, a curved goal picker (7, 10, 33 …) and a large area that counts every tap.
 */
public class TasbihActivity extends Activity {

    static int D_BG = 0xFF0D1B18, D_BG2 = 0xFF0F2B27, D_CARD = 0xFF1A2724, D_HEAD = 0xFF0F2A1C;
    static int D_TEXT = Theme.ov(0xFF), D_MUTED = Theme.ov(0x99), D_ACCENT = 0xFF63D3A0, D_GOLD = 0xFFE9C46A;
    static final int[] GOALS = {7, 10, 33, 41, 99, 100, 313, 500, 1000, 0};

    private Kit k;
    private List<String> list;
    private int idx;
    private TextView dhikr, sub;
    private LinearLayout chips;
    private HorizontalScrollView chipScroll;
    private CounterView counter;
    private TextView vibBtn, sndBtn;

    private SharedPreferences sp() {
        return getSharedPreferences("tasbih_app", Context.MODE_PRIVATE);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        L.init(this);
        k = new Kit(this);
        Theme.darkWindow(this);
        list = Prefs.list(this);
        idx = Math.floorMod(sp().getInt("idx", 0), list.size());

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutDirection(L.rtl() ? View.LAYOUT_DIRECTION_RTL : View.LAYOUT_DIRECTION_LTR);
        root.addView(header());
        root.addView(dhikrCard());
        root.addView(chipRow());

        TextView goalT = k.title(L.t("goal_set"), 18, D_TEXT);
        goalT.setGravity(Gravity.CENTER);
        goalT.setPadding(0, k.dp(14), 0, 0);
        root.addView(goalT);
        GoalArc arc = new GoalArc(this);
        root.addView(arc, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, k.dp(92)));

        counter = new CounterView(this);
        root.addView(counter, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));
        setContentView(root);
        select(idx, 0);
    }

    // ---------------------------------------------------------------- header
    private View header() {
        LinearLayout h = k.row();
        h.setBackgroundColor(D_HEAD);
        h.setPadding(k.dp(8), k.dp(10), k.dp(8), k.dp(10));
        TextView back = icon(L.rtl() ? "→" : "←", v -> finish());
        h.addView(back);
        TextView t = k.title(L.t("tasbih_t"), 22, D_TEXT);
        t.setPaddingRelative(k.dp(14), 0, 0, 0);
        h.addView(t, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        h.addView(icon("↻", v -> confirmReset()));
        vibBtn = icon("📳", v -> {
            sp().edit().putBoolean("vib", !vib()).apply();
            paintToggles();
        });
        h.addView(vibBtn);
        sndBtn = icon("🔊", v -> {
            sp().edit().putBoolean("snd", !snd()).apply();
            paintToggles();
        });
        h.addView(sndBtn);
        paintToggles();
        return h;
    }

    private TextView icon(String glyph, View.OnClickListener l) {
        TextView b = new TextView(this);
        b.setText(glyph);
        b.setTextSize(22);
        b.setTextColor(D_TEXT);
        b.setGravity(Gravity.CENTER);
        b.setBackground(k.rounded(Theme.ov(0x14), 14, 0));
        b.setOnClickListener(l);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(k.dp(44), k.dp(44));
        lp.setMarginStart(k.dp(6));
        b.setLayoutParams(lp);
        return b;
    }

    private boolean vib() {
        return sp().getBoolean("vib", true);
    }

    private boolean snd() {
        return sp().getBoolean("snd", false);
    }

    private void paintToggles() {
        vibBtn.setAlpha(vib() ? 1f : 0.35f);
        sndBtn.setText(snd() ? "🔊" : "🔇");
        sndBtn.setAlpha(snd() ? 1f : 0.5f);
    }

    private void confirmReset() {
        new AlertDialog.Builder(this)
                .setMessage(L.t("tas_reset_q"))
                .setPositiveButton(L.t("tas_reset"), (d, w) -> {
                    sp().edit().putInt("c_" + key(), 0).putInt("r_" + key(), 0).apply();
                    refresh();
                })
                .setNegativeButton(L.t("cancel"), null)
                .show();
    }

    // ---------------------------------------------------------------- dhikr card
    private View dhikrCard() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setGravity(Gravity.CENTER);
        c.setPadding(k.dp(18), k.dp(20), k.dp(18), k.dp(16));
        c.setBackground(k.glass(26));
        Theme.elev(c, k.dp(6));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, k.dp(150));
        lp.setMargins(k.dp(16), k.dp(16), k.dp(16), 0);
        c.setLayoutParams(lp);
        dhikr = k.arabic("", 30, D_TEXT);
        dhikr.setLineSpacing(0, 1.2f);
        dhikr.setMaxLines(3);
        dhikr.setEllipsize(android.text.TextUtils.TruncateAt.END);
        c.addView(dhikr, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));
        dhikr.setGravity(Gravity.CENTER);
        sub = k.strong("", 13, D_MUTED);
        sub.setGravity(Gravity.CENTER);
        c.addView(sub);

        // swipe left = next dhikr, swipe right = previous
        final float[] downX = new float[1];
        c.setOnTouchListener((v, e) -> {
            if (e.getAction() == MotionEvent.ACTION_DOWN) downX[0] = e.getX();
            if (e.getAction() == MotionEvent.ACTION_UP) {
                float dx = e.getX() - downX[0];
                if (Math.abs(dx) > k.dp(60)) select(idx + (dx < 0 ? 1 : -1), dx < 0 ? -1 : 1);
            }
            return true;
        });
        return c;
    }

    private View chipRow() {
        chipScroll = new HorizontalScrollView(this);
        chipScroll.setHorizontalScrollBarEnabled(false);
        chipScroll.setClipToPadding(false);
        chipScroll.setPadding(k.dp(12), 0, k.dp(12), 0);
        chips = new LinearLayout(this);
        chips.setOrientation(LinearLayout.HORIZONTAL);
        for (int i = 0; i < list.size(); i++) {
            final int n = i;
            TextView t = k.arabic(list.get(i), 15, D_ACCENT);
            t.setMaxLines(2);
            t.setEllipsize(android.text.TextUtils.TruncateAt.END);
            t.setPadding(k.dp(10), k.dp(6), k.dp(10), k.dp(6));
            t.setOnClickListener(v -> select(n, 0));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(k.dp(128), k.dp(70));
            lp.setMargins(k.dp(5), 0, k.dp(5), 0);
            chips.addView(t, lp);
        }
        chipScroll.addView(chips);
        LinearLayout.LayoutParams lp = k.matchWrap();
        lp.setMargins(0, k.dp(14), 0, 0);
        chipScroll.setLayoutParams(lp);
        return chipScroll;
    }

    private void select(int i, int dir) {
        idx = Math.floorMod(i, list.size());
        sp().edit().putInt("idx", idx).apply();
        for (int j = 0; j < chips.getChildCount(); j++) {
            boolean sel = j == idx;
            chips.getChildAt(j).setBackground(k.rounded(sel ? Theme.ov(0x33) : D_CARD, 20, sel ? D_GOLD : Theme.ov(0x10)));
            ((TextView) chips.getChildAt(j)).setTextColor(sel ? D_GOLD : D_ACCENT);
        }
        View chip = chips.getChildAt(idx);
        chipScroll.post(() -> chipScroll.smoothScrollTo(Math.max(0, chip.getLeft() - (chipScroll.getWidth() - chip.getWidth()) / 2), 0));
        String text = list.get(idx);
        dhikr.setText(text);
        dhikr.setTextSize(text.length() > 70 ? 19 : text.length() > 40 ? 23 : 30);
        if (dir != 0) {
            dhikr.setTranslationX(-dir * k.dp(90));
            dhikr.setAlpha(0f);
            dhikr.animate().translationX(0).alpha(1f).setDuration(260).setInterpolator(new DecelerateInterpolator()).start();
        }
        refresh();
    }

    // ---------------------------------------------------------------- counting
    private String key() {
        return Integer.toHexString(list.get(idx).hashCode());
    }

    int count() {
        return sp().getInt("c_" + key(), 0);
    }

    int rounds() {
        return sp().getInt("r_" + key(), 0);
    }

    int total() {
        return sp().getInt("t_" + key(), 0);
    }

    int goal() {
        return sp().getInt("goal", 33);
    }

    void setGoal(int g) {
        sp().edit().putInt("goal", g).apply();
        refresh();
    }

    void tap() {
        int c = count() + 1, r = rounds(), g = goal();
        boolean reached = g > 0 && c >= g;
        if (reached) {
            c = 0;
            r++;
        }
        sp().edit().putInt("c_" + key(), c).putInt("r_" + key(), r).putInt("t_" + key(), total() + 1).apply();
        Prefs.setLastZikr(this, System.currentTimeMillis());
        if (vib()) vibrate(reached ? 300 : 18);
        if (snd()) {
            AudioManager am = getSystemService(AudioManager.class);
            if (am != null) am.playSoundEffect(AudioManager.FX_KEY_CLICK, 0.7f);
        }
        if (reached) Toast.makeText(this, L.t("goal_done"), Toast.LENGTH_SHORT).show();
        refresh();
        counter.pulse();
    }

    private void refresh() {
        int g = goal();
        String goalS = g > 0 ? L.num(g) : "∞";
        sub.setText(L.f("tas_line", L.num(count()), goalS, L.num(total())));
        if (counter != null) counter.invalidate();
    }

    private void vibrate(long ms) {
        Vibrator v = getSystemService(Vibrator.class);
        if (v == null || !v.hasVibrator()) return;
        if (android.os.Build.VERSION.SDK_INT >= 26) {
            v.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            v.vibrate(ms);
        }
    }

    float sp(float v) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, v, getResources().getDisplayMetrics());
    }

    // ================================================================ goal picker on a curve
    final class GoalArc extends View {
        private final Paint num = new Paint(Paint.ANTI_ALIAS_FLAG), line = new Paint(Paint.ANTI_ALIAS_FLAG);
        private float offset;
        private float downX, lastX;
        private boolean moved;
        private ValueAnimator snap;

        GoalArc(Context c) {
            super(c);
            num.setTypeface(k.strong);
            num.setTextAlign(Paint.Align.CENTER);
            line.setStyle(Paint.Style.STROKE);
            line.setStrokeWidth(k.dp(1.5f));
            line.setColor(Theme.ov(0x40));
            int g = goal();
            for (int i = 0; i < GOALS.length; i++) if (GOALS[i] == g) offset = i;
        }

        private float spacing() {
            return getWidth() / 4.2f;
        }

        @Override
        protected void onDraw(Canvas c) {
            float w = getWidth(), h = getHeight(), cx = w / 2f;
            Path p = new Path();
            p.moveTo(0, h * 0.98f);
            p.quadTo(cx, h * 0.48f, w, h * 0.98f);
            c.drawPath(p, line);
            for (int i = 0; i < GOALS.length; i++) {
                float dx = (i - offset) * spacing();
                float t = dx / (w / 2f);
                if (Math.abs(t) > 1.25f) continue;
                float y = h * 0.52f + t * t * h * 0.32f;
                float focus = Math.max(0f, 1f - Math.abs(i - offset));
                num.setTextSize(sp(22 + 12 * focus));
                num.setColor(focus > 0.5f ? D_TEXT : Theme.ov(0xCC));
                num.setAlpha((int) (255 * Math.max(0.25f, 1f - 0.55f * Math.abs(t))));
                c.save();
                c.rotate(t * 22f, cx + dx, y);
                c.drawText(GOALS[i] > 0 ? L.num(GOALS[i]) : "∞", cx + dx, y, num);
                c.restore();
                if (focus > 0.5f) {
                    num.setTextSize(sp(10));
                    num.setColor(D_GOLD);
                    c.drawCircle(cx + dx, y + k.dp(9), k.dp(2.5f), num);
                }
            }
        }

        @Override
        public boolean onTouchEvent(MotionEvent e) {
            switch (e.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    if (snap != null) snap.cancel();
                    downX = lastX = e.getX();
                    moved = false;
                    getParent().requestDisallowInterceptTouchEvent(true);
                    return true;
                case MotionEvent.ACTION_MOVE:
                    if (Math.abs(e.getX() - downX) > k.dp(6)) moved = true;
                    offset -= (e.getX() - lastX) / spacing();
                    offset = Math.max(0, Math.min(GOALS.length - 1, offset));
                    lastX = e.getX();
                    invalidate();
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    float target = Math.round(offset);
                    if (!moved) target = Math.max(0, Math.min(GOALS.length - 1,
                            Math.round(offset + (e.getX() - getWidth() / 2f) / spacing())));
                    animateTo(target);
                    return true;
            }
            return super.onTouchEvent(e);
        }

        private void animateTo(float target) {
            snap = ValueAnimator.ofFloat(offset, target);
            snap.setDuration(220);
            snap.setInterpolator(new DecelerateInterpolator());
            snap.addUpdateListener(a -> {
                offset = (float) a.getAnimatedValue();
                invalidate();
            });
            snap.start();
            int g = GOALS[(int) target];
            if (g != goal()) {
                setGoal(g);
                if (vib()) vibrate(10);
            }
        }
    }

    // ================================================================ big tap area
    final class CounterView extends View {
        private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG), ring = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
        private float press;
        private ValueAnimator anim;

        CounterView(Context c) {
            super(c);
            ring.setStyle(Paint.Style.STROKE);
            ring.setStrokeCap(Paint.Cap.ROUND);
            text.setTextAlign(Paint.Align.CENTER);
            text.setTypeface(k.strong);
        }

        void pulse() {
            if (anim != null) anim.cancel();
            anim = ValueAnimator.ofFloat(1f, 0f);
            anim.setDuration(380);
            anim.addUpdateListener(a -> {
                press = (float) a.getAnimatedValue();
                invalidate();
            });
            anim.start();
        }

        @Override
        protected void onDraw(Canvas c) {
            float w = getWidth(), h = getHeight(), cx = w / 2f;
            // curved lighter panel
            Path bg = new Path();
            bg.moveTo(0, h * 0.14f);
            bg.quadTo(cx, -h * 0.06f, w, h * 0.14f);
            bg.lineTo(w, h);
            bg.lineTo(0, h);
            bg.close();
            fill.setShader(new LinearGradient(0, 0, 0, h, Theme.ov(0x1A), Theme.ov(0x05), Shader.TileMode.CLAMP));
            c.drawPath(bg, fill);
            fill.setShader(null);

            float cy = h * 0.54f;
            float r = Math.min(w, h) * 0.30f;

            // tap ripple
            if (press > 0) {
                fill.setColor(D_ACCENT);
                fill.setAlpha((int) (60 * press));
                c.drawCircle(cx, cy, r * (1.25f - 0.15f * press), fill);
            }

            // progress towards the goal
            int g = goal();
            ring.setStrokeWidth(k.dp(5));
            ring.setColor(Theme.ov(0x1F));
            RectF oval = new RectF(cx - r * 1.18f, cy - r * 1.18f, cx + r * 1.18f, cy + r * 1.18f);
            c.drawArc(oval, 0, 360, false, ring);
            if (g > 0) {
                ring.setColor(D_GOLD);
                c.drawArc(oval, -90, 360f * count() / g, false, ring);
            }

            // fingerprint lines
            ring.setStrokeWidth(k.dp(3.2f));
            ring.setColor(Theme.ov(0x3D));
            for (int i = 0; i < 6; i++) {
                float rr = r * (0.28f + i * 0.13f);
                RectF o = new RectF(cx - rr * 0.82f, cy - rr, cx + rr * 0.82f, cy + rr);
                float start = 200 + (i % 2) * 18 - i * 4;
                c.drawArc(o, start, 150 - i * 6, false, ring);
                c.drawArc(o, start + 175 - i * 3, 90 + i * 9, false, ring);
            }

            // number
            text.setColor(D_TEXT);
            text.setTextSize(r * (count() > 999 ? 0.62f : 0.9f));
            Paint.FontMetrics fm = text.getFontMetrics();
            c.drawText(L.num(count()), cx, cy - (fm.ascent + fm.descent) / 2f, text);

            text.setTextSize(sp(13));
            text.setColor(D_MUTED);
            c.drawText(L.f("tas_rounds", L.num(rounds())), cx, Math.min(h - k.dp(14), cy + r * 1.18f + k.dp(30)), text);
        }

        @Override
        public boolean onTouchEvent(MotionEvent e) {
            if (e.getAction() == MotionEvent.ACTION_DOWN) {
                tap();
                return true;
            }
            return e.getAction() == MotionEvent.ACTION_UP || super.onTouchEvent(e);
        }
    }
}
