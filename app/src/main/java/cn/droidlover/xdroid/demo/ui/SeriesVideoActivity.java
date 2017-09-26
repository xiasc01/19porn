package cn.droidlover.xdroid.demo.ui;

import android.content.Intent;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.os.Bundle;

import butterknife.BindView;
import cn.droidlover.xdroid.base.XActivity;
import cn.droidlover.xdroid.demo.R;

public class SeriesVideoActivity extends XActivity {

    private Fragment seriesVideoFragment;
    private FragmentManager fragmentManager;
    private FragmentTransaction transaction;

    @BindView(R.id.head_view)
    CommonActivityHeadView mHeadView;
    @Override
    public void initData(Bundle savedInstanceState) {
        Intent intent = getIntent();
        String seriesVideoName = intent.getStringExtra("SeriesVideoName");
        mHeadView.setTitle("合集："+ seriesVideoName);
        mHeadView.setTitleSize(13);
        mHeadView.setTitleMargins(80,0,40,0);
        mHeadView.setBackButtonMargins(3,3,0,0);

        fragmentManager = getSupportFragmentManager();
        transaction = fragmentManager.beginTransaction();
        seriesVideoFragment = new SeriesVideoFragment();
        ((SeriesVideoFragment)seriesVideoFragment).setSeriesName(seriesVideoName);
        transaction.replace(R.id.content, seriesVideoFragment);
        transaction.commit();
    }

    @Override
    public void setListener() {

    }

    @Override
    public int getLayoutId() {
        return R.layout.activity_series_video;
    }
}
