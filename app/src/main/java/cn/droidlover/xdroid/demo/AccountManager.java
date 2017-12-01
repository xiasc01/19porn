package cn.droidlover.xdroid.demo;

import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import cn.droidlover.xdroid.demo.kit.AppKit;
import cn.droidlover.xdroid.demo.model.MovieInfo;
import cn.droidlover.xdroid.demo.net.JsonCallback;
import cn.droidlover.xdroid.demo.net.NetApi;
import okhttp3.Call;

/**
 * Created by lzmlsfe on 2017/12/1.
 */

public class AccountManager {
    private static int mReConnectNum = 0;

    public class ChargeItem{
        public String mOderId;
        public String mTime;
        public String mCoin;
        public String mUnPayAmount;
    }

    public class ChargeDetail{
        public boolean  isError = true;
        public List<ChargeItem> chargeItems;
    }

    public static void getChargeDetail(final JsonCallback<ChargeDetail> callback){
        final JsonCallback<ChargeDetail> localCallback =  new JsonCallback<ChargeDetail>() {
            @Override
            public void onFail(Call call, Exception e, int id) {
                if(e instanceof SocketTimeoutException && mReConnectNum < 5){
                    mReConnectNum++;
                    AppKit.updateServerUrl();
                    getChargeDetail(callback);
                }else {
                    callback.onFail(call,e,id);
                    mReConnectNum = 0;
                }
            }

            @Override
            public void onResponse(ChargeDetail response, int id) {
                if(response != null && !response.isError){
                    callback.onResponse(response,id);
                }
                mReConnectNum = 0;
            }
        };


        HashMap<String, String> params = new HashMap<String, String>();
        params.put("request_type","fetch_charge_info");
        //params.put("user_id",User.getInstance().getUserId());
        params.put("user_id","pxme6n");

        NetApi.invokeGet(params,localCallback);
    }
}
