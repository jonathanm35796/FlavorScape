package io.ruin.model.combat;

import io.ruin.model.World;
import io.ruin.api.protocol.world.WorldStage;
import io.ruin.model.activities.duelarena.Duel;
import io.ruin.model.entity.player.GameMode;
import io.ruin.model.inter.InterfaceHandler;
import io.ruin.model.inter.InterfaceType;
import io.ruin.model.item.Item;
import io.ruin.model.item.containers.Inventory;
import io.ruin.model.item.containers.Trade;
import java.util.*;
import org.junit.*;
import static org.junit.Assert.*;

/** Exercises both native trade screens and actual container transfers, using same-IP accounts. */
public class TradeInteractionTest {
    private WorldStage previous;
    @BeforeClass public static void data() throws Exception {
        CombatEquipmentInteractionTest.data();
        java.lang.reflect.Field stage=io.ruin.process.CoreWorker.class.getDeclaredField("processStage");stage.setAccessible(true);
        stage.set(null,io.ruin.process.CoreWorker.Stage.UPDATE);
    }
    @Before public void stage(){previous=World.stage;World.stage=WorldStage.DEV;}
    @After public void restore(){World.stage=previous;}
    static class Trader extends CombatEquipmentInteractionTest.Fighter {
        final String name;final int id;final Trade trade=new Trade(this);final Inventory inventory=new Inventory();final Duel duel=new Duel(this);
        final List<String> messages=new ArrayList<>();final Set<Integer> interfaces=new HashSet<>();GameMode mode=GameMode.STANDARD;
        Trader(String name,int id){this.name=name;this.id=id;inventory.init(this,28,-1,-1,-1,false);}
        @Override public String getName(){return name;}
        @Override public int getUserId(){return id;}
        @Override public int getIpInt(){return 0x7f000001;}
        @Override public Trade getTrade(){return trade;}
        @Override public Inventory getInventory(){return inventory;}
        @Override public Duel getDuel(){return duel;}
        @Override public GameMode getGameMode(){return mode;}
        @Override public boolean isAdmin(){return false;}
        @Override public boolean isLocked(){return false;}
        @Override public void sendMessage(String message){messages.add(message);}
        @Override public void openInterface(InterfaceType type,int id){interfaces.add(id);}
        @Override public void closeInterface(InterfaceType type){}
    }
    private void open(Trader a,Trader b){a.trade.request(b);b.trade.request(a);assertTrue(a.interfaces.contains(335));assertTrue(b.interfaces.contains(335));}
    private void click(Trader p,int group,int child){InterfaceHandler.HANDLERS[group].actions[child].handleClick(p,1,-1,-1);}
    @Test public void sameIpOrdinaryAccountsCanExchangeItemsThroughBothConfirmationScreens(){
        Trader a=new Trader("Ordinary A",101),b=new Trader("Ordinary B",102);open(a,b);
        a.inventory.add(13307,1000);b.inventory.add(4151);a.trade.offer(a.inventory.findItem(13307),500);b.trade.offer(b.inventory.findItem(4151),1);
        click(a,335,10);click(b,335,10);assertTrue(a.interfaces.contains(334));assertTrue(b.interfaces.contains(334));
        click(a,334,13);click(b,334,13);
        assertEquals(500,a.inventory.count(13307));assertEquals(1,a.inventory.count(4151));assertEquals(500,b.inventory.count(13307));assertEquals(0,b.inventory.count(4151));
        click(a,334,13);click(b,334,13);assertEquals(500,b.inventory.count(13307));assertEquals(1,a.inventory.count(4151));
    }
    @Test public void decliningReturnsOffersToBothOwners(){
        Trader a=new Trader("Jon",1),b=new Trader("Jon2",2);open(a,b);
        a.inventory.add(13307,100);b.inventory.add(4151);a.trade.offer(a.inventory.findItem(13307),100);b.trade.offer(b.inventory.findItem(4151),1);
        a.trade.close();assertEquals(100,a.inventory.count(13307));assertEquals(1,b.inventory.count(4151));
    }
    @Test public void onlyNamedDevPairsCanBypassIronmanTradingRestrictions(){
        Trader a=new Trader("Jon",1),b=new Trader("Sales Tax",3);a.mode=GameMode.IRONMAN;b.mode=GameMode.ULTIMATE_IRONMAN;open(a,b);
        Trader ordinary=new Trader("Someone else",4);ordinary.mode=GameMode.IRONMAN;ordinary.trade.request(new Trader("Jon2",2));
        assertTrue(ordinary.interfaces.isEmpty());assertTrue(ordinary.messages.contains("Ironmen stand alone."));
        World.stage=WorldStage.LIVE;
        Trader live=new Trader("Jon",1);live.mode=GameMode.IRONMAN;live.trade.request(new Trader("Jon2",2));
        assertTrue(live.interfaces.isEmpty());assertTrue(live.messages.contains("Ironmen stand alone."));
    }
    @Test public void selfAndStaleRequestsDoNotOpenOrCrash(){
        Trader a=new Trader("Jon",1),b=new Trader("Jon2",2);a.trade.request(a);assertTrue(a.interfaces.isEmpty());
        a.trade.request(null);
        // A legacy invalid account ID could formerly match the default -1 request without a timer.
        Trader legacy=new Trader("Legacy",-1);legacy.trade.request(b);assertTrue(legacy.interfaces.isEmpty());
    }
}
