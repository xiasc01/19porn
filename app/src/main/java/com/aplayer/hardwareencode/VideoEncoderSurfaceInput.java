package com.aplayer.hardwareencode;

import android.annotation.SuppressLint;
import android.media.MediaCodec;
import android.opengl.GLES20;
import com.aplayer.aplayerandroid.Log;
import android.view.Surface;

import com.aplayer.aplayerandroid.GPUImageFilter;
import com.aplayer.aplayerandroid.InputSurface;
import com.aplayer.aplayerandroid.SurfaceRenderer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.List;

/**
 * Created by LZ on 2016/10/31.
 */

public class VideoEncoderSurfaceInput extends VideoEncoder{

    private Surface mEncoderInputSurface = null;
    private SurfaceRenderer mSurfaceRenderer = null;
    private static final String TAG = "APlayerAndroid";
    private InputSurface mRenderInputSurface = null;
    private GPUImageFilter       mFilter    = null;
    private FloatBuffer mVertexBuf;
    private FloatBuffer mTextureCoordinateBuf;

    private static final String ERROR_TAGE = "Aplayer_ERROR" + VideoEncoderSurfaceInput.class.getSimpleName();

    public VideoEncoderSurfaceInput(HardwareEncoder hardwareEncoder, ENCODE_FORMAT encodeFormat, int width, int height, double frameRate, int bitrate, int IFrameInterval) {
        super(hardwareEncoder,encodeFormat, COLOR_FORMAT.COLOR_FormatSurface, width, height, frameRate, bitrate, IFrameInterval);
    }

    @SuppressWarnings("finally")
    @Override
    public void run(){
        while (mRunning){
            List<EncodeFrame> encodeFrames = fetchEncodeData();
            mHardwareEncoder.putEncoderData(this,encodeFrames);

            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public boolean init()
    {
        boolean isInit = super.init();
        if(!isInit)
        {
            final int OLD_DEVICE_WIDTH_MAX  = 1280;                        //部分旧手机不支持高分辨率编码
            final int OLD_DEVICE_HEIGHT_MAX = 720;
            if(OLD_DEVICE_WIDTH_MAX < mWidth || OLD_DEVICE_HEIGHT_MAX < mHeight)
            {
                //scall video size, and  retry some codec not support to lage size
                isInit = sacllSizeRetryInit(OLD_DEVICE_WIDTH_MAX, OLD_DEVICE_HEIGHT_MAX);
            }
            else
            {
                Log.e(ERROR_TAGE, "mediaCodec.configure() failed!  width = " + mWidth + "height = " + mHeight);
            }
        }
        return isInit;
    }

    private boolean sacllSizeRetryInit(int limiteWidth, int limiteHeight )
    {
        float videoRatio = mWidth*1.0f / mHeight;
        float limiteRation = limiteWidth*1.0f / limiteHeight;

        if(mWidth > limiteWidth || mHeight > limiteHeight)
        {
            if(videoRatio > limiteRation)
            {
                mWidth  = limiteWidth;
                mHeight = (int)(limiteHeight / videoRatio);
            }
            else
            {
                mHeight = limiteHeight;
                mWidth  = (int)(limiteHeight * videoRatio);
            }

            boolean isInit = super.init();
            return isInit;
        }
        else
        {
            return false;
        }
    }

    @SuppressLint("NewApi")
	@SuppressWarnings("deprecation")
    @Override
    protected void beforeMediaCodecStart(MediaCodec mediaCodec) {
        if(null == mediaCodec){
            Log.e(TAG, "Please make sure, init() function is called successed!");
            return;
        }

        synchronized (this){
            mEncoderInputSurface = mediaCodec.createInputSurface();
        }
    }

    private long mCnt = 0;
    public boolean renderTexture(int texture,long presentationTime){
        if(mRenderInputSurface == null && mEncoderInputSurface != null){
            mRenderInputSurface = new InputSurface(mEncoderInputSurface);
        }

        if(mRenderInputSurface != null){
            mRenderInputSurface.makeCurrent();

            if(mFilter == null){
                mFilter = new GPUImageFilter(mRenderInputSurface);
                mFilter.init();
            }

            GLES20.glViewport(0, 0, mWidth, mHeight);
            GLES20.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
            GLES20.glEnable(GLES20.GL_DEPTH_TEST);
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

            if(mVertexBuf == null || mTextureCoordinateBuf == null){
                CreateFlatModel();
            }

            mRenderInputSurface.setPresentationTime(presentationTime * 1000000);

            mFilter.draw(texture, mVertexBuf, mTextureCoordinateBuf);
        }
        return true;
    }

    //input is surrface, not need feed data to encoder manual
    @Override
    public boolean feedRawData(byte[] input, long pts, long timeoutUs) {
        return true;
    }

    @Override
    public void release() {
        Log.i(TAG,"VideoEncoderSurfaceInput release");
        super.release();

        if(null != mSurfaceRenderer){
            mSurfaceRenderer.release();
            mSurfaceRenderer = null;
        }

        if(null  != mEncoderInputSurface){
            mEncoderInputSurface.release();
            mEncoderInputSurface = null;
        }
    }

    private void CreateFlatModel(){
        int vertexNum = 6;

        float[] vertexCoordinate  = {1.0f, -1.0f,  0.0f,
                1.0f,  1.0f,  0.0f,
                -1.0f,  1.0f,  0.0f,
                -1.0f,  1.0f,  0.0f,
                -1.0f, -1.0f,  0.0f,
                1.0f, -1.0f,  0.0f};

        ByteBuffer bbVertices = ByteBuffer.allocateDirect(vertexNum * 3 * 4);
        bbVertices.order(ByteOrder.nativeOrder());
        mVertexBuf = bbVertices.asFloatBuffer();
        mVertexBuf.put(vertexCoordinate);
        mVertexBuf.position(0);


        float[] textureCoordinate  = {1.0f, 0.0f,
                1.0f, 1.0f,
                0.0f, 1.0f,
                0.0f, 1.0f,
                0.0f, 0.0f,
                1.0f, 0.0f};


        ByteBuffer bbColors = ByteBuffer.allocateDirect(vertexNum * 2 * 4);
        bbColors.order(ByteOrder.nativeOrder());
        mTextureCoordinateBuf = bbColors.asFloatBuffer();
        mTextureCoordinateBuf.put(textureCoordinate);
        mTextureCoordinateBuf.position(0);
    }
}
