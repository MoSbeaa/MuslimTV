package io.github.toolicious.homeonfire.muslimtv;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Signature-protected owner settings/scope updates and the app's own alarm. */
public final class AdhanReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c, Intent intent) { HijackService.updateAdhan(intent); }
}
