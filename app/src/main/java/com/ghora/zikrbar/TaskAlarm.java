package com.ghora.zikrbar;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Rings a task at its time, and handles the "done" and "+10 min" buttons of its notification. */
public class TaskAlarm extends BroadcastReceiver {

    @Override
    public void onReceive(Context c, Intent intent) {
        L.init(c);
        String a = intent.getAction();
        long id = intent.getLongExtra(Tasks.EXTRA_ID, 0);
        if (Tasks.ACTION_DUE.equals(a)) {
            Tasks.ring(c);
        } else if (Tasks.ACTION_DONE.equals(a)) {
            Tasks.setDone(c, id);
        } else if (Tasks.ACTION_SNOOZE.equals(a)) {
            Tasks.reschedule(c, id, Tasks.in(10));
        }
    }
}
