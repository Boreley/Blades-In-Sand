
package utility;

import org.lwjgl.LWJGLException;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.DisplayMode;
import org.lwjgl.opengl.GL11;

public class CameraTest {

    public static void main(String args[]) throws LWJGLException {
        Display.setVSyncEnabled(true);
        Display.setDisplayMode(new DisplayMode(640, 480));
        Display.setResizable(true);
        Display.create();
        EulerCamera camera = new EulerCamera.Builder().setAspectRatio(640f / 480f).setFieldOfView(60).build();
        camera.applyPerspectiveMatrix();
        camera.applyOptimalStates();
        Mouse.setGrabbed(true);
        while (!Display.isCloseRequested()) {
            GL11.glViewport(0, 0, Display.getWidth(), Display.getHeight());
            camera.setAspectRatio((float) Display.getWidth() / Display.getHeight());
            if (Display.wasResized()) {
                camera.applyPerspectiveMatrix();
            }
            while (Keyboard.next()) {
                if (Keyboard.isKeyDown(Keyboard.KEY_ESCAPE)) {
                    Mouse.setGrabbed(false);
                }
            }
            System.out.println(camera);
            GL11.glLoadIdentity();
            camera.applyTranslations();
            if (Mouse.isGrabbed()) {
                camera.processMouse();
                camera.processKeyboard(16);
            }
            GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
            GL11.glRectf(-1, -1, 1, 1);
            Display.sync(60);
            Display.update();
        }
        Display.destroy();
        System.exit(0);
    }
}
