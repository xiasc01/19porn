package cn.droidlover.xdroid.demo.ui.person.activity;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.ImageButton;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import cn.droidlover.xdroid.base.XActivity;
import cn.droidlover.xdroid.demo.R;
import cn.droidlover.xdroid.demo.ui.CommonActivityHeadView;
import cn.droidlover.xdroid.demo.ui.ImageText;

public class InvitationActivity extends XActivity implements View.OnClickListener{

    @BindView(R.id.head_view)
    CommonActivityHeadView headView;

    @BindView(R.id.share_qq)
    ImageButton btnShareQQ;

    @BindView(R.id.share_qqspace)
    ImageButton btnShareQQSpace;

    @BindView(R.id.share_weixin)
    ImageButton btnShareWeixin;

    @BindView(R.id.share_other)
    ImageButton btnShareOther;

    @Override
    public void initData(Bundle savedInstanceState) {
        headView = (CommonActivityHeadView)findViewById(R.id.head_view);
        headView.setTitle("邀请");


        String strDlgTitle = "对话框标题 - 分享文字";
        String strSubject = "我的主题";
        String strContent = "我的分享内容";

        //getShareAppList();

        btnShareQQ.setOnClickListener(this);
        btnShareQQSpace.setOnClickListener(this);
        btnShareWeixin.setOnClickListener(this);
        btnShareOther.setOnClickListener(this);

        /**
         * 1.分享纯文字内容
         */
        //shareText(strDlgTitle, strSubject, strContent);

        /**
         * 2.分享图片和文字内容
         */
        strDlgTitle = "对话框标题 - 分享图片";
        // 图片文件路径（SD卡根目录下“1.png”图片）
        String imgPath = Environment.getExternalStorageDirectory().getPath()
                + File.separator + "test.png";
        // 图片URI
        Uri imageUri = Uri.fromFile(new File(imgPath));
        // 分享
        shareImg(strDlgTitle, strSubject, strContent, imageUri);
    }

    public List<ResolveInfo> getShareApps(Context context) {
        List<ResolveInfo> mApps = new ArrayList<ResolveInfo>();
        Intent intent = new Intent(Intent.ACTION_SEND, null);
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.setType("text/plain");
//      intent.setType("*/*");
        PackageManager pManager = context.getPackageManager();
        mApps = pManager.queryIntentActivities(intent, PackageManager.COMPONENT_ENABLED_STATE_DEFAULT);
        return mApps;
    }

    @Override
    public void onClick(View view) {
        if(view.getId() == R.id.share_qq){

        }

        if(view.getId() == R.id.share_qqspace){

        }

        if(view.getId() == R.id.share_weixin){

        }

        if(view.getId() == R.id.share_weixin){

        }
    }

    class AppInfo{
        String appPkgName = null;
        String appLauncherClassName        = null;
        String appName     = null;
        Drawable  appIcon  = null;
        public void setAppPkgName(String appPkgName){
            this.appPkgName = appPkgName;
        }

        public void setAppLauncherClassName(String appLauncherClassName){
            this.appLauncherClassName = appLauncherClassName;
        }

        public void setAppName(String appName){
            this.appName = appName;
        }

        public void setAppIcon(Drawable  appIcon){
            this.appIcon = appIcon;
        }
    }


    private List<AppInfo> getShareAppList() {
        List<AppInfo> shareAppInfos = new ArrayList<AppInfo>();
        PackageManager packageManager = getPackageManager();
        List<ResolveInfo> resolveInfos = getShareApps((Context) this);
        if (null == resolveInfos) {
            return null;
        } else {
            for (ResolveInfo resolveInfo : resolveInfos) {
                AppInfo appInfo = new AppInfo();
                appInfo.setAppPkgName(resolveInfo.activityInfo.packageName);
//              showLog_I(TAG, "pkg>" + resolveInfo.activityInfo.packageName + ";name>" + resolveInfo.activityInfo.name);
                appInfo.setAppLauncherClassName(resolveInfo.activityInfo.name);
                appInfo.setAppName(resolveInfo.loadLabel(packageManager).toString());
                appInfo.setAppIcon(resolveInfo.loadIcon(packageManager));
                shareAppInfos.add(appInfo);
            }
        }
        return shareAppInfos;
    }


    private void shareText(String dlgTitle, String subject, String content) {
        if (content == null || "".equals(content)) {
            return;
        }
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        if (subject != null && !"".equals(subject)) {
            intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        }

        intent.putExtra(Intent.EXTRA_TEXT, content);

        // 设置弹出框标题
        if (dlgTitle != null && !"".equals(dlgTitle)) { // 自定义标题
            startActivity(Intent.createChooser(intent, dlgTitle));
        } else { // 系统默认标题
            startActivity(intent);
        }
    }

    /**
     * 分享图片和文字内容
     *
     * @param dlgTitle
     *            分享对话框标题
     * @param subject
     *            主题
     * @param content
     *            分享内容（文字）
     * @param uri
     *            图片资源URI
     */
    private void shareImg(String dlgTitle, String subject, String content,
                          Uri uri) {
        if (uri == null) {
            return;
        }
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        if (subject != null && !"".equals(subject)) {
            intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        }
        if (content != null && !"".equals(content)) {
            intent.putExtra(Intent.EXTRA_TEXT, content);
        }

        // 设置弹出框标题
        if (dlgTitle != null && !"".equals(dlgTitle)) { // 自定义标题
            startActivity(Intent.createChooser(intent, dlgTitle));
        } else { // 系统默认标题
            startActivity(intent);
        }
    }

    @Override
    public void setListener() {

    }

    @Override
    public int getLayoutId() {
        return R.layout.activity_invitation;
    }
}
