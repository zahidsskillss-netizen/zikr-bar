package com.ghora.zikrbar;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private EditText listInput;
    private Spinner intervalSpinner;
    private TextView status;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(buildUi());
        askNotificationPermission();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshStatus();
    }

    private View buildUi() {
        int pad = dp(20);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        root.setPadding(pad, pad, pad, pad);

        TextView title = new TextView(this);
        title.setText("ذکر بار");
        title.setTextSize(28);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setTextColor(Color.parseColor("#1B5E20"));
        title.setGravity(Gravity.CENTER);
        root.addView(title);

        TextView sub = new TextView(this);
        sub.setText("فَاذْكُرُونِي أَذْكُرْكُمْ");
        sub.setTextSize(18);
        sub.setGravity(Gravity.CENTER);
        sub.setPadding(0, dp(4), 0, dp(16));
        root.addView(sub);

        status = new TextView(this);
        status.setTextSize(16);
        status.setGravity(Gravity.CENTER);
        status.setPadding(0, 0, 0, dp(12));
        root.addView(status);

        root.addView(label("ذکرونه — هر ذکر په یوه کرښه کې ولیکئ:"));

        listInput = new EditText(this);
        listInput.setText(Prefs.rawList(this));
        listInput.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        listInput.setSingleLine(false);
        listInput.setMinLines(8);
        listInput.setGravity(Gravity.TOP | Gravity.START);
        listInput.setTextDirection(View.TEXT_DIRECTION_RTL);
        root.addView(listInput);

        root.addView(label("څو دقیقې وروسته ذکر بدل شي:"));

        intervalSpinner = new Spinner(this);
        String[] labels = new String[Prefs.INTERVALS.length];
        int selected = 0;
        for (int i = 0; i < Prefs.INTERVALS.length; i++) {
            labels[i] = Prefs.INTERVALS[i] + " دقیقې";
            if (Prefs.INTERVALS[i] == Prefs.intervalMinutes(this)) selected = i;
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_dropdown_item, labels);
        intervalSpinner.setAdapter(adapter);
        intervalSpinner.setSelection(selected);
        root.addView(intervalSpinner);

        Button start = new Button(this);
        start.setText("ساتل او پیلول");
        start.setOnClickListener(v -> startBar());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(20);
        root.addView(start, lp);

        Button stop = new Button(this);
        stop.setText("بندول");
        stop.setOnClickListener(v -> stopBar());
        root.addView(stop, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView credit = new TextView(this);
        credit.setText("Ghora Graphics — غوره ګرافیکس");
        credit.setGravity(Gravity.CENTER);
        credit.setPadding(0, dp(24), 0, 0);
        credit.setTextColor(Color.GRAY);
        root.addView(credit);

        ScrollView scroll = new ScrollView(this);
        scroll.addView(root);
        return scroll;
    }

    private TextView label(String s) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(15);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setPadding(0, dp(12), 0, dp(4));
        return t;
    }

    private void startBar() {
        Prefs.saveList(this, listInput.getText().toString());
        Prefs.setIntervalMinutes(this, Prefs.INTERVALS[intervalSpinner.getSelectedItemPosition()]);
        if (!hasNotificationPermission()) {
            askNotificationPermission();
            Toast.makeText(this, "مهرباني وکړئ د خبرتیا (Notification) اجازه ورکړئ", Toast.LENGTH_LONG).show();
            return;
        }
        startForegroundService(new Intent(this, ZikrService.class));
        Prefs.setRunning(this, true);
        Toast.makeText(this, "ذکر بار پیل شو", Toast.LENGTH_SHORT).show();
        refreshStatus();
    }

    private void stopBar() {
        stopService(new Intent(this, ZikrService.class));
        Prefs.setRunning(this, false);
        refreshStatus();
    }

    private void refreshStatus() {
        if (status == null) return;
        boolean on = Prefs.running(this);
        status.setText(on ? "● روان دی" : "○ بند دی");
        status.setTextColor(on ? Color.parseColor("#2E7D32") : Color.GRAY);
    }

    private boolean hasNotificationPermission() {
        return Build.VERSION.SDK_INT < 33
                || checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
    }

    private void askNotificationPermission() {
        if (!hasNotificationPermission()) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
        }
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
