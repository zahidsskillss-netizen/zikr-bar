package com.ghora.zikrbar;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Paint;
import android.graphics.drawable.RippleDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Calendar;

import static com.ghora.zikrbar.Kit.*;

/** The tasks page: write a task, pick its date and time quickly, and manage the tasks that are waiting. */
public class TasksActivity extends Activity {

    static final String EXTRA_CHANGE = "change";

    private interface OnTime { void on(long ms); }

    private Kit k;
    private EditText input;
    private TextView whenLabel;
    private LinearLayout notice, listBox;
    private final Calendar pick = Calendar.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        L.init(this);
        k = new Kit(this);
        Theme.window(this);
        pick.setTimeInMillis(Tasks.in(60));
        pick.set(Calendar.MINUTE, 0);
        pick.set(Calendar.SECOND, 0);
        pick.set(Calendar.MILLISECOND, 0);
        build();
        handleIntent(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleIntent(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshNotice();
        refreshList();
    }

    private void handleIntent(Intent i) {
        long id = i == null ? 0 : i.getLongExtra(EXTRA_CHANGE, 0);
        if (id == 0) return;
        i.removeExtra(EXTRA_CHANGE);
        Tasks.Task t = Tasks.get(this, id);
        if (t != null) pickDateTime(t.when, ms -> Tasks.reschedule(this, id, ms));
    }

    // ================= page =================

    private void build() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutDirection(L.rtl() ? View.LAYOUT_DIRECTION_RTL : View.LAYOUT_DIRECTION_LTR);
        root.setPadding(0, 0, 0, k.dp(32));

        LinearLayout h = new LinearLayout(this);
        h.setOrientation(LinearLayout.VERTICAL);
        h.setPadding(k.dp(18), k.dp(14), k.dp(18), k.dp(22));
        h.setBackground(k.headerBackground());
        TextView back = k.strong((L.rtl() ? "→  " : "←  ") + L.t("back"), 14, 0xE6FFFFFF);
        back.setPaddingRelative(k.dp(14), k.dp(6), k.dp(16), k.dp(8));
        back.setBackground(new RippleDrawable(ColorStateList.valueOf(0x40FFFFFF), k.rounded(0x26FFFFFF, 18, 0x40FFFFFF), null));
        back.setOnClickListener(v -> finish());
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        bp.gravity = Gravity.START;
        h.addView(back, bp);
        root.addView(h);

        notice = new LinearLayout(this);
        notice.setOrientation(LinearLayout.VERTICAL);
        root.addView(notice);

        root.addView(addCard());

        listBox = new LinearLayout(this);
        listBox.setOrientation(LinearLayout.VERTICAL);
        root.addView(listBox);

        ScrollView scroll = new ScrollView(this);
        scroll.addView(root);
        setContentView(scroll);
    }

    private View addCard() {
        LinearLayout c = k.card("✔", L.t("tk_title"), L.t("tk_sub"));

        input = new EditText(this);
        input.setHint(L.t("tk_hint"));
        input.setHintTextColor(MUTED);
        input.setTextColor(INK);
        input.setTypeface(k.body);
        input.setTextSize(17);
        input.setMinLines(2);
        input.setGravity(Gravity.TOP | Gravity.START);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        input.setBackground(k.rounded(SOFT, 16, LINE));
        input.setPadding(k.dp(14), k.dp(12), k.dp(14), k.dp(12));
        c.addView(input, k.topMargin(8));

        // the chosen date and time, always visible
        whenLabel = k.title("", 18, GREEN);
        whenLabel.setGravity(Gravity.CENTER);
        whenLabel.setPadding(k.dp(12), k.dp(12), k.dp(12), k.dp(14));
        whenLabel.setBackground(k.rounded(ON_CARD_DIM, 16, 0));
        c.addView(whenLabel, k.topMargin(12));

        c.addView(k.label(L.t("tk_date")));
        c.addView(scrollRow(
                chip(L.t("tk_today"), () -> setDay(0)),
                chip(L.t("tk_tomorrow"), () -> setDay(1)),
                chip(L.t("tk_after"), () -> setDay(2)),
                chip(L.t("tk_pickdate"), this::askDate)));

        c.addView(k.label(L.t("tk_time")));
        c.addView(scrollRow(
                chip(Tasks.nd("08:00"), () -> setTime(8, 0)),
                chip(Tasks.nd("12:00"), () -> setTime(12, 0)),
                chip(Tasks.nd("16:00"), () -> setTime(16, 0)),
                chip(Tasks.nd("20:00"), () -> setTime(20, 0)),
                chip(L.t("tk_picktime"), this::askTime)));
        LinearLayout rel = scrollRow(
                chip(L.f("tk_plus_min", L.num(10)), () -> setIn(10)),
                chip(L.f("tk_plus_min", L.num(30)), () -> setIn(30)),
                chip(L.f("tk_plus_h", L.num(1)), () -> setIn(60)),
                chip(L.f("tk_plus_h", L.num(3)), () -> setIn(180)));
        c.addView(rel, k.topMargin(8));

        c.addView(k.button(L.t("tk_add"), true, v -> addTask()), k.topMargin(16));
        updateLabel();
        return c;
    }

    // ================= choosing the time =================

    private void updateLabel() {
        long ms = pick.getTimeInMillis();
        boolean past = ms <= System.currentTimeMillis();
        whenLabel.setText("⏰   " + Tasks.when(this, ms));
        whenLabel.setTextColor(past ? 0xFFE5484D : GREEN);
    }

    private void setDay(int offset) {
        Calendar d = Calendar.getInstance();
        d.add(Calendar.DAY_OF_YEAR, offset);
        pick.set(d.get(Calendar.YEAR), d.get(Calendar.MONTH), d.get(Calendar.DAY_OF_MONTH));
        updateLabel();
    }

    private void setTime(int h, int m) {
        pick.set(Calendar.HOUR_OF_DAY, h);
        pick.set(Calendar.MINUTE, m);
        pick.set(Calendar.SECOND, 0);
        pick.set(Calendar.MILLISECOND, 0);
        updateLabel();
    }

    private void setIn(int minutes) {
        pick.setTimeInMillis(Tasks.in(minutes));
        pick.set(Calendar.SECOND, 0);
        pick.set(Calendar.MILLISECOND, 0);
        updateLabel();
    }

    private void askDate() {
        new DatePickerDialog(this, (view, y, m, d) -> {
            pick.set(y, m, d);
            updateLabel();
        }, pick.get(Calendar.YEAR), pick.get(Calendar.MONTH), pick.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void askTime() {
        new TimePickerDialog(this, (view, h, m) -> setTime(h, m),
                pick.get(Calendar.HOUR_OF_DAY), pick.get(Calendar.MINUTE), true).show();
    }

    /** Date first, then time. */
    private void pickDateTime(long initial, OnTime done) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(Math.max(initial, System.currentTimeMillis()));
        new DatePickerDialog(this, (view, y, m, d) -> {
            Calendar r = Calendar.getInstance();
            r.setTimeInMillis(c.getTimeInMillis());
            r.set(y, m, d);
            new TimePickerDialog(this, (tv, h, mi) -> {
                r.set(Calendar.HOUR_OF_DAY, h);
                r.set(Calendar.MINUTE, mi);
                r.set(Calendar.SECOND, 0);
                r.set(Calendar.MILLISECOND, 0);
                done.on(r.getTimeInMillis());
            }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true).show();
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
    }

    // ================= actions =================

    private void addTask() {
        String text = input.getText().toString().replace('\n', ' ').trim();
        if (text.isEmpty()) {
            Toast.makeText(this, L.t("tk_empty_text"), Toast.LENGTH_SHORT).show();
            return;
        }
        long ms = pick.getTimeInMillis();
        if (ms <= System.currentTimeMillis() + 20_000L) {
            Toast.makeText(this, L.t("tk_past"), Toast.LENGTH_LONG).show();
            return;
        }
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission("android.permission.POST_NOTIFICATIONS")
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"}, 9);
        }
        Tasks.add(this, text, ms);
        input.setText("");
        Toast.makeText(this, L.t("tk_saved") + "\n" + Tasks.when(this, ms), Toast.LENGTH_LONG).show();
        pick.setTimeInMillis(Tasks.in(60));
        pick.set(Calendar.MINUTE, 0);
        updateLabel();
        refreshList();
    }

    private void changeTime(Tasks.Task t) {
        CharSequence[] items = {
                L.f("tk_plus_min", L.num(10)), L.f("tk_plus_h", L.num(1)), L.t("tk_tonight"), L.t("tk_tomorrow8"), L.t("tk_custom")};
        new AlertDialog.Builder(this)
                .setTitle(L.t("tk_later"))
                .setItems(items, (d, which) -> {
                    if (which == 0) Tasks.reschedule(this, t.id, Tasks.in(10));
                    else if (which == 1) Tasks.reschedule(this, t.id, Tasks.in(60));
                    else if (which == 2) Tasks.reschedule(this, t.id, Tasks.tonight());
                    else if (which == 3) Tasks.reschedule(this, t.id, Tasks.tomorrowMorning());
                    else pickDateTime(t.when, ms -> {
                        Tasks.reschedule(this, t.id, ms);
                        refreshList();
                    });
                    refreshList();
                })
                .setNegativeButton(L.t("cancel"), null)
                .show();
    }

    // ================= pieces =================

    private TextView chip(String s, Runnable r) {
        TextView t = k.strong(s, 13.5f, INK);
        t.setGravity(Gravity.CENTER);
        t.setPadding(k.dp(14), k.dp(8), k.dp(14), k.dp(10));
        t.setBackground(new RippleDrawable(ColorStateList.valueOf(0x301B5E20), k.rounded(SOFT, 16, LINE), null));
        t.setClickable(true);
        t.setOnClickListener(v -> r.run());
        return t;
    }

    private LinearLayout scrollRow(View... chips) {
        LinearLayout outer = new LinearLayout(this);
        HorizontalScrollView hs = new HorizontalScrollView(this);
        hs.setHorizontalScrollBarEnabled(false);
        LinearLayout row = new LinearLayout(this);
        for (View v : chips) {
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            p.setMarginEnd(k.dp(8));
            row.addView(v, p);
        }
        hs.addView(row);
        outer.addView(hs, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));
        return outer;
    }

    /** A hint (with a button) when the phone does not yet allow what makes the task stay on the screen. */
    private void refreshNotice() {
        notice.removeAllViews();
        boolean overlay = Settings.canDrawOverlays(this);
        boolean alarms = Build.VERSION.SDK_INT < 31 || getSystemService(android.app.AlarmManager.class).canScheduleExactAlarms();
        if (overlay && alarms) return;
        LinearLayout c = k.card("⚠", L.t("tk_perm_t"), null);
        if (!overlay) {
            TextView p = k.text(L.t("tk_perm_overlay"), 14.5f, INK);
            p.setLineSpacing(0, 1.35f);
            c.addView(p, k.topMargin(4));
            c.addView(k.button(L.t("tk_perm_btn"), true, v -> startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + getPackageName())))), k.topMargin(10));
        }
        if (!alarms) {
            TextView p = k.text(L.t("tk_perm_alarm"), 14.5f, INK);
            p.setLineSpacing(0, 1.35f);
            c.addView(p, k.topMargin(12));
            c.addView(k.button(L.t("tk_perm_btn"), false, v -> {
                if (Build.VERSION.SDK_INT >= 31) {
                    try {
                        startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:" + getPackageName())));
                    } catch (RuntimeException ignored) {
                    }
                }
            }), k.topMargin(10));
        }
        notice.addView(c);
    }

    private void refreshList() {
        if (listBox == null) return;
        listBox.removeAllViews();
        ArrayList<Tasks.Task> up = Tasks.pending(this, false);
        LinearLayout box = k.card("☰", L.t("tk_upcoming"), null);
        if (up.isEmpty()) {
            TextView e = k.text(L.t("tk_none"), 15, MUTED);
            e.setPadding(0, k.dp(6), 0, k.dp(4));
            box.addView(e);
        }
        long now = System.currentTimeMillis();
        for (Tasks.Task t : up) box.addView(taskRow(t, t.when <= now), k.topMargin(10));
        listBox.addView(box);

        ArrayList<Tasks.Task> fin = Tasks.finished(this);
        if (!fin.isEmpty()) {
            LinearLayout d = k.card("✓", L.t("tk_done_h"), null);
            int n = 0;
            for (Tasks.Task t : fin) {
                if (n++ >= 8) break;
                LinearLayout r = k.row();
                r.setPadding(0, k.dp(8), 0, k.dp(8));
                TextView tx = k.text(t.text, 15, MUTED);
                tx.setPaintFlags(tx.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                r.addView(tx, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
                TextView del = k.strong("🗑", 18, MUTED);
                del.setPadding(k.dp(10), k.dp(4), k.dp(10), k.dp(4));
                del.setOnClickListener(v -> {
                    Tasks.delete(this, t.id);
                    refreshList();
                });
                r.addView(del);
                d.addView(r);
            }
            listBox.addView(d);
        }
    }

    private View taskRow(Tasks.Task t, boolean overdue) {
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.VERTICAL);
        r.setPadding(k.dp(14), k.dp(12), k.dp(14), k.dp(12));
        r.setBackground(k.rounded(SOFT, 18, overdue ? 0xFFE5484D : LINE));

        TextView tx = k.title(t.text, 16.5f, INK);
        tx.setLineSpacing(0, 1.2f);
        r.addView(tx);
        TextView when = k.strong("⏰  " + Tasks.when(this, t.when) + (overdue ? "   ·   " + L.t("tk_overdue") : ""), 13.5f,
                overdue ? 0xFFE5484D : GREEN);
        when.setPadding(0, k.dp(6), 0, k.dp(10));
        r.addView(when);

        LinearLayout b = k.row();
        TextView done = k.button("✔  " + L.t("tk_done"), true, v -> {
            Tasks.setDone(this, t.id);
            refreshList();
        });
        TextView change = k.button("⏱  " + L.t("tk_change"), false, v -> changeTime(t));
        TextView del = k.button("🗑", false, v -> {
            Tasks.delete(this, t.id);
            refreshList();
        });
        LinearLayout.LayoutParams p1 = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.2f);
        LinearLayout.LayoutParams p2 = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1.2f);
        LinearLayout.LayoutParams p3 = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 0.6f);
        p1.setMarginEnd(k.dp(6));
        p2.setMarginEnd(k.dp(6));
        b.addView(done, p1);
        b.addView(change, p2);
        b.addView(del, p3);
        r.addView(b);

        LinearLayout.LayoutParams lp = k.matchWrap();
        return wrapWithMargin(r, lp);
    }

    private View wrapWithMargin(View v, LinearLayout.LayoutParams lp) {
        v.setLayoutParams(lp);
        return v;
    }
}
