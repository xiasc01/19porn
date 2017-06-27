package cn.droidlover.xdroid.demo;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteConstraintException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.zhy.http.okhttp.OkHttpUtils;
import com.zhy.http.okhttp.callback.StringCallback;

import java.io.File;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import cn.droidlover.qtcontentlayout.QTContentLayout;
import cn.droidlover.xdroid.demo.kit.AppKit;
import cn.droidlover.xdroid.demo.model.MovieInfo;
import cn.droidlover.xdroid.demo.net.JsonCallback;
import cn.droidlover.xdroid.demo.net.NetApi;
import cn.droidlover.xdroid.kit.Kits;
import okhttp3.Call;

/**
 * Created by Administrator on 2017/5/28 0028.
 */

public class VideoManager {

    private DBManager mDbManager        =  null;
    private User      mUser             =  null;
    private int       mReConnectNum     =  0;

    private VideoManager(){
        mDbManager = new DBManager(App.getContext());
        mUser      = User.getInstance();
    };

    public void getVideos(final int videoType, int num, final JsonCallback<MovieInfo> callback){
        if(num == -1){
            if(!getVideosFromLocal(videoType,callback)){
                getVideosFromServer(videoType,10,callback);
            };
        }else {
            getVideosFromServer(videoType,num,callback);
        }
    }

    public boolean getPlayUrl(final String movieId, final User.GetPlayUrlCallback callback){
        StringCallback stringCallback = new StringCallback() {
            @Override
            public void onError(Call call, Exception e, int id) {
                e.printStackTrace();

                boolean isSocketTimeoutException = e instanceof SocketTimeoutException;
                if(isSocketTimeoutException){
                    mReConnectNum++;

                    if(mReConnectNum < 5){
                        AppKit.updateServerUrl();
                        getPlayUrl(movieId,callback);
                    }else {
                        callback.onGetPlayUrl(null);
                    }

                }else {
                    callback.onGetPlayUrl(null);
                }
            }

            @Override
            public void onResponse(String response, int id) {
                callback.onGetPlayUrl(response);
            }
        };

        HashMap<String, String> params = new HashMap<String, String>();
        params.put("request_type","fetch_play_url");
        params.put("user_id",User.getInstance().getUserId());
        params.put("signature", User.getInstance().getSignature());
        params.put("movie_id",movieId);

        if(hasCache(movieId)){
            params.put("isPay","true");
        }else {
            params.put("isPay","false");
        }

        NetApi.invokeGet(params,stringCallback);
        return true;
    }

    private boolean getVideosFromLocal(final int videoType,final JsonCallback<MovieInfo> callback){
        Log.i(App.TAG,"getVideosFromLocal");
        List<MovieInfo.Item> movies = mDbManager.query("" + videoType);
        MovieInfo movieInfo = new MovieInfo();
        movieInfo.setResults(movies);
        callback.onResponse(movieInfo,0);
        return movies.size() > 0;
    }

    private void getVideosFromServer(final int videoType, final int num, final JsonCallback<MovieInfo> callback){
        final JsonCallback<MovieInfo>  localCallback =  new JsonCallback<MovieInfo>() {
            @Override
            public void onFail(Call call, Exception e, int id) {
                if(e instanceof SocketTimeoutException && mReConnectNum < 5){
                    mReConnectNum++;
                    AppKit.updateServerUrl();
                    getVideosFromServer(videoType,num,callback);
                }else {
                    callback.onFail(call,e,id);
                    mReConnectNum = 0;
                }

            }

            @Override
            public void onResponse(MovieInfo response, int id) {
                if(response != null && !response.isError()){
                    List<MovieInfo.Item> movies = mDbManager.add(response.getResults());
                    MovieInfo movieInfo = new MovieInfo();
                    movieInfo.setResults(movies);
                    callback.onResponse(movieInfo,id);
                }
                mReConnectNum = 0;
            }
        };

        HashMap<String, String> params = new HashMap<String, String>();
        params.put("request_type","fetch_video_info");
        params.put("user_id",User.getInstance().getUserId());
        params.put("signature", User.getInstance().getSignature());
        params.put("video_type","" + videoType);

        NetApi.invokeGet(params,localCallback);
    }

    private boolean hasCache(String movieId){
        String cachePathName = AppKit.getMediaCachePath() + "/" + movieId + ".data";
        File file = new File(cachePathName);
        return file.exists();
    }

    private static VideoManager mVideoManager = null;

    public static VideoManager getInstance(){
        if(mVideoManager == null){
            return new VideoManager();
        }
        return mVideoManager;
    }

    private class DBHelper extends SQLiteOpenHelper {
        private static final int DATABASE_VERSION = 1;

        public DBHelper(Context context) {
            super(context, android.os.Environment.getExternalStorageDirectory().getAbsolutePath() +  "/XDroid/19porn_videos.db", null, DATABASE_VERSION);
        }

        //数据库第一次被创建时onCreate会被调用
        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL("CREATE TABLE IF NOT EXISTS video_info" +
                    "(movie_id VARCHAR PRIMARY KEY, title VARCHAR, type VARCHAR,duration VARCHAR, value VARCHAR,thumb_key VARCHAR)");
        }

        //如果DATABASE_VERSION值被改为2,系统发现现有数据库版本不同,即会调用onUpgrade
        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            //db.execSQL("ALTER TABLE person ADD COLUMN other STRING");
        }
    }

    private class DBManager {
        private DBHelper helper;
        private SQLiteDatabase db;

        public DBManager(Context context) {
            helper = new DBHelper(context);
            db = helper.getWritableDatabase();
        }

        public List<MovieInfo.Item> add(List<MovieInfo.Item> movies) {
            List<MovieInfo.Item> results = new ArrayList<MovieInfo.Item>();
            db.beginTransaction();  //开始事务
            try {
                for (MovieInfo.Item movie : movies) {
                    Cursor c = queryMovie(movie.getMovie_id());
                    if(c.getCount() == 0){
                        db.execSQL("INSERT INTO video_info VALUES(?, ?, ?, ?, ?, ?)", new Object[]{movie.getMovie_id(), movie.getTitle(),movie.getType(), movie.getDuration(),movie.getValue(),movie.getThumb_key()});
                        results.add(movie);
                    }
                }
                db.setTransactionSuccessful();  //设置事务成功完成
            }catch (Exception e){
                e.printStackTrace();
                return results;
            }
            finally {
                db.endTransaction();    //结束事务
            }
            return results;
        }

        public List<MovieInfo.Item> query(String videoType) {
            ArrayList<MovieInfo.Item> movies = new ArrayList<MovieInfo.Item>();
            Cursor c = queryTheCursor(videoType);
            while (c.moveToNext()) {
                MovieInfo.Item movie = new MovieInfo.Item();
                movie.setMovie_id(c.getString(c.getColumnIndex("movie_id")));
                movie.setTitle(c.getString(c.getColumnIndex("title")));
                movie.setDuration(c.getString(c.getColumnIndex("duration")));
                movie.setValue(c.getString(c.getColumnIndex("value")));
                movie.setThumb_key(c.getString(c.getColumnIndex("thumb_key")));
                movies.add(movie);
            }
            c.close();
            return movies;
        }

        public void closeDB() {
            db.close();
        }

        private Cursor queryTheCursor(String videoType) {
            Cursor c = db.rawQuery("SELECT * FROM video_info where type = ?", new String[]{videoType});
            return c;
        }

        private Cursor queryMovie(String movieID){
            Cursor c = db.rawQuery("SELECT * FROM video_info where movie_id = ?", new String[]{movieID});
            return c;
        }
    }

}
