package net.runelite.standalone;

import net.runelite.api.widgets.WidgetItem;
import org.junit.Test;
import static org.junit.Assert.*;

public class WidgetItemBoundsTest {
    @Test public void expandedShopSlotsCanHaveNoAttributeEntry() {
        Widget widget = new Widget();
        widget.width = 8;
        widget.itemIds = new int[64];
        widget.itemQuantities = new int[64];
        widget.itemAttributes = new String[28][3];
        widget.itemIds[39] = 101;
        widget.itemQuantities[39] = 5;
        WidgetItem item = widget.getWidgetItem(39);
        assertEquals(100, item.getId());
        assertEquals(5, item.getQuantity());
        assertNull(item.getAttributes());
        widget.itemAttributes = null;
        assertNotNull(widget.getWidgetItem(39));
    }

    @Test public void staleMenuSlotsCannotIndexOutsideCurrentContainer() {
        Widget widget = new Widget();
        widget.width = 8;
        widget.itemIds = new int[40];
        widget.itemQuantities = new int[28];
        assertNull(widget.getWidgetItem(-1));
        assertNull(widget.getWidgetItem(28));
        assertNull(widget.getWidgetItem(40));
    }
}
