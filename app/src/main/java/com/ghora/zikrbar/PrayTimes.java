package com.ghora.zikrbar;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Calendar;
import java.util.TimeZone;

/**
 * Prayer times worked out on the phone (no internet), with the astronomical method of PrayTimes.org.
 * Also the Hijri date (tabular calendar with a ± day offset) and the saved place and settings.
 */
final class PrayTimes {

    static final int FAJR = 0, SUNRISE = 1, DHUHR = 2, ASR = 3, MAGHRIB = 4, ISHA = 5;
    static final String[] KEYS = {"p_fajr", "p_sunrise", "p_dhuhr", "p_asr", "p_maghrib", "p_isha"};

    /** Calculation methods: name key, Fajr angle, Isha angle (or minutes after Maghrib if ishaMin). */
    static final String[] METHOD_KEYS = {"m_karachi", "m_mwl", "m_isna", "m_makkah", "m_egypt"};
    private static final double[][] METHODS = {{18, 18, 0}, {18, 17, 0}, {15, 15, 0}, {18.5, 90, 1}, {19.5, 17.5, 0}};

    private PrayTimes() {}

    // ---------------------------------------------------------------- settings
    static SharedPreferences sp(Context c) {
        return c.getSharedPreferences("prayer", Context.MODE_PRIVATE);
    }

    /** Saved city index in Cities, or -1 for the phone's location. Default: Kabul. */
    static int city(Context c) {
        return sp(c).getInt("city", 0);
    }

    static double lat(Context c) {
        int i = city(c);
        return i >= 0 && i < Cities.ALL.length ? Cities.ALL[i].lat : Double.longBitsToDouble(sp(c).getLong("lat", Double.doubleToLongBits(34.5553)));
    }

    static double lng(Context c) {
        int i = city(c);
        return i >= 0 && i < Cities.ALL.length ? Cities.ALL[i].lng : Double.longBitsToDouble(sp(c).getLong("lng", Double.doubleToLongBits(69.2075)));
    }

    static TimeZone zone(Context c) {
        int i = city(c);
        return i >= 0 && i < Cities.ALL.length ? TimeZone.getTimeZone(Cities.ALL[i].tz) : TimeZone.getDefault();
    }

    static String placeName(Context c) {
        int i = city(c);
        if (i >= 0 && i < Cities.ALL.length) return Cities.ALL[i].name();
        return L.t("my_location");
    }

    static void setCity(Context c, int i) {
        sp(c).edit().putInt("city", i).apply();
    }

    static void setLocation(Context c, double lat, double lng) {
        sp(c).edit().putInt("city", -1).putLong("lat", Double.doubleToLongBits(lat)).putLong("lng", Double.doubleToLongBits(lng)).apply();
    }

    static int method(Context c) {
        return sp(c).getInt("method", 0);
    }

    /** true = Hanafi Asr (shadow twice), false = Shafi'i/Maliki/Hanbali. Default Hanafi. */
    static boolean hanafi(Context c) {
        return sp(c).getBoolean("hanafi", true);
    }

    static int hijriOffset(Context c) {
        return sp(c).getInt("hijri", 0);
    }

    static boolean alertOn(Context c, int prayer) {
        return sp(c).getBoolean("alert_" + prayer, prayer != SUNRISE);
    }

    static void setAlertOn(Context c, int prayer, boolean on) {
        sp(c).edit().putBoolean("alert_" + prayer, on).apply();
    }

    // ---------------------------------------------------------------- times
    /** Times for that day as minutes after local midnight (in the place's time zone). */
    static int[] minutes(Context c, Calendar day) {
        TimeZone tz = zone(c);
        Calendar d = Calendar.getInstance(tz);
        d.setTimeInMillis(day.getTimeInMillis());
        double tzHours = tz.getOffset(d.getTimeInMillis()) / 3_600_000.0;
        double[] m = METHODS[Math.max(0, Math.min(METHODS.length - 1, method(c)))];
        double[] t = compute(d.get(Calendar.YEAR), d.get(Calendar.MONTH) + 1, d.get(Calendar.DAY_OF_MONTH),
                lat(c), lng(c), tzHours, m[0], m[1], m[2] == 1, hanafi(c) ? 2 : 1);
        int[] out = new int[6];
        for (int i = 0; i < 6; i++) out[i] = (int) Math.round(t[i] * 60) % (24 * 60);
        return out;
    }

    /** The moment (epoch ms) of a prayer on the given day, offsetDays from today. */
    static long when(Context c, int offsetDays, int prayer) {
        TimeZone tz = zone(c);
        Calendar d = Calendar.getInstance(tz);
        d.add(Calendar.DAY_OF_YEAR, offsetDays);
        int m = minutes(c, d)[prayer];
        d.set(Calendar.HOUR_OF_DAY, m / 60);
        d.set(Calendar.MINUTE, m % 60);
        d.set(Calendar.SECOND, 0);
        d.set(Calendar.MILLISECOND, 0);
        return d.getTimeInMillis();
    }

    static String clock(int minutes, boolean twelve) {
        int h = minutes / 60, m = minutes % 60;
        String mm = (m < 10 ? L.num(0) : "") + L.num(m);
        if (!twelve) return (h < 10 ? L.num(0) : "") + L.num(h) + ":" + mm;
        int hh = h % 12 == 0 ? 12 : h % 12;
        return L.num(hh) + ":" + mm + " " + L.t(h < 12 ? "am" : "pm");
    }

    private static double[] compute(int y, int mo, int day, double lat, double lng, double tz,
                                    double fajrAngle, double isha, boolean ishaMinutes, int asrFactor) {
        double jd = julian(y, mo, day) - lng / (15 * 24.0);
        double[] t = {5, 6, 12, 13, 18, 18};
        double fajr = sunAngleTime(jd, fajrAngle, t[0] / 24, lat, true);
        double sunrise = sunAngleTime(jd, 0.833, t[1] / 24, lat, true);
        double dhuhr = midDay(jd, t[2] / 24);
        double asr = asrTime(jd, asrFactor, t[3] / 24, lat);
        double sunset = sunAngleTime(jd, 0.833, t[4] / 24, lat, false);
        double ishaT = ishaMinutes ? sunset + isha / 60.0 : sunAngleTime(jd, isha, t[5] / 24, lat, false);

        // places far from the equator: keep Fajr/Isha inside the night (angle-based rule)
        double night = fix(sunrise - sunset, 24);
        if (Double.isNaN(fajr) || fix(sunrise - fajr, 24) > night * fajrAngle / 60.0) fajr = sunrise - night * fajrAngle / 60.0;
        if (!ishaMinutes && (Double.isNaN(ishaT) || fix(ishaT - sunset, 24) > night * isha / 60.0)) ishaT = sunset + night * isha / 60.0;

        double shift = tz - lng / 15.0;
        double[] out = {fajr, sunrise, dhuhr, asr, sunset, ishaT};
        for (int i = 0; i < out.length; i++) out[i] = fix(out[i] + shift, 24);
        return out;
    }

    private static double julian(int y, int m, int d) {
        if (m <= 2) {
            y -= 1;
            m += 12;
        }
        double a = Math.floor(y / 100.0);
        double b = 2 - a + Math.floor(a / 4);
        return Math.floor(365.25 * (y + 4716)) + Math.floor(30.6001 * (m + 1)) + d + b - 1524.5;
    }

    /** {declination, equation of time} */
    private static double[] sun(double jd) {
        double d = jd - 2451545.0;
        double g = fix(357.529 + 0.98560028 * d, 360);
        double q = fix(280.459 + 0.98564736 * d, 360);
        double l = fix(q + 1.915 * dsin(g) + 0.020 * dsin(2 * g), 360);
        double e = 23.439 - 0.00000036 * d;
        double ra = Math.toDegrees(Math.atan2(dcos(e) * dsin(l), dcos(l))) / 15;
        double eqt = q / 15 - fix(ra, 24);
        double decl = Math.toDegrees(Math.asin(dsin(e) * dsin(l)));
        return new double[]{decl, eqt};
    }

    private static double midDay(double jd, double t) {
        return fix(12 - sun(jd + t)[1], 24);
    }

    private static double sunAngleTime(double jd, double angle, double t, double lat, boolean ccw) {
        double decl = sun(jd + t)[0];
        double noon = midDay(jd, t);
        double x = (-dsin(angle) - dsin(decl) * dsin(lat)) / (dcos(decl) * dcos(lat));
        double tt = Math.toDegrees(Math.acos(x)) / 15;
        return noon + (ccw ? -tt : tt);
    }

    private static double asrTime(double jd, int factor, double t, double lat) {
        double decl = sun(jd + t)[0];
        double angle = -Math.toDegrees(Math.atan(1 / (factor + Math.tan(Math.toRadians(Math.abs(lat - decl))))));
        return sunAngleTime(jd, angle, t, lat, false);
    }

    private static double dsin(double d) {
        return Math.sin(Math.toRadians(d));
    }

    private static double dcos(double d) {
        return Math.cos(Math.toRadians(d));
    }

    private static double fix(double a, double b) {
        if (Double.isNaN(a)) return a;
        a = a - b * Math.floor(a / b);
        return a < 0 ? a + b : a;
    }

    // ---------------------------------------------------------------- Hijri date
    static final String[] HIJRI_MONTHS = {"مُحَرَّم", "صَفَر", "رَبِيع الأَوَّل", "رَبِيع الآخِر", "جُمَادَى الأُولَى",
            "جُمَادَى الآخِرَة", "رَجَب", "شَعْبَان", "رَمَضَان", "شَوَّال", "ذُو القَعْدَة", "ذُو الحِجَّة"};

    /** {year, month 1-12, day} */
    static int[] hijri(Context c, int extraDays) {
        Calendar d = Calendar.getInstance();
        d.add(Calendar.DAY_OF_YEAR, hijriOffset(c) + extraDays);
        int jd = (int) Math.floor(julian(d.get(Calendar.YEAR), d.get(Calendar.MONTH) + 1, d.get(Calendar.DAY_OF_MONTH)) + 0.5);
        int l = jd - 1948440 + 10632;
        int n = (l - 1) / 10631;
        l = l - 10631 * n + 354;
        int j = ((10985 - l) / 5316) * ((50 * l) / 17719) + (l / 5670) * ((43 * l) / 15238);
        l = l - ((30 - j) / 15) * ((17719 * j) / 50) - (j / 16) * ((15238 * j) / 43) + 29;
        int m = (24 * l) / 709;
        int day = l - (709 * m) / 24;
        int y = 30 * n + j - 30;
        return new int[]{y, m, day};
    }

    static String hijriText(Context c, int extraDays) {
        int[] h = hijri(c, extraDays);
        return L.num(h[2]) + " " + HIJRI_MONTHS[Math.max(0, Math.min(11, h[1] - 1))] + " " + L.num(h[0]);
    }
}
