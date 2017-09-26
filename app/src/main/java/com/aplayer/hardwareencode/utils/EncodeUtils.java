package com.aplayer.hardwareencode.utils;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.os.Build;
import android.util.Log;
import android.util.Range;

import java.io.IOException;

//import android.support.annotation.RequiresApi;

/**
 * Created by LZ on 2016/10/20.
 */

public class EncodeUtils {
    private static final String ERROR_TAGE = "AplayerAndroid_" + EncodeUtils.class.getSimpleName();
    private static final String TAGE = "AplayerAndroid_" + EncodeUtils.class.getSimpleName();
    private static final String TAG = "APlayerAndroid";

    public static MediaCodec.BufferInfo bufferInfoDup(final MediaCodec.BufferInfo bufferInfo) {
        MediaCodec.BufferInfo copy = new MediaCodec.BufferInfo();
        copy.set(bufferInfo.offset, bufferInfo.size, bufferInfo.presentationTimeUs, bufferInfo.flags);
        return copy;
    }

    public static void showCodecInfo() {
        int mediaCodecCount = MediaCodecList.getCodecCount();

        for (int i = 0; i < mediaCodecCount; i++) {
            MediaCodecInfo mediaCodecInfo = MediaCodecList.getCodecInfoAt(i);
            if (!mediaCodecInfo.isEncoder()) {
                continue;
            }

            Log.e(ERROR_TAGE, "CodecName:" + mediaCodecInfo.getName());
            String types[] = mediaCodecInfo.getSupportedTypes();

            String strTypes = "";
            for (int j = 0; j < types.length; j++) {
                if (!strTypes.isEmpty()) {
                    final String SPLITE = ",  ";
                    strTypes += SPLITE;
                }
                strTypes += types[j];
            }

            Log.e(ERROR_TAGE, "Suport Type:" + strTypes);
        }
    }

    private static String FindHardWareEncoder16(String mime) {
        int mediaCodecCount = MediaCodecList.getCodecCount();

        for (int i = 0; i < mediaCodecCount; i++) {
            MediaCodecInfo mediaCodecInfo = MediaCodecList.getCodecInfoAt(i);
            if (!mediaCodecInfo.isEncoder()) {
                continue;
            }

            String types[] = mediaCodecInfo.getSupportedTypes();
            for (int j = 0; j < types.length; j++) {
                if (types[j].equalsIgnoreCase(mime)) {
                    String decoderName = mediaCodecInfo.getName();
                    Log.e(ERROR_TAGE, "hwDecoderName = " + decoderName);
                    boolean isHardwareEncode = (decoderName.indexOf("google") == -1);
                    if (isHardwareEncode) {
                        return decoderName;
                    }
                }
            }
        }

        return null;
    }

    //@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private static String FindHardWareEncoder21(String mime) {

        MediaCodecList mediaCodecList = new MediaCodecList(MediaCodecList.ALL_CODECS);
        MediaCodecInfo[] mediaCodecInfos = mediaCodecList.getCodecInfos();

        for (int i = 0; i < mediaCodecInfos.length; i++) {
            MediaCodecInfo mediaCodecInfo = mediaCodecInfos[i];
            if (!mediaCodecInfo.isEncoder()) {
                continue;
            }

            String types[] = mediaCodecInfo.getSupportedTypes();
            for (int j = 0; j < types.length; j++) {
                if (types[j].equalsIgnoreCase(mime)) {
                    String decoderName = mediaCodecInfo.getName();
                    Log.e(TAG, "hwDecoderName = " + decoderName);
                    boolean isHardwareEncode = (decoderName.indexOf("google") == -1);
                    if (isHardwareEncode) {
                        return decoderName;
                    }
                }
            }
        }

        return null;
    }

    public static String findHardwareEncodeeName(String mime) {
        if (Build.VERSION.SDK_INT < 16) {
            return null;
        } else if (Build.VERSION.SDK_INT < 21) {
            return FindHardWareEncoder16(mime);
        } else {
            return FindHardWareEncoder21(mime);
        }
    }

    //1、寻找是否支持硬件编码
    //2、如果硬件编码器支持，返回硬件编码器，否则返回默认的编码器
    public static MediaCodec createMediaCodecEncoder(String mime) throws IOException {

        return MediaCodec.createEncoderByType(mime);
    	
    	
        /*String hardwareEncoderName = findHardwareEncodeeName(mime);
        if(null == hardwareEncoderName || hardwareEncoderName.equals("AACEncoder")){
            return MediaCodec.createEncoderByType(mime);
        }
        else{
        	Log.i(TAG,"hardwareEncoderName = " + hardwareEncoderName);
            return MediaCodec.createByCodecName(hardwareEncoderName);
        }*/
    }

    public static MediaCodecInfo getMediaCodecInfo(MediaCodec mediaCodec)
    {
        MediaCodecInfo retCodecInfo = null;
        if (Build.VERSION.SDK_INT < 18) {
            retCodecInfo = null;
        }else {
            retCodecInfo = (null != mediaCodec) ? mediaCodec.getCodecInfo() : null;
        }

        //retCodecInfo.getCapabilitiesForType(mime).getVideoCapabilities()
        return retCodecInfo;
    }

    public static EncodeVideoCapability getEncodVieoeCapability(MediaCodec mediaCodec, String mime)
    {
        if( mediaCodec == null || Build.VERSION.SDK_INT < 18) {
            return null;
        }

        EncodeVideoCapability retCapability = new EncodeVideoCapability();
        //String mime = mEncodeFormat.getValue();
        MediaCodecInfo codecInfo = mediaCodec.getCodecInfo();
        MediaCodecInfo.CodecCapabilities capabilities = codecInfo.getCapabilitiesForType(mime);
        int[] deviceColor = capabilities.colorFormats;
        retCapability.colorFormat = deviceColor;
        MediaCodecInfo.CodecProfileLevel[] profileLevels = capabilities.profileLevels;

        if(null != profileLevels)
        {
            retCapability.profileLevel = new EncodeVideoCapability.ProfileLevel[profileLevels.length];
            for(int i = 0; i < profileLevels.length; ++i)
            {
                retCapability.profileLevel[i] = new EncodeVideoCapability.ProfileLevel(profileLevels[i].profile, profileLevels[i].level);
            }
        }


        Range<Integer> widthRange = null;
        Range<Integer> heightRange = null;
        if(Build.VERSION.SDK_INT >= 21) {
                MediaCodecInfo.VideoCapabilities videoCapabilities = capabilities.getVideoCapabilities();
                heightRange = videoCapabilities.getSupportedHeights();
                widthRange = videoCapabilities.getSupportedWidths();

                retCapability.heightAlignment = videoCapabilities.getHeightAlignment();
                retCapability.widthAlignment  = videoCapabilities.getWidthAlignment();
        }
        else //for old device limite max width / height
        {
//            retCapability.widthUpper = 1280;
//            retCapability.widthLower = 176;
//            retCapability.heightUpper = 720;
//            retCapability.heightLower = 144;

            retCapability.heightAlignment = 2;
            retCapability.widthAlignment = 2;
        }

        if(null != widthRange)
        {
            retCapability.widthUpper = widthRange.getUpper();
            retCapability.widthLower = widthRange.getLower();
        }

        if(null != heightRange)
        {
            retCapability.heightUpper = heightRange.getUpper();
            retCapability.heightLower = heightRange.getLower();
        }

        return retCapability;
    }

    public static class EncodeVideoCapability
    {
        public static class ProfileLevel
        {
            public ProfileLevel(int profile, int level) {
                this.profile = profile;
                this.level = level;
            }

            int profile;
            int level;
        }

        public int widthUpper = 0;         //最大宽度范围上限制
        public int widthLower = 0;         //最小宽度范围下限制
        public int widthAlignment = 0;     //宽度对齐字节

        public int heightUpper = 0;
        public int heightLower = 0;
        public int heightAlignment = 0;

        public int[] colorFormat = null;                 //编码器支持的原始格式
        public ProfileLevel[] profileLevel = null;       //编码器支持的 编码Profile /等级
    }
}