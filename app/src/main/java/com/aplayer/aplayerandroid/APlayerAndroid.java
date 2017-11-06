package com.aplayer.aplayerandroid;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.TextureView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.aplayer.hardwareencode.HardwareEncoder;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.channels.FileChannel;

import cn.droidlover.xdroid.demo.App;

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
	private HardwareEncoder    mHardwareEncoder 			 = null;
	private int                mViewSurfaceWidth             = 0;
	private int                mViewSurfaceHeight            = 0;
	private int                mReCreateHwCodecState         = PlayerState.APLAYER_READ;
	private AHttp              mAHttp                        = null;
	private ALocalFile         mALocalFile                   = null;
	private boolean            mUpdateSurfaceView            = false;
	private String             mSilenceAudio                 = "0";
	private GPUImageFilter     mGpuImageFilter               = null;
	public APlayerAndroid()
	{
        Log.e(TAG,"APlayerAndroid construct");
		synchronized (APlayerAndroid.class)
		{
			mObjId = gObjId++;
		}

		mHwDecoder = new HardwareDecoder(this);

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
			System.loadLibrary("aplayer_ffmpeg_1.2.1.127");
			System.loadLibrary("aplayer_android_1.2.1.127");
		}
		catch (Exception e)
		{
			Log.e(TAG,"loadLibrary aplayer_android fail" + e.toString());
		}
		native_init(new WeakReference<APlayerAndroid>(this),mObjId);
		this.openLog(true);
	}
	
	public void  destroy(){
		if(mDestroy)
			return;

		mDestroy = true;
		new Thread(new Runnable() {
			public void run() {
				//Log.e(TAG, "Destroy");
				close();
				if(isSystemPlayer()){
					mSystemPlayer.release();
				}
				while(getState() != PlayerState.APLAYER_READ){
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				
				native_uninit(mObjId);
		}}).start();
	}

	
	public int setView(Surface surface) {
		if(mDestroy) return 0;
		
		mSurface = surface;
		
		if(mSurface != null){
			native_setdisplay(mSurface,mObjId);
			
			if(isSystemPlayer()){
				mSystemPlayer.SetView(mSurface);
			}
		}else{
			if(isHwDecode()){
				mHwDecoder.stopCodec();
			}
		}
		return 0;
	}

	public int setView(TextureView textureView){
		if(mDestroy) return 0;
		
		Log.i(TAG, "SetView TextureView");
		mHwDecoder.setRenderType(0);
		mSurfaceview 	= textureView;
		
		if(textureView.isAvailable()){
			mSurface = new Surface(textureView.getSurfaceTexture());
			
			if(isSystemPlayer()){
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
				Log.i(TAG, "TextureView SizeChanged width=" + width + ", height = " + height);
				if(mDestroy) return;	
				
				//mSurface = new Surface(surface);
				native_setdisplay(mSurface,mObjId);
				changeSubtitleViewSize();
			}
			
			@Override
			public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
				Log.v("APlayerAndroid","TextureView Destroyed");
				
				native_setdisplay(null,mObjId);
				
				if(!isSystemPlayer() && isHwDecode() && mHwDecoder.IsCodecPrepare()){
					Log.e(TAG, "!IsSystemPlayer() && isHwDecode()");
					mReCreateHwCodecState = getState();
					//mSilenceAudio         = getConfig(APlayerAndroid.CONFIGID.AUDIO_SILENCE);
					Log.i(TAG,"mSilenceAudio = " + mSilenceAudio);
					native_setconfig(APlayerAndroid.CONFIGID.AUDIO_SILENCE,"1",mObjId);
					pause();
					mHwDecoder.stopCodec();
					mHwReCreatePos = getPosition();
				}
				
				
				if(mOnSurfaceDestroyListener != null)
					mOnSurfaceDestroyListener.onSurfaceDestroy();
				
				return true;
			}
			
			@Override
			public void onSurfaceTextureAvailable(SurfaceTexture surface, int width,
					int height) {
				Log.i(TAG,"TextureView Created");
				if(mDestroy) return;
				
				mSurface = new Surface(surface);
				
				if(isSystemPlayer()){
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
					   setPosition(mHwReCreatePos);
					   
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
										setConfig(APlayerAndroid.CONFIGID.AUDIO_SILENCE, mSilenceAudio);
								}}).start();
								
								
								if(mOnReCreateHwDecoderListener != null){
									mOnReCreateHwDecoderListener.onReCreateHwDecoder();
								}
								
							}
					   });
					   
					   play();
					   mReCreateHwCodecState = PlayerState.APLAYER_READ;
					}
				}
			}
		});
		
		return 1;
	}
	
	public int setView(SurfaceView surfaceview){
		if(mDestroy) return 0;
		
		if(getAndroidLevel() >= 18){
			mHwDecoder.setRenderType(2);
		}else{
			mHwDecoder.setRenderType(0);
		}
		
		
		mSurfaceview 	= surfaceview;
		mSurface = surfaceview.getHolder().getSurface();
		if(!mSurface.isValid()){
			Log.i(TAG, "surface is not valid");
			mSurface = null;
		}else{
			if(isSystemPlayer()){
				mSystemPlayer.SetView(mSurface);
			}
			native_setdisplay(mSurface,mObjId);
		}

		mViewSurfaceWidth  = surfaceview.getWidth();
		mViewSurfaceHeight = surfaceview.getHeight();

		surfaceview.getHolder().addCallback(new SurfaceHolder.Callback() {
			public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
				Log.i(TAG, "surface Changed format=" + format + ", width=" + width + ", height="
			            + height);

				mViewSurfaceWidth  = width;
				mViewSurfaceHeight = height;

				if(mOnSurfaceChangeListener != null){
					mOnSurfaceChangeListener.onSurfaceChange(width,height);
				}

				if(mDestroy) return;			
				native_setdisplay(mSurface,mObjId);
				changeSubtitleViewSize();
			}
			
			public void surfaceCreated(SurfaceHolder holder) {
				Log.i(TAG,"surface Created");
				if(mDestroy) return;
				
				mSurface = holder.getSurface();
				
				if(isSystemPlayer()){
					mSystemPlayer.SetView(mSurface);
				}
				native_setdisplay(mSurface,mObjId);

				if(mReCreateHwCodecState == PlayerState.APLAYER_PAUSED  ||
				   mReCreateHwCodecState == PlayerState.APLAYER_PAUSING ||
				   mReCreateHwCodecState == PlayerState.APLAYER_PLAY    ||
				   mReCreateHwCodecState == PlayerState.APLAYER_PLAYING){
				   if(isHwDecode()){
					   Log.e(TAG, "lzm ReCreateCodec");
					   mHwDecoder.ReCreateCodec();
					   setPosition(mHwReCreatePos);
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
										setConfig(APlayerAndroid.CONFIGID.AUDIO_SILENCE,mSilenceAudio);
								}}).start();
								
								
								if(mOnReCreateHwDecoderListener != null){
									mOnReCreateHwDecoderListener.onReCreateHwDecoder();
								}
								
							}
					   });
					   play();
					   mReCreateHwCodecState = PlayerState.APLAYER_READ;
					}
				}
			}
			
			public void surfaceDestroyed(SurfaceHolder holder) {
				Log.i("APlayerAndroid","surface Destroyed");
				mSurface = null;
				native_setdisplay(null,mObjId);
				
				if(!isSystemPlayer() && isHwDecode() && mHwDecoder.IsCodecPrepare()){
					Log.e(TAG, "!IsSystemPlayer() && isHwDecode()");
					mReCreateHwCodecState = getState();
					//mSilenceAudio         = getConfig(APlayerAndroid.CONFIGID.AUDIO_SILENCE);
					Log.i(TAG,"mSilenceAudio = " + mSilenceAudio);
					native_setconfig(APlayerAndroid.CONFIGID.AUDIO_SILENCE,"1",mObjId);
					pause();
					mHwDecoder.stopCodec();
					mHwReCreatePos = getPosition();
				}
				
				if(mOnSurfaceDestroyListener != null){
					mOnSurfaceDestroyListener.onSurfaceDestroy();
				}
				}
			});
		return 0;
	}



	public void useSystemPlayer(boolean use){
		mIsCurrentUseSysmediaplay = use;
		if(use && (mSystemPlayer == null || !mSystemPlayer.hasMediaPlayer())){
			mSystemPlayer 	= new SystemMediaPlay();
		}
	}
	
	public boolean isSystemPlayer(){
		return mIsCurrentUseSysmediaplay && mSystemPlayer != null && mSystemPlayer.hasMediaPlayer();
	}
	
	public int open(String path){
		
		if(mDestroy) return 0;
		//path = "http://sl.video.kcloud.n0808.com/c6c80531ec733b6b32ba33e96a53e1c130b2f1d1?sign=2a033be4517a020eabe65b3955f04c58&t=593e4e29&ts=1497255465&hash=29a839481762f287e25ad2498d8e3a06";
		//path = "http://static3.ssp.xunlei.com/d1/2017/03/c9efdacf7a69a93a9a34b911eca7639a.mp4";
		//this.setConfig(CONFIGID.HTTP_USER_AHTTP, "1");
		//path = "http://192.168.123.234/2011.avi";
		//path = "http://10.10.121.10:55555/media/sdb1/bx.rmvb";
		//path = "http://10.10.121.10:47777/media/sda4/onecloud/young.rmvb";

		//path = "http://192.168.123.234/demo540p2.mp4";
		//path = "http://192.168.123.234/self.MOV";

		//path = "http://10.10.121.10:47777/media/sda2/onecloud/exception_sound.mkv";
		//path =  "http://10.10.121.10:47777/media/sda2/onecloud/seven.mkv";
		//path = "http://10.10.121.10:47777/media/sda2/onecloud/pre_alien.mp4";
		//path = "http://192.168.21.241/exception_sound.mkv";

		//path = "http://192.168.123.234/aiqing.mkv";

		mIsSuccess = false;
		mBufferProgress = 100;
		mALocalFile     = null;
		
		if(mUpdateSurfaceView){
			if(mSurfaceview != null){
				mSurfaceview.setVisibility(View.INVISIBLE);
				mSurfaceview.setVisibility(View.VISIBLE);
			}
			mUpdateSurfaceView = false;
		}

		if(isSystemPlayer()){
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
				Message m = mEventHandler.obtainMessage(MsgID.FRIST_VIDEO_FRAME_RENDER, 0, 0, null);
				if(m == null){
					return;
				}
				m.what = MsgID.FRIST_VIDEO_FRAME_RENDER;
				mEventHandler.sendMessage(m);
			}
	   });
	   return ret;
	}
	
	public int open(FileDescriptor fileDescriptor){
		if(mDestroy) return 0;
		
		mIsSuccess = false;
		mBufferProgress = 100;
		
		mALocalFile = new ALocalFile(fileDescriptor);
		this.setOnExtIOListerner(mALocalFile);
		
		int ret = native_open("c:\\",mObjId);
		if(ret == -1){
			Log.e(TAG, "throw Exception state is not right or other fatal error");
		}
		return ret;
	}
	
	public int close(){
		if(isSystemPlayer()){
			return mSystemPlayer.close();
		}
		
		/*if(isHwDecode()){
			mHwDecoder.stopCodec();
		}*/
		
		return native_close(mObjId);
	}
	
	public int play(){
		if(mDestroy) return 0;
		
		createSubtitleView();
		
		if(mIsSuccess){
			if(isSystemPlayer()){
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
	
	public int pause(){
		if(mDestroy) return 0;
		
		if(isSystemPlayer()){
			return mSystemPlayer.pause();
		}
		return native_pause(mObjId);
	}
	
	public static String getVersion(){
		return "1.2.1.125";
	}
	
	public int getState(){
		if(mDestroy) return 0;
		
		if(isSystemPlayer()){
			return mSystemPlayer.getState();
		}
		return native_getState(mObjId);
	}
	
	public int getDuration(){
		if(mDestroy) return 0;
		
		if(isSystemPlayer()){
			return mSystemPlayer.getDuration();
		}
		
		return native_getduration(mObjId);
	}
	
	public int getPosition(){
		if(mDestroy) return 0;
		
		if(isSystemPlayer()){
			return mSystemPlayer.getPosition();
		}
		
		return native_getposition(mObjId);
	}
	
	public int setPosition(int msec){
		Log.i(App.TAG,"setPosition pos = " + msec);
		if(mDestroy) return 0;
		
		if(isSystemPlayer()){
			return mSystemPlayer.setPosition(msec);
		}
		
		return native_setposition(msec,mObjId);
	}
	
	public int getVideoWidth(){
		if(mDestroy) return 0;
		
		if(isSystemPlayer()){
			return mSystemPlayer.getVideoWidth();
		}
		
		return native_getwidth(mObjId);
	}
	
	public int getVideoHeight(){
		if(mDestroy) return 0;
		
		if(isSystemPlayer()){
			return mSystemPlayer.getVideoHeight();
		}
		
		return native_getheight(mObjId);
	}
	
	public int getVolume()
	{
		return 0;
	}
	
	public int setVolume(int volume)
	{
		return 0;
	}
	
	public int getBufferProgress()
	{
		if(mDestroy) return 0;
		
		if(isSystemPlayer()){
			return mSystemPlayer.getBufferProgress();
		}

		return mBufferProgress;
		//return native_getbufferprogress(mObjId);
	}
	
	public String getConfig(int configID)
	{
		if(mDestroy) return "";
		
		if(isSystemPlayer()){
			return "";
		}
		
		switch(configID)
		{
			case CONFIGID.AUTO_PLAY:
				return config_get_auto_play();
			case CONFIGID.DOWN_SPEED:
				return config_get_down_speed();
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
	
	public int setConfig(int configID,String value)
	{
		if(mDestroy) return 0;
		
		if(isSystemPlayer()){
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
			case CONFIGID.HW_DECODER_USE:
				return config_set_hwdecode_use(value);
			case CONFIGID.HTTP_AHTTP_CACHE_DIR:
				return config_set_ahttp_cache_dir(value);
			case CONFIGID.HTTP_USER_AHTTP:
				return config_set_ahttp(value);	
			case CONFIGID.AUDIO_SILENCE:
				this.mSilenceAudio = value;
				return native_setconfig(configID,value,mObjId);
			case CONFIGID.RECORD_BIT:
				int bitRate = Integer.parseInt(value);
				return this.getEncodeCore().setVideoBitRate(bitRate);
			case CONFIGID.RECORD_HEIGHT:
				int recordHeight = Integer.parseInt(value);
				return this.getEncodeCore().setVideoHeight(recordHeight);
			case CONFIGID.RECORD_WIDTH:
				int recordeWidht = Integer.parseInt(value);
				return this.getEncodeCore().setVideoWidth(recordeWidht);
			default:
				return native_setconfig(configID,value,mObjId);
		}
	}
	
	public int setVideoOrientation(int nOrientation)
	{
		if(mDestroy) return 0;
		
		if(isSystemPlayer()){
			return 0;
		}
		 return native_setVideoOrientation(nOrientation,mObjId);
	}
	
	
	
	private boolean isHwDecode(){
		return (getConfig(CONFIGID.HW_DECODER_USE).equals("1") && getConfig(CONFIGID.HW_DECODER_ENABLE).equals("1"));
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

	private String config_get_down_speed(){
		if(mAHttp != null){
			return mAHttp.getSpeed() + "";
		}
		return 0 + "";
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
	
	
	private String config_get_vr_touch_rotate(){
		return (mIsVrTouchRotateEnable ? 1:0) + "";
	}
	

	private int config_set_hwdecode_use(String value){
		if(getConfig(CONFIGID.HW_DECODER_USE).equals(value)){
			return 1;
		}
		
		int state = getState();
		if(state == PlayerState.APLAYER_PAUSED  ||
		   state == PlayerState.APLAYER_PAUSING ||
		   state == PlayerState.APLAYER_PLAY    ||
		   state == PlayerState.APLAYER_PLAYING){
			return 0;
		}
		
		native_setconfig(CONFIGID.HW_DECODER_USE, value, mObjId);
		return 1;
	}

	private int config_set_ahttp(String value){
		if(value.equals("1")){
			if(mAHttp == null){
				mAHttp = new AHttp();
			}
			
			if(mAHttp != null){
				setOnExtIOListerner(mAHttp);
			}
		}else{
			mAHttp = null;
			setOnExtIOListerner(null);
		}
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
		public int 		width;
		public int 		height;
		public long 	duration_ms;
		public long		file_size;
		public long 	show_ms;
		public boolean 	is_key_frame;
		public int      avg_luma;       //亮度[0,255]
		public byte[] 	bitMap;
		
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
		int ret = native_is_support_record(mObjId);
		return (0 != ret);
	}
	
	public boolean startRecord(String outMediaPath)
	{
		Log.i(TAG,"startRecord");
		
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

		int ret = native_start_record(outMediaPath, mObjId);

		return (1 == ret);
	}
	
	public boolean isRecording()
	{
		if(mDestroy)  return false;
		int ret = native_is_recording(mObjId);
		return (0 != ret);
	}
	
	public void endRecord(){
		Log.i(TAG,"endRecord");
		
		if(mDestroy)  return;
		
		native_end_record(mObjId);
		Log.i(TAG,"endRecord leave");
	}
	
	public void stopRead(boolean stopRead){
		if(mDestroy) return;
		
		native_stop_read(stopRead,mObjId);
	}

	
	protected synchronized HardwareEncoder getEncodeCore(){
		if(mHardwareEncoder == null){
			mHardwareEncoder = new HardwareEncoder();
		}
		return mHardwareEncoder;
	}
	
	public void openLog(boolean openLog){
		Log.setOpenLog(openLog);
		this.native_openLog(openLog);
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

	private native void   native_openLog(boolean openLog);
	
    private int openSuccess(){    	
		mIsSuccess = true;
		if(mIsAutoPlay){
			play();
		}

		if(mOnOpenSuccessListener != null){
			mOnOpenSuccessListener.onOpenSuccess();
		}
		if(mOnOpenCompleteListener != null){
			mOnOpenCompleteListener.onOpenComplete(true);
		}

		if(!isHwDecode()){
			mUpdateSurfaceView = true;
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
				setOnExtIOListerner(null);
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
    public void setOnPlayStateChangeListener(OnPlayStateChangeListener listener) {
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
    	void onSurfaceDestroy();
    }
    public void setOnSurfaceDestroyListener(OnSurfaceDestroyListener listener){
    	mOnSurfaceDestroyListener = listener;
    }
    private OnSurfaceDestroyListener mOnSurfaceDestroyListener;
    
    
    public interface OnSystemPlayerFailListener{
    	void onSystemPlayerFail();
    }
    public void setOnSystemPlayerFailListener(OnSystemPlayerFailListener listener){
    	OnSystemPlayerFailListener = listener;
    }
    private OnSystemPlayerFailListener OnSystemPlayerFailListener;
    

    public interface OnShowSubtitleListener{
    	void onShowSubtitle(String subtitle);
    }
    public void setOnShowSubtitleListener(OnShowSubtitleListener listener){
    	mOnShowSubtitleListener = listener;
    }
    private OnShowSubtitleListener mOnShowSubtitleListener;


    
    public interface OnExtIOListerner{
		int  open(String url);
		int  close(String ret);
		int  read(ByteBuffer byteBuf);
		long seek(long offset, int whence);
		int  abort(boolean isAbort);
	}
    
	protected void setOnExtIOListerner(OnExtIOListerner listener){
		mOnExtIOListerner = listener;
		if(listener != null){
			setConfig(CONFIGID.EXTIO, "1");
		}else{
			setConfig(CONFIGID.EXTIO, "0");
		}
		
	}
	private OnExtIOListerner mOnExtIOListerner;
	
	
    
	protected interface OnSurfaceChangeListener{
		void onSurfaceChange(int width,int height);
	}
	protected void setOnSurfaceChangeListener(OnSurfaceChangeListener listener){
		mOnSurfaceChangeListener = listener;
	}
	private OnSurfaceChangeListener mOnSurfaceChangeListener;

	
	protected int getViewSurfaceWidth(){
		return mViewSurfaceWidth;
	}

	protected int getViewSurfaceHeight(){
		return mViewSurfaceHeight;
	}


    private void showSubtitle(CharSequence text)
    {    	
    	Log.e(TAG, "ShowSubtitle " + text.toString());
    	if(mSubtilteview == null)
    		createSubtitleView();
    
    	if(mSubtilteview == null || mSubtitleShow.equalsIgnoreCase("0"))
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
				Log.i(TAG, "surface getInnerSurface over");
    			return mSurface;
    		}
    		try {
				Thread.sleep(100);
			} catch (Exception e) {
				e.printStackTrace();
			}
    	}
	}

   public HardwareDecoder getHardwareDecoder(){
    	Log.i(TAG,"HardwareDecoder getHardwareDecoder");
		return  mHwDecoder;
	}

	public int renderTexture(int textureId){
		if(mGpuImageFilter == null){
			mGpuImageFilter = new GPUImageFilter(null);
			mGpuImageFilter.init();
		}


		int vertexNum = 6;

		float[] vertexCoordinate  = {1.0f, -1.0f,  0.0f,
				1.0f,  1.0f,  0.0f,
				-1.0f,  1.0f,  0.0f,
				-1.0f,  1.0f,  0.0f,
				-1.0f, -1.0f,  0.0f,
				1.0f, -1.0f,  0.0f};

		ByteBuffer bbVertices = ByteBuffer.allocateDirect(vertexNum * 3 * 4);
		bbVertices.order(ByteOrder.nativeOrder());
		FloatBuffer vertexBuf = bbVertices.asFloatBuffer();
		vertexBuf.put(vertexCoordinate);
		vertexBuf.position(0);

		float[] textureCoordinate1  = {1.0f, 1.0f,
				1.0f, 0.0f,
				0.0f, 0.0f,
				0.0f, 0.0f,
				0.0f, 1.0f,
				1.0f, 1.0f};


		ByteBuffer bbColors1 = ByteBuffer.allocateDirect(vertexNum * 2 * 4);
		bbColors1.order(ByteOrder.nativeOrder());
		FloatBuffer textureCoordinateBuf = bbColors1.asFloatBuffer();
		textureCoordinateBuf.put(textureCoordinate1);
		textureCoordinateBuf.position(0);


		mGpuImageFilter.draw(textureId,vertexBuf,textureCoordinateBuf);
		return 1;
	}

	protected int getAndroidLevel(){
		Log.i(TAG,"getAndroidLevel");

		return  Build.VERSION.SDK_INT;
	}

    private int extIOOpen(String url){
    	if(mOnExtIOListerner != null)
    		return mOnExtIOListerner.open(url);
    	
    	return 0;
    }
	
    private int extIOClose(String ret){
    	if(mOnExtIOListerner != null){
    		mOnExtIOListerner.close(ret);
        	return 1;
    	}
    	
    	return 0;
    }
    
    private int extIORead(ByteBuffer byteBuf){
    	if(mOnExtIOListerner != null)
    		return mOnExtIOListerner.read(byteBuf);
    	
    	return 0;
    }
    
    private long extIOSeek(int whence,long offset){
    	if(mOnExtIOListerner != null)
    		return mOnExtIOListerner.seek(offset, whence);
    	
    	return 0;
    }

    private int  extIOAbort(boolean isAbort){
		if(mOnExtIOListerner != null)
			return mOnExtIOListerner.abort(isAbort);

		return 0;
	}

    private int postEventFromNative(int what, int arg1, int arg2, Object obj){
    	Message m = mEventHandler.obtainMessage(what, arg1, arg2, obj);
		if(m == null){
			return 0;
		}
		m.arg1 = arg1;
		m.arg2 = arg2;
		m.obj  = obj;
		mEventHandler.sendMessage(m);
		return 1;
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
						mOnShowSubtitleListener.onShowSubtitle(text);
					}
					if(!mSubtitleShowExternal)
					showSubtitle(text);
				}
				break;
			case MsgID.FRIST_VIDEO_FRAME_RENDER:
				Log.i(TAG,"Message FRIST_VIDEO_FRAME_RENDER");
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
		public static final int FRIST_VIDEO_FRAME_RENDER = 104;
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
		public static final int EXTIO          			= 14;
		public static final int READPOSITION   			= 31;
		public static final int UPDATEWINDOW        	= 40;		
		public static final int ORIENTATION   			= 41;
		public static final int PLAY_SPEED              = 104;
		public static final int DOWN_SPEED              = 105;
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
		public static final int VR_MODEL				= 2413;
		public static final int VR_ENABLE_INNER_TOUCH_ROTATE  = 2414;
		public static final int VR_DISTORTION_CORRECTION      = 2415;
		
		public static final int SEEK_ENABLE  			= 3000;
		
		public static final int RECORD_BIT              = 4000;
		public static final int RECORD_WIDTH            = 4001;
		public static final int RECORD_HEIGHT           = 4002;
	}
	
	private class ALocalFile implements OnExtIOListerner{
		private FileDescriptor   		mFileDescriptor   		= null;
		private FileChannel				mFileChannel            = null;
		private FileInputStream 		mFileInputStream        = null;
		
		private long                    mCurPos                 = 0;
		private long                    mFileSize               = 0;
		
		public ALocalFile(FileDescriptor fileDescriptor){
			mFileDescriptor = fileDescriptor;
		}
		
		public synchronized int open(String url){
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
		
		public int close(String ret){
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
		
		public int read(ByteBuffer bbfout){
			synchronized(mFileChannel){
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
		
		public int  abort(boolean isAbort){
			return 0;
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
        			OnSystemPlayerFailListener.onSystemPlayerFail();
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
    			OnSystemPlayerFailListener.onSystemPlayerFail();
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
