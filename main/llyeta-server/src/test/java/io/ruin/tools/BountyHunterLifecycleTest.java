package io.ruin.tools;

import io.ruin.cache.ItemDef;
import io.ruin.cache.NPCDef;
import io.ruin.model.activities.wilderness.BountyHunter;
import io.ruin.model.entity.player.LocalAccountIds;
import io.ruin.model.entity.player.Player;
import io.ruin.model.inter.utils.Config;
import io.ruin.model.item.Item;
import io.ruin.model.item.actions.impl.MysteriousEmblem;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import org.junit.*;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;

public class BountyHunterLifecycleTest {
    @Rule public TemporaryFolder directory=new TemporaryFolder();
    @BeforeClass public static void data()throws Exception{BountyHunterAudit.prepare();}
    private static BountyHunterAudit.Hunter hunter(){return new BountyHunterAudit.Hunter();}
    private static void match(BountyHunterAudit.Hunter a,BountyHunterAudit.Hunter b,long time){
        BountyHunter.assignTargets(Arrays.asList(a,b),time);
        assertSame(b,a.bh.target);assertSame(a,b.bh.target);
    }
    private static void pair(BountyHunterAudit.Hunter a,BountyHunterAudit.Hunter b){a.bh.target=b;b.bh.target=a;}
    private static Object field(Object target,Class<?> type,String name)throws Exception{Field f=type.getDeclaredField(name);f.setAccessible(true);return f.get(target);}
    private static Runnable confirmation(BountyHunterAudit.Hunter a)throws Exception{
        return (Runnable)field(a.dialogues[0],io.ruin.model.inter.dialogue.YesNoDialogue.class,"yesAction");
    }
    @Test public void fullAssignedTargetProgressionEndsInOneHundredThousandBloodMoney()throws Exception{
        BountyHunterAudit.Hunter winner=hunter();long now=System.currentTimeMillis();
        for(int tier=1;tier<=10;tier++){
            BountyHunterAudit.Hunter victim=hunter();
            match(winner,victim,now+tier*61000L);
            assertTrue(victim.bh.deathByTarget(winner));
            assertNull(winner.bh.target);assertNull(victim.bh.target);
            if(tier==1){assertEquals(Collections.singletonList(MysteriousEmblem.itemId(1)),victim.drops);winner.inventory.set(0,new Item(victim.drops.get(0)));}
            assertEquals(tier,MysteriousEmblem.tier(winner.inventory.getId(0)));
        }
        assertEquals(10,Config.BOUNTY_HUNTER_TARGET_KILLS.get(winner));
        assertEquals(10,Config.BOUNTY_HUNTER_TARGET_RECORD.get(winner));
        winner.wildernessLevel=0;
        MysteriousEmblem.offerRedemption(winner,winner.inventory.get(0));confirmation(winner).run();
        assertEquals(100_000_000,winner.inventory.count(13307));
        assertEquals(0,winner.inventory.count(MysteriousEmblem.itemId(10)));
    }
    @Test public void highestEmblemUpgradesOnceAndTierTenRemainsCapped(){
        BountyHunterAudit.Hunter winner=hunter(),victim=hunter();
        winner.inventory.set(0,new Item(MysteriousEmblem.itemId(2)));
        winner.inventory.set(1,new Item(MysteriousEmblem.itemId(8)));
        pair(winner,victim);assertTrue(victim.bh.deathByTarget(winner));
        assertEquals(2,MysteriousEmblem.tier(winner.inventory.getId(0)));
        assertEquals(9,MysteriousEmblem.tier(winner.inventory.getId(1)));
        winner.inventory.set(1,new Item(MysteriousEmblem.itemId(10)));
        BountyHunterAudit.Hunter another=hunter();pair(winner,another);assertTrue(another.bh.deathByTarget(winner));
        assertEquals(10,MysteriousEmblem.tier(winner.inventory.getId(1)));
        assertEquals(Collections.singletonList(MysteriousEmblem.itemId(1)),another.drops);
    }
    @Test public void everyLostEmblemDowngradesToTheCorrectTier(){
        for(int tier=1;tier<=10;tier++){
            int id=MysteriousEmblem.itemId(tier);
            assertEquals(MysteriousEmblem.itemId(Math.max(1,tier-1)),MysteriousEmblem.downgradedId(id));
            assertTrue(ItemDef.get(id).neverProtect);
        }
        assertEquals(-1,MysteriousEmblem.downgradedId(385));
    }
    @Test public void rogueAndSameIpKillsNeverUpgradeEmblems(){
        BountyHunterAudit.Hunter winner=hunter(),victim=hunter();winner.inventory.set(0,new Item(12746));
        assertFalse(victim.bh.deathByTarget(winner));assertEquals(12746,winner.inventory.getId(0));
        BountyHunterAudit.Hunter sameIp=hunter();sameIp.ip=winner.ip;pair(winner,sameIp);
        assertFalse(sameIp.bh.deathByTarget(winner));assertEquals(12746,winner.inventory.getId(0));assertTrue(sameIp.drops.isEmpty());
    }
    @Test public void repeatedVictimAndAlternateAccountOnSameIpCannotFarmRewards()throws Exception{
        BountyHunterAudit.Hunter winner=hunter(),victim=hunter();winner.inventory.set(0,new Item(12746));
        pair(winner,victim);assertTrue(victim.bh.deathByTarget(winner));assertEquals(12748,winner.inventory.getId(0));
        pair(winner,victim);assertFalse(victim.bh.deathByTarget(winner));assertEquals(12748,winner.inventory.getId(0));
        BountyHunterAudit.Hunter alt=hunter();alt.ip=victim.ip;pair(winner,alt);
        assertFalse(alt.bh.deathByTarget(winner));assertEquals(12748,winner.inventory.getId(0));
        com.google.gson.Gson gson=new com.google.gson.GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
        String saved=gson.toJson(winner.bh,BountyHunter.class);
        BountyHunter restored=gson.fromJson(saved,BountyHunter.class);restored.init(winner);
        io.ruin.utility.TimedList history=(io.ruin.utility.TimedList)field(restored,BountyHunter.class,"rewardedAccounts");
        assertTrue(history.contains(victim.account,System.currentTimeMillis(),10));
    }
    @Test public void disabledDeepAndSameIpPlayersCannotBeAssigned(){
        BountyHunterAudit.Hunter a=hunter(),b=hunter();
        b.ip=a.ip;BountyHunter.assignTargets(Arrays.asList(a,b),System.currentTimeMillis());assertNull(a.bh.target);
        b.ip++;b.wildernessLevel=6;BountyHunter.assignTargets(Arrays.asList(a,b),System.currentTimeMillis());assertNull(a.bh.target);
        b.wildernessLevel=5;b.bh.targeting=BountyHunter.Targeting.DISABLED;
        BountyHunter.assignTargets(Arrays.asList(a,b),System.currentTimeMillis());assertNull(a.bh.target);
    }
    @Test public void logoutAlwaysClearsPairEvenDuringCombat(){
        BountyHunterAudit.Hunter a=hunter(),b=hunter();pair(a,b);a.defending=true;a.bh.loggedOut();
        assertNull(a.bh.target);assertNull(b.bh.target);
    }
    @Test public void returnAllowanceKeepsTargetPanelVisibleAndHonoursOverlayPreference(){
        BountyHunterAudit.Hunter a=hunter(),b=hunter();pair(a,b);a.position.set(3200,3550,0);
        a.bh.refreshOverlay();assertFalse(a.bh.interfaceHidden);
        a.bountyHunterOverlay=false;a.bh.refreshOverlay();assertTrue(a.bh.interfaceHidden);
        a.bountyHunterOverlay=true;a.bh.target=null;a.bh.refreshOverlay();assertTrue(a.bh.interfaceHidden);
    }
    @Test public void deepWildernessReturnTimeoutClearsPairDespiteCombat()throws Exception{
        BountyHunterAudit.Hunter a=hunter(),b=hunter();pair(a,b);a.wildernessLevel=6;a.defending=true;
        Method check=BountyHunter.class.getDeclaredMethod("checkActive");check.setAccessible(true);
        for(int tick=0;tick<=200;tick++)check.invoke(a.bh);
        assertNull(a.bh.target);assertNull(b.bh.target);
    }
    @Test public void staleChangedAndRepeatedRedemptionConfirmationsDoNotPay()throws Exception{
        BountyHunterAudit.Hunter a=hunter();a.wildernessLevel=0;a.inventory.set(0,new Item(12756));
        MysteriousEmblem.offerRedemption(a,a.inventory.get(0));Runnable action=confirmation(a);
        a.inventory.get(0).setId(12746);action.run();assertEquals(0,a.inventory.count(13307));
        MysteriousEmblem.offerRedemption(a,a.inventory.get(0));action=confirmation(a);action.run();action.run();
        assertEquals(3_000_000,a.inventory.count(13307));
    }
    @Test public void fullInventoryRedeemsIntoTheEmblemsFreedSlot()throws Exception{
        BountyHunterAudit.Hunter a=hunter();a.wildernessLevel=0;
        for(int slot=0;slot<28;slot++)a.inventory.set(slot,new Item(385));
        a.inventory.set(0,new Item(12756));MysteriousEmblem.offerRedemption(a,a.inventory.get(0));confirmation(a).run();
        assertEquals(100_000_000,a.inventory.count(13307));assertEquals(0,a.inventory.getFreeSlots());
    }
    @Test public void everyTierRedeemsForTheIncreasedBloodMoneyAmount()throws Exception{
        int[] expected={3_000_000,5_000_000,7_500_000,10_000_000,15_000_000,20_000_000,30_000_000,50_000_000,75_000_000,100_000_000};
        for(int tier=1;tier<=10;tier++){
            BountyHunterAudit.Hunter a=hunter();a.wildernessLevel=0;
            a.inventory.set(0,new Item(MysteriousEmblem.itemId(tier)));
            MysteriousEmblem.offerRedemption(a,a.inventory.get(0));confirmation(a).run();
            assertEquals("Tier "+tier,expected[tier-1],a.inventory.count(13307));
            assertEquals(0,a.inventory.count(MysteriousEmblem.itemId(tier)));
        }
    }
    @Test public void increasedPayoutRejectsCurrencyOverflowWithoutConsumingEmblem(){
        BountyHunterAudit.Hunter a=hunter();a.wildernessLevel=0;
        a.inventory.set(0,new Item(13307,Integer.MAX_VALUE-99_999_999));
        a.inventory.set(1,new Item(12756));
        MysteriousEmblem.offerRedemption(a,a.inventory.get(1));
        assertEquals(Integer.MAX_VALUE-99_999_999,a.inventory.count(13307));
        assertEquals(1,a.inventory.count(12756));
    }
    @Test public void traderAcceptsEveryTierAndPaysOneHundredMillionForTierTen()throws Exception{
        Class.forName("io.ruin.model.entity.npc.actions.edgeville.EmblemTrader");
        for(int tier=1;tier<=10;tier++)assertNotNull(NPCDef.get(315).itemActions.get(MysteriousEmblem.itemId(tier)));
        BountyHunterAudit.Hunter a=hunter();a.wildernessLevel=0;a.inventory.set(0,new Item(12756));
        NPCDef.get(315).itemActions.get(12756).handle(a,a.inventory.get(0),null);confirmation(a).run();
        assertEquals(100_000_000,a.inventory.count(13307));
    }
    @Test public void talkingToTraderOffersTheHighestEmblemForRedemption()throws Exception{
        Class.forName("io.ruin.model.entity.npc.actions.edgeville.EmblemTrader");
        BountyHunterAudit.Hunter a=hunter();a.wildernessLevel=0;
        a.inventory.set(0,new Item(12746));a.inventory.set(1,new Item(12756));
        NPCDef trader=NPCDef.get(315);int option=trader.getOption("talk-to");assertTrue(option>0);
        trader.defaultActions[option-1].handle(a,null);
        io.ruin.model.inter.utils.Option[] options=(io.ruin.model.inter.utils.Option[])field(a.dialogues[0],io.ruin.model.inter.dialogue.OptionsDialogue.class,"options");
        options[0].select(a);confirmation(a).run();
        assertEquals(100_000_000,a.inventory.count(13307));assertEquals(1,a.inventory.count(12746));assertEquals(0,a.inventory.count(12756));
    }
    @Test public void distinctIdsSurviveRestartAndCaseChanges()throws Exception{
        Path file=directory.getRoot().toPath().resolve("ids.json");LocalAccountIds ids=new LocalAccountIds(file);
        int first=ids.getOrCreate("Aa"),second=ids.getOrCreate("BB");
        assertNotEquals(first,second);assertEquals(first,ids.getOrCreate("aa"));
        LocalAccountIds reopened=new LocalAccountIds(file);assertEquals(first,reopened.getOrCreate("AA"));
        assertEquals(second,reopened.getOrCreate("bb"));assertNotEquals(second,reopened.getOrCreate("Third"));
    }
    @Test public void corruptIdentityRegistryFailsWithoutReassigningAccounts()throws Exception{
        Path file=directory.getRoot().toPath().resolve("ids.json");Files.writeString(file,"{\"one\":1,\"two\":1}");
        try{new LocalAccountIds(file).getOrCreate("Third");fail("duplicate IDs accepted");}catch(java.io.IOException expected){}
        assertEquals("{\"one\":1,\"two\":1}",Files.readString(file));
    }
}
