package sa.muslimtv.home;

import android.graphics.*;
import android.graphics.drawable.Drawable;
import java.util.Locale;

/** Lightweight TV card art drawn in the selected palette; no downloaded artwork or fixed brand colours. */
final class FavoriteArt extends Drawable {
    private final String pkg;
    private final int surface, accent;
    private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    FavoriteArt(String pkg, int surface, int accent) { this.pkg = pkg.toLowerCase(Locale.ROOT); this.surface = surface; this.accent = accent; }
    @Override public void draw(Canvas canvas) {
        Rect b = getBounds(); int save = canvas.save();
        canvas.translate(b.left, b.top); canvas.scale(b.width()/320f, b.height()/180f);
        paint.setStyle(Paint.Style.FILL);
        paint.setShader(new LinearGradient(0,0,320,180,new int[]{mix(surface,Color.WHITE,.08f),surface,mix(surface,Color.BLACK,.25f)},null,Shader.TileMode.CLAMP));
        canvas.drawRoundRect(0,0,320,180,18,18,paint); paint.setShader(null);
        paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(1); paint.setColor(alpha(accent,85));
        canvas.drawRoundRect(8,8,312,172,13,13,paint);
        paint.setColor(alpha(accent,45));
        for(int x : new int[]{27,293}) { Path star=new Path(); for(int n=0;n<16;n++){double a=n*Math.PI/8;float radius=n%2==0?13:6;float px=x+(float)Math.cos(a)*radius, py=90+(float)Math.sin(a)*radius;if(n==0)star.moveTo(px,py);else star.lineTo(px,py);}star.close();canvas.drawPath(star,paint); }
        paint.setStrokeWidth(2); paint.setColor(alpha(accent,65));
        Path arch=new Path();arch.moveTo(106,145);arch.lineTo(106,89);arch.cubicTo(106,61,142,49,160,29);arch.cubicTo(178,49,214,61,214,89);arch.lineTo(214,145);canvas.drawPath(arch,paint);
        paint.setColor(accent);paint.setStrokeWidth(4);paint.setStrokeCap(Paint.Cap.ROUND);paint.setStrokeJoin(Paint.Join.ROUND);
        if(pkg.contains("mushaftv") || pkg.contains("quran")) book(canvas);
        else if(pkg.contains("tizentube") || pkg.contains("youtube")) play(canvas);
        else if(pkg.contains("firebat") || pkg.contains("browser")) globe(canvas);
        else if(pkg.contains("drive") || pkg.contains("file")) folder(canvas);
        else if(pkg.contains("launcher")) grid(canvas);
        else if(pkg.contains("netflix") || pkg.contains("nuvio") || pkg.contains("video") || pkg.contains("disney") || pkg.contains("shahid")) screen(canvas);
        else grid(canvas);
        paint.setStrokeWidth(1);paint.setColor(alpha(accent,95));canvas.drawLine(131,156,189,156,paint);
        canvas.restoreToCount(save);
    }
    private void book(Canvas c){Path p=new Path();p.moveTo(160,130);p.cubicTo(147,120,132,118,122,120);p.lineTo(122,76);p.cubicTo(136,74,147,78,160,87);p.cubicTo(173,78,184,74,198,76);p.lineTo(198,120);p.cubicTo(185,118,174,120,160,130);p.close();c.drawPath(p,paint);c.drawLine(160,87,160,130,paint);}
    private void play(Canvas c){c.drawCircle(160,103,31,paint);Path p=new Path();p.moveTo(153,89);p.lineTo(176,103);p.lineTo(153,117);p.close();c.drawPath(p,paint);}
    private void mosque(Canvas c){Path p=new Path();p.moveTo(134,126);p.lineTo(134,98);p.cubicTo(134,81,151,80,160,69);p.cubicTo(169,80,186,81,186,98);p.lineTo(186,126);p.close();c.drawPath(p,paint);c.drawLine(122,126,198,126,paint);c.drawLine(122,126,122,85,paint);c.drawLine(198,126,198,85,paint);c.drawLine(116,85,128,85,paint);c.drawLine(192,85,204,85,paint);c.drawRoundRect(153,107,167,126,6,6,paint);}
    private void globe(Canvas c){c.drawCircle(160,103,32,paint);c.drawOval(144,71,176,135,paint);c.drawLine(128,103,192,103,paint);}
    private void folder(Canvas c){Path p=new Path();p.moveTo(124,128);p.lineTo(124,83);p.lineTo(150,83);p.lineTo(159,92);p.lineTo(196,92);p.lineTo(196,128);p.close();c.drawPath(p,paint);}
    private void grid(Canvas c){for(int x:new int[]{133,166})for(int y:new int[]{78,111})c.drawRoundRect(x,y,x+21,y+21,4,4,paint);}
    private void screen(Canvas c){c.drawRoundRect(124,78,196,125,8,8,paint);c.drawLine(149,135,171,135,paint);Path p=new Path();p.moveTo(154,91);p.lineTo(170,101);p.lineTo(154,112);p.close();c.drawPath(p,paint);}
    private static int alpha(int c,int a){return (c&0xFFFFFF)|(a<<24);}
    private static int mix(int a,int b,float t){return Color.rgb((int)(Color.red(a)*(1-t)+Color.red(b)*t),(int)(Color.green(a)*(1-t)+Color.green(b)*t),(int)(Color.blue(a)*(1-t)+Color.blue(b)*t));}
    @Override public void setAlpha(int alpha) { }
    @Override public void setColorFilter(ColorFilter filter) { }
    @Override public int getOpacity(){return PixelFormat.OPAQUE;}
}
