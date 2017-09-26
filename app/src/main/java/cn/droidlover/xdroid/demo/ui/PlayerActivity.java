package cn.droidlover.xdroid.demo.ui;

import java.lang.ref.WeakReference;

import com.aplayer.aplayerandroid.APlayerAndroid;

import butterknife.BindView;
import cn.droidlover.xdroid.demo.R;
import cn.droidlover.xdroid.demo.kit.AppKit;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings.SettingNotFoundException;
import android.support.v4.app.Fragment;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.Toast;

public class PlayerActivity extends Activity {
	private static final String DEBUG_TAG = "PlayerActivity";
	public static final String VIDEO_FILE_PATH = "video_file_path";
	public static final String VIDEO_FILE_NAME = "video_file_name";
	public static final String VIDEO_CACHE_PATH = "video_cache_path";

	private String mCurPlayVideoPath;
	private String mCurPlayVideoName;
	private int mCurPlayVideoIndex;


	private APlayerAndroid	mAPlayerAndroid;
	private SurfaceView    	mSurView;

	private ImageButton     mPlayPauseButton;
	
	// overlay
	private View mOverlayHeader;
	private View mOverlayFooter;
	private View mOverlayTipInfo;
	private View mOverlayPlayer;
	
	// Tip Info overlay
	private ImageView mTipInfoIcon;
	private TextView mTipInfoText;
	private View mTipInfoProgress;
	private View mTipInfoProgressBackground;
	private View mTipInfoProgressFill;
	private View mProgressView;
	private TextView mTextProgress;
	
	// Header overlay
	private ImageButton mGoBackButton;
	private TextView mTitleText;
	
	// Footer overlay
	private SeekBar mSeekBar;
	private TextView mTimeText;
	private TextView mDurationText;

	// Message
	private static final int FADE_OUT_OVERLAY = 0;
	private static final int UPDATE_PROGRESS = 1;
	private static final int FADE_OUT_TIP_INFO = 3;
	private static final int GO_BACK = 4;

	// Touch Action
	private static final int TOUCH_NONE = 0;
	private static final int TOUCH_VOLUME = 1;
	private static final int TOUCH_BRIGHTNESS = 2;
	private static final int TOUCH_SEEK = 3;

	// Volume manager
	private AudioManager mAudioManager;
	private int mAudioMaxVolume;
	private int mVolume;
	
	// visible state
	private boolean mIsOverlayVisible = false;
	private boolean mIsHistoryOverlayVisible = false;
	private static boolean mIsDragging = false;
	
	// play control
	private int mNextPlayIndex = -1;
	private boolean mPlayOnResume = false;

	private boolean isChangeHwSoftDecoder = false;
	private int     mCurPosition = 0;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.i(DEBUG_TAG, "onCreate");
		super.onCreate(savedInstanceState);

		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		// 全屏模式下显示状态栏
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);

		// 隐藏状态栏
		hideStatusBar();
		setContentView(R.layout.activity_player);
		Intent intent = getIntent();
		mCurPlayVideoPath = intent.getStringExtra(VIDEO_FILE_PATH);
		mCurPlayVideoName = intent.getStringExtra(VIDEO_FILE_NAME);


		mAPlayerAndroid = new APlayerAndroid();

		mAPlayerAndroid.setConfig(APlayerAndroid.CONFIGID.HW_DECODER_USE, "1");
		mAPlayerAndroid.setConfig(APlayerAndroid.CONFIGID.HTTP_USER_AHTTP, "1");
		mAPlayerAndroid.setConfig(APlayerAndroid.CONFIGID.HTTP_AHTTP_CACHE_DIR,intent.getStringExtra(VIDEO_CACHE_PATH));
		mSurView = (SurfaceView)findViewById(R.id.player_surface);

		mPlayPauseButton = (ImageButton)findViewById(R.id.play_pause);
		mPlayPauseButton.setOnClickListener(mPlayPauseButtonClickListener);

		mOverlayPlayer = findViewById(R.id.player_overlay);
		mOverlayPlayer.setVisibility(View.GONE);
		mOverlayPlayer.setOnClickListener(mOverlayPlayClickListener);

		mOverlayHeader = findViewById(R.id.player_overlay_header);
		mOverlayHeader.setVisibility(View.GONE);
		mOverlayHeader.setOnClickListener(mOverlayHeaderClickListener);
		// 设置layout header的上间距 避免被status bar遮挡
		/*int statusBarHeight = getStatusBarHeight();
		RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(mOverlayHeader.getLayoutParams());
		lp.setMargins(0, statusBarHeight, 0, 0);
		mOverlayHeader.setLayoutParams(lp);*/
		
		mOverlayFooter = findViewById(R.id.player_overlay_footer);
		mOverlayFooter.setVisibility(View.INVISIBLE);
		mOverlayFooter.setOnClickListener(mOverlayFooterClickListener);

		mOverlayTipInfo = findViewById(R.id.tip_info_overlay);
		mOverlayTipInfo.setVisibility(View.GONE);
		
		mTipInfoIcon = (ImageView)findViewById(R.id.tip_info_icon);
		mTipInfoText = (TextView)findViewById(R.id.tip_info_text);
		mTipInfoProgress = findViewById(R.id.tip_info_progress);
		mTipInfoProgressBackground = findViewById(R.id.tip_info_progress_background);
		mTipInfoProgressFill = findViewById(R.id.tip_info_progress_fill);
		mProgressView		 = findViewById(R.id.pay_progress);
		mTextProgress        = (TextView) findViewById(R.id.text_progress);


		mGoBackButton = (ImageButton)findViewById(R.id.go_back);
		mGoBackButton.setOnClickListener(mGoBackListener);
		mTitleText = (TextView)findViewById(R.id.player_overlay_title);
		
		mSeekBar = (SeekBar)findViewById(R.id.player_overlay_seekbar);
		mSeekBar.setOnSeekBarChangeListener(mSeekListener);
		mTimeText = (TextView)findViewById(R.id.player_overlay_time);
		mDurationText = (TextView)findViewById(R.id.player_overlay_duration);

		mAudioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
		mAudioMaxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
		
		mAPlayerAndroid.setView(mSurView);
		mAPlayerAndroid.setConfig(APlayerAndroid.CONFIGID.HW_DECODER_USE, "1");
		Log.d(DEBUG_TAG, "open: " + mCurPlayVideoPath);
		OpenMedia(mCurPlayVideoPath);
		showProgress(true);
		final Activity playerActivity = this;
		mAPlayerAndroid.setOnOpenSuccessListener(new APlayerAndroid.OnOpenSuccessListener() {	
			@Override
			public void onOpenSuccess() {
				showProgress(false);
				int videoWidth  = mAPlayerAndroid.getVideoWidth();
				int videoHeight = mAPlayerAndroid.getVideoHeight();

				Log.d(DEBUG_TAG, "onOpenSuccess video_width = " + videoWidth + " video_height = " + videoHeight);
				int screenWidth  = AppKit.getScreenWidth();
				int screenHeight = AppKit.getScreenHeight();
				int l = 0;
				int t = 0;
				int w  = screenHeight;
				int h =  screenWidth;
				if(videoWidth >= videoHeight){
					playerActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
					if((videoWidth * 1.0f) / (videoHeight * 1.0f) > (screenHeight* 1.0f / screenWidth * 1.0f)){
						h = videoHeight * w / videoWidth;
						t = (screenWidth - h) / 2;
					}else{
						w  = videoWidth * h / videoHeight;
						l  = (screenHeight - w) / 2;
					}
				}else{
					playerActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT);
					w = screenWidth;
					h = screenHeight;
					if((videoHeight * 1.0f) / (videoWidth * 1.0f) > (screenWidth* 1.0f / screenHeight * 1.0f)){
						w  = videoWidth * h / videoHeight;
						l  = (screenWidth - w) / 2;
					}else{
						h = videoHeight * w / videoWidth;
						t = (screenHeight - h) / 2;
					}
				}

				FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams)mSurView.getLayoutParams();
				layoutParams.width = w;
				layoutParams.height = h;
				layoutParams.leftMargin = l;
				layoutParams.topMargin = t;
				mSurView.setLayoutParams(layoutParams);
				Log.d(DEBUG_TAG, "mSurView.setLayoutParams width = " + w + " height = " + h);
				//layoutParams.width
				play();
				if(isChangeHwSoftDecoder){
					mAPlayerAndroid.setPosition(mCurPosition);
					isChangeHwSoftDecoder = false;
				}
			}
		});

		mAPlayerAndroid.setOnPlayCompleteListener(new APlayerAndroid.OnPlayCompleteListener() {
			
			@Override
			public void onPlayComplete(String playRet) {
				// TODO Auto-generated method stub
				/*if(isChangeHwSoftDecoder){
					OpenMedia(mCurPlayVideoPath);
				}else{
					handlePlayComplete();
				}*/
				handlePlayComplete();
			}
		});
		
		mAPlayerAndroid.setOnPlayStateChangeListener(new APlayerAndroid.OnPlayStateChangeListener() {
			@Override
			public void onPlayStateChange(int nCurrentState, int nPreState) {
				Log.v(DEBUG_TAG, "Play State Change: Cur: " + nCurrentState + " pre: " + nPreState);
				/*if (nCurrentState == APlayerAndroid.PlayerState.APLAYER_READ) {
					
				}*/
				updateTitle();
				updatePlayControlButtons();
				updateProgress(-1);
			}
		});

		mAPlayerAndroid.setOnBufferListener(new APlayerAndroid.OnBufferListener() {
			@Override
			public void onBuffer(int progress) {
				mTextProgress.setText(progress + "%");
				if(progress == 0){
					showProgress(true);
					mTextProgress.setVisibility(View.VISIBLE);
					mPlayerHandler.sendEmptyMessageDelayed(FADE_OUT_OVERLAY, 500);
				}
				if(progress == 100){
					showProgress(false);
					mTextProgress.setVisibility(View.GONE);
				}
			}
		});
	}
	
	private void handlePlayComplete(){
		/*if (mNextPlayIndex != -1) {
			mCurPlayVideoIndex = mNextPlayIndex;
			mNextPlayIndex = -1;
			Log.v(DEBUG_TAG, "Open " + mCurPlayVideoPath);
			OpenMedia(mCurPlayVideoPath);
		}
		else {
			Log.v(DEBUG_TAG, "before finish");
			String playResult = mAPlayerAndroid.GetConfig(APlayerAndroid.CONFIGID.PLAYRESULT);
			if (!playResult.equals("0") && !playResult.equals("1")) {
				Toast.makeText(mSurView.getContext(), "发生错误: " + playResult, Toast.LENGTH_LONG).show();
			}
			finish();
		}*/

		Log.v(DEBUG_TAG, "before finish");
		String playResult = mAPlayerAndroid.getConfig(APlayerAndroid.CONFIGID.PLAYRESULT);
		/*if (!playResult.equals("0x0") && !playResult.equals("0x1")) {
			Toast.makeText(mSurView.getContext(), "发生错误: " + playResult, Toast.LENGTH_LONG).show();
		}*/
		finish();
	}
	
	private int getStatusBarHeight() { 
		int result = 0;
		int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
		if (resourceId > 0) {
			result = getResources().getDimensionPixelSize(resourceId);
		}
		return result;
	}
	
	@Override
	protected void onResume() {
		Log.v(DEBUG_TAG, "onResume");
		super.onResume();
		if (mPlayOnResume) {
			play();
		}
	}
	
	@Override
	protected void onPause() {
		Log.d(DEBUG_TAG, "onPause");
		super.onPause();
		if (mAPlayerAndroid.getState() == APlayerAndroid.PlayerState.APLAYER_PLAYING) {
			mPlayOnResume = true;
		}
		else {
			mPlayOnResume = false;
		}
		mAPlayerAndroid.pause();
	}
	
	@Override
	protected void onStop() {
		Log.v(DEBUG_TAG, "onStop");
		super.onStop();
	}

	@Override
	protected void onDestroy() {
		Log.v(DEBUG_TAG, "onDestory");
		super.onDestroy();
		mAPlayerAndroid.close();
		mAPlayerAndroid.setOnOpenSuccessListener(null);
		mAPlayerAndroid.setOnPlayStateChangeListener(null);
		mAPlayerAndroid.destroy();
	}

	private int mTouchAction = TOUCH_NONE;
	private int mSurfaceYDisplayRange = 0;
	private float mTouchX = 0;
	private float mTouchY = 0;
	private boolean mIsFirstBrightnessGesture = true;
	
	@Override
	public boolean onTouchEvent(MotionEvent event){
		
		DisplayMetrics screen = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(screen);

        if (mSurfaceYDisplayRange == 0)
            mSurfaceYDisplayRange = Math.min(screen.widthPixels, screen.heightPixels);

        float y_changed = event.getRawY() - mTouchY;
        float x_changed = event.getRawX() - mTouchX;

        float coef = Math.abs (y_changed / x_changed);
        float xgesturesize = ((x_changed / screen.xdpi) * 2.54f);
		
		switch (event.getActionMasked()) {
		case MotionEvent.ACTION_DOWN:
			// Volume or Brightness
			mTouchY = event.getRawY();
            mVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            // Seek
            mTouchX = event.getRawX();
			mTouchAction = TOUCH_NONE;
			break;
		case MotionEvent.ACTION_MOVE:
			if (coef > 2) {
                // Volume
                if (mTouchX > (screen.widthPixels / 2)) {
                    doVolumeTouch(y_changed);
                }
                // Brightness
                if (mTouchX < (screen.widthPixels / 2)) {
                    doBrightnessTouch(y_changed);
                }
            }
            doSeekTouch(coef, xgesturesize, false);
			break;
		case MotionEvent.ACTION_UP:
			if (mTouchAction == TOUCH_NONE) {
				if (mIsOverlayVisible) {
					hideOverlay();
				}
				else {
					showOverlayTimeout(3000);
				}
			}
            doSeekTouch(coef, xgesturesize, true);
			break;
		default:
			return false;
		}
		return mTouchAction != TOUCH_NONE;
	}

	private void doVolumeTouch(float y_changed) {
        if (mTouchAction != TOUCH_NONE && mTouchAction != TOUCH_VOLUME)
            return;
        int delta = -(int) ((y_changed / mSurfaceYDisplayRange) * mAudioMaxVolume);
        int vol = (int) Math.min(Math.max(mVolume + delta, 0), mAudioMaxVolume);
        if (delta != 0) {
            mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, vol, 0);
            mTouchAction = TOUCH_VOLUME;
            showVolumeTipInfoTimeout((vol * 100 / mAudioMaxVolume) + "%", vol == 0, 1000);
        }
    }

	private void initBrightnessTouch() {
        float brightnesstemp = 0.01f;
        try {
            brightnesstemp = android.provider.Settings.System.getInt(getContentResolver(),
                    android.provider.Settings.System.SCREEN_BRIGHTNESS) / 255.0f;
        } catch (SettingNotFoundException e) {
            e.printStackTrace();
        }
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.screenBrightness = brightnesstemp;
        getWindow().setAttributes(lp);
        mIsFirstBrightnessGesture = false;
    }

    private void doBrightnessTouch(float y_changed) {
        if (mTouchAction != TOUCH_NONE && mTouchAction != TOUCH_BRIGHTNESS)
            return;
		if (mIsFirstBrightnessGesture) initBrightnessTouch();
        mTouchAction = TOUCH_BRIGHTNESS;

        float delta = - y_changed / mSurfaceYDisplayRange * 0.07f;

        WindowManager.LayoutParams lp = getWindow().getAttributes();
        float bright = Math.min(Math.max(lp.screenBrightness + delta, 0.01f), 1);
        lp.screenBrightness = bright;

        getWindow().setAttributes(lp);
        showBrightnessTipInfoTimeout((int)(bright * 100) + "%", 1000);
    }

    private void doSeekTouch(float coef, float gesturesize, boolean seek) {
        if (coef > 0.5 || Math.abs(gesturesize) < 1)
            return;

        if (mTouchAction != TOUCH_NONE && mTouchAction != TOUCH_SEEK)
            return;
        mTouchAction = TOUCH_SEEK;

        int length = mAPlayerAndroid.getDuration();
        int time = mAPlayerAndroid.getPosition();

        // 拖动距离 最长 10分钟
        int jump = (int) (Math.signum(gesturesize) * ((600000 * Math.pow((gesturesize / 8), 4)) + 3000));

        if ((jump > 0) && ((time + jump) > length))
            jump = (int) (length - time);
        if ((jump < 0) && ((time + jump) < 0))
            jump = (int) -time;

        int jumpTo = jump + time;
        if (seek) {
        	mAPlayerAndroid.setPosition(jumpTo);
        }

        float progress = (float) 1.0;
        if (length != 0) {
        	progress = (float) ((double)(jumpTo) / length);
        }
        
        showSeekTipInfoTimeout(formatTime(jumpTo), jump > 0, progress, 1000);
    }
	
	private final Handler mPlayerHandler = new VideoPlayerHandler(this);
	
	private static class VideoPlayerHandler extends Handler {
	    private final WeakReference<PlayerActivity> ownerRef;
	    
	    public VideoPlayerHandler(PlayerActivity activity) {
	        ownerRef = new WeakReference<PlayerActivity>(activity);
	    }

	    @Override
	    public void handleMessage(Message msg) {
	    	PlayerActivity activity = ownerRef.get();
	        if (activity == null) {
	            return;
	        }

	        switch (msg.what) {
	        case FADE_OUT_OVERLAY:
	        	activity.hideOverlay();
	        	break;
	        case UPDATE_PROGRESS:
	        	activity.updateProgress(-1);
	        	if (activity.isOverlayVisible() && !mIsDragging) {
	        		// 1秒后再更新进度条
	        		this.sendEmptyMessageDelayed(UPDATE_PROGRESS, 1000);
	        	}
	        	break;
	        case FADE_OUT_TIP_INFO:
	        	activity.hideTipInfo();
	        	break;
	        case GO_BACK:
	        	activity.goBack();
	        	break;
	        }
	    }
	}

	private void showOverlayTimeout(int timeout) {
		// 显示状态栏
		//showStatusBar();
		// 发送消息更进度条
		mOverlayPlayer.setVisibility(View.VISIBLE);
		mPlayerHandler.sendEmptyMessage(UPDATE_PROGRESS);
		mOverlayHeader.setVisibility(View.VISIBLE);
		updateTitle();
		mOverlayFooter.setVisibility(View.VISIBLE);
		mPlayerHandler.removeMessages(FADE_OUT_OVERLAY);
		// 延迟消失
		mPlayerHandler.sendEmptyMessageDelayed(FADE_OUT_OVERLAY, timeout);
		mIsOverlayVisible = true;
		updatePlayControlButtons();
	}

	private void hideOverlay() {
		// 隐藏状态栏
		//hideStatusBar();
		mPlayerHandler.removeMessages(FADE_OUT_OVERLAY);
		mOverlayHeader.setVisibility(View.INVISIBLE);
		mOverlayFooter.setVisibility(View.INVISIBLE);
		mOverlayPlayer.setVisibility(View.INVISIBLE);
		mIsOverlayVisible = false;
	}

	private void updateProgress(int progress) {
		// 更新播放进度
		int time = 0;
		if(progress == -1){
			time = (int)mAPlayerAndroid.getPosition();
		}else {
			time = progress;
		}

        int length = (int)mAPlayerAndroid.getDuration();
        mCurPosition = time;
        
        mSeekBar.setMax(length);
        mSeekBar.setProgress(time);

        mTimeText.setText(formatTime(time));
        mDurationText.setText(formatTime(length));
	}

	private void showBrightnessTipInfoTimeout(String text, int timeout) {
		mPlayerHandler.removeMessages(FADE_OUT_TIP_INFO);
		mTipInfoIcon.setImageResource(R.mipmap.ic_brightness_tip);
		mTipInfoText.setText(text);
		mTipInfoProgress.setVisibility(View.INVISIBLE);
		mOverlayTipInfo.setVisibility(View.VISIBLE);
		mPlayerHandler.sendEmptyMessageDelayed(FADE_OUT_TIP_INFO, timeout);
	}

	private void showVolumeTipInfoTimeout(String text, boolean isMute, int timeout) {
		mPlayerHandler.removeMessages(FADE_OUT_TIP_INFO);
		if (isMute) {
			mTipInfoIcon.setImageResource(R.mipmap.ic_mute_tip);
		}
		else {
			mTipInfoIcon.setImageResource(R.mipmap.ic_volume_tip);
		}
		mTipInfoText.setText(text);
		mTipInfoProgress.setVisibility(View.INVISIBLE);
		mOverlayTipInfo.setVisibility(View.VISIBLE);
		mPlayerHandler.sendEmptyMessageDelayed(FADE_OUT_TIP_INFO, timeout);
	}

	private void showSeekTipInfoTimeout(String text, boolean forward, float progress, int timeout) {
		mPlayerHandler.removeMessages(FADE_OUT_TIP_INFO);
		if (forward) {
			mTipInfoIcon.setImageResource(R.mipmap.ic_step_forward);
		}
		else {
			mTipInfoIcon.setImageResource(R.mipmap.ic_step_backward);
		}
		mTipInfoText.setText(text);
		mTipInfoProgress.setVisibility(View.VISIBLE);
		progress = (float) Math.min(Math.max(progress, 0.0), 1.0);
		int newWidth = (int) (mTipInfoProgressBackground.getLayoutParams().width * progress);
		LayoutParams lp = mTipInfoProgressFill.getLayoutParams();
		lp.width = newWidth;
		mTipInfoProgressFill.setLayoutParams(lp);
		mOverlayTipInfo.setVisibility(View.VISIBLE);
		mPlayerHandler.sendEmptyMessageDelayed(FADE_OUT_TIP_INFO, timeout);
	}
	
	private void OpenMedia(String path) {
		mAPlayerAndroid.open(path);
	}
	
	private void play() {
		mAPlayerAndroid.play();
		mSurView.setKeepScreenOn(true);
	}
	
	private void pause() {
		mAPlayerAndroid.pause();
		mSurView.setKeepScreenOn(false);
	}

	private void hideTipInfo() {
		mOverlayTipInfo.setVisibility(View.GONE);
	}

	private boolean isOverlayVisible() {
		return mIsOverlayVisible;
	}
	
	private void goBack() {
		finish();
	}

	private String formatTime(int time) {
		int minus = time / 60000;
		time %= 60000;
		int seconds = time / 1000;
		return String.format("%02d:%02d", minus, seconds);
	}
	
	private final OnSeekBarChangeListener mSeekListener = new OnSeekBarChangeListener() {
		int mProgress = 0;
        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            mIsDragging = true;
            showOverlayTimeout(Integer.MAX_VALUE);
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
        	mIsDragging = false;
			mAPlayerAndroid.setPosition(mProgress);
            showOverlayTimeout(3000);
        }

        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            if (fromUser) {
				mProgress = progress;
            	//mAPlayerAndroid.setPosition(progress);
            	updateProgress(progress);
            	mTimeText.setText(formatTime(progress));
            }
        }
    };
    
    private final OnClickListener mGoBackListener = new OnClickListener() {
		@Override
		public void onClick(View view) {
			goBack();
		}
    };
    
    private final OnClickListener mHistoryButtonClickListener = new OnClickListener() {
		@Override
		public void onClick(View view) {
			showHistoryOverlay();
		}
    };
    
    private final OnClickListener mOverlayHeaderClickListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			showOverlayTimeout(3000);
		}
    };
    
    private void showHistoryOverlay() {
		hideOverlay();
	}

    
    private final OnClickListener mOverlayFooterClickListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			showOverlayTimeout(3000);
		}
    };

	private final OnClickListener mOverlayPlayClickListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			showOverlayTimeout(3000);
		}
	};
    
    private final OnClickListener mPlayPauseButtonClickListener = new OnClickListener() {
		@Override
		public void onClick(View v) {
			int curState = mAPlayerAndroid.getState();
			if (curState == APlayerAndroid.PlayerState.APLAYER_PLAYING
					|| curState == APlayerAndroid.PlayerState.APLAYER_PLAY) {
				pause();
			}
			else {
				play();
			}
			showOverlayTimeout(3000);
		}
    };

    
    private void hideStatusBar() {
        WindowManager.LayoutParams attrs = getWindow().getAttributes();
        attrs.flags |= WindowManager.LayoutParams.FLAG_FULLSCREEN;
        getWindow().setAttributes(attrs);
    }
    
    private void showStatusBar() {
        WindowManager.LayoutParams attrs = getWindow().getAttributes();
        attrs.flags &= ~WindowManager.LayoutParams.FLAG_FULLSCREEN;
        getWindow().setAttributes(attrs);
    }
    
    private void updatePlayControlButtons() {
    	int curState = mAPlayerAndroid.getState();
		if (curState == APlayerAndroid.PlayerState.APLAYER_PLAYING
				|| curState == APlayerAndroid.PlayerState.APLAYER_PLAY) {
    		mPlayPauseButton.setImageResource(R.drawable.ic_pause);
    	}
    	else {
    		mPlayPauseButton.setImageResource(R.drawable.ic_play);
    	}
    	if (mNextPlayIndex != -1) {
        	mPlayPauseButton.setEnabled(false);
    	}
    	else {
        	mPlayPauseButton.setEnabled(true);
    	}
    }
    
    private void updateTitle() {
    	mTitleText.setText(mCurPlayVideoName);
    }

	@TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
	private void showProgress(final boolean show) {
		// On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
		// for very easy animations. If available, use these APIs to fade-in
		// the progress spinner.
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
			int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

           /* mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
            mLoginFormView.animate().setDuration(shortAnimTime).alpha(
                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                }
            });*/

			mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
			mProgressView.animate().setDuration(shortAnimTime).alpha(
					show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
				@Override
				public void onAnimationEnd(Animator animation) {
					mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
				}
			});
		} else {
			// The ViewPropertyAnimator APIs are not available, so simply show
			// and hide the relevant UI components.
			mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
			//mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
		}
	}

}
