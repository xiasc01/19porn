package cn.droidlover.xdroid.demo.ui;

import android.os.Bundle;
import android.view.View;

import butterknife.BindView;
import cn.droidlover.qtcontentlayout.QTContentLayout;
import cn.droidlover.xdroid.base.SimpleRecAdapter;
import cn.droidlover.xdroid.base.XFragment;
import cn.droidlover.xdroid.demo.R;
import cn.droidlover.xdroid.demo.model.MovieInfo;
import cn.droidlover.xdroid.demo.net.JsonCallback;
import cn.droidlover.xdroid.demo.net.NetApi;
import cn.droidlover.xrecyclerview.XRecyclerContentLayout;
import cn.droidlover.xrecyclerview.XRecyclerView;
import okhttp3.Call;

/**
 * Created by wanglei on 2016/12/10.
 */

public abstract class BasePagerFragment extends XFragment {

    @BindView(R.id.contentLayout)
    XRecyclerContentLayout contentLayout;

    protected static final int PAGE_SIZE = 10;
    protected static final int MAX_PAGE = 10;


    @Override
    public void initData(Bundle savedInstanceState) {
        initAdapter();
        loadData(1);
    }

    @Override
    public void setListener() {

    }

    private void initAdapter() {
        XRecyclerView recyclerView = contentLayout.getRecyclerView();
        setLayoutManager(recyclerView);
        recyclerView.setAdapter(getAdapter());
        recyclerView.setOnRefreshAndLoadMoreListener(new XRecyclerView.OnRefreshAndLoadMoreListener() {
                    @Override
                    public void onRefresh() {
                        loadData(1);
                    }

                    @Override
                    public void onLoadMore(int page) {
                        loadData(page);
                    }
                });

        contentLayout.loadingView(View.inflate(getContext(), R.layout.view_loading, null));
        contentLayout.getRecyclerView().useDefLoadMoreView();
    }

    public void loadData(final int page) {
        /*NetApi.getGankData(getType(), PAGE_SIZE, page, new JsonCallback<MovieInfo>(1 * 60 * 60 * 1000) {
            @Override
            public void onFail(Call call, Exception e, int id) {
                contentLayout.setDisplayState(QTContentLayout.STATE_ERROR);
            }

            @Override
            public void onResponse(MovieInfo response, int id) {
                if (!response.isError()) {
                    if (page > 1) {
                        getAdapter().addData(response.getResults());
                    } else {
                        getAdapter().setData(response.getResults());
                    }

                    contentLayout.getRecyclerView().setPage(page, MAX_PAGE);

                    if (getAdapter().getItemCount() < 1) {
                        contentLayout.showEmpty();
                        return;
                    }

                }
            }
        });*/
    }

    public abstract SimpleRecAdapter getAdapter();

    public abstract void setLayoutManager(XRecyclerView recyclerView);

    public abstract String getType();


    @Override
    public int getLayoutId() {
        return R.layout.fragment_base_pager;
    }

}
