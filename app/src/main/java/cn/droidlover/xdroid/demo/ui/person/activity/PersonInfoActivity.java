package cn.droidlover.xdroid.demo.ui.person.activity;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import cn.droidlover.xdroid.demo.R;
import cn.droidlover.xdroid.demo.User;
import cn.droidlover.xdroid.demo.ui.PersonItem;

public class PersonInfoActivity extends AppCompatActivity implements View.OnClickListener{
    PersonItem mUserId;
    PersonItem mUserName;
    PersonItem mPortrait;
    PersonItem mPassword;
    PersonItem mEmail;

    Bitmap pngBM;

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
        mUserId.setItemValue("529548");

        mUserName = (PersonItem)findViewById(R.id.userName);
        mUserName.setItemName("昵称");
        mUserName.setItemValue("海边的卡夫卡");
        mUserName.setOnClickListener(this);

        mPassword = (PersonItem)findViewById(R.id.password);
        mPassword.setItemName("密码");
        mPassword.setOnClickListener(this);

        mEmail = (PersonItem)findViewById(R.id.email);
        mEmail.setItemName("邮箱");
        mEmail.setItemValue("未设置");
        mEmail.setOnClickListener(this);
        mEmail.setLineVisible(View.GONE);
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
        startActivityForResult(intent, 0);
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
        final EditText editText = new EditText(this);
        AlertDialog.Builder inputDialog =  new AlertDialog.Builder(this);

        inputDialog.setTitle("修改用户昵称").setView(editText);

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
    }

    private void modifyPassword(){
        final AlertDialog.Builder inputDialog =  new AlertDialog.Builder(this);

        final View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_modify_password,null);
        final EditText password1 = (EditText)dialogView.findViewById(R.id.edit_password1);
        final EditText password2 = (EditText)dialogView.findViewById(R.id.edit_password2);
        final Context context = (Context) this;
        inputDialog.setTitle("设置用户密码").setView(dialogView);

        inputDialog.setPositiveButton("确定",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String strPwd1 = password1.getText().toString();
                        String strPwd2 = password2.getText().toString();
                        if(strPwd1.equals(strPwd2)){
                            User.getInstance().setUserPassword(strPwd1);
                        }
                        else {
                            modifyPassword();
                            Toast toast = Toast.makeText(context, "两次输入的密码不一致", Toast.LENGTH_LONG);
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

        inputDialog.create().show();
    }

    private void modifyEmail(){

    }
}
