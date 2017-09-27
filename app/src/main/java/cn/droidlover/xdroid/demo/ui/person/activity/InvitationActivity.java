package cn.droidlover.xdroid.demo.ui.person.activity;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.ImageButton;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import butterknife.BindView;
import cn.droidlover.xdroid.base.XActivity;
import cn.droidlover.xdroid.demo.R;
import cn.droidlover.xdroid.demo.ui.CommonActivityHeadView;
import cn.droidlover.xdroid.demo.ui.ImageText;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

import static android.graphics.Color.WHITE;
import static android.graphics.Color.BLACK;

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


        //MultiFormatWriter writer = new MultiFormatWriter();

        String strDlgTitle = "对话框标题 - 分享文字";
        String strSubject = "我的主题";
        String strContent = "http://l.dahaiwenhua.com/APlayerAndroid.apk";
        Bitmap bitmap = null;
        try {
            bitmap = createQRCode(strContent,480);
        } catch (WriterException e) {
            e.printStackTrace();
        }

        try {
            saveBitmap(bitmap,"");
        } catch (IOException e) {
            e.printStackTrace();
        }
        //getShareAppList();

        btnShareQQ.setOnClickListener(this);
        btnShareQQSpace.setOnClickListener(this);
        btnShareWeixin.setOnClickListener(this);
        btnShareOther.setOnClickListener(this);

        /**
         * 1.分享纯文字内容
         */
        shareText(strDlgTitle, strSubject, strContent);

        /**
         * 2.分享图片和文字内容
         */
        strDlgTitle = "对话框标题 - 分享图片";
        // 图片文件路径（SD卡根目录下“1.png”图片）
        String imgPath = Environment.getExternalStorageDirectory().getPath()
                + File.separator + "test2.png";
        // 图片URI
        Uri imageUri = Uri.fromFile(new File(imgPath));
        // 分享
        //shareImg(strDlgTitle, strSubject, strContent, imageUri);
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

    private Bitmap createQRCode(String str, int widthAndHeight)
            throws WriterException {
        Hashtable<EncodeHintType, String> hints = new Hashtable<EncodeHintType, String>();
        hints.put(EncodeHintType.CHARACTER_SET, "utf-8");// 使用utf8编码
        BitMatrix matrix = new MultiFormatWriter().encode(str,
                BarcodeFormat.QR_CODE, widthAndHeight, widthAndHeight, hints);// 这里需要把hints传进去，否则会出现中文乱码
        int width = matrix.getWidth();
        int height = matrix.getHeight();
        int[] pixels = new int[width * height];

        // 上色，如果不做保存二维码、分享二维码等功能，上白色部分可以不写。至于原因，在生成图片的时候，如果没有指定颜色，其会使用系统默认颜色来上色，很多情况就会出现保存的二维码图片全黑
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (matrix.get(x, y)) {// 有数据的像素点使用黑色
                    pixels[y * width + x] = BLACK;
                } else {// 其他部分则使用白色
                    pixels[y * width + x] = WHITE;
                }
            }
        }
        //生成bitmap
        Bitmap bitmap = Bitmap.createBitmap(width, height,
                Bitmap.Config.ARGB_8888);
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
        return bitmap;
    }

    private void saveBitmap(Bitmap bitmap,String bitName) throws IOException
    {
        String imgPath = Environment.getExternalStorageDirectory().getPath()
                + File.separator + "test2.png";
        File file = new File(imgPath);
        if(file.exists()){
            file.delete();
        }
        FileOutputStream out;
        try{
            out = new FileOutputStream(file);
            if(bitmap.compress(Bitmap.CompressFormat.PNG, 90, out)){
                out.flush();
                out.close();
            }
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
}
