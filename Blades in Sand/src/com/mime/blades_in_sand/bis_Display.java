package com.mime.blades_in_sand;

import org.lwjgl.LWJGLException;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.newdawn.slick.Font;
import org.newdawn.slick.SlickException;
import org.newdawn.slick.UnicodeFont;
import org.newdawn.slick.font.effects.ColorEffect;

import utility.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.text.DecimalFormat;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.glDeleteProgram;
import static org.lwjgl.opengl.GL20.glUseProgram;

public class bis_Display {

    private static UnicodeFont font;
    private static final DecimalFormat formatter = new DecimalFormat("#.##");
    private static Model model;

    private static EulerCamera cam;
    private static int shaderProgram;
    private static int vboVertexHandle;
    private static int vboNormalHandle;

    private static final FloatBuffer perspectiveProjectionMatrix = BufferTools.reserveData(16);
    private static final FloatBuffer orthographicProjectionMatrix = BufferTools.reserveData(16);

    private static final String MODEL_LOCATION = "res/models/bunny.obj";
    private static final String VERTEX_SHADER_LOCATION = "res/shaders/vertex_phong_lighting.vs";
    private static final String FRAGMENT_SHADER_LOCATION = "res/shaders/vertex_phong_lighting.fs";
    private static final int DISPLAY_WIDTH = 640;
    private static final int DISPLAY_HEIGHT = 480;

    public static void main(String[] args) {
        setUpDisplay();
        setUpFonts();
        setUpVBOs();
        setUpCamera();
        setUpShaders();
        setUpLighting();
        while (!Display.isCloseRequested()) {
            render();
            checkInput();
            Display.update();
        }
        cleanUp();
        System.exit(0);
    }

    private static void render() {
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        glLoadIdentity();
        cam.applyTranslations();
        glUseProgram(shaderProgram);
        glLight(GL_LIGHT0, GL_POSITION, BufferTools.asFlippedFloatBuffer(cam.x(), cam.y(), cam.z(), 1));
        glBindBuffer(GL_ARRAY_BUFFER, vboVertexHandle);
        glVertexPointer(3, GL_FLOAT, 0, 0L);
        glBindBuffer(GL_ARRAY_BUFFER, vboNormalHandle);
        glNormalPointer(GL_FLOAT, 0, 0L);
        glEnableClientState(GL_VERTEX_ARRAY);
        glEnableClientState(GL_NORMAL_ARRAY);
        glColor3f(0.4f, 0.27f, 0.17f);
        glMaterialf(GL_FRONT, GL_SHININESS, 10f);
        glDrawArrays(GL_TRIANGLES, 0, model.getFaces().size() * 3);
        glDisableClientState(GL_VERTEX_ARRAY);
        glDisableClientState(GL_NORMAL_ARRAY);
        glUseProgram(0);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glMatrixMode(GL_PROJECTION);
        glLoadMatrix(orthographicProjectionMatrix);
        glMatrixMode(GL_MODELVIEW);
        glPushMatrix();
        glLoadIdentity();
        glDisable(GL_LIGHTING);
        font.drawString(10, 10, "Tri-Ordinates: x=" + formatter.format(cam.x()) +
                ", y=" + formatter.format(cam.y()) + ", z=" + formatter.format(cam.z()) + "");
        glEnable(GL_LIGHTING);
        glPopMatrix();
        glMatrixMode(GL_PROJECTION);
        glLoadMatrix(perspectiveProjectionMatrix);
        glMatrixMode(GL_MODELVIEW);
    }

    @SuppressWarnings("unchecked")
    private static void setUpFonts() {
        java.awt.Font awtFont = new java.awt.Font("res/font/Francois One.ttf", java.awt.Font.BOLD, 18);
        font = new UnicodeFont(awtFont);
        font.getEffects().add(new ColorEffect(java.awt.Color.white));
        font.addAsciiGlyphs();
        try {
            font.loadGlyphs();
        } catch (SlickException e) {
            e.printStackTrace();
            cleanUp();
        }
    }

    private static void setUpLighting() {
        glShadeModel(GL_SMOOTH);
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_LIGHTING);
        glEnable(GL_TEXTURE_2D);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glEnable(GL_LIGHT0);
        glLightModel(GL_LIGHT_MODEL_AMBIENT, BufferTools.asFlippedFloatBuffer(new float[]{0.05f, 0.05f, 0.05f, 1f}));
        glLight(GL_LIGHT0, GL_POSITION, BufferTools.asFlippedFloatBuffer(new float[]{0, 0, 0, 1}));
        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);
        glEnable(GL_COLOR_MATERIAL);
        glColorMaterial(GL_FRONT, GL_DIFFUSE);
    }

    private static void setUpVBOs() {
        vboVertexHandle = glGenBuffers();
        vboNormalHandle = glGenBuffers();
        model = null;
        try {
            model = OBJLoader.loadModel(new File(MODEL_LOCATION));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            cleanUp();
            System.exit(1);
        } catch (IOException e) {
            e.printStackTrace();
            cleanUp();
            System.exit(1);
        }
        FloatBuffer vertices = BufferTools.reserveData(model.getFaces().size() * 9);
        FloatBuffer normals = BufferTools.reserveData(model.getFaces().size() * 9);
        for (Model.Face face : model.getFaces()) {
            vertices.put(BufferTools.asFloats(model.getVertices().get(face.getVertexIndices()[0] - 1)));
            vertices.put(BufferTools.asFloats(model.getVertices().get(face.getVertexIndices()[1] - 1)));
            vertices.put(BufferTools.asFloats(model.getVertices().get(face.getVertexIndices()[2] - 1)));
            normals.put(BufferTools.asFloats(model.getNormals().get(face.getNormalIndices()[0] - 1)));
            normals.put(BufferTools.asFloats(model.getNormals().get(face.getNormalIndices()[1] - 1)));
            normals.put(BufferTools.asFloats(model.getNormals().get(face.getNormalIndices()[2] - 1)));
        }
        vertices.flip();
        normals.flip();
        glBindBuffer(GL_ARRAY_BUFFER, vboVertexHandle);
        glBufferData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW);
        glBindBuffer(GL_ARRAY_BUFFER, vboNormalHandle);
        glBufferData(GL_ARRAY_BUFFER, normals, GL_STATIC_DRAW);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
    }

    private static void setUpShaders() {
        shaderProgram = ShaderLoader.loadShaderPair(VERTEX_SHADER_LOCATION, FRAGMENT_SHADER_LOCATION);
    }

    private static void setUpCamera() {
        cam = new EulerCamera((float) Display.getWidth() / (float) Display.getHeight(), -2.19f, 1.36f, 11.45f);
        cam.setFieldOfView(70);
        cam.applyPerspectiveMatrix();
        glGetFloat(GL_PROJECTION_MATRIX, perspectiveProjectionMatrix);
        glMatrixMode(GL_PROJECTION);
        glLoadIdentity();
        glOrtho(0, Display.getWidth(), Display.getHeight(), 0, 1, -1);
        glGetFloat(GL_PROJECTION_MATRIX, orthographicProjectionMatrix);
        glLoadMatrix(perspectiveProjectionMatrix);
        glMatrixMode(GL_MODELVIEW);
    }

    private static void setUpDisplay() {
        try {
            Display.setDisplayMode(new DisplayMode(DISPLAY_WIDTH, DISPLAY_HEIGHT));
            Display.setVSyncEnabled(true);
            Display.setTitle("Blades In Sand");
            Display.create();
        } catch (LWJGLException e) {
            System.err.println("The display wasn't initialized correctly. :(");
            Display.destroy();
            System.exit(1);
        }
    }

    private static void checkInput() {
        cam.processMouse(1, 80, -80);
        cam.processKeyboard(16, 1, 1, 1);
        if (Mouse.isButtonDown(0)) {
            Mouse.setGrabbed(true);
        } else if (Mouse.isButtonDown(1)) {
            Mouse.setGrabbed(false);
        }
        if(Keyboard.isKeyDown(Keyboard.KEY_ESCAPE)){
        	Display.destroy();
        	System.exit(0);
        }
    }

    private static void cleanUp() {
        glDeleteProgram(shaderProgram);
        glDeleteBuffers(vboVertexHandle);
        glDeleteBuffers(vboNormalHandle);
        Display.destroy();
        System.exit(0);
    }
}