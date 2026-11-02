package sa.muslimtv.home;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Signature-only bridge from the TV's temporary phone-control session to launcher settings. */
public final class PhoneConfigProvider extends ContentProvider {
    @Override public boolean onCreate() { return true; }

    @Override public Bundle call(String method, String arg, Bundle extras) {
        if (getContext().checkCallingOrSelfPermission("sa.muslimtv.screensaver.READ_CONFIG")
                != android.content.pm.PackageManager.PERMISSION_GRANTED) throw new SecurityException();
        Bundle result = new Bundle();
        if ("GET".equals(method)) {
            JSONObject state = new JSONObject();
            try {
                state.put("surface", ThemeColors.hex(ThemeColors.surface(getContext())));
                state.put("accent", ThemeColors.hex(ThemeColors.accent(getContext())));
                state.put("themedFavorites", ThemeColors.themedFavorites(getContext()));
                Map<String,String> apps = Dock.launchable(getContext());
                JSONArray available = new JSONArray();
                for (Map.Entry<String,String> e : apps.entrySet())
                    available.put(new JSONObject().put("id", e.getKey()).put("name", e.getValue()));
                state.put("apps", available);
                state.put("dock", new JSONArray(Dock.shown(getContext(), apps)));
                JSONArray links = new JSONArray();
                for (int i=0; i<QuickLinks.COUNT; i++)
                    links.put(new JSONObject().put("title", QuickLinks.title(getContext(),i)).put("url", QuickLinks.url(getContext(),i)));
                state.put("links", links);
            } catch (Exception ignored) { }
            result.putString("json", state.toString());
        } else if ("SET".equals(method) && extras != null) {
            try {
                JSONObject changes = new JSONObject(extras.getString("json", "{}"));
                int surface = changes.has("surface") ? ThemeColors.parse(changes.getString("surface")) : ThemeColors.surface(getContext());
                int accent = changes.has("accent") ? ThemeColors.parse(changes.getString("accent")) : ThemeColors.accent(getContext());
                JSONArray links = changes.optJSONArray("links");
                if (links != null && links.length() != QuickLinks.COUNT) throw new IllegalArgumentException("links");
                List<String> titles = new ArrayList<>(), urls = new ArrayList<>();
                if (links != null) for (int i=0; i<links.length(); i++) {
                    JSONObject link = links.getJSONObject(i);
                    String title = link.getString("title").trim();
                    if (title.length()>80 || title.isEmpty()) throw new IllegalArgumentException("title");
                    titles.add(title); urls.add(QuickLinks.normalize(link.getString("url")));
                }
                List<String> dock = null;
                if (changes.has("dock")) {
                    JSONArray chosen = changes.getJSONArray("dock");
                    if (chosen.length()>Dock.MAX) throw new IllegalArgumentException("dock");
                    Map<String,String> apps = Dock.launchable(getContext());
                    dock = new ArrayList<>();
                    for (int i=0;i<chosen.length();i++) {
                        String id=chosen.getString(i);
                        if (!apps.containsKey(id) || dock.contains(id)) throw new IllegalArgumentException("dock");
                        dock.add(id);
                    }
                }
                SharedPreferences.Editor e = Dock.prefs(getContext()).edit()
                    .putInt("theme_surface", surface).putInt("theme_accent", accent);
                if (changes.has("themedFavorites")) e.putBoolean("themed_favorites", changes.getBoolean("themedFavorites"));
                for (int i=0;i<titles.size();i++) e.putString("quick_title_"+i,titles.get(i)).putString("quick_url_"+i,urls.get(i));
                e.apply();
                if (dock != null) Dock.save(getContext(), dock);
                ThemeColors.syncToSaver(getContext());
                getContext().sendBroadcast(new android.content.Intent(Dock.CHANGED).setPackage(getContext().getPackageName()));
                result.putBoolean("ok", true);
            } catch (Exception invalid) { result.putString("error", "تحقق من الألوان والروابط والتطبيقات المختارة"); }
        }
        return result;
    }
    @Override public Cursor query(Uri uri,String[] p,String s,String[] a,String sort){return null;}
    @Override public String getType(Uri uri){return null;}
    @Override public Uri insert(Uri uri,ContentValues v){return null;}
    @Override public int delete(Uri uri,String s,String[] a){return 0;}
    @Override public int update(Uri uri,ContentValues v,String s,String[] a){return 0;}
}
