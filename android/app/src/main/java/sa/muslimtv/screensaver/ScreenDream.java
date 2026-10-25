package sa.muslimtv.screensaver;

import android.service.dreams.DreamService;
import android.webkit.WebView;

/** The system screensaver: starts on the TV's own idle timer, any key wakes it. */
public class ScreenDream extends DreamService {
    private WebView web;
    private android.content.BroadcastReceiver adhanQuery;

    @Override
    public void onAttachedToWindow() {
        super.onAttachedToWindow();
        setInteractive(false);
        setFullscreen(true);
        setScreenBright(true);
        web = ScreenView.create(this);
        setContentView(web);
    }

    @Override public void onDreamingStarted() { super.onDreamingStarted(); if (adhanQuery == null) adhanQuery = AdhanOptions.watchScope(this, "dream"); AdhanOptions.scope(this, "dream", true); }
    @Override public void onDreamingStopped() { AdhanOptions.scope(this, "dream", false); if (adhanQuery != null) { unregisterReceiver(adhanQuery); adhanQuery = null; } super.onDreamingStopped(); }

    @Override
    public void onDetachedFromWindow() {
        AdhanOptions.scope(this, "dream", false);
        if (adhanQuery != null) { unregisterReceiver(adhanQuery); adhanQuery = null; }
        if (web != null) { web.destroy(); web = null; }
        super.onDetachedFromWindow();
    }
}
