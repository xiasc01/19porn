package cn.droidlover.xdroid.demo.ui.person;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import com.zhy.http.okhttp.callback.StringCallback;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;

import butterknife.BindView;
import cn.droidlover.xdroid.base.XFragment;
import cn.droidlover.xdroid.demo.App;
import cn.droidlover.xdroid.demo.R;
import cn.droidlover.xdroid.demo.User;
import cn.droidlover.xdroid.demo.kit.AppKit;
import cn.droidlover.xdroid.demo.ui.PersonItem;
import cn.droidlover.xdroid.demo.ui.person.activity.AccountActivity;
import cn.droidlover.xdroid.demo.ui.person.activity.ChargeActivity;
import cn.droidlover.xdroid.demo.ui.person.activity.EnshrineActivity;
import cn.droidlover.xdroid.demo.ui.person.activity.InvitationActivity;
import cn.droidlover.xdroid.demo.ui.person.activity.PersonInfoActivity;
import cn.droidlover.xdroid.demo.ui.person.activity.PlayHistoryActivity;
import okhttp3.Call;

import static android.net.wifi.SupplicantState.COMPLETED;


/**
 * Created by Administrator on 2017/5/7 0007.
 */

public class PersonFragment extends XFragment implements View.OnClickListener{
    @BindView(R.id.portrait)
    ImageView mPortrait;

    @BindView(R.id.person_info)
    View mPersonInfo;

    @BindView(R.id.userId)
    TextView mUserId;

    @BindView(R.id.userName)
    TextView mUserName;

    @BindView(R.id.charge)
    PersonItem mCharge;

    @BindView(R.id.invitation)
    PersonItem mInvitation;

    @BindView(R.id.account)
    PersonItem mAccount;

    @BindView(R.id.enshrine)
    PersonItem mEnshrine;

    @BindView(R.id.play_history)
    PersonItem mPlayHistory;

    @BindView(R.id.message)
    PersonItem mMessage;

    @BindView(R.id.setting)
    PersonItem mSetting;

    Bitmap pngBM = null;
    Object lock = new Object();

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 0) {
                mPortrait.setImageBitmap(pngBM);
            }
        }
    };

    class CoinStringCallback extends StringCallback{
        public int mType = 0;

        public CoinStringCallback(int type){
            mType = type;
        }
        @Override
        public void onError(Call call, Exception e, int id) {
            e.printStackTrace();
        }

        @Override
        public void onResponse(String response, int id) {
            if(mType == 0){
                mAccount.setItemValue(response);
                User.getInstance().setCoin(response);
            }else{
                int unVerifyCoin = 0;
                try {
                    unVerifyCoin = Integer.parseInt(response);
                }catch (Exception e){
                    unVerifyCoin = 0;
                }
                if(unVerifyCoin != 0){
                    mAccount.setItemValue2("(" + response + ")");
                }else{
                    mAccount.setItemValue2(null);
                }
                User.getInstance().setUnVerifyCoin(response);
            }
        }
    }

    @Override
    public void initData(Bundle savedInstanceState) {

        AppKit.setStatusBarColor(getActivity(),"#000000");


        mCharge.setItemName("充值");
        mCharge.setOnClickListener(this);

        mInvitation.setItemName("邀请");
        mInvitation.setOnClickListener(this);

        mAccount.setItemName("账户");
        mAccount.setOnClickListener(this);
        mAccount.setLineVisible(View.INVISIBLE);
        mAccount.setItemValue(User.getInstance().getCoin(null));
        int unVerifyCoin = 0;
        try {
            unVerifyCoin = Integer.parseInt(User.getInstance().getUnVerifyCoin(null));
        }catch (Exception e){
            unVerifyCoin = 0;
        }

        if(unVerifyCoin != 0){
            mAccount.setItemValue2("(" + unVerifyCoin + ")");
        }else{
            mAccount.setItemValue2(null);
        }
        mAccount.setItemValue2Image(R.mipmap.pay_diamond);
        getNewAccountInfo();





        mPlayHistory.setItemName("播放历史");
        mPlayHistory.setOnClickListener(this);

        mEnshrine.setItemName("收藏");
        mEnshrine.setOnClickListener(this);
        mEnshrine.setLineVisible(View.INVISIBLE);


        mMessage.setItemName("消息");
        mMessage.setOnClickListener(this);

        mSetting.setItemName("设置");
        mSetting.setOnClickListener(this);
        mSetting.setLineVisible(View.INVISIBLE);

        mPersonInfo.setOnClickListener(this);
        initUserData();
    }

    public void initUserData(){
        setPortrait();

        mUserName.setText(User.getInstance().getUserName());
        mUserId.setText(User.getInstance().getUserId());
        getNewAccountInfo();
    }

    public void getNewAccountInfo(){
        User.getInstance().getCoin(new CoinStringCallback(0));
        User.getInstance().getUnVerifyCoin(new CoinStringCallback(1));
    }

    public void setPortrait(){

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    pngBM = User.getInstance().getUserPortrait();

                    Message msg = new Message();
                    msg.what = 0;
                    handler.sendMessage(msg);

                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        });
        thread.start();

    }

    @Override
    public void setListener() {

    }

    @Override
    public int getLayoutId() {
        return R.layout.person_fragment;
    }




    @Override
    public void onClick(View v) {
        Intent intent = null;
        if(v.getId() == R.id.person_info){
            intent = new Intent((Activity)context, PersonInfoActivity.class);
        }
        if(v.getId() == R.id.charge){
            intent = new Intent((Activity)context, ChargeActivity.class);
        }

        if(v.getId() == R.id.invitation){
            intent = new Intent((Activity)context, InvitationActivity.class);
        }

        if(v.getId() == R.id.account){
            intent = new Intent((Activity)context, AccountActivity.class);
        }

        if(v.getId() == R.id.enshrine){
            intent = new Intent((Activity)context, EnshrineActivity.class);
        }

        if(v.getId() == R.id.play_history){
            intent = new Intent((Activity)context, PlayHistoryActivity.class);
        }

        if(intent != null){
            context.startActivityForResult(intent,1);
        }

    }
}
