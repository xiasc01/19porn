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
import cn.droidlover.xdroid.demo.adapter.AccountDetailAdapter;
import cn.droidlover.xdroid.demo.adapter.AwardDetailAdapter;
import cn.droidlover.xdroid.demo.adapter.ChargeDetailAdapter;
import cn.droidlover.xdroid.demo.net.JsonCallback;
import cn.droidlover.xrecyclerview.RecyclerItemCallback;
import cn.droidlover.xrecyclerview.XRecyclerContentLayout;
import cn.droidlover.xrecyclerview.XRecyclerView;
import okhttp3.Call;

/**
 * Created by lzmlsfe on 2017/10/9.
 */

public class AccountDetailFragment extends XFragment {
    ChargeDetailAdapter  mChargeAdapter;
    AccountDetailAdapter mAccountAdapter;
    AwardDetailAdapter   mAwardDetailAdapter;

    @BindView(R.id.contentLayout)
    XRecyclerContentLayout contentLayout;

    private int mType = 0;


    private JsonCallback<AccountManager.ChargeDetail> mChargeCallback =   new JsonCallback<AccountManager.ChargeDetail>() {
        @Override
        public void onFail(Call call, Exception e, int id) {
            contentLayout.setDisplayState(QTContentLayout.STATE_ERROR);
        }

        @Override
        public void onResponse(AccountManager.ChargeDetail response, int id) {
            if(response.isError) return;

            List<AccountManager.ChargeItem> chargeItems = response.chargeItems;


            Log.i(App.TAG,"AccountDetailFragment chargeItemSize = " + chargeItems.size());

            getAdapter().clearData();
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

    private JsonCallback<AccountManager.AccountDetail> mAccountCallback =   new JsonCallback<AccountManager.AccountDetail>() {
        @Override
        public void onFail(Call call, Exception e, int id) {
            contentLayout.setDisplayState(QTContentLayout.STATE_ERROR);
        }

        @Override
        public void onResponse(AccountManager.AccountDetail response, int id) {
            if(response.isError) return;

            List<AccountManager.AccountItem> accountItems = response.accountItems;


            Log.i(App.TAG,"AccountDetailFragment accountItemSize = " + accountItems.size());

            getAdapter().clearData();
            for(int i = 0;i < accountItems.size();i++){
                AccountManager.AccountItem accountItem = accountItems.get(i);
                getAdapter().addElement(0,accountItem);
            }

            if(accountItems.size() > 0){
                getAdapter().notifyItemRangeInserted(0, accountItems.size());
            }

            getAdapter().notifyDataSetChanged();

        }
    };

    private JsonCallback<AccountManager.AwardDetail> mAwardCallback =   new JsonCallback<AccountManager.AwardDetail>() {
        @Override
        public void onFail(Call call, Exception e, int id) {
            contentLayout.setDisplayState(QTContentLayout.STATE_ERROR);
        }

        @Override
        public void onResponse(AccountManager.AwardDetail response, int id) {
            if(response.isError) return;

            List<AccountManager.AwardItem> awardItems = response.awardItems;


            Log.i(App.TAG,"AccountDetailFragment chargeItemSize = " + awardItems.size());

            getAdapter().clearData();
            for(int i = 0;i < awardItems.size();i++){
                AccountManager.AwardItem awardItem = awardItems.get(i);
                getAdapter().addElement(0,awardItem);
            }

            if(awardItems.size() > 0){
                getAdapter().notifyItemRangeInserted(0, awardItems.size());
            }

            getAdapter().notifyDataSetChanged();

        }
    };

    @Override
    public void initData(Bundle savedInstanceState) {
        XRecyclerView recyclerView = contentLayout.getRecyclerView();
        setLayoutManager(recyclerView);
        recyclerView.setAdapter(getAdapter());

        if(mType == 0)
            AccountManager.getAccountDetail(mAccountCallback);

        if(mType == 1)
            AccountManager.getChargeDetail(mChargeCallback);

        if(mType == 2)
            AccountManager.getAwardDetail(mAwardCallback);

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
        if (mType == 0) {
            if(mAccountAdapter == null)
                mAccountAdapter = new AccountDetailAdapter(context);
            return mAccountAdapter;
        }

        if (mType == 1) {
            if(mChargeAdapter == null){
                mChargeAdapter = new ChargeDetailAdapter(context);
                mChargeAdapter.setRecItemClick(new RecyclerItemCallback<AccountManager.ChargeItem, ChargeDetailAdapter.ViewHolder>() {
                    @Override
                    public void onItemClick(int position, AccountManager.ChargeItem model, int tag, ChargeDetailAdapter.ViewHolder holder) {
                        super.onItemClick(position, model, tag, holder);
                    }
                });
            }

            return mChargeAdapter;
        }

        if (mType == 2) {
            if(mAwardDetailAdapter == null){
                mAwardDetailAdapter = new AwardDetailAdapter(context);
            }
            return mAwardDetailAdapter;
        }

        return null;
    }

    public void setLayoutManager(XRecyclerView recyclerView) {
        recyclerView.gridLayoutManager(context, 1);
    }
}
