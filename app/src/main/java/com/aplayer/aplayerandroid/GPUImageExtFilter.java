package com.aplayer.aplayerandroid;

/**
 * Created by lzmlsfe on 2017/3/24.
 */

import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;

import java.nio.FloatBuffer;
import java.util.LinkedList;

public class GPUImageExtFilter{
    public static final String NO_FILTER_VERTEX_SHADER = ""                                             +
            "attribute vec4 position;                                                               \n" +
            "attribute vec4 inputTextureCoordinate;                                                 \n" +
            "varying vec2 textureCoordinate;                                                        \n" +
            "                                                                                       \n" +
            "void main()                                                                            \n" +
            "{                                                                                      \n" +
            "    gl_Position = position;                                                            \n" +
            "    textureCoordinate = inputTextureCoordinate.xy;                                     \n" +
            "}";


    public static final String NO_FILTER_FRAGMENT_SHADER = ""                                       +
            "#extension GL_OES_EGL_image_external : require                                     \n" +
            "varying highp vec2 textureCoordinate;                                              \n" +
            "uniform samplerExternalOES inputImageTexture1;                                      \n" +
            "void main()                                                                        \n" +
            "{                                                                                  \n" +
            "     gl_FragColor = texture2D(inputImageTexture1, textureCoordinate);               \n" +
            "}";


    private String TAG = GPUImageFilter.class.getSimpleName();
    private final LinkedList<Runnable> mRunOnDraw = null;
    private String mVertexShader;
    private String mFragmentShader;
    protected int mGLProgId;
    protected int mGLAttribPosition;
    protected int mGLUniformTexture1;
    protected int mGLAttribTextureCoordinate;
    protected int mTextureTarget;
    private boolean mIsInitialized;
    private SurfaceTexture mSurfaceTexture1 = null;

    private boolean  mFrameAvailable1 = false;
    private int[] mOESTextures;
    private InputSurface mInputSurface;
    private Object lock = new Object();

    public GPUImageExtFilter(InputSurface inputSurface) {
        this(NO_FILTER_VERTEX_SHADER, NO_FILTER_FRAGMENT_SHADER);
        mInputSurface = inputSurface;
    }

    public GPUImageExtFilter(final String vertexShader, final String fragmentShader) {
        mVertexShader = vertexShader;
        mFragmentShader = fragmentShader;
        mTextureTarget = GLES11Ext.GL_TEXTURE_EXTERNAL_OES;
    }

    public void Init(){
        OpenGlUtils.checkGlError("init start");

        mOESTextures = new int[1];
        GLES20.glGenTextures(1, mOESTextures, 0);

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, mOESTextures[0]);

        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER,
                GLES20.GL_NEAREST);
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER,
                GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S,
                GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T,
                GLES20.GL_CLAMP_TO_EDGE);


        mGLProgId = OpenGlUtils.loadProgram(mVertexShader, mFragmentShader);
        mGLAttribPosition = GLES20.glGetAttribLocation(mGLProgId,   "position");
        mGLUniformTexture1 = GLES20.glGetUniformLocation(mGLProgId, "inputImageTexture1");
        mGLAttribTextureCoordinate = GLES20.glGetAttribLocation(mGLProgId, "inputTextureCoordinate");
        mIsInitialized = true;

        OpenGlUtils.checkGlError("init end");
    }


    public SurfaceTexture getSurfaceTexture1(){
        if(mIsInitialized && mSurfaceTexture1 == null){
            mSurfaceTexture1 = new SurfaceTexture(mOESTextures[0]);
            mSurfaceTexture1.setOnFrameAvailableListener(new SurfaceTexture.OnFrameAvailableListener() {
                @Override
                public void onFrameAvailable(SurfaceTexture surfaceTexture) {
                    Log.i(TAG,"filter mFrameAvailable1 = true");
                    synchronized (lock){
                        mFrameAvailable1 = true;
                }
                }
            });
        }
        return  mSurfaceTexture1;
    }


    public boolean isFrameAvliable() {
    	synchronized (lock){
    		return mFrameAvailable1;
    	}
    }


    public boolean draw(final FloatBuffer cubeBuffer,
                       final FloatBuffer textureBuffer) {
        GLES20.glUseProgram(mGLProgId);

        if (!mIsInitialized) {
            return false;
        }
        if(mSurfaceTexture1 == null)
            return  false;

        synchronized (lock){
            if(mFrameAvailable1){
                try {
                    mSurfaceTexture1.updateTexImage();
                }catch (Exception ex){
                    ex.printStackTrace();
                }

                mFrameAvailable1 = false;
            }else{
                return  false;
            }
        }

        cubeBuffer.position(0);
        GLES20.glVertexAttribPointer(mGLAttribPosition, 3, GLES20.GL_FLOAT, false, 0, cubeBuffer);
        GLES20.glEnableVertexAttribArray(mGLAttribPosition);

        textureBuffer.position(0);
        GLES20.glVertexAttribPointer(mGLAttribTextureCoordinate, 2, GLES20.GL_FLOAT, false, 0, textureBuffer);
        GLES20.glEnableVertexAttribArray(mGLAttribTextureCoordinate);


        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(mTextureTarget, mOESTextures[0]);
        GLES20.glUniform1i(mGLUniformTexture1, 0);


        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 6);

        GLES20.glDisableVertexAttribArray(mGLAttribPosition);
        GLES20.glDisableVertexAttribArray(mGLAttribTextureCoordinate);
        GLES20.glBindTexture(mTextureTarget, 0);

        //mInputSurface.swapBuffers();
        GLES20.glFlush();
        return  true;
    }


    public void destroy() {
        mIsInitialized = false;
        GLES20.glDeleteProgram(mGLProgId);
    }
}
