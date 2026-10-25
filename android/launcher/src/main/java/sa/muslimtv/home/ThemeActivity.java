package sa.muslimtv.home;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.view.View;
import android.widget.*;

/** Saved launcher palette, editable with the TV remote. */
public final class ThemeActivity extends Activity {
    private LinearLayout body;
    private EditText surface, accent;
    private TextView preview;
    private int dp(int n) { return Math.round(n * getResources().getDisplayMetrics().density); }
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(0xFF101615);
        body = new LinearLayout(this); body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(100), dp(24), dp(100), dp(24)); scroll.addView(body);
        label("ألوان الواجهة", 27);
        label("اختر مظهرًا جاهزًا، أو أدخل لون البطاقات ولون الزخارف بصيغة #RRGGBB. تتغير ألوان المشغل وبطاقة الصلاة؛ تبقى الصور كما هي.", 16);
        preset("أخضر وذهبي — الافتراضي", 0xFF102D21, 0xFFD6AF6B);
        preset("أزرق وفضي", 0xFF142D49, 0xFFCEDDEC);
        preset("عنابي وذهبي", 0xFF421D2B, 0xFFE6C17A);
        preset("فحمي وأبيض", 0xFF22252B, 0xFFF2F2F2);
        preset("زمردي وعاجي", 0xFF073E35, 0xFFF4E8CB);
        preset("كحلي وذهبي", 0xFF101E36, 0xFFE5BB6C);
        preset("تركوازي ولؤلؤي", 0xFF12434B, 0xFFD8F3EE);
        preset("بنفسجي وفضي", 0xFF35244D, 0xFFE0D9EF);
        preset("بني ونحاسي", 0xFF382820, 0xFFE2AB7D);
        preset("زيتوني ورملي", 0xFF353E23, 0xFFE7D49C);
        preset("أسود وذهبي", 0xFF101010, 0xFFE4C577);
        preset("ليلي وأزرق سماوي", 0xFF122637, 0xFF99D9F2);

        label("لون البطاقات", 17); surface = field(ThemeColors.hex(ThemeColors.surface(this)));
        label("لون الزخارف والعناوين", 17); accent = field(ThemeColors.hex(ThemeColors.accent(this)));
        preview = label("معاينة الألوان — مواقيت الصلاة", 20); preview.setPadding(dp(16), dp(12), dp(16), dp(12));
        Button show = button("معاينة الألوان"); show.setOnClickListener(v -> preview());
        Button save = button("حفظ الألوان"); save.setOnClickListener(v -> {
            try { apply(ThemeColors.parse(surface.getText().toString()), ThemeColors.parse(accent.getText().toString())); }
            catch (IllegalArgumentException e) { invalid(); }
        });
        java.util.ArrayList<View> order = new java.util.ArrayList<>();
        for (int n = 0; n < body.getChildCount(); n++) {
            View child = body.getChildAt(n);
            if (child instanceof Button || child instanceof EditText) order.add(child);
        }
        for (int n = 0; n < order.size(); n++) {
            final int at = n;
            order.get(n).setOnKeyListener((v, key, event) -> {
                if (key != android.view.KeyEvent.KEYCODE_DPAD_UP && key != android.view.KeyEvent.KEYCODE_DPAD_DOWN) return false;
                if (event.getAction() == android.view.KeyEvent.ACTION_DOWN) {
                    int next = Math.max(0, Math.min(order.size() - 1, at + (key == android.view.KeyEvent.KEYCODE_DPAD_DOWN ? 1 : -1)));
                    order.get(next).requestFocus();
                }
                return true;
            });
        }
        setContentView(scroll); order.get(0).requestFocus(); preview();
    }
    private TextView label(String value, int size) {
        TextView t = new TextView(this); t.setText(value); t.setTextColor(Color.WHITE); t.setTextSize(size);
        t.setPadding(0, dp(6), 0, dp(6)); body.addView(t); return t;
    }
    private Button button(String title) {
        Button b = new Button(this); b.setText(title); body.addView(b, new LinearLayout.LayoutParams(-1, -2)); return b;
    }
    private void preset(String name, int bg, int gold) {
        Button b = button(name); b.setOnClickListener(v -> apply(bg, gold));
    }
    private EditText field(String value) {
        EditText e = new EditText(this); e.setSingleLine(true); e.setText(value); e.setTextColor(Color.WHITE);
        e.setTextDirection(View.TEXT_DIRECTION_LTR); e.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        e.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(7)});
        body.addView(e, new LinearLayout.LayoutParams(-1, -2)); return e;
    }
    private void preview() {
        try { preview.setBackgroundColor(ThemeColors.parse(surface.getText().toString())); preview.setTextColor(ThemeColors.parse(accent.getText().toString())); }
        catch (IllegalArgumentException e) { invalid(); }
    }
    private void invalid() { Toast.makeText(this, "أدخل ست خانات للون، مثل #102D21", Toast.LENGTH_LONG).show(); }
    private void apply(int bg, int gold) {
        Dock.prefs(this).edit().putInt("theme_surface", bg).putInt("theme_accent", gold).apply();
        Toast.makeText(this, "تم حفظ ألوان الواجهة", Toast.LENGTH_SHORT).show(); finish();
    }
}
