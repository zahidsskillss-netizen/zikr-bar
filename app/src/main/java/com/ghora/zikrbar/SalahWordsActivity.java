package com.ghora.zikrbar;

import android.app.Activity;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import static com.ghora.zikrbar.Kit.*;
import static com.ghora.zikrbar.TasbihActivity.*;

/** Word-by-word Pashto translation of what is said in the prayer, as two-column tables. */
public class SalahWordsActivity extends Activity {

    /** Each section: title, then pairs of Arabic word and its Pashto meaning. */
    static final String[][] SECTIONS = {
            {"ثنا",
                    "سُبْحَانَكَ اللَّهُمَّ", "پاکي ده تالره اى الله!",
                    "وَبِحَمْدِكَ", "او تالره ثنا ده",
                    "وَتَبَارَكَ اسْمُكَ", "او برکت والا دى نوم ستا",
                    "وَتَعَالَى جَدُّكَ", "او لوړ دى شان ستا",
                    "وَلَا إِلَهَ غَيْرُكَ", "نشته د عبادت لايق بې له تانه"},
            {"تعوذ",
                    "أَعُوذُ", "پناه غواړم",
                    "بِاللَّهِ", "په الله تعالى",
                    "مِنَ الشَّيْطَانِ", "د شيطان",
                    "الرَّجِيمِ", "رټلي شوي نه"},
            {"تسمیه",
                    "بِسْمِ اللَّهِ", "شروع کوم په نوم د الله",
                    "الرَّحْمَنِ", "چې عام مهربانه دى",
                    "الرَّحِيمِ", "خاص رحم کوونکى دی"},
            {"سورة الفاتحه",
                    "اَلْحَمْدُ", "ټول صفتونه",
                    "لِلّٰهِ", "خاص الله لره دي",
                    "رَبِّ الْعٰلَمِیْنَ ۝۱", "چې تربيت کوونکى د مخلوقاتو دى",
                    "الرَّحْمٰنِ", "چې عام مهربانه دى",
                    "الرَّحِیْمِ ۝۲", "خاص رحم کوونکى دى",
                    "مٰلِكِ یَوْمِ الدِّیْنِ ۝۳", "واکدار د قيامت د ورځې دى",
                    "اِیَّاكَ", "خاص تاته",
                    "نَعْبُدُ", "عبادت کوو",
                    "وَ اِیَّاكَ", "او خاص تا نه",
                    "نَسْتَعِیْنُ ۝۴", "مرسته غواړو",
                    "اِهْدِنَا", "برابر کړې موږ",
                    "الصِّرَاطَ", "په لار",
                    "الْمُسْتَقِیْمَ ۝۵", "سيده باندې",
                    "صِرَاطَ الَّذِیْنَ", "په لاره د هغه کسانو",
                    "اَنْعَمْتَ", "چې تا فضل کړى",
                    "عَلَیْهِمْ ۝۶", "په هغوى",
                    "غَیْرِ", "نه لار د هغه کسانو",
                    "الْمَغْضُوْبِ", "چې غضب کړى شوى",
                    "عَلَیْهِمْ", "په هغوى",
                    "وَ لَا الضَّآلِّیْنَ ۝۷", "او نه لار د ګمراهانو",
                    "آمِيْن", "اى الله! زموږ دعا قبوله کړې."},
            {"سورة الاخلاص",
                    "قُلْ هُوَ اللّٰهُ", "ووايه، هغه الله تعالى",
                    "اَحَدٌ ۝۱", "يو دى",
                    "اَللّٰهُ", "الله",
                    "الصَّمَدُ ۝۲", "بې نيازه دى",
                    "لَمْ یَلِدْ", "نه يې څوک زېږولي",
                    "وَ لَمْ یُوْلَدْ ۝۳", "او نه له چانه زېږېدلي",
                    "وَ لَمْ یَكُنْ لَّهٗ", "او نشته د هغه لپاره",
                    "كُفُوًا اَحَدٌ ۝۴", "برابر، هیڅوک"},
            {"تکبیر (رکوع ته تګ)",
                    "اللَّهُ أَكْبَرُ", "الله تعالي ډېر لوی دی"},
            {"د رکوع تسبیح",
                    "سُبْحَانَ", "پاکي ده",
                    "رَبِّيَ الْعَظِيمِ", "رب زما لره، چې لوى دى"},
            {"له رکوع پورته کېدل (تسمیع)",
                    "سَمِعَ اللَّهُ", "اوري الله تعالى",
                    "لِمَنْ", "وينا د هغه چا",
                    "حَمِدَهُ", "چې صفت يې کوي"},
            {"تحمید",
                    "رَبَّنَا", "اى ربه زموږ",
                    "لَكَ الْحَمْدُ", "تالره صفت دى"},
            {"تکبیر (سجدې ته تګ)",
                    "اللَّهُ أَكْبَرُ", "الله تعالى ډېر لوى دى"},
            {"د سجدې تسبیح",
                    "سُبْحَانَ", "پاکي ده",
                    "رَبِّيَ", "رب زما لره",
                    "الْأَعْلَى", "چې اوچت دى"},
            {"تکبیر (له سجدې پورته کېدل)",
                    "اللَّهُ أَكْبَرُ", "الله تعالى ډېر لوى دى"},
            {"تشهد (التحیات)",
                    "التَّحِيَّاتُ", "ټول وينا والا عبادتونه",
                    "لِلَّهِ", "خاص الله لره دي",
                    "وَالصَّلَوَاتُ", "او ټول بدني عبادتونه",
                    "وَالطَّيِّبَاتُ", "او ټول مالي عبادتونه",
                    "السَّلَامُ عَلَيْكَ", "سلام دې وي په تا باندې",
                    "أَيُّهَا النَّبِيُّ", "اى پیغمبره",
                    "وَرَحْمَةُ اللَّهِ", "او رحمت د الله تعالى",
                    "وَبَرَكَاتُهُ", "او برکتونه د هغه (الله تعالی)",
                    "السَّلَامُ عَلَيْنَا", "سلام دې وي، پر موږ باندې",
                    "وَعَلَى", "او په",
                    "عِبَادِ", "بنده ګانو",
                    "اللَّهِ", "د الله تعالى",
                    "الصَّالِحِينَ", "نېکانو باندې",
                    "أَشْهَدُ", "زه ګواهي کوم",
                    "أَنْ لَا إِلَهَ", "چې نشته د عبادت لايق",
                    "إِلَّا اللَّهُ", "مګر الله تعالى دى",
                    "وَأَشْهَدُ", "او زه ګواهي کوم",
                    "أَنَّ مُحَمَّدًا", "چې بېشکه محمد صلی الله علیه وسلم",
                    "عَبْدُهُ", "بنده د هغه (الله تعالی) دى",
                    "وَرَسُولُهُ", "او استازى د هغه (الله تعالی) دى"},
            {"درود شریف (لومړۍ برخه)",
                    "اللَّهُمَّ", "اى الله جل جلاله",
                    "صَلِّ", "رحم وکړې",
                    "عَلَى مُحَمَّدٍ", "په محمد صلی الله علیه وسلم",
                    "وَعَلَى آلِ مُحَمَّدٍ", "او په کورنۍ د محمد صلی الله علیه وسلم",
                    "كَمَا صَلَّيْتَ", "لکه څرنګه چې تا رحم کړى",
                    "عَلَى إِبْرَاهِيمَ", "په ابراهيم عليه السلام",
                    "وَعَلَى آلِ إِبْرَاهِيمَ", "او په کورنۍ د ابراهيم عليه السلام",
                    "إِنَّكَ", "بې شکه ته",
                    "حَمِيدٌ", "ستايلى شوى",
                    "مَجِيدٌ", "لوړ شان والا يې"},
            {"درود شریف (دوهمه برخه)",
                    "اللَّهُمَّ بَارِكْ", "اى الله برکت واوروې",
                    "عَلَى مُحَمَّدٍ", "په محمد صلی الله علیه وسلم",
                    "وَعَلَى آلِ مُحَمَّدٍ", "او په اولادې د محمد صلی الله علیه وسلم",
                    "كَمَا بَارَكْتَ", "لکه تا چې برکت ورولى و",
                    "عَلَى إِبْرَاهِيمَ", "په ابراهيم عليه السلام",
                    "وَعَلَى آلِ إِبْرَاهِيمَ", "او په اولادې د ابراهيم عليه السلام",
                    "إِنَّكَ", "بېشکه ته",
                    "حَمِيدٌ", "ستايلى شوى",
                    "مَجِيدٌ", "لوړ شان والا يې"},
            {"دعا (له درود وروسته)",
                    "اللَّهُمَّ", "اى الله",
                    "رَبَّنَا", "زموږ ربه",
                    "آتِنَا", "راکړې مونږ ته",
                    "فِي الدُّنْيَا", "په دنيا کې",
                    "حَسَنَةً", "نېکي",
                    "وَفِي الْآخِرَةِ", "او په اخرت کې",
                    "حَسَنَةً", "نېکي",
                    "وَقِنَا", "او وساتې موږ",
                    "عَذَابَ النَّارِ", "د عذاب د اور نه."},
            {"د وتر قنوت",
                    "اللَّهُمَّ", "اى الله",
                    "إِنَّا", "بېشکه موږ",
                    "نَسْتَعِينُكَ", "مرسته غواړو تانه",
                    "وَنَسْتَغْفِرُكَ", "او تانه بښنه غواړو",
                    "وَنُؤْمِنُ بِكَ", "او ايمان مو راوړى په تا باندې",
                    "وَنَتَوَكَّلُ", "او توکل کوو",
                    "عَلَيْكَ", "په تاباندې",
                    "وَنُثْنِي", "او صفت کوو",
                    "عَلَيْكَ الْخَيْرَ", "ستا غوره",
                    "وَنَشْكُرُكَ", "او موږ شکر کوو ستا",
                    "وَلَا نَكْفُرُكَ", "او ستا ناشکري نه کوو",
                    "وَنَخْلَعُ", "موږ جدا کېږو",
                    "وَنَتْرُكُ", "موږ پرېږدو",
                    "مَنْ", "هغه څوک",
                    "يَفْجُرُكَ", "چې ستا نافرماني کوي",
                    "اللَّهُمَّ", "يا الله مونږه",
                    "إِيَّاكَ", "خاص تالره",
                    "نَعْبُدُ", "عبادت کوو",
                    "وَلَكَ نُصَلِّي", "او تاته مونځ کوو",
                    "وَنَسْجُدُ", "او سجده کوو",
                    "وَإِلَيْكَ", "او ستا طرف ته",
                    "نَسْعَى", "منډې وهو",
                    "وَنَحْفِدُ", "او تېزي کوو",
                    "وَنَرْجُو", "او اميد لرو",
                    "رَحْمَتَكَ", "ستا د رحمت",
                    "وَنَخْشَى", "او وېرېږو",
                    "عَذَابَكَ", "ستا د عذاب نه",
                    "إِنَّ عَذَابَكَ", "بېشکه عذاب ستا",
                    "بِالْكُفَّارِ", "کافرانو پورې",
                    "مُلْحِقٌ", "پېوسته کېدونکى دى."},
    };

    private Kit k;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        L.init(this);
        k = new Kit(this);
        getWindow().setStatusBarColor(D_HEAD);
        getWindow().setNavigationBarColor(D_BG);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        root.setPadding(0, 0, 0, k.dp(28));

        LinearLayout h = k.row();
        h.setBackgroundColor(D_HEAD);
        h.setPadding(k.dp(8), k.dp(10), k.dp(8), k.dp(10));
        TextView back = new TextView(this);
        back.setText("→");
        back.setTextSize(22);
        back.setTextColor(D_TEXT);
        back.setGravity(Gravity.CENTER);
        back.setOnClickListener(v -> finish());
        h.addView(back, new LinearLayout.LayoutParams(k.dp(44), k.dp(44)));
        TextView t = k.title(L.t("sw_title"), 20, D_TEXT);
        t.setPaddingRelative(k.dp(10), 0, 0, 0);
        h.addView(t, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        root.addView(h);

        for (String[] s : SECTIONS) root.addView(section(s));

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(D_BG);
        scroll.addView(root);
        setContentView(scroll);
    }

    private View section(String[] s) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(k.rounded(D_CARD, 22, 0x14FFFFFF));
        card.setElevation(k.dp(3));
        LinearLayout.LayoutParams lp = k.matchWrap();
        lp.setMargins(k.dp(12), k.dp(14), k.dp(12), 0);
        card.setLayoutParams(lp);

        // title band
        TextView title = k.title(s[0], 17, D_GOLD);
        title.setGravity(Gravity.CENTER);
        title.setPadding(k.dp(12), k.dp(12), k.dp(12), k.dp(10));
        title.setBackground(k.rounded(0xFF1F3A32, 22, 0));
        card.addView(title, k.matchWrap());

        // whole text
        StringBuilder full = new StringBuilder();
        for (int i = 1; i < s.length; i += 2) {
            if (full.length() > 0) full.append(' ');
            full.append(s[i]);
        }
        TextView all = k.arabic(full.toString(), 22, D_TEXT);
        all.setLineSpacing(0, 1.5f);
        all.setPadding(k.dp(16), k.dp(14), k.dp(16), k.dp(12));
        card.addView(all, k.matchWrap());

        // column headings
        LinearLayout head = k.row();
        head.setBackgroundColor(0x22000000);
        head.addView(cell(k.strong("عربي", 12.5f, D_MUTED)), new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        head.addView(line(), new LinearLayout.LayoutParams(k.dp(1), LinearLayout.LayoutParams.MATCH_PARENT));
        head.addView(cell(k.strong("پښتو ژباړه", 12.5f, D_MUTED)), new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        card.addView(head, k.matchWrap());

        for (int i = 1; i + 1 < s.length; i += 2) {
            LinearLayout r = k.row();
            r.setGravity(Gravity.CENTER_VERTICAL);
            if ((i / 2) % 2 == 1) r.setBackgroundColor(0x0DFFFFFF);
            TextView ar = k.arabic(s[i], 20, D_TEXT);
            ar.setTextDirection(View.TEXT_DIRECTION_RTL);
            r.addView(cell(ar), new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
            r.addView(line(), new LinearLayout.LayoutParams(k.dp(1), LinearLayout.LayoutParams.MATCH_PARENT));
            TextView ps = k.text(s[i + 1], 15, 0xFFE3F1EA);
            ps.setTypeface(k.strong);
            ps.setTextDirection(View.TEXT_DIRECTION_RTL);
            r.addView(cell(ps), new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
            View sep = new View(this);
            sep.setBackgroundColor(0x14FFFFFF);
            card.addView(sep, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, k.dp(1)));
            card.addView(r, k.matchWrap());
        }
        View end = new View(this);
        card.addView(end, new LinearLayout.LayoutParams(1, k.dp(8)));
        return card;
    }

    private TextView cell(TextView t) {
        t.setGravity(Gravity.CENTER);
        t.setPadding(k.dp(10), k.dp(10), k.dp(10), k.dp(10));
        t.setEllipsize(TextUtils.TruncateAt.END);
        return t;
    }

    private View line() {
        View v = new View(this);
        v.setBackgroundColor(0x22FFFFFF);
        return v;
    }
}
