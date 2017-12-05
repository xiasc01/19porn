package cn.droidlover.xdroid.demo.kit;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.WindowManager;
import android.widget.ImageView;

import com.squareup.picasso.Downloader;
import com.squareup.picasso.Picasso;
import com.zhy.http.okhttp.OkHttpUtils;

import java.io.ByteArrayInputStream;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cn.droidlover.xdroid.cache.MemoryCache;
import cn.droidlover.xdroid.demo.App;
import cn.droidlover.xdroid.demo.User;
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
    private Picasso     mPicasso;

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 1) {
                BmpImage bmpImage = (BmpImage) msg.obj;
                bmpImage.imageView.setImageBitmap(bmpImage.bitmap);
                Log.i(App.TAG,"loadImage movie from msg id = " + bmpImage.movieId);
            }
        }
    };

    private class BmpImage{
        public  Bitmap bitmap;
        public  ImageView imageView;
        public  String movieId;
    }

    public class PicassoDownloader implements Downloader{
        private Uri mUri = null;

        @Override
        public Response load(Uri uri, int networkPolicy) throws IOException {
            mUri = uri;
            String movieID = uri.getQueryParameter("md5");
            String key     = uri.getQueryParameter("key");
            String url     = "http://" + uri.getHost()  + uri.getPath();
            int pos        = Integer.parseInt(uri.getQueryParameter("pos"));
            int size       = Integer.parseInt(uri.getQueryParameter("size"));

            InputStream stream    = null;
            boolean     fromCache = false;


            String thumbName     = movieID + ".thumb";
            String thumbPathName = mThumbCacheDir + thumbName;
            File file = new File(thumbPathName);

            while (true){
                if(file.exists()){
                    fromCache = true;
                    try {
                        stream = new FileInputStream(file);
                        if(((FileInputStream)stream).getChannel().size() != 0){
                            break;
                        }else{
                            stream = null;
                        }

                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                        stream = null;
                    }
                }

                fromCache = false;
                String range = "bytes=" + pos + "-" + (pos + size - 1);
                HashMap<String,String> header = new HashMap<String,String>();
                header.put("Range",range);

                okhttp3.Response response = OkHttpUtils.get().url(url)
                        .params(new HashMap<String, String>())
                        .headers(header)
                        .build()
                        .execute();

                byte[] thumbData = decodeThumb(response,key,size);
                if(thumbData != null){
                    try {
                        FileOutputStream fileOutputStream = new FileOutputStream(file,false);
                        fileOutputStream.write(thumbData);
                        fileOutputStream.flush();
                        fileOutputStream.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    stream = new ByteArrayInputStream(thumbData);
                }
                break;
            }
            return new Response(stream, fromCache, size);
        }

        @Override
        public void shutdown() {
            //OkHttpUtils.getInstance().cancelTag(mUri);
        }
    }


    private ThumbLoad() {
        Picasso.Builder picassoBuilder = new Picasso.Builder(App.getContext());
        picassoBuilder.downloader(new PicassoDownloader());
        mPicasso = picassoBuilder.build();
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

        byte[] thumbData = decodeThumb(response,key,0);
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
        Log.i(App.TAG,"loadImage movie id = " + movieID);
        if(imageView == null){ return false; }

        //String uri = url + "?&pos=" + pos + "&size=" + size + "&key=" + key + "&md5=" + movieID;
        int width = AppKit.getScreenWidth();
        int height = width * 9 / 16;
        final Bitmap emptyBitmap = Bitmap.createBitmap( width, height, Bitmap.Config.ARGB_8888 );

        //mPicasso.load(uri).resize(width,height).placeholder(new BitmapDrawable(emptyBitmap)).into(imageView);
        //return true;

        if(movieID == null){
            imageView.setImageBitmap(emptyBitmap);
        }

        final Message msg = new Message();
        msg.what = 1;
        final BmpImage bmpImage = new BmpImage();
        msg.obj = bmpImage;

        final MemoryCache memoryCache = MemoryCache.getInstance();
        if(memoryCache.contains(movieID)){
            final byte[] thumbData = (byte[]) memoryCache.get(movieID);
            Bitmap bitmap = BitmapFactory.decodeByteArray(thumbData,0,thumbData.length);
            bmpImage.bitmap = bitmap;
            bmpImage.imageView = imageView;
            handler.sendMessage(msg);
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
                imageView.setImageBitmap(emptyBitmap);
                return  false;
            }

            if(thumbData != null){
                Bitmap bitmap = BitmapFactory.decodeByteArray(thumbData,0,thumbData.length);
                if(bitmap != null){
                    memoryCache.put(movieID,thumbData);
                    bmpImage.bitmap = bitmap;
                    bmpImage.imageView = imageView;
                    bmpImage.movieId  = movieID;
                    handler.sendMessage(msg);
                }else{
                    imageView.setImageBitmap(emptyBitmap);
                }
            }
        }else{
            if(url == null || size == 0){
                imageView.setImageBitmap(emptyBitmap);
                return false;
            }

            Log.i(App.TAG,"loadImage movie from net id = " + movieID);
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
                        memoryCache.put(movieID,thumbData);
                        try {
                            FileOutputStream fileOutputStream = new FileOutputStream(file,false);
                            fileOutputStream.write(thumbData);
                            fileOutputStream.flush();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        Bitmap bitmap = BitmapFactory.decodeByteArray(thumbData,0,thumbData.length);
                        bmpImage.bitmap = bitmap;
                        bmpImage.imageView = imageView;
                        bmpImage.movieId  = movieID;
                        handler.sendMessage(msg);
                    }else{
                        imageView.setImageBitmap(emptyBitmap);
                    }
                }

                public void onError(Call call, Exception e, int id){
                    Log.i(App.TAG,"loadImage movie from net fail id = " + movieID);
                    imageView.setImageBitmap(emptyBitmap);
                    e.printStackTrace();
                }
            });
        }

        return true;
    }

    private byte[] decodeThumb(Response response, String key,int size){
        DES des = new DES();
        byte[] keyByte = MyBase64.decode(key.getBytes());
        InputStream fins = response.body().byteStream();

        if(fins == null || keyByte == null){
            return null;
        }

        byte[] buf = new byte[(int)size];

        int totalReadByte = 0;
        while (totalReadByte < size){
            try {
                int readByte = fins.read(buf,totalReadByte,size - totalReadByte);
                if(readByte == -1){
                    break;
                }
                totalReadByte += readByte;
            } catch (IOException e) {
                e.printStackTrace();
                return  null;
            }
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

    private byte[] decodeThumb(InputStream stream,String key,int size){
        if(stream == null || key == null){
            return null;
        }

        DES des = new DES();
        byte[] keyByte = MyBase64.decode(key.getBytes());

        byte[] buf = new byte[size];

        int totalReadByte = 0;
        while (totalReadByte < size)
        try {
            int readByte = stream.read(buf);
            if(readByte == -1){
                break;
            }
            totalReadByte += readByte;
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
            stream.close();
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

        int totalReadByte = 0;
        while (totalReadByte < size){
            try {
                int readByte = fins.read(buf,totalReadByte,size - totalReadByte);
                if(readByte == -1){
                    break;
                }
                totalReadByte += readByte;
            } catch (IOException e) {
                e.printStackTrace();
                return  null;
            }
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
