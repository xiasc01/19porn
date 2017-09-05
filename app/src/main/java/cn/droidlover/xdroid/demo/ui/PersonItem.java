package cn.droidlover.xdroid.demo.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.view.View;
import android.widget.TextView;

import cn.droidlover.xdroid.demo.R;
import cn.droidlover.xdroid.demo.kit.AppKit;

/**
 * Created by Administrator on 2017/5/7 0007.
 */

public class PersonItem extends RelativeLayout implements View.OnClickListener {
    Context     mContext;
    TextView    mItemName;
    TextView    mItemValue;
    TextView    mItemValue2;
    ImageView   mItemValueImage;
    ImageView   mItemNameImage;
    ImageView   mItemValue2Image;
    View        mLine;
    View        mItemLayout;
    View        mArrow;
    View.OnClickListener mListener = null;

    public PersonItem(Context context, AttributeSet attrs) {
        super(context, attrs);

        mContext = context;
        LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View parentView = inflater.inflate(R.layout.person_item, this, true);

        mItemLayout = (View)findViewById(R.id.item_layout);
        mItemLayout.setOnClickListener(this);

        mItemName           = (TextView)findViewById(R.id.item_name);
        mItemValue          = (TextView)findViewById(R.id.item_value);
        mItemValue2          = (TextView)findViewById(R.id.item_value2);
        mItemValueImage     = (ImageView)findViewById(R.id.item_value_image);
        mItemNameImage      = (ImageView)findViewById(R.id.item_name_image);
        mItemValue2Image    = (ImageView)findViewById(R.id.item_value2_image);
        mLine               = (View)findViewById(R.id.line);
        mArrow              = (View)findViewById(R.id.image_arrow);
    }

    @Override
    public void onClick(View view) {
        if(mListener != null){
            mListener.onClick(this);
        }
    }

    public void setItemName(String itemName){
        if(itemName == null){
            mItemName.setVisibility(View.GONE);
            return;
        }

        mItemName.setText(itemName);
    }

    public void setItemValue(String itemValue){
        if(itemValue == null){
            mItemValue.setVisibility(View.GONE);
            return;
        }

        mItemValue.setVisibility(View.VISIBLE);
        mItemValue.setText(itemValue);
    }

    public void setItemValue2(String itemValue){
        if(itemValue == null){
            mItemValue2.setVisibility(View.GONE);
            return;
        }

        mItemValue2.setVisibility(View.VISIBLE);
        mItemValue2.setText(itemValue);
    }

    public void setItemValueImage(Bitmap bitmap){
        if(bitmap == null){
            mItemValueImage.setVisibility(View.GONE);
            return;
        }

        mItemValueImage.setVisibility(View.VISIBLE);
        mItemValueImage.setImageBitmap(bitmap);

        Context context = getContext();
        int left   = AppKit.dip2px(context,12f);
        int top    = AppKit.dip2px(context,5f);
        int right  = AppKit.dip2px(context,12f);
        int bottom = AppKit.dip2px(context,5f);

        mItemLayout.setPadding(left,top,right,bottom);
    }

    public void setItemValue2Image(int resId){
        mItemValue2Image.setVisibility(View.VISIBLE);
        mItemValue2Image.setImageResource(resId);
    }

    public void setItemNameImage(int resId){
        mItemNameImage.setVisibility(View.VISIBLE);
        mItemNameImage.setImageResource(resId);
    }

    public void setArrowVisible(int visible){
        mArrow.setVisibility(visible);
    }

    public View getChildView(String viewId){
        if(viewId.equals("item_name")){
            return mItemName;
        }

        if(viewId.equals("item_value")){
            return mItemValue;
        }

        if(viewId.equals("item_value_image")){
            return mItemValueImage;
        }

        if(viewId.equals("item_name_image")){
            return mItemNameImage;
        }

        if(viewId.equals("line")){
            return mLine;
        }

        if(viewId.equals("image_arrow")){
            return mArrow;
        }
        return  null;
    }

    public void setLineVisible(int visibility){
        mLine.setVisibility(visibility);
    }

    public void setOnClickListener(View.OnClickListener listener){
        mListener  = listener;
    }
}
