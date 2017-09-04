package cn.droidlover.xdroid.demo.ui.person.activity;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cn.droidlover.xdroid.demo.App;
import cn.droidlover.xdroid.demo.R;
import cn.droidlover.xdroid.demo.User;
import cn.droidlover.xdroid.demo.kit.AppKit;
//import cn.droidlover.xdroid.demo.ui.LoginActivity;
import cn.droidlover.xdroid.demo.ui.LoginActivity;
import cn.droidlover.xdroid.demo.ui.PersonItem;

public class PersonInfoActivity extends AppCompatActivity implements View.OnClickListener{
    private static int RESULT_LOAD_IMAGE = 1;

    PersonItem mUserId;
    PersonItem mUserName;
    PersonItem mPortrait;
    PersonItem mPassword;
    PersonItem mEmail;

    Bitmap pngBM;

    Button loginOutBtn;
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 0) {
                mPortrait.setItemValueImage(pngBM);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_person_info);

        mPortrait = (PersonItem)findViewById(R.id.portrait);
        mPortrait.setItemName("头像");
        mPortrait.setOnClickListener(this);
        setPortrait();

        mUserId = (PersonItem)findViewById(R.id.userId);
        mUserId.setItemName("ID");
        mUserId.setItemValue(User.getInstance().getUserId());

        mUserName = (PersonItem)findViewById(R.id.userName);
        mUserName.setItemName("昵称");
        mUserName.setItemValue(User.getInstance().getUserName());
        mUserName.setOnClickListener(this);

        mPassword = (PersonItem)findViewById(R.id.password);
        mPassword.setItemName("密码");
        mPassword.setOnClickListener(this);

        mEmail = (PersonItem)findViewById(R.id.email);
        mEmail.setItemName("邮箱");
        String email = User.getInstance().getEmail();
        if(email == null || email.length() == 0){
            mEmail.setItemValue("未设置");
        }else{
            mEmail.setItemValue(email);
        }

        mEmail.setOnClickListener(this);
        //mEmail.setLineVisible(View.GONE);

        loginOutBtn = (Button) findViewById(R.id.login_out);
        loginOutBtn.setOnClickListener(this);
    }


    @Override
    public void onClick(View v) {
        if(v.getId() == R.id.portrait){
            modifyPortrait();
        }

        if(v.getId() == R.id.userName){
            modifyUserName();
        }

        if(v.getId() == R.id.password){
            modifyPassword();
        }

        if(v.getId() == R.id.email){
            modifyEmail();
        }

        if(v.getId() == R.id.login_out){
            loginOut();
        }
    }

    private void modifyPortrait(){
        selectPicFromLocal();
    }

    public void selectPicFromLocal() {
        Intent intent;
        if (Build.VERSION.SDK_INT < 19) {
            intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("image/*");
        } else {
            intent = new Intent(
                    Intent.ACTION_PICK,
                    android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        }
        startActivityForResult(intent, RESULT_LOAD_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RESULT_LOAD_IMAGE && resultCode == RESULT_OK && null != data) {
            Uri selectedImage = data.getData();
            String[] filePathColumn = { MediaStore.Images.Media.DATA };
            Cursor cursor = getContentResolver().query(selectedImage,
                    filePathColumn, null, null, null);
            cursor.moveToFirst();
            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            String picturePath = cursor.getString(columnIndex);
            cursor.close();

            Bitmap bitmap = User.getInstance().setUserPortrait(BitmapFactory.decodeFile(picturePath));
            if(bitmap != null){
                mPortrait.setItemValueImage(bitmap);
            }

        }
    }

    private void setPortrait(){
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    pngBM = User.getInstance().getUserPortrait();

                    Message msg = new Message();
                    msg.what = 0;
                    handler.sendMessage(msg);

                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        });
        thread.start();
    }

    private void modifyUserName(){
        AlertDialog.Builder inputDialog =  new AlertDialog.Builder(this);
        final View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_modify_username,null);
        final EditText editText = (EditText)dialogView.findViewById(R.id.edit_username);
        inputDialog.setTitle("修改用户昵称").setView(dialogView);

        inputDialog.setPositiveButton("确定",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String name = editText.getText().toString();
                        if(name != null && name.length() > 0){
                            User.getInstance().setUserName(name);
                            mUserName.setItemValue(name);
                        }
                    }
                });

        inputDialog.setNegativeButton("关闭",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //...To-do
                    }
                });

        inputDialog.show();
        editText.clearFocus();
    }

    private void modifyPassword(){
        final AlertDialog.Builder inputDialog =  new AlertDialog.Builder(this);

        final View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_modify_password,null);
        final EditText password1 = (EditText)dialogView.findViewById(R.id.edit_password1);
        final EditText password2 = (EditText)dialogView.findViewById(R.id.edit_password2);
        final EditText password3 = (EditText)dialogView.findViewById(R.id.edit_password3);

        final String password = User.getInstance().getPassword();
        if(password == null || password.length() == 0){
            final View view1 = dialogView.findViewById(R.id.layout_password1);
            view1.setVisibility(View.GONE);
        }

        final Context context = (Context) this;
        inputDialog.setTitle("设置用户密码").setView(dialogView);

        inputDialog.setPositiveButton("确定",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String msg = "";
                        boolean success = false;
                        String strPwd1 = password1.getText().toString();
                        String strPwd2 = password2.getText().toString();
                        String strPwd3 = password3.getText().toString();
                        if(password == null || password.length() == 0 || AppKit.stringToMD5(strPwd1).equals(password)){
                            if(strPwd2 != null && strPwd2.length() > 0 && strPwd2.equals(strPwd3)){
                                User.getInstance().setUserPassword(strPwd2);
                                msg = "修改密码成功";
                                success = true;
                            }
                            else {
                                if(!strPwd2.equals(strPwd3)){
                                    msg = "两次输入密码不一致";
                                }else{
                                    msg = "新密码不能为空";
                                }

                            }
                        }else{
                            msg = "原始密码错误";
                        }
                        Toast toast = Toast.makeText(context, msg, Toast.LENGTH_LONG);
                        toast.show();

                        if(!success){
                            modifyPassword();
                        }
                    }
                });

        inputDialog.setNegativeButton("关闭",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //...To-do
                    }
                });

        inputDialog.create().show();
    }

    private void modifyEmail(){
        AlertDialog.Builder inputDialog =  new AlertDialog.Builder(this);
        final View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_modify_username,null);
        final EditText editText = (EditText)dialogView.findViewById(R.id.edit_username);
        inputDialog.setTitle("修改用户邮箱").setView(dialogView);

        final Context context = (Context) this;

        inputDialog.setPositiveButton("确定",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String email = editText.getText().toString();

                        boolean flag = false;
                        try{
                            String check = "^([a-z0-9A-Z]+[-|_|\\.]?)+[a-z0-9A-Z]@([a-z0-9A-Z]+(-[a-z0-9A-Z]+)?\\.)+[a-zA-Z]{2,}$";
                            Pattern regex = Pattern.compile(check);
                            Matcher matcher = regex.matcher(email);
                            flag = matcher.matches();
                        }catch(Exception e){
                            flag = false;
                        }



                        if(email != null && email.length() > 0 && flag){
                            User.getInstance().setEmail(email);
                            mEmail.setItemValue(email);
                        }else{
                            modifyEmail();
                            Toast toast = Toast.makeText(context, "邮箱格式不正确", Toast.LENGTH_LONG);
                            toast.show();
                        }
                    }
                });

        inputDialog.setNegativeButton("关闭",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //...To-do
                    }
                });

        inputDialog.show();
        editText.clearFocus();
    }

    public void loginOut(){
        String password = User.getInstance().getPassword();
        if(password == null || password.length() == 0){
            Toast toast = Toast.makeText(this, "您还没有设置密码，必须设置密码后才能退出登录", Toast.LENGTH_LONG);
            toast.show();
            modifyPassword();
            return;
        }



        AlertDialog.Builder inputDialog =  new AlertDialog.Builder(this);
        inputDialog.setTitle("是否确定退出登陆，如退出登陆请记牢您的密码，否则无法再登入");

        final Activity context = this;
        inputDialog.setPositiveButton("确定",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        User.getInstance().loginOut();
                        Intent intent = new Intent((Activity)context, LoginActivity.class);
                        startActivityForResult(intent,1);
                        finish();

                        if(AppKit.mainActivity != null && !AppKit.mainActivity.isFinishing()){
                            AppKit.mainActivity.finish();
                        }
                    }
                });

        inputDialog.setNegativeButton("关闭",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //...To-do
                    }
                });

        inputDialog.show();
    }
}
