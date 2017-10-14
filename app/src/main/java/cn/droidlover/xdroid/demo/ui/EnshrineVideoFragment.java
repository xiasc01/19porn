package cn.droidlover.xdroid.demo.ui;

import android.os.Bundle;
import android.util.Log;
import android.view.View;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import cn.droidlover.qtcontentlayout.QTContentLayout;
import cn.droidlover.xdroid.base.SimpleRecAdapter;
import cn.droidlover.xdroid.base.XFragment;
import cn.droidlover.xdroid.demo.App;
import cn.droidlover.xdroid.demo.R;
import cn.droidlover.xdroid.demo.VideoManager;
import cn.droidlover.xdroid.demo.adapter.ShortVideoAdapter;
import cn.droidlover.xdroid.demo.model.MovieInfo;
import cn.droidlover.xdroid.demo.net.JsonCallback;
import cn.droidlover.xrecyclerview.RecyclerItemCallback;
import cn.droidlover.xrecyclerview.XRecyclerContentLayout;
import cn.droidlover.xrecyclerview.XRecyclerView;
import okhttp3.Call;

/**
 * Created by Administrator on 2017/9/23 0023.
 */

public class EnshrineVideoFragment  extends XFragment {
    ShortVideoAdapter mAdapter;

    @BindView(R.id.contentLayout)
    XRecyclerContentLayout contentLayout;
    private boolean mIsInit = false;

    private JsonCallback<MovieInfo> mCallback =   new JsonCallback<MovieInfo>() {
        @Override
        public void onFail(Call call, Exception e, int id) {
            contentLayout.setDisplayState(QTContentLayout.STATE_ERROR);
        }

        @Override
        public void onResponse(MovieInfo response, int id) {
            List<MovieInfo.Item> movies = response.getResults();

            for(int i = 0;i < movies.size();i++){
                MovieInfo.Item movieItem = movies.get(i);
                getAdapter().addElement(0,movieItem);
            }

            if(movies.size() > 0){
                getAdapter().notifyItemRangeInserted(0, movies.size());
            }

            getAdapter().notifyDataSetChanged();
        }
    };

    @Override
    public void initData(Bundle savedInstanceState) {
        Log.i(App.TAG,"SeriesVideoFragment initData");
        if(!mIsInit){
            mIsInit = true;
            initAdapter();
            VideoManager.getInstance().getEnShrineVideos(mCallback);
        }
    }


    private void initAdapter() {
        XRecyclerView recyclerView = contentLayout.getRecyclerView();
        setLayoutManager(recyclerView);
        recyclerView.setAdapter(getAdapter());
        recyclerView.setOnRefreshAndLoadMoreListener(new XRecyclerView.OnRefreshAndLoadMoreListener() {
            @Override
            public void onRefresh() {
                //VideoManager.getInstance().getSeriesVideos(mSeriesName,maxId,mCallback);
                getAdapter().notifyDataSetChanged();
            }

            @Override
            public void onLoadMore(int page) {
                //VideoManager.getInstance().getSeriesVideos(mSeriesName,maxId,mCallback);
                getAdapter().notifyDataSetChanged();
            }
        });

        contentLayout.loadingView(View.inflate(getContext(), R.layout.view_loading, null));
        contentLayout.getRecyclerView().useDefLoadMoreView();
    }

    @Override
    public int getLayoutId() {
        return R.layout.fragment_base_pager;
    }

    public SimpleRecAdapter getAdapter() {
        if (mAdapter == null) {
            mAdapter = new ShortVideoAdapter(context);
            ((ShortVideoAdapter)mAdapter).setIsSeriesVideoAdapter(true);
            mAdapter.setRecItemClick(new RecyclerItemCallback<MovieInfo.Item, ShortVideoAdapter.ViewHolder>() {
                @Override
                public void onItemClick(int position, MovieInfo.Item model, int tag, ShortVideoAdapter.ViewHolder holder) {
                    super.onItemClick(position, model, tag, holder);
                }
            });
        }
        return mAdapter;
    }

    public void setLayoutManager(XRecyclerView recyclerView) {
        recyclerView.gridLayoutManager(context, 1);
    }

    @Override
    public void setListener() {

    }
}
