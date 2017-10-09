package cn.droidlover.xdroid.demo.ui.person;

import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import cn.droidlover.xdroid.base.XFragment;
import cn.droidlover.xdroid.base.XFragmentAdapter;
import cn.droidlover.xdroid.demo.R;

/**
 * Created by lzmlsfe on 2017/10/9.
 */

public class AccountFragment extends XFragment {
    @BindView(R.id.tabLayout)
    TabLayout tabLayout;
    @BindView(R.id.viewPager)
    ViewPager viewPager;

    private       List<Fragment> fragmentList = new ArrayList<>();
    private       String[] titles  = {"交易详情","充值","奖励"};
    private       XFragmentAdapter adapter;


    @Override
    public void initData(Bundle savedInstanceState) {
        FragmentActivity activity = getActivity();
        if(activity == null){
            return;
        }

        for (int i = 0;i < 3;i++){
            Fragment fragment = new AccountDetailFragment();
            fragmentList.add(fragment);
        }

        if (adapter == null) {
            adapter = new XFragmentAdapter(activity.getSupportFragmentManager(), fragmentList, titles);
        }

        viewPager.setOffscreenPageLimit(3);
        viewPager.setAdapter(adapter);

        tabLayout.setupWithViewPager(viewPager);
        tabLayout.setTabMode(TabLayout.MODE_FIXED);
    }

    @Override
    public void setListener() {

    }

    @Override
    public int getLayoutId() {
        return R.layout.fragment_account;
    }
}
