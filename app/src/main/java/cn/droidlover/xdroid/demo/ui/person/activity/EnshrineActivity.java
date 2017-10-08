package cn.droidlover.xdroid.demo.ui.person.activity;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import butterknife.BindView;
import cn.droidlover.xdroid.base.XActivity;
import cn.droidlover.xdroid.demo.R;
import cn.droidlover.xdroid.demo.ui.CommonActivityHeadView;
import cn.droidlover.xdroid.demo.ui.EnshrineVideoFragment;
import cn.droidlover.xdroid.demo.ui.SeriesVideoFragment;

public class EnshrineActivity extends XActivity {

    private Fragment enshrineVideoFragment;
    private FragmentManager fragmentManager;
    private FragmentTransaction transaction;

    @BindView(R.id.head_view)
    CommonActivityHeadView mHeadView;
    @Override
    public void initData(Bundle savedInstanceState) {
        mHeadView.setTitle("收藏");

        fragmentManager = getSupportFragmentManager();
        transaction = fragmentManager.beginTransaction();
        enshrineVideoFragment = new EnshrineVideoFragment();
        transaction.replace(R.id.content, enshrineVideoFragment);
        transaction.commit();
    }

    @Override
    public void setListener() {

    }

    @Override
    public int getLayoutId() {
        return R.layout.activity_enshrine;
    }
}
