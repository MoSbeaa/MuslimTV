package io.github.toolicious.homeonfire.muslimtv;

import android.app.Activity;
import android.os.Bundle;
import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

/** Signature-protected entry point for the two shortcuts that need a playback reset. */
public final class RestartVideoActivity extends Activity {
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        String id = getIntent().getStringExtra("video_id");
        if (!"3ZJvLYqrUrM".equals(id) && !"fD4uDIoCn3M".equals(id)) { finish(); return; }
        try {
            Intent video = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/watch?v=" + id))
                .setPackage(VideoRestart.PACKAGE).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            startActivity(video);
            if (!VideoRestart.request(this, id)) Toast.makeText(this,
                "يلزم تفعيل الوصول إلى جلسة التشغيل لتشغيل المقطع من البداية", Toast.LENGTH_LONG).show();
        } catch (android.content.ActivityNotFoundException e) {
            Toast.makeText(this, "تعذر فتح TizenTube", Toast.LENGTH_LONG).show();
        }
        finish();
    }
}
