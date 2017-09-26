package com.aplayer.hardwareencode;

import android.annotation.SuppressLint;
import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.Build;
import com.aplayer.aplayerandroid.Log;
import com.aplayer.hardwareencode.module.EncoderConstant;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import static android.content.ContentValues.TAG;

/**
 * Created by LZ on 2016/10/19.
 */

public class Muxer extends Thread{
    private HardwareEncoder mHardwareEncoder = null;
    private  MUXER_FORMAT               mMuxerFmt                   = null;
    private  String                     mOutputPath                 = null;
    private  MediaMuxer                 mMuxer                      = null;
    private  Map<EncoderBase, Integer>  mEncoderToInnerTrackIDMap   = new HashMap<EncoderBase, Integer>();
    private  Queue<MuxerData>           mMuxQueue               	= new LinkedList<MuxerData>();
    private volatile boolean            mRunning                    = true;
    private volatile boolean            mMuxStart                   = false;


    public  Muxer(HardwareEncoder hardwareEncoder, String path, MUXER_FORMAT muxerFmt)
    {
        mHardwareEncoder = hardwareEncoder;
        mOutputPath = path;
        mMuxerFmt   = muxerFmt;
    }

    @SuppressLint("NewApi")
	public boolean init() {
        if(null != mMuxer){
            Log.e(TAG, "Muter is already init, before call this, must call stop()!");
        }

        if(!isSupportMuxter()) {
            Log.e(TAG, "System Version not support!");
            return  false;
        }

        if(!checkParam()){
            return  false;
        }

        try {
            mMuxer = new MediaMuxer(mOutputPath, mMuxerFmt.getValue());
        } catch (IOException e) {
            e.printStackTrace();
            mMuxer = null;
            Log.e(TAG, "Create MediaMuxer Failed!");
            return  false;
        }

        return mRunning  = (null != mMuxer);
    }

    @SuppressLint("NewApi")
	public void stopMux(){
        mRunning = false;
        try {
            this.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if(mMuxer != null){
            synchronized (mMuxer){
                if(mMuxStart){
                    try
                    {
                        mMuxer.stop();          //添加流，必须在Muxter start 前
                        mMuxer.release();       // MediaMuxer的使用要按照Constructor -> addTrack -> start -> writeSampleData -> stop 的顺序。如果既有音频又有视频，在stop前两个都要writeSampleData()过
                    }
                    catch (Exception e)
                    {
                        Log.e(TAG, "Mutex stop error!");
                        e.printStackTrace();
                    }

                    mMuxStart = false;
                }

                mMuxer = null; 
            }
            
        }

        mMuxQueue.clear();
        mEncoderToInnerTrackIDMap.clear();
    }

    @SuppressLint("NewApi")
	public void putMuxData(EncoderBase encoder, List<EncoderBase.EncodeFrame> encodeFrames){
        if(null == encodeFrames){
            Log.e(TAG, "encode return a null list");
            return;
        }

        for (EncoderBase.EncodeFrame encodeFrame : encodeFrames)
        {
            if(null != encodeFrame.newFormat){
                Log.i(TAG,"Muxer has new newFormat");
                int innerTrackIndex = addTrack(encodeFrame.newFormat);
                if(-1 == innerTrackIndex){
                    break;
                }

                updateInnerTrackIdMap(encoder, innerTrackIndex);
                boolean isAllEncodeGetNewFormat = (mEncoderToInnerTrackIDMap.size() == mHardwareEncoder.getTrackNum());
                if(isAllEncodeGetNewFormat){
                    Log.i(TAG,"Muxer isAllEncodeGetNewFormat");
                    synchronized (mMuxer){
                        mMuxStart = true;
                        mMuxer.start();
                    }
                }
            }

            Integer innerTrackIndex = mEncoderToInnerTrackIDMap.get(encoder);
            if(null == innerTrackIndex){
                Log.e(TAG, "invalidate encoder");
                return;
            }

            synchronized (mMuxQueue){
                while(!mMuxQueue.offer(new MuxerData(encodeFrame, innerTrackIndex )));
            }
        }
    }

    @SuppressWarnings("finally")
    @Override
    public void run() {
        while (mRunning){
            if(mMuxQueue.isEmpty() || !mMuxStart){
                try {
                    Thread.sleep(EncoderConstant.MUX_TIME_INTERVAL);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                finally {
                    continue;
                }
            }

            mux();
        }

        Log.i(TAG,"mRunning is false while is over");
        
        while (!mMuxQueue.isEmpty() && mMuxStart) {
            Log.e(TAG, "MuxThread mMuxQueue not empepty");
            mux();
        }
        Log.i(TAG,"Muxer mux thread leave");
    }

    private int addTrack(MediaFormat mediaFormat) {
        if(null == mMuxer) {
            Log.e(TAG, "before call this function, please confirm init is success!");
            return -1;
        }

        if(null == mediaFormat) {
            Log.e(TAG, "mediaFormat is null");
            return -1;
        }

        if(mMuxStart)
        {
            Log.e(TAG, "muxer is start, can't call this function, after mutxer start!");
            return -1;
        }
        synchronized (mMuxer){
        	return mMuxer.addTrack(mediaFormat);
        }
    }

    @SuppressLint("NewApi")
	private void mux() {
        MuxerData muxerData = null;
        synchronized (mMuxQueue) {
            muxerData = mMuxQueue.poll();
        }

        if (null == muxerData || null == muxerData.encodeFrame ||
                null == muxerData.encodeFrame.buffInfo ||
                null == muxerData.encodeFrame.buff ||
                muxerData.encodeFrame.buff.length <= 0) {
            Log.e(TAG, "invalidate muxer data !");
            return;
        }

        MediaCodec.BufferInfo    bufferInfo = muxerData.encodeFrame.buffInfo;
        ByteBuffer buf = ByteBuffer.wrap(muxerData.encodeFrame.buff);

        if (bufferInfo.size < 0 || bufferInfo.offset < 0
                || (bufferInfo.offset + bufferInfo.size) > buf.capacity()
                || bufferInfo.presentationTimeUs < 0) {

            Log.e(TAG,"Muxer trackID = " + muxerData.trackID + " bufferInfo size = " + bufferInfo.size + " offset = " + bufferInfo.offset + " capacity = "
                    + buf.capacity() + " presentationTimeUs = " + bufferInfo.presentationTimeUs);
            return;
        }

        Log.i(TAG,"Muxer trackID = " + muxerData.trackID + " bufferInfo size = " + bufferInfo.size + " offset = " + bufferInfo.offset + " capacity = "
                + buf.capacity() + " presentationTimeUs = " + bufferInfo.presentationTimeUs);
        
        synchronized (mMuxer) {
            mMuxer.writeSampleData(muxerData.trackID, buf, bufferInfo);
        }
        
        Log.i(TAG,"Muxer writeSampleData leave");
    }

    private static boolean isSupportMuxter() {
        return Build.VERSION.SDK_INT >= 18;
    }

    private boolean checkParam() {
        boolean isParamValid = false;

        do {
            if(null == mOutputPath || mOutputPath.isEmpty()){
                Log.e(TAG, "invalidate path, outpath is " + mOutputPath);
                break;
            }

            if(null == mMuxerFmt) {
                Log.e(TAG, "muxfmt is null");
                break;
            }

            isParamValid = true;
        }while(false);
        return isParamValid;
    }

    private boolean updateInnerTrackIdMap(EncoderBase encoder, int innerTrackID)
    {
        if(null == encoder ){
            Log.e(TAG, "invalidate encoder!");
            return  false;
        }

        synchronized (mEncoderToInnerTrackIDMap){
            if(null != mEncoderToInnerTrackIDMap.get(encoder)){
                Log.e(TAG, "format change more than once");
            }
            mEncoderToInnerTrackIDMap.put(encoder, innerTrackID);
        }

        return  true;
    }

    public enum  MUXER_FORMAT
    {
        MUXER_MP4 (MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);

        private final int value;
        MUXER_FORMAT(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    public class MuxerData{
        public EncoderBase.EncodeFrame  encodeFrame;
        public int                      trackID;

        public MuxerData(EncoderBase.EncodeFrame encodeFrame, int trackID) {
            this.encodeFrame = encodeFrame;
            this.trackID = trackID;
        }
    }
}
