package cn.droidlover.xdroid.demo.ui.person;

import android.os.Bundle;
import android.view.View;

import com.aplayer.aplayerandroid.Log;

import java.util.List;

import butterknife.BindView;
import cn.droidlover.qtcontentlayout.QTContentLayout;
import cn.droidlover.xdroid.base.SimpleRecAdapter;
import cn.droidlover.xdroid.base.XFragment;
import cn.droidlover.xdroid.demo.AccountManager;
import cn.droidlover.xdroid.demo.App;
import cn.droidlover.xdroid.demo.R;
import cn.droidlover.xdroid.demo.VideoManager;
import cn.droidlover.xdroid.demo.adapter.AccountDetailAdapter;
import cn.droidlover.xdroid.demo.adapter.ShortVideoAdapter;
import cn.droidlover.xdroid.demo.model.MovieInfo;
import cn.droidlover.xdroid.demo.net.JsonCallback;
import cn.droidlover.xrecyclerview.RecyclerItemCallback;
import cn.droidlover.xrecyclerview.XRecyclerContentLayout;
import cn.droidlover.xrecyclerview.XRecyclerView;
import okhttp3.Call;

/**
 * Created by lzmlsfe on 2017/10/9.
 */

public class AccountDetailFragment extends XFragment {
    AccountDetailAdapter mAdapter;

    @BindView(R.id.contentLayout)
    XRecyclerContentLayout contentLayout;

    private int mType = 0;


    private JsonCallback<AccountManager.ChargeDetail> mCallback =   new JsonCallback<AccountManager.ChargeDetail>() {
        @Override
        public void onFail(Call call, Exception e, int id) {
            contentLayout.setDisplayState(QTContentLayout.STATE_ERROR);
        }

        @Override
        public void onResponse(AccountManager.ChargeDetail response, int id) {
            if(response.isError) return;

            List<AccountManager.ChargeItem> chargeItems = response.chargeItems;


            Log.i(App.TAG,"AccountDetailFragment chargeItemSize = " + chargeItems.size());

            for(int i = 0;i < chargeItems.size();i++){
                AccountManager.ChargeItem chargeItem = chargeItems.get(i);
                getAdapter().addElement(0,chargeItem);
            }

            if(chargeItems.size() > 0){
                getAdapter().notifyItemRangeInserted(0, chargeItems.size());
            }

            getAdapter().notifyDataSetChanged();

        }
    };


    @Override
    public void initData(Bundle savedInstanceState) {
        XRecyclerView recyclerView = contentLayout.getRecyclerView();
        setLayoutManager(recyclerView);
        recyclerView.setAdapter(getAdapter());

        AccountManager.getChargeDetail(mCallback);

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

    public void setType(int type){
        mType = type;
    }

    @Override
    public int getLayoutId() {
        return R.layout.fragment_base_pager;
    }

    public SimpleRecAdapter getAdapter() {
        if (mAdapter == null) {
            mAdapter = new AccountDetailAdapter(context);
            mAdapter.setRecItemClick(new RecyclerItemCallback<AccountManager.ChargeItem, AccountDetailAdapter.ViewHolder>() {
                @Override
                public void onItemClick(int position, AccountManager.ChargeItem model, int tag, AccountDetailAdapter.ViewHolder holder) {
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
