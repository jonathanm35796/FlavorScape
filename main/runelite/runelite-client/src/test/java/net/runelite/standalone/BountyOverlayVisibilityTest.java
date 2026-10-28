package net.runelite.standalone;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class BountyOverlayVisibilityTest {
    private Widget[][] original;
    private int originalStackSize;

    @Before public void setUp() {
        original = UserComparator5.Widget_interfaceComponents;
        originalStackSize = Interpreter.Interpreter_intStackSize;
        UserComparator5.Widget_interfaceComponents = new Widget[91][];
        UserComparator5.Widget_interfaceComponents[90] = new Widget[70];
        install(6);
        install(18);
    }

    @After public void tearDown() {
        CustomMisc.handleCustomVarp(20002, 1);
        UserComparator5.Widget_interfaceComponents = original;
        Interpreter.Interpreter_intStackSize = originalStackSize;
    }

    private Widget install(int child) {
        Widget widget = new Widget();
        widget.id = 90 << 16 | child;
        widget.cycle = -1;
        widget.isHidden = true;
        UserComparator5.Widget_interfaceComponents[90][child] = widget;
        return widget;
    }

    // Execute the same IF_SETHIDE opcode used by native Wilderness script 866.
    private void nativeHide(int child, boolean hidden) {
        Interpreter.Interpreter_intStackSize = 2;
        Interpreter.Interpreter_intStack[0] = hidden ? 1 : 0;
        Interpreter.Interpreter_intStack[1] = 90 << 16 | child;
        assertEquals(1, Login.method1567(2003, null, false));
        assertEquals(0, Interpreter.Interpreter_intStackSize);
    }

    @Test public void customVisibilityShowsAndHidesBothNativeContainers() {
        for (int value : new int[]{0, 1}) {
            CustomMisc.handleCustomVarp(20002, value);
            assertEquals(value == 1, Canvas.get(90, 6).isHidden);
            assertEquals(value == 1, Canvas.get(90, 18).isHidden);
        }
    }

    @Test public void wildernessScriptsCannotHideAnEnabledPanel() {
        CustomMisc.handleCustomVarp(20002, 0);
        for (int child : new int[]{6, 18}) {
            nativeHide(child, true);
            assertFalse(Canvas.get(90, child).isHidden);
        }
    }

    @Test public void wildernessScriptsCannotShowADisabledPanel() {
        CustomMisc.handleCustomVarp(20002, 1);
        for (int child : new int[]{6, 18}) {
            nativeHide(child, false);
            assertTrue(Canvas.get(90, child).isHidden);
        }
    }

    @Test public void interfaceReloadRetainsServerVisibility() {
        CustomMisc.handleCustomVarp(20002, 0);
        for (int child : new int[]{6, 18}) {
            Widget reloaded = install(child);
            CustomInterfaceEdits.childLoaded(reloaded);
            assertFalse(reloaded.isHidden);
        }
    }

    @Test public void wildernessLevelAndOtherWidgetsKeepNativeVisibility() {
        CustomMisc.handleCustomVarp(20002, 0);
        for (int child : new int[]{0, 19, 57, 61, 66}) {
            Widget widget = install(child);
            assertTrue(CustomMisc.bountyOverlayHidden(widget, true));
            assertFalse(CustomMisc.bountyOverlayHidden(widget, false));
        }
    }
}
