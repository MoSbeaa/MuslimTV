package sa.muslimtv.home;

import android.content.Context;
import android.graphics.Color;
import java.util.Locale;

final class ThemeColors {
    static int surface(Context c) { return Dock.prefs(c).getInt("theme_surface", 0xFF102D21); }
    static int accent(Context c) { return Dock.prefs(c).getInt("theme_accent", 0xFFD6AF6B); }
    static boolean themedFavorites(Context c) { return Dock.prefs(c).getBoolean("themed_favorites", true); }
    static String signature(Context c) { return surface(c) + ":" + accent(c) + ":" + themedFavorites(c); }
    static String hex(int color) { return String.format(Locale.ROOT, "#%06X", color & 0xFFFFFF); }
    static int parse(String value) {
        String text = value.trim();
        if (!text.startsWith("#")) text = "#" + text;
        if (!text.matches("#[0-9a-fA-F]{6}")) throw new IllegalArgumentException();
        return Color.parseColor(text);
    }
    static int ink(int color) {
        return 0.2126 * Color.red(color) + 0.7152 * Color.green(color) + 0.0722 * Color.blue(color) > 155
            ? Color.BLACK : Color.WHITE;
    }
}
