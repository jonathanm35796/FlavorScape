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
    private static PvPMarket.MarketShop shop(Buyer p,int id,int price){PvPMarket.MarketShop s=new PvPMarket.MarketShop(List.of(new PvPMarket.Entry(id,price,0,io.ruin.model.activities.wilderness.BountyHunterEquipment.isRestricted(id))));p.viewingShop=s;return s;}
    @Test public void mainShopOpensWithNewGeneralStockAndBloodMoneyPrices(){
        Buyer p=new Buyer();PvPMarket.MarketShop s=PvPMarket.createShop();p.viewingShop=s;
        assertFalse(s.pvpOnly);assertEquals(178,s.entries.size());assertEquals(17,s.bountyEntries.size());
        assertEquals(8000000L,s.activeEntries().get(11335).price);
        assertFalse(s.activeEntries().containsKey(22622));
        assertFalse(s.activeEntries().containsKey(22613));
        assertSame(Currency.BLOOD_MONEY,s.currency);assertTrue(s.canSellToStore);
        s.snapshot(p);assertEquals(178,p.stockPackets.size());
        com.google.gson.JsonObject header=new com.google.gson.JsonParser().parse(p.headerPackets.get(0)).getAsJsonObject();
        assertTrue(header.get("hasMarketSelector").getAsBoolean());
        for(ShopItem item:s.defaultStock)assertTrue(s.activeEntries().containsKey(item.getId()));
    }

    @Test public void pointStoreContainsOnlyTheFourPvpEquipmentFamilies(){
        Buyer p=new Buyer();PvPMarket.MarketShop s=PvPMarket.createShop();p.viewingShop=s;
        PvPMarket.handle(p,new String[]{"filter",s.token,"0","1","~"});
        assertTrue(s.pvpOnly);assertEquals(17,s.activeEntries().size());
        assertSame(Currency.BOUNTY_HUNTER_POINTS,s.currency);
        for(PvPMarket.Entry entry:s.activeEntries().values()){
            assertTrue(io.ruin.model.activities.wilderness.BountyHunterEquipment.isRestricted(entry.id));
            assertFalse(s.entries.containsKey(entry.id));assertEquals(0,entry.buyback);
        }
        for(int id:new int[]{22613,22622,22638,22647})assertTrue(s.activeEntries().containsKey(id));
        assertFalse(s.activeEntries().containsKey(11335));assertFalse(s.canSellToStore);
        PvPMarket.handle(p,new String[]{"filter",s.token,"0","0","~"});
        assertFalse(s.pvpOnly);assertEquals(178,s.activeEntries().size());
        assertSame(Currency.BLOOD_MONEY,s.currency);
    }

    @Test public void productionStoresRejectOtherStoresItemsAndUseTheirOwnCurrency(){
        Buyer p=new Buyer();PvPMarket.MarketShop s=PvPMarket.createShop();p.viewingShop=s;
        p.bountyHunterPointUnits=500L*512+1;p.inventory.add(ItemID.BLOOD_MONEY,8_000_000);
        s.buyFromShop(p,new Item(22613));assertEquals(0,p.inventory.count(22613));
        s.buyFromShop(p,new Item(11335));assertEquals(1,p.inventory.count(11335));
        assertEquals(0,p.inventory.count(ItemID.BLOOD_MONEY));assertEquals(500L*512+1,p.bountyHunterPointUnits);
        PvPMarket.handle(p,new String[]{"filter",s.token,"0","1","~"});
        s.buyFromShop(p,new Item(11335));assertEquals(1,p.inventory.count(11335));
        s.buyFromShop(p,new Item(22613));assertEquals(1,p.inventory.count(22613));
        assertEquals(1,p.bountyHunterPointUnits);assertEquals(0,p.inventory.count(ItemID.BLOOD_MONEY));
    }
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

    @Test public void freeItemsNeedNoCurrencyAndHaveNoBuyback(){
        Buyer p=new Buyer();PvPMarket.MarketShop s=shop(p,4587,0);
        s.buyFromShop(p,new Item(4587,5));assertEquals(5,p.inventory.count(4587));
        assertEquals(0,s.getSellPrice(s.defaultStock.get(0)));assertEquals(-1,s.getBuyPrice(new Item(4587)));
        s.sellToShop(p,new Item(4587));assertEquals(5,p.inventory.count(4587));assertEquals(0,p.inventory.count(ItemID.BLOOD_MONEY));
    }

    @Test public void fiveBillionPriceUsesTokensWithoutIntegerOverflow(){
        Buyer p=new Buyer();PvPMarket.MarketShop s=new PvPMarket.MarketShop(List.of(new PvPMarket.Entry(11802,5_000_000_000L,6)));
        p.viewingShop=s;p.inventory.add(ItemID.PLATINUM_TOKEN,5_000_000);
        s.buyFromShop(p,new Item(11802));assertEquals(1,p.inventory.count(11802));assertEquals(0,p.inventory.count(ItemID.PLATINUM_TOKEN));
        s.sellToShop(p,new Item(11802));assertEquals(0,p.inventory.count(11802));assertEquals(2_000_000_000,p.inventory.count(ItemID.BLOOD_MONEY));
        assertTrue(s.defaultStock.isEmpty());
    }

    @Test public void bountyEquipmentRequiresPointsAndTheEquipmentView(){
        Buyer p=new Buyer();PvPMarket.MarketShop s=shop(p,22613,250);
        p.inventory.add(ItemID.BLOOD_MONEY,1000000);p.inventory.add(ItemID.PLATINUM_TOKEN,1000);
        s.buyFromShop(p,new Item(22613));assertEquals(0,p.inventory.count(22613));
        s.pvpOnly=true;s.refreshStock();
        s.buyFromShop(p,new Item(22613));assertEquals(0,p.inventory.count(22613));
        p.bountyHunterPointUnits=250L*512+100;
        s.buyFromShop(p,new Item(22613));
        assertEquals(1,p.inventory.count(22613));assertEquals(100,p.bountyHunterPointUnits);
        assertEquals(1000000,p.inventory.count(ItemID.BLOOD_MONEY));assertEquals(1000,p.inventory.count(ItemID.PLATINUM_TOKEN));
        s.sellToShop(p,new Item(22613));assertEquals(1,p.inventory.count(22613));assertEquals(100,p.bountyHunterPointUnits);
        s.pvpOnly=false;s.sellToShop(p,new Item(22613));assertEquals(1,p.inventory.count(22613));
    }

    @Test public void bountyPurchasesRespectCapacityAndRejectGeneralItems(){
        Buyer p=new Buyer();PvPMarket.MarketShop s=shop(p,22613,250);s.pvpOnly=true;
        p.bountyHunterPointUnits=10000L*512;p.inventory.add(11802,27);
        s.buyFromShop(p,new Item(22613,Integer.MAX_VALUE));
        assertEquals(1,p.inventory.count(22613));assertEquals(9750L*512,p.bountyHunterPointUnits);
        s.buyFromShop(p,new Item(22613));assertEquals(9750L*512,p.bountyHunterPointUnits);
        Buyer other=new Buyer();PvPMarket.MarketShop general=shop(other);general.pvpOnly=true;
        other.inventory.add(ItemID.BLOOD_MONEY,10000);general.buyFromShop(other,new Item(11802));
        assertEquals(0,other.inventory.count(11802));assertEquals(10000,other.inventory.count(ItemID.BLOOD_MONEY));
    }

    @Test public void bountyViewPublishesPointPricesAndExactBalance(){
        Buyer p=new Buyer();PvPMarket.MarketShop s=new PvPMarket.MarketShop(List.of(
                new PvPMarket.Entry(22613,250,0,true),new PvPMarket.Entry(11802,1000,0)));
        p.viewingShop=s;s.pvpOnly=true;s.refreshStock();p.bountyHunterPointUnits=513;s.snapshot(p);
        com.google.gson.JsonParser parser=new com.google.gson.JsonParser();
        assertEquals(1,p.stockPackets.size());assertEquals(250,parser.parse(p.stockPackets.get(0)).getAsJsonObject().get("price").getAsInt());
        assertEquals(1.001953125,parser.parse(p.headerPackets.get(0)).getAsJsonObject().get("bountyBalance").getAsDouble(),0);
        assertSame(Currency.BOUNTY_HUNTER_POINTS,s.currency);assertEquals(1,s.defaultStock.size());
    }

    @Test public void explicitCataloguesUseRequestedPricesAndVerifiedIds(){
        java.util.Map<Integer,PvPMarket.Entry> entries=new java.util.HashMap<>();
        for(PvPMarket.Entry e:PvPEquipmentCatalogue.loadGeneralMarket()){
            assertNull("Duplicate item "+e.id,entries.put(e.id,e));
            assertFalse(e.bounty);assertEquals(PvPMarket.buybackPrice(e.price),e.buyback);
            assertFalse(io.ruin.model.activities.wilderness.BountyHunterEquipment.isRestricted(e.id));
            assertFalse(io.ruin.cache.ItemDef.get(e.id).isNote());assertFalse(io.ruin.cache.ItemDef.get(e.id).isPlaceholder());
        }
        assertTrue(entries.size()>100);
        assertEquals(8000000L,entries.get(11335).price);assertEquals(25000000L,entries.get(21892).price);
        assertFalse(entries.containsKey(22613));assertEquals(0L,entries.get(4587).price);
        List<PvPMarket.Entry> bounty=PvPEquipmentCatalogue.load();assertEquals(17,bounty.size());
        for(PvPMarket.Entry e:bounty){assertTrue(e.bounty);assertEquals(0,e.buyback);assertTrue(io.ruin.model.activities.wilderness.BountyHunterEquipment.isRestricted(e.id));}
        java.util.Map<Integer,Long> pointPrices=java.util.Map.of(
                22634,20L,22636,25L,22638,250L,22641,400L,22644,400L,
                22647,400L,22650,250L,22653,500L,22656,500L);
        for(PvPMarket.Entry e:bounty)
            assertEquals("BH point price for "+e.name,e.category==0?500L:pointPrices.get(e.id).longValue(),e.price);
        assertEquals(6000000L,entries.get(4716).price);assertEquals(2000000L,entries.get(4734).price);
        assertFalse(entries.containsKey(22622));
        assertFalse("Quest chainbody must not be sold as armour",entries.containsKey(2513));
        assertFalse("Quest book must not be sold as equipment",entries.containsKey(7633));
        for(PvPMarket.Entry e:entries.values())
            if((e.category<=2||e.category==5||e.category==6)&&e.id!=52)
                assertTrue("Missing coded equipment slot for "+e.id+" "+e.name,io.ruin.cache.ItemDef.get(e.id).equipSlot>=0);
    }

    @Test public void sameItemCanHaveIndependentPricesAndCurrenciesInEachStore(){
        Buyer p=new Buyer();PvPMarket.MarketShop s=new PvPMarket.MarketShop(
                List.of(new PvPMarket.Entry(11802,1000,0)),List.of(new PvPMarket.Entry(11802,90_000_000,0,true)));
        p.viewingShop=s;p.inventory.add(ItemID.BLOOD_MONEY,1000);
        s.buyFromShop(p,new Item(11802));assertEquals(1,p.inventory.count(11802));
        s.pvpOnly=true;s.refreshStock();p.inventory.add(ItemID.BLOOD_MONEY,1000000);p.inventory.add(ItemID.PLATINUM_TOKEN,1000000);
        s.buyFromShop(p,new Item(11802));assertEquals(1,p.inventory.count(11802));
        p.bountyHunterPointUnits=90_000_000L*512+1;s.buyFromShop(p,new Item(11802));
        assertEquals(2,p.inventory.count(11802));assertEquals(1,p.bountyHunterPointUnits);
        assertEquals(1000000,p.inventory.count(ItemID.BLOOD_MONEY));assertEquals(1000000,p.inventory.count(ItemID.PLATINUM_TOKEN));
        s.sellToShop(p,new Item(11802));assertEquals(2,p.inventory.count(11802));
        s.pvpOnly=false;s.refreshStock();assertEquals(1000,s.getSellPrice(s.defaultStock.get(0)));
    }

    @Test public void bountyShopSupportsFreeItemsAndFiveBillionPointPurchases(){
        Buyer p=new Buyer();PvPMarket.MarketShop s=new PvPMarket.MarketShop(List.of(),List.of(
                new PvPMarket.Entry(4587,0,0,true),new PvPMarket.Entry(11802,5_000_000_000L,6,true)));
        p.viewingShop=s;s.pvpOnly=true;s.refreshStock();s.buyFromShop(p,new Item(4587));
        assertEquals(1,p.inventory.count(4587));assertEquals(0,p.bountyHunterPointUnits);
        p.bountyHunterPointUnits=5_000_000_000L*512+1;s.buyFromShop(p,new Item(11802));
        assertEquals(1,p.inventory.count(11802));assertEquals(1,p.bountyHunterPointUnits);
    }

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
