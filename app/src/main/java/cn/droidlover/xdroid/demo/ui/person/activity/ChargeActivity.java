package cn.droidlover.xdroid.demo.ui.person.activity;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.zhy.http.okhttp.OkHttpUtils;
import com.zhy.http.okhttp.callback.StringCallback;

import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import butterknife.BindView;
import cn.droidlover.xdroid.base.XActivity;
import cn.droidlover.xdroid.demo.App;
import cn.droidlover.xdroid.demo.R;
import cn.droidlover.xdroid.demo.User;
import cn.droidlover.xdroid.demo.kit.AppKit;
import cn.droidlover.xdroid.demo.net.JsonCallback;
import cn.droidlover.xdroid.demo.net.NetApi;
import cn.droidlover.xdroid.demo.ui.CommonActivityHeadView;
import cn.droidlover.xdroid.demo.ui.PersonItem;
import okhttp3.Call;

public class ChargeActivity extends XActivity implements View.OnClickListener {
    @BindView(R.id.head_view)
    CommonActivityHeadView mHeadView;

    @BindView(R.id.layout_charge)
    LinearLayout mLayoutCharge;

    @BindView(R.id.notice)
    TextView mNotice;

    @BindView(R.id.pay_progress)
    View mProgressView;

    Charge mCharge;

    int mReConnect = 0;
    @Override
    public void onClick(View view) {
        if(view instanceof PersonItem){
            int id = view.getId();
            onClickCharge(id);
        }
    }

    class ChargeItem{
        String value;
        String money;
    }

    class Charge{
        public List<ChargeItem> chargeItems = new ArrayList<ChargeItem>();
        public String notice;
        public String payAccount;
    }

    @Override
    public void initData(Bundle savedInstanceState) {
        mHeadView = (CommonActivityHeadView)findViewById(R.id.head_view);
        mHeadView.setTitle("充值");

        final Context chargeActivity = (Context)this;
        String url = AppKit.getServerUrl();
        JsonCallback<Charge> callback = new JsonCallback<Charge>(1 * 60 * 60 * 1000) {
            @Override
            public void onFail(Call call, Exception e, int id) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Charge response, int id) {
                mCharge = response;

                for (int i = 0;i < response.chargeItems.size();i++) {
                    PersonItem item = new PersonItem(chargeActivity,null);
                    ChargeItem chargeItems = response.chargeItems.get(i);
                    item.setArrowVisible(View.GONE);
                    item.setItemName(chargeItems.value);
                    item.setItemValue("¥ " + chargeItems.money);
                    item.setId(i);
                    item.setOnClickListener((View.OnClickListener)chargeActivity);

                    TextView valueText = (TextView) item.getChildView("item_value");
                    valueText.setTextColor(Color.parseColor("#ff0000"));

                    item.setItemNameImage(R.mipmap.pay_diamond);
                    LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT,
                            LayoutParams.WRAP_CONTENT);
                    mLayoutCharge.addView(item,params);
                }
                mNotice.setText(response.notice);
            }
        };

        HashMap<String, String> params = new HashMap<String, String>();
        params.put("request_type","fetch_charge");

        OkHttpUtils.get().url(url)
                .params(params)
                .build()
                .execute(callback);
    }

    @Override
    public void setListener() {

    }

    public void onClickCharge(final int id){
        final Dialog inputDialog =  new Dialog(this);
        inputDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        final View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_charge,null);

        final TextView chargeNotice = (TextView)dialogView.findViewById(R.id.charge_notice);
        final Button   btnCancel    = (Button)dialogView.findViewById(R.id.btn_cancel);
        final Button   btnConfirm   = (Button)dialogView.findViewById(R.id.btn_confirm);

        final Context context = (Context) this;
        inputDialog.setContentView(dialogView);

        String  value =  mCharge.chargeItems.get(id).value;
        String  money =  mCharge.chargeItems.get(id).money;

        String text = "您将充入" + value + "个钻石，需支付" + money + "元。确定后系统将记录该笔交易";
        chargeNotice.setText(text);

        btnConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //inputDialog.dismiss();

                pay(getPayAmountFormId(id));

               //String text   = "充值成功! 订单号435532 请尽快向支付宝" + mCharge.payAccount + "转账5.00 并备注订单号，若一定时间没有收到您的费用，将停止您对该app的使用";
                //String notice = "      您有一笔交易未支付，订单号435532 请尽快向支付宝" + mCharge.payAccount + "转账5.00 并备注订单号，若一定时间没有收到您的费用，将停止您对该app的使用";
                //mNotice.setText(notice);

                inputDialog.dismiss();

                /*chargeNotice.setText(text);
                btnConfirm.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        inputDialog.dismiss();
                    }
                });
                btnCancel.setVisibility(View.INVISIBLE);*/
            }
        });

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                inputDialog.dismiss();
            }
        });

        inputDialog.setCanceledOnTouchOutside(false);
        inputDialog.show();
    }

    @Override
    public int getLayoutId() {
        return R.layout.activity_charge;
    }

    private float getPayAmountFormId(int id){
        if(mCharge != null){
            if(id >= 0 && id < mCharge.chargeItems.size()){
                ChargeItem chargeItem = mCharge.chargeItems.get(id);
                return Float.parseFloat(chargeItem.money);
            }
        }

        return 0;
    }

    private int pay(final float amount){
        HashMap<String, String> params = new HashMap<String, String>();
        params.put("request_type","pay");
        params.put("user_id", User.getInstance().getUserId());
        params.put("signature", User.getInstance().getSignature());
        params.put("amount", amount + "");
        String msg = "";

        final Dialog inputDialog =  new Dialog(this);
        inputDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        final View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_charge,null);

        final TextView chargeNotice = (TextView)dialogView.findViewById(R.id.charge_notice);
        final Button   btnCancel    = (Button)dialogView.findViewById(R.id.btn_cancel);
        final Button   btnConfirm   = (Button)dialogView.findViewById(R.id.btn_confirm);
        btnCancel.setVisibility(View.INVISIBLE);
        inputDialog.setContentView(dialogView);

        btnConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                inputDialog.dismiss();
            }
        });

        StringCallback callback = new StringCallback() {
            @Override
            public void onError(Call call, Exception e, int id) {
                e.printStackTrace();
                String msg = "";

                boolean isSocketTimeoutException = e instanceof SocketTimeoutException;
                if(isSocketTimeoutException){
                    mReConnect++;

                    if(mReConnect < 5){
                        AppKit.updateServerUrl();
                        pay(amount);
                        return;
                    }else {
                        msg = "充值失败 网络超时 请稍后再试";
                    }
                }else {
                    msg = "充值失败 服务器出现错误";
                }

                inputDialog.show();
                showProgress(false);
            }

            @Override
            public void onResponse(String response, int id) {
                Log.i(App.TAG,"order id = " + response);

                String text   = "充值成功! 订单号" + response + " 请尽快向支付宝" + mCharge.payAccount + "转账"+ amount +" 并备注订单号，若一定时间没有收到您的费用，将停止您对该app的使用";
                chargeNotice.setText(text);


                String notice = "      您有一笔交易未支付，订单号" + response + " 请尽快向支付宝" + mCharge.payAccount + "转账"+ amount +" 并备注订单号，若一定时间没有收到您的费用，将停止您对该app的使用";
                mNotice.setText(notice);

                inputDialog.show();
                showProgress(false);
            }
        };

        NetApi.invokeGet(params,callback);
        showProgress(true);
        return 0;
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

           /* mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            mLoginFormView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });*/

            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            mProgressView.animate().setDuration(shortAnimTime).alpha(
                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
                }
            });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
            //mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }
}
