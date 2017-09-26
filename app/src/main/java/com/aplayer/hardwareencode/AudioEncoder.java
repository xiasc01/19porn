package com.aplayer.hardwareencode;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Build;
import com.aplayer.aplayerandroid.Log;

import com.aplayer.hardwareencode.module.EncoderConstant;
import com.aplayer.hardwareencode.module.RawFrame;
import com.aplayer.hardwareencode.utils.EncodeUtils;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import static android.content.ContentValues.TAG;
import static android.media.MediaFormat.MIMETYPE_AUDIO_AAC;
import static android.media.MediaFormat.MIMETYPE_AUDIO_MPEG;
import static com.aplayer.hardwareencode.module.EncoderConstant.QUEUE_FULL;
import static com.aplayer.hardwareencode.module.EncoderConstant.SUCCESS;

/**
 * Created by LZ on 2016/10/18.
 */

public class AudioEncoder extends EncoderBase{
    private  int mChannelCount;
    private  int mSampleRate;
    private  int mBitRate;

    private MediaCodec mediaCodec;
    private ENCODE_FORMAT mEncodeFormat;
    private long          mFirstPts = -1;

    private int TIMEOUT_USEC = 12000;

    private static final String ERROR_TAGE = "APlayerAndroid";
    private static final String INFO_TAGE = "APlayerAndroid";
    private long    mPts = 0;
    private long    mLastpts = -1;
    private  byte[] mInputMerge = null; //位提供编码效率，保证投递给编码器的数据 每帧 >100ms，编码前进行合并
    private  long   mInputMergePts = 0;
    private  final int MERGE_FRAME_LIMETE_MS = 100;
    private  int   mMergeFrameLimete = (int)(1.0 * MERGE_FRAME_LIMETE_MS * 44100 * 2 /1000 );//每帧 >100ms，编码前进行合并

    public static final int AACCodecProfileLevel = MediaCodecInfo.CodecProfileLevel.AACObjectMain;
    public static final int[] AAC_STANDARD_SAMPLE_RATE= {
            96000,
            88200,
            64000,
            48000,
            44100,
            32000,
            24000,
            22050,
            16000,
            12000,
            11025,
            8000 ,
            7350 };

        public enum  ENCODE_FORMAT
        {
            AUDIO_AAC  (MIMETYPE_AUDIO_AAC),
            AUDIO_MPEG (MIMETYPE_AUDIO_MPEG);

            private final String value;
            ENCODE_FORMAT(String value) {
                this.value = value;
            }

            public String getValue() {
                return value;
            }
        }

        public AudioEncoder(HardwareEncoder hardwareEncoder, ENCODE_FORMAT encodeFormat, int channelCount, int sampleRate, int bitRate) {
            super(hardwareEncoder);
            mEncodeFormat = encodeFormat;
            mChannelCount = channelCount;
            mSampleRate   = sampleRate;
            mBitRate = bitRate;
            mMaxInputQueueSize = EncoderConstant.MAX_AUDIO_INPUT_QUEUE_SIZE;
            mMergeFrameLimete  =  (int)(1.0* MERGE_FRAME_LIMETE_MS * mSampleRate * mChannelCount /1000 );
        }

        private boolean checkParam()
        {
            boolean isParamValid = false;

            do{
                if(0 >=  mChannelCount || 0 >=  mSampleRate || 0 >= mBitRate)
                {
                    String errorMsg = String.format("param is not invalidate, ChannelCount = %d, SampleRate = %d, BitRate = %d",
                            mChannelCount, mSampleRate, mBitRate);
                    Log.e(ERROR_TAGE, errorMsg);
                    break;
                }

                final int[] supportSampleRate = getSupportSampleRate(mEncodeFormat);
                if(null != supportSampleRate && indexOfArray(AAC_STANDARD_SAMPLE_RATE, mSampleRate) < 0)
                {
                    Log.e(ERROR_TAGE, "Not Support Sample Rate = " + mSampleRate);
                    break;
                }

                isParamValid = true;
            }while(false);
            return isParamValid;
        }

        private static boolean isSupportEncoder()
        {
            return Build.VERSION.SDK_INT >= 16;
        }

        public boolean init()
        {
            boolean isInitSuccess = false;
            mFirstPts  = -1;
            do
            {
                if(!isSupportEncoder())
                {
                    Log.e(ERROR_TAGE, "Not Support HardWareEncoder");
                    break;
                }

                if(!checkParam())
                {
                    break;
                }

                String mime = mEncodeFormat.getValue(); // "audio/mp4a-latm"
                MediaFormat format = new MediaFormat();
                //MediaFormat format = MediaFormat.createAudioFormat(mime, mSampleRate, mChannelCount);

                format.setString(MediaFormat.KEY_MIME, mime);
                format.setInteger(MediaFormat.KEY_CHANNEL_COUNT, mChannelCount);
                format.setInteger(MediaFormat.KEY_SAMPLE_RATE, mSampleRate);
                format.setInteger(MediaFormat.KEY_BIT_RATE, mBitRate);//64 * 1024 AAC-HE 64kbps
                format.setInteger(MediaFormat.KEY_AAC_PROFILE, AACCodecProfileLevel);
                format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 100 * 1024);//浣滅敤浜巌nputBuffer鐨勫ぇ灏�

                try {
                    //mediaCodec = MediaCodec.createEncoderByType(mime);
                    mediaCodec = EncodeUtils.createMediaCodecEncoder(mime);
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                    Log.e(ERROR_TAGE, "createEncoderByType() failed!");
                    break;
                }

                mediaCodec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
                mediaCodec.start();
                isInitSuccess = true;
            }while(false);

            mRunning = isInitSuccess;

            return isInitSuccess;
        }

        public void release()
        {
            try {
                mediaCodec.stop();
                mediaCodec.release();
            } catch (Exception e){
                e.printStackTrace();
                Log.e(ERROR_TAGE, "mediaCodec.release() failed!");
            }
        }


    @Override
    public boolean feedRawData(byte[] input, long pts, long timeoutUs) {

        if(null == input)
        {
            return false;
        }

        //合并数据
        if(null != mInputMerge)
        {
            byte[] mergeFrame = new byte[mInputMerge.length + input.length];
            System.arraycopy(mInputMerge, 0, mergeFrame, 0, mInputMerge.length);
            System.arraycopy(input, 0, mergeFrame, mInputMerge.length, input.length);
            mInputMerge = mergeFrame;
        }
        else
        {
            mInputMerge = input;
            mInputMergePts = pts;
        }

        if(mInputMerge.length < mMergeFrameLimete)
        {
            return true;
        }

        boolean bret = false;
        long mergeFrmaeValidatePts = 0;
        ByteBuffer[] inputBuffers = mediaCodec.getInputBuffers();
        ByteBuffer[] outputBuffers = mediaCodec.getOutputBuffers();
        int inputBufferIndex = mediaCodec.dequeueInputBuffer(timeoutUs);
        if (inputBufferIndex >= 0) {
        	if(mInputMergePts <= 0){
                mergeFrmaeValidatePts = mPts;
                computePresentationTime(mInputMerge.length);
        	}
        	else
            {
                mPts = mInputMergePts;
                mergeFrmaeValidatePts  = mInputMergePts;
            }
            
            ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
            inputBuffer.clear();
            inputBuffer.put(mInputMerge);
            mediaCodec.queueInputBuffer(inputBufferIndex, 0, mInputMerge.length, mergeFrmaeValidatePts, 0);
            bret =  true;
        }

        mInputMerge = null;
        mInputMergePts = 0;
        return bret;
    }

    private void computePresentationTime(int dataSize){
        mPts += (long)(dataSize * 1000000L / mSampleRate / mChannelCount / 2);
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
                Log.i(TAG,"AudioEncoder has a new Format");
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
                    Log.i(ERROR_TAGE, "Get Buffer Success! flag = "+bufferInfo.flags+",pts = "+ bufferInfo.presentationTimeUs);
                    // adjust the ByteBuffer values to match BufferInfo (not needed?)
               
                    encodedData.position(bufferInfo.offset);
                    encodedData.limit(bufferInfo.offset + bufferInfo.size);

                    byte[] aacRawFrame = new byte[bufferInfo.size];                 //aac编码后的裸流（不带ADTS头），MP4 复用器使用不带ADTS头的AAC编码裸流
                    encodedData.get(aacRawFrame);

                    //out.write(aacRawFrame, 0, aacRawFrame.length);
                    MediaCodec.BufferInfo aacBuffInfo = EncodeUtils.bufferInfoDup(bufferInfo);
                    aacBuffInfo.size = aacRawFrame.length;
                    aacBuffInfo.offset = 0;

                    if(mFirstPts == -1){
                        mFirstPts = aacBuffInfo.presentationTimeUs;
                        Log.i(ERROR_TAGE,"firstPts = " + mFirstPts);
                    }

                    aacBuffInfo.presentationTimeUs -= mFirstPts;
                    Log.i(ERROR_TAGE,"audio presentationTimeUs = " + aacBuffInfo.presentationTimeUs);

                    EncodeFrame encodeFrame = new EncodeFrame(aacRawFrame, aacBuffInfo, null);
                    
                    if(aacBuffInfo.presentationTimeUs > mLastpts){
                    	encodeFrames.add(encodeFrame);
                    	mLastpts = aacBuffInfo.presentationTimeUs;
                    }
                    
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

//    private static  byte[] PacketToAdtsFrame(byte[] encodeByte, int sampleRate, int chanelCount)
//        {
//            final  int ADTS_HEAD_SIZE = 7;
//            final  int adtsPktSize = ADTS_HEAD_SIZE + encodeByte.length;
//            byte[] adtsPacket = new byte[adtsPktSize];
//            System.arraycopy(encodeByte, 0, adtsPacket, ADTS_HEAD_SIZE, encodeByte.length);
//            addADTStoPacket(adtsPacket,adtsPktSize, sampleRate, chanelCount);
//
//            return adtsPacket;
//        }

    @Override
    public MediaFormat getMediaFormat() {
        MediaFormat mediaFormat = (null != mediaCodec) ? mediaCodec.getOutputFormat() : null;
        return mediaFormat;
    }

//    /**
//     * @param packet
//     * @param packetLen
//     */
//    private static void addADTStoPacket(byte[] packet, int packetLen, int sampleRate, int chanelCount) {
//        int profile = AACCodecProfileLevel; // 2 - AAC LC
//        int freqIdx = indexOfArray(AAC_STANDARD_SAMPLE_RATE, sampleRate); // 4 - 44.1KHz
//        int chanCfg = chanelCount;
//
//
//// fill in ADTS data
//        packet[0] = (byte) 0xFF;
//        packet[1] = (byte) 0xF9;
//        packet[2] = (byte) (((profile - 1) << 6) + (freqIdx << 2) + (chanCfg >> 2));
//        packet[3] = (byte) (((chanCfg & 3) << 6) + (packetLen >> 11));
//        packet[4] = (byte) ((packetLen & 0x7FF) >> 3);
//        packet[5] = (byte) (((packetLen & 7) << 5) + 0x1F);
//        packet[6] = (byte) 0xFC;
//    }

    public static int indexOfArray(final int[] arry, int val)
    {
        int pos = -1;
        for(int i = 0;null != arry && i < arry.length; ++i)
        {
            if(arry[i] == val)
            {   pos = i;
                break;
            }
        }

        return pos;
    }
    //return null,means not limit, support any sample rate
    public static final int[] getSupportSampleRate(ENCODE_FORMAT encodeFormat)
    {
        int[] supportSampleRate = null;
        if(encodeFormat == ENCODE_FORMAT.AUDIO_AAC)
        {
            supportSampleRate = AAC_STANDARD_SAMPLE_RATE;
        }

        return supportSampleRate;
    }
}
