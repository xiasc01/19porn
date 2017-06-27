package cn.droidlover.xdroid.demo.adapter;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import butterknife.BindView;
import cn.droidlover.xdroid.base.SimpleRecAdapter;
import cn.droidlover.xdroid.demo.App;
import cn.droidlover.xdroid.demo.User;
import cn.droidlover.xdroid.demo.VideoManager;
import cn.droidlover.xdroid.demo.kit.AppKit;
import cn.droidlover.xdroid.demo.kit.ThumbLoad;
import cn.droidlover.xdroid.demo.ui.PlayerActivity;
import cn.droidlover.xdroid.demo.R;
import cn.droidlover.xdroid.demo.model.MovieInfo;
import cn.droidlover.xdroid.kit.KnifeKit;

/**
 * Created by wanglei on 2016/12/10.
 */

public class ShortVideoAdapter extends SimpleRecAdapter<MovieInfo.Item, ShortVideoAdapter.ViewHolder> {

    public ShortVideoAdapter(Context context) {
        super(context);
    }

    @Override
    public ViewHolder newViewHolder(View itemView) {
        return new ViewHolder(itemView);
    }

    @Override
    public int getLayoutId() {
        return R.layout.adapter_short_video;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        final  MovieInfo.Item item = data.get(position);

        if(item.getMovie_id() == null || item.getThumb_key() == null){
            Log.e(App.TAG,"onBindViewHolder movie is null");
            return;
        }

        int thumbPos = 0;
        if(item.getThumb_pos() != null){
            thumbPos = Integer.parseInt(item.getThumb_pos());
        }

        int thumbSize = 0;
        if(item.getThumb_size() != null){
            thumbSize = Integer.parseInt(item.getThumb_size());
        }

        ThumbLoad.getInstance().loadImage(holder.ivGirl,item.getThumb_url(),thumbPos,thumbSize,item.getThumb_key(),item.getMovie_id());

        holder.title.setText(item.getTitle());
        holder.duration.setText(item.getFormatDuration());

        if(item.getHasPlay()){
            holder.title.setTextColor(Color.argb(250,200, 200, 200));
            holder.valueIcon.setImageResource(R.mipmap.pay_diamond_disable);
        }

        Log.i(App.TAG,"title " + item.getTitle());


        holder.ivGirl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startPlay(item);
            }
        });

        holder.playBtn.setOnClickListener(new ImageButton.OnClickListener(){
            public void onClick(View v) {
                startPlay(item);
            }
        });

        holder.praiseBtn.setOnClickListener(new ImageButton.OnClickListener(){
            public void onClick(View v) {
                ((ImageButton)v).setImageResource(R.mipmap.res_click_good_hover);
            }
        });

        holder.enshrineBtn.setOnClickListener(new ImageButton.OnClickListener(){
            public void onClick(View v) {
                ((ImageButton)v).setImageResource(R.mipmap.a6e);
            }
        });
    }

    private void startPlay(final MovieInfo.Item item){
        final Intent intent = new Intent((Activity)context, PlayerActivity.class);

        final ShortVideoAdapter shortVideoAdapter = this;

        VideoManager.getInstance().getPlayUrl(item.getMovie_id(), new User.GetPlayUrlCallback() {
            @Override
            public void onGetPlayUrl(String url) {
                if(url != null && url.length() > 4){
                    intent.putExtra(PlayerActivity.VIDEO_FILE_PATH, url);
                    intent.putExtra(PlayerActivity.VIDEO_FILE_NAME, item.getTitle());
                    intent.putExtra(PlayerActivity.VIDEO_CACHE_PATH, AppKit.getMediaCachePath());
                    context.startActivity(intent);

                    int pos = shortVideoAdapter.data.indexOf(item);
                    item.setHasPlay(true);
                    shortVideoAdapter.updateElement(item,pos);
                   // shortVideoAdapter.removeElement(item);
                }
            }
        });

    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        @BindView(R.id.iv_girl)
        ImageView ivGirl;
        @BindView(R.id.short_video_title)
        TextView title;
        @BindView(R.id.duration)
        TextView duration;
        @BindView(R.id.th_play)
        ImageButton playBtn;
        @BindView(R.id.praise_icon)
        ImageButton praiseBtn;
        @BindView(R.id.enshrine_icon)
        ImageButton enshrineBtn;
        @BindView(R.id.value_icon)
        ImageView valueIcon;

        public ViewHolder(View itemView) {
            super(itemView);
            KnifeKit.bind(this, itemView);
        }
    }
}
