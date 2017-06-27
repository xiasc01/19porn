package com.aplayer.aplayerandroid;

import android.R.bool;
import android.R.integer;
import android.R.transition;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.media.MediaCodec.BufferInfo;
import android.os.Build;
import android.os.Environment;
import android.util.Log;
import android.view.Surface;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;

import com.aplayer.aplayerandroid.APlayerAndroid.OnOpenSuccessListener;

/**
 * Created by lzmlsfe on 2016/3/22.
 */
public class HardwareDecoder {
    private static  String TAG = "APlayerAndroid";
    private static HashMap<Integer,String> mAVCodeIDToMime = new HashMap<Integer, String>();
    //private static String[] mHWCmpName = {"Exynos","qcom"};
    private APlayerAndroid mAPlayerAndroid;
    private MediaCodec mDecoder = null;
    private String mHWDecoderName = null;
    private int mCodeId = 0;
    private int samplecnt = 0;
    private ByteBuffer mBbcsd;

    private class AV_CODEC_ID{
        private static final int  AV_CODEC_ID_MPEG4= 13;
        private static final int  AV_CODEC_ID_H264 = 28;
    }

    HardwareDecoder(APlayerAndroid aPlayerAndroid){
        mAVCodeIDToMime.put(AV_CODEC_ID.AV_CODEC_ID_H264, "video/avc");
        mAVCodeIDToMime.put(AV_CODEC_ID.AV_CODEC_ID_MPEG4, "video/mp4v-es");
        mAPlayerAndroid = aPlayerAndroid;
    }

    public int FindHardWareDecoder(int codeId){
    	
    	this.mCodeId = codeId;
    	Log.e(TAG,"sdk version " + Build.VERSION.SDK_INT);
    	if (Build.VERSION.SDK_INT < 16){
    		return 0;
    	}
    	
        if(Build.VERSION.SDK_INT < 21){
        	return FindHardWareDecoder16();
        }
        
       return FindHardWareDecoder21();
        //return FindHardWareDecoder16();
    }
    
    @SuppressWarnings("deprecation")
	public int FindHardWareDecoder16(){
    	String mimeType = mAVCodeIDToMime.get(mCodeId);
        Log.e(TAG, "mimeType = " + mimeType + "codeid = " + mCodeId);
        
        int mediaCodecCount = MediaCodecList.getCodecCount();
        
    	for (int i = 0; i < mediaCodecCount; i++) {
    		MediaCodecInfo mediaCodecInfo = MediaCodecList.getCodecInfoAt(i);
            if(mediaCodecInfo.isEncoder()){
                continue;
            }

            String types[] = mediaCodecInfo.getSupportedTypes();
            for (int j = 0;j < types.length;j++){
                if (types[j].equalsIgnoreCase(mimeType)){
                    String decoderName = mediaCodecInfo.getName();
                    Log.e(TAG,"hwDecoderName = " + decoderName);
                    if(decoderName.indexOf("google") == -1){
                    	mHWDecoderName = decoderName;
                    	return 1;
                    }
                }
            }
		}
    	return 0;
    }
    
    public int FindHardWareDecoder21(){
        String mimeType = mAVCodeIDToMime.get(mCodeId);
        Log.e(TAG, "mimeType = " + mimeType + "codeid = " + mCodeId);

        MediaCodecList mediaCodecList = new MediaCodecList(MediaCodecList.ALL_CODECS);
        MediaCodecInfo[] mediaCodecInfos =  mediaCodecList.getCodecInfos();
        
        for (int i = 0;i < mediaCodecInfos.length;i++){
            MediaCodecInfo mediaCodecInfo = mediaCodecInfos[i];
            if(mediaCodecInfo.isEncoder()){
                continue;
            }

            String types[] = mediaCodecInfo.getSupportedTypes();
            for (int j = 0;j < types.length;j++){
                if (types[j].equalsIgnoreCase(mimeType)){
                    String decoderName = mediaCodecInfo.getName();
                    Log.e(TAG,"hwDecoderName = " + decoderName);
                    if(decoderName.indexOf("google") == -1){
                    	mHWDecoderName = decoderName;
                    	return 1;
                    }
                }
            }
        }
        return  0;
    }


    public  int CreateCodec(ByteBuffer bbcsd){
    	Log.e(TAG, "HardwareDecoder:CreateCodec");
    	
        int   height  	= mAPlayerAndroid.GetVideoHeight();
        int   width   	= mAPlayerAndroid.GetVideoWidth();
        long  duration  = mAPlayerAndroid.GetDuration() * 1000L;

        Surface surface = mAPlayerAndroid.getInnerSurface();
        
        mBbcsd = bbcsd;

        MediaFormat mediaFormat = new MediaFormat();
        mediaFormat.setInteger(MediaFormat.KEY_HEIGHT, height);
        mediaFormat.setInteger(MediaFormat.KEY_WIDTH, width);
        mediaFormat.setString(MediaFormat.KEY_MIME, mAVCodeIDToMime.get(mCodeId));
        mediaFormat.setLong(MediaFormat.KEY_DURATION, duration);
        mediaFormat.setByteBuffer("csd-0", mBbcsd);
        
        mBbcsd.position(0);

        return CreateCodec(mediaFormat, surface) ? 1 : 0;
    }

    public synchronized void stopCodec(){
    	if(mDecoder != null){
    		try {
    			mDecoder.stop();
			} catch (Exception e) {
				Log.e(TAG, e.toString());
			}
    		
    		try {
    			mDecoder.release();
			} catch (Exception e) {
				Log.e(TAG, e.toString());
			}
    		
    		mDecoder = null;
    	}  
    }

    public synchronized boolean IsCodecPrepare(){
    	return (mDecoder != null);
    }
    
    public void ReCreateCodec(){
        if (mHWDecoderName != null){
            CreateCodec(mBbcsd);
        }
    }

    private boolean CreateCodec(MediaFormat format,Surface surface){
        if (mHWDecoderName == null || mHWDecoderName.length() == 0 || mDecoder != null)
            return false;

        try {
            mDecoder = MediaCodec.createByCodecName(mHWDecoderName);
        }
        catch (Exception e){
        	e.printStackTrace();
        	Log.e(TAG, "createByCodecName fail mHWDecoderName = " + mHWDecoderName);
        	return false;
        }
        
        try {
        	mDecoder.configure(format, surface, null, 0);
            mDecoder.start();
		} catch (Exception e) {
			e.printStackTrace();
			Log.e(TAG, "configure fail");
			return false;
		}
        
        FlushCodec();
        
        return  true;
    }

    public void FlushCodec(){
        if(mDecoder != null)
        	try {
        		mDecoder.flush();
			} catch (Exception e) {
				Log.e(TAG, e.toString());
			}    
    }
    
    public synchronized int Decode(ByteBuffer bBufIn,long timeStamp,ByteBuffer bBufOut){
    	
    	if (Build.VERSION.SDK_INT < 16){
    		return 0;
    	}
    	
        if(Build.VERSION.SDK_INT < 21){
        	return Decode16(bBufIn,timeStamp,bBufOut);
        }
        
       return Decode21(bBufIn,timeStamp,bBufOut);
    }

    @SuppressWarnings("deprecation")
	public synchronized int Decode16(ByteBuffer bBufIn,long timeStamp,ByteBuffer bBufOut){
    	 Log.e(TAG,"Decode enter timestamp = " + timeStamp);
    	 
         if(mDecoder == null)
             return 0;

		 try {
			 
			 ByteBuffer[] inputBuffers = mDecoder.getInputBuffers();
			 ByteBuffer[] outputBuffers = mDecoder.getOutputBuffers();
			 
			 long realTimeUs = 0;
	         BufferInfo info = new BufferInfo();
	         if (bBufIn != null){
	             int index;
	             while ((index = mDecoder.dequeueInputBuffer(1000)) < 0){
	                 int outIndex = mDecoder.dequeueOutputBuffer(info, 1000);
	                 switch (outIndex) {
	                     case -3:
	                    	 inputBuffers = mDecoder.getInputBuffers();
	                         break;
	                     case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
	                         MediaFormat mediaFormat= mDecoder.getOutputFormat();
	                         Log.e(TAG,"KEY_COLOR_FORMAT =  " + mediaFormat.getInteger(MediaFormat.KEY_COLOR_FORMAT));
	                         break;
	                     case MediaCodec.INFO_TRY_AGAIN_LATER:
	                         break;
	                     default:
	                    	 realTimeUs = info.presentationTimeUs;
	    	                 realTimeUs = realTimeUs < 0 ? 0 : realTimeUs;
	    	                 Log.e(TAG,"realTimeUs = " + realTimeUs);
	    	                 mDecoder.releaseOutputBuffer(outIndex, true);
	    	                 
	    	                 if(mOnDecoderOneFrameListener != null){
    	                    	mOnDecoderOneFrameListener.onDecoderOneFrame();
    	                     }  
	                 }
	             }

	             ByteBuffer buffer = inputBuffers[index];// mDecoder.getInputBuffer(index);
	             buffer.put(bBufIn);
	             mDecoder.queueInputBuffer(index, 0, bBufIn.limit(), timeStamp, 0);
	         }

	         int outIndex = mDecoder.dequeueOutputBuffer(info, 1000);
	         switch (outIndex) {
	             case -3:
	            	 outputBuffers = mDecoder.getOutputBuffers();
	                 return  0;
	             case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
	                 MediaFormat mediaFormat= mDecoder.getOutputFormat();
	                 Log.e(TAG,"KEY_COLOR_FORMAT =  " + mediaFormat.getInteger(MediaFormat.KEY_COLOR_FORMAT));
	                 return  0;
	             case MediaCodec.INFO_TRY_AGAIN_LATER:
	                 return  (int)realTimeUs;
	             default:
	                 realTimeUs = info.presentationTimeUs;
	                 realTimeUs = realTimeUs < 0 ? 0 : realTimeUs;
	                 Log.e(TAG,"realTimeUs = " + realTimeUs);
	                 mDecoder.releaseOutputBuffer(outIndex, true);
	                 
	                 if(mOnDecoderOneFrameListener != null){
                    	mOnDecoderOneFrameListener.onDecoderOneFrame();
                     }
	         }
	         return  (int)realTimeUs;
		} catch (Exception e) {
			return -1;
		}
    }
    
    public synchronized int Decode21(ByteBuffer bBufIn,long timeStamp,ByteBuffer bBufOut){
        Log.i("HardwareDecoder","HardwareDecoder21 Decode java enter timestamp = " + timeStamp + "size = "  + bBufIn.remaining());
        
        if(mDecoder == null)
            return 0;

        try {
        	long realTimeUs = 0;
            BufferInfo info = new BufferInfo();
            if (bBufIn != null){
                int index;
                while ((index = mDecoder.dequeueInputBuffer(1000)) < 0){
                    int outIndex = mDecoder.dequeueOutputBuffer(info, 10000);
                    switch (outIndex) {
                        case -3:
                            break;
                        case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                            MediaFormat mediaFormat= mDecoder.getOutputFormat();
                            Log.e(TAG,"HardwareDecoder21 Decode java KEY_COLOR_FORMAT =  " + mediaFormat.getInteger(MediaFormat.KEY_COLOR_FORMAT));
                            break;
                        case MediaCodec.INFO_TRY_AGAIN_LATER:
                        	Log.i(TAG,"HardwareDecoder21 Decode java INFO_TRY_AGAIN_LATER ");
                            break;
                        default:
                            mDecoder.getOutputBuffer(outIndex);
                            mDecoder.releaseOutputBuffer(outIndex,true);
                            
                            if(mOnDecoderOneFrameListener != null){
                            	mOnDecoderOneFrameListener.onDecoderOneFrame();
                            }
                            
                            realTimeUs = info.presentationTimeUs;
                            realTimeUs = realTimeUs < 0 ? 0 : realTimeUs;
                            Log.i(TAG, "HardwareDecoder21 Decode java releaseOutputBuffer realTimeUs = " + realTimeUs);
                    }
                }

                ByteBuffer buffer = mDecoder.getInputBuffer(index);
                buffer.put(bBufIn);
                mDecoder.queueInputBuffer(index, 0, bBufIn.limit(), timeStamp, 0);
            }

            int outIndex = mDecoder.dequeueOutputBuffer(info, 10000);
            switch (outIndex) {
                case -3:
                    return  0;
                case MediaCodec.INFO_OUTPUT_FORMAT_CHANGED:
                    MediaFormat mediaFormat= mDecoder.getOutputFormat();
                    Log.e(TAG,"HardwareDecoder21 Decode java KEY_COLOR_FORMAT =  " + mediaFormat.getInteger(MediaFormat.KEY_COLOR_FORMAT));
                    return  0;
                case MediaCodec.INFO_TRY_AGAIN_LATER:
                	 Log.i(TAG,"HardwareDecoder21 Decode java INFO_TRY_AGAIN_LATER ");
                    return  (int)realTimeUs;
                default:
                    mDecoder.getOutputBuffer(outIndex);
                    mDecoder.releaseOutputBuffer(outIndex, true);
                    
                    if(mOnDecoderOneFrameListener != null){
                    	mOnDecoderOneFrameListener.onDecoderOneFrame();
                    }
                    
                    realTimeUs = info.presentationTimeUs;
                    realTimeUs = realTimeUs < 0 ? 0 : realTimeUs;
                    Log.i(TAG,"HardwareDecoder21 Decode java realTimeUs = " + realTimeUs);
            }
            return  (int)realTimeUs;
		} catch (Exception e) {
			return -1;
		}
    }
    
    public interface OnDecoderOneFrameListener{
		void onDecoderOneFrame();
	}
    public void setOnDecoderOneFrameListener(OnDecoderOneFrameListener listener){
    	mOnDecoderOneFrameListener = listener;
    }
    private OnDecoderOneFrameListener mOnDecoderOneFrameListener;
}
