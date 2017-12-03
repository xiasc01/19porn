package cn.droidlover.xdroid.demo;

import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import cn.droidlover.xdroid.demo.kit.AppKit;
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
        public int mCoin;
        public float mUnPayAmount;
    }

    public class ChargeDetail{
        public boolean  isError = true;
        public List<ChargeItem> chargeItems;
    }

    public class AccountItem{
        public String mTime;
        public String mType;
        public String mName;
        public int mSpare;
        public int mCoin;
    }

    public class PlayItem{
        public String mTime;
        public String mMovieId;
        public int mCoin;
    }

    public class PlayDetail{
        public boolean  isError = true;
        public List<PlayItem> playItems;
    }

    public class InvitationItem{
        public String mTime;
        public int mCoin;
        public String mToUser;
    }

    public class InvitationDetail{
        public boolean  isError = true;
        public List<InvitationItem> invitationItems;
    }

    public class AccountDetail{
        public boolean  isError = true;
        public List<InvitationItem> invitationItems;
        public List<ChargeItem> chargeItems;
        public List<PlayItem> playItems;
        public List<AccountItem> accountItems;
    }

    public static void getInvitationDetail(final JsonCallback<InvitationDetail> callback){
        final JsonCallback<InvitationDetail> localCallback =  new JsonCallback<InvitationDetail>() {
            @Override
            public void onFail(Call call, Exception e, int id) {
                if(e instanceof SocketTimeoutException && mReConnectNum < 5){
                    mReConnectNum++;
                    AppKit.updateServerUrl();
                    getInvitationDetail(callback);
                }else {
                    callback.onFail(call,e,id);
                    mReConnectNum = 0;
                }
            }

            @Override
            public void onResponse(InvitationDetail response, int id) {
                if(response != null && !response.isError){
                    callback.onResponse(response,id);
                }
                mReConnectNum = 0;
            }
        };


        HashMap<String, String> params = new HashMap<String, String>();
        params.put("request_type","fetch_invitation_info");
        //params.put("user_id",User.getInstance().getUserId());
        params.put("user_id","pxme6n");

        NetApi.invokeGet(params,localCallback);
    }

    public static void getPlayDetail(final JsonCallback<PlayDetail> callback){
        final JsonCallback<PlayDetail> localCallback =  new JsonCallback<PlayDetail>() {
            @Override
            public void onFail(Call call, Exception e, int id) {
                if(e instanceof SocketTimeoutException && mReConnectNum < 5){
                    mReConnectNum++;
                    AppKit.updateServerUrl();
                    getPlayDetail(callback);
                }else {
                    callback.onFail(call,e,id);
                    mReConnectNum = 0;
                }
            }

            @Override
            public void onResponse(PlayDetail response, int id) {
                if(response != null && !response.isError){
                    callback.onResponse(response,id);
                }
                mReConnectNum = 0;
            }
        };


        HashMap<String, String> params = new HashMap<String, String>();
        params.put("request_type","fetch_play_info");
        //params.put("user_id",User.getInstance().getUserId());
        params.put("user_id","pxme6n");

        NetApi.invokeGet(params,localCallback);
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

    public static void getAccountDetail(final JsonCallback<AccountDetail> callback){
        final JsonCallback<AccountDetail> localCallback =  new JsonCallback<AccountDetail>() {
            @Override
            public void onFail(Call call, Exception e, int id) {
                if(e instanceof SocketTimeoutException && mReConnectNum < 5){
                    mReConnectNum++;
                    AppKit.updateServerUrl();
                    getAccountDetail(callback);
                }else {
                    callback.onFail(call,e,id);
                    mReConnectNum = 0;
                }
            }

            @Override
            public void onResponse(AccountDetail response, int id) {
                if(response != null && !response.isError){
                    int size = response.playItems.size() + response.chargeItems.size() + response.invitationItems.size();
                    int playPos = 0,chargePos = 0,invitationPos = 0;
                    String  invitationTime,playTime,chargeTime;
                    response.accountItems = new ArrayList<AccountItem>(size);

                    int spare = 0;

                    for (int i = 0;i < size;i++){
                        int f = 0;
                        if(response.invitationItems.size() > invitationPos){
                            invitationTime = response.invitationItems.get(invitationPos).mTime;
                        }else{
                            invitationTime = "9999-99-99 99:99:99";
                        }

                        if(response.playItems.size() > playPos){
                            playTime       = response.playItems.get(playPos).mTime;
                        }else{
                            playTime = "9999-99-99 99:99:99";
                        }

                        if(response.chargeItems.size() > chargePos){
                            chargeTime     = response.chargeItems.get(chargePos).mTime;
                        }else{
                            chargeTime = "9999-99-99 99:99:99";
                        }

                        String minTime = null;
                        if(invitationTime.compareTo(playTime) < 0){
                            minTime = invitationTime;
                            f = 0;
                        }else{
                            minTime = playTime;
                            f = 1;
                        }

                        if(minTime.compareTo(chargeTime) > 0){
                            minTime = chargeTime;
                            f = 2;
                        }

                        AccountItem accountItem = new AccountManager().new AccountItem();
                        if(f == 0){
                            accountItem.mCoin = response.invitationItems.get(invitationPos).mCoin;
                            spare  += accountItem.mCoin;
                            accountItem.mSpare = spare;
                            accountItem.mTime = minTime;
                            accountItem.mType = "推荐奖励";
                            invitationPos++;
                        }
                        if(f == 1){
                            accountItem.mCoin = response.playItems.get(playPos).mCoin;
                            spare -= accountItem.mCoin;
                            accountItem.mSpare = spare;
                            accountItem.mTime = minTime;
                            accountItem.mType = "播放";
                            accountItem.mName = response.playItems.get(playPos).mMovieId;
                            playPos++;
                        }
                        if(f == 2){
                            accountItem.mCoin = response.chargeItems.get(chargePos).mCoin;
                            spare  += accountItem.mCoin;
                            accountItem.mSpare += spare;
                            accountItem.mTime = minTime;
                            accountItem.mType = "充值";
                            chargePos++;
                        }
                        response.accountItems.add(accountItem);
                    }
                    callback.onResponse(response,id);
                }
                mReConnectNum = 0;
            }
        };


        HashMap<String, String> params = new HashMap<String, String>();
        params.put("request_type","fetch_account_info");
        //params.put("user_id",User.getInstance().getUserId());
        params.put("user_id","pxme6n");

        NetApi.invokeGet(params,localCallback);
    }
}
