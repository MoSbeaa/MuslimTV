// SPDX-License-Identifier: GPL-3.0-or-later
// Based on Home on Fire by toolicious; local Arabic transition customization.
package io.github.toolicious.homeonfire.muslimtv;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.os.Build;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextDirectionHeuristics;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.View;

/** Static native overlay: Arabic shaping, no network or animation startup cost. */
public class LoaderView extends View {
    private final Paint decor = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint text = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final StaticLayout phrase;
    private OnEndListener listener;
    private boolean looping;
    private final Runnable end = new Runnable() {
        @Override public void run() { if (!looping && listener != null) listener.onEnd(); }
    };
    public interface OnEndListener { void onEnd(); }

    public LoaderView(Context context) { this(context, null); }
    public LoaderView(Context context, AttributeSet attributes) {
        super(context, attributes);
        text.setColor(Color.rgb(227, 198, 126));
        text.setTextSize(100f);
        Typeface face = Typeface.create("serif", Typeface.NORMAL);
        if (Build.VERSION.SDK_INT >= 26) face = getResources().getFont(R.font.rakkas);
        text.setTypeface(face);
        phrase = StaticLayout.Builder.obtain("اللهم صلِّ وسلِّم على سيدنا محمد", 0,
                "اللهم صلِّ وسلِّم على سيدنا محمد".length(), text, 1600)
                .setAlignment(Layout.Alignment.ALIGN_CENTER)
                .setTextDirection(TextDirectionHeuristics.RTL)
                .setIncludePad(true).setLineSpacing(16f, 1f).build();
        setContentDescription("اللهم صلِّ وسلِّم على سيدنا محمد");
    }
    public void setCaptions(String loading, String ready) { }
    public void setBlackScreen(boolean black) { }
    public void setLooping(boolean value) { looping = value; }
    public void setOnEndListener(OnEndListener value) { listener = value; }
    public void start() { removeCallbacks(end); postDelayed(end, 5000); invalidate(); }
    public void stop() { removeCallbacks(end); }
    @Override protected void onAttachedToWindow() { super.onAttachedToWindow(); start(); }
    @Override protected void onDetachedFromWindow() { stop(); super.onDetachedFromWindow(); }

    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawColor(Color.rgb(5, 26, 20));
        float scale = Math.min(getWidth() / 1920f, getHeight() / 1080f);
        canvas.save();
        canvas.translate((getWidth() - 1920f * scale) / 2f, (getHeight() - 1080f * scale) / 2f);
        canvas.scale(scale, scale);
        decor.setShader(new LinearGradient(0, 0, 1920, 1080,
                Color.rgb(7, 48, 35), Color.rgb(3, 20, 16), Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, 1920, 1080, decor);
        decor.setShader(null);
        decor.setColor(Color.rgb(177, 143, 66));
        decor.setStrokeWidth(2f);
        canvas.drawLine(640, 382, 1280, 382, decor);
        canvas.drawLine(640, 698, 1280, 698, decor);
        canvas.save();
        canvas.translate(160, 540 - phrase.getHeight() / 2f);
        phrase.draw(canvas);
        canvas.restore();
        canvas.restore();
    }
}
