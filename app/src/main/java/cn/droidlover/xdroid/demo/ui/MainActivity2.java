package cn.droidlover.xdroid.demo.ui;

/**
 * Created by Administrator on 2017/5/7 0007.
 */

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.Menu;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import cn.droidlover.xdroid.base.XActivity;
import cn.droidlover.xdroid.demo.App;
import cn.droidlover.xdroid.demo.R;
import cn.droidlover.xdroid.demo.ui.person.PersonFragment;

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
        fragmentManager = getSupportFragmentManager();
        radioGroup = (RadioGroup)findViewById(R.id.radioGroup1);
        ((RadioButton)radioGroup.findViewById(R.id.radio0)).setChecked(true);

        transaction = fragmentManager.beginTransaction();
        homeFragment = new HomeFragment();
        transaction.replace(R.id.content, homeFragment);
        transaction.commit();

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
    }
}