package cn.droidlover.xdroid.demo.net;

import android.util.Log;

import com.zhy.http.okhttp.OkHttpUtils;
import com.zhy.http.okhttp.callback.Callback;
import com.zhy.http.okhttp.callback.StringCallback;

import java.net.SocketTimeoutException;
import java.util.HashMap;

import cn.droidlover.xdroid.demo.kit.AppKit;
import cn.droidlover.xdroid.demo.model.MovieInfo;
import okhttp3.Call;

/**
 * Created by wanglei on 2016/12/9.
 */

public class NetApi {
    private static int reConnectNum = 0;

    public static void invokeGet(HashMap params, final Callback callback) {
        OkHttpUtils.get().url(AppKit.getServerUrl())
                .params(params == null ? new HashMap<String, String>() : params)
                .build()
                .execute(callback);
    }

    public static void invokeGet(HashMap params, final StringCallback callback) {
        OkHttpUtils.get().url(AppKit.getServerUrl())
                .params(params == null ? new HashMap<String, String>() : params)
                .build()
                .execute(callback);
    }
}
