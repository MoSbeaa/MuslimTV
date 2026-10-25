package sa.muslimtv.home;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;
import java.util.Locale;

/** Five user-editable YouTube videos, always opened in TizenTube. */
final class QuickLinks {
    static final int COUNT = 5;
    static final String TIZENTUBE = "io.gh.reisxd.tizentube.cobalt";
    static final String[] TITLES = {
        "سورة مريم - عثمان المكي", "سورة الكهف - حسين عزام",
        "سورة مريم - حسين عزام", "سورة مريم - إبراهيم إدريس", "أذكار الصباح - أحمد النفيس"
    };
    // Use the actual hyperlink destinations supplied by the user, not their display labels.
    static final String[] URLS = {
        "https://www.youtube.com/watch?v=WmMNROhzoEg",
        "https://www.youtube.com/watch?v=3ZJvLYqrUrM",
        "https://www.youtube.com/watch?v=81AdCO4ur68",
        "https://www.youtube.com/watch?v=gfwn-9HShjQ",
        "https://www.youtube.com/watch?v=fD4uDIoCn3M"
    };

    static String title(Context c, int i) {
        return Dock.prefs(c).getString("quick_title_" + i, TITLES[i]);
    }
    static String url(Context c, int i) {
        return Dock.prefs(c).getString("quick_url_" + i, URLS[i]);
    }

    /** Accept shared and canonical video URLs, and discard tracking parameters. */
    static String normalize(String input) {
        String s = input.trim();
        if (!s.matches("(?i)^https?://.*")) s = "https://" + s;
        Uri u = Uri.parse(s);
        String host = u.getHost();
        if (host == null) throw new IllegalArgumentException();
        host = host.toLowerCase(Locale.ROOT);
        String id = null;
        if (host.equals("youtu.be")) {
            if (u.getPathSegments().size() == 1) id = u.getLastPathSegment();
        } else if (host.equals("youtube.com") || host.equals("www.youtube.com")
                || host.equals("m.youtube.com") || host.equals("music.youtube.com")) {
            if ("/watch".equals(u.getPath())) id = u.getQueryParameter("v");
            else if (u.getPathSegments().size() == 2
                    && ("shorts".equals(u.getPathSegments().get(0))
                        || "live".equals(u.getPathSegments().get(0))
                        || "embed".equals(u.getPathSegments().get(0)))) id = u.getLastPathSegment();
        }
        if (id == null || !id.matches("[A-Za-z0-9_-]{11}")) throw new IllegalArgumentException();
        return "https://www.youtube.com/watch?v=" + id;
    }

    static void open(Context c, int index) {
        try {
            Uri video = Uri.parse(normalize(url(c, index))).buildUpon()
                .appendQueryParameter("t", "1s").build();
            Intent i = new Intent(Intent.ACTION_VIEW, video);
            i.setPackage(TIZENTUBE);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            c.startActivity(i);
        } catch (android.content.ActivityNotFoundException e) {
            Toast.makeText(c, "تعذر فتح الرابط في TizenTube. تأكد من تثبيته وتحديثه.", Toast.LENGTH_LONG).show();
        } catch (IllegalArgumentException e) {
            Toast.makeText(c, "عدّل رابط الفيديو من إعدادات الروابط السريعة.", Toast.LENGTH_LONG).show();
        }
    }
}
