package io.github.toolicious.homeonfire.muslimtv;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Explicit, shell-only owner test. Does not alter prayer times or the daily delivery ledger. */
public final class AdhanTestReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        HijackService.updateAdhan(new Intent().putExtra("preview_adhan", true));
        setResultData("Manual adhan test requested; audio remains limited to the launcher/screensaver and saved settings.");
    }
}
