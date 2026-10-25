package com.ghora.zikrbar;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.drawable.Icon;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;

import java.io.File;

/** Plays the long beep or the user's own sound for a prayer, with a message that has a Stop button. */
public class AdhanService extends Service {

    static final String ACTION_STOP = "com.ghora.zikrbar.STOP_ADHAN";
    private static final String CHANNEL = "prayer_playing";
    private static final int ID = 700;
    private MediaPlayer player;
    private int prayer = -1;
    private boolean testing;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        L.init(this);
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            stopAll();
            return START_NOT_STICKY;
        }
        int p = intent == null ? 0 : intent.getIntExtra("p", 0);
        boolean test = intent != null && intent.getBooleanExtra("test", false);
        prayer = p;
        testing = test;
        int mode = intent != null && intent.hasExtra("mode") ? intent.getIntExtra("mode", PrayTimes.A_BEEP) : PrayTimes.alertMode(this, p);
        Notification n = build(p, test);
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
        } else {
            startForeground(ID, n);
        }
        play(p, mode);
        return START_NOT_STICKY;
    }

    private void play(int p, int mode) {
        release();
        try {
            player = new MediaPlayer();
            player.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build());
            String path = PrayTimes.customPath(this, p);
            if (mode == PrayTimes.A_CUSTOM && path != null && new File(path).exists()) {
                player.setDataSource(path);
            } else {
                player.setDataSource(this, Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.long_beep));
            }
            player.setOnCompletionListener(m -> stopAll());
            player.setOnErrorListener((m, w, e) -> {
                stopAll();
                return true;
            });
            player.prepare();
            player.start();
        } catch (Exception e) {
            stopAll();
        }
    }

    private Notification build(int p, boolean test) {
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (Build.VERSION.SDK_INT >= 26 && nm != null) createChannel(nm);
        PendingIntent stop = PendingIntent.getService(this, 720,
                new Intent(this, AdhanService.class).setAction(ACTION_STOP),
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        Icon icon = Icon.createWithBitmap(Ui.allahIcon(this));
        int min = PrayTimes.minutes(this, java.util.Calendar.getInstance())[p];
        return Ui.notif(this, CHANNEL, true)
                .setSmallIcon(icon)
                .setContentTitle(test ? L.t("pa_test") : PrayerAlarm.title(this, p))
                .setContentText(PrayTimes.placeName(this) + " · " + PrayTimes.clock(min, true))
                .setColor(0xFF1B5E20)
                .setContentIntent(PrayerAlarm.openPage(this))
                .addAction(new Notification.Action.Builder(icon, L.t("pa_stop"), stop).build())
                .build();
    }

    @android.annotation.TargetApi(26)
    private void createChannel(NotificationManager nm) {
        NotificationChannel ch = new NotificationChannel(CHANNEL, L.t("prayer_t"), NotificationManager.IMPORTANCE_HIGH);
        ch.setSound(null, null);
        ch.enableVibration(false);
        nm.createNotificationChannel(ch);
    }

    private void release() {
        if (player != null) {
            try {
                player.stop();
            } catch (RuntimeException ignored) {
            }
            player.release();
            player = null;
        }
    }

    /** Stops the sound; for a real prayer alert a quiet message stays in the notification list. */
    private void stopAll() {
        release();
        if (Build.VERSION.SDK_INT >= 24) {
            stopForeground(STOP_FOREGROUND_REMOVE);
        } else {
            stopForegroundOld();
        }
        if (prayer >= 0 && !testing) PrayerAlarm.show(this, prayer, PrayTimes.A_SILENT);
        prayer = -1;
        stopSelf();
    }

    @SuppressWarnings("deprecation")
    private void stopForegroundOld() {
        stopForeground(true);
    }

    @Override
    public void onDestroy() {
        release();
        super.onDestroy();
    }
}
