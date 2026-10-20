package com.ghora.zikrbar;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import static com.ghora.zikrbar.Kit.*;

/** About the app, about the developer, and support (donate) pages. */
public class InfoActivity extends Activity {

    static final String EXTRA_PAGE = "page";
    static final int PAGE_ABOUT = 0, PAGE_DEV = 1, PAGE_DONATE = 2;

    static final String SHARE_LINK = "https://github.com/zahidsskillss-netizen/zikr-bar/releases/latest";
    static final String HESAB = "0780630100";
    static final String ATOMA = "0773233182";
    static final String PAYONEER = "zahidalkozii@gmail.com";

    private Kit k;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        L.init(this);
        k = new Kit(this);
        getWindow().setStatusBarColor(GREEN2);
        getWindow().setNavigationBarColor(BG);
        int page = getIntent().getIntExtra(EXTRA_PAGE, PAGE_ABOUT);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutDirection(L.rtl() ? View.LAYOUT_DIRECTION_RTL : View.LAYOUT_DIRECTION_LTR);
        root.setPadding(0, 0, 0, k.dp(32));

        if (page == PAGE_DEV) buildDeveloper(root);
        else if (page == PAGE_DONATE) buildDonate(root);
        else buildAbout(root);

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(BG);
        scroll.addView(root);
        setContentView(scroll);
    }

    // ================= pages =================

    private void buildAbout(LinearLayout root) {
        android.widget.ImageView logo = new android.widget.ImageView(this);
        logo.setImageResource(R.drawable.logo);
        logo.setBackground(k.rounded(0x00000000, 22, 0));
        logo.setClipToOutline(true);
        logo.setLayoutParams(new LinearLayout.LayoutParams(k.dp(88), k.dp(88)));
        root.addView(header(logo, L.t("nav_about"), L.t("app_name") + " · " + L.t("version") + " " + version()));

        LinearLayout goal = k.card("✎", L.t("goal_t"), null);
        goal.addView(para(L.t("goal_text")));
        TextView dua = k.arabic("جَزَاكَ اللَّهُ خَيْرًا كَثِيرًا", 21, GREEN);
        dua.setPadding(0, k.dp(12), 0, k.dp(4));
        goal.addView(dua);

        LinearLayout sign = k.row();
        sign.setPadding(0, k.dp(10), 0, 0);
        sign.addView(k.photo(42));
        LinearLayout st = new LinearLayout(this);
        st.setOrientation(LinearLayout.VERTICAL);
        st.setPadding(k.dp(10), 0, k.dp(10), 0);
        st.addView(k.title(L.t("dev_name"), 15, INK));
        st.addView(k.text(L.t("maker"), 12.5f, MUTED));
        sign.addView(st);
        goal.addView(sign);
        root.addView(goal);

        LinearLayout feat = k.card("✦", L.t("feat_t"), null);
        String[] marks = {"ﷲ", L.num(1), L.num(2), L.num(3), L.num(4), L.num(5)};
        for (int i = 0; i < 6; i++) feat.addView(bullet(marks[i], L.t("f" + (i + 1))));
        root.addView(feat);

        LinearLayout privacy = k.card("✓", L.t("privacy_t"), null);
        privacy.addView(para(L.t("privacy")));
        root.addView(privacy);

        LinearLayout share = k.card("↗", L.t("share_btn"), null);
        share.addView(para(L.t("share_text")));
        share.addView(k.button(L.t("share_now"), true, v -> share(this)), k.topMargin(12));
        root.addView(share);
    }

    private void buildDeveloper(LinearLayout root) {
        root.addView(header(k.photo(120), L.t("dev_name"), L.lang == L.EN ? L.t("ghora") : "Zahid Ullah Alokozay"));

        LinearLayout me = k.card("✎", L.t("about_me"), null);
        me.addView(para(L.t("me_text")));
        LinearLayout chips = k.row();
        chips.setPadding(0, k.dp(10), 0, 0);
        chips.addView(chip(L.t("chip1")));
        chips.addView(k.gap(6));
        chips.addView(chip(L.t("chip2")));
        chips.addView(k.gap(6));
        chips.addView(chip(L.t("chip3")));
        me.addView(chips);
        TextView tapHint = k.text(L.t("photo_hint"), 13, MUTED);
        tapHint.setPadding(0, k.dp(10), 0, 0);
        me.addView(tapHint);
        root.addView(me);

        LinearLayout contact = k.card("☎", L.t("contact"), null);
        contact.addView(contactRow(L.t("whatsapp"), PHONE, WHATSAPP_LINK));
        contact.addView(contactRow(L.t("telegram"), "t.me/zahid_af", TELEGRAM));
        contact.addView(contactRow(L.t("facebook"), "zahid.alkozai", FACEBOOK));
        contact.addView(contactRow(L.t("call"), PHONE, "tel:" + PHONE));
        root.addView(contact);

        root.addView(ghoraAd());

        LinearLayout support = k.card("♥", L.t("support_t"), null);
        support.addView(para(L.t("support_text")));
        support.addView(k.button(L.t("donate_btn"), true, v -> openPage(PAGE_DONATE)), k.topMargin(12));
        root.addView(support);
    }

    private View ghoraAd() {
        LinearLayout ad = new LinearLayout(this);
        ad.setOrientation(LinearLayout.VERTICAL);
        ad.setPadding(k.dp(20), k.dp(22), k.dp(20), k.dp(20));
        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{0xFF14432A, 0xFF061A0E});
        bg.setCornerRadius(k.dp(24));
        bg.setStroke(k.dp(1.5f), GOLD_LINE);
        ad.setBackground(bg);
        ad.setElevation(k.dp(3));
        LinearLayout.LayoutParams alp = k.matchWrap();
        alp.setMargins(k.dp(16), k.dp(14), k.dp(16), 0);
        ad.setLayoutParams(alp);

        TextView brand = k.title(L.t("ghora"), 27, GOLD);
        brand.setGravity(Gravity.CENTER);
        ad.addView(brand);
        if (L.lang != L.EN) {
            TextView en = k.strong("GHORA GRAPHICS PRODUCTION", 11, 0xB3F3E3A5);
            en.setGravity(Gravity.CENTER);
            en.setLetterSpacing(0.18f);
            en.setPadding(0, k.dp(6), 0, 0);
            ad.addView(en);
        }
        TextView tag = k.text(L.t("ad_tag"), 15, WHITE);
        tag.setGravity(Gravity.CENTER);
        tag.setPadding(0, k.dp(12), 0, k.dp(12));
        ad.addView(tag);

        String[] services = {L.t("s1"), L.t("chip2"), L.t("s3"), L.t("s4"), L.t("s5")};
        for (String s : services) {
            LinearLayout r = k.row();
            r.setPadding(0, k.dp(5), 0, k.dp(5));
            r.addView(k.text("✦", 13, GOLD));
            TextView t = k.text(s, 15.5f, WHITE);
            t.setPadding(k.dp(10), 0, k.dp(10), 0);
            r.addView(t, k.weight());
            ad.addView(r);
        }
        TextView call = k.title(L.t("contact_now"), 15, GOLD);
        call.setGravity(Gravity.CENTER);
        call.setPadding(0, k.dp(16), 0, k.dp(10));
        ad.addView(call);
        ad.addView(k.goldButton(L.t("whatsapp") + ": " + PHONE, v -> open(WHATSAPP_LINK)), k.matchWrap());
        LinearLayout more = k.row();
        more.addView(darkButton(L.t("telegram"), v -> open(TELEGRAM)), k.weight());
        more.addView(k.gap(8));
        more.addView(darkButton(L.t("facebook"), v -> open(FACEBOOK)), k.weight());
        ad.addView(more, k.topMargin(10));
        return ad;
    }

    private void buildDonate(LinearLayout root) {
        root.addView(header(glyphCircle("♥"), L.t("support_t"), L.t("donate_sub")));

        LinearLayout intro = k.card(null, L.t("why_t"), null);
        LinearLayout who = k.row();
        who.setPadding(0, k.dp(4), 0, k.dp(10));
        who.addView(k.photo(52));
        LinearLayout wt = new LinearLayout(this);
        wt.setOrientation(LinearLayout.VERTICAL);
        wt.setPadding(k.dp(12), 0, k.dp(12), 0);
        wt.addView(k.title(L.t("dev_name"), 16, INK));
        wt.addView(k.text(L.t("maker") + " · " + L.t("ghora"), 12.5f, MUTED));
        who.addView(wt, k.weight());
        intro.addView(who);
        intro.addView(para(L.t("why_text")));
        root.addView(intro);

        root.addView(payCard("H", "HesabPay", HESAB));
        root.addView(payCard("A", "Atoma Pay", ATOMA));

        LinearLayout pay = k.card("P", "Payoneer", L.t("payoneer_s"));
        pay.addView(valueBox(PAYONEER), k.topMargin(8));
        pay.addView(k.button(L.t("copy_email"), false, v -> copy("Payoneer", PAYONEER, L.t("copied"))), k.topMargin(12));
        root.addView(pay);

        TextView dua = k.arabic("جَزَاكُمُ اللَّهُ خَيْرًا", 24, GREEN);
        dua.setPadding(0, k.dp(22), 0, 0);
        root.addView(dua);
    }

    /** A wallet card: shows the full number; copy gives the 9 digits without the leading 0. */
    private View payCard(String glyph, String title, String number) {
        LinearLayout c = k.card(glyph, title, L.t("mobile_no"));
        c.addView(valueBox(number), k.topMargin(8));
        String copyValue = number.startsWith("0") ? number.substring(1) : number;
        c.addView(k.button(L.t("copy") + "  (" + copyValue + ")", false,
                v -> copy(title, copyValue, L.f("copied_9", copyValue))), k.topMargin(12));
        return c;
    }

    // ================= actions =================

    static void share(Context c) {
        L.init(c);
        String msg = L.t("share_msg") + "\n" + SHARE_LINK + "\n\nفَاذْكُرُونِي أَذْكُرْكُمْ";
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("text/plain");
        i.putExtra(Intent.EXTRA_TEXT, msg);
        c.startActivity(Intent.createChooser(i, L.t("share_now")));
    }

    private void open(String url) {
        Kit.open(this, url);
    }

    private void openPage(int page) {
        startActivity(new Intent(this, InfoActivity.class).putExtra(EXTRA_PAGE, page));
    }

    private void copy(String label, String value, String toast) {
        ClipboardManager cm = getSystemService(ClipboardManager.class);
        cm.setPrimaryClip(ClipData.newPlainText(label, value));
        Toast.makeText(this, toast, Toast.LENGTH_LONG).show();
    }

    private String version() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (Exception e) {
            return "";
        }
    }

    // ================= ui pieces =================

    private View header(View top, String title, String sub) {
        LinearLayout h = new LinearLayout(this);
        h.setOrientation(LinearLayout.VERTICAL);
        h.setGravity(Gravity.CENTER_HORIZONTAL);
        h.setPadding(k.dp(18), k.dp(14), k.dp(18), k.dp(28));
        h.setBackground(k.headerBackground());

        TextView back = k.strong((L.rtl() ? "→  " : "←  ") + L.t("back"), 14, 0xE6FFFFFF);
        back.setPadding(k.dp(14), k.dp(6), k.dp(16), k.dp(8));
        back.setBackground(new RippleDrawable(ColorStateList.valueOf(0x40FFFFFF), k.rounded(0x26FFFFFF, 18, 0x40FFFFFF), null));
        back.setOnClickListener(v -> finish());
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        bp.gravity = Gravity.START;
        bp.bottomMargin = k.dp(12);
        h.addView(back, bp);

        h.addView(top);
        TextView t = k.title(title, 28, WHITE);
        t.setGravity(Gravity.CENTER);
        t.setPadding(0, k.dp(14), 0, k.dp(6));
        h.addView(t);
        TextView s = k.text(sub, 13.5f, 0xD9FFFFFF);
        s.setGravity(Gravity.CENTER);
        h.addView(s);
        return h;
    }

    private View glyphCircle(String glyph) {
        TextView g = k.arabic(glyph, 40, GOLD);
        g.setIncludeFontPadding(false);
        GradientDrawable ring = new GradientDrawable();
        ring.setShape(GradientDrawable.OVAL);
        ring.setColor(0x22000000);
        ring.setStroke(k.dp(1.5f), 0x99F3E3A5);
        g.setBackground(ring);
        g.setLayoutParams(new LinearLayout.LayoutParams(k.dp(86), k.dp(86)));
        return g;
    }

    private TextView para(String s) {
        TextView t = k.text(s, 16, INK);
        t.setLineSpacing(0, 1.5f);
        return t;
    }

    private TextView chip(String s) {
        TextView t = k.strong(s, 12.5f, GREEN);
        t.setPadding(k.dp(12), k.dp(4), k.dp(12), k.dp(6));
        t.setBackground(k.rounded(0x141B5E20, 14, 0));
        return t;
    }

    private View bullet(String mark, String s) {
        LinearLayout r = k.row();
        r.setPadding(0, k.dp(6), 0, k.dp(6));
        r.addView(k.badge(mark, 32));
        TextView t = k.text(s, 15, INK);
        t.setPadding(k.dp(12), 0, k.dp(12), 0);
        r.addView(t, k.weight());
        return r;
    }

    private View contactRow(String label, String value, String url) {
        LinearLayout r = k.row();
        r.setPadding(k.dp(14), k.dp(10), k.dp(14), k.dp(12));
        r.setBackground(new RippleDrawable(ColorStateList.valueOf(0x201B5E20), k.rounded(SOFT, 16, LINE), null));
        r.setClickable(true);
        r.setOnClickListener(v -> open(url));
        r.addView(k.title(label, 15, INK), new LinearLayout.LayoutParams(k.dp(90), LinearLayout.LayoutParams.WRAP_CONTENT));
        TextView val = k.text(value, 15, GREEN);
        val.setTextDirection(View.TEXT_DIRECTION_LTR);
        val.setGravity(Gravity.END);
        r.addView(val, k.weight());
        r.addView(k.text(L.rtl() ? "  ‹" : "  ›", 20, MUTED));
        r.setLayoutParams(k.topMargin(8));
        return r;
    }

    private TextView darkButton(String s, View.OnClickListener l) {
        TextView b = k.strong(s, 14, GOLD);
        b.setGravity(Gravity.CENTER);
        b.setPadding(k.dp(12), k.dp(10), k.dp(12), k.dp(12));
        b.setBackground(new RippleDrawable(ColorStateList.valueOf(0x30F3E3A5), k.rounded(0x1AFFFFFF, 14, 0x66F3E3A5), null));
        b.setClickable(true);
        b.setOnClickListener(l);
        return b;
    }

    private TextView valueBox(String v) {
        TextView t = k.title(v, 22, GREEN);
        t.setGravity(Gravity.CENTER);
        t.setTextIsSelectable(true);
        t.setTextDirection(View.TEXT_DIRECTION_LTR);
        t.setLetterSpacing(0.04f);
        t.setPadding(k.dp(12), k.dp(14), k.dp(12), k.dp(14));
        t.setBackground(k.rounded(SOFT, 16, LINE));
        return t;
    }
}
