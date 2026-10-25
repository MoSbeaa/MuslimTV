package sa.muslimtv.quran;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/** Original Quran Android Madani images, kept unchanged in private persistent storage. */
final class PageStore {
    interface Progress { void update(String message); }
    private final Context context;
    private final File directory;
    final ExecutorService reader = Executors.newSingleThreadExecutor();
    private final ExecutorService downloader = Executors.newSingleThreadExecutor();
    final AtomicBoolean downloading = new AtomicBoolean(false);
    private volatile boolean cancelled;
    private volatile HttpURLConnection bulkConnection;
    private final Object fileLock = new Object();
    static final String SOURCE = "https://android.quran.com/data/";

    PageStore(Context context) {
        this.context = context.getApplicationContext();
        directory = new File(context.getFilesDir(), "pages");
        directory.mkdirs();
    }
    static String name(int page) { return String.format(Locale.ROOT, "page%03d.png", page); }
    private HttpURLConnection connect(String address) throws IOException {
        HttpURLConnection c = (HttpURLConnection) new URL(address).openConnection();
        c.setConnectTimeout(15000); c.setReadTimeout(20000);
        c.setRequestProperty("User-Agent", "Mozilla/5.0 MuslimTV-Quran/0.1");
        if (c.getResponseCode() != 200) { int code = c.getResponseCode(); c.disconnect(); throw new IOException("HTTP " + code); }
        return c;
    }
    Bitmap load(int page) throws IOException {
        File file = new File(directory, name(page));
        Bitmap bitmap = BitmapFactory.decodeFile(file.getPath());
        if (bitmap != null) return bitmap;
        try (InputStream in = context.getAssets().open("pages/" + name(page))) {
            bitmap = BitmapFactory.decodeStream(in);
        } catch (IOException ignored) { }
        if (bitmap != null) return bitmap;
        HttpURLConnection c = connect(SOURCE + "width_1024/" + name(page));
        try (InputStream in = c.getInputStream()) { save(page, in, 3 * 1024 * 1024); }
        finally { c.disconnect(); }
        bitmap = BitmapFactory.decodeFile(file.getPath());
        if (bitmap == null) throw new IOException("Invalid page image");
        return bitmap;
    }
    private void save(int page, InputStream input, int maximum) throws IOException {
        synchronized (fileLock) {
            File temp = new File(directory, name(page) + ".part");
            try {
                try (OutputStream out = new FileOutputStream(temp)) {
                    byte[] buffer = new byte[16384]; int n, size = 0;
                    while ((n = input.read(buffer)) != -1) {
                        size += n;
                        if (size > maximum) throw new IOException("Image too large");
                        out.write(buffer, 0, n);
                    }
                }
                BitmapFactory.Options bounds = new BitmapFactory.Options(); bounds.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(temp.getPath(), bounds);
                if (bounds.outWidth < 500 || bounds.outHeight < 500 || bounds.outWidth > 3000 || bounds.outHeight > 4000)
                    throw new IOException("Invalid page dimensions");
                if (!temp.renameTo(new File(directory, name(page)))) throw new IOException("Could not save page");
            } finally { temp.delete(); }
        }
    }
    int count() {
        int count = 0;
        for (int i = 1; i <= 604; i++) if (i <= 2 || new File(directory, name(i)).length() > 1000) count++;
        return count;
    }
    void downloadAll(Progress progress) {
        if (!downloading.compareAndSet(false, true)) return;
        cancelled = false;
        downloader.execute(() -> {
            String message;
            try {
                if (directory.getUsableSpace() < 150L * 1024 * 1024) throw new IOException("Not enough free space");
                progress.update("جارٍ تنزيل المصحف كاملًا…");
                HttpURLConnection c = connect(SOURCE + "zips/images_1024.zip");
                bulkConnection = c;
                try (ZipInputStream zip = new ZipInputStream(new BufferedInputStream(c.getInputStream()))) {
                    ZipEntry entry; int entries = 0;
                    while (!cancelled && (entry = zip.getNextEntry()) != null) {
                        if (++entries > 1500) throw new IOException("Unexpected archive");
                        String leaf = entry.getName().substring(entry.getName().lastIndexOf('/') + 1);
                        if (leaf.matches("page[0-9]{3}\\.png")) {
                            int p = Integer.parseInt(leaf.substring(4, 7));
                            if (p >= 1 && p <= 604) {
                                save(p, zip, 3 * 1024 * 1024);
                                if (p % 8 == 0) progress.update("تنزيل المصحف · " + ReaderActivity.ar(count()) + " / ٦٠٤");
                            }
                        }
                        zip.closeEntry();
                    }
                } finally { c.disconnect(); bulkConnection = null; }
                message = cancelled ? "توقف التنزيل؛ الصفحات المحفوظة متاحة" : count() == 604
                    ? "اكتمل تنزيل المصحف · متاح دون إنترنت" : "لم تكتمل جميع الصفحات؛ أعد محاولة التنزيل";
            } catch (IOException e) {
                message = cancelled ? "توقف التنزيل؛ الصفحات المحفوظة متاحة" : "تعذر إكمال التنزيل. تحقق من الاتصال والمساحة وأعد المحاولة";
            } finally { downloading.set(false); }
            progress.update(message);
        });
    }
    void cancelDownload() { cancelled = true; HttpURLConnection c = bulkConnection; if (c != null) c.disconnect(); }
    void close() { cancelDownload(); reader.shutdownNow(); downloader.shutdownNow(); }
}
