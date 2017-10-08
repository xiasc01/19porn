package cn.droidlover.xdroid.demo.ui.person.activity;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;

import butterknife.BindView;
import cn.droidlover.xdroid.base.XActivity;
import cn.droidlover.xdroid.demo.R;
import cn.droidlover.xdroid.demo.ui.CommonActivityHeadView;
import cn.droidlover.xdroid.demo.ui.EnshrineVideoFragment;
import cn.droidlover.xdroid.demo.ui.PlayHistoryFragment;

public class PlayHistoryActivity extends XActivity {

    private Fragment playHistoryFragment;
    private FragmentManager fragmentManager;
    private FragmentTransaction transaction;

    @BindView(R.id.head_view)
    CommonActivityHeadView mHeadView;
    @Override
    public void initData(Bundle savedInstanceState) {
        mHeadView.setTitle("播放历史");

        fragmentManager = getSupportFragmentManager();
        transaction = fragmentManager.beginTransaction();
        playHistoryFragment = new PlayHistoryFragment();
        transaction.replace(R.id.content, playHistoryFragment);
        transaction.commit();
    }

    @Override
    public void setListener() {

    }

    @Override
    public int getLayoutId() {
        return R.layout.activity_play_history;
    }
}
