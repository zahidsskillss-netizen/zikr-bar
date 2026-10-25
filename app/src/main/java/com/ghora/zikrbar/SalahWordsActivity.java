package com.ghora.zikrbar;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.graphics.drawable.RippleDrawable;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import static com.ghora.zikrbar.Kit.*;
import static com.ghora.zikrbar.TasbihActivity.*;

/**
 * Word-by-word Pashto translation of what is said in the prayer, for learning: tap a word to count
 * one more repetition (and hear it in Arabic), mark it learned, and get daily reminders.
 */
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

    static final String EXTRA_SECTION = "section";

    private Kit k;
    private ScrollView scroll;
    private LinearLayout root;
    private final List<View> cards = new ArrayList<>();
    private TextView progressText;
    private ProgressBar progressBar;
    private TextToSpeech tts;
    private boolean ttsReady;

    static SharedPreferences sp(Context c) {
        return c.getSharedPreferences("learn", Context.MODE_PRIVATE);
    }

    static int total() {
        int n = 0;
        for (String[] s : SECTIONS) n += (s.length - 1) / 2;
        return n;
    }

    static int learned(Context c) {
        int n = 0;
        for (int a = 0; a < SECTIONS.length; a++)
            for (int i = 1; i + 1 < SECTIONS[a].length; i += 2) if (sp(c).getBoolean("l_" + a + "_" + i, false)) n++;
        return n;
    }

    /** First word not yet learned, as {section, index}; null when all are learned. */
    static int[] nextWord(Context c) {
        for (int a = 0; a < SECTIONS.length; a++)
            for (int i = 1; i + 1 < SECTIONS[a].length; i += 2) if (!sp(c).getBoolean("l_" + a + "_" + i, false)) return new int[]{a, i};
        return null;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        L.init(this);
        k = new Kit(this);
        Theme.darkWindow(this);

        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        root.setPadding(0, 0, 0, k.dp(28));

        LinearLayout h = k.row();
        h.setBackgroundColor(D_HEAD);
        h.setPadding(k.dp(8), k.dp(10), k.dp(8), k.dp(10));
        TextView back = new TextView(this);
        back.setText("\u2192");
        back.setTextSize(22);
        back.setTextColor(D_TEXT);
        back.setGravity(Gravity.CENTER);
        back.setOnClickListener(v -> finish());
        h.addView(back, new LinearLayout.LayoutParams(k.dp(44), k.dp(44)));
        TextView t = k.title(L.t("sw_title"), 20, D_TEXT);
        t.setPaddingRelative(k.dp(14), 0, 0, 0);
        h.addView(t, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        root.addView(h);

        root.addView(learnCard());
        for (int a = 0; a < SECTIONS.length; a++) {
            View c = section(a);
            cards.add(c);
            root.addView(c);
        }

        scroll = new ScrollView(this);
        scroll.addView(root);
        setContentView(scroll);
        updateProgress();

        int go = getIntent().getIntExtra(EXTRA_SECTION, -1);
        if (go >= 0 && go < cards.size()) scroll.post(() -> scroll.smoothScrollTo(0, cards.get(go).getTop() - k.dp(8)));

        tts = new TextToSpeech(this, st -> {
            if (st == TextToSpeech.SUCCESS && tts != null) {
                int r = tts.setLanguage(new Locale("ar"));
                ttsReady = r != TextToSpeech.LANG_MISSING_DATA && r != TextToSpeech.LANG_NOT_SUPPORTED;
                tts.setSpeechRate(0.8f);
            }
        });
    }

    @Override
    protected void onDestroy() {
        if (tts != null) {
            tts.shutdown();
            tts = null;
        }
        super.onDestroy();
    }

    private void speak(String text) {
        if (!sp(this).getBoolean("voice", true)) return;
        if (tts == null || !ttsReady) {
            Toast.makeText(this, L.t("sw_no_voice"), Toast.LENGTH_LONG).show();
            return;
        }
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "w");
    }

    // ---------------------------------------------------------------- learning card
    private View learnCard() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setPadding(k.dp(18), k.dp(16), k.dp(18), k.dp(16));
        c.setBackground(k.glass(22));
        LinearLayout.LayoutParams lp = k.matchWrap();
        lp.setMargins(k.dp(12), k.dp(14), k.dp(12), 0);
        c.setLayoutParams(lp);

        TextView title = k.title(L.t("sw_learn"), 17, D_GOLD);
        c.addView(title);
        TextView how = k.text(L.t("sw_how"), 13, D_MUTED);
        how.setPadding(0, k.dp(6), 0, k.dp(10));
        c.addView(how);

        progressText = k.strong("", 14, D_TEXT);
        c.addView(progressText);
        progressBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progressBar.setMax(total());
        progressBar.setProgressTintList(ColorStateList.valueOf(D_GOLD));
        progressBar.setProgressBackgroundTintList(ColorStateList.valueOf(Theme.ov(0x33)));
        LinearLayout.LayoutParams pb = k.matchWrap();
        pb.setMargins(0, k.dp(6), 0, k.dp(12));
        c.addView(progressBar, pb);

        LinearLayout btns = k.row();
        TextView remind = k.button("\uD83D\uDD14\u2003" + L.t("sw_remind"), true, v -> reminderDialog());
        btns.addView(remind, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        TextView voice = k.button((sp(this).getBoolean("voice", true) ? "\uD83D\uDD0A" : "\uD83D\uDD07") + "\u2003" + L.t("sw_voice"), false, null);
        voice.setOnClickListener(v -> {
            boolean on = !sp(this).getBoolean("voice", true);
            sp(this).edit().putBoolean("voice", on).apply();
            voice.setText((on ? "\uD83D\uDD0A" : "\uD83D\uDD07") + "\u2003" + L.t("sw_voice"));
        });
        LinearLayout.LayoutParams vp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        vp.setMarginStart(k.dp(10));
        btns.addView(voice, vp);
        c.addView(btns);
        return c;
    }

    private void updateProgress() {
        int n = learned(this);
        progressText.setText(L.f("sw_progress", L.num(n), L.num(total())));
        progressBar.setProgress(n);
    }

    // ---------------------------------------------------------------- reminders
    private void reminderDialog() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(k.dp(20), k.dp(8), k.dp(20), 0);
        box.setLayoutDirection(L.rtl() ? View.LAYOUT_DIRECTION_RTL : View.LAYOUT_DIRECTION_LTR);
        fillReminder(box);
        new AlertDialog.Builder(this).setTitle(L.t("sw_remind")).setView(box)
                .setPositiveButton(L.t("ok"), null).show();
    }

    private void fillReminder(LinearLayout box) {
        box.removeAllViews();
        LinearLayout r = k.row();
        TextView l = k.strong(L.t("sw_remind_on"), 15, 0xFF16231A);
        r.addView(l, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        Switch sw = new Switch(this);
        sw.setChecked(LearnAlarm.on(this));
        sw.setOnCheckedChangeListener((b, on) -> {
            LearnAlarm.setOn(this, on);
            if (on && LearnAlarm.times(this).isEmpty()) LearnAlarm.setTimes(this, defaultTimes());
            LearnAlarm.schedule(this);
            fillReminder(box);
        });
        r.addView(sw);
        box.addView(r);

        TextView s = k.text(L.t("sw_remind_s"), 13, 0xFF66746A);
        s.setPadding(0, k.dp(6), 0, k.dp(8));
        box.addView(s);

        List<Integer> times = LearnAlarm.times(this);
        for (int i = 0; i < times.size(); i++) {
            final int m = times.get(i);
            LinearLayout tr = k.row();
            tr.setPadding(0, k.dp(4), 0, k.dp(4));
            TextView tt = k.title(PrayTimes.clock(m, true), 17, 0xFF1B5E20);
            tt.setTextDirection(View.TEXT_DIRECTION_LTR);
            tr.addView(tt, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
            TextView del = k.strong("\u2715", 16, 0xFFB3261E);
            del.setPadding(k.dp(14), k.dp(6), k.dp(14), k.dp(6));
            del.setOnClickListener(v -> {
                List<Integer> t2 = LearnAlarm.times(this);
                t2.remove(Integer.valueOf(m));
                LearnAlarm.setTimes(this, t2);
                LearnAlarm.schedule(this);
                fillReminder(box);
            });
            tr.addView(del);
            box.addView(tr);
        }
        if (times.size() < 12) {
            TextView add = k.strong("\uFF0B\u2003" + L.t("sw_add_time"), 15, 0xFF1B5E20);
            add.setPadding(0, k.dp(10), 0, k.dp(10));
            add.setOnClickListener(v -> new TimePickerDialog(this, android.R.style.Theme_Material_Light_Dialog, (pk, hh, mm) -> {
                List<Integer> t2 = LearnAlarm.times(this);
                int val = hh * 60 + mm;
                if (!t2.contains(val)) t2.add(val);
                Collections.sort(t2);
                LearnAlarm.setTimes(this, t2);
                LearnAlarm.setOn(this, true);
                LearnAlarm.schedule(this);
                fillReminder(box);
            }, 9, 0, false).show());
            box.addView(add);
        }
        TextView cnt = k.text(L.f("sw_times_day", L.num(times.size())), 13, 0xFF66746A);
        cnt.setPadding(0, k.dp(2), 0, k.dp(8));
        box.addView(cnt);
    }

    private static List<Integer> defaultTimes() {
        List<Integer> t = new ArrayList<>();
        t.add(9 * 60);
        t.add(14 * 60);
        t.add(20 * 60);
        return t;
    }

    // ---------------------------------------------------------------- one section
    private View section(int a) {
        String[] s = SECTIONS[a];
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(k.glass(22));
        LinearLayout.LayoutParams lp = k.matchWrap();
        lp.setMargins(k.dp(12), k.dp(14), k.dp(12), 0);
        card.setLayoutParams(lp);

        StringBuilder full = new StringBuilder();
        for (int i = 1; i < s.length; i += 2) {
            if (full.length() > 0) full.append(' ');
            full.append(s[i]);
        }

        // title band with a "listen" button
        LinearLayout band = k.row();
        band.setPadding(k.dp(14), k.dp(10), k.dp(10), k.dp(10));
        band.setBackground(k.rounded(Theme.ov(0x1F), 22, 0));
        TextView title = k.title(s[0], 17, D_GOLD);
        band.addView(title, new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        TextView listen = new TextView(this);
        listen.setText("\uD83D\uDD0A");
        listen.setTextSize(18);
        listen.setGravity(Gravity.CENTER);
        listen.setContentDescription(L.t("sw_voice"));
        listen.setBackground(k.rounded(Theme.ov(0x1A), 20, 0));
        listen.setOnClickListener(v -> speak(full.toString()));
        band.addView(listen, new LinearLayout.LayoutParams(k.dp(40), k.dp(40)));
        card.addView(band, k.matchWrap());

        TextView all = k.arabic(full.toString(), 22, D_TEXT);
        all.setLineSpacing(0, 1.5f);
        all.setPadding(k.dp(16), k.dp(14), k.dp(16), k.dp(12));
        card.addView(all, k.matchWrap());

        LinearLayout head = k.row();
        head.setBackgroundColor(Theme.ov(0x10));
        head.addView(cell(k.strong(L.t("sw_col_ar"), 12.5f, D_MUTED)), new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        head.addView(cell(k.strong(L.t("sw_col_n"), 12.5f, D_MUTED)), new LinearLayout.LayoutParams(k.dp(46), LinearLayout.LayoutParams.WRAP_CONTENT));
        head.addView(cell(k.strong(L.t("sw_col_ps"), 12.5f, D_MUTED)), new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        head.addView(cell(k.strong(L.t("sw_col_ok"), 12.5f, D_MUTED)), new LinearLayout.LayoutParams(k.dp(52), LinearLayout.LayoutParams.WRAP_CONTENT));
        card.addView(head, k.matchWrap());

        for (int i = 1; i + 1 < s.length; i += 2) card.addView(wordRow(a, i, s[i], s[i + 1]), k.matchWrap());
        View end = new View(this);
        card.addView(end, new LinearLayout.LayoutParams(1, k.dp(8)));
        return card;
    }

    private View wordRow(int a, int i, String ar, String ps) {
        String key = a + "_" + i;
        LinearLayout r = k.row();
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setMinimumHeight(k.dp(52));

        TextView arT = k.arabic(ar, 20, D_TEXT);
        arT.setTextDirection(View.TEXT_DIRECTION_RTL);
        r.addView(cell(arT), new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        TextView count = k.title("", 14, D_TEXT);
        count.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(k.dp(36), k.dp(36));
        cp.setMargins(k.dp(5), 0, k.dp(5), 0);
        r.addView(count, cp);

        TextView psT = k.text(ps, 15, D_TEXT);
        psT.setTypeface(k.strong);
        psT.setTextDirection(View.TEXT_DIRECTION_RTL);
        r.addView(cell(psT), new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));

        TextView ok = new TextView(this);
        ok.setGravity(Gravity.CENTER);
        ok.setTextSize(16);
        ok.setContentDescription(L.t("sw_col_ok"));
        LinearLayout.LayoutParams op = new LinearLayout.LayoutParams(k.dp(40), k.dp(40));
        op.setMargins(k.dp(6), 0, k.dp(6), 0);
        r.addView(ok, op);

        Runnable paint = () -> {
            int n = sp(this).getInt("c_" + key, 0);
            boolean done = sp(this).getBoolean("l_" + key, false);
            count.setText(n > 0 ? L.num(n) : "\u00B7");
            count.setBackground(k.rounded(n > 0 ? Theme.ov(0x26) : 0x00000000, 18, n > 0 ? D_GOLD : 0));
            ok.setText(done ? "\u2713" : "");
            ok.setTextColor(done ? 0xFFFFFFFF : D_MUTED);
            ok.setBackground(k.rounded(done ? 0xFF2E9E5B : 0x00000000, 20, done ? 0 : D_MUTED));
            r.setBackground(new RippleDrawable(ColorStateList.valueOf(Theme.ov(0x30)),
                    done ? k.rounded(0x332E9E5B, 0, 0) : null, k.rounded(0xFFFFFFFF, 0, 0)));
        };
        paint.run();
        r.setOnClickListener(v -> {
            int n = sp(this).getInt("c_" + key, 0) + 1;
            sp(this).edit().putInt("c_" + key, n).apply();
            paint.run();
            count.animate().cancel();
            count.setScaleX(1.3f);
            count.setScaleY(1.3f);
            count.animate().scaleX(1f).scaleY(1f).setDuration(180).start();
            speak(ar);
        });
        ok.setOnClickListener(v -> {
            boolean done = !sp(this).getBoolean("l_" + key, false);
            sp(this).edit().putBoolean("l_" + key, done).apply();
            paint.run();
            updateProgress();
            if (done) Toast.makeText(this, L.t("sw_learned"), Toast.LENGTH_SHORT).show();
        });
        return r;
    }

    private TextView cell(TextView t) {
        t.setGravity(Gravity.CENTER);
        t.setPadding(k.dp(8), k.dp(10), k.dp(8), k.dp(10));
        return t;
    }
}
