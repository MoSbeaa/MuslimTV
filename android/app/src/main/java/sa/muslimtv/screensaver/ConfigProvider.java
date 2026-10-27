package sa.muslimtv.screensaver;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;

/**
 * Read-only: hands the launcher (same signing key, see READ_CONFIG in the manifest) what its home-screen
 * card needs: the page settings as a URL fragment and which card to show ("none" = off).
 */
public class ConfigProvider extends ContentProvider {
    @Override public boolean onCreate() { return true; }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] args, String sort) {
        if ("/adhan".equals(uri.getPath())) {
            MatrixCursor result = new MatrixCursor(new String[]{"config"});
            result.addRow(new Object[]{AdhanOptions.json(getContext()).toString()}); return result;
        }
        Prefs p = new Prefs(getContext());
        MatrixCursor c = new MatrixCursor(new String[] {"fragment", "home_panel"});
        c.addRow(new Object[] {p.fragment(), p.homePanel()});
        return c;
    }

    @Override public android.content.res.AssetFileDescriptor openAssetFile(Uri uri, String mode) throws java.io.FileNotFoundException {
        if (!"r".equals(mode)) throw new java.io.FileNotFoundException();
        String path = uri.getPath();
        if (path != null && (path.equals("/adhan/timetable.json") || (path.startsWith("/adhan/") && path.endsWith(".mp3") && AdhanOptions.hasVoice(path.substring(7, path.length() - 4))))) {
            try { return getContext().getAssets().openFd(path.substring(1)); }
            catch (java.io.IOException e) { throw new java.io.FileNotFoundException(path); }
        }
        throw new java.io.FileNotFoundException();
    }
    @Override public android.os.Bundle call(String method, String arg, android.os.Bundle extras) {
        if (getContext().checkCallingOrSelfPermission("sa.muslimtv.screensaver.READ_CONFIG") != android.content.pm.PackageManager.PERMISSION_GRANTED)
            throw new SecurityException("Signature permission required");
        if ("ADHAN_STATUS".equals(method) && extras != null) {
            AdhanOptions.prefs(getContext()).edit().putString("status", extras.getString("status", "")).apply();
        }
        if ("SET_THEME".equals(method) && extras != null) {
            getContext().getSharedPreferences("settings", android.content.Context.MODE_PRIVATE).edit()
                .putInt("theme_surface", extras.getInt("surface", 0xFF102D21) | 0xFF000000)
                .putInt("theme_accent", extras.getInt("accent", 0xFFD6AF6B) | 0xFF000000).apply();
        }
        return new android.os.Bundle();
    }

    @Override public String getType(Uri uri) { return null; }
    @Override public Uri insert(Uri uri, ContentValues v) { return null; }
    @Override public int delete(Uri uri, String s, String[] a) { return 0; }
    @Override public int update(Uri uri, ContentValues v, String s, String[] a) { return 0; }
}
