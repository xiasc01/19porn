package cn.droidlover.xdroid.demo.ui.person;

import android.os.Bundle;
import android.view.View;

import butterknife.BindView;
import cn.droidlover.xdroid.base.SimpleRecAdapter;
import cn.droidlover.xdroid.base.XFragment;
import cn.droidlover.xdroid.demo.R;
import cn.droidlover.xdroid.demo.VideoManager;
import cn.droidlover.xdroid.demo.adapter.ShortVideoAdapter;
import cn.droidlover.xdroid.demo.model.MovieInfo;
import cn.droidlover.xrecyclerview.RecyclerItemCallback;
import cn.droidlover.xrecyclerview.XRecyclerContentLayout;
import cn.droidlover.xrecyclerview.XRecyclerView;

/**
 * Created by lzmlsfe on 2017/10/9.
 */

public class AccountDetailFragment extends XFragment {
    ShortVideoAdapter mAdapter;

    @BindView(R.id.contentLayout)
    XRecyclerContentLayout contentLayout;

    @Override
    public void initData(Bundle savedInstanceState) {
        XRecyclerView recyclerView = contentLayout.getRecyclerView();
        setLayoutManager(recyclerView);
        recyclerView.setAdapter(getAdapter());
        recyclerView.setOnRefreshAndLoadMoreListener(new XRecyclerView.OnRefreshAndLoadMoreListener() {
            @Override
            public void onRefresh() {
            }

            @Override
            public void onLoadMore(int page) {
            }
        });

        contentLayout.loadingView(View.inflate(getContext(), R.layout.view_loading, null));
        contentLayout.getRecyclerView().useDefLoadMoreView();
    }

    @Override
    public void setListener() {

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
}
