package io.ruin.model.shop;

import io.ruin.model.entity.player.Player;
import org.junit.Test;
import static org.junit.Assert.*;

public class ShopCurrencyTest {
    @org.junit.BeforeClass public static void cache() throws Exception { io.ruin.tools.ItemCacheFixture.load(); }
    @Test public void customHandlersDoNotRequireAnEnumAndMissingCurrencyFailsEarly() {
        Shop custom = new Shop();
        custom.currencyHandler = Currency.REFUNDED_CREDITS.getCurrencyHandler();
        custom.init();
        assertNull(custom.currency);
        assertNotNull(custom.shopItems);
        Player visitor = new Player() {
            private final io.ruin.network.PacketSender sender = new io.ruin.network.PacketSender(this, null) {
                @Override public void write(io.ruin.api.buffer.OutBuffer packet) {}
            };
            @Override public io.ruin.network.PacketSender getPacketSender() { return sender; }
            @Override public io.ruin.model.entity.player.GameMode getGameMode() { return io.ruin.model.entity.player.GameMode.STANDARD; }
            @Override public void openInterface(io.ruin.model.inter.InterfaceType type, int id) {}
            @Override public void sendMessage(String text) {}
        };
        custom.title = "Custom currency shop";
        custom.open(visitor);
        assertSame(custom, visitor.viewingShop);
        try { new Shop().init(); fail("Unconfigured shop must fail before interaction"); }
        catch (IllegalArgumentException expected) { assertTrue(expected.getMessage().contains("currency")); }
    }
    @Test public void refundedCreditsChargeTheirOwnBalance() {
        Player player = new Player();
        CurrencyHandler handler = Currency.REFUNDED_CREDITS.getCurrencyHandler();
        assertEquals(100, handler.addCurrency(player, 100));
        assertEquals(0, handler.removeCurrency(player, 101));
        assertEquals(40, handler.removeCurrency(player, 40));
        assertEquals(60, handler.getCurrencyCount(player));
        assertEquals(0, handler.removeCurrency(player, -1));
    }
}
