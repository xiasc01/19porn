package cn.droidlover.xdroid.demo;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.graphics.Bitmap;
import android.util.Log;
import android.widget.ImageView;

import com.zhy.http.okhttp.callback.StringCallback;

import java.io.File;
import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.droidlover.xdroid.demo.adapter.ShortVideoAdapter;
import cn.droidlover.xdroid.demo.kit.AppKit;
import cn.droidlover.xdroid.demo.kit.ThumbLoad;
import cn.droidlover.xdroid.demo.model.MovieInfo;
import cn.droidlover.xdroid.demo.net.JsonCallback;
import cn.droidlover.xdroid.demo.net.NetApi;
import okhttp3.Call;

/**
 * Created by Administrator on 2017/5/28 0028.
 */

public class VideoManager extends Thread {

    private DBManager mDbManager                            =  null;
    private User      mUser                                 =  null;
    private int       mReConnectNum                         =  0;
    private Map<String,MovieInfo.Item>  mMovies             = new HashMap<String,MovieInfo.Item>();
    private Map<String,List<MovieInfo.Item> > mMovieSeries = new HashMap<String,List<MovieInfo.Item>>();
    private Map<String ,List<MovieInfo.Item> > mTypeMovies  = new HashMap<String,List<MovieInfo.Item> >();
    private Map<String,ImageView>  mThumbImageView          = new HashMap<String,ImageView>();

    private String mCurrentType   = "0";
    private int mLastType      = 0;
    private int mCurrentPos    = 0;
    private int mLastPos       = 0;

    private boolean mStopDecodeThumb = false;
    private Map<String,ThumbCache>    mMovieThumbCaches         = new HashMap<String,ThumbCache>();

    class ThumbCache{
        Bitmap  bitmap;
        String  type;
        int     pos;
    }

    private VideoManager(){
        mDbManager = new DBManager(App.getContext());
        mUser      = User.getInstance();
        //this.start();
    };

    @Override
    public void run() {
        while (!mStopDecodeThumb){
            Log.i(App.TAG,"try decoder thumb once");
            try {Thread.sleep(100);} catch (InterruptedException e) {e.printStackTrace();}
            synchronized (mMovieThumbCaches){
                if(!mTypeMovies.containsKey(mCurrentType)){
                    Log.e(App.TAG,"mTypeMovies do not contains key " + mCurrentType);
                    try {Thread.sleep(10);} catch (InterruptedException e) {e.printStackTrace();}
                    continue;
                }

                List<MovieInfo.Item> movies = mTypeMovies.get(mCurrentType);
                int pos = movies.size() - mCurrentPos - 1;

                if(mMovieThumbCaches.size() > 31){
                    for (String key : mMovieThumbCaches.keySet()) {
                        ThumbCache thumbCache = mMovieThumbCaches.get(key);
                        boolean needRecycle = false;
                        if(!thumbCache.type.equals(mCurrentType) ){
                            needRecycle = true;
                        }

                        if(!needRecycle && thumbCache.pos > pos + 15 && thumbCache.pos < pos - 15){
                            needRecycle = true;
                        }

                        if(needRecycle){
                            thumbCache = mMovieThumbCaches.remove(key);
                            if(!thumbCache.bitmap.isRecycled()){
                                thumbCache.bitmap.recycle();
                            }
                            break;
                        }
                    }
                }

                boolean doDecodeThumb = false;
                for(int i = 0;i < 16;i++){
                    int curPos  = pos - i;
                    MovieInfo.Item item = null;

                    if(curPos >= 0 && curPos < movies.size()){
                        item = movies.get(curPos);
                        if(item != null){
                            if(!mMovieThumbCaches.containsKey(item.getMovie_id())){
                                doDecodeThumb = true;
                            }
                        }
                    }

                    if(!doDecodeThumb){
                        curPos = pos + i;
                        if(curPos >= 0 && curPos < movies.size()){
                            item = movies.get(curPos);
                            if(item != null){
                                if(!mMovieThumbCaches.containsKey(item.getMovie_id())){
                                    doDecodeThumb = true;
                                }
                            }
                        }
                    }



                    if(doDecodeThumb){
                        Bitmap bitmap = ThumbLoad.getInstance().getThumb(item.getMovie_id());
                        ThumbCache thumbCache = new ThumbCache();
                        thumbCache.bitmap  = bitmap;
                        thumbCache.pos      = curPos;
                        thumbCache.type     = mCurrentType;
                        mMovieThumbCaches.put(item.getMovie_id(),thumbCache);

                        if(item.getSet_name() != null && item.getSet_name().length() > 0){
                            List<MovieInfo.Item> movieSets = mMovieSeries.get(item.getSet_name());
                            for(int j = 1;j < 4 && j < movieSets.size();j++){
                                MovieInfo.Item itemSet = movieSets.get(j);

                                bitmap = ThumbLoad.getInstance().getThumb(itemSet.getMovie_id());
                                thumbCache = new ThumbCache();
                                thumbCache.bitmap  = bitmap;
                                thumbCache.pos      = curPos;
                                thumbCache.type     = mCurrentType;
                                mMovieThumbCaches.put(itemSet.getMovie_id(),thumbCache);
                            }
                        }
                        break;
                    }
                }

                if(!doDecodeThumb){
                    try {Thread.sleep(100);} catch (InterruptedException e) {e.printStackTrace();}
                    continue;
                }else{
                    try {Thread.sleep(10);} catch (InterruptedException e) {e.printStackTrace();}
                    continue;
                }
            }
        }
    }

    public void getVideos(final int videoType, int id, final JsonCallback<MovieInfo> callback){
        if(id == -1){
            /*if(!getVideosFromLocal(videoType,callback)){
                getVideosFromServer(videoType,10,callback);
            }*/
            getVideosFromLocal(videoType,callback);
        }else {
            getVideosFromServer(videoType,null,id,callback);
        }
    }

    public void getSeriesVideos(String seriesName,int startId,final JsonCallback<MovieInfo> callback){
        if(startId == -1){
            if(mMovieSeries.containsKey(seriesName)){
                List<MovieInfo.Item> movies = mMovieSeries.get(seriesName);
                MovieInfo movieInfo = new MovieInfo();
                movieInfo.setResults(movies);
                callback.onResponse(movieInfo,0);
            }
        }else{
            getVideosFromServer(0,seriesName,startId,callback);
        }
    }

    public void getEnShrineVideos(final JsonCallback<MovieInfo> callback){
        MovieInfo movieInfo = new MovieInfo();
        List<MovieInfo.Item> movies = new ArrayList<MovieInfo.Item>();
        movieInfo.setResults(movies);
        for (Map.Entry<String, MovieInfo.Item> entry : mMovies.entrySet()){
            MovieInfo.Item item = entry.getValue();
            if(item.getIsEnshrine().equals("1")){
                movies.add(item);
            }
        }
        callback.onResponse(movieInfo,0);
    }

    public void getHasPlayVideos(final JsonCallback<MovieInfo> callback){
        MovieInfo movieInfo = new MovieInfo();
        List<MovieInfo.Item> movies = new ArrayList<MovieInfo.Item>();
        movieInfo.setResults(movies);
        for (Map.Entry<String, MovieInfo.Item> entry : mMovies.entrySet()){
            MovieInfo.Item item = entry.getValue();
            if(item.getIsPlay().equals("1")){
                movies.add(item);
            }
        }
        callback.onResponse(movieInfo,0);
    }

    public void modifyMovieInfo(String movieId, Map<String,String> modifyMovieInfos){
        Log.i(App.TAG,"modifyMovieInfo enter");
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("request_type","modify_movie_info");
        params.put("movie_id",movieId);

        MovieInfo.Item  movieItem = mMovies.get(movieId);
        for (Map.Entry<String, String> entry : modifyMovieInfos.entrySet()) {
            if(entry.getKey().equals("title")){
                movieItem.setTitle(entry.getValue());
            }
            if(entry.getKey().equals("score")){
                movieItem.setScore(entry.getValue());
            }
            if(entry.getKey().equals("pic_score")){
                movieItem.setPic_score(entry.getValue());
            }
            if(entry.getKey().equals("value")){
                movieItem.setValue(entry.getValue());
            }
            if(entry.getKey().equals("sub_type1")){
                movieItem.setSub_type1(entry.getValue());
            }
        }

        mDbManager.update(movieId,modifyMovieInfos);

        for (Map.Entry<String, String> entry : modifyMovieInfos.entrySet()) {
            Log.i(App.TAG,"modifyMovieInfo Key = " + entry.getKey() + ", Value = " + entry.getValue());
            params.put(entry.getKey(),entry.getValue());
        }


        NetApi.invokeGet(params,null);
    }

    public boolean getVideoPraise(final String movieId, final User.GetVideoPraiseCallback callback){
        StringCallback stringCallback = new StringCallback() {
            @Override
            public void onError(Call call, Exception e, int id) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(String response, int id) {
                callback.onGetVideoPraise(response);
            }
        };

        HashMap<String, String> params = new HashMap<String, String>();
        params.put("request_type","fetch_video_praise");
        params.put("movie_id",movieId);
        NetApi.invokeGet(params,stringCallback);
        return true;
    }

    public boolean addVideoPraise(final String movieId){
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("request_type","add_video_praise");
        params.put("movie_id",movieId);
        NetApi.invokeGet(params,null);

        Map<String,String> mapMovieInfos = new HashMap<String,String>();
        mapMovieInfos.put("isPraise","1");
        mDbManager.update(movieId,mapMovieInfos);
        return true;
    }

    public boolean setEnshrine(final String movieId,String isEnshrine){
        Map<String,String> mapMovieInfos = new HashMap<String,String>();
        mapMovieInfos.put("isEnshrine",isEnshrine);
        mDbManager.update(movieId,mapMovieInfos);
        return true;
    }

    public boolean getPlayUrl(final String movieId, final User.GetPlayUrlCallback callback){
        final JsonCallback<ShortVideoAdapter.GetPlayUrlResult>  localCallback =new JsonCallback<ShortVideoAdapter.GetPlayUrlResult>() {
            @Override
            public void onFail(Call call, Exception e, int id) {
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
            public void onResponse(ShortVideoAdapter.GetPlayUrlResult response, int id) {
                callback.onGetPlayUrl(response);
                if(response.state){
                    Map<String,String> mapMovieInfos = new HashMap<String,String>();
                    mapMovieInfos.put("isPlay","1");
                    mDbManager.update(movieId,mapMovieInfos);
                }
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

        NetApi.invokeGet(params,localCallback);
        return true;
    }

    public MovieInfo.Item getMovieInfoItem(String movieId){
        if(mMovies.containsKey(movieId)){
            return mMovies.get(movieId);
        }
        return  null;
    }

    public List<MovieInfo.Item> getMovieSet(String setName){
        if(mMovieSeries.containsKey(setName)){
            return mMovieSeries.get(setName);
        }
        return null;
    }

    public void setThumbToImageView(String movieId,String type, int pos, ImageView imageView){
        Log.i(App.TAG,"setThumbToImageView enter");
        //synchronized (mMovieThumbCaches){
            mCurrentPos   = pos;
            mCurrentType  = type;
            if(mMovieThumbCaches.containsKey(movieId)){
                ThumbCache thumbCache = mMovieThumbCaches.get(movieId);
                 imageView.setImageBitmap(thumbCache.bitmap);
            }
       // }
        Log.i(App.TAG,"setThumbToImageView leave");
    }

    private boolean getVideosFromLocal(final int videoType,final JsonCallback<MovieInfo> callback){
        Log.i(App.TAG,"getVideosFromLocal");
        List<MovieInfo.Item> movies = mDbManager.query("" + videoType);
        List<MovieInfo.Item> movie2s = new ArrayList<MovieInfo.Item>();

        List<MovieInfo.Item> movie3s = null;
        if(mTypeMovies.containsKey(videoType + "")){
            movie3s = mTypeMovies.get(videoType + "");
        }else{
            movie3s = new ArrayList<MovieInfo.Item>();
            mTypeMovies.put(videoType + "",movie3s);
        }

        for(int i = 0;i < movies.size();i++){
            MovieInfo.Item item = movies.get(i);
            if(!mMovies.containsKey(item.getMovie_id())){
                mMovies.put(item.getMovie_id(),item);
            }

            if(item.getSet_name() != null && item.getSet_name().length() > 0){
                if(mMovieSeries.containsKey(item.getSet_name())){
                    List<MovieInfo.Item>movieItems = mMovieSeries.get(item.getSet_name());
                    movieItems.add(item);
                }else{
                    movie2s.add(item);
                    movie3s.add(item);
                    List<MovieInfo.Item>movieItems = new ArrayList<MovieInfo.Item>();
                    movieItems.add(item);
                    mMovieSeries.put(item.getSet_name(),movieItems);
                }
            }else{
                movie2s.add(item);
                movie3s.add(item);
            }
        }

        MovieInfo movieInfo = new MovieInfo();
        movieInfo.setResults(movie2s);
        callback.onResponse(movieInfo,0);
        return movie2s.size() > 0;
    }

    private void getVideosFromServer(final int videoType,final String seriesVideoName,final int id_index, final JsonCallback<MovieInfo> callback){
        final JsonCallback<MovieInfo>  localCallback =  new JsonCallback<MovieInfo>() {
            @Override
            public void onFail(Call call, Exception e, int id) {
                if(e instanceof SocketTimeoutException && mReConnectNum < 5){
                    mReConnectNum++;
                    AppKit.updateServerUrl();
                    getVideosFromServer(videoType,seriesVideoName,id_index,callback);
                }else {
                    callback.onFail(call,e,id);
                    mReConnectNum = 0;
                }

            }

            @Override
            public void onResponse(MovieInfo response, int id) {
                if(response != null && !response.isError()){
                    List<MovieInfo.Item> movies = mDbManager.add(response.getResults());
                    List<MovieInfo.Item> movie2s = new ArrayList<MovieInfo.Item>();

                    List<MovieInfo.Item> movie3s = null;
                    if(mTypeMovies.containsKey(videoType + "")){
                        movie3s = mTypeMovies.get(videoType + "");
                    }else{
                        movie3s = new ArrayList<MovieInfo.Item>();
                        mTypeMovies.put(videoType + "",movie3s);
                    }

                    for(int i = 0;i < movies.size();i++){
                        MovieInfo.Item item = movies.get(i);
                        if(!mMovies.containsKey(item.getMovie_id())){
                            mMovies.put(item.getMovie_id(),item);
                        }

                        if(item.getSet_name() != null && item.getSet_name().length() > 0){
                            if(mMovieSeries.containsKey(item.getSet_name())){
                                List<MovieInfo.Item>movieItems = mMovieSeries.get(item.getSet_name());
                                movieItems.add(item);
                            }else{
                                movie2s.add(item);
                                movie3s.add(item);
                                List<MovieInfo.Item>movieItems = new ArrayList<MovieInfo.Item>();
                                movieItems.add(item);
                                mMovieSeries.put(item.getSet_name(),movieItems);
                            }
                        }else{
                            movie2s.add(item);
                            movie3s.add(item);
                        }
                    }
                    MovieInfo movieInfo = new MovieInfo();
                    if(seriesVideoName != null && seriesVideoName.length() > 0){
                        movieInfo.setResults(mMovieSeries.get(seriesVideoName));
                    }else{
                        movieInfo.setResults(movie2s);
                    }
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
        params.put("id_index","" + id_index);
        if(seriesVideoName != null && seriesVideoName.length() > 0){
            params.put("series_video_name","" + seriesVideoName);
        }


        NetApi.invokeGet(params,localCallback);
    }

    public boolean hasCache(String movieId){
        String cachePathName = AppKit.getMediaCachePath() + "/" + movieId + ".data";
        File file = new File(cachePathName);
        return file.exists();
    }

    private static VideoManager mVideoManager = null;

    public static VideoManager getInstance(){
        if(mVideoManager == null){
            mVideoManager = new VideoManager();
            return mVideoManager;
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
                    "(id mediumint PRIMARY KEY,movie_id VARCHAR, title VARCHAR,score tinyint,pic_score tinyint,sub_type1 VARCHAR,type VARCHAR,duration VARCHAR, value VARCHAR, set_name VARCHAR,thumb_path VARCHAR,thumb_pos bigint,thumb_size int,thumb_key VARCHAR,grade int,isPraise int,isEnshrine int,isPlay int)");
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
                        db.execSQL("INSERT INTO video_info VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                                new Object[]{Integer.parseInt(movie.getId()), movie.getMovie_id(), movie.getTitle(),movie.getScore(),movie.getPic_score(),movie.getSub_type1(),movie.getType(), movie.getDuration(),movie.getValue(),movie.getSet_name(),movie.getThumb_url(),movie.getThumb_pos(),movie.getThumb_size(),movie.getThumb_key(),movie.getGrade(),movie.getIsPraise(),movie.getIsEnshrine(),movie.getIsPlay()});
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

        public void update(String movieId, Map<String,String> modifyMovieInfos){
            ContentValues cv = new ContentValues();
            for (Map.Entry<String, String> entry : modifyMovieInfos.entrySet()) {
                cv.put(entry.getKey(), entry.getValue());
            }

            String[] args = {movieId};

            db.update("video_info",cv,"movie_id=?",args);
        }

        public List<MovieInfo.Item> query(String videoType) {
            ArrayList<MovieInfo.Item> movies = new ArrayList<MovieInfo.Item>();
            Cursor c = queryTheCursor(videoType);
            while (c.moveToNext()) {
                MovieInfo.Item movie = new MovieInfo.Item();
                movie.setId(c.getInt(c.getColumnIndex("id")) + "");
                movie.setMovie_id(c.getString(c.getColumnIndex("movie_id")));
                movie.setTitle(c.getString(c.getColumnIndex("title")));
                movie.setDuration(c.getString(c.getColumnIndex("duration")));
                movie.setValue(c.getString(c.getColumnIndex("value")));
                movie.setThumb_key(c.getString(c.getColumnIndex("thumb_key")));
                movie.setThumb_size(c.getString(c.getColumnIndex("thumb_size")));
                movie.setThumb_pos(c.getString(c.getColumnIndex("thumb_pos")));
                movie.setThumb_url(c.getString(c.getColumnIndex("thumb_path")));
                movie.setSet_name(c.getString(c.getColumnIndex("set_name")));
                movie.setType(c.getString(c.getColumnIndex("type")));
                movie.setScore(c.getString(c.getColumnIndex("score")));
                movie.setPic_score(c.getString(c.getColumnIndex("pic_score")));
                movie.setSub_type1(c.getString(c.getColumnIndex("sub_type1")));
                movie.setGrade(c.getString(c.getColumnIndex("grade")));
                movie.setIsPraise(c.getString(c.getColumnIndex("isPraise")));
                movie.setIsEnshrine(c.getString(c.getColumnIndex("isEnshrine")));
                movie.setIsPlay(c.getString(c.getColumnIndex("isPlay")));
                movies.add(movie);
            }
            c.close();
            return movies;
        }

        public void closeDB() {
            db.close();
        }

        private Cursor queryTheCursor(String videoType) {
            Cursor c = db.rawQuery("SELECT * FROM video_info where type = ? order by id", new String[]{videoType});
            return c;
        }

        private Cursor queryMovie(String movieID){
            Cursor c = db.rawQuery("SELECT * FROM video_info where movie_id = ?", new String[]{movieID});
            return c;
        }
    }

}
