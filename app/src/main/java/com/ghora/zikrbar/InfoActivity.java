package com.ghora.zikrbar;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
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
        k = new Kit(this);
        int page = getIntent().getIntExtra(EXTRA_PAGE, PAGE_ABOUT);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
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
        root.addView(header(glyphCircle("ﷲ"), "د پروګرام په اړه", "ذکر بار · نسخه " + version()));

        LinearLayout goal = k.card("✎", "د دې پروګرام موخه", null);
        goal.addView(para(
                "زما هدف دا و چې نن سبا ډېری ځوانان د موبایل او انټرنېټ له امله د الله تعالی له ذکر څخه غافل شوي دي. "
                + "نو غوره مې وګڼله چې داسې یو پروګرام جوړ کړم چې د همغه شي په ذریعه چې موږ یې له ذکره غافل کړي یو، "
                + "بېرته ګټه واخلو او خپل رب تل په یاد ولرو.\n\n"
                + "ډېره خوښي احساسوم چې متعال رب پر دې کار بریالی کړم. اوس له تاسو هیله لرم چې زموږ ملاتړ وکړئ، "
                + "ترڅو د نورو داسې موخو لپاره کار وکړو. هیله ده دا پروګرام خپلو ټولو ملګرو او دوستانو ته ورسوئ "
                + "او د دارینو ثواب ترلاسه کړئ."));
        TextView dua = k.arabic("جَزَاكَ اللَّهُ خَيْرًا كَثِيرًا", 21, GREEN);
        dua.setPadding(0, k.dp(12), 0, k.dp(4));
        goal.addView(dua);

        LinearLayout sign = k.row();
        sign.setPadding(0, k.dp(10), 0, 0);
        sign.addView(k.photo(42));
        LinearLayout st = new LinearLayout(this);
        st.setOrientation(LinearLayout.VERTICAL);
        st.setPadding(k.dp(10), 0, 0, 0);
        st.addView(k.title("زاهدالله الکوزی", 15, INK));
        st.addView(k.text("د ذکر بار جوړونکی", 12, MUTED));
        sign.addView(st);
        goal.addView(sign);
        root.addView(goal);

        LinearLayout feat = k.card("✦", "پروګرام څه کوي؟", null);
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

        LinearLayout privacy = k.card("✓", "ستاسو محرمیت", null);
        privacy.addView(para("دا پروګرام انټرنېټ نه کاروي او ستاسو هیڅ معلومات نه راټولوي او نه چېرته لېږي. "
                + "ستاسو ذکرونه او شمېر یوازې ستاسو په موبایل کې ساتل کیږي."));
        root.addView(privacy);

        LinearLayout share = k.card("↗", "ملګرو ته یې ولېږئ", null);
        share.addView(para("هر څوک چې ستاسو په لاس دا پروګرام وکاروي او ذکر وکړي، ان شاء الله تاسو ته به یې هم ثواب رسیږي."));
        share.addView(k.button("ملګرو ته لېږل", true, v -> share(this)), k.topMargin(12));
        root.addView(share);
    }

    private void buildDeveloper(LinearLayout root) {
        root.addView(header(k.photo(120), "زاهدالله الکوزی", "Zahid Ullah Alokozay · په عکس کېکاږئ: فیسبوک"));

        LinearLayout me = k.card("✎", "زما په اړه", null);
        me.addView(para("ګرافیک ډیزاینر او ویډیو ایډیټر، له درې کالو ډېره تجربه، "
                + "او د غوره ګرافیکس پروډکشن بنسټګر."));
        LinearLayout chips = k.row();
        chips.setPadding(0, k.dp(10), 0, 0);
        chips.addView(chip("ګرافیک ډیزاین"));
        chips.addView(k.gap(6));
        chips.addView(chip("ویډیو ایډیټ"));
        chips.addView(k.gap(6));
        chips.addView(chip("اپ جوړول"));
        me.addView(chips);
        TextView tapHint = k.text("په عکس کېکاږئ چې فیسبوک پاڼه مې خلاصه شي", 12.5f, MUTED);
        tapHint.setPadding(0, k.dp(10), 0, 0);
        me.addView(tapHint);
        root.addView(me);

        LinearLayout contact = k.card("☎", "اړیکه", null);
        contact.addView(contactRow("واټساپ", PHONE, WHATSAPP_LINK));
        contact.addView(contactRow("ټلګرام", "t.me/zahid_af", TELEGRAM));
        contact.addView(contactRow("فیسبوک", "zahid.alkozai", FACEBOOK));
        contact.addView(contactRow("زنګ", PHONE, "tel:" + PHONE));
        root.addView(contact);

        root.addView(ghoraAd());

        LinearLayout support = k.card("♥", "زموږ ملاتړ", null);
        support.addView(para("که دا پروګرام مو خوښ شو، زموږ ملاتړ وکړئ چې د نورو داسې ګټورو پروګرامونو لپاره کار وکړو."));
        support.addView(k.button("ملاتړ (ډونېټ)", true, v -> openPage(PAGE_DONATE)), k.topMargin(12));
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

        TextView brand = k.title("غوره ګرافیکس پروډکشن", 27, GOLD);
        brand.setGravity(Gravity.CENTER);
        ad.addView(brand);
        TextView en = k.text("GHORA GRAPHICS PRODUCTION", 11, 0xB3F3E3A5);
        en.setGravity(Gravity.CENTER);
        en.setLetterSpacing(0.18f);
        en.setPadding(0, k.dp(6), 0, 0);
        ad.addView(en);
        TextView tag = k.text("ستاسو د کار لپاره ښکلی، مسلکي او اغېزناک ډیزاین", 14, WHITE);
        tag.setGravity(Gravity.CENTER);
        tag.setPadding(0, k.dp(12), 0, k.dp(12));
        ad.addView(tag);

        String[] services = {
                "ګرافیک ډیزاین: لوګو، پوسټر، بنر او د ټولنیزو رسنیو پوسټونه",
                "ویډیو ایډیټ",
                "موشن اعلانونه (Motion Ads)",
                "د سپانسر خدمات",
                "د موبایل اپلیکېشن جوړول"
        };
        for (String s : services) {
            LinearLayout r = k.row();
            r.setPadding(0, k.dp(5), 0, k.dp(5));
            TextView dot = k.text("✦", 13, GOLD);
            r.addView(dot);
            TextView t = k.text(s, 15, WHITE);
            t.setPadding(k.dp(10), 0, 0, 0);
            r.addView(t, k.weight());
            ad.addView(r);
        }
        TextView call = k.title("همدا اوس اړیکه ونیسئ", 15, GOLD);
        call.setGravity(Gravity.CENTER);
        call.setPadding(0, k.dp(16), 0, k.dp(10));
        ad.addView(call);
        ad.addView(k.goldButton("واټساپ: " + PHONE, v -> open(WHATSAPP_LINK)), k.matchWrap());
        LinearLayout more = k.row();
        more.addView(darkButton("ټلګرام", v -> open(TELEGRAM)), k.weight());
        more.addView(k.gap(8));
        more.addView(darkButton("فیسبوک", v -> open(FACEBOOK)), k.weight());
        ad.addView(more, k.topMargin(10));
        return ad;
    }

    private void buildDonate(LinearLayout root) {
        root.addView(header(glyphCircle("♥"), "زموږ ملاتړ", "ستاسو هره مرسته د نورو ګټورو کارونو لپاره ده"));

        LinearLayout intro = k.card(null, "ولې ملاتړ؟", null);
        LinearLayout who = k.row();
        who.setPadding(0, k.dp(4), 0, k.dp(10));
        who.addView(k.photo(52));
        LinearLayout wt = new LinearLayout(this);
        wt.setOrientation(LinearLayout.VERTICAL);
        wt.setPadding(k.dp(12), 0, 0, 0);
        wt.addView(k.title("زاهدالله الکوزی", 16, INK));
        wt.addView(k.text("د ذکر بار جوړونکی · غوره ګرافیکس پروډکشن", 12, MUTED));
        who.addView(wt, k.weight());
        intro.addView(who);
        intro.addView(para("ذکر بار وړیا دی او وړیا به پاتې وي. ستاسو ملاتړ له موږ سره مرسته کوي چې دا پروګرام نور هم ښه کړو "
                + "او د نورو داسې اسلامي او ګټورو پروګرامونو لپاره کار وکړو."));
        root.addView(intro);

        root.addView(payCard("ح", "HesabPay · حساب پی", HESAB, true));
        root.addView(payCard("ا", "Atoma Pay · اټوما پی", ATOMA, true));

        LinearLayout pay = k.card("P", "Payoneer · له بهر څخه", "که له افغانستان بهر یاست، دې ایمیل ته یې ولېږئ");
        pay.addView(valueBox(PAYONEER), k.topMargin(8));
        pay.addView(k.button("ایمیل کاپي کول", false, v -> copy("Payoneer", PAYONEER, PAYONEER)), k.topMargin(12));
        root.addView(pay);

        TextView dua = k.arabic("جَزَاكُمُ اللَّهُ خَيْرًا", 24, GREEN);
        dua.setPadding(0, k.dp(22), 0, 0);
        root.addView(dua);
    }

    /** A wallet card: shows the full number; copy gives the 9 digits without the leading 0. */
    private View payCard(String glyph, String title, String number, boolean dropZero) {
        LinearLayout c = k.card(glyph, title, "د موبایل شمېره");
        c.addView(valueBox(number), k.topMargin(8));
        String copyValue = dropZero && number.startsWith("0") ? number.substring(1) : number;
        c.addView(k.button("کاپي کول (" + copyValue + ")", false, v -> copy(title, copyValue,
                copyValue + " کاپي شو. په اپ کې یې د ۹ عددي شمېرې ځای کې پیسټ کړئ.")), k.topMargin(12));
        return c;
    }

    // ================= actions =================

    static void share(Context c) {
        String msg = "ذکر بار: د الله تعالی مبارک نوم ستاسو د موبایل په status bar کې، او تسبیح د هر اپ له پاسه.\n"
                + "فَاذْكُرُونِي أَذْكُرْكُمْ\n\nوړیا یې ښکته کړئ:\n" + SHARE_LINK;
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("text/plain");
        i.putExtra(Intent.EXTRA_TEXT, msg);
        c.startActivity(Intent.createChooser(i, "ملګرو ته لېږل"));
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
        h.setPadding(k.dp(20), k.dp(14), k.dp(20), k.dp(28));
        h.setBackground(k.headerBackground());

        TextView back = k.text("→  بېرته", 14, 0xE6FFFFFF);
        back.setPadding(k.dp(12), k.dp(4), k.dp(14), k.dp(6));
        back.setBackground(k.rounded(0x26FFFFFF, 14, 0));
        back.setOnClickListener(v -> finish());
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        bp.gravity = Gravity.START;
        bp.bottomMargin = k.dp(10);
        h.addView(back, bp);

        h.addView(top);
        TextView t = k.title(title, 28, WHITE);
        t.setGravity(Gravity.CENTER);
        t.setPadding(0, k.dp(14), 0, k.dp(4));
        h.addView(t);
        TextView s = k.text(sub, 13, 0xD9FFFFFF);
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

    private View contactRow(String label, String value, String url) {
        LinearLayout r = k.row();
        r.setPadding(k.dp(14), k.dp(10), k.dp(14), k.dp(12));
        r.setBackground(new android.graphics.drawable.RippleDrawable(
                android.content.res.ColorStateList.valueOf(0x201B5E20), k.rounded(SOFT, 16, LINE), null));
        r.setClickable(true);
        r.setOnClickListener(v -> open(url));
        r.addView(k.title(label, 15, INK), new LinearLayout.LayoutParams(k.dp(80), LinearLayout.LayoutParams.WRAP_CONTENT));
        TextView val = k.text(value, 15, GREEN);
        val.setTextDirection(View.TEXT_DIRECTION_LTR);
        val.setGravity(Gravity.END);
        r.addView(val, k.weight());
        r.addView(k.text("  ‹", 20, MUTED));
        LinearLayout.LayoutParams lp = k.topMargin(8);
        r.setLayoutParams(lp);
        return r;
    }

    private TextView darkButton(String s, View.OnClickListener l) {
        TextView b = k.strong(s, 14, GOLD);
        b.setGravity(Gravity.CENTER);
        b.setPadding(k.dp(12), k.dp(10), k.dp(12), k.dp(12));
        b.setBackground(new android.graphics.drawable.RippleDrawable(
                android.content.res.ColorStateList.valueOf(0x30F3E3A5), k.rounded(0x1AFFFFFF, 14, 0x66F3E3A5), null));
        b.setClickable(true);
        b.setOnClickListener(l);
        return b;
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
        TextView m = k.badge(mark, 32);
        r.addView(m);
        TextView t = k.text(s, 15, INK);
        t.setPadding(k.dp(12), 0, 0, 0);
        r.addView(t, k.weight());
        return r;
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
