package io.ruin.tools;

import io.ruin.api.protocol.world.WorldStage;
import io.ruin.model.World;
import io.ruin.model.activities.wilderness.BountyHunter;
import io.ruin.model.entity.player.DevTesting;
import io.ruin.model.entity.player.Player;
import io.ruin.model.inter.utils.Config;
import io.ruin.model.item.Item;
import io.ruin.model.item.actions.impl.MysteriousEmblem;
import java.util.*;
import org.junit.*;
import static org.junit.Assert.*;

public class DevTestingTest {
    private WorldStage previous;
    static class Tester extends BountyHunterAudit.Hunter {
        final String name;
        Tester(String name){this.name=name;ip=0x7f000001;}
        @Override public String getName(){return name;}
    }
    @BeforeClass public static void data() throws Exception {BountyHunterAudit.prepare();}
    @Before public void stage(){previous=World.stage;World.stage=WorldStage.DEV;}
    @After public void restore(){World.stage=previous;}
    private static void assign(Tester a,Tester b){BountyHunter.assignTargets(Arrays.asList(a,b),System.currentTimeMillis());}

    @Test public void onlyNamedDistinctDevAccountsReceiveExceptions(){
        Tester a=new Tester("Jon"),b=new Tester("JON2"),c=new Tester("Sales_Tax");
        assertTrue(DevTesting.isPair(a,b));assertTrue(DevTesting.isPair(b,c));
        assertFalse(DevTesting.isPair(a,a));assertFalse(DevTesting.isPair(a,new Tester("Jon")));
        assertFalse(DevTesting.isPair(a,new Tester("Jon3")));assertFalse(DevTesting.isAccount("Admin"));
        World.stage=WorldStage.LIVE;assertFalse(DevTesting.isPair(a,b));
        World.stage=WorldStage.BETA;assertFalse(DevTesting.isPair(a,b));
    }
    @Test public void devAccountsCanProgressThroughTierTenAfterEachFiveMinuteRewardCooldown(){
        Tester a=new Tester("Jon"),b=new Tester("Jon2");
        long now=System.currentTimeMillis();
        for(int tier=1;tier<=10;tier++){
            assign(a,b);assertSame(b,a.bh.target);assertSame(a,b.bh.target);
            long killTime=now+(tier-1)*300000L;
            boolean allowed=io.ruin.model.combat.PvpKillRewards.claim(a,b,killTime);
            assertTrue(b.bh.deathByTarget(a,allowed,killTime));
            if(tier==1)a.inventory.set(0,new Item(b.drops.get(0)));
            assertEquals(tier,MysteriousEmblem.tier(a.inventory.getId(0)));
        }
    }
    @Test public void sameIpGuardsRemainForOtherAccountsAndLiveWorlds(){
        Tester a=new Tester("Jon"),b=new Tester("Ordinary");assign(a,b);assertNull(a.bh.target);
        b=new Tester("Jon2");World.stage=WorldStage.LIVE;assign(a,b);assertNull(a.bh.target);
    }
    @Test public void devMatchingStillRequiresCombatBracketAreaAndDistinctAccountIds(){
        Tester a=new Tester("Jon"),b=new Tester("Sales Tax");b.level=a.level+6;assign(a,b);assertNull(a.bh.target);
        b.level=a.level;b.wildernessLevel=0;assign(a,b);assertNull(a.bh.target);
        b.wildernessLevel=5;b.account=a.account;assign(a,b);assertNull(a.bh.target);
    }
    @Test public void assignedTargetPublishesNameHintArrowAndEveryEmblemTier(){
        Tester a=new Tester("Jon"),b=new Tester("Sales Tax");a.bh.interfaceHidden=true;b.bh.interfaceHidden=true;
        b.inventory.set(0,new Item(MysteriousEmblem.itemId(1)));assign(a,b);
        assertEquals(1,a.icons);assertEquals(1,b.icons);assertEquals("Sales Tax",a.strings.get("90:"+BountyHunter.TARGET_NAME_CHILD));
        assertEquals(Integer.valueOf(0),a.sentVarps.get(20002));
        assertTrue(a.strings.get("90:"+BountyHunter.TARGET_LOCATION_CHILD).contains("Lvl 1-5"));
        for(int tier=1;tier<=10;tier++){
            b.inventory.set(0,new Item(MysteriousEmblem.itemId(tier)));assign(a,b);
            assertEquals(tier,Config.BOUNTY_HUNTER_EMBLEM.get(a));
        }
        b.inventory.set(0,null);assign(a,b);assertEquals(0,Config.BOUNTY_HUNTER_EMBLEM.get(a));
    }
    @Test public void testAccountsDoNotConsumeWildernessIpSlotsButLiveLimitsRemain() {
        Tester a=new Tester("Jon"),b=new Tester("Jon2");List<Player> players=Arrays.asList(a,b);
        assertFalse(DevTesting.atWildernessIpLimit(a.ip,players));
        assertTrue(DevTesting.canLogin("Sales Tax",a.ip,players));
        List<Player> ordinary=Arrays.asList(new Tester("Other A"),new Tester("Other B"));
        assertFalse(DevTesting.canLogin("Other C",a.ip,ordinary));
        assertTrue(DevTesting.canLogin("Sales Tax",a.ip,ordinary));
        World.stage=WorldStage.LIVE;
        assertTrue(DevTesting.atWildernessIpLimit(a.ip,players));
        assertFalse(DevTesting.canLogin("Sales Tax",a.ip,players));
    }

    @Test public void nativeCacheHasTargetTextAndAnEmblemConfiguration() throws Exception {
        var index=io.ruin.Server.fileStore.get(3);
        byte[] panel=index.getFile(90,BountyHunter.TARGET_PANEL_CHILD);
        assertEquals("Target container",0,panel[1]&255);
        for(int child:new int[]{BountyHunter.TARGET_NAME_CHILD,BountyHunter.TARGET_LOCATION_CHILD}){
            byte[] widget=index.getFile(90,child);assertNotNull(widget);
            assertEquals("Target text widget "+child,4,widget[0]==-1?widget[1]&255:widget[0]&255);
        }
        boolean emblem=false;
        for(var script:io.ruin.cache.InterfaceDef.getScripts(90,true))
            for(int i=0;i<script.instructions.length;i++)
                if(script.instructions[i]==25&&script.intArgs[i]==4162)emblem=true;
        assertTrue("Native target overlay reads the target emblem tier",emblem);
        Set<Integer> nativeEmblemFields=new HashSet<>();
        for(int child=0;child<io.ruin.cache.InterfaceDef.COUNTS[90];child++){
            byte[] data=index.getFile(90,child);if(data==null)continue;
            for(int offset=0;offset+26<=data.length;offset++){
                if(data[offset]!=5)continue;
                var in=new io.ruin.api.buffer.InBuffer(data);in.position(offset+1);int[] args=new int[5];boolean ints=true;
                for(int j=0;j<5;j++){if(in.readUnsignedByte()!=0){ints=false;break;}args[j]=in.readInt();}
                if(ints&&args[0]==12)for(int j=1;j<5;j++)nativeEmblemFields.add(args[j]);
            }
        }
        assertFalse("Name must not be overwritten by native emblem/wealth updates: "+nativeEmblemFields,nativeEmblemFields.contains((90<<16)|BountyHunter.TARGET_NAME_CHILD));
        assertFalse("Location must not be overwritten by native emblem/wealth updates: "+nativeEmblemFields,nativeEmblemFields.contains((90<<16)|BountyHunter.TARGET_LOCATION_CHILD));
    }
}
