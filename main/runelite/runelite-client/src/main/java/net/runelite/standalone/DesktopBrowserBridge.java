package net.runelite.standalone;

import java.applet.Applet;

/** Browser-page callbacks are unavailable in the standalone desktop client. */
final class DesktopBrowserBridge {
    static DesktopBrowserBridge getWindow(Applet applet) {
        throw new UnsupportedOperationException("No browser page in the desktop client");
    }

    Object call(String name, Object[] arguments) {
        throw new UnsupportedOperationException("No browser JavaScript runtime");
    }

    Object eval(String script) {
        throw new UnsupportedOperationException("No browser JavaScript runtime");
    }
}