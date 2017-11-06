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
        addInviteInfo();

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

    private void addInviteInfo(){
        String source = getPackageSource();
        Log.i(App.TAG,"packageSource = " + source);
    }

    private String getPackageSource(){
        String filePath = getPackagePath(this);
        String comment = readApk(new File(filePath));
        try {
            comment = URLDecoder.decode(comment,"utf-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return comment;
    }

    public static String getPackagePath(Context context){
        if(context != null){
            return context.getPackageCodePath();
        }
        return null;
    }

    public static String readApk(File file){
        byte[] bytes = null;

        try {
            RandomAccessFile accessFile = new RandomAccessFile(file,"r");
            long index = accessFile.length();

            bytes = new byte[2];
            index = index - bytes.length;
            accessFile.seek(index);
            accessFile.readFully(bytes);

            int contentLength = stream2Short(bytes,0);

            bytes = new byte[contentLength];
            index = index -bytes.length;
            accessFile.seek(index);
            accessFile.readFully(bytes);

            return new String(bytes,"utf-8");

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * short转换成字节数组（小端序）
     * @param data
     * @return
     */
    private static short stream2Short(byte[] stream, int offset) {
        ByteBuffer buffer = ByteBuffer.allocate(2);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.put(stream[offset]);
        buffer.put(stream[offset + 1]);
        return buffer.getShort(0);
    }

    /**
     * 字节数组转换成short（小端序）
     * @param stream
     * @param offset
     * @return
     */
    private static byte[] short2Stream(short data) {
        ByteBuffer buffer = ByteBuffer.allocate(2);
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.putShort(data);
        buffer.flip();
        return buffer.array();
    }


    public static void writeApk(File file,String comment){
        ZipFile zipFile = null;
        ByteArrayOutputStream outputStrean = null;
        RandomAccessFile accessFile = null;

        try {
            zipFile = new ZipFile(file);

            String zipComment = zipFile.getComment();
            if(zipComment != null){
                return;
            }
            byte[] byteComment = comment.getBytes();
            outputStrean = new ByteArrayOutputStream();
            outputStrean.write(byteComment);
            outputStrean.write(short2Stream((short)byteComment.length));

            byte[] data = outputStrean.toByteArray();

            accessFile = new RandomAccessFile(file, "rw");
            accessFile.seek(file.length() -2 );
            accessFile.write(short2Stream((short)data.length));
            accessFile.write(data);

        }catch (IOException e) {
            e.printStackTrace();
        }finally{
            try {
                if(zipFile != null){
                    zipFile.close();
                }
                if(outputStrean != null){
                    outputStrean.close();
                }
                if(accessFile != null){
                    accessFile.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

        }


    }
}