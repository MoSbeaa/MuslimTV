package sa.urtv.home;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/** Remote-friendly editor. Validate every slot before saving any changes. */
public class QuickLinksActivity extends Activity {
    private final EditText[] titles = new EditText[QuickLinks.COUNT];
    private final EditText[] urls = new EditText[QuickLinks.COUNT];
    private int dp(int v) { return Math.round(v * getResources().getDisplayMetrics().density); }

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(0xFF051B13);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(110), dp(26), dp(110), dp(38));
        scroll.addView(content);
        TextView heading = label("روابط يوتيوب السريعة", 27);
        content.addView(heading);
        content.addView(label("اكتب عنوان كل زر ورابط الفيديو. يُفتح مباشرة في TizenTube. امسح الحقلين لإخفاء زر.", 15));
        for (int n = 0; n < QuickLinks.COUNT; n++) {
            content.addView(label("الزر " + new String[]{"الأول", "الثاني", "الثالث", "الرابع", "الخامس"}[n], 17));
            titles[n] = field(QuickLinks.title(this, n), "عنوان الزر", false);
            urls[n] = field(QuickLinks.url(this, n), "رابط فيديو YouTube", true);
            content.addView(titles[n]);
            content.addView(urls[n]);
        }
        Button save = new Button(this);
        save.setText("حفظ الروابط");
        save.setOnClickListener(v -> save());
        content.addView(save);
        View[] focusOrder = new View[QuickLinks.COUNT * 2 + 1];
        for (int n = 0; n < QuickLinks.COUNT; n++) {
            focusOrder[n * 2] = titles[n]; focusOrder[n * 2 + 1] = urls[n];
        }
        focusOrder[focusOrder.length - 1] = save;
        for (int n = 0; n < focusOrder.length; n++) {
            final int at = n;
            focusOrder[n].setId(View.generateViewId());
            focusOrder[n].setOnKeyListener((v, key, event) -> {
                if (key != android.view.KeyEvent.KEYCODE_DPAD_UP && key != android.view.KeyEvent.KEYCODE_DPAD_DOWN) return false;
                if (event.getAction() == android.view.KeyEvent.ACTION_DOWN) {
                    int next = Math.max(0, Math.min(focusOrder.length - 1,
                        at + (key == android.view.KeyEvent.KEYCODE_DPAD_DOWN ? 1 : -1)));
                    focusOrder[next].requestFocus();
                }
                return true;
            });
        }
        setContentView(scroll);
        titles[0].requestFocus();
        getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
    }

    private TextView label(String value, int size) {
        TextView t = new TextView(this);
        t.setText(value); t.setTextSize(size); t.setTextColor(0xFFE0C17C);
        t.setGravity(Gravity.RIGHT); t.setTextDirection(View.TEXT_DIRECTION_RTL);
        t.setPadding(0, dp(12), 0, dp(8));
        return t;
    }
    private EditText field(String value, String hint, boolean url) {
        EditText e = new EditText(this);
        e.setText(value); e.setHint(hint); e.setSingleLine(true); e.setTextSize(17);
        e.setTextColor(Color.WHITE); e.setHintTextColor(0xFFB5BAAF);
        e.setPadding(dp(12), dp(7), dp(12), dp(7));
        e.setMinHeight(dp(48));
        e.setTextDirection(url ? View.TEXT_DIRECTION_LTR : View.TEXT_DIRECTION_RTL);
        e.setGravity((url ? Gravity.LEFT : Gravity.RIGHT) | Gravity.CENTER_VERTICAL);
        e.setInputType(InputType.TYPE_CLASS_TEXT | (url ? InputType.TYPE_TEXT_VARIATION_URI : InputType.TYPE_TEXT_FLAG_CAP_SENTENCES));
        e.setImeOptions(EditorInfo.IME_ACTION_NEXT);
        if (!url) try { e.setTypeface(Typeface.createFromAsset(getAssets(), "fonts/rakkas.ttf")); } catch (RuntimeException ignored) { }
        e.setOnFocusChangeListener((v, focus) -> {
            GradientDrawable bg = new GradientDrawable(); bg.setColor(focus ? 0xFF1D3A2C : 0xFF10271E);
            bg.setCornerRadius(dp(7)); bg.setStroke(dp(focus ? 2 : 1), focus ? 0xFFF2D58B : 0xFF64735D);
            v.setBackground(bg);
        });
        GradientDrawable bg = new GradientDrawable(); bg.setColor(0xFF10271E); bg.setCornerRadius(dp(7));
        e.setBackground(bg);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(-1, -2); p.bottomMargin = dp(7); e.setLayoutParams(p);
        return e;
    }
    private void save() {
        String[] names = new String[QuickLinks.COUNT], links = new String[QuickLinks.COUNT];
        for (int n = 0; n < QuickLinks.COUNT; n++) {
            names[n] = titles[n].getText().toString().trim();
            String raw = urls[n].getText().toString().trim();
            if (names[n].isEmpty() && raw.isEmpty()) { links[n] = ""; continue; }
            if (names[n].isEmpty()) { titles[n].setError("اكتب عنوان الزر"); titles[n].requestFocus(); return; }
            try { links[n] = QuickLinks.normalize(raw); }
            catch (IllegalArgumentException ex) { urls[n].setError("أدخل رابط فيديو YouTube صحيحاً"); urls[n].requestFocus(); return; }
        }
        SharedPreferences.Editor edit = Dock.prefs(this).edit();
        for (int n = 0; n < QuickLinks.COUNT; n++) edit.putString("quick_title_" + n, names[n]).putString("quick_url_" + n, links[n]);
        edit.apply();
        Toast.makeText(this, "تم حفظ الروابط", Toast.LENGTH_SHORT).show();
        finish();
    }
}
