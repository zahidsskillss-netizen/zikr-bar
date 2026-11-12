package com.ghora.zikrbar;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.SoundPool;

/** The bead "tick" for every dhikr counted and a soft chime when the goal is reached (both made for this app). */
final class Sfx {

    private static SoundPool pool;
    private static int bead, chime;
    private static boolean readyBead, readyChime;

    private Sfx() {}

    static synchronized void prepare(Context c) {
        if (pool != null) return;
        pool = new SoundPool.Builder()
                .setMaxStreams(4)
                .setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build())
                .build();
        pool.setOnLoadCompleteListener((sp, id, status) -> {
            if (id == bead) readyBead = status == 0;
            if (id == chime) readyChime = status == 0;
        });
        bead = pool.load(c.getApplicationContext(), R.raw.bead, 1);
        chime = pool.load(c.getApplicationContext(), R.raw.chime, 1);
    }

    static synchronized void bead() {
        if (pool != null && readyBead) pool.play(bead, 0.9f, 0.9f, 1, 0, 1f);
    }

    static synchronized void chime() {
        if (pool != null && readyChime) pool.play(chime, 0.8f, 0.8f, 1, 0, 1f);
    }

    static synchronized void release() {
        if (pool != null) {
            pool.release();
            pool = null;
            readyBead = false;
            readyChime = false;
        }
    }
}
