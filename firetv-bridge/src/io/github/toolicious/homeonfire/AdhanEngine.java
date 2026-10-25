package io.github.toolicious.homeonfire.urtv;

import android.accessibilityservice.AccessibilityService;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/** MIA beginning-of-prayer alarms. Never plays audio outside the two URTV surfaces. */
final class AdhanEngine {
    private static final Uri CONFIG = Uri.parse("content://sa.urtv.screensaver.config/adhan");
    private static final String[] KEYS = {"Fajr","Dhuhr","Asr","Maghrib","Isha"};
    private static final String[] NAMES = {"الفجر","الظهر","العصر","المغرب","العشاء"};
    private static final TimeZone ZONE = TimeZone.getTimeZone("America/Winnipeg");
    private final AccessibilityService service;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final SharedPreferences store;
    private final AudioManager audio;
    private JSONObject config = new JSONObject(), days = new JSONObject();
    private boolean home, dream, preview, stopped, fetching;
    private long configAt, attemptAt, alarmAt, statusAt;
    private MediaPlayer player;
    private View banner;
    private final AudioManager.OnAudioFocusChangeListener focus = change -> { if(change < 0) stopAudio(); };
    private final Runnable tick = new Runnable() { public void run() { if(stopped) return; check(); handler.postDelayed(this,1000); } };
    private final Runnable hide = () -> hideBanner();
    private final Runnable audioLimit = () -> stopAudio();

    AdhanEngine(AccessibilityService s) {
        service=s;store=s.getSharedPreferences("adhan_engine",Context.MODE_PRIVATE);audio=s.getSystemService(AudioManager.class);
        try { days=new JSONObject(store.getString("days","{}")); } catch(Exception ignored) { }
    }
    void start() {
        stopped=false;refreshConfig();
        if(days.length()==0) new Thread(() -> {
            try(InputStream input=service.getContentResolver().openInputStream(Uri.parse(CONFIG+"/timetable.json"))) {
                JSONObject seed=flatten(new JSONTokener(read(input)).nextValue());
                handler.post(() -> { if(!stopped && days.length()==0) { days=seed;store.edit().putString("days",seed.toString()).apply();check(); } });
            }catch(Exception ignored) { }
        },"MiaBundledTimetable").start();
        service.sendBroadcast(new Intent("sa.urtv.ADHAN_QUERY_SCOPE").setPackage("sa.urtv.home"));
        service.sendBroadcast(new Intent("sa.urtv.ADHAN_QUERY_SCOPE").setPackage("sa.urtv.screensaver"));
        handler.post(tick);
    }
    void stop() { stopped=true;handler.removeCallbacksAndMessages(null);stopAudio();hideBanner();cancelAlarm(); }
    void update(Intent intent) {
        if(intent!=null && intent.hasExtra("scope")) {
            boolean visible=intent.getBooleanExtra("visible",false);String scope=intent.getStringExtra("scope");
            if("home".equals(scope)) home=visible;
            if("dream".equals(scope)) dream=visible;
            if("preview".equals(scope)) preview=visible;
        }
        refreshConfig();
        if(intent!=null && intent.getBooleanExtra("preview_popup",false)) showBanner("معاينة: حان وقت أذان الظهر",System.currentTimeMillis());
        if(!inside())handler.postDelayed(() -> { if(!inside())stopAudio(); },300);check();
    }
    void onWindow(String pkg,String cls) {
        if(pkg==null || cls==null) return;
        if("sa.urtv.home".equals(pkg) && "sa.urtv.home.MainActivity".equals(cls)) { home=true;preview=false; }
        else if("sa.urtv.screensaver".equals(pkg) && "sa.urtv.screensaver.MainActivity".equals(cls)) { preview=true;home=false; }
        else if(!cls.startsWith("android.") && !pkg.equals("com.android.systemui")) { home=false;preview=false;dream=false; }
        if(!inside())stopAudio();
    }
    private boolean inside() { return home || dream || preview; }
    private boolean awake() { PowerManager p=service.getSystemService(PowerManager.class);return p!=null && p.isInteractive(); }
    private int dp(int n){return Math.round(n*service.getResources().getDisplayMetrics().density);}
    private void refreshConfig() {
        configAt=System.currentTimeMillis();
        try(Cursor c=service.getContentResolver().query(CONFIG,null,null,null,null)) {
            if(c!=null && c.moveToFirst())config=new JSONObject(c.getString(0));
        }catch(Exception e){Log.w("URTVAdhan","Settings provider not ready");}
        if(!config.optBoolean("enabled",false) || !config.optBoolean("sound",true))stopAudio();
        if(!config.optBoolean("enabled",false) || !config.optBoolean("popup",true))hideBanner();
    }
    private static String dayKey(long millis) { SimpleDateFormat f=new SimpleDateFormat("yyyy-MM-dd",Locale.ROOT);f.setTimeZone(ZONE);return f.format(new Date(millis)); }
    private long time(JSONObject row,String field) {
        try {
            String text=row.getString(field);
            if(!text.matches("[0-2][0-9]:[0-5][0-9](:[0-5][0-9])?"))return -1;
            int h=Integer.parseInt(text.substring(0,2)),m=Integer.parseInt(text.substring(3,5));if(h>23)return -1;
            String date=row.getString("d_date");
            Calendar c=Calendar.getInstance(ZONE);c.clear();c.set(Integer.parseInt(date.substring(0,4)),Integer.parseInt(date.substring(5,7))-1,Integer.parseInt(date.substring(8,10)),h,m,0);return c.getTimeInMillis();
        }catch(Exception e){return -1;}
    }
    private String field(int index) {
        return new String[]{"fajr_begins","zuhr_begins",config.optInt("asr",2)==1?"asr_mithl_1":"asr_mithl_2","maghrib_begins","isha_begins"}[index];
    }
    private void check() {
        if(stopped)return;
        long now=System.currentTimeMillis();
        if(now-configAt>10000 || now<configAt)refreshConfig();
        if(!awake())stopAudio();
        String today=dayKey(now);
        if(!fetching && (days.optJSONObject(today)==null || now-store.getLong("fetched_at",0)>12*3600000L)
                && (now-attemptAt>5*60000L || now<attemptAt))fetchTimetable();
        if(!config.optBoolean("enabled",false)){cancelAlarm();return;}
        JSONObject row=days.optJSONObject(today);
        if(row!=null && awake())for(int i=0;i<KEYS.length;i++) {
            if(!config.optBoolean(KEYS[i],true))continue;
            long due=time(row,field(i));
            if(due<0 || now<due || now-due>45000 || today.equals(store.getString("done_"+KEYS[i],"")))continue;
            // Persist before emitting: periodic checks and exact alarm delivery cannot duplicate it.
            if(!store.edit().putString("done_"+KEYS[i],today).commit())continue;
            if(config.optBoolean("popup",true))showBanner("حان وقت أذان "+NAMES[i],due);
            if(inside() && config.optBoolean("sound",true))play(i==0?config.optString("fajr_voice","fajr_makkah"):config.optString("voice","makkah"));
        }
        long next=(now/3600000L+1)*3600000L;String nextName="";
        for(int offset=0;offset<2;offset++) {
            Calendar c=Calendar.getInstance(ZONE);c.setTimeInMillis(now);c.add(Calendar.DATE,offset);
            JSONObject nextDay=days.optJSONObject(dayKey(c.getTimeInMillis()));if(nextDay==null)continue;
            for(int i=0;i<5;i++)if(config.optBoolean(KEYS[i],true)){
                long at=time(nextDay,field(i));if(at>now && at<next){next=at;nextName=NAMES[i];}
            }
        }
        schedule(next);
        if(now-statusAt>60000 || now<statusAt){
            statusAt=now;SimpleDateFormat format=new SimpleDateFormat("h:mm a",new Locale("ar"));format.setTimeZone(ZONE);
            report(row==null?"لا يوجد جدول صالح لهذا اليوم؛ لن يصدر أذان بتوقيت تخميني":("جدول MiaOnline محفوظ"+(nextName.isEmpty()?"":" · القادم "+nextName+" "+format.format(new Date(next)))));
        }
    }
    private PendingIntent alarm() {
        return PendingIntent.getBroadcast(service,3301,new Intent("io.github.toolicious.homeonfire.urtv.ADHAN_ALARM").setComponent(new ComponentName(service,AdhanReceiver.class)),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
    }
    private void schedule(long at) {
        if(alarmAt==at)return;alarmAt=at;
        AlarmManager m=service.getSystemService(AlarmManager.class);if(m==null)return;
        // Non-wakeup alarm: never turn on a sleeping TV merely to sound the adhan.
        try{m.setExactAndAllowWhileIdle(AlarmManager.RTC,at,alarm());}
        catch(SecurityException unavailable){m.setAndAllowWhileIdle(AlarmManager.RTC,at,alarm());}
    }
    private void cancelAlarm(){AlarmManager m=service.getSystemService(AlarmManager.class);if(m!=null)m.cancel(alarm());alarmAt=0;}
    private void fetchTimetable() {
        fetching=true;attemptAt=System.currentTimeMillis();
        new Thread(() -> {
            HttpURLConnection connection=null;
            try {
                connection=(HttpURLConnection)new URL("https://miaonline.org/wp-json/dpt/v1/prayertime?filter=year").openConnection();
                connection.setConnectTimeout(10000);connection.setReadTimeout(15000);
                JSONObject fresh;try(InputStream in=connection.getInputStream()){fresh=flatten(new JSONTokener(read(in)).nextValue());}
                if(fresh.optJSONObject(dayKey(System.currentTimeMillis()))==null)throw new java.io.IOException("No current date");
                store.edit().putString("days",fresh.toString()).putLong("fetched_at",System.currentTimeMillis()).apply();
                handler.post(() -> {if(!stopped){days=fresh;statusAt=0;check();}});
            } catch(Exception failed){Log.w("URTVAdhan","Using cached MIA timetable");}
            finally{if(connection!=null)connection.disconnect();handler.post(() -> fetching=false);}
        },"MiaPrayerTimetable").start();
    }
    private static String read(InputStream input)throws java.io.IOException {
        if(input==null)throw new java.io.IOException();ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] buffer=new byte[16384];int n;
        while((n=input.read(buffer))!=-1){out.write(buffer,0,n);if(out.size()>2*1024*1024)throw new java.io.IOException("Too large");}return out.toString("UTF-8");
    }
    private static JSONObject flatten(Object value)throws org.json.JSONException {
        JSONObject result=new JSONObject();collect(value,result);return result;
    }
    private static void collect(Object value,JSONObject result)throws org.json.JSONException {
        if(value instanceof JSONArray){JSONArray a=(JSONArray)value;for(int i=0;i<a.length();i++)collect(a.get(i),result);}
        else if(value instanceof JSONObject){JSONObject o=(JSONObject)value;String day=o.optString("d_date","");if(day.matches("20[0-9]{2}-[0-9]{2}-[0-9]{2}") && o.has("fajr_begins") && o.has("isha_begins"))result.put(day,o);}
    }
    private void report(String message) {
        try {Bundle extra=new Bundle();extra.putString("status",message);service.getContentResolver().call(CONFIG,"ADHAN_STATUS",null,extra);}catch(Exception ignored){}
    }
    private void play(String id) {
        if(!inside() || !awake() || !id.matches("makkah|quds|madinah|alafasy|fajr_makkah|fajr_madinah"))return;
        stopAudio();
        if(audio==null || audio.requestAudioFocus(focus,AudioManager.STREAM_MUSIC,AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)!=AudioManager.AUDIOFOCUS_REQUEST_GRANTED)return;
        try {
            MediaPlayer m=new MediaPlayer();player=m;
            m.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build());
            m.setWakeMode(service,PowerManager.PARTIAL_WAKE_LOCK);
            try(AssetFileDescriptor fd=service.getContentResolver().openAssetFileDescriptor(Uri.parse(CONFIG+"/"+id+".mp3"),"r")){
                if(fd==null)throw new java.io.IOException();m.setDataSource(fd.getFileDescriptor(),fd.getStartOffset(),fd.getLength());
            }
            float level=Math.max(0,Math.min(100,config.optInt("volume",65)))/100f;m.setVolume(level,level);
            m.setOnPreparedListener(p->{if(player==p && inside() && awake()){p.start();handler.postDelayed(audioLimit,10*60000L);}else stopAudio();});
            m.setOnCompletionListener(p->stopAudio());m.setOnErrorListener((p,w,e)->{stopAudio();report("تعذر تشغيل تسجيل الأذان");return true;});m.prepareAsync();
        }catch(Exception e){stopAudio();report("تعذر فتح تسجيل الأذان");}
    }
    private void stopAudio(){handler.removeCallbacks(audioLimit);if(player!=null){MediaPlayer old=player;player=null;old.release();}if(audio!=null)audio.abandonAudioFocus(focus);}
    private void showBanner(String title,long when) {
        if(!awake())return;hideBanner();
        LinearLayout box=new LinearLayout(service);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(20),dp(15),dp(20),dp(15));box.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        GradientDrawable bg=new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,new int[]{0xF5103425,0xF5071B13});bg.setCornerRadius(dp(14));bg.setStroke(dp(1),0xFFD6AF6B);box.setBackground(bg);
        TextView heading=new TextView(service);heading.setText(title);heading.setTextColor(0xFFFFE2A4);heading.setTextSize(21);heading.setGravity(Gravity.RIGHT);
        if(android.os.Build.VERSION.SDK_INT>=26)try{heading.setTypeface(service.getResources().getFont(R.font.rakkas));}catch(Exception ignored){}
        box.addView(heading);
        TextView time=new TextView(service);SimpleDateFormat format=new SimpleDateFormat("h:mm a",new Locale("ar"));format.setTimeZone(ZONE);time.setText(format.format(new Date(when))+" · جدول MiaOnline");time.setTextColor(0xFFE5DECC);time.setTextSize(12);time.setGravity(Gravity.RIGHT);box.addView(time);
        WindowManager.LayoutParams params=new WindowManager.LayoutParams(Math.min(dp(350),service.getResources().getDisplayMetrics().widthPixels-dp(48)),-2,WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE|WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,PixelFormat.TRANSLUCENT);
        params.gravity=Gravity.TOP|Gravity.RIGHT;params.x=dp(28);params.y=dp(32);
        try {service.getSystemService(WindowManager.class).addView(box,params);banner=box;handler.postDelayed(hide,Math.max(5,Math.min(30,config.optInt("duration",12)))*1000L);}catch(Exception e){Log.w("URTVAdhan","Overlay unavailable",e);}
    }
    private void hideBanner(){handler.removeCallbacks(hide);if(banner!=null){try{service.getSystemService(WindowManager.class).removeView(banner);}catch(Exception ignored){}banner=null;}}
}
