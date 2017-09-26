package com.aplayer.hardwareencode;

import android.annotation.SuppressLint;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Build;
import com.aplayer.aplayerandroid.Log;
import android.util.Range;

import com.aplayer.hardwareencode.module.EncoderConstant;
import com.aplayer.hardwareencode.utils.EncodeUtils;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by LZ on 2016/10/17.
 */

public class VideoEncoder extends EncoderBase {

    private     double mFrameRate;
    private     int mBitRate;
    private     int mIFrameInterval;
    private     int mColorFormat;
    private     ENCODE_FORMAT mEncodeFormat;
    private     int mRawFrameNum = 0;
    private     long mPrevEncodeFrameTimeUS = 0;
    private     long mFirstPts = -1;

    protected   MediaCodec mediaCodec;
    protected   int mWidth;
    protected   int mHeight;

    private     int[] mColors = {MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible,MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar,
                                 MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar};

    private static final int TIMEOUT_USEC = 12000;
    private static final int TIME_BASE_MICROSECOND = 1000000;

    private static final String ERROR_TAGE = "Aplayer_ERROR" + VideoEncoder.class.getSimpleName();
    private static final String INFO_TAGE = "Aplayer_INFO" + VideoEncoder.class.getSimpleName();
    private static final String TAG = "APlayerAndroid";


    public enum  ENCODE_FORMAT
    {
        VIDEO_ACV  ("video/avc");
        private final String value;
        ENCODE_FORMAT(String value) {
            this.value = value;
        }
        public String getValue() {
            return value;
        }
    }

    public static class COLOR_FORMAT
    {
        public static final int COLOR_FormatSurface = MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface;
        public static final int YUV420P = MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar;
        public static final int NV12 = MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar;
        public static final int YUV420Flexible = MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible;
    }

    public VideoEncoder(HardwareEncoder hardwareEncoder, ENCODE_FORMAT encodeFormat, int colorFormat, int width, int height, double frameRate, int bitrate , int IFrameInterval) {
        super(hardwareEncoder);
        mWidth          = width;
        mHeight         = height;
        mFrameRate      = frameRate;
        mBitRate        = bitrate;
        mColorFormat    = colorFormat;
        mEncodeFormat   = encodeFormat;
        mIFrameInterval = IFrameInterval;
        mFirstPts       = -1;
        mMaxInputQueueSize = EncoderConstant.MAX_VIDEO_INPUT_QUEUE_SIZE;
    }


//    @SuppressLint("NewApi")
//	public int getSupportCapbility(){
//        if(mEncodeFormat != null && mEncodeFormat!= null){
//            String mime = mEncodeFormat.getValue();
//            int[] deviceColor = mediaCodec.getCodecInfo().getCapabilitiesForType(mime).colorFormats;
//
//            for(int i = 1;i < mColors.length;i++){
//                for(int j = 0;j < deviceColor.length;j++){
//                    if(deviceColor[j] == mColors[i]){
//                        return mColors[i];
//                    }
//                }
//            }
//        }
//
//        return 0;
//    }


    public EncodeUtils.EncodeVideoCapability getEncodeCapability()
    {
        if(mEncodeFormat != null || mediaCodec!= null || Build.VERSION.SDK_INT < 18) {
            return null;
        }

        return EncodeUtils.getEncodVieoeCapability(mediaCodec, mEncodeFormat.getValue());
    }

    private boolean checkParam(){
        boolean isParamValid = false;

        do{
            if(0 >=  mWidth || 0 >=  mHeight || 0 >= mFrameRate || 0 >= mBitRate)
            {
                String errorMsg = String.format("param is not invalidate, Width = %d, Height = %d, FrameRate = %d, BitRate = %d",
                        mWidth, mHeight, mFrameRate, mBitRate);
                Log.e(ERROR_TAGE, errorMsg);
                break;
            }

            if(COLOR_FORMAT.COLOR_FormatSurface != mColorFormat &&
               COLOR_FORMAT.YUV420P != mColorFormat &&
               COLOR_FORMAT.NV12     != mColorFormat &&
               COLOR_FORMAT.YUV420Flexible != mColorFormat)
            {
                Log.e(ERROR_TAGE, "Error not support format: " + mColorFormat);
                break;
            }

            isParamValid = true;
        }while(false);
        return isParamValid;
    }

    private static boolean isSupportEncoder(){
        return Build.VERSION.SDK_INT >= 16;
    }

    private MediaFormat createMediaCodecFormat(int colorFmt, int width, int height, Integer profile, Integer level){
        String mime = mEncodeFormat.getValue(); // "video/avc"

        MediaFormat mediaFormat = MediaFormat.createVideoFormat(mime, width, height);
        mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, colorFmt);
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, mBitRate);
        mediaFormat.setFloat(MediaFormat.KEY_FRAME_RATE, (float) mFrameRate*1.0f);
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, mIFrameInterval);

        if(null != profile)
        {
            //mediaFormat.setInteger(MediaFormat.KEY_PROFILE, MediaCodecInfo.CodecProfileLevel.AVCProfileHigh);
            mediaFormat.setInteger(MediaFormat.KEY_PROFILE, profile.intValue());
        }

        if(null != level)
        {
            //mediaFormat.setInteger(MediaFormat.KEY_LEVEL, MediaCodecInfo.CodecProfileLevel.AVCLevel4);
            //mediaFormat.setInteger(MediaFormat.KEY_LEVEL, level.intValue());
        }

        return mediaFormat;
    }

    protected void beforeMediaCodecStart(MediaCodec mediaCodec){

    }

    @SuppressLint("NewApi")
	public boolean init()
    {
        boolean isInitSuccess = false;
        mFirstPts  = -1;
        do {
            if(!isSupportEncoder()) {
                Log.e(ERROR_TAGE, "Not Support HardWareEncoder");
                break;
            }

            if(!checkParam()){
                break;
            }

            String mime = mEncodeFormat.getValue(); // "video/avc"
            try {
                mediaCodec = EncodeUtils.createMediaCodecEncoder(mime);
            } catch (Exception e) {
                e.printStackTrace();
                Log.e(ERROR_TAGE, "createEncoderByType() failed!");
                break;
            }

            VideoEncodeParam encodeParam = findBestMatchProfileLeve(mediaCodec, mime);
            Integer profile = null;//(null != encodeParam) ? encodeParam.codecProfileLevel.profile : null;
            Integer level =  null;//(null != encodeParam) ? encodeParam.codecProfileLevel.level : null;
            int width  = mWidth;
            int height = mHeight;
            if(null != encodeParam)
            {
                if(null != encodeParam.codecProfileLevel)
                {
                    profile = encodeParam.codecProfileLevel.profile;
                    level = encodeParam.codecProfileLevel.level;
                }

                if(null != encodeParam.widthRange && !encodeParam.widthRange.contains(width))
                {
                    Integer widthUpper = encodeParam.widthRange.getUpper();
                    Integer widthLower = encodeParam.widthRange.getLower();
                    width = (width > widthUpper) ? widthUpper : width;
                    width = (width < widthLower) ? widthLower : width;
                }

                if(null != encodeParam.heightRanger && !encodeParam.heightRanger.contains(height))
                {
                    Integer heightUpper = encodeParam.heightRanger.getUpper();
                    Integer heightLower = encodeParam.heightRanger.getLower();
                    height = (height > heightUpper) ? heightUpper : height;
                    height = (height < heightLower) ? heightLower : height;
                }
            }

            MediaFormat mediaFormat = createMediaCodecFormat(mColorFormat, width, height, profile, level);

            try
            {
                mediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            }
            catch (Exception e)
            {
                return false;
            }
            //mediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
            beforeMediaCodecStart(mediaCodec);
            mediaCodec.start();
            isInitSuccess = true;
        }while(false);

        mRunning = isInitSuccess;
        return isInitSuccess;
    }

    public void release()
    {
        Log.i(TAG,"VideoEncoder release");
        try {
            mediaCodec.stop();
            mediaCodec.release();
        } catch (Exception e){
            e.printStackTrace();
            Log.e(ERROR_TAGE, "mediaCodec.release() failed!");
        }

        mPrevEncodeFrameTimeUS = 0;
    }

    @Override
    public boolean feedRawData(byte[] input, long pts, long timeoutUs) {
        ByteBuffer[] inputBuffers = mediaCodec.getInputBuffers();
        int inputBufferIndex = mediaCodec.dequeueInputBuffer(timeoutUs);
        if (inputBufferIndex >= 0) {
            pts = (pts <= 0) ? computePresentationTime(mRawFrameNum) : pts;

            ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
            inputBuffer.clear();
            inputBuffer.put(input);
            mediaCodec.queueInputBuffer(inputBufferIndex, 0, input.length, pts, 0);
            mRawFrameNum += 1;

            return true;
        }

        return false;
    }

    @Override
    public List<EncodeFrame> fetchEncodeData() {
        ByteBuffer[] outputBuffers = mediaCodec.getOutputBuffers();

        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        ArrayList<EncodeFrame> encodeFrames = new ArrayList<EncodeFrame>();
        while(true){
            int encoderStatus = mediaCodec.dequeueOutputBuffer(bufferInfo, TIMEOUT_USEC);
            if(MediaCodec.INFO_TRY_AGAIN_LATER == encoderStatus){
                break;
            }
            else if(encoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED){
                outputBuffers = mediaCodec.getOutputBuffers();
            }
            else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                MediaFormat newFormat = mediaCodec.getOutputFormat();
                EncodeFrame encodeFrame = new EncodeFrame(null, null, newFormat);
                encodeFrames.add(encodeFrame);
            }
            else if(encoderStatus < 0){
                Log.e(ERROR_TAGE, "unexpected result from encoder.dequeueOutputBuffer: " +  encoderStatus);
            }
            else{
                ByteBuffer encodedData = outputBuffers[encoderStatus];
                if (encodedData == null) {
                    throw new RuntimeException("encoderOutputBuffer " + encoderStatus + " was null");
                }

                if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    // The codec config data was pulled out and fed to the muxer when we got
                    // the INFO_OUTPUT_FORMAT_CHANGED status.  Ignore it.
                    bufferInfo.size = 0;
                }

                if (bufferInfo.size != 0) {
                    // adjust the ByteBuffer values to match BufferInfo (not needed?)
                    encodedData.position(bufferInfo.offset);
                    encodedData.limit(bufferInfo.offset + bufferInfo.size);

                    MediaCodec.BufferInfo buffInfoCopy = EncodeUtils.bufferInfoDup(bufferInfo);
                    if(mFirstPts == -1){
                        mFirstPts = buffInfoCopy.presentationTimeUs;
                    }

                    buffInfoCopy.presentationTimeUs -= mFirstPts;
                    buffInfoCopy.presentationTimeUs = correctionBuffInfoTime(bufferInfo.presentationTimeUs);
                    byte[] outData = new byte[bufferInfo.size];
                    encodedData.get(outData);
                    EncodeFrame encodeFrame = new EncodeFrame(outData, buffInfoCopy, null);
                    encodeFrames.add(encodeFrame);
                }

                mediaCodec.releaseOutputBuffer(encoderStatus, false);

                if ((bufferInfo.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    Log.e(INFO_TAGE, "reached end of stream unexpectedly");
                    break;      // out of while
                }
            }

        }

        return encodeFrames;
    }

    @Override
    public MediaFormat getMediaFormat() {
        MediaFormat mediaFormat = (null != mediaCodec) ? mediaCodec.getOutputFormat() : null;
        return mediaFormat;
    }

//    @SuppressLint("NewApi")
//	private   Integer colorFmtToCodecFmt(COLOR_FORMAT colorFormat)
//    {
//        Integer retFmt = null;
//        if(null == colorFormat){
//            return null;
//        }
//
//        if(COLOR_FORMAT.YUV420P == colorFormat) {
//            if(mediaCodec != null && mEncodeFormat!= null){
//                String mime = mEncodeFormat.getValue();
//                int[] deviceColor = mediaCodec.getCodecInfo().getCapabilitiesForType(mime).colorFormats;
//
//                for(int i = 0;i < mColors.length;i++){
//                    for(int j = 0;j < deviceColor.length;j++){
//                        if(deviceColor[j] == mColors[i]){
//                            return mColors[i];
//                        }
//                    }
//                }
//            }
//
//
//            return MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible;
//        }
//        else if(COLOR_FORMAT.COLOR_FormatSurface == colorFormat){
//            retFmt = MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface;
//        }
//
//        return retFmt;
//    }

    //getSupportProfileLevel
    @SuppressLint("NewApi")
	private  static VideoEncodeParam findBestMatchProfileLeve(MediaCodec mediaCodec, String mime)
    {
        if(Build.VERSION.SDK_INT < 16 || null == mediaCodec || null == mime)
        {
            return null;
        }

        MediaCodecInfo mediaCodecInfo = EncodeUtils.getMediaCodecInfo(mediaCodec);
        MediaCodecInfo.CodecCapabilities capabilitiesForType = (null != mediaCodecInfo) ? mediaCodecInfo.getCapabilitiesForType(mime) : null;
        if(null == capabilitiesForType || null == capabilitiesForType.profileLevels)
        {
            return null;
        }

        ArrayList supportList = new ArrayList();
        //绛涢�鏀寔鐨勭瓑绾�
        for (MediaCodecInfo.CodecProfileLevel codecProfileLevel : capabilitiesForType.profileLevels)
        {
            if(MediaCodecInfo.CodecProfileLevel.AVCProfileHigh == codecProfileLevel.profile       ||
                    MediaCodecInfo.CodecProfileLevel.AVCProfileMain == codecProfileLevel.profile  ||
                    MediaCodecInfo.CodecProfileLevel.AVCProfileExtended == codecProfileLevel.profile ||
                    MediaCodecInfo.CodecProfileLevel.AVCProfileBaseline == codecProfileLevel.profile)
            {
                supportList.add(codecProfileLevel);
            }
        }

        //鎺掑簭 鎸夌収 Profile / Level /瀹介珮鐨勬柟寮忔帓搴�
        for (int i = 0; i < supportList.size() - 1; ++i)
        {
            for(int j = i + 1; j < supportList.size(); ++j)
            {
                MediaCodecInfo.CodecProfileLevel  codecProfileLevelI = (MediaCodecInfo.CodecProfileLevel)supportList.get(i);
                MediaCodecInfo.CodecProfileLevel  codecProfileLevelJ = (MediaCodecInfo.CodecProfileLevel)supportList.get(j);
                if(codecProfileLevelI.profile < codecProfileLevelJ.profile ||
                        (codecProfileLevelI.profile ==codecProfileLevelJ.profile && codecProfileLevelI.level < codecProfileLevelJ.level))
                {
                    //Swap
                    supportList.set(i, codecProfileLevelJ);
                    supportList.set(j, codecProfileLevelI);
                }
            }
        }


        VideoEncodeParam videoEncodeParam = null;
        if(!supportList.isEmpty())
        {
            Range<Integer> widthRange = null;
            Range<Integer> heightRange = null;

            MediaCodecInfo.CodecProfileLevel  codecProfileLevel = (MediaCodecInfo.CodecProfileLevel)supportList.get(0);
            if(Build.VERSION.SDK_INT >= 21)
            {
                MediaCodecInfo.VideoCapabilities videoCapabilities = capabilitiesForType.getVideoCapabilities();
                heightRange = videoCapabilities.getSupportedHeights();
                widthRange  = videoCapabilities.getSupportedWidths();
            }

            videoEncodeParam = new VideoEncodeParam(codecProfileLevel, widthRange, heightRange);
        }

        return videoEncodeParam;
    }

    private long computePresentationTime(long frameIndex) {
        double time = frameIndex * (TIME_BASE_MICROSECOND / mFrameRate);
        return (long)time;
    }

    //閮ㄥ垎鎵嬫満 濡侽PPOR7m 鍦ㄨ缃瓵VC缂栫爜鐨凱rofile 鍜孡evel鍚庯紝鏃舵埑寮傚父 - 涓嶆槸鍗曡皟閫掑搴忓垪
    private long correctionBuffInfoTime(long timeUS)
    {
        long retTimeUS = timeUS;
        final long MAX_DEVIATION_RANGE_US = 10 * 1000;
        if(Math.abs(timeUS - mPrevEncodeFrameTimeUS) > MAX_DEVIATION_RANGE_US)
        {
            retTimeUS = mPrevEncodeFrameTimeUS;
        }

        mPrevEncodeFrameTimeUS = retTimeUS + (long)((1000 * 1000) / mFrameRate);
        return retTimeUS;
    }

    private static class VideoEncodeParam
    {
        MediaCodecInfo.CodecProfileLevel codecProfileLevel = null;
        Range<Integer> widthRange = null;
        Range<Integer> heightRanger = null;

        public VideoEncodeParam(MediaCodecInfo.CodecProfileLevel codecProfileLevel, Range<Integer> widthRange, Range<Integer> heightRanger) {
            this.codecProfileLevel = codecProfileLevel;
            this.widthRange = widthRange;
            this.heightRanger = heightRanger;
        }
    }
}
