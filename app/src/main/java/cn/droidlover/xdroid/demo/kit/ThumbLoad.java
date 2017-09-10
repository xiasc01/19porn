package cn.droidlover.xdroid.demo.kit;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.os.Environment;
import android.util.Log;
import android.widget.ImageView;

import com.zhy.http.okhttp.OkHttpUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

import cn.droidlover.xdroid.cache.MemoryCache;
import cn.droidlover.xdroid.demo.App;
import cn.droidlover.xdroid.demo.VideoManager;
import cn.droidlover.xdroid.demo.model.MovieInfo;
import okhttp3.Call;
import okhttp3.Response;

/**
 * Created by lzmlsfe on 2017/3/8.
 */

public class ThumbLoad {
    private static final ThumbLoad instance = new ThumbLoad();
    private static final int encryDataSizePerTime = 64;
    private static final int headEncrySize = 1024;
    private static final int segmentSize = 4096;
    private String      mThumbCacheDir   = Environment.getExternalStorageDirectory() + "/droid/thumb/";

    private ThumbLoad() {
    }

    public static ThumbLoad getInstance() {
        return instance;
    }



    public void setThumbCacheDir(String thumbCacheDir){
        mThumbCacheDir = thumbCacheDir;
    }

    public Bitmap getThumb(String movieID){
        Log.i(App.TAG,"getThumb enter");
        MovieInfo.Item item = VideoManager.getInstance().getMovieInfoItem(movieID);
        if(item == null){
            Log.e(App.TAG,"get thumb do not has movie " + movieID);
            return  null;
        }

        final String key = item.getThumb_key();
        if(key == null){
            return  null;
        }

        String thumbName     = movieID + ".thumb";
        String thumbPathName = mThumbCacheDir + thumbName;

        File file = new File(thumbPathName);

        while (file.exists()) {
            FileInputStream fins = null;
            try {
                fins = new FileInputStream(file);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                break;
            }

            int bufSize = 0;
            try {
                bufSize = (int) fins.getChannel().size();
            } catch (IOException e) {
                e.printStackTrace();
                break;
            }

            byte[] thumbData = new byte[bufSize];

            try {
                fins.read(thumbData);
            } catch (IOException e) {
                e.printStackTrace();
                break;
            }

            if (thumbData != null) {
                Bitmap bitmap = BitmapFactory.decodeByteArray(thumbData, 0, thumbData.length);

                if (bitmap != null) {
                    Log.i(App.TAG,"getThumb leave from file decode");
                    return bitmap;
                }
            }
            break;
        }

        int thumbPos = 0;
        if(item.getThumb_pos() != null){
            try { thumbPos = Integer.parseInt(item.getThumb_pos());}catch (Exception e){ return null;}
        }

        int thumbSize = 0;
        if(item.getThumb_size() != null){
            try { thumbSize = Integer.parseInt(item.getThumb_size());}catch (Exception e){ return null;}
        }

        String thumbUrl =  item.getThumb_url();
        if(thumbUrl == null){
            return null;
        }

        String range = "bytes=" + thumbPos + "-" + (thumbPos + thumbSize);
        HashMap<String,String> header = new HashMap<String,String>();
        header.put("Range",range);

        Response response = null;
        try {
            response = OkHttpUtils.get().url(thumbUrl)
                    .params(new HashMap<String, String>())
                    .headers(header)
                    .build()
                    .execute();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        byte[] thumbData = decodeThumb(response,key);
        if(thumbData != null){
            try {
                FileOutputStream fileOutputStream = new FileOutputStream(file,false);
                fileOutputStream.write(thumbData);
                fileOutputStream.flush();
            } catch (Exception e) {
                e.printStackTrace();
            }

            Log.i(App.TAG,"getThumb leave from net decode");
            Bitmap bitmap = BitmapFactory.decodeByteArray(thumbData,0,thumbData.length);
            return  bitmap;
        }
        return null;
    }

    public boolean loadImage(final ImageView imageView,final String url,final int pos,final int size,final String key,final String movieID){
        if(imageView == null || movieID == null){
            return false;
        }

        MemoryCache memoryCache = MemoryCache.getInstance();
        if(memoryCache.contains(movieID)){
            byte[] thumbData = (byte[]) memoryCache.get(movieID);
            Bitmap bitmap = BitmapFactory.decodeByteArray(thumbData,0,thumbData.length);
            imageView.setImageBitmap(bitmap);
            return true;
        }

        String thumbName     = movieID + ".thumb";
        String thumbPathName = mThumbCacheDir + thumbName;

        File file = new File(thumbPathName);

        if(file.exists()){
            if(key == null){
                return false;
            }

            FileInputStream fins = null;
            try {
                fins = new FileInputStream(file);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                return  false;
            }


            int bufSize = 0;
            try {
                bufSize = (int)fins.getChannel().size();
            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }

            byte[] thumbData = new byte[bufSize];

            try {
                fins.read(thumbData);
            } catch (IOException e) {
                e.printStackTrace();
                return  false;
            }

            if(thumbData != null){
                memoryCache.put(movieID,thumbData);
                Bitmap bitmap = BitmapFactory.decodeByteArray(thumbData,0,thumbData.length);

                if(bitmap != null){
                    imageView.setImageBitmap(bitmap);
                }
            }
        }else{
            if(url == null || size == 0){
                return false;
            }

            String range = "bytes=" + pos + "-" + (pos + size);
            HashMap<String,String> header = new HashMap<String,String>();
            header.put("Range",range);

            OkHttpUtils.get().url(url)
            .params(new HashMap<String, String>())
            .headers(header)
            .build()
            .execute(new FileCallback(mThumbCacheDir,thumbName) {
                public void onResponse(File file, int id) {
                    byte[] thumbData = decodeThumb(file,key);
                    if(thumbData != null){

                        try {
                            FileOutputStream fileOutputStream = new FileOutputStream(file,false);
                            fileOutputStream.write(thumbData);
                            fileOutputStream.flush();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        Bitmap bitmap = BitmapFactory.decodeByteArray(thumbData,0,thumbData.length);
                        imageView.setImageBitmap(bitmap);
                    }
                }

                public void onError(Call call, Exception e, int id){
                }
            });
        }

        return true;
    }

    private byte[] decodeThumb(Response response, String key){
        DES des = new DES();
        byte[] keyByte = MyBase64.decode(key.getBytes());

        InputStream fins = response.body().byteStream();
        final long size = response.body().contentLength();
        String keyString = new String(keyByte);
        byte[] buf = new byte[(int)size];

        try {
            fins.read(buf);
        } catch (IOException e) {
            e.printStackTrace();
            return  null;
        }

        des.setKey(keyByte);

        byte[] temp1  = new byte[8];
        byte[] temp2  = new byte[8];
        for (int i = 0;i + 8 < size;i += 8){
            if(i < headEncrySize || (i - headEncrySize) % segmentSize < encryDataSizePerTime){
                System.arraycopy(buf,i,temp1,0,8);
                des.decrypt8(temp1,temp2);
                System.arraycopy(temp2,0,buf,i,8);
            }
        }

        try {
            fins.close();
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            return buf;
        }
    }

    private byte[] decodeThumb(File file,String key){
        DES des = new DES();

        byte[] keyByte = MyBase64.decode(key.getBytes());

        FileInputStream fins = null;
        try {
            fins = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return  null;
        }


        int size = 0;
        try {
            size = (int)fins.getChannel().size();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        byte[] buf = new byte[size];

        try {
            fins.read(buf);
        } catch (IOException e) {
            e.printStackTrace();
            return  null;
        }

        des.setKey(keyByte);

        byte[] temp1  = new byte[8];
        byte[] temp2  = new byte[8];
        for (int i = 0;i + 8 < size;i += 8){
            if(i < headEncrySize || (i - headEncrySize) % segmentSize < encryDataSizePerTime){
                System.arraycopy(buf,i,temp1,0,8);
                des.decrypt8(temp1,temp2);
                System.arraycopy(temp2,0,buf,i,8);
            }
        }

        try {
            fins.close();
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            return buf;
        }
    }
}
