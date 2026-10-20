package com.ghora.zikrbar;

import android.content.Context;
import android.graphics.Typeface;

/** Shared fonts and number formatting. */
final class Ui {
    private static Typeface amiri, amiriBold;

    private Ui() {}

    static Typeface amiri(Context c) {
        if (amiri == null) amiri = c.getResources().getFont(R.font.amiri);
        return amiri;
    }

    static Typeface amiriBold(Context c) {
        if (amiriBold == null) amiriBold = c.getResources().getFont(R.font.amiri_bold);
        return amiriBold;
    }

    /** Writes a number with Pashto digits (۰۱۲۳...). */
    static String fa(int n) {
        String s = String.valueOf(n);
        StringBuilder b = new StringBuilder();
        for (char ch : s.toCharArray()) {
            b.append(ch >= '0' && ch <= '9' ? (char) ('۰' + (ch - '0')) : ch);
        }
        return b.toString();
    }
}
