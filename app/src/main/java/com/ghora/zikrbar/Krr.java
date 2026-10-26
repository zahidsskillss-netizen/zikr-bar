package com.ghora.zikrbar;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.SoundPool;

/** The short "krrr" tick played for each dhikr counted by the roller (and the face counter). */
final class Krr {

    private static SoundPool pool;
    private static int id;
    private static boolean ready;

    private Krr() {}

    /** Loads the sound ahead of time so the first tick is not lost. */
    static synchronized void prepare(Context c) {
        if (pool != null) return;
        pool = new SoundPool.Builder()
                .setMaxStreams(4)
                .setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build())
                .build();
        pool.setOnLoadCompleteListener((sp, sampleId, status) -> ready = status == 0);
        id = pool.load(c.getApplicationContext(), R.raw.krr, 1);
    }

    static synchronized void play(Context c) {
        if (pool == null) {
            prepare(c);
            return;
        }
        if (ready) pool.play(id, 0.9f, 0.9f, 1, 0, 1f);
    }

    static synchronized void release() {
        if (pool != null) {
            pool.release();
            pool = null;
            ready = false;
        }
    }
}
