package cn.droidlover.xdroid.demo.ui;

import android.os.Bundle;
import android.util.Log;
import android.view.View;

import com.zhy.http.okhttp.OkHttpUtils;

import java.util.HashMap;

import butterknife.BindView;
import cn.droidlover.qtcontentlayout.QTContentLayout;
import cn.droidlover.xdroid.base.SimpleRecAdapter;
import cn.droidlover.xdroid.base.XFragment;
import cn.droidlover.xdroid.demo.App;
import cn.droidlover.xdroid.demo.R;
import cn.droidlover.xdroid.demo.VideoManager;
import cn.droidlover.xdroid.demo.adapter.ShortVideoAdapter;
import cn.droidlover.xdroid.demo.kit.AppKit;
import cn.droidlover.xdroid.demo.model.MovieInfo;
import cn.droidlover.xdroid.demo.net.JsonCallback;
import cn.droidlover.xrecyclerview.RecyclerItemCallback;
import cn.droidlover.xrecyclerview.XRecyclerContentLayout;
import cn.droidlover.xrecyclerview.XRecyclerView;
import okhttp3.Call;

/**
 * Created by wanglei on 2016/12/10.
 */

public class ShortVideoFragment extends XFragment{

    ShortVideoAdapter mAdapter;


    @BindView(R.id.contentLayout)
    XRecyclerContentLayout contentLayout;

    protected static final int PAGE_SIZE = 10;
    protected static final int MAX_PAGE = 10;

    private int mType = 0;
    private boolean mIsInit = false;

    private JsonCallback<MovieInfo>  mCallback =   new JsonCallback<MovieInfo>() {
        @Override
        public void onFail(Call call, Exception e, int id) {
            contentLayout.setDisplayState(QTContentLayout.STATE_ERROR);
        }

        @Override
        public void onResponse(MovieInfo response, int id) {
           // if (!response.isError()) {
               // Log.i(App.TAG,"lzmlsfe getVideos is success");
                getAdapter().addData(response.getResults());

               // contentLayout.getRecyclerView().setPage(2, MAX_PAGE);

                    /*if (getAdapter().getItemCount() < 1) {
                        contentLayout.showEmpty();
                        return;
                    }*/
            //}
        }
    };

    @Override
    public void initData(Bundle savedInstanceState) {
        Log.i(App.TAG,"ShortVideoFragment initData");
        if(!mIsInit){
            mIsInit = true;
            initAdapter();
            VideoManager.getInstance().getVideos(mType,-1,mCallback);
        }
    }


    private void initAdapter() {
        XRecyclerView recyclerView = contentLayout.getRecyclerView();
        setLayoutManager(recyclerView);
        recyclerView.setAdapter(getAdapter());
        recyclerView.setOnRefreshAndLoadMoreListener(new XRecyclerView.OnRefreshAndLoadMoreListener() {
            @Override
            public void onRefresh() {
                VideoManager.getInstance().getVideos(mType,10,mCallback);
            }

            @Override
            public void onLoadMore(int page) {
                VideoManager.getInstance().getVideos(mType,10,mCallback);
            }
        });

        contentLayout.loadingView(View.inflate(getContext(), R.layout.view_loading, null));
        contentLayout.getRecyclerView().useDefLoadMoreView();
    }

    public void setType(int type){
        mType = type;
    }

    @Override
    public int getLayoutId() {
        return R.layout.fragment_base_pager;
    }

    public SimpleRecAdapter getAdapter() {
        if (mAdapter == null) {
            mAdapter = new ShortVideoAdapter(context);
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

    public String getType() {
        return "福利";
    }

    public static ShortVideoFragment newInstance() {
        return new ShortVideoFragment();
    }

    @Override
    public void setListener() {

    }
}
