package com.aplayer.hardwareencode.module;

import java.util.Comparator;

/**
 * Created by LZ on 2016/10/24.
 */

public class RawFrame {
    public byte[]                   rawData;
    public long                     pts;
    public int                      trackID;

    public RawFrame(byte[] rawData, long pts, int trackID) {
        this.rawData = rawData;
        this.pts = pts;
        this.trackID = trackID;
    }
}
