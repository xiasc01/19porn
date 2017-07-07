package cn.droidlover.xdroid.demo;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;

import com.zhy.http.okhttp.callback.StringCallback;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.net.SocketTimeoutException;
import java.util.HashMap;

import cn.droidlover.xdroid.cache.DiskCache;
import cn.droidlover.xdroid.demo.kit.AppKit;
import cn.droidlover.xdroid.demo.net.JsonCallback;
import cn.droidlover.xdroid.demo.net.NetApi;
import okhttp3.Call;
/**
 * Created by Administrator on 2017/4/19 0019.
 */

public class User {

    private final String LoginMagic1 = "xskdjs";
    private final String LoginMagic2 = "agzz";
    private final String LoginMagic3 = "tmhks";

    private String mUserId    =  null;
    private String mUserName  =  null;
    private String mPassword  =  null;

    private String mStatus = "false";
    private int mReConnect = 0;

    private static User user = null;
    private int    mReConnectNum = 0;


    public interface LoginCallback{
        public void onLogin(String status);
    }

    public interface GetPlayUrlCallback{
        public void onGetPlayUrl(String url);
    }


    public static User getInstance(){
        if(user == null){
            user = new User();
            user.init();
        }
        return  user;
    }

    private User(){

    }

    public boolean init(){
        if(getCacheLoginOutStatus()){
            return true;
        }

        mUserId     = getCacheUserId();
        mPassword   = getCacheUserPassword();
        if(mUserId == null){
            register();
        }else{
            login();
        }
        return true;
    }

    public boolean getLoginStatus(){
        if(mUserId == null || mPassword == null){
            return false;
        }
        return  true;
    }

    public void login(){
        String signature = getSignature();

        final User user = this;
        JsonCallback<User> callback = new JsonCallback<User>() {
            @Override
            public void onFail(Call call, Exception e, int id) {
                e.printStackTrace();

                boolean isSocketTimeoutException = e instanceof SocketTimeoutException;
                if(isSocketTimeoutException){
                    mReConnect++;

                    if(mReConnect < 5){
                        AppKit.updateServerUrl();
                        login();
                    }else {
                    }
                }else {
                    //mUserId    = null;
                    //mPassword  = null;
                }
            }

            @Override
            public void onResponse(User response, int id) {
                if(!user.mStatus.equals("ok")){
                    mUserId    = null;
                    mPassword  = null;
                    return;
                }

                user.mUserName  = response.mUserName;
            }
        };

        HashMap<String, String> params = new HashMap<String, String>();
        params.put("request_type","user_login");
        params.put("user_id",mUserId);
        params.put("signature", signature);

        NetApi.invokeGet(params,callback);

        return;
    }

    public boolean manualLogin(final String userId, final String password, final LoginCallback loginCallback){
        String passwordHash = LoginMagic1 + password + LoginMagic2;
        passwordHash = AppKit.stringToMD5(passwordHash);

        mPassword        = passwordHash;
        mUserId          = userId;

        String signature = getSignature();

        final User user = this;
        JsonCallback<User> callback = new JsonCallback<User>() {
            @Override
            public void onFail(Call call, Exception e, int id) {
                e.printStackTrace();

                boolean isSocketTimeoutException = e instanceof SocketTimeoutException;
                if(isSocketTimeoutException){
                    mReConnect++;

                    if(mReConnect < 5){
                        AppKit.updateServerUrl();
                        manualLogin(userId,password,loginCallback);
                    }else {
                        loginCallback.onLogin("网络超时 请稍后再试");
                        mUserId    = null;
                        mPassword  = null;
                    }
                }else {
                    loginCallback.onLogin("服务器出现错误");
                    mUserId    = null;
                    mPassword  = null;
                }
            }

            @Override
            public void onResponse(User response, int id) {
                if(!user.mStatus.equals("ok")){
                    loginCallback.onLogin(user.mStatus);
                    mUserId    = null;
                    mPassword  = null;
                    return;
                }

                user.mUserId    = response.mUserId;
                user.mPassword  = response.mPassword;

                DiskCache.getInstance(App.getContext()).put("mUserId",user.mUserId);
                DiskCache.getInstance(App.getContext()).put("mPassword",user.mPassword);

                loginCallback.onLogin(user.mStatus);
            }
        };

        HashMap<String, String> params = new HashMap<String, String>();
        params.put("request_type","user_login");
        params.put("user_id",userId);
        params.put("signature", signature);

        NetApi.invokeGet(params,callback);

        return  true;
    }

    public boolean loginOut(){
        DiskCache.getInstance(App.getContext()).remove("mUserId");
        DiskCache.getInstance(App.getContext()).remove("mPassword");
        DiskCache.getInstance(App.getContext()).put("loginOutStatus","true",Integer.MAX_VALUE);

        mUserId = null;
        mPassword = null;

        return  true;
    }

    public String getUserId(){
        return mUserId;
    }

    public String getUserName(){
        return mUserName;
    }

    public void setUserName(String userName){
        mUserName = userName;
        return;
    }

    public void setUserPassword(String pwd){

    }

    public Bitmap setUserPortrait(Bitmap bitmap) {
        float width = bitmap.getWidth();
        float height = bitmap.getHeight();

        Matrix matrix = new Matrix();
        float scaleWidth = ((float) 200) / width;
        matrix.postScale(scaleWidth, scaleWidth);

        Bitmap dstBitmap = Bitmap.createBitmap(bitmap, 0, 0, (int) width,
                (int) width, matrix, true);


        String thumbCacheDir   = Environment.getExternalStorageDirectory() + "/droid/thumb/portrait.thumb";
        FileOutputStream fileOutputStream = null;
        try {
            fileOutputStream = new FileOutputStream(thumbCacheDir);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        dstBitmap.compress(Bitmap.CompressFormat.JPEG,85,fileOutputStream);

        return  dstBitmap;
    }

    public Bitmap getUserPortrait() throws Exception {
        File file = null;
        String thumbCacheDir   = Environment.getExternalStorageDirectory() + "/droid/thumb/";
        file = new File(thumbCacheDir);
        if(!file.exists()){
            file.mkdirs();
        }


        String thumbName     =  "portrait.thumb";
        String thumbPathName = thumbCacheDir + thumbName;

        file = new File(thumbPathName);
        Bitmap bitmap = null;
        if(file.exists()){
            FileInputStream fileInputStream = new FileInputStream(thumbPathName);
            bitmap = BitmapFactory.decodeStream(fileInputStream);
        }else{
            String server = AppKit.getServerUrl();
            String Url    = server + "/test.jpg";
            bitmap = BitmapFactory.decodeStream(AppKit.getHttpStream(Url));
            if(bitmap != null){
                file = new File(thumbPathName);
                if(!file.exists()){
                    file.createNewFile();
                }

                FileOutputStream out = new FileOutputStream(thumbPathName);
                bitmap.compress(Bitmap.CompressFormat.PNG, 90, out);
                out.flush();
                out.close();
            }
        }

        return  bitmap;
    }

    public  String getSignature(){
        if(mUserId == null){
            return null;
        }

        String password = mPassword;
        if(mPassword == null){
            password = "";
        }

        String time = AppKit.getCurrentTime();
        String signature = LoginMagic1 + time + LoginMagic2 + mUserId + LoginMagic3 + password;
        signature = AppKit.stringToMD5(signature);
        return signature;
    }

    private String getCacheUserId(){
        return PreferenceManager.getDefaultSharedPreferences(App.getContext()).getString("mUserId",null);
    }

    private String getCacheUserPassword(){
        return PreferenceManager.getDefaultSharedPreferences(App.getContext()).getString("mPassword",null);
    }

    private boolean getCacheLoginOutStatus(){
        String loginOutStatus = PreferenceManager.getDefaultSharedPreferences(App.getContext()).getString("loginOutStatus",null);
        if(loginOutStatus == null || !loginOutStatus.equals("true")){
            return  false;
        }
        return  true;
    }

    private boolean register(){
        String deviceId = AppKit.getDeviceId(App.getContext());
        String time = AppKit.getCurrentTime();
        String signature = LoginMagic1 + time + LoginMagic2 + deviceId + LoginMagic3;
        signature = AppKit.stringToMD5(signature);

        final User user = this;
        JsonCallback<User> callback = new JsonCallback<User>() {
            @Override
            public void onFail(Call call, Exception e, int id) {
                e.printStackTrace();

                boolean isSocketTimeoutException = e instanceof SocketTimeoutException;
                if(isSocketTimeoutException && mReConnectNum < 5){
                    AppKit.updateServerUrl();
                    register();
                    mReConnectNum++;
                }
            }

            @Override
            public void onResponse(User response, int id) {
                if(!response.mStatus.equals("ok")){
                    register();
                    return;
                }

                user.mUserId    = response.mUserId;
                user.mPassword  = response.mPassword;

                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(App.getContext()).edit();
                editor.putString("mUserId",user.mUserId);
                editor.putString("mPassword",user.mPassword);
                editor.putString("mUserName",user.mUserName);
                editor.commit();

                mReConnectNum = 0;
            }
        };

        HashMap<String, String> params = new HashMap<String, String>();
        params.put("request_type","user_register");
        params.put("device_id",deviceId);
        params.put("signature",signature);

        NetApi.invokeGet(params,callback);

        return  true;
    }
}
