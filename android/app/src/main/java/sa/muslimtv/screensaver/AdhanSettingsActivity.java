package sa.muslimtv.screensaver;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.graphics.Color;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.view.Gravity;
import android.view.View;
import android.widget.*;

/** TV remote settings and local previews; samples stop when this screen loses foreground. */
public final class AdhanSettingsActivity extends Activity {
    private LinearLayout body;
    private SharedPreferences prefs;
    private MediaPlayer preview;
    private AudioManager audio;
    private final AudioManager.OnAudioFocusChangeListener focus = change -> { if (change < 0) stopPreview(); };
    private int dp(int n) { return Math.round(n * getResources().getDisplayMetrics().density); }
    @Override public void onCreate(Bundle state) {
        super.onCreate(state); prefs = AdhanOptions.prefs(this); audio = getSystemService(AudioManager.class);
        render(); AdhanOptions.changed(this);
    }
    private void render() {
        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true); scroll.setBackgroundColor(0xFF071C14);
        body = new LinearLayout(this); body.setOrientation(LinearLayout.VERTICAL); body.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        body.setPadding(dp(80), dp(22), dp(80), dp(30)); scroll.addView(body);
        label("الأذان وتنبيهات الصلاة", 26, 0xFFD6AF6B);
        label("يمكن اختيار تشغيل صوت الأذان داخل MuslimTV وأثناء استخدام التطبيقات الأخرى بشكل مستقل. الوضع الافتراضي خارج الواجهة صامت.", 15, Color.WHITE);
        label("المواقيت: MiaOnline — بداية الصلاة، لا وقت الإقامة. محفوظة محليًا للعمل دون إنترنت.", 14, 0xFFC6BBA4);
        toggle("تفعيل الأذان والتنبيهات", "enabled");
        toggle("صوت الأذان داخل MuslimTV", "sound");
        row("أثناء استخدام التطبيقات: " + (prefs.getBoolean("sound_outside", false) ? "تشغيل الأذان" : "صامت"), this::outsideSound);
        toggle("التنبيه المرئي أعلى اليمين", "popup");
        row("الصلوات المفعّلة", () -> prayers());
        row("صوت الأذان: " + AdhanOptions.label(prefs.getString("voice", "makkah")), () -> voices(false));
        row("صوت الفجر: " + AdhanOptions.label(prefs.getString("fajr_voice", "fajr_makkah")), () -> voices(true));
        row("مستوى صوت الأذان: " + prefs.getInt("volume", 65) + "٪", () -> volume());
        row("مدة التنبيه: " + prefs.getInt("duration", 12) + " ثانية", () -> duration());
        row("وقت العصر: المثل " + (prefs.getInt("asr", 2) == 2 ? "الثاني" : "الأول"), () -> asr());
        row("إيقاف الاستماع للصوت", () -> stopPreview());
        row("معاينة التنبيه الصامت", () -> sendPreview());
        label("حالة الخدمة: " + prefs.getString("status", "بانتظار تحميل الجدول من خدمة MuslimTV Bridge"), 14, 0xFFC6BBA4);
        label("مصادر التسجيلات: مكتبة Kiwifu/adhan-mp3 وروابط YouTube التي أضافها المستخدم. الملفات داخل التطبيق؛ المعاينة والتشغيل لا يحتاجان الإنترنت.", 13, 0xFFC6BBA4);
        setContentView(scroll);
        body.getChildAt(3).requestFocus();
    }
    private void label(String value, int size, int color) {
        TextView t = new TextView(this); t.setText(value); t.setTextSize(size); t.setTextColor(color); t.setGravity(Gravity.RIGHT);
        t.setPadding(0, dp(6), 0, dp(6)); body.addView(t);
    }
    private void row(String title, Runnable action) {
        Button b = new Button(this); b.setText(title); b.setAllCaps(false); b.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        body.addView(b, new LinearLayout.LayoutParams(-1, -2)); b.setOnClickListener(v -> action.run());
    }
    private void toggle(String name, String key) {
        row(name + (prefs.getBoolean(key, true) ? " — يعمل" : " — متوقف"), () -> {
            prefs.edit().putBoolean(key, !prefs.getBoolean(key, true)).apply(); AdhanOptions.changed(this); render();
        });
    }
    private void outsideSound() {
        new AlertDialog.Builder(this).setTitle("الأذان أثناء استخدام التطبيقات")
            .setSingleChoiceItems(new String[]{"صامت — تنبيه مرئي فقط", "تشغيل صوت الأذان"}, prefs.getBoolean("sound_outside", false) ? 1 : 0, (dialog, which) -> {
                prefs.edit().putBoolean("sound_outside", which == 1).apply();
                AdhanOptions.changed(this); dialog.dismiss(); render();
            }).setNegativeButton("إلغاء", null).show();
    }
    private void prayers() {
        boolean[] chosen = new boolean[5]; for (int i = 0; i < 5; i++) chosen[i] = prefs.getBoolean(AdhanOptions.KEYS[i], true);
        new AlertDialog.Builder(this).setTitle("اختيار الصلوات")
            .setMultiChoiceItems(AdhanOptions.NAMES, chosen, (d, i, value) -> chosen[i] = value)
            .setPositiveButton("حفظ", (d, w) -> { SharedPreferences.Editor e = prefs.edit(); for(int i=0;i<5;i++) e.putBoolean(AdhanOptions.KEYS[i], chosen[i]); e.apply(); AdhanOptions.changed(this); render(); })
            .setNegativeButton("إلغاء", null).show();
    }
    private void voices(boolean fajr) {
        new AlertDialog.Builder(this).setTitle(fajr ? "صوت الفجر — استمع ثم اختر" : "صوت الأذان — استمع ثم اختر")
            .setItems(AdhanOptions.LABELS, (d, i) -> voiceDetails(i, fajr)).show();
    }
    private void voiceDetails(int index, boolean fajr) {
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle(AdhanOptions.LABELS[index])
            .setMessage("استمع إلى التسجيل قبل اختياره. تتوقف المعاينة عند إغلاق النافذة أو الخروج من الإعدادات.")
            .setPositiveButton("اختيار", (d, w) -> { prefs.edit().putString(fajr ? "fajr_voice" : "voice", AdhanOptions.IDS[index]).apply(); AdhanOptions.changed(this); render(); })
            .setNeutralButton("استماع / إيقاف", null).setNegativeButton("رجوع", null).create();
        dialog.setOnDismissListener(d -> stopPreview()); dialog.show();
        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v -> { if (preview != null) stopPreview(); else listen(AdhanOptions.IDS[index]); });
    }
    private void listen(String id) {
        stopPreview();
        if (audio != null && audio.requestAudioFocus(focus, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT) != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            Toast.makeText(this, "تعذر تشغيل المعاينة الآن", Toast.LENGTH_SHORT).show(); return;
        }
        try {
            MediaPlayer m = new MediaPlayer(); preview = m;
            m.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build());
            try (AssetFileDescriptor fd = getAssets().openFd("adhan/" + id + ".mp3")) { m.setDataSource(fd.getFileDescriptor(), fd.getStartOffset(), fd.getLength()); }
            float level = prefs.getInt("volume", 65) / 100f; m.setVolume(level, level);
            m.setOnPreparedListener(player -> { if (preview == player) player.start(); });
            m.setOnCompletionListener(player -> stopPreview());
            m.setOnErrorListener((player, what, extra) -> { stopPreview(); Toast.makeText(this, "تعذر تشغيل التسجيل", Toast.LENGTH_SHORT).show(); return true; });
            m.prepareAsync();
        } catch (Exception error) { stopPreview(); Toast.makeText(this, "تعذر فتح التسجيل", Toast.LENGTH_SHORT).show(); }
    }
    private void stopPreview() {
        if (preview != null) { MediaPlayer old = preview; preview = null; old.release(); }
        if (audio != null) audio.abandonAudioFocus(focus);
    }
    private void volume() {
        LinearLayout box = new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setPadding(dp(20),dp(12),dp(20),dp(12));
        TextView value = new TextView(this); value.setTextColor(Color.WHITE);
        SeekBar bar = new SeekBar(this); bar.setMax(100); bar.setProgress(prefs.getInt("volume",65)); value.setText(bar.getProgress()+"٪");
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar b,int p,boolean user){value.setText(p+"٪");}
            public void onStartTrackingTouch(SeekBar b){} public void onStopTrackingTouch(SeekBar b){}
        }); box.addView(value); box.addView(bar);
        AlertDialog d = new AlertDialog.Builder(this).setTitle("مستوى صوت الأذان") .setView(box)
            .setPositiveButton("حفظ",(v,w)->{prefs.edit().putInt("volume",bar.getProgress()).apply();AdhanOptions.changed(this);render();}).setNegativeButton("إلغاء",null).create();
        d.show(); bar.requestFocus();
    }
    private void duration() {
        int[] seconds = {5,8,12,20,30}; String[] names = {"٥ ثوانٍ","٨ ثوانٍ","١٢ ثانية","٢٠ ثانية","٣٠ ثانية"};
        new AlertDialog.Builder(this).setTitle("مدة التنبيه المرئي").setItems(names,(d,i)->{prefs.edit().putInt("duration",seconds[i]).apply();AdhanOptions.changed(this);render();}).show();
    }
    private void asr() {
        new AlertDialog.Builder(this).setTitle("وقت العصر في جدول MiaOnline")
            .setItems(new String[]{"المثل الأول","المثل الثاني"},(d,i)->{prefs.edit().putInt("asr",i+1).apply();AdhanOptions.changed(this);render();}).show();
    }
    private void sendPreview() {
        sendBroadcast(new android.content.Intent(AdhanOptions.ACTION)
            .setComponent(new android.content.ComponentName(AdhanOptions.BRIDGE,AdhanOptions.BRIDGE+".AdhanReceiver")).putExtra("preview_popup",true));
    }
    @Override public void onPause() { stopPreview(); super.onPause(); }
    @Override public void onDestroy() { stopPreview(); super.onDestroy(); }
}
