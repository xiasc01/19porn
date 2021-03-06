package cn.droidlover.xdroid.demo.ui;

/**
 * Created by Administrator on 2017/5/7 0007.
 */

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.zip.ZipFile;

import cn.droidlover.xdroid.base.XActivity;
import cn.droidlover.xdroid.demo.App;
import cn.droidlover.xdroid.demo.R;
import cn.droidlover.xdroid.demo.User;
import cn.droidlover.xdroid.demo.VideoManager;
import cn.droidlover.xdroid.demo.kit.AppKit;
import cn.droidlover.xdroid.demo.ui.person.PersonFragment;

import static android.Manifest.permission.READ_CONTACTS;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;

public class MainActivity2 extends XActivity {

    public final static int num = 3 ;

    Fragment homeFragment;
    Fragment personFragment;
    private FragmentManager fragmentManager;
    private FragmentTransaction transaction;
    private RadioGroup radioGroup;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public void initData(Bundle savedInstanceState) {
        User user = User.getInstance();

        AppKit.mainActivity = this;

        fragmentManager = getSupportFragmentManager();
        radioGroup = (RadioGroup)findViewById(R.id.radioGroup1);
        ((RadioButton)radioGroup.findViewById(R.id.radio0)).setChecked(true);

        transaction = fragmentManager.beginTransaction();
        homeFragment = new HomeFragment();
        transaction.replace(R.id.content, homeFragment);
        transaction.commit();

          /*transaction = fragmentManager.beginTransaction();
        if(personFragment == null){
            personFragment = new PersonFragment();
        }
        transaction.replace(R.id.content, personFragment);
        transaction.commit();*/

        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId) {
                    case R.id.radio0:
                        transaction = fragmentManager.beginTransaction();
                        if(homeFragment == null){
                            homeFragment = new HomeFragment();
                        }
                        transaction.replace(R.id.content, homeFragment);
                        transaction.commit();
                        break;
                    case R.id.radio1:
                        transaction = fragmentManager.beginTransaction();
                        if(personFragment == null){
                            personFragment = new PersonFragment();
                        }
                        transaction.replace(R.id.content, personFragment);
                        transaction.commit();
                        break;
                }

            }
        });
    }

    @Override
    public void setListener() {

    }

    @Override
    public int getLayoutId() {
        return R.layout.activity_main2;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == 1){
            ((PersonFragment)personFragment).initUserData();
        }
        if(requestCode == 2){
            if(MovieInfoActivity.modifyMovieInfos.size() > 0){
                VideoManager.getInstance().modifyMovieInfo(MovieInfoActivity.movieId,MovieInfoActivity.modifyMovieInfos);
            }
        }
    }
}