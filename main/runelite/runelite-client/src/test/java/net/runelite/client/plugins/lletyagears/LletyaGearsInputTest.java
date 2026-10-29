package net.runelite.client.plugins.lletyagears;

import java.awt.Canvas;
import java.awt.Rectangle;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.lang.reflect.Field;
import net.runelite.client.input.MouseListener;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class LletyaGearsInputTest {
    private MouseListener input;
    private final Canvas canvas = new Canvas();

    @Before public void setup() throws Exception {
        LletyaGearsPlugin plugin = new LletyaGearsPlugin();
        Field frame = LletyaGearsPlugin.class.getDeclaredField("questFrame");
        frame.setAccessible(true);
        frame.set(plugin, new Rectangle(10, 10, 190, 261));
        Field listener = LletyaGearsPlugin.class.getDeclaredField("input");
        listener.setAccessible(true);
        input = (MouseListener) listener.get(plugin);
    }

    private MouseEvent event(int type, int button, int modifiers) {
        return new MouseEvent(canvas, type, System.currentTimeMillis(), modifiers, 40, 130, 1, false, button);
    }

    @Test public void releasesOverQuestTabReachNativeHeldButtonReset() {
        for (int button : new int[]{MouseEvent.BUTTON1, MouseEvent.BUTTON2, MouseEvent.BUTTON3}) {
            MouseEvent release = event(MouseEvent.MOUSE_RELEASED, button, 0);
            assertSame(release, input.mouseReleased(release));
            assertFalse("Release must clear the native held button", release.isConsumed());
        }
    }

    @Test public void cameraPressesOverQuestTabPassThrough() {
        for (MouseEvent press : new MouseEvent[]{
                event(MouseEvent.MOUSE_PRESSED, MouseEvent.BUTTON2, 0),
                event(MouseEvent.MOUSE_PRESSED, MouseEvent.BUTTON3, 0),
                event(MouseEvent.MOUSE_PRESSED, MouseEvent.BUTTON1, InputEvent.ALT_DOWN_MASK)}) {
            input.mousePressed(press);
            assertFalse("Camera gesture must not become a UI click", press.isConsumed());
        }
    }

    @Test public void regularInterfaceClickDoesNotReachTheGameWorld() {
        MouseEvent press = event(MouseEvent.MOUSE_PRESSED, MouseEvent.BUTTON1, 0);
        input.mousePressed(press);
        assertTrue(press.isConsumed());
        MouseEvent release = event(MouseEvent.MOUSE_RELEASED, MouseEvent.BUTTON1, 0);
        input.mouseReleased(release);
        assertFalse(release.isConsumed());
    }
}
