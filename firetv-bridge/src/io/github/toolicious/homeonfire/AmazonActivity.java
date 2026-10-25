package io.github.toolicious.homeonfire.urtv;

import android.app.Activity;
import android.os.Bundle;
import android.content.Intent;
import android.content.ComponentName;
import android.widget.Toast;

/** Signature protected bridge from URTV to Amazon, with a temporary Home bypass. */
public final class AmazonActivity extends Activity {
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        String screen = getIntent().getStringExtra("screen");
        if (!"home".equals(screen) && !"settings".equals(screen)) { finish(); return; }
        HijackService.requestAmazonSession();
        Intent target = new Intent("settings".equals(screen) ? android.provider.Settings.ACTION_SETTINGS : Intent.ACTION_MAIN).setComponent(new ComponentName("com.amazon.tv.launcher",
            "settings".equals(screen) ? "com.amazon.tv.launcher.ui.SettingsActivity"
                : "com.amazon.tv.launcher.ui.HomeActivity_vNext"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try { startActivity(target); }
        catch (Exception e) {
            HijackService.requestBypass(0);
            Toast.makeText(this, "تعذر فتح واجهة أمازون", Toast.LENGTH_LONG).show();
        }
        finish();
    }
}

