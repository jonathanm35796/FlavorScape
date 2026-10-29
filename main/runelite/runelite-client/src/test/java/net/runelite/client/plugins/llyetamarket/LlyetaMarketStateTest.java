package net.runelite.client.plugins.llyetamarket;

import net.runelite.standalone.LlyetaMarketState;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class LlyetaMarketStateTest {
    private static final int BASE=32765<<16;
    @Before public void clear(){LlyetaMarketState.clear();}
    @Test public void onlyCompletedServerSnapshotsBecomeVisible(){
        assertTrue(LlyetaMarketState.receive(BASE,"{\"token\":\"test\",\"category\":2,\"balance\":1000,\"query\":\"staff\"}"));
        LlyetaMarketState.receive(BASE|1,"{\"id\":4675,\"name\":\"Ancient staff\",\"price\":1000,\"buyback\":400,\"category\":2}");
        assertNull(LlyetaMarketState.get());
        LlyetaMarketState.receive(BASE|2,"END");
        assertEquals(1000,LlyetaMarketState.get().balance);
        assertEquals(1,LlyetaMarketState.get().items.size());
        assertEquals(400,LlyetaMarketState.get().items.get(0).buyback);
        LlyetaMarketState.receive(BASE,"{\"token\":\"test\",\"category\":1,\"balance\":600}");
        assertEquals(2,LlyetaMarketState.get().category);
        LlyetaMarketState.receive(BASE|2,"END");
        assertEquals(1,LlyetaMarketState.get().category);
        assertTrue(LlyetaMarketState.get().items.isEmpty());
    }
    @Test public void malformedPacketsAndOtherWidgetsDoNotOpenTheMarket(){
        assertFalse(LlyetaMarketState.receive(300<<16,"ordinary shop"));
        LlyetaMarketState.receive(BASE,"invalid JSON");LlyetaMarketState.receive(BASE|2,"END");
        assertNull(LlyetaMarketState.get());
    }
    @Test public void closeClearsPendingAndVisibleData(){
        LlyetaMarketState.receive(BASE,"{\"token\":\"test\"}");LlyetaMarketState.receive(BASE|2,"END");
        LlyetaMarketState.receive(BASE|3,"CLOSE");assertNull(LlyetaMarketState.get());
    }
    @Test public void lateUpdatesCannotReopenLocallyDismissedShop(){
        LlyetaMarketState.receive(BASE,"{\"token\":\"old\"}");LlyetaMarketState.receive(BASE|2,"END");
        LlyetaMarketState.receive(BASE,"{\"token\":\"old\",\"balance\":900}");
        LlyetaMarketState.dismiss("old");
        LlyetaMarketState.receive(BASE|2,"END");assertNull(LlyetaMarketState.get());
        LlyetaMarketState.receive(BASE,"{\"token\":\"old\"}");LlyetaMarketState.receive(BASE|2,"END");
        assertNull(LlyetaMarketState.get());
        LlyetaMarketState.receive(BASE|3,"CLOSE");
        LlyetaMarketState.receive(BASE,"{\"token\":\"new\"}");LlyetaMarketState.receive(BASE|2,"END");
        assertEquals("new",LlyetaMarketState.get().token);
        LlyetaMarketState.dismiss("old");assertNotNull(LlyetaMarketState.get());
    }
    @Test public void pricesUseTheReferenceStyle(){
        assertEquals("750K",LlyetaMarketPlugin.price(750000));assertEquals("12.5M",LlyetaMarketPlugin.price(12500000));
        assertEquals("8M",LlyetaMarketPlugin.price(8000000));assertEquals("40",LlyetaMarketPlugin.price(40));
        assertEquals("100K",LlyetaMarketPlugin.price(100000));
        assertEquals("1M",LlyetaMarketPlugin.price(1000000));
        assertEquals("3B",LlyetaMarketPlugin.price(3000000000L));
    }
    @Test public void balanceSnapshotsSupportValuesAboveIntRange(){
        LlyetaMarketState.receive(BASE,"{\"token\":\"test\",\"balance\":3000000000}");
        LlyetaMarketState.receive(BASE|2,"END");
        assertEquals(3000000000L,LlyetaMarketState.get().balance);
    }
    @Test public void catalogueSupportsFiveBillionPrices(){
        LlyetaMarketState.receive(BASE,"{\"token\":\"expensive\"}");
        LlyetaMarketState.receive(BASE|1,"{\"id\":11802,\"price\":5000000000,\"buyback\":2000000000}");
        LlyetaMarketState.receive(BASE|2,"END");
        assertEquals(5_000_000_000L,LlyetaMarketState.get().items.get(0).price);
    }
    @Test public void bountySnapshotsPreserveFractionalPointsAndCurrencyView(){
        LlyetaMarketState.receive(BASE,"{\"token\":\"bh\",\"pvpOnly\":true,\"bountyBalance\":110.1953125}");
        LlyetaMarketState.receive(BASE|2,"END");
        assertTrue(LlyetaMarketState.get().pvpOnly);
        assertEquals(110.1953125,LlyetaMarketState.get().bountyBalance,0);
    }
    @Test public void replacementSnapshotRemovesTheLegacyMarketChoice(){
        LlyetaMarketState.receive(BASE,"{\"token\":\"new\",\"pvpOnly\":true,\"hasMarketSelector\":false}");
        LlyetaMarketState.receive(BASE|2,"END");
        assertFalse(LlyetaMarketState.get().hasMarketSelector);
        assertTrue(LlyetaMarketState.get().pvpOnly);
    }
    @Test public void twoStoreSnapshotsRestoreTheSelectorAndUseTheSelectedCurrency(){
        LlyetaMarketState.receive(BASE,"{\"token\":\"split\",\"pvpOnly\":false,\"hasMarketSelector\":true,\"balance\":8000000}");
        LlyetaMarketState.receive(BASE|2,"END");
        assertTrue(LlyetaMarketState.get().hasMarketSelector);assertFalse(LlyetaMarketState.get().pvpOnly);
        assertEquals(8000000,LlyetaMarketState.get().balance);
        LlyetaMarketState.receive(BASE,"{\"token\":\"split\",\"pvpOnly\":true,\"hasMarketSelector\":true,\"bountyBalance\":110.5}");
        LlyetaMarketState.receive(BASE|2,"END");
        assertTrue(LlyetaMarketState.get().hasMarketSelector);assertTrue(LlyetaMarketState.get().pvpOnly);
        assertEquals(110.5,LlyetaMarketState.get().bountyBalance,0);
    }
}
