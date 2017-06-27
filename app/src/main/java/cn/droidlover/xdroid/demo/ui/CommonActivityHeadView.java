package cn.droidlover.xdroid.demo.ui;

import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import cn.droidlover.xdroid.demo.R;

/**
 * TODO: document your custom view class.
 */
public class CommonActivityHeadView extends LinearLayout implements View.OnClickListener {
    private Context mContext = null;
    TextView mTitle;
    ImageButton imageButton;
    View.OnClickListener mListener;

    public CommonActivityHeadView(Context context, AttributeSet attrs) {
        super(context, attrs);
        // TODO Auto-generated constructor stub
        mContext = context;
        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View parentView = inflater.inflate(R.layout.common_activity_head_view, this, true);
        mTitle = (TextView) findViewById(R.id.activity_head_title);
        imageButton = (ImageButton)findViewById(R.id.back_image);
        imageButton.setOnClickListener(this);
    }


    public void setTitle(String title){
        mTitle.setText(title);
    }

    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.back_image){
            ((Activity)getContext()).finish();
        }
    }
}
