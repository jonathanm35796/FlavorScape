package io.ruin.tools;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import io.ruin.api.protocol.world.WorldStage;
import io.ruin.model.World;
import io.ruin.model.combat.Killer;
import io.ruin.model.combat.PvpKillRewards;
import io.ruin.model.entity.player.Player;
import io.ruin.model.entity.player.PlayerGroup;
import io.ruin.model.item.Item;
import io.ruin.utility.TimedList;
import org.junit.*;
import static org.junit.Assert.*;

public class PvpKillRewardsTest {
    private static final long NOW=1_800_000_000_000L;
    private WorldStage previousStage;
    private int previousMultiplier;

    static class Hunter extends BountyHunterAudit.Hunter {
        String name="Reward test";
        int cash;
        Hunter(){dailyTasks=new io.ruin.model.activities.tasks.DailyTask[0];nextWildernessBonus=Long.MAX_VALUE;}
        @Override public String getName(){return name;}
        @Override public boolean isGroup(PlayerGroup group){return false;}
        @Override public io.ruin.model.item.containers.bank.BankPin getBankPin(){return new io.ruin.model.item.containers.bank.BankPin();}
        @Override public void rewardBm(Player target,int amount){cash+=amount;}
    }

    @BeforeClass public static void data()throws Exception{
        BountyHunterAudit.prepare();
        for(int id=0;id<io.ruin.model.map.Region.LOADED.length;id++)
            if(io.ruin.model.map.Region.LOADED[id]==null)io.ruin.model.map.Region.LOADED[id]=new io.ruin.model.map.Region(id);
        var region=io.ruin.model.map.Region.get(3184,3944);
        var json=new com.google.gson.JsonParser().parse(java.nio.file.Files.readString(java.nio.file.Path.of("data/region_keys.json"))).getAsJsonObject();
        int id=((3184>>6)<<8)|(3944>>6);
        if(json.has(Integer.toString(id)))region.keys=new com.google.gson.Gson().fromJson(json.get(Integer.toString(id)),int[].class);
        region.init();
    }
    @Before public void setUp(){previousStage=World.stage;previousMultiplier=World.bmMultiplier;World.stage=WorldStage.LIVE;World.bmMultiplier=0;}
    @After public void restore(){World.stage=previousStage;World.bmMultiplier=previousMultiplier;}

    private static void kill(Hunter winner,Hunter victim,long time){
        winner.bh.target=victim;victim.bh.target=winner;
        boolean allowed=PvpKillRewards.claim(winner,victim,time);
        victim.bh.deathByTarget(winner,allowed,time);
        Killer killer=new Killer();killer.player=winner;killer.reward(victim,true,allowed);
    }

    @Test public void sharedDeathRewardGatePaysTenMillionBaseAndBlocksAllRepeatBonuses(){
        assertEquals(10_000_000,Killer.BASE_BM_REWARD);
        Hunter winner=new Hunter(),victim=new Hunter();winner.inventory.set(0,new Item(12746));
        kill(winner,victim,NOW);
        assertTrue(winner.cash>=10_000_000);assertEquals(11L*512,winner.bountyHunterPointUnits);
        assertEquals(12748,winner.inventory.getId(0));assertEquals(1,winner.currentKillSpree);
        int paid=winner.cash;
        kill(winner,victim,NOW+1000);
        assertEquals(paid,winner.cash);assertEquals(11L*512,winner.bountyHunterPointUnits);
        assertEquals(12748,winner.inventory.getId(0));assertEquals(1,winner.currentKillSpree);
        assertTrue(winner.messages.stream().anyMatch(s->s.contains("4:59 remaining")));
        kill(winner,victim,NOW+300000);
        assertTrue(winner.cash>paid);assertEquals(22L*512,winner.bountyHunterPointUnits);
        assertEquals(12749,winner.inventory.getId(0));
    }

    @Test public void edgevilleCooldownExpiresAtFiveMinutesAndBlockedAttemptsDoNotRestartIt(){
        Hunter winner=new Hunter(),victim=new Hunter();
        assertTrue(victim.getPosition().inBounds(Killer.EDGEVILLE_FARM_SKIP_BOUNDS));
        assertTrue(PvpKillRewards.claim(winner,victim,NOW));
        assertFalse(PvpKillRewards.claim(winner,victim,NOW+60000));
        assertEquals(240000,PvpKillRewards.remainingMillis(winner,victim,NOW+60000));
        assertFalse(PvpKillRewards.claim(winner,victim,NOW+299999));
        assertTrue(PvpKillRewards.claim(winner,victim,NOW+300000));
    }

    @Test public void anotherOpponentCanPayImmediatelyButAnAlternateAccountOnTheSameIpCannot(){
        Hunter winner=new Hunter(),victim=new Hunter(),other=new Hunter(),alternate=new Hunter();alternate.ip=victim.ip;
        assertTrue(PvpKillRewards.claim(winner,victim,NOW));
        assertTrue(PvpKillRewards.claim(winner,other,NOW+1));
        assertFalse(PvpKillRewards.claim(winner,alternate,NOW+2));
    }

    @Test public void changingVictimIpDoesNotBypassAccountCooldown(){
        Hunter winner=new Hunter(),victim=new Hunter();assertTrue(PvpKillRewards.claim(winner,victim,NOW));
        victim.ip+=10000;assertFalse(PvpKillRewards.claim(winner,victim,NOW+1));
    }

    @Test public void namedDevAccountsStillWaitFiveMinutesForTheSamePerson(){
        World.stage=WorldStage.DEV;
        Hunter winner=new Hunter(),victim=new Hunter(),other=new Hunter();
        winner.name="Jon";victim.name="Jon2";other.name="Sales Tax";victim.ip=other.ip=winner.ip;
        assertTrue(PvpKillRewards.claim(winner,victim,NOW));
        assertFalse(PvpKillRewards.claim(winner,victim,NOW+1));
        assertTrue(PvpKillRewards.claim(winner,other,NOW+2));
        assertTrue(PvpKillRewards.claim(winner,victim,NOW+300000));
    }

    @Test public void sameAccountAndOrdinarySameIpKillsNeverPay(){
        Hunter winner=new Hunter(),victim=new Hunter();assertFalse(PvpKillRewards.claim(winner,winner,NOW));
        victim.account=winner.account;assertFalse(PvpKillRewards.claim(winner,victim,NOW));
        victim.account+=10000;victim.ip=winner.ip;assertFalse(PvpKillRewards.claim(winner,victim,NOW));
    }

    @Test public void rewardHistorySurvivesSavingAndRelogging(){
        Hunter winner=new Hunter(),victim=new Hunter();assertTrue(PvpKillRewards.claim(winner,victim,NOW));
        Gson gson=new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
        Hunter relogged=new Hunter();
        relogged.bmUserLogs=gson.fromJson(gson.toJson(winner.bmUserLogs),TimedList.class);
        relogged.bmIpLogs=gson.fromJson(gson.toJson(winner.bmIpLogs),TimedList.class);
        assertFalse(PvpKillRewards.claim(relogged,victim,NOW+299999));
        assertTrue(PvpKillRewards.claim(relogged,victim,NOW+300000));
    }

    @Test public void earlierBountyHunterRewardHistoryUsesTheNewFiveMinuteLimit()throws Exception{
        Hunter winner=new Hunter(),victim=new Hunter();
        var field=io.ruin.model.activities.wilderness.BountyHunter.class.getDeclaredField("rewardedAccounts");field.setAccessible(true);
        ((TimedList)field.get(winner.bh)).add(victim.account,NOW);
        assertFalse(PvpKillRewards.claim(winner,victim,NOW+299999));
        assertTrue(PvpKillRewards.claim(winner,victim,NOW+300000));
    }
}
