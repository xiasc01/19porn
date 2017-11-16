package cn.droidlover.xdroid.demo;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;

import com.zhy.http.okhttp.callback.Callback;
import com.zhy.http.okhttp.callback.StringCallback;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.net.SocketTimeoutException;
import java.util.HashMap;

import cn.droidlover.xdroid.cache.DiskCache;
import cn.droidlover.xdroid.demo.adapter.ShortVideoAdapter;
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
    private String mEmail     =  null;
    private String mCoin      =  null;
    private String mUnVerifyCoin =  null;
    private String mStatus = "false";
    private int mReConnect = 0;

    private static User user = null;
    private int    mReConnectNum = 0;

    public interface LoginCallback{
        public void onLogin(String status);
    }

    public interface GetPlayUrlCallback{
        public void onGetPlayUrl(ShortVideoAdapter.GetPlayUrlResult result);
    }

    public interface GetVideoPraiseCallback{
        public void onGetVideoPraise(String url);
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

    public boolean isLoginOut(){
        return getCacheLoginOutStatus();
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
                if(!response.mStatus.equals("ok")){
                    mUserId    = null;
                    mPassword  = null;
                    return;
                }

                user.mUserName  = response.mUserName;
                user.mEmail     = response.mEmail;
                user.mCoin      = response.mCoin;
                user.mUnVerifyCoin = response.mUnVerifyCoin;
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
        mPassword        = AppKit.stringToMD5(password);
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
                if(!response.mStatus.equals("ok")){
                    loginCallback.onLogin("账号或者密码出错");
                    mUserId    = null;
                    mPassword  = null;
                    return;
                }

                user.mUserId    = response.mUserId;
                user.mPassword  = response.mPassword;
                user.mUserName  = response.mUserName;
                user.mEmail     = response.mEmail;

                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(App.getContext()).edit();
                editor.putString("mUserId",user.mUserId);
                editor.putString("mPassword",user.mPassword);
                editor.remove("loginOutStatus");
                editor.commit();

                loginCallback.onLogin(response.mStatus);
            }
        };

        HashMap<String, String> params = new HashMap<String, String>();
        params.put("request_type","user_login");

        if(userId == null || userId.length() == 0){
            params.put("device_id",AppKit.getDeviceId(App.getContext()));
        }

        params.put("user_id",userId);
        params.put("signature", signature);

        NetApi.invokeGet(params,callback);

        return  true;
    }

    public boolean loginOut(){
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(App.getContext()).edit();
        editor.putString("loginOutStatus","true");
        editor.remove("mPassword");
        editor.commit();

        mUserId = null;
        mPassword = null;

        return  true;
    }

    public String getUserId(){
        if(mUserId != null){
            return mUserId;
        }

        return getCacheUserId();
    }

    public String getUserName(){
        return mUserName;
    }

    public void setUserName(String userName){
        mUserName = userName;
        String signature = getSignature();

        HashMap<String, String> params = new HashMap<String, String>();
        params.put("request_type","modify_user_name");
        params.put("user_id",mUserId);
        params.put("signature", signature);
        params.put("user_name", userName);

        NetApi.invokeGet(params,null);

        return;
    }

    public String getEmail(){
        return mEmail;
    }

    public void setEmail(String email){
        mEmail = email;
        String signature = getSignature();

        HashMap<String, String> params = new HashMap<String, String>();
        params.put("request_type","modify_email");
        params.put("user_id",mUserId);
        params.put("signature", signature);
        params.put("email", email);

        NetApi.invokeGet(params,null);

        return;
    }

    public void setCoin(String coin){
        mCoin = coin;
    }

    public String getCoin(Callback<String> onCallBack){
        if(onCallBack == null){
            return  mCoin;
        }

        String signature = getSignature();

        HashMap<String, String> params = new HashMap<String, String>();
        params.put("request_type","get_coin");
        params.put("user_id",mUserId);
        params.put("signature", signature);

        NetApi.invokeGet(params,onCallBack);

        return "";
    }

    public String getUnVerifyCoin(Callback<String> onCallBack){
        if(onCallBack == null){
            return  mUnVerifyCoin;
        }

        String signature = getSignature();

        HashMap<String, String> params = new HashMap<String, String>();
        params.put("request_type","get_unverify_coin");
        params.put("user_id",mUserId);
        params.put("signature", signature);

        NetApi.invokeGet(params,onCallBack);
        return "";
    }

    public String getPassword(){
        return mPassword;
    }

    public void setUserPassword(String password){
        String signature = getSignature();

        mPassword = AppKit.stringToMD5(password);

        HashMap<String, String> params = new HashMap<String, String>();
        params.put("request_type","modify_password");
        params.put("user_id",mUserId);
        params.put("signature", signature);
        params.put("user_password", mPassword);

        NetApi.invokeGet(params,null);
        PreferenceManager.getDefaultSharedPreferences(App.getContext()).edit().putString("mPassword",user.mPassword).commit();

        return;
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
                user.mUserName  = response.mUserName;
                user.mEmail     = response.mEmail;

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

        String apkSource = "";
        apkSource = AppKit.getPackageSource();
        if(apkSource == null || apkSource.length() == 0){
            apkSource = "someOne";
        }

        params.put("apk_source",apkSource);
        params.put("signature",signature);

        NetApi.invokeGet(params,callback);

        return  true;
    }
}
