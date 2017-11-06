package cn.droidlover.xdroid.demo.ui;

import android.app.Activity;
import android.graphics.Color;
import android.nfc.Tag;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;

import com.zhy.http.okhttp.OkHttpUtils;

import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import butterknife.BindView;
import cn.droidlover.xdroid.base.XFragment;
import cn.droidlover.xdroid.base.XFragmentAdapter;
import cn.droidlover.xdroid.demo.App;
import cn.droidlover.xdroid.demo.R;
import cn.droidlover.xdroid.demo.User;
import cn.droidlover.xdroid.demo.VideoManager;
import cn.droidlover.xdroid.demo.kit.AppKit;
import cn.droidlover.xdroid.demo.net.JsonCallback;
import cn.droidlover.xdroid.demo.net.NetApi;
import okhttp3.Call;

/**
 * Created by Administrator on 2017/5/7 0007.
 */

public class HomeFragment extends XFragment {
    @BindView(R.id.tabLayout)
    TabLayout tabLayout;
    @BindView(R.id.viewPager)
    ViewPager viewPager;

    static List<Fragment> fragmentList = new ArrayList<>();
    static String[] titles ;
    private       XFragmentAdapter adapter;

    int           mRequestNum = 0;
    boolean       mIsInitChannel = false;

    class ChannelItem{
        public String channelName;
        public int    channelId;
    }
    class Channel{
        public ChannelItem[] channels;
    }

    @Override
    public void initData(Bundle savedInstanceState) {
        AppKit.setStatusBarColor(getActivity(),"#cc4444");
        if(!mIsInitChannel){
            initChannel();
            User.getInstance();
            mIsInitChannel = true;
        }

    }

    @Override
    public void setListener() {

    }

    @Override
    public int getLayoutId() {
        return R.layout.home_fragment;
    }

    private void initPageView(){
        FragmentActivity activity = getActivity();
        if(activity == null){
            return;
        }

        if (adapter == null) {
            adapter = new XFragmentAdapter(activity.getSupportFragmentManager(), fragmentList, titles);
        }

        viewPager.setOffscreenPageLimit(3);
        viewPager.setAdapter(adapter);

        tabLayout.setupWithViewPager(viewPager);
        tabLayout.setTabMode(TabLayout.MODE_SCROLLABLE);
    }

    private void initChannel(){
        Log.i(App.TAG,"init Channel start");
        if(fragmentList.size() == 0){
            HashMap<String, String> params = new HashMap<String, String>();
            params.put("request_type","fetch_channel");
            NetApi.invokeGet(params,callback);
        }else {
            initPageView();
        }
    }

    JsonCallback<Channel> callback = new JsonCallback<Channel>() {
        @Override
        public void onFail(Call call, Exception e, int id) {
            Log.e(App.TAG,"Channel onFail");
            e.printStackTrace();

            boolean isSocketTimeoutException = e instanceof SocketTimeoutException;
            if(isSocketTimeoutException){
                mRequestNum++;

                if(mRequestNum < 5){
                    AppKit.updateServerUrl();
                    initChannel();
                }else {
                    Log.e("19porn","网络超时 请稍后再试");
                }
            }else {
                Log.e("19porn","fetch_channel 服务器出现错误");
            }
        }

        @Override
        public void onResponse(Channel response, int id) {
            Log.i(App.TAG,"onResponse Channel");
            titles  = new String[response.channels.length];
            for(int i = 0;i < response.channels.length;i++){
                titles[i] = response.channels[i].channelName;
            }

            fragmentList.clear();

            for (int i = 0;i < response.channels.length;i++){
                ShortVideoFragment shortVideoFragment = ShortVideoFragment.newInstance();
                shortVideoFragment.setType(response.channels[i].channelId);
                fragmentList.add(shortVideoFragment);
            }
            initPageView();
        }
    };
}
