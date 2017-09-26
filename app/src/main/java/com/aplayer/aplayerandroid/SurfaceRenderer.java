package com.aplayer.aplayerandroid;
/**
 * Created by lzmlsfe on 2016/5/13.
 */
import android.graphics.SurfaceTexture;
import android.opengl.GLES20;
import android.view.Surface;
import android.view.SurfaceHolder;


import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;


public class SurfaceRenderer extends Thread implements SurfaceHolder.Callback{
    private static final String TAG = "APlayerAndroid";
    private FloatBuffer vertexBuf;
    private FloatBuffer textureCoordinateBuf1;
    private FloatBuffer textureCoordinateBuf2;
    private  int vertexNum   = 0;
    private Surface mViewSurface;
    private InputSurface mInputSurface = null;
    private int[] mFrameBuffer = null;
    private int[] mFrameBufferTexture = null;

    private GPUImageExtFilter    mExtFilter;
    private GPUImageFilter       mFilter;
    private Object lock = new Object();
    private APlayerAndroid mAPlayerAndroid;
    private boolean  isRunning = true;
    private boolean  surfaceChange = false;

    private int     mWidth  = 1920;
    private int     mHeight = 1280;
    private int     mVideoWidth  = 1920;
    private int     mVideoHeight = 1280;


    public SurfaceRenderer(APlayerAndroid aPlayerAndroid,Surface surface,int width,int height,int videoWidth,int videoHeight){
        mWidth = width;
        mHeight = height;

        mVideoWidth   = videoWidth;
        mVideoHeight  = videoHeight;

        mAPlayerAndroid = aPlayerAndroid;

        mAPlayerAndroid.setOnSurfaceChangeListener(new APlayerAndroid.OnSurfaceChangeListener() {
            @Override
            public void onSurfaceChange(int width, int height) {
                synchronized (lock){
                    mWidth  = width;
                    mHeight = height;
                    surfaceChange = true;
                }
            }
        });

        mViewSurface  = surface;

        start();
    }

    @Override
    public void run(){
        if(mViewSurface != null){
            try {
                mInputSurface = new InputSurface(mViewSurface);
            }catch (Exception e){
                return;
            }
        }

        try{
            mInputSurface.makeCurrent();
        }
        catch(Exception e){
            e.printStackTrace();
        }

        if (null == mFrameBuffer && null == mFrameBufferTexture) {
            mFrameBuffer = new int[2];
            mFrameBufferTexture = new int[2];
            createFrameBuffer(mWidth, mHeight, mFrameBuffer, mFrameBufferTexture, 2);
        }

        CreateFlatModel();

        mExtFilter = new GPUImageExtFilter(mInputSurface);
        mExtFilter.Init();

        mFilter = new GPUImageFilter(mInputSurface);
        mFilter.init();

        synchronized (lock){
            lock.notifyAll();
        }

        GLES20.glViewport(0, 0, mWidth, mHeight);


    	while(mAPlayerAndroid.getState() != APlayerAndroid.PlayerState.APLAYER_READ && isRunning){
            try {
                Thread.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }


            if(!mExtFilter.isFrameAvliable()){
            	continue;
            }
            
            GLES20.glEnable(GLES20.GL_DEPTH_TEST);
            GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

    		if(mInputSurface != null){
    			
    			/*if(mAPlayerAndroid.getEncodeCore() != null && mAPlayerAndroid.getEncodeCore().isEncoding()){
    				try{
    					mInputSurface.makeCurrent();
    				}
    				catch(Exception e){
    					e.printStackTrace();
    				}
    			}*/
                
                synchronized (lock){
                    if(surfaceChange){
                        onSurfaceChanged();
                        surfaceChange = false;
                    }

                }

                GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, mFrameBuffer[0]);
                boolean ret  = mExtFilter.draw(vertexBuf, textureCoordinateBuf1);
                if(!ret){
                    continue;
                }

                Log.i(TAG,"mFilter.onDraw");
                GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
                mFilter.draw(mFrameBufferTexture[0],vertexBuf, textureCoordinateBuf2);

            }


            if(mAPlayerAndroid.getEncodeCore() != null && mAPlayerAndroid.getEncodeCore().isEncoding()){
                long presentationTime = mAPlayerAndroid.getHardwareDecoder().getRealTimeUs();
                mAPlayerAndroid.getEncodeCore().putRawData(mFrameBufferTexture[0],presentationTime);
                try{
                    mInputSurface.makeCurrent();
                }
                catch(Exception e){
                    e.printStackTrace();
                }
            }

    	}

        interRelease();
    }


    public void createFrameBuffer(int width, int height, int[] frameBuffer, int[] frameBufferTexture, int frameBufferSize) {
        for (int i = 0; i < frameBufferSize; i++) {

            GLES20.glGenTextures(1, frameBufferTexture, i);
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, frameBufferTexture[i]);
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height, 0,
                    GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D,
                    GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);

            GLES20.glGenFramebuffers(1, frameBuffer, i);
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frameBuffer[i]);
            GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                    GLES20.GL_TEXTURE_2D, frameBufferTexture[i], 0);

            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0);
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0);
        }
    }

    private void destroyFrameBuffers() {
        if (mFrameBufferTexture != null) {
            GLES20.glDeleteTextures(mFrameBufferTexture.length, mFrameBufferTexture, 0);
            mFrameBufferTexture = null;
        }
        if (mFrameBuffer != null) {
            GLES20.glDeleteFramebuffers(mFrameBuffer.length, mFrameBuffer, 0);
            mFrameBuffer = null;
        }


    }

    private void onSurfaceChanged() {
        if(mFrameBufferTexture != null){
            destroyFrameBuffers();
        }

        if (null == mFrameBuffer && null == mFrameBufferTexture) {
            mFrameBuffer = new int[1];
            mFrameBufferTexture = new int[1];
            createFrameBuffer(mWidth, mHeight, mFrameBuffer, mFrameBufferTexture, 1);
        }

        GLES20.glViewport(0, 0, mWidth, mHeight);
    }

    private void interRelease(){
        destroyFrameBuffers();
        mInputSurface.release();
        mExtFilter.destroy();
        mFilter.destroy();
    }

    public void release(){
        isRunning = false;
    }

    public Surface GetSurface(){

        if(mExtFilter == null){
            synchronized (lock){
                try {
                    lock.wait();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        SurfaceTexture surfaceTexture = null;
        if(mExtFilter != null){
            surfaceTexture =  mExtFilter.getSurfaceTexture1();
        }

        if(surfaceTexture != null){
            return  new Surface(surfaceTexture);
        }

        return  null;
    }
      
    private void CreateFlatModel(){
    	vertexNum = 6;
    	
    	float[] vertexCoordinate  = {1.0f, -1.0f,  0.0f,
    			 					1.0f,  1.0f,  0.0f,
    			 				   -1.0f,  1.0f,  0.0f,
    			 				   -1.0f,  1.0f,  0.0f,
    			 				   -1.0f, -1.0f,  0.0f,
    			 				    1.0f, -1.0f,  0.0f};
    	
    	ByteBuffer bbVertices = ByteBuffer.allocateDirect(vertexNum * 3 * 4);
        bbVertices.order(ByteOrder.nativeOrder());
        vertexBuf = bbVertices.asFloatBuffer();
        vertexBuf.put(vertexCoordinate);
        vertexBuf.position(0);

        float[] textureCoordinate1  = {1.0f, 1.0f,
                1.0f, 0.0f,
                0.0f, 0.0f,
                0.0f, 0.0f,
                0.0f, 1.0f,
                1.0f, 1.0f};
        
        
        ByteBuffer bbColors1 = ByteBuffer.allocateDirect(vertexNum * 2 * 4);
        bbColors1.order(ByteOrder.nativeOrder());
        textureCoordinateBuf1 = bbColors1.asFloatBuffer();
        textureCoordinateBuf1.put(textureCoordinate1);
        textureCoordinateBuf1.position(0);

        float[] textureCoordinate2  = {1.0f, 0.0f,
                1.0f, 1.0f,
                0.0f, 1.0f,
                0.0f, 1.0f,
                0.0f, 0.0f,
                1.0f, 0.0f};


        ByteBuffer bbColors = ByteBuffer.allocateDirect(vertexNum * 2 * 4);
        bbColors.order(ByteOrder.nativeOrder());
        textureCoordinateBuf2 = bbColors.asFloatBuffer();
        textureCoordinateBuf2.put(textureCoordinate2);
        textureCoordinateBuf2.position(0);

    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.i(TAG,"surfaceRender width = " + width);
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

    }
}
;