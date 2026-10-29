package net.runelite.client.plugins.lletyamarket;

import java.awt.Rectangle;

/** Geometry shared by the native-style scrollbar's painting and input. */
final class MarketScrollBar {
    static final int WIDTH = 16, ARROW = 16;
    final int maximum, scroll, top, trackHeight, travel;
    final Rectangle thumb;

    MarketScrollBar(int x, int y, int height, int contentHeight, int scroll) {
        maximum = Math.max(0, contentHeight - height);
        this.scroll = Math.max(0, Math.min(maximum, scroll));
        top = y + ARROW;
        trackHeight = height - ARROW * 2;
        int size = Math.min(trackHeight, Math.max(8, trackHeight * height / Math.max(height, contentHeight)));
        travel = trackHeight - size;
        thumb = new Rectangle(x, top + (maximum == 0 ? 0 : this.scroll * travel / maximum), WIDTH, size);
    }

    int dragTo(int pointerY, int grabOffset) {
        if (travel <= 0) return 0;
        int offset = Math.max(0, Math.min(travel, pointerY - grabOffset - top));
        return (int) Math.round((double) offset * maximum / travel);
    }
}
