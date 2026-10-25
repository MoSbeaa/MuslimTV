package io.github.toolicious.homeonfire.urtv;

import android.content.ComponentName;
import android.content.Context;
import android.media.MediaMetadata;
import android.media.session.MediaController;
import android.media.session.MediaSessionManager;
import android.media.session.PlaybackState;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;

/** Seek once, only after the requested TizenTube video is playing in the foreground. */
final class VideoRestart {
    static final String PACKAGE = "io.gh.reisxd.tizentube.cobalt";
    private static final Handler MAIN = new Handler(Looper.getMainLooper());
    private static int generation;
    static boolean request(Context context, String id) {
        final Context app = context.getApplicationContext();
        final ComponentName listener = new ComponentName(app, PlaybackAccess.class);
        String enabled = Settings.Secure.getString(app.getContentResolver(), "enabled_notification_listeners");
        boolean allowed = false;
        if (enabled != null) for (String entry : enabled.split(":")) {
            if (listener.equals(ComponentName.unflattenFromString(entry))) allowed = true;
        }
        final int token = ++generation; // a newer shortcut cancels the previous request
        if (!allowed) return false;
        final long expires = SystemClock.elapsedRealtime() + 60000;
        MAIN.post(new Runnable() {
            boolean entered;
            @Override public void run() {
                if (token != generation || SystemClock.elapsedRealtime() > expires) return;
                boolean foreground = HijackService.isTizenForeground();
                if (entered && !foreground) return; // never seek a video after the user leaves TizenTube
                entered |= foreground;
                if (foreground) try {
                    MediaSessionManager manager = app.getSystemService(MediaSessionManager.class);
                    if (manager != null) for (MediaController controller : manager.getActiveSessions(listener)) {
                        if (!PACKAGE.equals(controller.getPackageName())) continue;
                        MediaMetadata metadata = controller.getMetadata();
                        PlaybackState state = controller.getPlaybackState();
                        if (metadata == null || state == null || state.getState() != PlaybackState.STATE_PLAYING
                                || (state.getActions() & PlaybackState.ACTION_SEEK_TO) == 0) continue;
                        String mediaId = metadata.getString(MediaMetadata.METADATA_KEY_MEDIA_ID);
                        String title = normalize(metadata.getString(MediaMetadata.METADATA_KEY_TITLE));
                        boolean matches = mediaId != null && mediaId.contains(id);
                        if (!matches) matches = "3ZJvLYqrUrM".equals(id)
                            ? title.contains("كهف") && title.contains("عزام")
                            : title.contains("صباح") && title.contains("نفيس");
                        if (!matches) continue;
                        controller.getTransportControls().seekTo(0);
                        Log.i("HomeOnFire", "Requested video matched; sent one seekTo(0)");
                        return;
                    }
                } catch (SecurityException unavailable) {
                    Log.w("HomeOnFire", "Media session access is not available yet");
                }
                MAIN.postDelayed(this, 500);
            }
        });
        return true;
    }
    private static String normalize(String text) {
        return text == null ? "" : text.replaceAll("[\u064B-\u065F\u0670\u0640]", "")
            .replace('أ', 'ا').replace('إ', 'ا').replace('آ', 'ا');
    }
}
