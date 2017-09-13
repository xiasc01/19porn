package cn.droidlover.xdroid.demo.kit;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

import cn.droidlover.xdroid.demo.App;
import cn.droidlover.xdroid.demo.ui.MainActivity2;
import okhttp3.MediaType;
import okhttp3.Request;
import okio.Buffer;

/**
 * Created by wanglei on 2016/12/11.
 */

public class AppKit {

    private static String serverUrl = "http://l.dahaiwenhua.com";

    public static MainActivity2 mainActivity = null;

    public static void copyToClipBoard(Context context, String text) {
        ClipboardManager cm = (ClipboardManager) context.getSystemService(
                Context.CLIPBOARD_SERVICE);
        cm.setPrimaryClip(ClipData.newPlainText("xdroid_copy", text));
        Toast.makeText(context, "复制成功", Toast.LENGTH_SHORT).show();
    }

    public static void openInBrowser(Context context, String url) {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        Uri uri = Uri.parse(url);
        intent.setData(uri);
        if (intent.resolveActivity(context.getPackageManager()) != null) {
            context.startActivity(intent);
        } else {
            Toast.makeText(context, "打开失败了，没有可打开的应用", Toast.LENGTH_SHORT).show();
        }
    }

    public static void shareText(Context context, String shareText) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, "分享");
        intent.putExtra(Intent.EXTRA_TEXT, shareText);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(Intent.createChooser(intent, "分享"));
    }

    public static void shareImage(Context context, Uri uri) {
        Intent shareIntent = new Intent();
        shareIntent.setAction(Intent.ACTION_SEND);
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
        shareIntent.setType("image/jpeg");
        context.startActivity(Intent.createChooser(shareIntent, "分享图片"));
    }

    public static boolean isText(MediaType mediaType) {
        if (mediaType.type() != null && mediaType.type().equals("text")) {
            return true;
        }
        if (mediaType.subtype() != null) {
            if (mediaType.subtype().equals("json") ||
                    mediaType.subtype().equals("xml") ||
                    mediaType.subtype().equals("html") ||
                    mediaType.subtype().equals("webviewhtml")
                    )
                return true;
        }
        return false;
    }

    public static String bodyToString(final Request request) {
        try {
            final Request copy = request.newBuilder().build();
            final Buffer buffer = new Buffer();
            copy.body().writeTo(buffer);
            return buffer.readUtf8();
        } catch (final IOException e) {
            return "something error when show requestBody.";
        }
    }

    public static String getDeviceId(Context context){
        String serialNumber = android.os.Build.SERIAL;
        String androidId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);

        String deviceId = null;
        if(serialNumber == null){
            serialNumber = "";
        }

        if(androidId == null){
            androidId = "";
        }

        deviceId = serialNumber + androidId;
        deviceId = stringToMD5(deviceId);
        if(deviceId == null){
            deviceId = "";
        }

        return deviceId;
    }

    public static String stringToMD5(String string) {
        byte[] hash;

        try {
            hash = MessageDigest.getInstance("MD5").digest(string.getBytes("UTF-8"));
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }

        StringBuilder hex = new StringBuilder(hash.length * 2);
        for (byte b : hash) {
            if ((b & 0xFF) < 0x10)
                hex.append("0");
            hex.append(Integer.toHexString(b & 0xFF));
        }

        return hex.toString();
    }

    public static String getServerUrl(){
        return serverUrl;
    }

    public static boolean updateServerUrl(){
        serverUrl = "http://l.3ysd.net";
        return true;
    }

    public static String getMediaCachePath(){
        return Environment.getExternalStorageDirectory().getAbsolutePath() + "/droid/media/";
    }

    public static String getThumbCachePath(){
        return Environment.getExternalStorageDirectory().getAbsolutePath() + "/droid/thumb/";
    }

    public static String getSqlLitePath(){
        return Environment.getExternalStorageDirectory().getAbsolutePath() + "/droid/database/";
    }

    public static int getScreenWidth(){
        WindowManager manager = (WindowManager) App.getContext().getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics outMetrics = new DisplayMetrics();
        manager.getDefaultDisplay().getMetrics(outMetrics);
        int width = outMetrics.widthPixels;
        return  width;
    }

    public static String getCurrentTime(){
        SimpleDateFormat    sDateFormat    =   new SimpleDateFormat("yyyy-MM-dd HH:mm");
        sDateFormat.setTimeZone(TimeZone.getTimeZone("Asia/Shanghai"));
        String    date    =    sDateFormat.format(new    java.util.Date());
        return date;
    }

    public static int dip2px(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    /**
     * 根据手机的分辨率从 px(像素) 的单位 转成为 dp
     */
    public static int px2dip(Context context, float pxValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (pxValue / scale + 0.5f);
    }

    public static void setStatusBarColor(Activity activity,String color){
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                Window window = activity.getWindow();
                window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
                window.setStatusBarColor(Color.parseColor(color));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static InputStream getHttpStream(String path) throws Exception {
        URL url = new URL(path);
        HttpURLConnection conn = (HttpURLConnection)url.openConnection();
        conn.setReadTimeout(10 * 1000);
        conn.setConnectTimeout(10 * 1000);
        conn.setRequestMethod("GET");
        if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
            return conn.getInputStream();
        }
        return null;
    }

}
