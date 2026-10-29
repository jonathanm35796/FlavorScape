package net.runelite.client.plugins.lletyamarket;

import net.runelite.standalone.LletyaMarketState;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class LletyaMarketStateTest {
    private static final int BASE=32765<<16;
    @Before public void clear(){LletyaMarketState.clear();}
    @Test public void onlyCompletedServerSnapshotsBecomeVisible(){
        assertTrue(LletyaMarketState.receive(BASE,"{\"token\":\"test\",\"category\":2,\"balance\":1000,\"query\":\"staff\"}"));
        LletyaMarketState.receive(BASE|1,"{\"id\":4675,\"name\":\"Ancient staff\",\"price\":1000,\"buyback\":400,\"category\":2}");
        assertNull(LletyaMarketState.get());
        LletyaMarketState.receive(BASE|2,"END");
        assertEquals(1000,LletyaMarketState.get().balance);
        assertEquals(1,LletyaMarketState.get().items.size());
        assertEquals(400,LletyaMarketState.get().items.get(0).buyback);
        LletyaMarketState.receive(BASE,"{\"token\":\"test\",\"category\":1,\"balance\":600}");
        assertEquals(2,LletyaMarketState.get().category);
        LletyaMarketState.receive(BASE|2,"END");
        assertEquals(1,LletyaMarketState.get().category);
        assertTrue(LletyaMarketState.get().items.isEmpty());
    }
    @Test public void malformedPacketsAndOtherWidgetsDoNotOpenTheMarket(){
        assertFalse(LletyaMarketState.receive(300<<16,"ordinary shop"));
        LletyaMarketState.receive(BASE,"invalid JSON");LletyaMarketState.receive(BASE|2,"END");
        assertNull(LletyaMarketState.get());
    }
    @Test public void closeClearsPendingAndVisibleData(){
        LletyaMarketState.receive(BASE,"{\"token\":\"test\"}");LletyaMarketState.receive(BASE|2,"END");
        LletyaMarketState.receive(BASE|3,"CLOSE");assertNull(LletyaMarketState.get());
    }
    @Test public void lateUpdatesCannotReopenLocallyDismissedShop(){
        LletyaMarketState.receive(BASE,"{\"token\":\"old\"}");LletyaMarketState.receive(BASE|2,"END");
        LletyaMarketState.receive(BASE,"{\"token\":\"old\",\"balance\":900}");
        LletyaMarketState.dismiss("old");
        LletyaMarketState.receive(BASE|2,"END");assertNull(LletyaMarketState.get());
        LletyaMarketState.receive(BASE,"{\"token\":\"old\"}");LletyaMarketState.receive(BASE|2,"END");
        assertNull(LletyaMarketState.get());
        LletyaMarketState.receive(BASE|3,"CLOSE");
        LletyaMarketState.receive(BASE,"{\"token\":\"new\"}");LletyaMarketState.receive(BASE|2,"END");
        assertEquals("new",LletyaMarketState.get().token);
        LletyaMarketState.dismiss("old");assertNotNull(LletyaMarketState.get());
    }
    @Test public void pricesUseTheReferenceStyle(){
        assertEquals("750K",LletyaMarketPlugin.price(750000));assertEquals("12.5M",LletyaMarketPlugin.price(12500000));
        assertEquals("8M",LletyaMarketPlugin.price(8000000));assertEquals("40",LletyaMarketPlugin.price(40));
        assertEquals("100K",LletyaMarketPlugin.price(100000));
        assertEquals("1M",LletyaMarketPlugin.price(1000000));
        assertEquals("3B",LletyaMarketPlugin.price(3000000000L));
    }
    @Test public void balanceSnapshotsSupportValuesAboveIntRange(){
        LletyaMarketState.receive(BASE,"{\"token\":\"test\",\"balance\":3000000000}");
        LletyaMarketState.receive(BASE|2,"END");
        assertEquals(3000000000L,LletyaMarketState.get().balance);
    }
    @Test public void catalogueSupportsFiveBillionPrices(){
        LletyaMarketState.receive(BASE,"{\"token\":\"expensive\"}");
        LletyaMarketState.receive(BASE|1,"{\"id\":11802,\"price\":5000000000,\"buyback\":2000000000}");
        LletyaMarketState.receive(BASE|2,"END");
        assertEquals(5_000_000_000L,LletyaMarketState.get().items.get(0).price);
    }
    @Test public void bountySnapshotsPreserveFractionalPointsAndCurrencyView(){
        LletyaMarketState.receive(BASE,"{\"token\":\"bh\",\"pvpOnly\":true,\"bountyBalance\":110.1953125}");
        LletyaMarketState.receive(BASE|2,"END");
        assertTrue(LletyaMarketState.get().pvpOnly);
        assertEquals(110.1953125,LletyaMarketState.get().bountyBalance,0);
    }
    @Test public void replacementSnapshotRemovesTheLegacyMarketChoice(){
        LletyaMarketState.receive(BASE,"{\"token\":\"new\",\"pvpOnly\":true,\"hasMarketSelector\":false}");
        LletyaMarketState.receive(BASE|2,"END");
        assertFalse(LletyaMarketState.get().hasMarketSelector);
        assertTrue(LletyaMarketState.get().pvpOnly);
    }
    @Test public void twoStoreSnapshotsRestoreTheSelectorAndUseTheSelectedCurrency(){
        LletyaMarketState.receive(BASE,"{\"token\":\"split\",\"pvpOnly\":false,\"hasMarketSelector\":true,\"balance\":8000000}");
        LletyaMarketState.receive(BASE|2,"END");
        assertTrue(LletyaMarketState.get().hasMarketSelector);assertFalse(LletyaMarketState.get().pvpOnly);
        assertEquals(8000000,LletyaMarketState.get().balance);
        LletyaMarketState.receive(BASE,"{\"token\":\"split\",\"pvpOnly\":true,\"hasMarketSelector\":true,\"bountyBalance\":110.5}");
        LletyaMarketState.receive(BASE|2,"END");
        assertTrue(LletyaMarketState.get().hasMarketSelector);assertTrue(LletyaMarketState.get().pvpOnly);
        assertEquals(110.5,LletyaMarketState.get().bountyBalance,0);
    }
}
