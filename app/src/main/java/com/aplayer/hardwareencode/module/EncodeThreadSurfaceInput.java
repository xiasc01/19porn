package com.aplayer.hardwareencode.module;

/**
 * Created by LZ on 2016/11/1.
 */

public class EncodeThreadSurfaceInput{

    /*private IEncoder mEncoder = null;
    private static final String ERROR_TAGE = "Aplayer_" + EncodeThreadSurfaceInput.class.getSimpleName();

    public EncodeThreadSurfaceInput(HardwareEncoder encodeCore, IEncoder encoder) {
        super(encodeCore);
        mEncoder = encoder;
        if(!(encoder instanceof VideoEncoderSurfaceInput)){
            Log.e(ERROR_TAGE, "Encoder is not VideoEncoderSurfaceInput class instance, EncodeThreadSurfaceInput only use for VideoEncoderSurfaceInput");
        }
    }

    @Override
    protected boolean fetchTaskAndProcess() {
        //if don't have data, this fun will block some millisecond
        mHardwareEncoder.encode(mEncoder, null);
        return true;
    }

    @Override
    public int deliveryTask(EncodeTask encodeTask) {
        Log.e(ERROR_TAGE, "surface input encode not support deliveryTask() function, input date from inputsurface automatic");
        return SUCCESS;
    }*/
}
