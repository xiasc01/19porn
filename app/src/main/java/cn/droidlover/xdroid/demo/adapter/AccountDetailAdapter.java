//AccountDetailAdapter
package cn.droidlover.xdroid.demo.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import cn.droidlover.xdroid.base.SimpleRecAdapter;
import cn.droidlover.xdroid.demo.model.MovieInfo;
import cn.droidlover.xdroid.kit.KnifeKit;

/**
 * Created by lzmlsfe on 2017/10/9.
 */

public class AccountDetailAdapter  extends SimpleRecAdapter<MovieInfo.Item, AccountDetailAdapter.ViewHolder> {

    public  AccountDetailAdapter(Context context){
        super(context);
    }
    @Override
    public ViewHolder newViewHolder(View itemView) {
        return null;
    }

    @Override
    public int getLayoutId() {
        return 0;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {

    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public ViewHolder(View itemView) {
            super(itemView);
            KnifeKit.bind(this, itemView);
        }
    }
}
