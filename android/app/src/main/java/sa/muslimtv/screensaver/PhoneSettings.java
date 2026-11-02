package sa.muslimtv.screensaver;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import org.json.JSONArray;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;

/** The phone editor writes the same preferences as the TV settings screens. */
final class PhoneSettings {
    private static final Uri LAUNCHER = Uri.parse("content://sa.muslimtv.home.phone");
    private static final String[] PRAYERS = {"Fajr","Sunrise","Dhuhr","Asr","Maghrib","Isha"};
    private static final Set<String> ZONES = new HashSet<>(Arrays.asList(TimeZone.getAvailableIDs()));
    private PhoneSettings() {}

    static JSONObject read(Context c) throws Exception {
        Prefs p = new Prefs(c);
        SharedPreferences a = AdhanOptions.prefs(c);
        JSONObject state = new JSONObject(), adhan = new JSONObject(), offsets = new JSONObject();
        String[] city = p.city();
        state.put("city", new JSONObject().put("name",city[0]).put("lat",Double.parseDouble(city[1]))
            .put("lon",Double.parseDouble(city[2]))
            .put("timezone",a.getString("prayer_tz","وينبيغ".equals(city[0])?"Etc/GMT+5":Prefs.TZ)));
        state.put("source",a.getString("prayer_source","mia"));
        state.put("method",a.getInt("prayer_method",2));
        state.put("asr",a.getInt("asr",2));
        for (String key:PRAYERS) offsets.put(key,a.getInt("off_"+key,0));
        state.put("offsets",offsets);
        for (String key:new String[]{"enabled","sound","sound_outside","popup","clock_overlay","Fajr","Dhuhr","Asr","Maghrib","Isha"})
            adhan.put(key,a.getBoolean(key,true));
        adhan.put("voice",a.getString("voice","yt_GN1f4LNoTUs"));
        adhan.put("fajr_voice",a.getString("fajr_voice","yt_ADVpixw_lhU"));
        adhan.put("volume",a.getInt("volume",80));
        adhan.put("fajr_volume",a.getInt("fajr_volume",10));
        adhan.put("duration",a.getInt("duration",30));
        JSONArray voices = new JSONArray();
        for (int i=0;i<AdhanOptions.IDS.length;i++) voices.put(new JSONObject()
            .put("id",AdhanOptions.IDS[i]).put("name",AdhanOptions.LABELS[i]));
        state.put("adhan",adhan).put("voices",voices);
        state.put("panel",p.panel()).put("homePanel",p.homePanel())
            .put("photoMinutes",Integer.parseInt(p.photoMinutes()))
            .put("eventTitle",p.eventTitle()).put("eventDate",p.eventDate());
        try {
            Bundle b = c.getContentResolver().call(LAUNCHER,"GET",null,null);
            if (b != null) state.put("launcher",new JSONObject(b.getString("json","{}")));
        } catch (Exception unavailable) { state.put("launcher",JSONObject.NULL); }
        return state;
    }

    static void save(Context c, JSONObject change) throws Exception {
        Prefs p = new Prefs(c);
        SharedPreferences a = AdhanOptions.prefs(c);
        JSONObject city = change.optJSONObject("city"), offsets = change.optJSONObject("offsets"), adhan = change.optJSONObject("adhan");
        String cityName=null, zone=null; double lat=0,lon=0;
        if(city!=null){
            cityName=city.getString("name").trim(); lat=city.getDouble("lat"); lon=city.getDouble("lon"); zone=city.getString("timezone");
            if("Winnipeg".equalsIgnoreCase(cityName) || "وينيبغ".equals(cityName))cityName="وينبيغ";
            if(cityName.isEmpty() || cityName.length()>80 || Double.isNaN(lat) || Double.isInfinite(lat) || Double.isNaN(lon) || Double.isInfinite(lon)
                || Math.abs(lat)>90 || Math.abs(lon)>180 || !ZONES.contains(zone)) throw new IllegalArgumentException("مدينة غير صالحة");
        }
        String source=change.has("source")?change.getString("source"):a.getString("prayer_source","mia");
        if(!source.equals("mia") && !source.equals("aladhan")) throw new IllegalArgumentException("مصدر غير معروف");
        String effectiveCity=cityName!=null?cityName:p.city()[0];
        if((city!=null || change.has("source")) && source.equals("mia") && !"وينبيغ".equals(effectiveCity))
            throw new IllegalArgumentException("جدول MiaOnline مخصص لوينبيغ؛ اختر الحساب حسب المدينة");
        int method=change.has("method")?change.getInt("method"):a.getInt("prayer_method",2);
        if(method<1 || method>23 || method==6) throw new IllegalArgumentException("طريقة الحساب غير معروفة");
        int asr=change.has("asr")?change.getInt("asr"):a.getInt("asr",2);
        if(asr!=1 && asr!=2) throw new IllegalArgumentException("وقت العصر غير صالح");
        int[] minutes=new int[PRAYERS.length];
        for(int i=0;i<minutes.length;i++){
            minutes[i]=offsets!=null?offsets.getInt(PRAYERS[i]):a.getInt("off_"+PRAYERS[i],0);
            if(minutes[i]<-120 || minutes[i]>120) throw new IllegalArgumentException("تعديل الصلاة يجب أن يكون بين −١٢٠ و+١٢٠ دقيقة");
        }
        String title=change.has("eventTitle")?change.getString("eventTitle").trim():p.eventTitle();
        String date=change.has("eventDate")?change.getString("eventDate").trim():p.eventDate();
        if(title.length()>80 || (!date.isEmpty() && !validDate(date))) throw new IllegalArgumentException("تحقق من عنوان الموعد وتاريخه");
        String panel=change.has("panel")?change.getString("panel"):p.panel();
        String homePanel=change.has("homePanel")?change.getString("homePanel"):p.homePanel();
        if(!validPanel(panel) || !validPanel(homePanel)) throw new IllegalArgumentException("نوع البطاقة غير صالح");
        int photos=change.has("photoMinutes")?change.getInt("photoMinutes"):Integer.parseInt(p.photoMinutes());
        if(!Arrays.asList(5,10,15,30,60).contains(photos)) throw new IllegalArgumentException("فترة الصور غير صالحة");

        if(adhan!=null){
            for(String key:new String[]{"enabled","sound","sound_outside","popup","clock_overlay","Fajr","Dhuhr","Asr","Maghrib","Isha"})
                if(adhan.has(key)) adhan.getBoolean(key);
            for(String key:new String[]{"voice","fajr_voice"}) if(adhan.has(key) && !AdhanOptions.hasVoice(adhan.getString(key)))
                throw new IllegalArgumentException("صوت أذان غير معروف");
            for(String key:new String[]{"volume","fajr_volume"}) if(adhan.has(key) && (adhan.getInt(key)<0 || adhan.getInt(key)>100))
                throw new IllegalArgumentException("مستوى الصوت غير صالح");
            if(adhan.has("duration") && !Arrays.asList(5,8,12,20,30).contains(adhan.getInt("duration")))
                throw new IllegalArgumentException("مدة التنبيه غير صالحة");
        }

        boolean sourceChanged=!source.equals(a.getString("prayer_source","mia")) ||
            (cityName!=null && (!cityName.equals(p.city()[0]) || !String.valueOf(lat).equals(p.city()[1]) || !String.valueOf(lon).equals(p.city()[2])
                || !zone.equals(a.getString("prayer_tz","وينبيغ".equals(p.city()[0])?"Etc/GMT+5":Prefs.TZ)))) ||
            method!=a.getInt("prayer_method",2) || asr!=a.getInt("asr",2);
        if(!sourceChanged && a.getBoolean("needs_prayer_review",false) && adhan!=null && adhan.optBoolean("enabled",false))
            throw new IllegalArgumentException("اعرض مواقيت اليوم وراجعها قبل تفعيل الأذان");

        if(change.has("launcher") && !change.isNull("launcher")){
            Bundle args=new Bundle();args.putString("json",change.getJSONObject("launcher").toString());
            Bundle answer=c.getContentResolver().call(LAUNCHER,"SET",null,args);
            if(answer==null || !answer.getBoolean("ok",false)) throw new IllegalArgumentException(answer==null?"الواجهة الرئيسية غير متاحة":answer.getString("error","تعذر حفظ الواجهة"));
        }
        SharedPreferences.Editor e=a.edit().putString("prayer_source",source).putInt("prayer_method",method).putInt("asr",asr);
        if(zone!=null)e.putString("prayer_tz",zone);
        for(int i=0;i<minutes.length;i++)e.putInt("off_"+PRAYERS[i],minutes[i]);
        if(adhan!=null){
            for(String key:new String[]{"enabled","sound","sound_outside","popup","clock_overlay","Fajr","Dhuhr","Asr","Maghrib","Isha"})
                if(adhan.has(key))e.putBoolean(key,adhan.getBoolean(key));
            for(String key:new String[]{"voice","fajr_voice"})if(adhan.has(key))e.putString(key,adhan.getString(key));
            for(String key:new String[]{"volume","fajr_volume","duration"})if(adhan.has(key))e.putInt(key,adhan.getInt(key));
        }
        if(sourceChanged)e.putBoolean("enabled",false).putBoolean("needs_prayer_review",true);
        e.apply();
        if(cityName!=null)p.setCity(cityName,String.valueOf(lat),String.valueOf(lon));
        p.setEvent(title,date);p.setPanel(panel);p.setHomePanel(homePanel);p.setPhotoMinutes(String.valueOf(photos));
        AdhanOptions.changed(c);
    }

    private static boolean validPanel(String value){for(String[] row:Prefs.PANELS)if(row[0].equals(value))return true;return false;}
    private static boolean validDate(String text){
        if(!text.matches("\\d{4}-\\d{2}-\\d{2}"))return false;
        try{SimpleDateFormat f=new SimpleDateFormat("yyyy-MM-dd",Locale.ROOT);f.setLenient(false);return f.format(f.parse(text)).equals(text);}
        catch(Exception invalid){return false;}
    }
}
