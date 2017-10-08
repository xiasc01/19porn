package cn.droidlover.xdroid.demo.adapter;

import android.content.Context;
import android.view.View;

import cn.droidlover.xdroid.base.SimpleRecAdapter;
import cn.droidlover.xdroid.demo.model.MovieInfo;

/**
 * Created by Wyc on 2017/10/1 0001.
 */

public class PlayHistoryAdapter  extends SimpleRecAdapter<MovieInfo.Item, ShortVideoAdapter.ViewHolder> {
    public PlayHistoryAdapter(Context context) {
        super(context);
    }

    @Override
    public ShortVideoAdapter.ViewHolder newViewHolder(View itemView) {
        return null;
    }

    @Override
    public int getLayoutId() {
        return 0;
    }

    @Override
    public void onBindViewHolder(ShortVideoAdapter.ViewHolder holder, int position) {

    }
}
