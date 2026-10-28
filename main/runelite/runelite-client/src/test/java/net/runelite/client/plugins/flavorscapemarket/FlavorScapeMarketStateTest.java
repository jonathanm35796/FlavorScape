package net.runelite.client.plugins.flavorscapemarket;

import net.runelite.standalone.FlavorScapeMarketState;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class FlavorScapeMarketStateTest {
    private static final int BASE=32765<<16;
    @Before public void clear(){FlavorScapeMarketState.clear();}
    @Test public void onlyCompletedServerSnapshotsBecomeVisible(){
        assertTrue(FlavorScapeMarketState.receive(BASE,"{\"token\":\"test\",\"category\":2,\"balance\":1000,\"query\":\"staff\"}"));
        FlavorScapeMarketState.receive(BASE|1,"{\"id\":4675,\"name\":\"Ancient staff\",\"price\":1000,\"buyback\":400,\"category\":2}");
        assertNull(FlavorScapeMarketState.get());
        FlavorScapeMarketState.receive(BASE|2,"END");
        assertEquals(1000,FlavorScapeMarketState.get().balance);
        assertEquals(1,FlavorScapeMarketState.get().items.size());
        assertEquals(400,FlavorScapeMarketState.get().items.get(0).buyback);
        FlavorScapeMarketState.receive(BASE,"{\"token\":\"test\",\"category\":1,\"balance\":600}");
        assertEquals(2,FlavorScapeMarketState.get().category);
        FlavorScapeMarketState.receive(BASE|2,"END");
        assertEquals(1,FlavorScapeMarketState.get().category);
        assertTrue(FlavorScapeMarketState.get().items.isEmpty());
    }
    @Test public void malformedPacketsAndOtherWidgetsDoNotOpenTheMarket(){
        assertFalse(FlavorScapeMarketState.receive(300<<16,"ordinary shop"));
        FlavorScapeMarketState.receive(BASE,"invalid JSON");FlavorScapeMarketState.receive(BASE|2,"END");
        assertNull(FlavorScapeMarketState.get());
    }
    @Test public void closeClearsPendingAndVisibleData(){
        FlavorScapeMarketState.receive(BASE,"{\"token\":\"test\"}");FlavorScapeMarketState.receive(BASE|2,"END");
        FlavorScapeMarketState.receive(BASE|3,"CLOSE");assertNull(FlavorScapeMarketState.get());
    }
    @Test public void lateUpdatesCannotReopenLocallyDismissedShop(){
        FlavorScapeMarketState.receive(BASE,"{\"token\":\"old\"}");FlavorScapeMarketState.receive(BASE|2,"END");
        FlavorScapeMarketState.receive(BASE,"{\"token\":\"old\",\"balance\":900}");
        FlavorScapeMarketState.dismiss("old");
        FlavorScapeMarketState.receive(BASE|2,"END");assertNull(FlavorScapeMarketState.get());
        FlavorScapeMarketState.receive(BASE,"{\"token\":\"old\"}");FlavorScapeMarketState.receive(BASE|2,"END");
        assertNull(FlavorScapeMarketState.get());
        FlavorScapeMarketState.receive(BASE|3,"CLOSE");
        FlavorScapeMarketState.receive(BASE,"{\"token\":\"new\"}");FlavorScapeMarketState.receive(BASE|2,"END");
        assertEquals("new",FlavorScapeMarketState.get().token);
        FlavorScapeMarketState.dismiss("old");assertNotNull(FlavorScapeMarketState.get());
    }
    @Test public void pricesUseTheReferenceStyle(){
        assertEquals("750K",FlavorScapeMarketPlugin.price(750000));assertEquals("12.5M",FlavorScapeMarketPlugin.price(12500000));
        assertEquals("8M",FlavorScapeMarketPlugin.price(8000000));assertEquals("40",FlavorScapeMarketPlugin.price(40));
        assertEquals("100K",FlavorScapeMarketPlugin.price(100000));
        assertEquals("1M",FlavorScapeMarketPlugin.price(1000000));
        assertEquals("3B",FlavorScapeMarketPlugin.price(3000000000L));
    }
    @Test public void balanceSnapshotsSupportValuesAboveIntRange(){
        FlavorScapeMarketState.receive(BASE,"{\"token\":\"test\",\"balance\":3000000000}");
        FlavorScapeMarketState.receive(BASE|2,"END");
        assertEquals(3000000000L,FlavorScapeMarketState.get().balance);
    }
    @Test public void catalogueSupportsFiveBillionPrices(){
        FlavorScapeMarketState.receive(BASE,"{\"token\":\"expensive\"}");
        FlavorScapeMarketState.receive(BASE|1,"{\"id\":11802,\"price\":5000000000,\"buyback\":2000000000}");
        FlavorScapeMarketState.receive(BASE|2,"END");
        assertEquals(5_000_000_000L,FlavorScapeMarketState.get().items.get(0).price);
    }
    @Test public void bountySnapshotsPreserveFractionalPointsAndCurrencyView(){
        FlavorScapeMarketState.receive(BASE,"{\"token\":\"bh\",\"pvpOnly\":true,\"bountyBalance\":110.1953125}");
        FlavorScapeMarketState.receive(BASE|2,"END");
        assertTrue(FlavorScapeMarketState.get().pvpOnly);
        assertEquals(110.1953125,FlavorScapeMarketState.get().bountyBalance,0);
    }
    @Test public void replacementSnapshotRemovesTheLegacyMarketChoice(){
        FlavorScapeMarketState.receive(BASE,"{\"token\":\"new\",\"pvpOnly\":true,\"hasMarketSelector\":false}");
        FlavorScapeMarketState.receive(BASE|2,"END");
        assertFalse(FlavorScapeMarketState.get().hasMarketSelector);
        assertTrue(FlavorScapeMarketState.get().pvpOnly);
    }
    @Test public void twoStoreSnapshotsRestoreTheSelectorAndUseTheSelectedCurrency(){
        FlavorScapeMarketState.receive(BASE,"{\"token\":\"split\",\"pvpOnly\":false,\"hasMarketSelector\":true,\"balance\":8000000}");
        FlavorScapeMarketState.receive(BASE|2,"END");
        assertTrue(FlavorScapeMarketState.get().hasMarketSelector);assertFalse(FlavorScapeMarketState.get().pvpOnly);
        assertEquals(8000000,FlavorScapeMarketState.get().balance);
        FlavorScapeMarketState.receive(BASE,"{\"token\":\"split\",\"pvpOnly\":true,\"hasMarketSelector\":true,\"bountyBalance\":110.5}");
        FlavorScapeMarketState.receive(BASE|2,"END");
        assertTrue(FlavorScapeMarketState.get().hasMarketSelector);assertTrue(FlavorScapeMarketState.get().pvpOnly);
        assertEquals(110.5,FlavorScapeMarketState.get().bountyBalance,0);
    }
}
