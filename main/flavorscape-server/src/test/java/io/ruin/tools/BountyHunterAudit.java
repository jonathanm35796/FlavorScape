package io.ruin.tools;

import io.ruin.cache.ItemDef;
import io.ruin.model.activities.wilderness.BountyHunter;
import io.ruin.model.combat.CombatCalculationTest;
import io.ruin.model.entity.player.*;
import io.ruin.model.inter.InterfaceType;
import io.ruin.model.inter.dialogue.Dialogue;
import io.ruin.model.inter.utils.Config;
import io.ruin.model.item.Item;
import io.ruin.model.item.containers.Inventory;
import io.ruin.model.map.Position;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;

/** Read-only runtime probes against production BH methods using isolated simulated players. */
public final class BountyHunterAudit {
    private static final long NOW=System.currentTimeMillis();
    private static final List<String> results=new ArrayList<>();
    private static int sequence=1;
    public static class Hunter extends CombatCalculationTest.Fighter {
        int account=sequence++, ip=account, level=100;
        boolean defending,dead;
        final List<Integer> drops=new ArrayList<>();
        final BountyHunter bh=new BountyHunter(){@Override protected void dropEmblem(Player killer,int id){drops.add(id);}};
        final Position position=new Position(3090,3530,0);
        final Inventory inventory=new Inventory();
        final List<String> messages=new ArrayList<>();
        final Map<String,String> strings=new HashMap<>();
        final Map<Integer,Integer> sentVarps=new HashMap<>();
        Dialogue[] dialogues;
        int icons,resets;
        final PlayerMovement movement=new PlayerMovement(this){
            @Override public boolean startTeleport(io.ruin.process.event.EventConsumer action){return false;}
        };
        final PlayerCombat controlled=new PlayerCombat(){
            @Override public int getLevel(){return level;}
            @Override public boolean isDead(){return dead;}
            @Override public boolean isDefending(long ticks){return defending;}
            @Override public boolean isAttacking(int ticks){return false;}
            @Override public boolean isSkulled(){return false;}
        };
        final io.ruin.network.PacketSender sender=new io.ruin.network.PacketSender(this,null){
            @Override public void write(io.ruin.api.buffer.OutBuffer packet){}
            @Override public void sendString(int group,int child,String value){strings.put(group+":"+child,value);}
            @Override public void sendHintIcon(io.ruin.model.entity.Entity entity){icons++;}
            @Override public void resetHintIcon(boolean minimap){resets++;}
            @Override public void sendVarp(int id,int value){sentVarps.put(id,value);}
        };
        Hunter(){bh.init(this);inventory.init(this,28,-1,-1,-1,false);wildernessLevel=5;varps=new int[6000];controlled.init(this);}
        @Override public BountyHunter getBountyHunter(){return bh;}
        @Override public PlayerCombat getCombat(){return controlled==null?super.getCombat():controlled;}
        @Override public Inventory getInventory(){return inventory;}
        @Override public PlayerMovement getMovement(){return movement;}
        @Override public Position getPosition(){return position;}
        @Override public int getUserId(){return account;}
        @Override public int getIpInt(){return ip;}
        @Override public String getName(){return "Audit"+account;}
        @Override public io.ruin.network.PacketSender getPacketSender(){return sender;}
        @Override public void updateVarp(int id){}
        @Override public void sendMessage(String value){messages.add(value);}
        @Override public void sendFilteredMessage(String value){messages.add(value);}
        @Override public void dialogue(Dialogue... value){dialogues=value;}
        @Override public void closeInterface(InterfaceType type){}
    }
    private static Object call(Object object,String name,Object... args)throws Exception {
        for(Class<?> type=object.getClass();type!=null;type=type.getSuperclass())for(Method method:type.getDeclaredMethods())if(method.getName().equals(name)){
            method.setAccessible(true);
            try{return method.invoke(object,args);}catch(InvocationTargetException e){throw new RuntimeException(e.getCause());}
        }
        throw new NoSuchMethodException(name);
    }
    private static void set(Object object,String name,Object value)throws Exception {
        Field field=findField(object,name);field.setAccessible(true);field.set(object,value);
    }
    private static Object get(Object object,String name)throws Exception {
        Field field=findField(object,name);field.setAccessible(true);return field.get(object);
    }
    private static Field findField(Object object,String name)throws Exception {
        for(Class<?> type=object.getClass();type!=null;type=type.getSuperclass())try{return type.getDeclaredField(name);}catch(NoSuchFieldException ignored){}
        throw new NoSuchFieldException(name);
    }
    private interface Probe {void run()throws Exception;}
    private static void probe(String name,Probe test){
        try{test.run();results.add("PASS\t"+name+"\tverified");}
        catch(AssertionError e){results.add("FAIL\t"+name+"\t"+e.getMessage());}
        catch(Throwable e){results.add("ERROR\t"+name+"\t"+e.toString().replace('\n',' '));}
    }
    private static void require(boolean condition,String observed){if(!condition)throw new AssertionError(observed);}
    private static boolean eligible(Hunter candidate,Hunter searching)throws Exception{return (boolean)call(candidate.bh,"allowAsTarget",searching,NOW);}
    private static void pair(Hunter a,Hunter b)throws Exception{call(a.bh,"target",b,NOW);call(b.bh,"target",a,NOW);}
    private static void redeem(Hunter hunter)throws Exception {
        Item item=hunter.inventory.get(0);ItemDef def=item.getDef();
        int option=def.getOption("redeem");
        require(option>0&&def.inventoryActions!=null&&def.inventoryActions[option-1]!=null,"Redeem action absent for "+def.id);
        def.inventoryActions[option-1].handle(hunter,item);
    }
    private static void confirm(Hunter hunter)throws Exception {
        Object dialogue=hunter.dialogues[0];Runnable action=(Runnable)get(dialogue,"yesAction");action.run();
    }
    public static void prepare()throws Exception {
        ItemCacheFixture.load();
        // Exercise individual production methods without starting unrelated world/map events.
        Field stage=io.ruin.process.CoreWorker.class.getDeclaredField("processStage");
        stage.setAccessible(true);stage.set(null,io.ruin.process.CoreWorker.Stage.UPDATE);
        Class.forName("io.ruin.model.item.actions.impl.MysteriousEmblem");
    }
    public static void main(String[] args)throws Exception {
        prepare();
        probe("default Edgeville participation enabled",()->require(new Hunter().bh.targeting==BountyHunter.Targeting.EDGEVILLE,"Edgeville default absent"));
        probe("distinct eligible players can match",()->require(eligible(new Hunter(),new Hunter()),"eligible pair rejected"));
        probe("same account rejected",()->{Hunter a=new Hunter(),b=new Hunter();b.account=a.account;require(!eligible(a,b),"same account accepted");});
        probe("same IP rejected",()->{Hunter a=new Hunter(),b=new Hunter();b.ip=a.ip;require(!eligible(a,b),"same IP accepted");});
        probe("combat gap five allowed six rejected",()->{Hunter a=new Hunter(),b=new Hunter();b.level=105;require(eligible(a,b),"gap five rejected");b.level=106;require(!eligible(a,b),"gap six accepted");});
        probe("dead players rejected",()->{Hunter a=new Hunter(),b=new Hunter();a.dead=true;require(!eligible(a,b),"dead candidate accepted");require(!(boolean)call(a.bh,"searchForTarget",NOW),"dead searcher accepted");});
        probe("disabled targeting rejected",()->{Hunter a=new Hunter(),b=new Hunter();a.bh.targeting=BountyHunter.Targeting.DISABLED;require(!eligible(a,b),"disabled candidate accepted");});
        probe("Edgeville mode respects bounds",()->{Hunter a=new Hunter(),b=new Hunter();a.bh.targeting=BountyHunter.Targeting.EDGEVILLE;b.position.set(3200,3530,0);require(!eligible(a,b),"out of area accepted");});
        probe("recent opponent blocked for ten minutes",()->{Hunter a=new Hunter(),b=new Hunter();call(a.bh,"target",b,NOW);call(a.bh,"removeTarget");require(!eligible(a,b),"recent opponent accepted");require((boolean)call(a.bh,"allowAsTarget",b,NOW+600001),"history never expires");});
        probe("paired targets are reciprocal with hint icons",()->{Hunter a=new Hunter(),b=new Hunter();pair(a,b);require(a.bh.target==b&&b.bh.target==a&&a.icons==1&&b.icons==1,"pair/icon mismatch");});
        probe("skip clears both targets",()->{Hunter a=new Hunter(),b=new Hunter();pair(a,b);a.bh.skip(false);require(a.bh.target==null&&b.bh.target==null,"pair left stale");require((int)get(a.bh,"skips")==1,"skip not recorded");});
        probe("manual skip blocked during combat",()->{Hunter a=new Hunter(),b=new Hunter();pair(a,b);a.defending=true;a.bh.skip(false);require(a.bh.target==b,"combat skip succeeded");});
        probe("ten skips trigger thirty minute penalty",()->{Hunter a=new Hunter();set(a.bh,"skips",10);require((boolean)call(a.bh,"hasPenalty",NOW),"no penalty");require((long)get(a.bh,"penaltyEnd")==NOW+1800000,"wrong duration");require(!(boolean)call(a.bh,"hasPenalty",NOW+1800001),"penalty fails to expire");});
        probe("safe-area target expires after two minutes",()->{Hunter a=new Hunter(),b=new Hunter();pair(a,b);a.wildernessLevel=0;call(a.bh,"checkActive");require((int)get(a.bh,"returnTicks")==200,"wrong return duration");for(int i=0;i<200;i++)call(a.bh,"checkActive");require(a.bh.target==null&&b.bh.target==null,"expired pair remains");});
        probe("logout clears reciprocal target",()->{Hunter a=new Hunter(),b=new Hunter();pair(a,b);a.bh.loggedOut();require(a.bh.target==null&&b.bh.target==null,"logout left stale target");});
        probe("target kill upgrades highest emblem and records kill",()->{Hunter victim=new Hunter(),killer=new Hunter();killer.inventory.set(0,new Item(12746));pair(victim,killer);require(victim.bh.deathByTarget(killer),"target kill not recognised");require(killer.inventory.getId(0)==12748,"tier one did not upgrade");require(Config.BOUNTY_HUNTER_TARGET_KILLS.get(killer)==1,"target counter absent");require(victim.bh.target==null&&killer.bh.target==null,"death pair not cleared");long now=System.currentTimeMillis();require((long)get(victim.bh,"penaltyEnd")>=now+359000,"victim cooldown wrong");require((long)get(killer.bh,"penaltyEnd")>=now+59000,"killer cooldown wrong");});
        probe("same IP rogue kill cannot generate emblem rewards",()->{Hunter victim=new Hunter(),killer=new Hunter();killer.ip=victim.ip;killer.inventory.set(0,new Item(12746));victim.bh.deathByTarget(killer);require(killer.inventory.getId(0)==12746,"same-IP rogue kill upgraded emblem to "+killer.inventory.getId(0));});
        probe("death resets displayed target kill streak",()->{Hunter victim=new Hunter(),killer=new Hunter();Config.BOUNTY_HUNTER_TARGET_KILLS.set(victim,4);killer.inventory.set(0,new Item(12746));pair(victim,killer);victim.bh.deathByTarget(killer);require(Config.BOUNTY_HUNTER_TARGET_KILLS.get(victim)==0,"victim target streak remains "+Config.BOUNTY_HUNTER_TARGET_KILLS.get(victim));});
        probe("large target wealth uses 64 bit multiplication",()->{Hunter a=new Hunter();int id=13204;int old=ItemDef.get(id).protectValue;try{ItemDef.get(id).protectValue=1000;a.inventory.set(0,new Item(id,3000000));long wealth=(long)call(a.bh,"countWealth",a.inventory);require(wealth==3000000000L,"expected 3000000000, actual "+wealth);}finally{ItemDef.get(id).protectValue=old;}});
        probe("all ten emblem redemption actions registered",()->{for(int id:new int[]{12746,12748,12749,12750,12751,12752,12753,12754,12755,12756}){ItemDef d=ItemDef.get(id);int o=d.getOption("redeem");require(o>0&&d.inventoryActions!=null&&d.inventoryActions[o-1]!=null,"missing redeem for "+id);}});
        probe("emblem redemption pays configured blood money",()->{Hunter a=new Hunter();a.wildernessLevel=0;a.inventory.set(0,new Item(12746));redeem(a);confirm(a);require(a.inventory.count(13307)==3000&&a.inventory.count(12746)==0,"incorrect tier-one payout");});
        probe("all ten emblem tiers redeem at configured values",()->{int[] ids={12746,12748,12749,12750,12751,12752,12753,12754,12755,12756},values={3000,5000,7500,10000,15000,20000,30000,50000,75000,100000};for(int i=0;i<ids.length;i++){Hunter a=new Hunter();a.wildernessLevel=0;a.inventory.set(0,new Item(ids[i]));redeem(a);confirm(a);require(a.inventory.count(13307)==values[i],"wrong payout for tier "+(i+1));}});
        probe("redemption confirmation rechecks Wilderness restriction",()->{Hunter a=new Hunter();a.wildernessLevel=0;a.inventory.set(0,new Item(12746));redeem(a);a.wildernessLevel=5;confirm(a);require(a.inventory.count(13307)==0,"confirmation redeemed inside Wilderness");});
        probe("redemption confirmation rejects removed emblem",()->{Hunter a=new Hunter();a.wildernessLevel=0;a.inventory.set(0,new Item(12746));redeem(a);a.inventory.set(0,null);confirm(a);require(a.inventory.count(13307)==0,"confirmation paid without owning emblem");});
        probe("redemption preserves emblem if payout would overflow",()->{Hunter a=new Hunter();a.wildernessLevel=0;a.inventory.set(0,new Item(12746));a.inventory.set(1,new Item(13307,Integer.MAX_VALUE));redeem(a);if(a.dialogues!=null)confirm(a);require(a.inventory.count(12746)==1,"emblem removed despite full Blood Money stack");});
        probe("overlay enable message matches enabled state",()->{Hunter a=new Hunter();a.bountyHunterOverlay=false;new io.ruin.model.inter.journal.toggles.BountyOverlay().select(a);require(a.bountyHunterOverlay&&!a.messages.get(0).contains("no longer"),"enable message says overlay will no longer be shown");});
        probe("local file login assigns distinct account IDs",()->{
            io.ruin.api.protocol.login.LoginInfo a=new io.ruin.api.protocol.login.LoginInfo("127.0.0.1","AuditA","unused","","","",0,false,0);
            io.ruin.api.protocol.login.LoginInfo b=new io.ruin.api.protocol.login.LoginInfo("127.0.0.2","AuditB","unused","","","",0,false,0);
            Path directory=Files.createTempDirectory("bh-account-audit-");Path file=directory.resolve("ids.json");
            try{
                LocalAccountIds ids=new LocalAccountIds(file);
                a.update(ids.getOrCreate(a.name),a.name,"",Collections.emptyList(),0);
                b.update(ids.getOrCreate(b.name),b.name,"",Collections.emptyList(),0);
                require(a.userId>0&&a.userId!=b.userId,"file-login accounts do not have distinct IDs");
                require(new LocalAccountIds(file).getOrCreate("audita")==a.userId,"ID changed on reopening registry");
            }finally{Files.deleteIfExists(file);Files.deleteIfExists(directory);}
        });
        probe("Bounty teleport rejects unlearned spell",()->require(!io.ruin.model.skills.magic.spells.BountyTeleport.cast(new Hunter(),0),"unlearned teleport accepted"));
        probe("Bounty teleport rejects missing target",()->{Hunter a=new Hunter();Config.BOUNTY_HUNTER_TELEPORT.set(a,1);require(!io.ruin.model.skills.magic.spells.BountyTeleport.cast(a,0),"no-target teleport accepted");});
        probe("Bounty teleport rejects nearby target",()->{Hunter a=new Hunter(),b=new Hunter();a.bh.target=b;Config.BOUNTY_HUNTER_TELEPORT.set(a,1);require(!io.ruin.model.skills.magic.spells.BountyTeleport.cast(a,0),"nearby target accepted");});
        probe("Bounty teleport rejects safe-area target",()->{Hunter a=new Hunter(),b=new Hunter();a.bh.target=b;b.position.set(3200,3500,0);b.wildernessLevel=0;Config.BOUNTY_HUNTER_TELEPORT.set(a,1);require(!io.ruin.model.skills.magic.spells.BountyTeleport.cast(a,0),"safe target accepted");});
        probe("Bounty teleport propagates rejected teleport request",()->{Hunter a=new Hunter(),b=new Hunter();a.bh.target=b;b.position.set(3200,3550,0);Config.BOUNTY_HUNTER_TELEPORT.set(a,1);require(!io.ruin.model.skills.magic.spells.BountyTeleport.cast(a,0),"cast returns success even when startTeleport returns false; spell wrapper charges runes/XP");});
        probe("emblems cannot be protected on death",()->{Hunter a=new Hunter();require(!(boolean)call(new io.ruin.model.inter.handlers.IKOD(),"allowProtect",a,new Item(12756)),"tier-ten emblem is protectable");});
        probe("target preferences and anti-repeat history persist",()->{Hunter a=new Hunter(),b=new Hunter();call(a.bh,"target",b,NOW);call(a.bh,"removeTarget");a.bh.targeting=BountyHunter.Targeting.EDGEVILLE;set(a.bh,"penaltyEnd",NOW+60000);com.google.gson.Gson gson=new com.google.gson.GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();BountyHunter restored=gson.fromJson(gson.toJson(a.bh,BountyHunter.class),BountyHunter.class);restored.init(a);require(restored.targeting==BountyHunter.Targeting.EDGEVILLE,"mode not saved");require((long)get(restored,"penaltyEnd")==NOW+60000,"penalty not saved");require(((io.ruin.utility.TimedList)get(restored,"recentTargets")).contains(b.account,NOW,10L),"history not saved");require(restored.target==null,"live opponent reference persisted");});
        probe("cached target overlay contains server widget slots",()->require(io.ruin.cache.InterfaceDef.COUNTS[90]>66,"overlay lacks expected children"));
        Path output=Paths.get(args.length==0?"../../.tools/bounty-hunter-audit.tsv":args[0]);
        Files.createDirectories(output.toAbsolutePath().getParent());
        List<String> lines=new ArrayList<>();lines.add("status\tcheck\tobservation");lines.addAll(results);Files.write(output,lines);
        for(String line:results)System.out.println("BH_AUDIT "+line);
        System.out.println("BH_AUDIT TOTAL="+results.size()+" PASS="+results.stream().filter(s->s.startsWith("PASS")).count()+" FAIL="+results.stream().filter(s->s.startsWith("FAIL")).count()+" ERROR="+results.stream().filter(s->s.startsWith("ERROR")).count());
        // Server utility pools are not daemon threads; terminate this standalone audit process.
        System.exit(results.stream().allMatch(s->s.startsWith("PASS"))?0:1);
    }
}
