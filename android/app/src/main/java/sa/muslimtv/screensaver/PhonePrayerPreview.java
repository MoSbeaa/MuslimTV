package sa.muslimtv.screensaver;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/** Preview uses the same dated sources and per-prayer offsets as the TV and Fire TV scheduler. */
final class PhonePrayerPreview {
    private static final String[] KEYS={"Fajr","Sunrise","Dhuhr","Asr","Maghrib","Isha"};
    private PhonePrayerPreview(){}
    static JSONObject today(Context c)throws Exception{
        SharedPreferences a=AdhanOptions.prefs(c);String source=a.getString("prayer_source","mia");
        String[] city=new Prefs(c).city();String tz=a.getString("prayer_tz","وينبيغ".equals(city[0])?"Etc/GMT+5":Prefs.TZ);
        TimeZone zone=TimeZone.getTimeZone(tz);
        SimpleDateFormat iso=new SimpleDateFormat("yyyy-MM-dd",Locale.ROOT);iso.setTimeZone(zone);
        String date=iso.format(new Date());JSONObject raw;
        if("aladhan".equals(source)){
            SimpleDateFormat apiDate=new SimpleDateFormat("dd-MM-yyyy",Locale.ROOT);apiDate.setTimeZone(zone);
            String query="latitude="+URLEncoder.encode(city[1],"UTF-8")+"&longitude="+URLEncoder.encode(city[2],"UTF-8")
                +"&method="+a.getInt("prayer_method",2)+"&school="+(a.getInt("asr",2)==2?1:0)
                +"&timezonestring="+URLEncoder.encode(tz,"UTF-8");
            raw=new JSONObject(fetch("https://api.aladhan.com/v1/timings/"+apiDate.format(new Date())+"?"+query))
                .getJSONObject("data").getJSONObject("timings");
        }else{
            Object input;
            try(InputStream in=c.getAssets().open("adhan/timetable.json")){input=new JSONTokener(read(in)).nextValue();}
            raw=find(input,date);
            if(raw==null)raw=find(new JSONTokener(fetch("https://miaonline.org/wp-json/dpt/v1/prayertime?filter=year")).nextValue(),date);
            if(raw==null)throw new IllegalStateException("لا يوجد جدول لهذا اليوم");
        }
        JSONObject times=new JSONObject();int asr=a.getInt("asr",2);
        String[] fields="aladhan".equals(source)?KEYS:new String[]{"fajr_begins","sunrise","zuhr_begins",asr==2?"asr_mithl_2":"asr_mithl_1","maghrib_begins","isha_begins"};
        for(int i=0;i<KEYS.length;i++){
            String value=raw.optString(fields[i],"");
            if(!value.matches("[0-2][0-9]:[0-5][0-9].*"))throw new IllegalStateException("وقت غير صالح: "+KEYS[i]);
            int minutes=Integer.parseInt(value.substring(0,2))*60+Integer.parseInt(value.substring(3,5))+a.getInt("off_"+KEYS[i],0);
            minutes=(minutes%1440+1440)%1440;
            times.put(KEYS[i],String.format(Locale.ROOT,"%02d:%02d",minutes/60,minutes%60));
        }
        return new JSONObject().put("date",date).put("source",source).put("times",times);
    }
    private static JSONObject find(Object value,String date)throws Exception{
        if(value instanceof JSONObject){JSONObject o=(JSONObject)value;
            if(date.equals(o.optString("d_date")))return o;
            java.util.Iterator<String> keys=o.keys();while(keys.hasNext()){JSONObject hit=find(o.get(keys.next()),date);if(hit!=null)return hit;}
        }else if(value instanceof JSONArray){JSONArray a=(JSONArray)value;for(int i=0;i<a.length();i++){JSONObject hit=find(a.get(i),date);if(hit!=null)return hit;}}
        return null;
    }
    private static String fetch(String address)throws Exception{
        HttpURLConnection con=(HttpURLConnection)new URL(address).openConnection();
        con.setConnectTimeout(10000);con.setReadTimeout(12000);
        try(InputStream in=con.getInputStream()){return read(in);}finally{con.disconnect();}
    }
    private static String read(InputStream in)throws Exception{
        ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[8192];int n;
        while((n=in.read(b))!=-1){out.write(b,0,n);if(out.size()>2*1024*1024)throw new java.io.IOException("large");}
        return out.toString("UTF-8");
    }
}
