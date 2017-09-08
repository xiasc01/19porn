package cn.droidlover.xdroid.demo.ui.person.activity;

import android.os.Bundle;
import android.widget.LinearLayout;

import butterknife.BindView;
import cn.droidlover.xdroid.base.XActivity;
import cn.droidlover.xdroid.demo.R;
import cn.droidlover.xdroid.demo.ui.CommonActivityHeadView;

/**
 * Created by lzmlsfe on 2017/7/26.
 */

public class AccountActivity extends XActivity {
    @BindView(R.id.head_view)
    CommonActivityHeadView mHeadView;

    @BindView(R.id.layout_transaction_record)
    LinearLayout mLayoutTransactionRecord;

    @Override
    public void initData(Bundle savedInstanceState) {
        mHeadView.setTitle("交易记录");
    }

    @Override
    public void setListener() {

    }

    @Override
    public int getLayoutId() {
        return R.layout.activity_account;
    }
}
