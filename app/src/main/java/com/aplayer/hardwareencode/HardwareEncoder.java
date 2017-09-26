package com.aplayer.hardwareencode;

import android.media.MediaCodec;
import android.os.Build;
import com.aplayer.aplayerandroid.Log;

import com.aplayer.hardwareencode.VideoEncoder.COLOR_FORMAT;
import com.aplayer.hardwareencode.module.RawFrame;
import com.aplayer.hardwareencode.utils.EncodeUtils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.aplayer.hardwareencode.Muxer.MUXER_FORMAT.MUXER_MP4;
import static com.aplayer.hardwareencode.module.EncoderConstant.*;

/**
 * Created by LZ on 2016/10/21.
 */

public class HardwareEncoder {
    private static final String         TAG              					= "APlayerAndroid";
    private String                      mEncodeOutpath;
    private Muxer.MUXER_FORMAT          mMuxerFmt;
    private Muxer                       mMuxer                              = null;
    private Map<Integer, EncoderBase>   mExternalIDToEncoderMap             = new HashMap<Integer, EncoderBase>();
    private int                         mTrackAllocID                       = 0;

    private double                      mVideoFrameRate                     =  25;
    private int                         mVideoBitRate                       =  2000 * 1000;
    private int                         mIFrameInterval                     =  5; // 秒
    private boolean                     mEncoding                           = false;
    private Object                      mLock                               = new Object();
    private int                         mVideoWidth                         = 0;
    private int                         mVideoHeight                        = 0;

    public HardwareEncoder(){
        this.mMuxerFmt      = MUXER_MP4;
    }

    public int start(){
        Log.i(TAG,"encoder start");

        if(null != mMuxer){
            Log.e(TAG, "encode is running...");
            return -1;
        }

        if(!initMuxer()){
            Log.e(TAG, "muxer init failed");
            return -1;
        }

        for (Map.Entry<Integer, EncoderBase> entry : mExternalIDToEncoderMap.entrySet()) {
            EncoderBase encoder = entry.getValue();
            if (null == encoder) {
                continue;
            }
            encoder.start();
        }

        mEncoding = true;
        return  1;
    }

    public int close(){
        Log.i(TAG,"encoder stop");

        synchronized (mLock){
            mEncoding = false;
            for (Map.Entry<Integer, EncoderBase> entry : mExternalIDToEncoderMap.entrySet()) {
                EncoderBase encoder = entry.getValue();
                if (null == encoder) {
                    continue;
                }
                encoder.stopEncode();
            }

            mExternalIDToEncoderMap.clear();
            
            Log.i(TAG,"encoder releaseMuxer s");
            releaseMuxer();
            Log.i(TAG,"encoder stop leave");
            
        }
        
        mVideoWidth   = 0;
        mVideoHeight  = 0;
        
        mVideoBitRate  =  1024 * 1024;
        
        return 1;
    }

    public int setOutFileName(String outFileName){
        mEncodeOutpath = outFileName;
        return 1;
    }

    public int addVideoTrack(int width,int height, int encodeFmt, double frameRate){
        int  videoWidth  = width;
        int  videoHeight = height;
        if(COLOR_FORMAT.COLOR_FormatSurface == encodeFmt && mVideoWidth != 0){
        		videoWidth = mVideoWidth;
        		videoHeight = videoWidth * height / width;
        }

//        else{
//            return _addVideoTrack(VideoEncoder.ENCODE_FORMAT.VIDEO_ACV,COLOR_FORMAT.YUV420P,width,height,mVideoFrameRate,mVideoBitRate,mIFrameInterval);
//        }

        mVideoFrameRate = frameRate;
        return _addVideoTrack(VideoEncoder.ENCODE_FORMAT.VIDEO_ACV,encodeFmt,videoWidth,videoHeight,mVideoFrameRate,mVideoBitRate,mIFrameInterval);
    }

    public int addVideoTrack(int width,int height,int encodeFmt, double frameRate, int bitRate){
        mVideoFrameRate = frameRate;
        mVideoBitRate   = bitRate;
        return _addVideoTrack(VideoEncoder.ENCODE_FORMAT.VIDEO_ACV, encodeFmt,width,height,frameRate,bitRate,mIFrameInterval);
    }

    public int addAudioTrack(int channelCount, int sampleRate, int bitRate){
        bitRate =  48000;
    	return _addAudioTrack(AudioEncoder.ENCODE_FORMAT.AUDIO_AAC,channelCount,sampleRate,bitRate);
    }

    public int addSubTrack(){
        return 0;
    }

    public int putRawData(int texture,long presentationTime){
    	 Log.i(TAG,"putRawData enter trackIndex " + " presentationTime " + presentationTime);
    	
        synchronized (mLock){
            if(!mEncoding){
                return ENCODE_NOT_STARTING;
            }

            VideoEncoderSurfaceInput videoEncoderSurface = getVideoSurfaceEncoder();
            if(videoEncoderSurface == null){
                return -1;
            }


            return videoEncoderSurface.renderTexture(texture,presentationTime)? 0 : -1;
        }

    }

    public int putRawData(int trackIndex, ByteBuffer rawData, int pts){
        Log.i(TAG,"putRawData enter trackIndex " + trackIndex + " rawData size " + rawData.limit() + " pts " + pts);

        if(!mEncoding){
            return ENCODE_NOT_STARTING;
        }

        EncoderBase encoder = mExternalIDToEncoderMap.get(trackIndex);
        if(null == encoder) {
            return TRACK_INDEX_INVALIDATE;
        }

        byte[] copyData = null;
        if(null != rawData && rawData.limit() > 0){
            copyData = new byte[rawData.limit()];
            rawData.get(copyData);
        }

        return encoder.feedRewData(new RawFrame(copyData,pts * 1000L,trackIndex));
    }

    public int setVideoWidth(int width){
    	if(mEncoding){
    		return 0;
    	}
    	
    	mVideoWidth = width;
    	return 1;
    }
    
    public int setVideoHeight(int height){
    	if(mEncoding){
    		return 0;
    	}
    	
    	mVideoHeight = height;
    	return 1;
    }
    
    public int setVideoBitRate(int bitRate){
    	if(mEncoding){
    		return 0;
    	}
    	
    	mVideoBitRate = bitRate * 1024;
    	return 1;
    }


    public boolean isEncoding(){
        return mEncoding;
    }

    public Object getVideoEncodeCapability(){

//        for (Map.Entry<Integer, EncoderBase> entry : mExternalIDToEncoderMap.entrySet()) {
//            EncoderBase encoder = entry.getValue();
//            if (null == encoder) {
//                continue;
//            }
//
//            if(encoder instanceof VideoEncoder){
//                return ((VideoEncoder) encoder).getEncodeCapability();
//            }
//        }
//        return  null;

        if(Build.VERSION.SDK_INT < 18)
        {
            return null;
        }

        String mime = VideoEncoder.ENCODE_FORMAT.VIDEO_ACV.getValue();
        MediaCodec mediaCodec = null;
        try {
            mediaCodec = EncodeUtils.createMediaCodecEncoder(mime);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if(null == mediaCodec)
        {
            return null;
        }
        else
        {
            return EncodeUtils.getEncodVieoeCapability(mediaCodec, mime);
        }

    }

    protected void putEncoderData(EncoderBase encoder,List<EncoderBase.EncodeFrame> encodeFrames){
        if(mMuxer != null){
            mMuxer.putMuxData(encoder,encodeFrames);
        }
    }

    protected int getTrackNum(){
        synchronized (mExternalIDToEncoderMap){
            return  mExternalIDToEncoderMap.size();
        }

    }

    private int _addVideoTrack(VideoEncoder.ENCODE_FORMAT encodeFormat, int colorFormat, int width, int height, double frameRate, int bitrate , int IFrameInterval){
        EncoderBase videoEncoder = null;

        if(colorFormat == COLOR_FORMAT.COLOR_FormatSurface){
            videoEncoder = new VideoEncoderSurfaceInput(this,encodeFormat,width,height,frameRate,bitrate,IFrameInterval);
        }else{
            videoEncoder = new VideoEncoder(this,encodeFormat,colorFormat,width,height,frameRate,bitrate,IFrameInterval);
        }

        if(videoEncoder.init()){
            return addTrack(videoEncoder);
        }
    	return -1;
    }
    
    private int _addAudioTrack(AudioEncoder.ENCODE_FORMAT encodeFormat, int channelCount, int sampleRate, int bitRate){
    	AudioEncoder audioEncoder = new AudioEncoder(this,encodeFormat, channelCount, sampleRate,bitRate);
        if(audioEncoder.init()){
    	    return addTrack(audioEncoder);
        }
        return -1;
    }

    private int addTrack(EncoderBase encoder) {
        int trackID = -1;
        synchronized (this)
        {
            if(null != encoder && !mExternalIDToEncoderMap.containsValue(encoder))
            {
                trackID = mTrackAllocID;
                mExternalIDToEncoderMap.put(trackID, encoder);
                mTrackAllocID++;
            }
        }

        return trackID;
    }

    public VideoEncoderSurfaceInput getVideoSurfaceEncoder(){
        for (Map.Entry<Integer, EncoderBase> entry : mExternalIDToEncoderMap.entrySet()) {
            EncoderBase encoder = entry.getValue();
            if (null == encoder) {
                continue;
            }

            if(encoder instanceof VideoEncoderSurfaceInput){
                return (VideoEncoderSurfaceInput) encoder;
            }
        }
        return  null;
    }

    private boolean initMuxer(){
    	if(mEncodeOutpath == null){
    		Log.e(TAG, "initMuxer: outpath is null");
    		return false;
    	}
    	
        mMuxer = new Muxer(this,mEncodeOutpath, mMuxerFmt);
        if(mMuxer.init()){
            mMuxer.start();
            return true;
        }
        return  false;
    }

    private void releaseMuxer(){
        if(null != mMuxer){
            mMuxer.stopMux();
            mMuxer = null;
        }
    }
}
