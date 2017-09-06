package cn.droidlover.xdroid.demo.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.TextView;

import butterknife.BindView;
import cn.droidlover.xdroid.base.XActivity;
import cn.droidlover.xdroid.demo.R;
import cn.droidlover.xdroid.demo.VideoManager;
import cn.droidlover.xdroid.demo.adapter.ShortVideoAdapter;
import cn.droidlover.xdroid.demo.model.MovieInfo;

public class MovieInfoActivity extends XActivity {
    @BindView(R.id.IDEdit)
    TextView IDEdit;

    @Override
    public void initData(Bundle savedInstanceState) {
        Intent intent = getIntent();
        String movieID = intent.getStringExtra("MovieID");
        MovieInfo.Item item = VideoManager.getInstance().getMovieInfoItem(movieID);
        if(item == null){
            return;
        }
        IDEdit.setText(item.getMovie_id());
    }

    @Override
    public void setListener() {

    }

    @Override
    public int getLayoutId() {
        return R.layout.activity_movie_info;
    }
}
