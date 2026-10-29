package net.runelite.client.plugins.llyetamarket;

import java.util.Arrays;
import java.util.Collections;
import net.runelite.standalone.LlyetaMarketState;
import org.junit.Test;
import static org.junit.Assert.*;

public class MarketLayoutTest {
    private LlyetaMarketState.Item item(int category){LlyetaMarketState.Item item=new LlyetaMarketState.Item();item.category=category;return item;}
    @Test public void headersSeparateCategoriesAndCannotPurchaseItems(){
        MarketLayout layout=new MarketLayout(Arrays.asList(item(2),item(0),item(0)),false);
        assertEquals(2,layout.sections.size());assertEquals(0,layout.sections.get(0).category);
        assertEquals(-1,layout.itemAt(10,10));assertEquals(1,layout.itemAt(10,25));
        assertEquals(2,layout.itemAt(50,25));
        int magic=layout.headerOffset(2);
        assertEquals(-1,layout.itemAt(10,magic+10));assertEquals(0,layout.itemAt(10,magic+25));
        assertEquals(2,layout.categoryAt(magic));
        assertEquals(0,layout.maximumScroll(255));
    }
    @Test public void generalMarketRetainsAllNineJumpTargetsIncludingEmptySections(){
        MarketLayout layout=new MarketLayout(Collections.singletonList(item(0)),true);
        assertEquals(9,layout.sections.size());
        for(int category=0;category<9;category++)assertEquals(category,layout.categoryAt(layout.headerOffset(category)));
        assertEquals(layout.height-255,layout.maximumScroll(255));
        assertTrue(layout.headerOffset(8)<layout.maximumScroll(255)+255);
    }
    @Test public void filteredEmptyCatalogueHasNoClickableItems(){
        MarketLayout layout=new MarketLayout(Collections.emptyList(),false);
        assertEquals(0,layout.height);assertEquals(0,layout.maximumScroll(255));assertEquals(-1,layout.itemAt(10,25));
    }
}
