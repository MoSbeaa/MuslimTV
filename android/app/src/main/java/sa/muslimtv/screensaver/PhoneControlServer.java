package sa.muslimtv.screensaver;

import android.content.Context;
import android.net.Uri;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

/** Local, temporary HTTP session. It exists only while the pairing screen is open. */
final class PhoneControlServer implements AutoCloseable {
    interface Confirmation { void answer(boolean allowed); }
    interface Listener { void onSaved(); void onPairRequest(String address, Confirmation confirmation); }
    private final Context context;
    private final Listener listener;
    private final ServerSocket server;
    private final Thread worker;
    private final SecureRandom random = new SecureRandom();
    private final String pin;
    private final String host;
    private String session;
    private long sessionUntil, blockedUntil;
    private int failures;
    private volatile boolean closed;

    PhoneControlServer(Context context, String host, Listener listener) throws Exception {
        this.context=context.getApplicationContext();this.host=host;this.listener=listener;
        this.server=new ServerSocket(0,16,InetAddress.getByName("0.0.0.0"));
        this.pin=String.format(java.util.Locale.ROOT,"%06d",random.nextInt(1000000));
        this.worker=new Thread(this::run,"MuslimTVPhoneControl");worker.start();
    }
    String url(){return "http://"+host+":"+server.getLocalPort()+"/";}
    String pin(){return pin;}
    @Override public void close(){closed=true;try{server.close();}catch(Exception ignored){}session=null;}

    private void run(){
        while(!closed)try(Socket socket=server.accept()){
            socket.setSoTimeout(10000);
            if(!socket.getInetAddress().isSiteLocalAddress() && !socket.getInetAddress().isLoopbackAddress())continue;
            handle(socket);
        }catch(Exception ignored){if(closed)return;}
    }
    private void handle(Socket socket) throws Exception {
        InputStream in=socket.getInputStream();
        String line=readLine(in);
        if(line==null)return;
        String[] request=line.split(" ");
        if(request.length!=3 || !request[2].startsWith("HTTP/1.")){respond(socket,400,"application/json","{}",null);return;}
        String method=request[0], path=request[1];
        Map<String,String> headers=new HashMap<>();int total=0;
        while((line=readLine(in))!=null && !line.isEmpty()){
            total+=line.length();if(total>16384){respond(socket,413,"application/json","{}",null);return;}
            int colon=line.indexOf(':');if(colon>0)headers.put(line.substring(0,colon).toLowerCase(java.util.Locale.ROOT),line.substring(colon+1).trim());
        }
        String authority=host+":"+server.getLocalPort();
        if(!authority.equals(headers.get("host"))){respond(socket,403,"application/json","{}",null);return;}
        if("POST".equals(method) && !url().substring(0,url().length()-1).equals(headers.get("origin"))){respond(socket,403,"application/json","{}",null);return;}
        byte[] body=new byte[0];
        if("POST".equals(method)){
            int length=Integer.parseInt(headers.getOrDefault("content-length","-1"));
            if(length<0 || length>32768){respond(socket,413,"application/json","{}",null);return;}
            body=new byte[length];int at=0,n;while(at<length && (n=in.read(body,at,length-at))>0)at+=n;
            if(at!=length)return;
        }
        if("GET".equals(method) && "/".equals(path)){
            try(InputStream asset=context.getAssets().open("phone-control.html")){
                respond(socket,200,"text/html; charset=utf-8",read(asset),null);
            }return;
        }
        if("POST".equals(method) && "/api/pair".equals(path)){
            if(System.currentTimeMillis()<blockedUntil){respond(socket,429,"application/json","{\"error\":\"انتظر قليلًا قبل المحاولة مجددًا\"}",null);return;}
            String entered=new JSONObject(new String(body,StandardCharsets.UTF_8)).optString("pin","");
            if(!pin.equals(entered)){
                if(++failures>=5){blockedUntil=System.currentTimeMillis()+30000;failures=0;}
                respond(socket,403,"application/json","{\"error\":\"رمز الاقتران غير صحيح\"}",null);return;
            }
            java.util.concurrent.CountDownLatch gate=new java.util.concurrent.CountDownLatch(1);
            java.util.concurrent.atomic.AtomicBoolean approved=new java.util.concurrent.atomic.AtomicBoolean(false);
            listener.onPairRequest(socket.getInetAddress().getHostAddress(),allowed->{approved.set(allowed);gate.countDown();});
            if(!gate.await(60,java.util.concurrent.TimeUnit.SECONDS) || !approved.get() || closed){
                respond(socket,403,"application/json","{\"error\":\"لم يؤكد التلفاز الاقتران\"}",null);return;
            }
            byte[] bytes=new byte[32];random.nextBytes(bytes);StringBuilder key=new StringBuilder();
            for(byte b:bytes)key.append(String.format(java.util.Locale.ROOT,"%02x",b&255));
            session=key.toString();sessionUntil=System.currentTimeMillis()+20*60000L;failures=0;
            respond(socket,200,"application/json","{\"ok\":true}","session="+session+"; HttpOnly; SameSite=Strict; Path=/; Max-Age=1200");return;
        }
        if(session==null || System.currentTimeMillis()>sessionUntil || !headers.getOrDefault("cookie","").contains("session="+session)){
            respond(socket,401,"application/json","{\"error\":\"افتح رمز QR واقترن بالتلفاز مجددًا\"}",null);return;
        }
        if("GET".equals(method) && "/api/state".equals(path)){
            respond(socket,200,"application/json",PhoneSettings.read(context).toString(),null);return;
        }
        if("POST".equals(method) && "/api/save".equals(path)){
            try{PhoneSettings.save(context,new JSONObject(new String(body,StandardCharsets.UTF_8)));
                respond(socket,200,"application/json","{\"ok\":true}",null);listener.onSaved();}
            catch(Exception invalid){respond(socket,400,"application/json",new JSONObject().put("error",invalid.getMessage()==null?"تعذر حفظ الإعدادات":invalid.getMessage()).toString(),null);}
            return;
        }
        if("GET".equals(method) && path.startsWith("/api/cities?q=")){
            String q=Uri.parse("http://"+authority+path).getQueryParameter("q");
            if(q==null || q.trim().length()<2 || q.length()>80){respond(socket,400,"application/json","{}",null);return;}
            try{
                URL api=new URL("https://geocoding-api.open-meteo.com/v1/search?count=8&language=ar&name="+URLEncoder.encode(q,"UTF-8"));
                HttpURLConnection c=(HttpURLConnection)api.openConnection();c.setConnectTimeout(8000);c.setReadTimeout(8000);
                try(InputStream stream=c.getInputStream()){respond(socket,200,"application/json",read(stream),null);}finally{c.disconnect();}
            }catch(Exception unavailable){respond(socket,503,"application/json","{\"error\":\"تعذر البحث عن المدن الآن\"}",null);}
            return;
        }
        if("GET".equals(method) && "/api/prayers".equals(path)){
            try{JSONObject preview=PhonePrayerPreview.today(context);
                AdhanOptions.prefs(context).edit().putBoolean("needs_prayer_review",false).apply();
                AdhanOptions.changed(context);
                respond(socket,200,"application/json",preview.toString(),null);}
            catch(Exception unavailable){respond(socket,503,"application/json","{\"error\":\"تعذر جلب مواقيت اليوم؛ تحقق من الاتصال والمصدر\"}",null);}
            return;
        }
        if("GET".equals(method) && path.startsWith("/api/voice?id=")){
            String id=Uri.parse("http://"+authority+path).getQueryParameter("id");
            if(id==null || !AdhanOptions.hasVoice(id)){respond(socket,404,"application/json","{}",null);return;}
            try(android.content.res.AssetFileDescriptor fd=context.getAssets().openFd("adhan/"+id+".mp3");
                InputStream audio=fd.createInputStream()){
                String head="HTTP/1.1 200 OK\r\nContent-Type: audio/mpeg\r\nContent-Length: "+fd.getLength()+
                    "\r\nConnection: close\r\nCache-Control: no-store\r\nX-Content-Type-Options: nosniff\r\n\r\n";
                OutputStream out=socket.getOutputStream();out.write(head.getBytes(StandardCharsets.US_ASCII));
                byte[] bytes=new byte[16384];int count;while((count=audio.read(bytes))!=-1)out.write(bytes,0,count);out.flush();
            }return;
        }
        respond(socket,404,"application/json","{}",null);
    }
    private static String readLine(InputStream in)throws Exception{
        ByteArrayOutputStream out=new ByteArrayOutputStream();int b;
        while((b=in.read())!=-1){if(b=='\n')break;if(b!='\r')out.write(b);if(out.size()>8192)throw new java.io.IOException("line");}
        return b==-1&&out.size()==0?null:out.toString("UTF-8");
    }
    private static String read(InputStream in)throws Exception{
        ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[8192];int n;
        while((n=in.read(b))!=-1){out.write(b,0,n);if(out.size()>2*1024*1024)throw new java.io.IOException("too large");}
        return out.toString("UTF-8");
    }
    private static void respond(Socket socket,int code,String type,String body,String cookie)throws Exception{
        byte[] data=body.getBytes(StandardCharsets.UTF_8);
        String status=code==200?"OK":code==400?"Bad Request":code==401?"Unauthorized":code==403?"Forbidden":code==404?"Not Found":code==429?"Too Many Requests":"Service Unavailable";
        String head="HTTP/1.1 "+code+" "+status+"\r\nContent-Type: "+type+"\r\nContent-Length: "+data.length+
            "\r\nConnection: close\r\nCache-Control: no-store\r\nX-Content-Type-Options: nosniff\r\nReferrer-Policy: no-referrer\r\n"+
            (cookie==null?"":"Set-Cookie: "+cookie+"\r\n")+"\r\n";
        OutputStream out=socket.getOutputStream();out.write(head.getBytes(StandardCharsets.US_ASCII));out.write(data);out.flush();
    }
}
