package cn.droidlover.xdroid.demo.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import butterknife.BindView;
import cn.droidlover.xdroid.base.XActivity;
import cn.droidlover.xdroid.demo.R;
import cn.droidlover.xdroid.demo.VideoManager;
import cn.droidlover.xdroid.demo.model.MovieInfo;

public class MovieInfoActivity extends XActivity {
    public static Map<String,String> modifyMovieInfos = new HashMap<String,String>();
    public static String movieId;

    @BindView(R.id.IDEdit)
    TextView IDEdit;
    @BindView(R.id.title)
    EditText title;
    @BindView(R.id.content)
    EditText content;
    @BindView(R.id.pic)
    EditText pic;
    @BindView(R.id.subtype)
    EditText subType;
    @BindView(R.id.value)
    EditText value;
    @BindView(R.id.grade)
    EditText grade;

    @BindView(R.id.cancel_btn)
    Button cancelBtn;
    @BindView(R.id.ok_btn)
    Button okBtn;

    @Override
    public void initData(Bundle savedInstanceState) {
        modifyMovieInfos.clear();

        Intent intent = getIntent();
        String movieID = intent.getStringExtra("MovieID");
        movieId = movieID;

        MovieInfo.Item item = VideoManager.getInstance().getMovieInfoItem(movieID);
        if(item == null){
            return;
        }
        IDEdit.setText(item.getMovie_id());
        title.setText(item.getTitle());
        content.setText(item.getScore());
        pic.setText(item.getPic_score());
        value.setText(item.getValue());


        title.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                if(modifyMovieInfos.containsKey("title")){
                    modifyMovieInfos.remove("title");
                }
                modifyMovieInfos.put("title","" + s);
            }
        });

        content.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                if(modifyMovieInfos.containsKey("score")){
                    modifyMovieInfos.remove("score");
                }
                modifyMovieInfos.put("score","" + s);
            }
        });

        pic.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                if(modifyMovieInfos.containsKey("pic_score")){
                    modifyMovieInfos.remove("pic_score");
                }
                modifyMovieInfos.put("pic_score","" + s);
            }
        });

        subType.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                if(modifyMovieInfos.containsKey("sub_type1")){
                    modifyMovieInfos.remove("sub_type1");
                }
                modifyMovieInfos.put("sub_type1","" + s);
            }
        });

        value.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                if(modifyMovieInfos.containsKey("value")){
                    modifyMovieInfos.remove("value");
                }
                modifyMovieInfos.put("value","" + s);
            }
        });

        grade.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                if(modifyMovieInfos.containsKey("grade")){
                    modifyMovieInfos.remove("grade");
                }
                modifyMovieInfos.put("grade","" + s);
            }
        });


        okBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                modifyMovieInfos.clear();
                finish();
            }
        });
    }

    @Override
    public void setListener() {

    }

    @Override
    public int getLayoutId() {
        return R.layout.activity_movie_info;
    }
}
