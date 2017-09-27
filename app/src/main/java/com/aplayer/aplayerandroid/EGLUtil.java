package com.aplayer.aplayerandroid;

import android.opengl.GLSurfaceView;
import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;

public class EGLUtil {
	
	public static class  MyEGLConfigChooser implements GLSurfaceView.EGLConfigChooser {
        @Override
        public  EGLConfig chooseConfig(EGL10 egl, EGLDisplay display){
            final int EGL_OPENGL_ES2_BIT = 0x0004;

            int[] attribList = {
                    EGL10.EGL_RED_SIZE, 8,
                    EGL10.EGL_GREEN_SIZE, 8,
                    EGL10.EGL_BLUE_SIZE, 8,
                    EGL10.EGL_ALPHA_SIZE, 8,
                    EGL10.EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT,
                    EGL10.EGL_SURFACE_TYPE, EGL10.EGL_PBUFFER_BIT,
                    EGL10.EGL_NONE
            };
            EGLConfig[] configs = new EGLConfig[1];
            int[] numConfigs = new int[1];
            if (!egl.eglChooseConfig(display, attribList, configs, configs.length,
                    numConfigs)) {
                throw new RuntimeException("unable to find RGB888+recordable ES2 EGL config");
            }

            return  configs[0];
        }
    }

	public static class  MyEGLContextFactory implements GLSurfaceView.EGLContextFactory {
        @Override
        public EGLContext createContext(EGL10 egl, EGLDisplay display, EGLConfig eglConfig){
            final int EGL_CONTEXT_CLIENT_VERSION = 0x3098;

            int[] attrib_list = {
                    EGL_CONTEXT_CLIENT_VERSION, 2,
                    EGL10.EGL_NONE
            };
            EGLContext mEGLContext = egl.eglCreateContext(display, eglConfig, EGL10.EGL_NO_CONTEXT,
                    attrib_list);
            //checkEglError("eglCreateContext");
            if (mEGLContext == null) {
                throw new RuntimeException("null context");
            }
            return mEGLContext;
        }

        @Override
        public void destroyContext(EGL10 egl, EGLDisplay display, EGLContext context){
            egl.eglDestroyContext(display, context);
        }
    }

}
