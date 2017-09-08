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
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

import cn.droidlover.xdroid.cache.MemoryCache;
import cn.droidlover.xdroid.demo.App;
import okhttp3.Call;

/**
 * Created by lzmlsfe on 2017/3/8.
 */

public class ThumbLoad {
    private static final ThumbLoad instance = new ThumbLoad();
    private static final int encryDataSizePerTime = 64;
    private static final int headEncrySize = 1024;
    private static final int segmentSize = 4096;

    private String      mThumbCacheDir   = Environment.getExternalStorageDirectory() + "/droid/thumb/";
    private String      mMovieID         = null;
    //private Queue<Bitmap> bitmaps        = new ArrayBlockingQueue<Bitmap>()

    private ThumbLoad() {
    }

    public static ThumbLoad getInstance() {
        return instance;
    }



    public void setThumbCacheDir(String thumbCacheDir){
        mThumbCacheDir = thumbCacheDir;
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

            /*Log.i(App.TAG,"decodeFile s");
            Bitmap bitmap = BitmapFactory.decodeFile(thumbPathName);
            Log.i(App.TAG,"decodeFile e");

            if(bitmap != null){
                imageView.setImageBitmap(bitmap);
            }*/

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


            //byte[] thumbData = decodeThumb(file,key);


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
                        //MemoryCache.getInstance().put(mMovieID,bitmap);
                        imageView.setImageBitmap(bitmap);
                    }
                }

                public void onError(Call call, Exception e, int id){
                }
            });
        }

        return true;
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
