/*
 * Copyright (C) 2012 CyberAgent
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.aplayer.aplayerandroid;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.PointF;
import android.graphics.SurfaceTexture;
import android.opengl.GLES11Ext;
import android.opengl.GLES20;
import android.util.Log;

import java.io.InputStream;
import java.nio.FloatBuffer;
import java.util.LinkedList;
import java.util.NoSuchElementException;



public class GPUImageFilter implements SurfaceTexture.OnFrameAvailableListener{
    private String TAG=GPUImageFilter.class.getSimpleName();
    public static final String NO_FILTER_VERTEX_SHADER = "" +
            "attribute vec4 position;                                                               \n" +
            "attribute vec4 inputTextureCoordinate;                                                 \n" +
            "varying vec2 textureCoordinate;                                                        \n" +
            " \n" +
            "void main()                                                                            \n" +
            "{                                                                                      \n" +
            "    gl_Position = position;                                                            \n" +
            "    textureCoordinate = inputTextureCoordinate.xy;                                     \n" +
            "}";

    public static final String NO_FILTER_FRAGMENT_SHADER = "" +
            "varying highp vec2 textureCoordinate;\n" +
            " \n" +
            "uniform sampler2D inputImageTexture;\n" +
            " \n" +
            "void main()\n" +
            "{\n" +
            "     gl_FragColor = texture2D(inputImageTexture, textureCoordinate);\n" +
            "}";

    public static final String NO_FILTER_FRAGMENT_SHADER2 = "" +
            "varying highp vec2 textureCoordinate;                                              \n" +
            "uniform sampler2D inputImageTexture1;                                              \n" +
            "uniform sampler2D inputImageTexture2;                                              \n" +
            "void main()                                                                        \n" +
            "{                                                                                  \n" +
            "     gl_FragColor = texture2D(inputImageTexture1, textureCoordinate)*0.5;          \n" +
            "     gl_FragColor+= texture2D(inputImageTexture2, textureCoordinate)*0.5;          \n" +
            "}";

    private final LinkedList<Runnable> mRunOnDraw;
    private String mVertexShader;
    private String mFragmentShader;
    protected int mGLProgId;
    protected int mGLAttribPosition;
    protected int mGLUniformTexture;
    protected int mGLUniformTexture1;
    protected int mGLUniformTexture2;
    protected int mGLAttribTextureCoordinate;
    protected int mOutputWidth;
    protected int mOutputHeight;
    protected int mTextureTarget;
    private boolean mIsInitialized;
    private int mSysRenderTexture = -1;
    private SurfaceTexture mSurfaceTexture;

    private Object mFrameSyncObject = new Object();
    private boolean  mFrameAvailable = true;

    private InputSurface mInputSurface;

    public GPUImageFilter(InputSurface inputSurface) {
        this(inputSurface,NO_FILTER_VERTEX_SHADER, NO_FILTER_FRAGMENT_SHADER);

    }

    public GPUImageFilter(InputSurface inputSurface,final String vertexShader, final String fragmentShader) {
        mRunOnDraw = new LinkedList<Runnable>();
        mInputSurface = inputSurface;
        mVertexShader = vertexShader;
        mFragmentShader = fragmentShader;
    }

    public final void init() {
        OpenGlUtils.checkGlError("init start");
        onInitTextureExt(false);
        onInit();
        onInitialized();
        OpenGlUtils.checkGlError("init end");
    }

    public final void initExt() {
        OpenGlUtils.checkGlError("initExt start");
        onInitTextureExt(true);
        onInit();
        onInitialized();
        OpenGlUtils.checkGlError("initExt end");
    }

    private void onInitTextureExt(boolean isForExternalTextureInput) {
        mTextureTarget = GLES20.GL_TEXTURE_2D;
        if (isForExternalTextureInput) {
            mTextureTarget = GLES11Ext.GL_TEXTURE_EXTERNAL_OES;
            mFragmentShader = "#extension GL_OES_EGL_image_external : require\n" +
                    mFragmentShader.replace("uniform sampler2D inputImageTexture;",
                            "uniform samplerExternalOES inputImageTexture;");


            int[] textures = new int[1];
            GLES20.glGenTextures(1, textures, 0);

            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textures[0]);

            GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER,
                    GLES20.GL_NEAREST);
            GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER,
                    GLES20.GL_LINEAR);
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S,
                    GLES20.GL_CLAMP_TO_EDGE);
            GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T,
                    GLES20.GL_CLAMP_TO_EDGE);

            mSysRenderTexture = textures[0];

        }
    }

    public void onInit() {
        mGLProgId = OpenGlUtils.loadProgram(mVertexShader, mFragmentShader);
        mGLAttribPosition = GLES20.glGetAttribLocation(mGLProgId, "position");
        mGLUniformTexture = GLES20.glGetUniformLocation(mGLProgId, "inputImageTexture");
        mGLAttribTextureCoordinate = GLES20.glGetAttribLocation(mGLProgId,
                "inputTextureCoordinate");
        mIsInitialized = true;
    }

    public void onInitialized() {
    }

    public void onFrameAvailable(SurfaceTexture st) {
        Log.i(TAG, "lzmlsf new frame available");
        synchronized (mFrameSyncObject) {
            mFrameAvailable = true;
        }
    }

    public SurfaceTexture GetSurfaceTexture(){
        if(mSurfaceTexture == null){
            Log.e(TAG, "GetSurface enter");

            while (mSysRenderTexture == -1){
                try {
                    Thread.sleep(10);
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
            mSurfaceTexture = new SurfaceTexture(mSysRenderTexture);
            mSurfaceTexture.setOnFrameAvailableListener(this);
        }

        return  mSurfaceTexture;
    }

    public final void destroy() {
        mIsInitialized = false;
        GLES20.glDeleteProgram(mGLProgId);
        onDestroy();
    }

    public void onDestroy() {
    }

    public void onOutputSizeChanged(final int width, final int height) {
        mOutputWidth = width;
        mOutputHeight = height;
    }

    public void draw(final FloatBuffer cubeBuffer,
                       final FloatBuffer textureBuffer){
        if(mSysRenderTexture != -1){
            if(mFrameAvailable){
                mSurfaceTexture.updateTexImage();
            }
            draw(mSysRenderTexture,cubeBuffer,textureBuffer);
        }
    }

    public void draw(final int textureId, final FloatBuffer cubeBuffer,
                       final FloatBuffer textureBuffer) {
        GLES20.glUseProgram(mGLProgId);
        runPendingOnDrawTasks();
        if (!mIsInitialized) {
            return;
        }
        cubeBuffer.position(0);
        GLES20.glVertexAttribPointer(mGLAttribPosition, 3, GLES20.GL_FLOAT, false, 0, cubeBuffer);
        GLES20.glEnableVertexAttribArray(mGLAttribPosition);

        textureBuffer.position(0);
        GLES20.glVertexAttribPointer(mGLAttribTextureCoordinate, 2, GLES20.GL_FLOAT, false, 0,
                textureBuffer);
        GLES20.glEnableVertexAttribArray(mGLAttribTextureCoordinate);

        if (textureId != OpenGlUtils.NO_TEXTURE) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(mTextureTarget, textureId);
            GLES20.glUniform1i(mGLUniformTexture, 0);
        }
        onDrawArraysPre();
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 6);
        GLES20.glDisableVertexAttribArray(mGLAttribPosition);
        GLES20.glDisableVertexAttribArray(mGLAttribTextureCoordinate);
        GLES20.glBindTexture(mTextureTarget, 0);

        Log.i(TAG,"java filter draw textureId = " + textureId);

        if(mInputSurface != null){
            mInputSurface.swapBuffers();
            GLES20.glFlush();
        }

    }

    public void draw(final int textureId1, final int textureId2,final FloatBuffer cubeBuffer,
                       final FloatBuffer textureBuffer) {
        GLES20.glUseProgram(mGLProgId);
        runPendingOnDrawTasks();
        if (!mIsInitialized) {
            return;
        }
        cubeBuffer.position(0);
        GLES20.glVertexAttribPointer(mGLAttribPosition, 3, GLES20.GL_FLOAT, false, 0, cubeBuffer);
        GLES20.glEnableVertexAttribArray(mGLAttribPosition);
        textureBuffer.position(0);
        GLES20.glVertexAttribPointer(mGLAttribTextureCoordinate, 2, GLES20.GL_FLOAT, false, 0,
                textureBuffer);
        GLES20.glEnableVertexAttribArray(mGLAttribTextureCoordinate);
        if (textureId1 != OpenGlUtils.NO_TEXTURE) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
            GLES20.glBindTexture(mTextureTarget, textureId1);
            GLES20.glUniform1i(mGLUniformTexture1, 0);
        }
        if (textureId2 != OpenGlUtils.NO_TEXTURE) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE1);
            GLES20.glBindTexture(mTextureTarget, textureId2);
            GLES20.glUniform1i(mGLUniformTexture2, 1);
        }
        onDrawArraysPre();
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 6);
        GLES20.glDisableVertexAttribArray(mGLAttribPosition);
        GLES20.glDisableVertexAttribArray(mGLAttribTextureCoordinate);
        GLES20.glBindTexture(mTextureTarget, 0);
    }

    protected void onDrawArraysPre() {}

    protected void runPendingOnDrawTasks() {
		try {
			while (!mRunOnDraw.isEmpty()) {
				mRunOnDraw.removeFirst().run();
			}
		} catch (NoSuchElementException e) {
			e.printStackTrace();
		}
    }

    public boolean isInitialized() {
        return mIsInitialized;
    }

    public int getOutputWidth() {
        return mOutputWidth;
    }

    public int getOutputHeight() {
        return mOutputHeight;
    }

    public int getProgram() {
        return mGLProgId;
    }

    public int getAttribPosition() {
        return mGLAttribPosition;
    }

    public int getAttribTextureCoordinate() {
        return mGLAttribTextureCoordinate;
    }

    public int getUniformTexture() {
        return mGLUniformTexture;
    }

    protected void setInteger(final int location, final int intValue) {
        runOnDraw(new Runnable() {
            @Override
            public void run() {
                GLES20.glUniform1i(location, intValue);
            }
        });
    }

    protected void setFloat(final int location, final float floatValue) {
        runOnDraw(new Runnable() {
            @Override
            public void run() {
                GLES20.glUniform1f(location, floatValue);
            }
        });
    }

    protected void setFloatVec2(final int location, final float[] arrayValue) {
        runOnDraw(new Runnable() {
            @Override
            public void run() {
                GLES20.glUniform2fv(location, 1, FloatBuffer.wrap(arrayValue));
            }
        });
    }

    protected void setFloatVec3(final int location, final float[] arrayValue) {
        runOnDraw(new Runnable() {
            @Override
            public void run() {
                GLES20.glUniform3fv(location, 1, FloatBuffer.wrap(arrayValue));
            }
        });
    }

    protected void setFloatVec4(final int location, final float[] arrayValue) {
        runOnDraw(new Runnable() {
            @Override
            public void run() {
                GLES20.glUniform4fv(location, 1, FloatBuffer.wrap(arrayValue));
            }
        });
    }

    protected void setFloatArray(final int location, final float[] arrayValue) {
        runOnDraw(new Runnable() {
            @Override
            public void run() {
                GLES20.glUniform1fv(location, arrayValue.length, FloatBuffer.wrap(arrayValue));
            }
        });
    }

    protected void setPoint(final int location, final PointF point) {
        runOnDraw(new Runnable() {

            @Override
            public void run() {
                float[] vec2 = new float[2];
                vec2[0] = point.x;
                vec2[1] = point.y;
                GLES20.glUniform2fv(location, 1, vec2, 0);
            }
        });
    }

    protected void setUniformMatrix3f(final int location, final float[] matrix) {
        runOnDraw(new Runnable() {

            @Override
            public void run() {
                GLES20.glUniformMatrix3fv(location, 1, false, matrix, 0);
            }
        });
    }

    protected void setUniformMatrix4f(final int location, final float[] matrix) {
        runOnDraw(new Runnable() {

            @Override
            public void run() {
                GLES20.glUniformMatrix4fv(location, 1, false, matrix, 0);
            }
        });
    }

    protected void runOnDraw(final Runnable runnable) {
        synchronized (mRunOnDraw) {
            mRunOnDraw.addLast(runnable);
        }
    }

    public static String loadShader(String file, Context context) {
        try {
            AssetManager assetManager = context.getAssets();
            InputStream ims = assetManager.open(file);

            String re = convertStreamToString(ims);
            ims.close();
            return re;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return "";
    }

    public static String convertStreamToString(InputStream is) {
        java.util.Scanner s = new java.util.Scanner(is).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }
}
