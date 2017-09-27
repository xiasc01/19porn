package com.aplayer.hardwareencode;

import android.media.MediaFormat;

import static com.aplayer.hardwareencode.AudioEncoder.AAC_STANDARD_SAMPLE_RATE;
import static com.aplayer.hardwareencode.AudioEncoder.indexOfArray;


/**
 * Created by LZ on 2017/4/27.
 */

//将AAC裸流，打包AAC文件（在每帧数据前添加ADTS头），只支持单路AAC打包成
public class AACAdtsPacket {

    public int getProfile(MediaFormat mediaFormat)
    {
        return mediaFormat.getInteger(MediaFormat.KEY_AAC_PROFILE);
    }

    public static  byte[] PacketToAdtsFrame(byte[] encodeByte, int sampleRate, int chanelCount, int profile)
    {
            final  int ADTS_HEAD_SIZE = 7;
            final  int adtsPktSize = ADTS_HEAD_SIZE + encodeByte.length;
            byte[] adtsPacket = new byte[adtsPktSize];
            System.arraycopy(encodeByte, 0, adtsPacket, ADTS_HEAD_SIZE, encodeByte.length);
            addADTStoPacket(adtsPacket,adtsPktSize, sampleRate, chanelCount, profile);

            return adtsPacket;
    }

    /**
     * 添加ADTS头,具体可参考ADTS头字段定义
     * @param packet
     * @param packetLen
     */
    private static void addADTStoPacket(byte[] packet, int packetLen, int sampleRate, int chanelCount, int profile) {
        int freqIdx = indexOfArray(AAC_STANDARD_SAMPLE_RATE, sampleRate); // 4 - 44.1KHz
        int chanCfg = chanelCount;


// fill in ADTS data
        packet[0] = (byte) 0xFF;
        packet[1] = (byte) 0xF9;
        packet[2] = (byte) (((profile - 1) << 6) + (freqIdx << 2) + (chanCfg >> 2));
        packet[3] = (byte) (((chanCfg & 3) << 6) + (packetLen >> 11));
        packet[4] = (byte) ((packetLen & 0x7FF) >> 3);
        packet[5] = (byte) (((packetLen & 7) << 5) + 0x1F);
        packet[6] = (byte) 0xFC;
    }
}
