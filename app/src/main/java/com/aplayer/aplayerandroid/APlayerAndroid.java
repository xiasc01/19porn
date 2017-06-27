package com.aplayer.aplayerandroid;
import java.lang.ref.WeakReference;

import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.SurfaceHolder;
import android.view.Gravity;
import android.view.TextureView;
import android.view.View;
import android.view.View.OnTouchListener;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.SurfaceTexture;
import android.app.Activity;
import android.widget.TextView;
import android.widget.FrameLayout;
import android.os.Looper;
import android.os.Handler;
import android.os.Message;
import android.media.MediaPlayer;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

public class APlayerAndroid {
	private static final String TAG 	  = APlayerAndroid.class.getSimpleName();

	private SurfaceHolder  	   mSurholder  	  				 = null;
	private View        	   mSurfaceview   				 = null;
	private TextView           mSubtilteview  				 = null;
	private Surface 	   	   mSurface 	                 = null;
	private SystemMediaPlay    mSystemPlayer 			     = null;
	
    private boolean	   	   	   mIsVrTouchRotateEnable        = false;
    private boolean        	   mIsSuccess        			 = false;
    private boolean        	   mIsAutoPlay        			 = false;
    private boolean        	   mIsCurrentUseSysmediaplay 	 = false;
    private boolean            mSubtitleShowExternal       	 = false;
    private boolean            mUseDES                       = false;
    private ByteBuffer 		   mDecryptKey = null;
    
    private int            	   mObjId                        = 0;
    public static  int     	   gObjId                        = 0;
    private boolean        	   mDestroy                      = false;
    private HardwareDecoder    mHwDecoder 	  				 = null;
    private EventHandler       mEventHandler 				 = null; 
    private String 			   mSubtitleShow 				 = "1";
    private String 			   mFileName 					 = "";
    private int                mSubtitleViewTop              = 0;
    private int                mHwReCreatePos                = 0;
	private int	   			   mBufferProgress				 = 100;
	private int                mReCreateHwCodecState         = PlayerState.APLAYER_READ;
	private AHttp              mAHttp                        = null;
	private ALocalFile         mALocalFile                   = null;
    
	public APlayerAndroid()
	{	
		Log.e(TAG,"APlayerAndroid construct");
		
		mHwDecoder = new HardwareDecoder(this);
		mObjId = gObjId++;
		
		Looper looper = Looper.myLooper();
		if(looper == null) {
			looper = Looper.getMainLooper();
		}
		if(looper != null) {
			mEventHandler = new EventHandler(this, looper);
		}
		else {
			mEventHandler = null;
		}
		
		try
		{
			System.loadLibrary("aplayer_ffmpeg_1.1.1.54");
			System.loadLibrary("aplayer_android_1.1.1.54");
		}
		catch (Exception e)
		{
			e.printStackTrace();
			Log.e(TAG,"loadLibrary aplayer_android fail" + e.toString());
		}
		native_init(new WeakReference<APlayerAndroid>(this),mObjId);
	}
	
	public void  Destroy(){
		if(mDestroy)
			return;
		
		mDestroy = true;
		new Thread(new Runnable() {
			public void run() {
				Log.e(TAG, "Destroy");
				Close();
				if(IsSystemPlayer()){
					mSystemPlayer.release();
				}
				while(GetState() != PlayerState.APLAYER_READ){
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				native_uninit(mObjId);
				
		}}).start();
	}
	
	public int SetView(Surface surface) {
		if(mDestroy) return 0;
		
		mSurface = surface;
		
		if(mSurface != null){
			native_setdisplay(mSurface,mObjId);
			
			if(IsSystemPlayer()){
				mSystemPlayer.SetView(mSurface);
			}
		}else{
			if(isHwDecode()){
				mHwDecoder.stopCodec();
			}
		}
		return 0;
	}
	
	public int SetView(TextureView textureView){
		if(mDestroy) return 0;
		
		Log.i(TAG, "SetView TextureView");
		
		mSurfaceview 	= textureView;
		
		if(textureView.isAvailable()){
			mSurface = new Surface(textureView.getSurfaceTexture());
			
			if(IsSystemPlayer()){
				mSystemPlayer.SetView(mSurface);
			}
			native_setdisplay(mSurface,mObjId);
		}
		
		textureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
			
			@Override
			public void onSurfaceTextureUpdated(SurfaceTexture surface) {
				
			}
			
			@Override
			public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width,
					int height) {
				Log.i(TAG, "SurfaceTexture SizeChanged width=" + width + ", height = " + height);
				if(mDestroy) return;	
				
				//mSurface = new Surface(surface);
				native_setdisplay(mSurface,mObjId);
				changeSubtitleViewSize();
			}
			
			@Override
			public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
				Log.v("APlayerAndroid","SurfaceTexture Destroyed");
				
				native_setdisplay(null,mObjId);
				
				if(!IsSystemPlayer() && isHwDecode()){
					Log.e(TAG, "!IsSystemPlayer() && isHwDecode()");
					mReCreateHwCodecState = GetState();
					SetConfig(APlayerAndroid.CONFIGID.AUDIO_SILENCE, "1");
					Pause();
					mHwDecoder.stopCodec();
					mHwReCreatePos = GetPosition();
				}
				
				if(mOnSurfaceDestroyListener != null)
					mOnSurfaceDestroyListener.OnSurfaceDestroy();
				
				return true;
			}
			
			@Override
			public void onSurfaceTextureAvailable(SurfaceTexture surface, int width,
					int height) {
				Log.i(TAG,"SurfaceTexture Created");
				if(mDestroy) return;
				
				mSurface = new Surface(surface);
				
				if(IsSystemPlayer()){
					mSystemPlayer.SetView(mSurface);
				}
				native_setdisplay(mSurface,mObjId);
			    
				if(mReCreateHwCodecState == PlayerState.APLAYER_PAUSED  ||
				   mReCreateHwCodecState == PlayerState.APLAYER_PAUSING ||
				   mReCreateHwCodecState == PlayerState.APLAYER_PLAY    ||
				   mReCreateHwCodecState == PlayerState.APLAYER_PLAYING){
					
				   if(isHwDecode()){
					   Log.e(TAG, "ReCreateCodec");
					   mHwDecoder.ReCreateCodec();
					   SetPosition(mHwReCreatePos);
					   
					   mHwDecoder.setOnDecoderOneFrameListener(new HardwareDecoder.OnDecoderOneFrameListener() {
							@Override
							public void onDecoderOneFrame() {
								mHwDecoder.setOnDecoderOneFrameListener(null);
								
								new Thread(new Runnable() {
									public void run() {
										try {
											Thread.sleep(500);
										} catch (InterruptedException e) {
											e.printStackTrace();
										}
										SetConfig(APlayerAndroid.CONFIGID.AUDIO_SILENCE, "0");
								}}).start();
								
								
								if(mOnReCreateHwDecoderListener != null){
									mOnReCreateHwDecoderListener.onReCreateHwDecoder();
								}
								
							}
					   });
					   
					   Play();
					   mReCreateHwCodecState = PlayerState.APLAYER_READ;
					}
				}
			}
		});
		
		return 1;
	}
	
	public int SetView(SurfaceView surfaceview){
		if(mDestroy) return 0;
		
		mSurfaceview 	= surfaceview;
		
		mSurface = surfaceview.getHolder().getSurface();
		if(!mSurface.isValid()){
			Log.i(TAG, "surface is not valid");
			mSurface = null;
		}else{
			if(IsSystemPlayer()){
				mSystemPlayer.SetView(mSurface);
			}
			native_setdisplay(mSurface,mObjId);
		}
		
		surfaceview.getHolder().addCallback(new SurfaceHolder.Callback() {
			public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
				Log.i(TAG, "surface Changed format=" + format + ", width=" + width + ", height="
			            + height);
				if(mDestroy) return;			
				native_setdisplay(mSurface,mObjId);
				changeSubtitleViewSize();
			}
			
			public void surfaceCreated(SurfaceHolder holder) {
				Log.i(TAG,"surface Created");
				if(mDestroy) return;
				
				mSurface = holder.getSurface();
				
				if(IsSystemPlayer()){
					mSystemPlayer.SetView(mSurface);
				}
				native_setdisplay(mSurface,mObjId);
			    

                int state = GetState();
				if(mReCreateHwCodecState == PlayerState.APLAYER_PAUSED  ||
				   mReCreateHwCodecState == PlayerState.APLAYER_PAUSING ||
				   mReCreateHwCodecState == PlayerState.APLAYER_PLAY    ||
				   mReCreateHwCodecState == PlayerState.APLAYER_PLAYING){
				   if(isHwDecode()){
					   Log.e(TAG, "ReCreateCodec");
					   mHwDecoder.ReCreateCodec();
					   SetPosition(mHwReCreatePos);
					   
					   mHwDecoder.setOnDecoderOneFrameListener(new HardwareDecoder.OnDecoderOneFrameListener() {
							@Override
							public void onDecoderOneFrame() {
								mHwDecoder.setOnDecoderOneFrameListener(null);
								
								new Thread(new Runnable() {
									public void run() {
										try {
											Thread.sleep(500);
										} catch (InterruptedException e) {
											e.printStackTrace();
										}
										SetConfig(APlayerAndroid.CONFIGID.AUDIO_SILENCE, "0");
								}}).start();
								
								
								if(mOnReCreateHwDecoderListener != null){
									mOnReCreateHwDecoderListener.onReCreateHwDecoder();
								}
								
							}
					   });
					   
					   Play();
					   mReCreateHwCodecState = PlayerState.APLAYER_READ;
					}
				}
			}
			
			public void surfaceDestroyed(SurfaceHolder holder) {
				Log.i("APlayerAndroid","surface Destroyed");
				
				native_setdisplay(null,mObjId);
				
				if(!IsSystemPlayer() && isHwDecode()){
					Log.e(TAG, "!IsSystemPlayer() && isHwDecode()");
					mReCreateHwCodecState = GetState();
					SetConfig(APlayerAndroid.CONFIGID.AUDIO_SILENCE, "1");
					Pause();
					mHwDecoder.stopCodec();
					mHwReCreatePos = GetPosition();
				}
				
				if(mOnSurfaceDestroyListener != null)
					mOnSurfaceDestroyListener.OnSurfaceDestroy();
				}
			});
		
		//surfaceview.setVisibility(View.INVISIBLE);
		//surfaceview.setVisibility(View.VISIBLE);//Ç¿ÆÈsurfaceCreated±»µ÷ÓÃ
		return 0;
	}
	
	public void UseSystemPlayer(boolean use){
		mIsCurrentUseSysmediaplay = use;
		if(use && (mSystemPlayer == null || !mSystemPlayer.hasMediaPlayer())){
			mSystemPlayer 	= new SystemMediaPlay();
		}
	}
	
	public boolean IsSystemPlayer(){
		return mIsCurrentUseSysmediaplay && mSystemPlayer != null && mSystemPlayer.hasMediaPlayer();
	}
	
	public int Open(String path){		
		if(mDestroy) return 0;

		//path = "http://lzmlsfe-test1.oss-cn-shenzhen.aliyuncs.com/G(Flash%20Video)_V(Main%40L3.0%2CSD)_A(LC).mp4?OSSAccessKeyId=LTAIYNOWOQy5cJTY&Expires=1484289225&Signature=6GjmTKB9iOs9238HQfyubv%2FJ1aE%3D&pos=23425&size=3435&md5=ajdfklajdfkasdakdgj";
		SetConfig(APlayerAndroid.CONFIGID.HTTP_USER_AHTTP, "1");
		
		mIsSuccess 		= false;
		mBufferProgress = 100;
		mALocalFile     = null;
		
		if(IsSystemPlayer()){
			return mSystemPlayer.open(path);
		}
		
		int ret = native_open(path,mObjId);
		if(ret == -1){
			Log.e(TAG, "throw Exception state is not right or other fatal error");
		}
		
		mHwDecoder.setOnDecoderOneFrameListener(new HardwareDecoder.OnDecoderOneFrameListener() {
			@Override
			public void onDecoderOneFrame() {
				mHwDecoder.setOnDecoderOneFrameListener(null);
				Message m = mEventHandler.obtainMessage(MsgID.FRIST_FRAME_RENDER, 0, 0, null);
				if(m == null){
					return;
				}
				m.what = MsgID.FRIST_FRAME_RENDER;
				mEventHandler.sendMessage(m);
			}
	   });
		return ret;
	}
	
	public int Open(FileDescriptor fileDescriptor){
		if(mDestroy) return 0;
		
		mIsSuccess = false;
		
		if(IsSystemPlayer()){
			//return mSystemPlayer.open(fileDescriptor);
		}
		
		SetConfig(APlayerAndroid.CONFIGID.HTTP_USER_AHTTP, "1");
		mALocalFile = new ALocalFile(fileDescriptor);
		
		int ret = native_open("c:\\",mObjId);
		if(ret == -1){
			Log.e(TAG, "throw Exception state is not right or other fatal error");
		}
		return ret;
	}
	
	public int Close(){
		if(IsSystemPlayer()){
			return mSystemPlayer.close();
		}
		
		if(isHwDecode()){
			mHwDecoder.stopCodec();
		}
		
		return native_close(mObjId);
	}
	
	public int Play(){
		if(mDestroy) return 0;
		
		createSubtitleView();
		
		if(mIsSuccess){
			if(IsSystemPlayer()){
				return mSystemPlayer.play();
			}
			
			if(isHwDecode()){
				if(!mHwDecoder.IsCodecPrepare()){
					return 0;
				}
			}
			return native_play(mObjId);
		}
		return 0;
	}
	
	public int Pause(){
		if(mDestroy) return 0;
		
		if(IsSystemPlayer()){
			return mSystemPlayer.pause();
		}
		return native_pause(mObjId);
	}
	
	public static String GetVersion(){
		return "1.1.1.54";
	}
	
	public int GetState(){
		if(mDestroy) return 0;
		
		if(IsSystemPlayer()){
			return mSystemPlayer.getState();
		}
		return native_getState(mObjId);
	}
	
	public int GetDuration(){
		if(mDestroy) return 0;
		
		if(IsSystemPlayer()){
			return mSystemPlayer.getDuration();
		}
		
		return native_getduration(mObjId);
	}
	
	public int GetPosition(){
		if(mDestroy) return 0;
		
		if(IsSystemPlayer()){
			return mSystemPlayer.getPosition();
		}
		
		return native_getposition(mObjId);
	}
	
	public int SetPosition(int msec){
		if(mDestroy) return 0;
		
		if(IsSystemPlayer()){
			return mSystemPlayer.setPosition(msec);
		}
		
		return native_setposition(msec,mObjId);
	}
	
	public int GetVideoWidth(){
		if(mDestroy) return 0;
		
		if(IsSystemPlayer()){
			return mSystemPlayer.getVideoWidth();
		}
		
		return native_getwidth(mObjId);
	}
	
	public int GetVideoHeight(){
		if(mDestroy) return 0;
		
		if(IsSystemPlayer()){
			return mSystemPlayer.getVideoHeight();
		}
		
		return native_getheight(mObjId);
	}
	
	public int GetVolume()
	{
		return 0;
	}
	
	public int SetVolume(int volume)
	{
		return 0;
	}
	
	public int GetBufferProgress()
	{
		if(mDestroy) return 0;
		
		if(IsSystemPlayer()){
			return mSystemPlayer.getBufferProgress();
		}

		return mBufferProgress;
		//return native_getbufferprogress(mObjId);
	}
	
	public String GetConfig(int configID)
	{
		if(mDestroy) return "";
		
		if(IsSystemPlayer()){
			return "";
		}
		
		switch(configID)
		{
			case CONFIGID.AUTO_PLAY:
				return config_get_auto_play();
			case CONFIGID.SUBTITLE_SHOW:
				return config_get_subtitle_show();
			case CONFIGID.SUBTITLE_FILE_NAME:
				return config_get_subtitle_file_name();
			case CONFIGID.VR_ENABLE_INNER_TOUCH_ROTATE:
				return config_get_vr_touch_rotate();
			default:
				return native_getconfig(configID,mObjId);
		}
	}
	
	public int SetConfig(int configID,String value)
	{
		if(mDestroy) return 0;
		
		if(IsSystemPlayer()){
			return 0;
		}
		
		Log.e(TAG, "SetConfig " + value);
		switch(configID)
		{
			case CONFIGID.AUTO_PLAY:
				return config_set_auto_play(value);
			case CONFIGID.SUBTITLE_SHOW:
				return config_set_subtitle_show(value);
			case CONFIGID.SUBTITLE_FILE_NAME:
				return config_set_subtitle_file_name(value);
			case CONFIGID.VR_ENABLE_INNER_TOUCH_ROTATE:
				return config_set_vr_touch_rotate(value);
			case CONFIGID.VR_ENABLE:
				boolean vr_enable = value.equalsIgnoreCase("1");
				if(!vr_enable){
					config_set_vr_touch_rotate("0");
				}
				return native_setconfig(configID,value,mObjId);
			case CONFIGID.HW_DECODER_USE:
				return config_set_hwdecode_use(value);
			case CONFIGID.HTTP_AHTTP_CACHE_DIR:
				return config_set_ahttp_cache_dir(value);
			default:
				return native_setconfig(configID,value,mObjId);
		}
	}
	
	public int SetVideoOrientation(int nOrientation)
	{
		if(mDestroy) return 0;
		
		if(IsSystemPlayer()){
			return 0;
		}
		 return native_setVideoOrientation(nOrientation,mObjId);
	}
	
	public void SetDecryKey(ByteBuffer key){
		mDecryptKey = key;
		mUseDES     = true;
	}
	
	private boolean isHwDecode(){
		return (GetConfig(CONFIGID.HW_DECODER_USE).equals("1") && GetConfig(CONFIGID.HW_DECODER_ENABLE).equals("1"));
	}
	
	private int  config_set_subtitle_show(String value){
		if(mSubtilteview == null)
    		createSubtitleView();
    
    	if(mSubtilteview == null)
    		return 0;
		
		if(value.equalsIgnoreCase("1")){
			mSubtilteview.setVisibility(TextView.VISIBLE);
			mSubtitleShow = "1";
		}else{
			mSubtilteview.setVisibility(TextView.INVISIBLE);
			mSubtitleShow = "0";
		}
		
		return 1;
	}
	
	private int  config_set_subtitle_show_external(String value)
	{
		if(value.equalsIgnoreCase("1")){
			mSubtitleShowExternal = true;
		}else{
			mSubtitleShowExternal = false;
		}
		return 1;
	}
	
	private String config_get_subtitle_show_external(){
		if(mSubtitleShowExternal){
			return "1";
		}else{
			return "0";
		}
	}
	
	private String  config_get_subtitle_show()
	{
		return mSubtitleShow;
	}
	
	private int  config_set_auto_play(String value){
		mIsAutoPlay = (value.equalsIgnoreCase("1"));
		return 1;
	}
	
	private String  config_get_auto_play(){
		return (mIsAutoPlay ? "1" : "0");
	}
	
	private int config_set_subtitle_file_name(String value){
		mFileName = value;
		return native_setconfig(CONFIGID.SUBTITLE_FILE_NAME,value,mObjId);
	}
	
	private String config_get_subtitle_file_name(){
		return mFileName;
	}
	
	private int config_set_vr_touch_rotate(String value){
		boolean vr_touch_rotate = value.equalsIgnoreCase("1");
		if(vr_touch_rotate && mSurfaceview != null)
		{
			mSurfaceview.setOnTouchListener(onTouchListener);
			mIsVrTouchRotateEnable = true;
		}
		if(!vr_touch_rotate && mSurfaceview != null)
		{
			mSurfaceview.setOnTouchListener(null);
			mIsVrTouchRotateEnable = false;
		}
		return 1;
	}
	
	private String config_get_vr_touch_rotate(){
		return (mIsVrTouchRotateEnable ? 1:0) + "";
	}
	
	private int config_set_hwdecode_use(String value){
		if(GetConfig(CONFIGID.HW_DECODER_USE).equals(value)){
			return 1;
		}
		
		int state = GetState();
		if(state == PlayerState.APLAYER_PAUSED  ||
		   state == PlayerState.APLAYER_PAUSING ||
		   state == PlayerState.APLAYER_PLAY    ||
		   state == PlayerState.APLAYER_PLAYING){
			return 0;
		}
		
		native_setconfig(CONFIGID.HW_DECODER_USE, value, mObjId);
		return 1;
	}
	
	private int config_set_ahttp_cache_dir(String value){
		if(mAHttp == null){
			mAHttp = new AHttp();
		}
		
		
		
		if(mAHttp != null){
			mAHttp.setCacheFileDir(value);
		}
		return 1;
	}
	
	public static class MediaInfo
	{
		public int width;
		public int height;
		public long duration_ms;
		public byte[] bitMap;
		
		public static Bitmap byteArray2BitMap(byte[] bitMap){
			Bitmap bitmap = BitmapFactory.decodeByteArray(bitMap, 0, bitMap.length);
			return bitmap;
		}
	}

	public MediaInfo parseThumbnail(String mediaPath, long timeMs, int width, int height)
	{
		MediaInfo mediaInfo = new MediaInfo();
		if(0 != native_thumbnail_parse(mediaInfo, mediaPath, timeMs, width, height))
		{
			mediaInfo = null;
		}
		
		//mediaInfo.bitMap = new byte[10];
		return mediaInfo;
	}
	
	public boolean isSupportRecord()
	{
		if(mDestroy)  return false;
		
		int retval = native_is_support_record(mObjId);
		return (0 != retval);
	}
	
	public boolean startRecord(String outMediaPath)
	{
		if(mDestroy)  return false;
		
		if(null == outMediaPath || outMediaPath.isEmpty()){
			return false;
		}

		int dirEndPos = outMediaPath.lastIndexOf('\\');
		dirEndPos = (-1 == dirEndPos) ? outMediaPath.lastIndexOf('/') : dirEndPos;
		if(-1 == dirEndPos){
			return false;
		}
		
		String dirPath = outMediaPath.substring(0, dirEndPos); 
		if(!isFolderExists(dirPath)){
			return false;
		}
		
		
		int retval = native_start_record(outMediaPath, mObjId);
		return (0 == retval);
		
	}
	
	public boolean isRecording()
	{
		if(mDestroy)  return false;
		
		int retval = native_is_recording(mObjId);
		return (0 != retval);
	}
	
	public void endRecord(){
		if(mDestroy)  return;
		
		native_end_record(mObjId);
	}
	
	public void stopRead(boolean stopRead){
		if(mDestroy) return;
		
		native_stop_read(stopRead,mObjId);
	}
	
	private native int    native_open(String strPath,int objid);
	
	private native int    native_close(int objid);
	
	private native int    native_play(int objid);
	
	private native int    native_pause(int objid);
	
	private native int 	  native_getState(int objid);
	
	private native int    native_setposition(int msec,int objid);
	
	private native int    native_getposition(int objid);
	
	private native int    native_getwidth(int objid);
	
	private native int    native_getheight(int objid);
	
	private native String native_getconfig(int configID,int objid);
	
	private native int 	  native_setconfig(int configID,String value,int objid);
	
	private native int 	  native_setdisplay(Surface surface,int objid);
	
	private native int 	  native_init(Object aplayer_this,int objid);
	
	private native int 	  native_uninit(int objid);
	
	private native int 	  native_getduration(int objid);
	
	private native int 	  native_setVideoOrientation(int nOrientation,int objid);
	
	private native int    native_isseeking(int objid);
	
	private native int    native_getbufferprogress(int objid);
    
	private native int    native_rotate(float angley,float anglex,int objid);
	
	private native int 	  native_thumbnail_parse(Object outMediaInfo, String mediaPath, long timeMs, int width, int height);
	
	private native int	  native_is_support_record(int objid);
	
	private native int	  native_start_record(String mediaOutPath, int objid);
	
	private native int	  native_is_recording(int objid);
	
	private native int	  native_end_record(int objid);
	
	private native int    native_stop_read(boolean stopRead,int objid);
	
    private int openSuccess(){
		mIsSuccess = true;
		if(mIsAutoPlay){
			Play();
		}
		
		if(mOnOpenSuccessListener != null){
			mOnOpenSuccessListener.onOpenSuccess();
		}
		if(mOnOpenCompleteListener != null){
			mOnOpenCompleteListener.onOpenComplete(true);
		}
		return 1;
	}
    
    private void stateChange(int preState,int curState,Object obj){		
		if(preState == APlayerAndroid.PlayerState.APLAYER_CLOSEING &&
		   curState == APlayerAndroid.PlayerState.APLAYER_READ){
			
			if(isHwDecode()){
				mHwDecoder.stopCodec();
			}
			
			if(mOnPlayCompleteListener != null){
				mOnPlayCompleteListener.onPlayComplete((String)obj);
			}
			
			if(((String)obj).equals(PlayCompleteRet.PLAYRE_RESULT_OPENRROR)){
				if(mOnOpenCompleteListener != null){
					mOnOpenCompleteListener.onOpenComplete(false);
				}
			}
			
			if(mALocalFile != null){
				SetConfig(APlayerAndroid.CONFIGID.HTTP_USER_AHTTP, "0");
				mALocalFile = null;
			}
			
			
			Log.e(TAG, "Event mOnPlayCompleteListener result = " +(String)obj);
		}
	}
    
    public interface OnReCreateHwDecoderListener{
		void onReCreateHwDecoder();
	}
    public void setOnReCreateHwDecoderListener(OnReCreateHwDecoderListener listener){
    	mOnReCreateHwDecoderListener = listener;
    }
    private OnReCreateHwDecoderListener mOnReCreateHwDecoderListener;
    
    public interface OnFirstFrameRenderListener{
		void onFirstFrameRender();
	}
    public void setOnFirstFrameRenderListener(OnFirstFrameRenderListener listener){
    	mOnFirstFrameRenderListener = listener;
    }
    private OnFirstFrameRenderListener mOnFirstFrameRenderListener;
    
    
    public interface OnOpenSuccessListener{
		void onOpenSuccess();
	}
    public void setOnOpenSuccessListener(OnOpenSuccessListener listener){
    	mOnOpenSuccessListener = listener;
    }
    private OnOpenSuccessListener mOnOpenSuccessListener;
    
    
    public interface OnPlayStateChangeListener{
		void onPlayStateChange(int nCurrentState,int nPreState);
	}
    public void setOnPlayStateChangeListener(OnPlayStateChangeListener listener){
    	mOnPlayStateChangeListener = listener;
    }
    private OnPlayStateChangeListener mOnPlayStateChangeListener;
    
    
    public interface OnOpenCompleteListener{
    	void onOpenComplete(boolean isOpenSuccess);
    }
    public void setOnOpenCompleteListener(OnOpenCompleteListener listener){
    	mOnOpenCompleteListener = listener;
    }
    private OnOpenCompleteListener mOnOpenCompleteListener;
    
   
	public interface OnPlayCompleteListener{
    	void onPlayComplete(String playRet);
    }
    public void setOnPlayCompleteListener(OnPlayCompleteListener listener){
    	mOnPlayCompleteListener = listener;
    }
    private OnPlayCompleteListener mOnPlayCompleteListener;
    
    public interface OnBufferListener{
   		void onBuffer(int progress);
   	}
    public void setOnBufferListener(OnBufferListener listener){
    	mOnBufferListener = listener;
    }
    private OnBufferListener mOnBufferListener;
     
    
    public interface OnSeekCompleteListener{
    	void onSeekComplete();
    }
    public void setOnSeekCompleteListener(OnSeekCompleteListener listener){
    	mOnSeekCompleteListener = listener;
    }
    private OnSeekCompleteListener mOnSeekCompleteListener;
    
    
    public interface OnSurfaceDestroyListener{
    	void OnSurfaceDestroy();
    }
    public void setOnSurfaceDestroyListener(OnSurfaceDestroyListener listener){
    	mOnSurfaceDestroyListener = listener;
    }
    private OnSurfaceDestroyListener mOnSurfaceDestroyListener;
    
    
    public interface OnSystemPlayerFailListener{
    	void OnSystemPlayerFail();
    }
    public void setOnSystemPlayerFailListener(OnSystemPlayerFailListener listener){
    	OnSystemPlayerFailListener = listener;
    }
    private OnSystemPlayerFailListener OnSystemPlayerFailListener;
    
    
    
    
    public interface OnShowSubtitleListener{
    	void OnShowSubtitle(String subtitle);
    }
    public void setOnShowSubtitleListener(OnShowSubtitleListener listener){
    	mOnShowSubtitleListener = listener;
    }
    private OnShowSubtitleListener mOnShowSubtitleListener;

    
    
    
    private void showSubtitle(CharSequence text)
    {    	
    	Log.e(TAG, "ShowSubtitle " + text.toString());
    	if(mSubtilteview == null)
    		createSubtitleView();
    
    	if(mSubtilteview == null)
    		return;
    	
    	mSubtilteview.setText(text);
    	int lineCount  = mSubtilteview.getLineCount();
    	if(lineCount < 1)
    		lineCount = 1;
    	
    	int textViewHeight = lineCount * mSubtilteview.getLineHeight();
    	FrameLayout.LayoutParams lytp = (FrameLayout.LayoutParams)mSubtilteview.getLayoutParams();
    	if(lytp == null){
    		return;
    	}
    	
    	lytp.topMargin    = mSubtitleViewTop - textViewHeight;
    	Log.e(TAG, "ShowSubtitle mSubtitleViewTop = " + mSubtitleViewTop + " textViewHeight =  " + textViewHeight);
    	mSubtilteview.setLayoutParams(lytp);
    	mSubtilteview.setHeight(textViewHeight);
    }
    
    private void changeSubtitleViewSize()
    {
    	Log.i(TAG, "ChangeSubtitleViewSize");
    	if(mSubtilteview == null){
    		return;
    	}
    	
    	if(mSurfaceview == null || mSurfaceview.getWidth() == 0 || mSurfaceview.getBottom() == 0)
			return ;
			
    	Context context = mSurfaceview.getContext();
		if (context instanceof Activity)
		{
			FrameLayout.LayoutParams lytp = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT,FrameLayout.LayoutParams.WRAP_CONTENT);
	    	lytp.leftMargin   = mSurfaceview.getLeft();
	    	lytp.topMargin    = mSurfaceview.getBottom();
	    	mSubtitleViewTop  = lytp.topMargin;
	    	int surfaceWidth  = mSurfaceview.getWidth();
    		mSubtilteview.setWidth(surfaceWidth);
    		mSubtilteview.setGravity(Gravity.CENTER);
    		mSubtilteview.setTextSize(TypedValue.COMPLEX_UNIT_PX,40);
		    mSubtilteview.setLayoutParams(lytp);
		    mSubtilteview.setVisibility(TextView.VISIBLE);
		}
    	
    }
    
    private boolean createSubtitleView()
    {
    	Log.i(TAG, "CreateSubtitleView");
    	if(mSubtilteview == null)
    	{
    		if(mSurfaceview == null || mSurfaceview.getWidth() == 0 || mSurfaceview.getBottom() == 0)
    			return false;
    		
    		Context context = mSurfaceview.getContext();
    		mSubtilteview = new TextView(context);
    		
    		if (context instanceof Activity)
    		{
    			FrameLayout.LayoutParams lytp = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT,FrameLayout.LayoutParams.WRAP_CONTENT);
    	    	lytp.leftMargin   = 0;
    	    	lytp.topMargin    = 0;
    		    Activity activity = (Activity)context;
    		    activity.addContentView(mSubtilteview, lytp);
    		    mSubtilteview.setTextColor(Color.rgb(255, 255, 255));
    		    mSubtilteview.setText("");
    		}
    		changeSubtitleViewSize();
    	}
    	return true;
    }

    protected Surface getInnerSurface(){
    	Log.i(TAG, "surface getInnerSurface");
    	while(true){
    		if(mSurface != null){
    			return mSurface;
    		}
    		try {
				Thread.sleep(100);
			} catch (Exception e) {
				// TODO: handle exception
			}
    	}
	}
    
    private HardwareDecoder getHardwareDecoder(){
		return  mHwDecoder;
	}

	private static int callFNFindHardwareDecoder(Object mediaplayer_ref,int codeid){
		APlayerAndroid amp = (APlayerAndroid)((WeakReference)mediaplayer_ref).get();
		if(amp != null && amp.getHardwareDecoder() != null){
			return amp.getHardwareDecoder().FindHardWareDecoder(codeid);
		}
		return -1;
	}

	private static int callFNCreateHardwareDecoder(Object mediaplayer_ref,Object csd){
		APlayerAndroid amp = (APlayerAndroid)((WeakReference)mediaplayer_ref).get();
		ByteBuffer bbcsd = (ByteBuffer)csd;
		if(amp != null && amp.getHardwareDecoder() != null){
			return  amp.getHardwareDecoder().CreateCodec(bbcsd);
		}
		return -1;
	}

	private static int callFNHardwareDecode(Object mediaplayer_ref,Object bufIn,long timeSample,Object bufOut){
		ByteBuffer bBufIn = (ByteBuffer)bufIn;
		ByteBuffer bBufOut = (ByteBuffer)bufOut;
		APlayerAndroid amp = (APlayerAndroid)((WeakReference)mediaplayer_ref).get();
		if(amp != null && amp.getHardwareDecoder() != null){
			return  amp.getHardwareDecoder().Decode(bBufIn, timeSample, bBufOut);
		}
		return -1;
	}

	private static int callFNFlushHardwareDecoder(Object mediaplayer_ref){
		APlayerAndroid amp = (APlayerAndroid)((WeakReference)mediaplayer_ref).get();
		if(amp != null && amp.getHardwareDecoder() != null){
			amp.getHardwareDecoder().FlushCodec();
		}
		return 1;
	}
	
	private static int callFNCloseHardwareDecoder(Object mediaplayer_ref){
		APlayerAndroid amp = (APlayerAndroid)((WeakReference)mediaplayer_ref).get();
		if(amp != null && amp.getHardwareDecoder() != null){
			amp.getHardwareDecoder().stopCodec();
		}
		return 1;
	}
    
	
	private static int callFNAhttpOpen(Object mediaplayer_ref,Object url){
		APlayerAndroid amp = (APlayerAndroid)((WeakReference)mediaplayer_ref).get();
		if(amp != null){
			
			if(amp.mALocalFile != null){
				return amp.mALocalFile.open();
			}
			
			if(amp.mAHttp == null){
				amp.mAHttp = new AHttp();
			}
			
			if(amp.mAHttp != null){
				return amp.mAHttp.open((String)url);
			}
		}
		return -1;
	}

	private static int callFNAhttpClose(Object mediaplayer_ref){
		APlayerAndroid amp = (APlayerAndroid)((WeakReference)mediaplayer_ref).get();
		
		if(amp != null && amp.mALocalFile != null){
			return amp.mALocalFile.close();
		}
		
		if(amp != null && amp.mAHttp != null){
			return amp.mAHttp.close();
		}
		return -1;
	}
	
	private static int callFNAhttpRead(Object mediaplayer_ref,Object bufOut){
		APlayerAndroid amp = (APlayerAndroid)((WeakReference)mediaplayer_ref).get();
		
		if(amp != null && amp.mALocalFile != null){
			return amp.mALocalFile.read(bufOut);
		}
		
		if(amp != null && amp.mAHttp != null){
			return amp.mAHttp.read(bufOut);
		}
		return  -1;
	}
	
	private static long callFNAhttpSeek(Object mediaplayer_ref,long offset, int whence){
		APlayerAndroid amp = (APlayerAndroid)((WeakReference)mediaplayer_ref).get();
		
		if(amp != null && amp.mALocalFile != null){
			return amp.mALocalFile.seek(offset,whence);
		}
		
		if(amp != null && amp.mAHttp != null){
			return amp.mAHttp.seek(offset,whence);
		}
		return  -1;
	}
	
    private OnTouchListener onTouchListener = new OnTouchListener() {
        float lastX, lastY;
        float angleX, angleY;
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                break;

            case MotionEvent.ACTION_POINTER_DOWN:
                break;

            case MotionEvent.ACTION_POINTER_UP:
                break;

            case MotionEvent.ACTION_UP:
                break;

            case MotionEvent.ACTION_MOVE:
            	float dx = event.getRawX() - lastX;
                float dy = event.getRawY() - lastY;

                float a = 180.0f / 320;
                
                angleX += dx * a;
                angleY += (dy * a / 2);
                if(angleY > 90.0)
                	angleY = 90.0f;
                if(angleY < -90.0)
                	angleY = 90.0f;
                
                Log.e(TAG, "move anglex = " + angleX);
                Log.e(TAG, "move angley = " + angleY);
                String angle = angleX + ";" + angleY; 
                if(mDestroy) return true;
                native_setconfig(CONFIGID.VR_ROTATE,angle,mObjId);
                break;
            }

            lastX = (int) event.getRawX();
            lastY = (int) event.getRawY();
            return true;
        }
    };
    
    private static void postEventFromNative(Object mediaplayer_ref,
            int what, int arg1, int arg2, Object obj){
    	
    	if(mediaplayer_ref == null){
    		return;
    	}
    	
	  	APlayerAndroid mp = (APlayerAndroid)((WeakReference)mediaplayer_ref).get();
		if (mp == null) {
			return;
		}
	
		if (mp.mEventHandler != null) 
		{
			Message m = mp.mEventHandler.obtainMessage(what, arg1, arg2, obj);
			if(m == null){
				return;
			}
			m.arg1 = arg1;
			m.arg2 = arg2;
			m.obj  = obj;
			mp.mEventHandler.sendMessage(m);
		}
	}
    
    private class EventHandler extends Handler{

		APlayerAndroid mp = null;
		public EventHandler(APlayerAndroid mp, Looper looper) {
			super(looper);
			this.mp = mp;
		}
		
		public void handleMessage(Message msg){
			switch(msg.what) 
			{
			case MsgID.PLAY_OPEN_SUCCESSDED:
				Log.e(TAG, "PLAY_OPEN_SUCCESSDED is received");
				openSuccess();
				break;
			case MsgID.PLAY_STATECHANGED:
				if(mOnPlayStateChangeListener != null)
					mOnPlayStateChangeListener.onPlayStateChange(msg.arg1, msg.arg2);
				
				stateChange(msg.arg2,msg.arg1,msg.obj);
				break;
			case MsgID.GET_BUFFERPRO:
				if (mOnBufferListener != null) {
					mBufferProgress = msg.arg1;
					mOnBufferListener.onBuffer(msg.arg1);
				}
				break;
			case MsgID.SEEK_COMPLETE:
				if(mOnSeekCompleteListener != null){
					mOnSeekCompleteListener.onSeekComplete();
				}
				break;
			case MsgID.SHOW_SUBTITLE:
				if (msg.obj instanceof String)
				{
					String text = (String)msg.obj;
					text = subtitleFormat(text);
					if(mOnShowSubtitleListener != null){
						mOnShowSubtitleListener.OnShowSubtitle(text);
					}
					if(!mSubtitleShowExternal)
						showSubtitle(text);
				}
				break;
			case MsgID.FRIST_FRAME_RENDER:
				if(mOnFirstFrameRenderListener != null){
					mOnFirstFrameRenderListener.onFirstFrameRender();
				}
				break;
			default:
				Log.e(TAG, "Unknown message tyupe " + msg.what);
				break;
			}
		}
	}
	
    private static String subtitleFormat(String strSubtitle)
    {
    	if(null == strSubtitle)
    		return null;
    	
    	
    	final String bracketsStart = "{";
    	final String bracketsEnd = "}";
    	final String angleBracketsStart = "<";
    	final String angleBracketsEnd = ">";
    	
    	String formatSubtitle = subtitleFormat(strSubtitle, bracketsStart, bracketsEnd);
    	formatSubtitle = subtitleFormat(formatSubtitle,angleBracketsStart, angleBracketsEnd);
    	
    	return formatSubtitle;
    }
    
    private static String subtitleFormat(String strSubtitle, String strFilterStart, String strFilterEnd)
    {
    	int startPos = strSubtitle.indexOf(strFilterStart);
    	boolean isContainsStartStr = (-1 != startPos);
    	int endPos = strSubtitle.indexOf(strFilterEnd);
    	boolean isContainsEndStr = (-1 != endPos);
    	if(!isContainsStartStr || !isContainsEndStr || startPos >= endPos)
    	{
    		return strSubtitle;
    	}
    	
    	String formatStr = strSubtitle.substring(0, startPos) + strSubtitle.substring(endPos + 1);
    	return formatStr;
    }
    
    private class MsgID
	{
		public static final int PLAY_OPEN_SUCCESSDED 	= 1;
		public static final int PLAY_STATECHANGED       = 5;
		public static final int SEEK_COMPLETE			= 6;
		public static final int GET_BUFFERPRO          	= 102;
		public static final int SHOW_SUBTITLE           = 103;
		public static final int FRIST_FRAME_RENDER      = 104;
	}
	
	public class Orientation
	{
		public static final String VIDEO_ORIENTARION_NORM      = "NORM";
		public static final String VIDEO_ORIENTARION_LEFT90    = "LEFT90";
		public static final String VIDEO_ORIENTARION_RIGHT90   = "RIGHT90";
		public static final String VIDEO_ORIENTARION_LEFT180   = "LEFT180";
	}
	
	public class PlayerState
	{
		public static final int APLAYER_READ   		= 0;
		public static final int APLAYER_OPENING   	= 1;
		public static final int APLAYER_PAUSING   	= 2;
		public static final int APLAYER_PAUSED   	= 3;
		public static final int APLAYER_PLAYING   	= 4;
		public static final int APLAYER_PLAY   		= 5;
		public static final int APLAYER_CLOSEING   	= 6;
		public static final int APLAYER_RESET   	= 7;
	}
	
	public class PlayCompleteRet{
		public static final String PLAYRE_RESULT_COMPLETE       	= "0x0";//鎾斁缁撴潫
		public static final String PLAYRE_RESULT_CLOSE          	= "0x1";//鎵嬪姩鍏抽棴
		public static final String PLAYRE_RESULT_OPENRROR       	= "0x80000001";
		public static final String PLAYRE_RESULT_SEEKERROR      	= "0x80000002";
		public static final String PLAYRE_RESULT_READEFRAMERROR 	= "0x80000003";
		public static final String PLAYRE_RESULT_CREATEGRAPHERROR 	= "0x80000004";
		public static final String PLAYRE_RESULT_DECODEERROR    	= "0x80000005";
		public static final String PLAYRE_RESULT_HARDDECODERROR     = "0x80000006";
	}
	
	public class CONFIGID
	{
		public static final int PLAYRESULT          	= 7;
		public static final int AUTO_PLAY               = 8;
		public static final int READPOSITION   			= 31;
		public static final int UPDATEWINDOW        	= 40;		
		public static final int ORIENTATION   			= 41;
		public static final int RECOR_DMODE   			= 42;
		public static final int PLAY_SPEED              = 104;
		public static final int ASPECT_RATIO_NATIVE     = 203;
		public static final int ASPECT_RATIO_CUSTOM     = 204;
		
		public static final int HW_DECODER_USE          = 209;
		public static final int HW_DECODER_ENABLE       = 230;
		public static final int HW_DECODER_DETEC        = 231;
		
		public static final int AUDIO_TRACK_LIST   		= 402;
		public static final int AUDIO_TRACK_CURRENT   	= 403;
		public static final int AUDIO_SILENCE           = 420;
		
		public static final int SUBTITLE_USABLE         = 501;
		public static final int SUBTITLE_EXT_NAME       = 502;
		public static final int SUBTITLE_FILE_NAME      = 503;
		public static final int SUBTITLE_SHOW           = 504;
		public static final int SUBTITLE_LANGLIST       = 505;
		public static final int SUBTITLE_CURLANG        = 506;
		public static final int SUBTITLE_SHOW_EXTERNAL  = 507;
		
		public static final int HTTP_COOKIE   			= 1105;
		public static final int HTTP_REFERER   			= 1106;
		public static final int HTTP_CUSTOM_HEADERS   	= 1107;
		public static final int HTTP_USER_AGENT   		= 1108;
		public static final int HTTP_USER_AHTTP   		= 1109;
		public static final int HTTP_AHTTP_CACHE_DIR   	= 1110;
		
		public static final int NET_BUFFER_ENTER        = 1001;
		public static final int NET_BUFFER_LEAVE        = 1002;
		public static final int NET_BUFFER_READ         = 1003;
		public static final int NET_BUFFER_READ_TIME    = 1005;
		public static final int NET_SEEKBUFFER_WAITTIME = 1004;
		
		public static final int VR_ENABLE 				= 2401;
		public static final int VR_ROTATE 				= 2411;
		public static final int VR_FOVY					= 2412;
		public static final int VR_ENABLE_INNER_TOUCH_ROTATE  = 2414;
	}
	
	
	private class ALocalFile{
		private FileDescriptor   		mFileDescriptor   		= null;
		private FileChannel				mFileChannel            = null;
		private FileInputStream 		mFileInputStream        = null;
		
		private long                    mCurPos                 = 0;
		private long                    mFileSize               = 0;
		
		public ALocalFile(FileDescriptor fileDescriptor){
			mFileDescriptor = fileDescriptor;
		}
		
		public synchronized int open(){
			Log.i(TAG, "ALocalFile open");
			
			mCurPos = 0;
			
			try {
				if(!mFileDescriptor.valid())
					return -1;
				
				mFileInputStream = new FileInputStream(mFileDescriptor);
				if(mFileInputStream != null){
					mFileChannel =  mFileInputStream.getChannel();
				}
					
				
				if(mFileChannel != null){
					mFileSize = mFileChannel.size();
					return mFileChannel.isOpen() ? 1 : -1;
				}
			} catch (Exception e) {
				e.printStackTrace();
				return -1;
			}
			return -1;
		}
		
		public int close(){
			if(mFileChannel != null){
				try {
					mFileChannel.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			
			if(mFileInputStream != null){
				try {
					mFileInputStream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			return 1;
		}
		
		public int read(Object bufOut){
			synchronized(mFileChannel){
				ByteBuffer bbfout = ((ByteBuffer) bufOut);
				bbfout.position(0);
				int readByte = 0;
				try {
					readByte = mFileChannel.read(bbfout);
				} catch (Exception e) {
					e.printStackTrace();
					return -1;
				}
				bbfout.flip();
				Log.i(TAG,"ALocalFile read " + readByte);
				if(readByte != -1){
					mCurPos += readByte; 
				}
				return readByte;
			}

		}
		
		public long seek(long offset, int whence){
			Log.i(TAG,"ALocalFile seek");
			synchronized(mFileChannel){
				if(0x10000 == whence){
					return mFileSize;
				}
				
				try{
					if(0 == whence){
						mCurPos = offset;
					}else if(1 == whence){
						mCurPos +=  offset;
					}else if(2 == whence){
						mCurPos = mFileSize - offset;
					}else{
						Log.e(TAG, "Ahttp seek whence = " + whence);
						return -1;
					}
					Log.i(TAG,"ALocalFile seek mCurPos = " + mCurPos);
					if(mFileDescriptor.valid()){
						mFileChannel.position(mCurPos);
					}else{
						Log.i(TAG,"ALocalFile seek mFileDescriptor is not valid");
						return -1;
					}
					
				}
				catch (Exception e) {
					Log.e(TAG,"ALocalFile seek Exception");
					e.printStackTrace();
					return -1;
				}

				return 1;
			}
		}
		
		
	}
	
	private class SystemMediaPlay{
		private MediaPlayer mPlayer;
		private int         mState = PlayerState.APLAYER_READ;
		private String      mMediaPath;
		private int         mBufferProgress = 0;

		public SystemMediaPlay() {
			mPlayer 	= new MediaPlayer();
			//mPlayer.setScreenOnWhilePlaying(true);
			
			if(mPlayer != null){
				mPlayer.setOnSeekCompleteListener(new MediaPlayer.OnSeekCompleteListener() {
					public void onSeekComplete(MediaPlayer mp) {
						Log.i(TAG, "mediaplayer setOnSeekCompleteListener");
						
						if(mOnSeekCompleteListener != null)
							mOnSeekCompleteListener.onSeekComplete();
					}
				});
				
				mPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
					public boolean onError(MediaPlayer mp, int what, int extra) {
						Log.i(TAG, "mediaplayer onError what "+ what + "extra = " + extra);
						
						release();
						onSystemMediaPlayerError();
						return false;
					}
				});
				
				mPlayer.setOnBufferingUpdateListener(new MediaPlayer.OnBufferingUpdateListener() {
	    			public void onBufferingUpdate(MediaPlayer mp, int percent) {
	    				Log.i(TAG, "mediaplayer setOnBufferingUpdateListener");
	    				
	    				mBufferProgress = percent;
	    				
	    				if(mOnBufferListener != null)
	    					mOnBufferListener.onBuffer(percent);
	    			}
	    		} );
				
				mPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
					public void onCompletion(MediaPlayer mp) {
						Log.i(TAG, "mediaplayer setOnCompletionListener");
						
						mState = PlayerState.APLAYER_READ;
						
						if(mOnPlayCompleteListener != null)
							mOnPlayCompleteListener.onPlayComplete(PlayCompleteRet.PLAYRE_RESULT_COMPLETE);
					}
				});
				
				mPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
	    			public void onPrepared(MediaPlayer mp) {
	    				Log.i(TAG, "mediaplayer setOnPreparedListener");
	    				
	    				if(mState == PlayerState.APLAYER_RESET){
	    					mPlayer.start();
	    					mState = PlayerState.APLAYER_PLAYING;
	    				}else{
	    					mState = PlayerState.APLAYER_PAUSED;
	        				mIsSuccess = true;
	        				
	        				openSuccess();
	    				}
	    			}
	    		});
	    	}
		}
		
		public boolean hasMediaPlayer() {
			if(mPlayer == null){
				return false;
			}
			return true;
		}
		
		public int release(){
			if(mState != PlayerState.APLAYER_READ){
				close();
			}
			mPlayer.release();
			mPlayer = null;
			return 1;
		}
		
		
		public int SetView(Surface surface){
			mPlayer.setSurface(surface);
			return 1;
		}
		
		public int open(String path) {
			Log.i(TAG, "SystemMediaPlay open enter");
			
			mMediaPath = path;
			
			if(mState != PlayerState.APLAYER_READ){
				return -1;
			}
			mState = PlayerState.APLAYER_OPENING;
				
			try
        	{
				mPlayer.setDataSource(mMediaPath);
				mPlayer.prepareAsync();
        	}
        	catch(IOException e){
        		Log.i(TAG,"SystemMediaPlay IOException: " + e.toString());
        		
        		if(OnSystemPlayerFailListener != null){
        			OnSystemPlayerFailListener.OnSystemPlayerFail();
        		}
        		
        		release();
        		int ret = native_open(path,mObjId);
        		if(ret == -1){
        			throw new RuntimeException("state is not right or other fatal error");
        		}

        		return ret;
        		
        	}
			return 1;
		}
		
		public int close() {
			Log.i(TAG, "SystemMediaPlay close enter");
			
			if(mState == PlayerState.APLAYER_READ)
				return -1;
			
			mState = PlayerState.APLAYER_CLOSEING;
			mPlayer.stop();
			mState = PlayerState.APLAYER_READ;
			return 1;
		}
		
		public int play() {
			Log.i(TAG, "SystemMediaPlay play enter");
			
			if(mState == PlayerState.APLAYER_RESET){
				try{
					Log.e(TAG, "ReSet DataSource");
					mState = PlayerState.APLAYER_OPENING;
					mPlayer.setDataSource(mMediaPath);
					mPlayer.prepareAsync();
				}
				catch(IOException e){
					
				}	
			}
			
			
			if(mState == PlayerState.APLAYER_PAUSED){
				mPlayer.start();
				mState = PlayerState.APLAYER_PLAYING;	
			}
			return 0;
		}
		
		public int pause() {
			mState = PlayerState.APLAYER_PAUSING;
			mPlayer.pause();
			mState = PlayerState.APLAYER_PAUSED;
			return 1;
		}
		
		public int getDuration() {
			if(mState != PlayerState.APLAYER_READ){
				return mPlayer.getDuration();
			}else{
				return 0;
			}
		}
		
		public int getPosition() {
			if(mState != PlayerState.APLAYER_READ){
				return mPlayer.getCurrentPosition();
			}else{
				return 0;
			}
		}
		
		public int setPosition(int msec) {
			if(mState != PlayerState.APLAYER_READ){
				mPlayer.seekTo(msec);
			}
			 return 1;
		}
		
		public int getVideoWidth(){
			if(mState != PlayerState.APLAYER_READ){
				return mPlayer.getVideoWidth();
			}else{
				return 0;
			}
		}
		
		public int getVideoHeight(){
			if(mState != PlayerState.APLAYER_READ){
				return mPlayer.getVideoHeight();
			}else{
				return 0;
			}
		}
		
		public int getBufferProgress() {
			return mBufferProgress;
		}
		
		public int getState() {
			return mState;
		}
		
		private int onSystemMediaPlayerError(){
    		int ret = native_open(mMediaPath,mObjId);
    		
    		if(OnSystemPlayerFailListener != null){
    			OnSystemPlayerFailListener.OnSystemPlayerFail();
    		}
    		
    		return ret;
		}
	}
	
	private static boolean isFolderExists(final String strFolder) {
		if(null == strFolder || strFolder.isEmpty()){
			return false;
		}
		
        File file = new File(strFolder);        
        if (!file.exists()) {
            if (file.mkdirs()) {                
                return true;
            } else {
                return false;

            }
        }
        return true;

    }
}
