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
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import butterknife.BindView;
import cn.droidlover.xdroid.base.XFragment;
import cn.droidlover.xdroid.demo.R;
import cn.droidlover.xdroid.demo.User;
import cn.droidlover.xdroid.demo.kit.AppKit;
import cn.droidlover.xdroid.demo.ui.PersonItem;
import cn.droidlover.xdroid.demo.ui.person.activity.ChargeActivity;
import cn.droidlover.xdroid.demo.ui.person.activity.InvitationActivity;
import cn.droidlover.xdroid.demo.ui.person.activity.PersonInfoActivity;

import static android.net.wifi.SupplicantState.COMPLETED;


/**
 * Created by Administrator on 2017/5/7 0007.
 */

public class PersonFragment extends XFragment implements View.OnClickListener{
    @BindView(R.id.portrait)
    ImageView mPortrait;

    @BindView(R.id.person_info)
    View mPersonInfo;

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

    @BindView(R.id.userId)
    TextView mUserId;

    @BindView(R.id.userName)
    TextView mUserName;

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

        setPortrait();

        mPersonInfo.setOnClickListener(this);
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

        if(intent != null){
            context.startActivityForResult(intent,1);
        }

    }
}
