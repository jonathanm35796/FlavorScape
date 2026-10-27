package io.ruin.model.shop;

import io.ruin.cache.ItemID;
import io.ruin.model.combat.CombatCalculationTest;
import io.ruin.model.entity.player.PlayerCombat;
import io.ruin.model.item.Item;
import io.ruin.model.item.containers.Inventory;
import io.ruin.model.map.Position;
import java.util.List;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

public class PvPMarketTest {
    @BeforeClass public static void data() throws Exception {io.ruin.tools.ItemCacheFixture.load();}
    private static class Buyer extends CombatCalculationTest.Fighter {
        final Inventory inventory=new Inventory();
        final io.ruin.model.entity.player.PlayerMovement movement=new io.ruin.model.entity.player.PlayerMovement(this);
        final Position position=new Position(PvPMarket.X-2,PvPMarket.Y+2,0);
        final java.util.List<String> stockPackets=new java.util.ArrayList<>();
        final java.util.List<String> headerPackets=new java.util.ArrayList<>();
        final java.util.List<String> messages=new java.util.ArrayList<>();
        final java.util.List<String> interfaceEvents=new java.util.ArrayList<>();
        @Override public void sendMessage(String message){messages.add(message);}
        @Override public void integerInput(String message,java.util.function.Consumer<Integer> consumer){consumerInt=consumer;}
        final PlayerCombat marketCombat=new PlayerCombat(){@Override public boolean isDefending(long ticks){return false;}};
        final io.ruin.network.PacketSender sender=new io.ruin.network.PacketSender(this,null){
            @Override public void write(io.ruin.api.buffer.OutBuffer packet){}
            @Override public void sendString(int parent,int child,String text){if(parent==32765)interfaceEvents.add("market:"+child);if(parent==32765&&child==0)headerPackets.add(text);if(parent==32765&&child==1)stockPackets.add(text);}
        };
        Buyer(){inventory.init(this,28,-1,-1,-1,false);marketCombat.init(this);}
        @Override public Inventory getInventory(){return inventory;}
        @Override public io.ruin.model.entity.player.PlayerMovement getMovement(){return movement;}
        @Override public Position getPosition(){return position;}
        @Override public int getAbsX(){return position.getX();}
        @Override public int getAbsY(){return position.getY();}
        @Override public int getHeight(){return position.getZ();}
        @Override public PlayerCombat getCombat(){return marketCombat==null?super.getCombat():marketCombat;}
        @Override public io.ruin.network.PacketSender getPacketSender(){return sender;}
    }
    private static PvPMarket.MarketShop shop(Buyer p){PvPMarket.MarketShop s=new PvPMarket.MarketShop(List.of(new PvPMarket.Entry(11802,1000,0)));p.viewingShop=s;return s;}
    private static PvPMarket.MarketShop shop(Buyer p,int id,int price){PvPMarket.MarketShop s=new PvPMarket.MarketShop(List.of(new PvPMarket.Entry(id,price,0)));p.viewingShop=s;return s;}
    @Test public void openingAndReopeningPublishCatalogueBeforeNativeWindow(){
        Buyer p=new Buyer(){
            @Override public io.ruin.model.entity.player.GameMode getGameMode(){return io.ruin.model.entity.player.GameMode.STANDARD;}
            @Override public boolean isVisibleInterface(int id){return false;}
            @Override public void openInterface(io.ruin.model.inter.InterfaceType type,int id){interfaceEvents.add("open:"+id);}
            @Override public void closeInterface(io.ruin.model.inter.InterfaceType type){
                if(type==io.ruin.model.inter.InterfaceType.MAIN&&viewingShop!=null)viewingShop.close(this);
            }
        };
        for(int attempt=0;attempt<2;attempt++){
            p.interfaceEvents.clear();
            PvPMarket.MarketShop s=new PvPMarket.MarketShop(List.of(new PvPMarket.Entry(11802,1000,0)));
            s.open(p);
            assertSame(s,p.viewingShop);
            assertTrue(p.interfaceEvents.indexOf("market:2")<p.interfaceEvents.indexOf("open:300"));
            assertTrue(p.interfaceEvents.indexOf("market:2")<p.interfaceEvents.indexOf("open:301"));
            if(attempt==1)assertTrue("Previous CLOSE must precede the new catalogue",p.interfaceEvents.indexOf("market:3")<p.interfaceEvents.indexOf("market:0"));
        }
    }
    @Test public void buyThenSellReturnsExactlyFortyPercent(){
        Buyer p=new Buyer();PvPMarket.MarketShop s=shop(p);p.inventory.add(ItemID.BLOOD_MONEY,1000);
        s.buyFromShop(p,new Item(11802));assertEquals(1,p.inventory.count(11802));assertEquals(0,p.inventory.count(ItemID.BLOOD_MONEY));
        s.sellToShop(p,new Item(11802));assertEquals(0,p.inventory.count(11802));assertEquals(400,p.inventory.count(ItemID.BLOOD_MONEY));
    }
    @Test public void onlyCatalogueItemsAreBoughtBack(){
        Buyer p=new Buyer();PvPMarket.MarketShop s=shop(p);p.inventory.add(4151);
        s.sellToShop(p,new Item(4151));assertEquals(1,p.inventory.count(4151));assertEquals(0,p.inventory.count(ItemID.BLOOD_MONEY));
    }
    @Test public void notedItemsUseTheSameFixedBuybackPrice(){
        Buyer p=new Buyer();PvPMarket.MarketShop s=shop(p);int note=io.ruin.cache.ItemDef.get(11802).notedId;
        p.inventory.add(note,3);s.sellToShop(p,new Item(note,3));assertEquals(1200,p.inventory.count(ItemID.BLOOD_MONEY));assertEquals(0,p.inventory.count(note));
    }
    @Test public void purchasesCannotOverflowOrExceedInventoryCapacity(){
        Buyer p=new Buyer();PvPMarket.MarketShop s=shop(p);p.inventory.add(ItemID.BLOOD_MONEY,Integer.MAX_VALUE);
        s.buyFromShop(p,new Item(11802,Integer.MAX_VALUE));assertEquals(27,p.inventory.count(11802));assertEquals(Integer.MAX_VALUE-27000,p.inventory.count(ItemID.BLOOD_MONEY));
    }
    @Test public void platinumTokensPayAndReturnBloodMoneyChange(){
        Buyer p=new Buyer();PvPMarket.MarketShop s=shop(p,11802,1200);p.inventory.add(ItemID.PLATINUM_TOKEN,2);
        s.buyFromShop(p,new Item(11802));
        assertEquals(1,p.inventory.count(11802));assertEquals(0,p.inventory.count(ItemID.PLATINUM_TOKEN));assertEquals(800,p.inventory.count(ItemID.BLOOD_MONEY));
    }
    @Test public void bloodMoneyIsSpentBeforePlatinumTokens(){
        Buyer p=new Buyer();PvPMarket.MarketShop s=shop(p,11802,1200);p.inventory.add(ItemID.BLOOD_MONEY,700);p.inventory.add(ItemID.PLATINUM_TOKEN,1);
        s.buyFromShop(p,new Item(11802));
        assertEquals(1,p.inventory.count(11802));assertEquals(0,p.inventory.count(ItemID.PLATINUM_TOKEN));assertEquals(500,p.inventory.count(ItemID.BLOOD_MONEY));
    }
    @Test public void tokenBalanceSnapshotUsesLongValue(){
        Buyer p=new Buyer();PvPMarket.MarketShop s=shop(p);p.inventory.add(ItemID.BLOOD_MONEY,1_000_000);p.inventory.add(ItemID.PLATINUM_TOKEN,3_000_000);
        s.snapshot(p);
        com.google.gson.JsonObject header=new com.google.gson.JsonParser().parse(p.headerPackets.get(0)).getAsJsonObject();
        assertEquals(3_001_000_000L,header.get("balance").getAsLong());
    }
    @Test public void tokenPaymentCanFreeTheOnlySlotForStackablePurchase(){
        Buyer p=new Buyer();PvPMarket.MarketShop s=shop(p,560,1000);p.inventory.add(11802,27);p.inventory.add(ItemID.PLATINUM_TOKEN,1);
        assertEquals(0,p.inventory.getFreeSlots());
        s.buyFromShop(p,new Item(560));
        assertEquals(1,p.inventory.count(560));assertEquals(0,p.inventory.count(ItemID.PLATINUM_TOKEN));
    }
    @Test public void currencyStackAtItsLimitDoesNotDestroySoldItems(){
        Buyer p=new Buyer();PvPMarket.MarketShop s=shop(p);p.inventory.add(ItemID.BLOOD_MONEY,Integer.MAX_VALUE-10);p.inventory.add(11802);
        s.sellToShop(p,new Item(11802));assertEquals(1,p.inventory.count(11802));assertEquals(Integer.MAX_VALUE-10,p.inventory.count(ItemID.BLOOD_MONEY));
    }
    @Test public void staleOrRemoteRequestsCannotTrade(){
        Buyer p=new Buyer();PvPMarket.MarketShop s=shop(p);p.inventory.add(ItemID.BLOOD_MONEY,1000);
        PvPMarket.handle(p,new String[]{"buy","forged","11802","1"});assertEquals(0,p.inventory.count(11802));
        p.position.set(3200,3200,0);s.buyFromShop(p,new Item(11802));assertEquals(0,p.inventory.count(11802));
        p.position.set(PvPMarket.X,PvPMarket.Y,0);p.viewingShop=null;s.buyFromShop(p,new Item(11802));assertEquals(1000,p.inventory.count(ItemID.BLOOD_MONEY));
    }
    @Test public void fortyPercentUsesIntegerArithmetic(){assertEquals(3,PvPMarket.buybackPrice(9));assertEquals(858993458,PvPMarket.buybackPrice(Integer.MAX_VALUE));}

    @Test public void catalogueIncludesEveryCategoryAndSearchesAcrossSections(){
        Buyer p=new Buyer();PvPMarket.MarketShop s=new PvPMarket.MarketShop(List.of(
                new PvPMarket.Entry(4675,1000,2),new PvPMarket.Entry(11802,1000,0)));
        s.selected=2;s.snapshot(p);
        assertEquals(2,p.stockPackets.size());
        com.google.gson.JsonParser parser=new com.google.gson.JsonParser();
        assertEquals(0,parser.parse(p.stockPackets.get(0)).getAsJsonObject().get("category").getAsInt());
        assertEquals(2,parser.parse(p.stockPackets.get(1)).getAsJsonObject().get("category").getAsInt());
        p.stockPackets.clear();s.query="ancient";s.selected=0;s.snapshot(p);
        assertEquals(1,p.stockPackets.size());assertEquals(4675,parser.parse(p.stockPackets.get(0)).getAsJsonObject().get("id").getAsInt());
    }

    @Test public void usedCatalogueItemsStillReceiveTheFixedBuyback(){
        Buyer p=new Buyer();PvPMarket.MarketShop s=shop(p);
        p.inventory.add(11802,1,java.util.Map.of("charges","123"));
        s.sellToShop(p,new Item(11802));assertEquals(400,p.inventory.count(ItemID.BLOOD_MONEY));
        assertFalse(java.util.Arrays.stream(p.inventory.getItems()).anyMatch(i->i!=null&&i.getId()==11802));
    }
    @Test public void valueOnlyReportsTheServerPriceWithoutBuying(){
        Buyer p=new Buyer();PvPMarket.MarketShop s=shop(p);p.inventory.add(ItemID.BLOOD_MONEY,20000);
        PvPMarket.handle(p,new String[]{"value",s.token,"11802"});
        assertEquals(20000,p.inventory.count(ItemID.BLOOD_MONEY));assertEquals(0,p.inventory.count(11802));
        assertEquals(1,p.messages.size());assertTrue(p.messages.get(0).contains("1,000 Blood Money each"));
    }
    @Test public void buyXUsesTheEnteredAmountAndRechecksTheShopAtSubmission(){
        Buyer p=new Buyer();PvPMarket.MarketShop s=shop(p);p.inventory.add(ItemID.BLOOD_MONEY,20000);
        PvPMarket.handle(p,new String[]{"buyx",s.token,"11802"});assertNotNull(p.consumerInt);
        p.consumerInt.accept(7);assertEquals(7,p.inventory.count(11802));assertEquals(13000,p.inventory.count(ItemID.BLOOD_MONEY));
        PvPMarket.handle(p,new String[]{"buyx",s.token,"11802"});p.consumerInt.accept(0);p.consumerInt.accept(-1);
        assertEquals(7,p.inventory.count(11802));
        p.viewingShop=null;p.consumerInt.accept(3);assertEquals(7,p.inventory.count(11802));
        p.viewingShop=s;p.position.set(3200,3200,0);p.consumerInt.accept(3);assertEquals(7,p.inventory.count(11802));
    }
    @Test public void obsoleteBulkCommandsAndForgedBuyXRequestsCannotBuy(){
        Buyer p=new Buyer();PvPMarket.MarketShop s=shop(p);p.inventory.add(ItemID.BLOOD_MONEY,20000);
        PvPMarket.handle(p,new String[]{"buyx","forged","11802"});assertNull(p.consumerInt);
        PvPMarket.handle(p,new String[]{"buyx",s.token,"4151"});assertNull(p.consumerInt);
        PvPMarket.handle(p,new String[]{"buy",s.token,"11802","50"});
        PvPMarket.handle(p,new String[]{"buy",s.token,"11802",Integer.toString(Integer.MAX_VALUE)});
        assertEquals(0,p.inventory.count(11802));assertEquals(20000,p.inventory.count(ItemID.BLOOD_MONEY));
    }
}
