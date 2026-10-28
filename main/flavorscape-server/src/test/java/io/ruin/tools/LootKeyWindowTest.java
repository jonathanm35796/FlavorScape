package io.ruin.tools;

import io.ruin.model.activities.wilderness.LootKeys;
import io.ruin.model.activities.wilderness.LootKeyWindow;
import io.ruin.model.inter.InterfaceType;
import io.ruin.model.item.Item;
import io.ruin.model.item.containers.bank.Bank;
import org.junit.*;
import java.util.*;
import static org.junit.Assert.*;

public class LootKeyWindowTest {
    static class Hunter extends LootKeysTest.Hunter {
        final Bank bank=new Bank();
        boolean visible;
        Hunter(){bank.init(this,800,-1,-1,-1,true);}
        @Override public Bank getBank(){return bank;}
        @Override public void openInterface(InterfaceType type,int id){if(type==InterfaceType.MAIN)visible=id==LootKeyWindow.HOST;}
        @Override public void closeInterface(InterfaceType type){if(type==InterfaceType.MAIN){visible=false;LootKeyWindow.closed(this);}}
        @Override public boolean isVisibleInterface(int id){return visible&&id==LootKeyWindow.HOST;}
    }
    @BeforeClass public static void data()throws Exception{BountyHunterAudit.prepare();}
    private static Item key(Hunter p,Item... items){int slot=p.inventory.freeSlot();p.inventory.add(LootKeys.create(Arrays.asList(items)));return p.inventory.get(slot);}
    private static String[] request(Hunter p,String action){return new String[]{p.lootKeySession.token,Long.toString(p.lootKeySession.revision),action};}
    private static void action(Hunter p,String action){LootKeyWindow.handle(p,request(p,action));}
    private static com.google.gson.JsonObject snapshot(Hunter p){return new com.google.gson.JsonParser().parse(p.strings.get("32764:0")).getAsJsonObject();}
    private static Runnable confirmation(Hunter p)throws Exception{var field=io.ruin.model.inter.dialogue.YesNoDialogue.class.getDeclaredField("yesAction");field.setAccessible(true);return (Runnable)field.get(p.dialogues[0]);}

    @Test public void usingAKeyOpensTheWindowWithItsActualContentsAndAllKeyTabs(){
        Hunter p=new Hunter();Item first=key(p,new Item(11802,2));key(p,new Item(4151));LootKeyWindow.open(p,first);
        var data=snapshot(p);assertEquals(2,data.getAsJsonArray("keys").size());assertEquals(1,data.getAsJsonArray("items").size());
        assertEquals(2,data.getAsJsonArray("items").get(0).getAsJsonObject().get("amount").getAsInt());
        assertEquals(LootKeys.read(first).token,data.get("selected").getAsString());assertEquals(800,data.get("bankCapacity").getAsInt());assertTrue(p.visible);
    }
    @Test public void inventoryButtonClaimsContentsAndClosesTheWindowWhenTheKeyIsEmpty(){
        Hunter p=new Hunter();Item key=key(p,new Item(11802));long value=LootKeys.value(key);LootKeyWindow.open(p,key);
        String[] click=request(p,"inventory");LootKeyWindow.handle(p,click);LootKeyWindow.handle(p,click);
        assertEquals(1,p.inventory.count(11802));assertEquals(1,p.lootKeysClaimed);assertEquals(value,p.lootKeyValueClaimed);assertNull(p.lootKeySession);assertFalse(p.visible);
    }
    @Test public void bankButtonDepositsOriginalAmountsAndAttributesDirectly(){
        Hunter p=new Hunter();Item key=key(p,new Item(11802,1,Map.of("charges","123")),new Item(13307,4000));LootKeyWindow.open(p,key);action(p,"bank");
        assertEquals(0,LootKeys.count(p));assertEquals(4000,p.bank.count(13307));
        Item weapon=p.bank.findItem(11802,Map.of("charges","123"));assertNotNull(weapon);assertEquals(1,weapon.getAmount());assertEquals(1,p.lootKeysClaimed);
    }
    @Test public void notedToggleWithdrawsNotesToInventory(){
        Hunter p=new Hunter();Item key=key(p,new Item(11802,2));LootKeyWindow.open(p,key);action(p,"note");
        assertTrue(snapshot(p).get("noted").getAsBoolean());action(p,"inventory");
        int note=io.ruin.cache.ItemDef.get(11802).notedId;assertTrue(note>=0);assertEquals(2,p.inventory.count(note));assertEquals(0,LootKeys.count(p));
    }
    @Test public void itemToggleRestoresUnnotedWithdrawalsAndBankAlwaysUsesUnnotedItems(){
        Hunter p=new Hunter();Item key=key(p,new Item(11802,2));LootKeyWindow.open(p,key);action(p,"note");action(p,"item");
        assertFalse(p.lootKeySession.noted);action(p,"note");action(p,"bank");assertEquals(2,p.bank.count(11802));
        assertEquals(0,p.bank.count(io.ruin.cache.ItemDef.get(11802).notedId));
    }
    @Test public void fullBankKeepsTheKeyAndItsUnclaimedContents(){
        Hunter p=new Hunter();p.bank.init(p,1,-1,-1,-1,true);p.bank.add(4151);Item key=key(p,new Item(11802));LootKeyWindow.open(p,key);action(p,"bank");
        assertEquals(1,LootKeys.count(p));assertEquals(0,p.bank.count(11802));assertEquals(0,p.lootKeyValueClaimed);assertNotNull(LootKeys.read(key));
    }
    @Test public void aBankStackAtItsLimitOnlyTakesWhatFitsAndPreservesTheRest(){
        Hunter p=new Hunter();p.bank.add(13307,Integer.MAX_VALUE-10);Item key=key(p,new Item(13307,20));LootKeyWindow.open(p,key);action(p,"bank");
        assertEquals(Integer.MAX_VALUE,p.bank.count(13307));assertEquals(10,LootKeys.value(key));assertEquals(10,p.lootKeyValueClaimed);
        action(p,"inventory");assertEquals(10,p.inventory.count(13307));assertEquals(20,p.lootKeyValueClaimed);assertEquals(0,LootKeys.count(p));
    }
    @Test public void anInventoryStackAtItsLimitKeepsTheUnclaimedRemainderInTheKey(){
        Hunter p=new Hunter();p.inventory.add(13307,Integer.MAX_VALUE-10);Item key=key(p,new Item(13307,20));LootKeyWindow.open(p,key);action(p,"inventory");
        assertEquals(Integer.MAX_VALUE,p.inventory.count(13307));assertEquals(10,LootKeys.value(key));assertEquals(10,p.lootKeyValueClaimed);assertEquals(1,LootKeys.count(p));
        action(p,"bank");assertEquals(10,p.bank.count(13307));assertEquals(20,p.lootKeyValueClaimed);assertEquals(0,LootKeys.count(p));
    }
    @Test public void selectingAnotherTabUpdatesTheDisplayedAndWithdrawnKey(){
        Hunter p=new Hunter();Item first=key(p,new Item(11802)),second=key(p,new Item(4151));LootKeyWindow.open(p,first);
        var session=p.lootKeySession;LootKeyWindow.handle(p,new String[]{session.token,Long.toString(session.revision),"key",LootKeys.read(second).token});
        assertSame(second,p.lootKeySession.selected);action(p,"inventory");assertEquals(1,p.inventory.count(4151));assertEquals(0,p.inventory.count(11802));
        assertEquals(1,LootKeys.count(p));assertSame(first,p.lootKeySession.selected);
    }
    @Test public void clickingAnItemWithdrawsOnlyOneAndPublishesTheRemainingAmount(){
        Hunter p=new Hunter();Item key=key(p,new Item(11802,3));LootKeyWindow.open(p,key);var session=p.lootKeySession;
        LootKeyWindow.handle(p,new String[]{session.token,Long.toString(session.revision),"take","0"});
        assertEquals(1,p.inventory.count(11802));assertEquals(2,LootKeys.read(key).rows.get(0).item.getAmount());assertEquals(0,p.lootKeysClaimed);
    }
    @Test public void trashButtonRequiresConfirmationThenDeletesKeyAndContents()throws Exception{
        Hunter p=new Hunter();Item key=key(p,new Item(11802));long value=LootKeys.value(key);LootKeyWindow.open(p,key);action(p,"trash");
        assertEquals(1,LootKeys.count(p));Runnable confirm=confirmation(p);confirm.run();confirm.run();
        assertEquals(0,LootKeys.count(p));assertEquals(value,p.lootKeyValueDestroyed);assertEquals(0,p.inventory.count(11802));assertNull(p.lootKeySession);
    }
    @Test public void switchingKeysInvalidatesAnOlderTrashConfirmation()throws Exception{
        Hunter p=new Hunter();Item first=key(p,new Item(11802)),second=key(p,new Item(4151));LootKeyWindow.open(p,first);action(p,"trash");Runnable confirm=confirmation(p);
        var session=p.lootKeySession;LootKeyWindow.handle(p,new String[]{session.token,Long.toString(session.revision),"key",LootKeys.read(second).token});confirm.run();
        assertEquals(2,LootKeys.count(p));assertEquals(0,p.lootKeyValueDestroyed);
    }
    @Test public void forgedStaleAndChangedPayloadRequestsCannotWithdraw(){
        Hunter p=new Hunter();Item key=key(p,new Item(11802,2));LootKeyWindow.open(p,key);String[] old=request(p,"inventory");
        LootKeyWindow.handle(p,new String[]{"forged",old[1],"bank"});assertEquals(0,p.bank.count(11802));
        action(p,"note");LootKeyWindow.handle(p,old);assertEquals(0,p.inventory.count(11802));
        LootKeys.withdraw(p,key,false,false,0,1);long claimed=p.lootKeyValueClaimed;action(p,"inventory");
        assertEquals(claimed,p.lootKeyValueClaimed);assertEquals(1,p.inventory.count(11802));assertEquals(1,LootKeys.count(p));
    }
    @Test public void closeAndMovingAwayClearTheSessionWithoutTakingLoot(){
        Hunter p=new Hunter();Item key=key(p,new Item(11802));LootKeyWindow.open(p,key);action(p,"close");assertNull(p.lootKeySession);assertEquals(1,LootKeys.count(p));
        LootKeyWindow.open(p,key);p.position.set(3200,3200,0);action(p,"inventory");assertNull(p.lootKeySession);assertEquals(0,p.inventory.count(11802));
    }
}
