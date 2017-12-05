package cn.droidlover.xdroid.demo.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.TextView;

import com.aplayer.aplayerandroid.Log;

import butterknife.BindView;
import cn.droidlover.xdroid.base.SimpleRecAdapter;
import cn.droidlover.xdroid.demo.AccountManager;
import cn.droidlover.xdroid.demo.App;
import cn.droidlover.xdroid.demo.R;
import cn.droidlover.xdroid.kit.KnifeKit;

/**
 * Created by Administrator on 2017/12/1 0001.
 */

public class AccountDetailAdapter extends SimpleRecAdapter<AccountManager.AccountItem, AccountDetailAdapter.ViewHolder> {

    public AccountDetailAdapter(Context context){
        super(context);
    }
    @Override
    public AccountDetailAdapter.ViewHolder newViewHolder(View itemView) {
        return new AccountDetailAdapter.ViewHolder(itemView);
    }

    @Override
    public int getLayoutId() {
        return R.layout.adapter_account;
    }

    @Override
    public void onBindViewHolder(AccountDetailAdapter.ViewHolder holder, int position) {
        final  AccountManager.AccountItem item = data.get(position);
        Log.i(App.TAG,"Account onBindViewHolder time = " + item.mTime);
        if(item != null){
            holder.accountTimeYmd.setText(item.mTime.substring(0,10));
            holder.accountTimeHms.setText(item.mTime.substring(11));
            holder.accountSpare.setText(item.mSpare + "");
            int coin = item.mCoin;
            if(coin > 0){
                holder.accountCoin.setText("+" + item.mCoin);
            }else{
                holder.accountCoin.setText(item.mCoin + "");
            }
            holder.accountType.setText(item.mType);
            if(item.mName == null ||item.mName.length() == 0){
                holder.accountDetail.setVisibility(View.GONE);
            }else{
                holder.accountDetail.setText(item.mName);
                holder.accountDetail.setVisibility(View.VISIBLE);
            }

        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.charge_time_ymd_text)
        TextView accountTimeYmd;

        @BindView(R.id.charge_time_hms_text)
        TextView accountTimeHms;

        @BindView(R.id.account_spare_text)
        TextView accountSpare;

        @BindView(R.id.account_coin_text)
        TextView accountCoin;

        @BindView(R.id.account_type_text)
        TextView accountType;

        @BindView(R.id.account_detail_text)
        TextView accountDetail;

        public ViewHolder(View itemView) {
            super(itemView);
            KnifeKit.bind(this, itemView);
        }
    }
}
