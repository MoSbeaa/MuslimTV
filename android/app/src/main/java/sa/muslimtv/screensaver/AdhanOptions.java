package sa.muslimtv.screensaver;

import android.content.Context;
import android.content.Intent;
import android.content.ComponentName;
import android.content.SharedPreferences;
import org.json.JSONObject;

final class AdhanOptions {
    static final String BRIDGE = "io.github.toolicious.homeonfire.muslimtv";
    static final String ACTION = BRIDGE + ".ADHAN_UPDATE";
    static final String[] IDS = {"makkah", "quds", "madinah", "alafasy", "fajr_makkah", "fajr_madinah", "yt_ADVpixw_lhU", "yt_PQnkQJeYsec", "yt_GN1f4LNoTUs", "yt_KEAjg0zzRmU", "yt_SI4CScs4D2Q"};
    static final String[] LABELS = {"مكة المكرمة — علي أحمد ملا", "القدس — المسجد الأقصى", "المدينة المنورة — الحرم النبوي", "مشاري راشد العفاسي", "أذان الفجر — مكة المكرمة", "أذان الفجر — المدينة المنورة", "أذان سوريا — قناة يحيى معمو", "أذان سوريا — قناة عبد الله عثمان", "ناصر القطامي", "الأذان الدمشقي الجماعي — الجامع الأموي", "عمر هشام العربي"};
    static final String[] KEYS = {"Fajr", "Dhuhr", "Asr", "Maghrib", "Isha"};
    static final String[] NAMES = {"الفجر", "الظهر", "العصر", "المغرب", "العشاء"};
    static SharedPreferences prefs(Context c) { return c.getSharedPreferences("adhan", Context.MODE_PRIVATE); }
    static boolean hasVoice(String id) { for (String known : IDS) if (known.equals(id)) return true; return false; }
    static String label(String id) { for (int i = 0; i < IDS.length; i++) if (IDS[i].equals(id)) return LABELS[i]; return LABELS[0]; }
    static JSONObject json(Context c) {
        SharedPreferences p = prefs(c); JSONObject j = new JSONObject();
        try {
            j.put("voices", new org.json.JSONArray(java.util.Arrays.asList(IDS)));
            j.put("enabled", p.getBoolean("enabled", true));
            j.put("sound", p.getBoolean("sound", true)); j.put("popup", p.getBoolean("popup", true));
            j.put("voice", p.getString("voice", "makkah")); j.put("fajr_voice", p.getString("fajr_voice", "fajr_makkah"));
            j.put("volume", p.getInt("volume", 65)); j.put("duration", p.getInt("duration", 12));
            j.put("asr", p.getInt("asr", 2));
            for (String key : KEYS) j.put(key, p.getBoolean(key, true));
        } catch (org.json.JSONException impossible) { }
        return j;
    }
    static android.content.BroadcastReceiver watchScope(Context context, String scope) {
        android.content.BroadcastReceiver receiver = new android.content.BroadcastReceiver() {
            @Override public void onReceive(Context c, Intent i) { scope(context, scope, true); }
        };
        context.registerReceiver(receiver, new android.content.IntentFilter("sa.muslimtv.ADHAN_QUERY_SCOPE"), "sa.muslimtv.screensaver.READ_CONFIG", null);
        return receiver;
    }
    static void changed(Context c) {
        c.sendBroadcast(new Intent(ACTION).setComponent(new ComponentName(BRIDGE, BRIDGE + ".AdhanReceiver")));
    }
    static void scope(Context c, String scope, boolean visible) {
        c.sendBroadcast(new Intent(ACTION).setComponent(new ComponentName(BRIDGE, BRIDGE + ".AdhanReceiver"))
            .putExtra("scope", scope).putExtra("visible", visible));
    }
}
