package cn.droidlover.xdroid.demo.ui.person.activity;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import butterknife.BindView;
import cn.droidlover.xdroid.base.XActivity;
import cn.droidlover.xdroid.demo.R;
import cn.droidlover.xdroid.demo.kit.AppKit;
import cn.droidlover.xdroid.demo.ui.CommonActivityHeadView;
import cn.droidlover.xdroid.demo.ui.HomeFragment;
import cn.droidlover.xdroid.demo.ui.person.AccountFragment;

/**
 * Created by lzmlsfe on 2017/7/26.
 */

public class AccountActivity extends XActivity {
    @BindView(R.id.head_view)
    CommonActivityHeadView mHeadView;

    private FragmentManager fragmentManager;
    private FragmentTransaction transaction;
    private Fragment            accountFragment;

    @Override
    public void initData(Bundle savedInstanceState) {
        mHeadView.setTitle("交易记录");

        fragmentManager = getSupportFragmentManager();
        transaction = fragmentManager.beginTransaction();
        accountFragment = new AccountFragment();
        transaction.replace(R.id.content, accountFragment);
        transaction.commit();
    }

    @Override
    public void setListener() {

    }

    @Override
    public int getLayoutId() {
        return R.layout.activity_account;
    }
}
