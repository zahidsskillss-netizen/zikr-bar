package com.ghora.zikrbar;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.List;

import static com.ghora.zikrbar.Kit.*;

/**
 * The dhikr list as separate rows: each row can move up or down, be edited or removed,
 * and a + button adds a new dhikr. Every change is saved at once.
 */
final class ListEditor {

    private final Activity a;
    private final Kit k;
    private final Runnable onChange;
    private final LinearLayout box;
    private List<String> items;

    private ListEditor(Activity a, Kit k, Runnable onChange) {
        this.a = a;
        this.k = k;
        this.onChange = onChange;
        box = new LinearLayout(a);
        box.setOrientation(LinearLayout.VERTICAL);
        android.animation.LayoutTransition lt = new android.animation.LayoutTransition();
        lt.setDuration(180);
        box.setLayoutTransition(lt);
    }

    /** The editor as a view to put inside a card. */
    static View inline(Activity a, Kit k, Runnable onChange) {
        ListEditor e = new ListEditor(a, k, onChange);
        LinearLayout wrap = new LinearLayout(a);
        wrap.setOrientation(LinearLayout.VERTICAL);
        wrap.addView(e.box, k.matchWrap());
        wrap.addView(e.addButton(), k.topMargin(12));
        e.fill();
        return wrap;
    }

    /** The editor in a dialog (used on the tasbih page). */
    static void dialog(Activity a, Kit k, Runnable onClose) {
        Dialog d = new Dialog(a);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        ListEditor e = new ListEditor(a, k, null);

        LinearLayout root = new LinearLayout(a);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutDirection(L.rtl() ? View.LAYOUT_DIRECTION_RTL : View.LAYOUT_DIRECTION_LTR);
        root.setPadding(k.dp(18), k.dp(18), k.dp(18), k.dp(16));
        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{Theme.glass ? Theme.base : 0xFFFFFFFF, Theme.glass ? Theme.base : 0xFFF6F8F4});
        bg.setCornerRadius(k.dp(26));
        bg.setStroke(k.dp(1.2f), LINE);
        root.setBackground(bg);

        root.addView(k.title(L.t("le_title"), 19, INK));
        TextView s = k.text(L.t("le_sub"), 13, MUTED);
        s.setPadding(0, k.dp(4), 0, k.dp(8));
        root.addView(s);

        ScrollView sc = new ScrollView(a);
        LinearLayout inner = new LinearLayout(a);
        inner.setOrientation(LinearLayout.VERTICAL);
        inner.addView(e.box, k.matchWrap());
        inner.addView(e.addButton(), k.topMargin(12));
        sc.addView(inner);
        root.addView(sc, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1));

        root.addView(k.button(L.t("done_btn"), true, v -> d.dismiss()), k.topMargin(12));
        e.fill();

        d.setContentView(root);
        Window w = d.getWindow();
        if (w != null) {
            w.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(0));
            int h = Math.round(a.getResources().getDisplayMetrics().heightPixels * 0.82f);
            w.setLayout(Math.round(a.getResources().getDisplayMetrics().widthPixels * 0.94f), h);
        }
        d.setOnDismissListener(x -> {
            if (onClose != null) onClose.run();
        });
        d.show();
    }

    private void fill() {
        items = Prefs.list(a);
        box.removeAllViews();
        for (int i = 0; i < items.size(); i++) box.addView(row(i));
    }

    private void save() {
        Prefs.setList(a, items);
        if (onChange != null) onChange.run();
    }

    private View row(int i) {
        LinearLayout r = k.row();
        r.setPadding(k.dp(10), k.dp(8), k.dp(10), k.dp(8));
        r.setBackground(k.rounded(SOFT, 16, LINE));
        LinearLayout.LayoutParams lp = k.matchWrap();
        lp.topMargin = k.dp(8);
        r.setLayoutParams(lp);

        TextView n = k.strong(L.num(i + 1), 12, GREEN);
        n.setGravity(Gravity.CENTER);
        n.setBackground(k.rounded(Theme.ov(0x14), 12, 0));
        r.addView(n, new LinearLayout.LayoutParams(k.dp(26), k.dp(26)));

        TextView t = k.arabic(items.get(i), 17, INK);
        t.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
        t.setTextDirection(View.TEXT_DIRECTION_RTL);
        t.setLineSpacing(0, 1.15f);
        t.setPadding(k.dp(12), k.dp(2), k.dp(12), k.dp(2));
        t.setOnClickListener(v -> edit(i));
        r.addView(t, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        r.addView(small("▲", i > 0, v -> move(i, -1)));
        r.addView(small("▼", i < items.size() - 1, v -> move(i, 1)));
        r.addView(small("✕", items.size() > 1, v -> remove(i)));
        return r;
    }

    private TextView small(String glyph, boolean enabled, View.OnClickListener l) {
        TextView b = k.strong(glyph, 13, GREEN);
        b.setGravity(Gravity.CENTER);
        b.setBackground(new RippleDrawable(ColorStateList.valueOf(0x301B5E20), k.rounded(Theme.ov(0x10), 12, 0), null));
        b.setAlpha(enabled ? 1f : 0.3f);
        b.setEnabled(enabled);
        b.setOnClickListener(l);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(k.dp(36), k.dp(36));
        lp.setMarginStart(k.dp(4));
        b.setLayoutParams(lp);
        return b;
    }

    private void move(int i, int dir) {
        int j = i + dir;
        if (j < 0 || j >= items.size()) return;
        String x = items.get(i);
        items.set(i, items.get(j));
        items.set(j, x);
        save();
        fill();
    }

    private void remove(int i) {
        if (items.size() <= 1) return;
        new AlertDialog.Builder(a)
                .setMessage(items.get(i))
                .setPositiveButton(L.t("le_delete"), (d, w) -> {
                    items.remove(i);
                    save();
                    fill();
                })
                .setNegativeButton(L.t("cancel"), null)
                .show();
    }

    private void edit(int i) {
        ask(items.get(i), text -> {
            items.set(i, text);
            save();
            fill();
        });
    }

    private TextView addButton() {
        return k.button("+  " + L.t("le_add"), false, v -> ask("", text -> {
            items.add(text);
            save();
            fill();
        }));
    }

    private interface OnText { void on(String s); }

    private void ask(String start, OnText done) {
        EditText in = new EditText(a);
        in.setText(start);
        in.setTypeface(k.amiri);
        in.setTextSize(19);
        in.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        in.setTextDirection(View.TEXT_DIRECTION_RTL);
        in.setMinLines(2);
        LinearLayout pad = new LinearLayout(a);
        pad.setPadding(k.dp(20), k.dp(8), k.dp(20), 0);
        pad.addView(in, k.matchWrap());
        new AlertDialog.Builder(a)
                .setTitle(L.t(start.isEmpty() ? "le_add" : "le_edit"))
                .setView(pad)
                .setPositiveButton(L.t("save"), (d, w) -> {
                    String t = in.getText().toString().replace('\n', ' ').trim();
                    if (!t.isEmpty()) done.on(t);
                })
                .setNegativeButton(L.t("cancel"), null)
                .show();
    }
}
