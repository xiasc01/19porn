package cn.droidlover.xdroid.demo.ui;

import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import com.zhy.http.okhttp.OkHttpUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import butterknife.BindView;
import cn.droidlover.xdroid.base.XActivity;
import cn.droidlover.xdroid.base.XFragmentAdapter;
import cn.droidlover.xdroid.demo.R;
import cn.droidlover.xdroid.demo.User;
import cn.droidlover.xdroid.demo.net.JsonCallback;
import okhttp3.Call;

/**
 * Created by wanglei on 2016/11/29.
 */



public class MainActivity extends XActivity {

    /*@BindView(R.id.toolbar)
    Toolbar toolbar;*/
    @BindView(R.id.tabLayout)
    TabLayout tabLayout;
    @BindView(R.id.viewPager)
    ViewPager viewPager;

    @BindView(R.id.imageButtonMainHome)
    ImageButton ibtnMainHome;
    @BindView(R.id.tvMainHome)
    TextView  tvMainHome;
    @BindView(R.id.layoutMainHome)
    View layoutMainHome;

    @BindView(R.id.imageButtonPersonalHome)
    ImageButton ibtnPersonalHome;
    @BindView(R.id.textViewPersonHome)
    TextView tvPersonHome;
    @BindView(R.id.layoutPersonalHome)
    View layoutPersonalHome;


    @BindView(R.id.mainHome)
    View viewMainHome;
    @BindView(R.id.personHome)
    View personHome;

    PageSwitch mPageSwitch;

    List<Fragment> fragmentList = new ArrayList<>();
    List<Fragment> fragmentList2 = new ArrayList<>();
    String[] titles = {"首页", "干货", "妹子"};
    String[] titles2 = {"首页", "收藏", "我的"};
    XFragmentAdapter adapter;
    XFragmentAdapter adapter2;

    final Object lock = new Object();

    class Channel{
        private String[] channels;
    }

    @Override
    public void initData(Bundle savedInstanceState) {

        User user = User.getInstance();

        //setSupportActionBar(toolbar);
        String url = "http://123.56.65.245/19porn.php";
        JsonCallback<Channel> callback = new JsonCallback<Channel>(1 * 60 * 60 * 1000) {
            @Override
            public void onFail(Call call, Exception e, int id) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Channel response, int id) {
                titles = response.channels;

                fragmentList.clear();

                for (int i = 0;i < titles.length;i++){
                    fragmentList.add(ShortVideoFragment.newInstance());
                }

                if (adapter == null) {
                    adapter = new XFragmentAdapter(getSupportFragmentManager(), fragmentList, titles);
                }

                viewPager.setOffscreenPageLimit(3);
                viewPager.setAdapter(adapter);

                tabLayout.setupWithViewPager(viewPager);
                tabLayout.setTabMode(TabLayout.MODE_SCROLLABLE);

            }
        };

        HashMap<String, String> params = new HashMap<String, String>();
        params.put("request_type","fetch_channel");

        OkHttpUtils.get().url(url)
                .params(params)
                .build()
                .execute(callback);

        mPageSwitch = new PageSwitch();
        layoutMainHome.setOnClickListener(mPageSwitch);
        layoutPersonalHome.setOnClickListener(mPageSwitch);
    }




    public class PageSwitch implements View.OnClickListener{
        private View        lastView   = viewMainHome;
        private ImageButton lastBtn    = ibtnMainHome;
        private TextView    lastTV     = tvMainHome;
        private int  lastBtnNor = R.mipmap.ic_home_main_normal;
        private int  lastBtnSel = R.mipmap.ic_home_main_selectd;

        @Override
        public void onClick(View view) {

            lastView.setVisibility(View.GONE);
            lastBtn.setImageResource(lastBtnNor);
            lastTV.setTextColor(0xff000000);

            if(view == layoutPersonalHome){
                lastView    = personHome;
                lastBtn     = ibtnPersonalHome;
                lastTV      = tvPersonHome;
                lastBtnSel  = R.mipmap.ic_home_person_selected;
                lastBtnNor  = R.mipmap.ic_home_person_normal;

            }else if(view == layoutMainHome){
                lastView    = viewMainHome;
                lastBtn     = ibtnMainHome;
                lastTV      = tvMainHome;
                lastBtnSel  = R.mipmap.ic_home_main_selectd;
                lastBtnNor  = R.mipmap.ic_home_main_normal;
            }

            lastView.setVisibility(View.VISIBLE);
            lastTV.setTextColor(0xffff0000);
            lastBtn.setImageResource(lastBtnSel);
        }
    }


    View.OnClickListener mainHomeListener = new View.OnClickListener(){
        @Override
        public void onClick(View view) {
            viewMainHome.setVisibility(View.VISIBLE);
            personHome.setVisibility(View.GONE);

            tvMainHome.setTextColor(0xffff0000);
            ibtnMainHome.setImageResource(R.mipmap.ic_home_person_selected);
        }
    };

    View.OnClickListener personalHomeListener = new View.OnClickListener(){
        @Override
        public void onClick(View view) {
            personHome.setVisibility(View.VISIBLE);
            viewMainHome.setVisibility(View.GONE);
            //tvPersonHome.setTextColor(0xffff0000);
        }
    };

    @Override
    public void setListener() {

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_droid:
                AboutActivity.launch(context);
                break;
        }
        return super.onOptionsItemSelected(item);
    }


    @Override
    public int getLayoutId() {
        return R.layout.activity_main;
    }

}
