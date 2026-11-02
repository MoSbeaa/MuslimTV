package sa.muslimtv.screensaver;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.LinkAddress;
import android.net.LinkProperties;
import android.net.Network;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Collections;

/** QR pairing screen; closing it shuts down the LAN listener and invalidates the session. */
public final class PhoneControlActivity extends Activity {
    private PhoneControlServer server;
    @Override public void onCreate(Bundle state){
        super.onCreate(state);getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setGravity(Gravity.CENTER);
        root.setPadding(36,24,36,24);root.setBackgroundColor(0xFF22252B);setContentView(root);
        label(root,"التحكم من الهاتف",30,0xFFF2F2F2);
        label(root,"صل هاتفك بشبكة المنزل نفسها، ثم امسح الرمز بالكاميرا.",19,Color.WHITE);
        try{
            String host=localAddress();
            if(host==null)throw new IllegalStateException("لا يوجد عنوان محلي للجهاز");
            server=new PhoneControlServer(this,host,new PhoneControlServer.Listener(){
                @Override public void onSaved(){runOnUiThread(()->android.widget.Toast.makeText(PhoneControlActivity.this,"حُفظت الإعدادات من الهاتف",android.widget.Toast.LENGTH_SHORT).show());}
                @Override public void onPairRequest(String address,PhoneControlServer.Confirmation confirmation){
                    runOnUiThread(()->new android.app.AlertDialog.Builder(PhoneControlActivity.this)
                        .setTitle("السماح بالتحكم من الهاتف؟")
                        .setMessage("هاتف على شبكتك ("+address+") أدخل رمز الاقتران. اسمح له بتعديل إعدادات MuslimTV؟")
                        .setPositiveButton("سماح",(d,w)->confirmation.answer(true))
                        .setNegativeButton("رفض",(d,w)->confirmation.answer(false))
                        .setOnCancelListener(d->confirmation.answer(false)).show());
                }
            });
            ImageView qr=new ImageView(this);qr.setImageBitmap(qr(server.url()));
            LinearLayout.LayoutParams image=new LinearLayout.LayoutParams(300,300);image.topMargin=24;root.addView(qr,image);
            label(root,server.url(),17,0xFFF2F2F2);
            label(root,"رمز الاقتران: "+server.pin(),28,0xFFF2F2F2);
            label(root,"اترك هذه الشاشة مفتوحة أثناء التعديل. عند الخروج ينقطع الاتصال فورًا.",16,Color.WHITE);
        }catch(Exception error){label(root,"تعذر فتح التحكم. تأكد من اتصال التلفاز بشبكة المنزل.",20,Color.WHITE);}
    }
    private void label(LinearLayout parent,String value,int size,int color){
        TextView text=new TextView(this);text.setText(value);text.setTextSize(size);text.setTextColor(color);
        text.setGravity(Gravity.CENTER);text.setPadding(8,8,8,8);parent.addView(text);
    }
    private static Bitmap qr(String value)throws Exception{
        BitMatrix bits=new QRCodeWriter().encode(value,BarcodeFormat.QR_CODE,300,300);
        Bitmap bitmap=Bitmap.createBitmap(300,300,Bitmap.Config.RGB_565);
        for(int y=0;y<300;y++)for(int x=0;x<300;x++)bitmap.setPixel(x,y,bits.get(x,y)?Color.BLACK:Color.WHITE);
        return bitmap;
    }
    private String localAddress()throws Exception{
        ConnectivityManager cm=(ConnectivityManager)getSystemService(CONNECTIVITY_SERVICE);
        if(cm!=null){Network active=cm.getActiveNetwork();LinkProperties links=active==null?null:cm.getLinkProperties(active);
            if(links!=null)for(LinkAddress address:links.getLinkAddresses()){
                InetAddress ip=address.getAddress();if(ip instanceof Inet4Address && ip.isSiteLocalAddress())return ip.getHostAddress();
            }}
        for(NetworkInterface nic:Collections.list(NetworkInterface.getNetworkInterfaces()))
            if(nic.isUp() && !nic.isLoopback())for(InetAddress ip:Collections.list(nic.getInetAddresses()))
                if(ip instanceof Inet4Address && ip.isSiteLocalAddress())return ip.getHostAddress();
        return null;
    }
    @Override protected void onDestroy(){if(server!=null)server.close();super.onDestroy();}
}
