package cn.droidlover.xdroid.demo.adapter;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Message;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import butterknife.BindView;
import cn.droidlover.xdroid.base.SimpleRecAdapter;
import cn.droidlover.xdroid.demo.App;
import cn.droidlover.xdroid.demo.User;
import cn.droidlover.xdroid.demo.VideoManager;
import cn.droidlover.xdroid.demo.kit.AppKit;
import cn.droidlover.xdroid.demo.kit.ThumbLoad;
import cn.droidlover.xdroid.demo.ui.MovieInfoActivity;
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

        if(item.getSet_name() != null && item.getSet_name().length() > 0){
            /*holder.mulItemLayout.setVisibility(View.VISIBLE);
            FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams)holder.mulItemLayout.getLayoutParams();
            layoutParams.width = 1080;
            layoutParams.height = 9 * layoutParams.width / 16;
            holder.mulItemLayout.setLayoutParams(layoutParams);
            holder.singleItemLayout.setVisibility(View.GONE);
            List<MovieInfo.Item> movies = VideoManager.getInstance().getMovieSet(item.getSet_name());
            if(movies != null){
                for (int i = 0;i < movies.size() && i < 4;i++){
                    MovieInfo.Item subItem = movies.get(i);
                    if(i == 0) setImage(subItem,holder.ivThumb1);
                    if(i == 1) setImage(subItem,holder.ivThumb2);
                    if(i == 2) setImage(subItem,holder.ivThumb3);
                    if(i == 3) setImage(subItem,holder.ivThumb4);
                }
            }*/
            return;
        }

        holder.singleItemLayout.setVisibility(View.VISIBLE);
        holder.mulItemLayout.setVisibility(View.GONE);
        if(item.getMovie_id() == null || item.getThumb_key() == null){
            Log.e(App.TAG,"onBindViewHolder movie is null");
            return;
        }

        setImage(item,holder.ivGirl);
        //VideoManager.getInstance().setThumbToImageView(item.getMovie_id(),item.getType(),position,holder.ivGirl);

        holder.title.setText(item.getTitle());
        holder.duration.setText(item.getFormatDuration());
        holder.value.setText(item.getValue());
        holder.contentScore.setText("内容 " + item.getScore());
        holder.picScore.setText("画质 " + item.getPic_score());
        if(item.getSub_type1() != null && item.getSub_type1().length() > 0){
            holder.subType.setText(item.getSub_type1());
        }else{
            holder.subType.setVisibility(View.GONE);
            holder.splitLine1.setVisibility(View.GONE);
        }


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

        holder.detailInfoLabel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent((Activity)context, MovieInfoActivity.class);
                if(intent != null){
                    intent.putExtra("MovieID", item.getMovie_id());
                    ((Activity) context).startActivityForResult(intent,2);
                }
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

    private void setImage(final MovieInfo.Item item,final ImageView imageView){
        if(ThumbLoad.getInstance() == null){
            ThumbLoad.getInstance();
        }

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    int thumbPos = 0;
                    if(item.getThumb_pos() != null){
                        thumbPos = Integer.parseInt(item.getThumb_pos());
                    }

                    int thumbSize = 0;
                    if(item.getThumb_size() != null){
                        thumbSize = Integer.parseInt(item.getThumb_size());
                    }

                    ThumbLoad.getInstance().loadImage(imageView,item.getThumb_url(),thumbPos,thumbSize,item.getThumb_key(),item.getMovie_id());

                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        });
        thread.start();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.mul_item_layout)
        View mulItemLayout;

        @BindView(R.id.single_item_layout)
        View singleItemLayout;

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
        @BindView(R.id.value_text)
        TextView value;
        @BindView(R.id.detail_info_label)
        View detailInfoLabel;
        @BindView(R.id.label_content_score)
        TextView contentScore;
        @BindView(R.id.label_pic_score)
        TextView picScore;
        @BindView(R.id.label_sub_type)
        TextView subType;
        @BindView(R.id.detail_info_split_line1)
        TextView splitLine1;




        @BindView(R.id.ivThumb1)
        ImageView ivThumb1;
        @BindView(R.id.ivThumb2)
        ImageView ivThumb2;
        @BindView(R.id.ivThumb3)
        ImageView ivThumb3;
        @BindView(R.id.ivThumb4)
        ImageView ivThumb4;


        public ViewHolder(View itemView) {
            super(itemView);
            KnifeKit.bind(this, itemView);
        }
    }
}
