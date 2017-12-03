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
 * Created by Administrator on 2017/12/3 0003.
 */

public class AwardDetailAdapter  extends SimpleRecAdapter<AccountManager.InvitationItem, AwardDetailAdapter.ViewHolder> {
    public AwardDetailAdapter(Context context){
        super(context);
    }
    @Override
    public AwardDetailAdapter.ViewHolder newViewHolder(View itemView) {
        return new AwardDetailAdapter.ViewHolder(itemView);
    }

    @Override
    public int getLayoutId() {
        return R.layout.adapter_charge;
    }

    @Override
    public void onBindViewHolder(AwardDetailAdapter.ViewHolder holder, int position) {
        final  AccountManager.InvitationItem item = data.get(position);
        Log.i(App.TAG,"Account onBindViewHolder time = " + item.mTime);
        if(item != null){
            /*holder.accountTimeYmd.setText(item.mTime.substring(0,10));
            holder.accountTimeHms.setText(item.mTime.substring(11));
            holder.accountOrderId.setText(item.mOderId);
            holder.accountCoin.setText(item.mCoin);
            holder.accountUnPayAmount.setText(item.mUnPayAmount);*/
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.account_time_ymd_text)
        TextView accountTimeYmd;

        @BindView(R.id.account_time_hms_text)
        TextView accountTimeHms;

        @BindView(R.id.account_orderId_text)
        TextView accountOrderId;

        @BindView(R.id.account_coin_text)
        TextView accountCoin;

        @BindView(R.id.account_unpay_amount_text)
        TextView accountUnPayAmount;

        public ViewHolder(View itemView) {
            super(itemView);
            KnifeKit.bind(this, itemView);
        }
    }
}
