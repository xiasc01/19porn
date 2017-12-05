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

public class AwardDetailAdapter  extends SimpleRecAdapter<AccountManager.AwardItem, AwardDetailAdapter.ViewHolder> {
    public AwardDetailAdapter(Context context){
        super(context);
    }
    @Override
    public AwardDetailAdapter.ViewHolder newViewHolder(View itemView) {
        return new AwardDetailAdapter.ViewHolder(itemView);
    }

    @Override
    public int getLayoutId() {
        return R.layout.adapter_award;
    }

    @Override
    public void onBindViewHolder(AwardDetailAdapter.ViewHolder holder, int position) {
        final  AccountManager.AwardItem item = data.get(position);
        Log.i(App.TAG,"Account onBindViewHolder time = " + item.mTime);
        if(item != null){
            holder.awardTimeYmd.setText(item.mTime.substring(0,10));
            holder.awardTimeHms.setText(item.mTime.substring(11));
            holder.awardCoin.setText(item.mCoin + "");
            holder.awardType.setText(item.mType);
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.award_time_ymd_text)
        TextView awardTimeYmd;

        @BindView(R.id.award_time_hms_text)
        TextView awardTimeHms;

        @BindView(R.id.award_coin_text)
        TextView awardCoin;

        @BindView(R.id.award_type_text)
        TextView awardType;

        public ViewHolder(View itemView) {
            super(itemView);
            KnifeKit.bind(this, itemView);
        }
    }
}
