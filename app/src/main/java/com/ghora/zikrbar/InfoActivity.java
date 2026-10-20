package com.ghora.zikrbar;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/** About the app, about the developer, and support (donate) pages. */
public class InfoActivity extends Activity {

    static final String EXTRA_PAGE = "page";
    static final int PAGE_ABOUT = 0, PAGE_DEV = 1, PAGE_DONATE = 2;

    static final String SHARE_LINK = "https://github.com/zahidsskillss-netizen/zikr-bar/releases/latest";
    static final String HESAB_LINK = "https://hesab.com/pay/ZIKARBAR/ps";
    static final String ATOMA = "0773233182";
    static final String PAYONEER = "zahidalkozii@gmail.com";
    static final String WHATSAPP = "0773233182";
    static final String WHATSAPP_LINK = "https://wa.me/93773233182";

    private static final int GREEN = 0xFF1B5E20, GREEN2 = 0xFF2E7D32, DARK = 0xFF0F3D1F;
    private static final int INK = 0xFF16231A, MUTED = 0xFF5B695E, LINE = 0xFFDBE4DA;
    private static final int BG = 0xFFF2F5F1, SOFT = 0xFFF5F8F4, GOLD = 0xFFF3E3A5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int page = getIntent().getIntExtra(EXTRA_PAGE, PAGE_ABOUT);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        root.setPadding(0, 0, 0, dp(28));

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
        root.addView(header("ﷲ", "د پروګرام په اړه", "ذکر بار · نسخه " + version()));

        LinearLayout goal = card("د دې پروګرام موخه");
        goal.addView(para(
                "زما هدف دا و چې نن سبا ډېری ځوانان د موبایل او انټرنېټ له امله د الله تعالی له ذکر څخه غافل شوي دي. "
                + "نو غوره مې وګڼله چې داسې یو پروګرام جوړ کړم چې د همغه شي په ذریعه چې موږ یې له ذکره غافل کړي یو، "
                + "بېرته ګټه واخلو او خپل رب تل په یاد ولرو.\n\n"
                + "ډېره خوښي احساسوم چې متعال رب پر دې کار بریالی کړم. اوس له تاسو هیله لرم چې زموږ ملاتړ وکړئ، "
                + "ترڅو د نورو داسې موخو لپاره کار وکړو. هیله ده دا پروګرام خپلو ټولو ملګرو او دوستانو ته ورسوئ "
                + "او د دارینو ثواب ترلاسه کړئ."));
        TextView dua = arabic("جَزَاكَ اللَّهُ خَيْرًا كَثِيرًا", 20);
        dua.setPadding(0, dp(10), 0, 0);
        goal.addView(dua);
        TextView sign = text("— زاهدالله الکوزی", 13, MUTED, false);
        sign.setGravity(Gravity.END);
        sign.setPadding(0, dp(6), 0, 0);
        goal.addView(sign);
        root.addView(goal);

        LinearLayout feat = card("پروګرام څه کوي؟");
        String[][] items = {
                {"ﷲ", "د الله تعالی مبارک نوم تل د موبایل په status bar کې ښکاري"},
                {"۱", "ذکر په خبرتیا کې ښکاري او هر څو دقیقې وروسته بدلیږي"},
                {"۲", "تسبیح بټن د هر اپ له پاسه، په شپږو ډیزاینونو کې"},
                {"۳", "د دوو ګوتو سره لوی او کوچنی کېدل، څنډې ته پټېدل، ✕ ته بندېدل"},
                {"۴", "که څو ساعته ذکر ونه شي، د «فَاذْكُرُونِي أَذْكُرْكُمْ» یادونه"},
                {"۵", "خپل ذکرونه زیاتولی، کمولی او بدلولی شئ"}
        };
        for (String[] it : items) feat.addView(bullet(it[0], it[1]));
        root.addView(feat);

        LinearLayout privacy = card("ستاسو محرمیت");
        privacy.addView(para("دا پروګرام انټرنېټ نه کاروي او ستاسو هیڅ معلومات نه راټولوي او نه چېرته لېږي. "
                + "ستاسو ذکرونه او شمېر یوازې ستاسو په موبایل کې ساتل کیږي."));
        root.addView(privacy);

        LinearLayout share = card("ملګرو ته یې ولېږئ");
        share.addView(para("هر څوک چې ستاسو په لاس دا پروګرام وکاروي او ذکر وکړي، ان شاء الله تاسو ته به یې هم ثواب رسیږي."));
        share.addView(button("ملګرو ته لېږل", true, v -> shareApp()), topMargin(12));
        root.addView(share);
    }

    private void buildDeveloper(LinearLayout root) {
        root.addView(header("غ", "پراختیاکوونکی", "زاهدالله الکوزی · Zahid Ullah Alokozay"));

        LinearLayout me = card("زاهدالله الکوزی");
        me.addView(text("Zahid Ullah Alokozay", 14, MUTED, false));
        me.addView(para("ګرافیک ډیزاینر او ویډیو ایډیټر، له درې کالو ډېره تجربه. "
                + "د غوره ګرافیکس پروډکشن بنسټګر."), topMargin(8));
        root.addView(me);

        // --- Ghora Graphics Production ad ---
        LinearLayout ad = new LinearLayout(this);
        ad.setOrientation(LinearLayout.VERTICAL);
        ad.setPadding(dp(20), dp(20), dp(20), dp(20));
        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{0xFF14432A, 0xFF071F12});
        bg.setCornerRadius(dp(22));
        bg.setStroke(dp(1.5f), 0xFFC9A227);
        ad.setBackground(bg);
        ad.setElevation(dp(2));
        LinearLayout.LayoutParams alp = matchWrap();
        alp.setMargins(dp(16), dp(16), dp(16), 0);
        ad.setLayoutParams(alp);

        TextView brand = new TextView(this);
        brand.setText("غوره ګرافیکس پروډکشن");
        brand.setTypeface(Ui.amiriBold(this));
        brand.setTextSize(26);
        brand.setTextColor(GOLD);
        brand.setGravity(Gravity.CENTER);
        ad.addView(brand);
        TextView en = text("GHORA GRAPHICS PRODUCTION", 11, 0xB3F3E3A5, true);
        en.setGravity(Gravity.CENTER);
        en.setLetterSpacing(0.15f);
        ad.addView(en);
        TextView tag = text("ستاسو د کار لپاره ښکلی، مسلکي او اغېزناک ډیزاین", 14, Color.WHITE, false);
        tag.setGravity(Gravity.CENTER);
        tag.setPadding(0, dp(12), 0, dp(10));
        ad.addView(tag);

        String[] services = {
                "ګرافیک ډیزاین: لوګو، پوسټر، بنر او د ټولنیزو رسنیو پوسټونه",
                "ویډیو ایډیټ",
                "موشن اعلانونه (Motion Ads)",
                "د سپانسر خدمات",
                "د موبایل اپلیکېشن جوړول"
        };
        for (String s : services) {
            TextView t = text("✦  " + s, 15, Color.WHITE, false);
            t.setPadding(0, dp(5), 0, dp(5));
            t.setLineSpacing(0, 1.2f);
            ad.addView(t);
        }
        TextView call = text("همدا اوس اړیکه ونیسئ", 14, GOLD, true);
        call.setGravity(Gravity.CENTER);
        call.setPadding(0, dp(14), 0, dp(8));
        ad.addView(call);
        TextView wa = goldButton("واټساپ: " + WHATSAPP, v -> open(WHATSAPP_LINK));
        ad.addView(wa, matchWrap());
        TextView fb = text("فیسبوک پاڼه: Ghora Graphics", 13, 0xCCFFFFFF, false);
        fb.setGravity(Gravity.CENTER);
        fb.setPadding(0, dp(10), 0, 0);
        ad.addView(fb);
        root.addView(ad);

        LinearLayout support = card("زموږ ملاتړ");
        support.addView(para("که دا پروګرام مو خوښ شو، زموږ ملاتړ وکړئ چې د نورو داسې ګټورو پروګرامونو لپاره کار وکړو."));
        support.addView(button("ملاتړ (ډونېټ)", true, v -> openPage(PAGE_DONATE)), topMargin(12));
        root.addView(support);
    }

    private void buildDonate(LinearLayout root) {
        root.addView(header("♥", "زموږ ملاتړ", "ستاسو هره مرسته زموږ د نورو ګټورو کارونو لپاره ده"));

        LinearLayout intro = card("ولې ملاتړ؟");
        intro.addView(para("ذکر بار وړیا دی او وړیا به پاتې وي. ستاسو ملاتړ له موږ سره مرسته کوي چې دا پروګرام نور هم ښه کړو "
                + "او د نورو داسې اسلامي او ګټورو پروګرامونو لپاره کار وکړو."));
        root.addView(intro);

        LinearLayout hesab = card("HesabPay · حساب پی");
        hesab.addView(valueBox(HESAB_LINK, true));
        LinearLayout hb = row();
        hb.addView(button("ورکول", true, v -> open(HESAB_LINK)), weight());
        hb.addView(space());
        hb.addView(button("کاپي", false, v -> copy("HesabPay", HESAB_LINK)), weight());
        hesab.addView(hb, topMargin(12));
        root.addView(hesab);

        LinearLayout atoma = card("Atoma Pay · اټوما پی");
        atoma.addView(valueBox(ATOMA, true));
        atoma.addView(button("شمېره کاپي کول", false, v -> copy("Atoma Pay", ATOMA)), topMargin(12));
        root.addView(atoma);

        LinearLayout pay = card("Payoneer · له بهر څخه");
        pay.addView(para("که له افغانستان بهر یاست، Payoneer ته یې دې ایمیل ته ولېږئ:"));
        pay.addView(valueBox(PAYONEER, true), topMargin(8));
        pay.addView(button("ایمیل کاپي کول", false, v -> copy("Payoneer", PAYONEER)), topMargin(12));
        root.addView(pay);

        TextView dua = arabic("جَزَاكُمُ اللَّهُ خَيْرًا", 22);
        dua.setPadding(0, dp(20), 0, 0);
        root.addView(dua);
    }

    // ================= actions =================

    private void shareApp() {
        String msg = "ذکر بار: د الله تعالی مبارک نوم ستاسو د موبایل په status bar کې، او تسبیح د هر اپ له پاسه.\n"
                + "فَاذْكُرُونِي أَذْكُرْكُمْ\n\nوړیا یې ښکته کړئ:\n" + SHARE_LINK;
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("text/plain");
        i.putExtra(Intent.EXTRA_TEXT, msg);
        startActivity(Intent.createChooser(i, "ملګرو ته لېږل"));
    }

    private void open(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (RuntimeException e) {
            Toast.makeText(this, "لینک خلاص نه شو", Toast.LENGTH_SHORT).show();
        }
    }

    private void openPage(int page) {
        startActivity(new Intent(this, InfoActivity.class).putExtra(EXTRA_PAGE, page));
    }

    private void copy(String label, String value) {
        ClipboardManager cm = getSystemService(ClipboardManager.class);
        cm.setPrimaryClip(ClipData.newPlainText(label, value));
        Toast.makeText(this, label + " کاپي شو", Toast.LENGTH_SHORT).show();
    }

    private String version() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (Exception e) {
            return "";
        }
    }

    // ================= ui helpers =================

    private View header(String glyph, String title, String sub) {
        LinearLayout h = new LinearLayout(this);
        h.setOrientation(LinearLayout.VERTICAL);
        h.setGravity(Gravity.CENTER_HORIZONTAL);
        h.setPadding(dp(24), dp(16), dp(24), dp(24));
        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{GREEN2, DARK});
        float rr = dp(32);
        bg.setCornerRadii(new float[]{0, 0, 0, 0, rr, rr, rr, rr});
        h.setBackground(bg);

        TextView back = text("→ بېرته", 14, 0xCCFFFFFF, true);
        back.setPadding(0, dp(4), 0, dp(4));
        back.setOnClickListener(v -> finish());
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        bp.gravity = Gravity.START;
        h.addView(back, bp);

        TextView g = new TextView(this);
        g.setText(glyph);
        g.setTypeface(Ui.amiriBold(this));
        g.setTextSize(44);
        g.setTextColor(GOLD);
        g.setGravity(Gravity.CENTER);
        h.addView(g);
        TextView t = new TextView(this);
        t.setText(title);
        t.setTypeface(Ui.amiriBold(this));
        t.setTextSize(26);
        t.setTextColor(Color.WHITE);
        t.setGravity(Gravity.CENTER);
        h.addView(t);
        TextView s = text(sub, 13, 0xD9FFFFFF, false);
        s.setGravity(Gravity.CENTER);
        h.addView(s);
        return h;
    }

    private LinearLayout card(String title) {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(dp(18), dp(16), dp(18), dp(18));
        c.setBackground(rounded(Color.WHITE, 22, LINE));
        c.setElevation(dp(1));
        LinearLayout.LayoutParams lp = matchWrap();
        lp.setMargins(dp(16), dp(16), dp(16), 0);
        c.setLayoutParams(lp);
        TextView t = text(title, 18, INK, true);
        t.setPadding(0, 0, 0, dp(8));
        c.addView(t);
        return c;
    }

    private TextView para(String s) {
        TextView t = text(s, 15, INK, false);
        t.setLineSpacing(0, 1.45f);
        return t;
    }

    private TextView arabic(String s, int sp) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTypeface(Ui.amiriBold(this));
        t.setTextSize(sp);
        t.setTextColor(GREEN);
        t.setGravity(Gravity.CENTER);
        return t;
    }

    private View bullet(String mark, String s) {
        LinearLayout r = row();
        r.setPadding(0, dp(5), 0, dp(5));
        TextView m = new TextView(this);
        m.setText(mark);
        m.setTypeface(Ui.amiriBold(this));
        m.setTextSize(14);
        m.setTextColor(GREEN);
        m.setGravity(Gravity.CENTER);
        m.setBackground(rounded(0x141B5E20, 14, 0x331B5E20));
        r.addView(m, new LinearLayout.LayoutParams(dp(30), dp(30)));
        TextView t = text(s, 14, INK, false);
        t.setPadding(dp(12), dp(4), 0, 0);
        t.setLineSpacing(0, 1.3f);
        r.addView(t, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        return r;
    }

    private TextView valueBox(String v, boolean ltr) {
        TextView t = text(v, 16, GREEN, true);
        t.setGravity(Gravity.CENTER);
        t.setTextIsSelectable(true);
        if (ltr) t.setTextDirection(View.TEXT_DIRECTION_LTR);
        t.setPadding(dp(12), dp(12), dp(12), dp(12));
        t.setBackground(rounded(SOFT, 14, LINE));
        t.setLayoutParams(matchWrap());
        return t;
    }

    private TextView text(String s, int sp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        return t;
    }

    private TextView button(String s, boolean filled, View.OnClickListener l) {
        TextView b = text(s, 15, filled ? Color.WHITE : GREEN, true);
        b.setGravity(Gravity.CENTER);
        b.setPadding(dp(16), dp(12), dp(16), dp(12));
        GradientDrawable shape = rounded(filled ? GREEN : Color.WHITE, 14, GREEN);
        b.setBackground(new RippleDrawable(ColorStateList.valueOf(filled ? 0x40FFFFFF : 0x201B5E20), shape, null));
        b.setClickable(true);
        b.setFocusable(true);
        b.setOnClickListener(l);
        return b;
    }

    private TextView goldButton(String s, View.OnClickListener l) {
        TextView b = text(s, 16, 0xFF2D2002, true);
        b.setGravity(Gravity.CENTER);
        b.setPadding(dp(16), dp(13), dp(16), dp(13));
        GradientDrawable shape = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, new int[]{0xFFF6DD7D, 0xFFC9A227});
        shape.setCornerRadius(dp(14));
        b.setBackground(new RippleDrawable(ColorStateList.valueOf(0x40FFFFFF), shape, null));
        b.setClickable(true);
        b.setOnClickListener(l);
        return b;
    }

    private LinearLayout row() {
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setGravity(Gravity.CENTER_VERTICAL);
        return r;
    }

    private View space() {
        View v = new View(this);
        v.setLayoutParams(new LinearLayout.LayoutParams(dp(10), 1));
        return v;
    }

    private LinearLayout.LayoutParams weight() {
        return new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
    }

    private LinearLayout.LayoutParams topMargin(int dpv) {
        LinearLayout.LayoutParams lp = matchWrap();
        lp.topMargin = dp(dpv);
        return lp;
    }

    private GradientDrawable rounded(int fill, int radiusDp, int stroke) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill);
        g.setCornerRadius(dp(radiusDp));
        g.setStroke(dp(1.5f), stroke);
        return g;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
    }

    private int dp(float v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
