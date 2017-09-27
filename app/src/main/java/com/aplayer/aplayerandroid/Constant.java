package com.aplayer.aplayerandroid;

/**
 * Created by admin on 2016/7/26.
 */
public class Constant {
    public static final String VERTEX = "precision mediump float;\n" +
            "attribute vec4 a_position;\n" +
            "uniform mat4 roat_x;\n" +
            "attribute vec2 a_tex_coord_in;\n" +
            "varying vec2 v_tex_coord_out;\n" +
            "void main() {\n" +
            "    gl_Position  = a_position;\n" +
            "    v_tex_coord_out = a_tex_coord_in;\n" +
            "}";

    public static final String FRAGMENT_RGB = "#extension GL_OES_EGL_image_external : require\n" +
            "precision mediump float;\n" +
            "varying vec2 v_tex_coord_out;\n" +
            "uniform samplerExternalOES sTexture;\n" +
            "void main() {\n" +
            "    gl_FragColor = texture2D(sTexture, v_tex_coord_out);\n" +
            "}";

    public static final String VERTEX2 = "precision mediump float;\n" +
            "attribute vec4 a_position;\n" +
            "attribute vec2 a_tex_coord_in;\n" +
            "varying vec2 v_tex_coord_out;\n" +
            "void main() {\n" +
            "    gl_Position  = a_position;\n" +
            "    v_tex_coord_out = a_tex_coord_in;\n" +
            "}";
}
