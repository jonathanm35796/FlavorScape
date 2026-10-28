package io.ruin.tools;

import com.google.gson.GsonBuilder;
import io.ruin.cache.ItemDef;
import io.ruin.cache.NPCDef;
import io.ruin.cache.ObjectDef;
import io.ruin.model.activities.wilderness.LootKeys;
import io.ruin.model.entity.player.GameMode;
import io.ruin.model.item.Item;
import java.util.*;
import org.junit.*;
import static org.junit.Assert.*;

public class LootKeysTest {
    static class Hunter extends BountyHunterAudit.Hunter {
        GameMode mode=GameMode.STANDARD;
        Hunter(){position.set(LootKeys.CHEST_X,LootKeys.CHEST_Y,0);wildernessLevel=0;}
        @Override public GameMode getGameMode(){return mode;}
        @Override public int getAbsX(){return position.getX();}
        @Override public int getAbsY(){return position.getY();}
        @Override public int getHeight(){return position.getZ();}
        @Override public void closeDialogue(){dialogues=null;}
    }
    private final Map<Integer,int[]> original=new HashMap<>();
    @BeforeClass public static void data()throws Exception{
        BountyHunterAudit.prepare();Class.forName(LootKeys.class.getName());
        var output=java.nio.file.Path.of("../../.tools/loot-key-ui-assets");java.nio.file.Files.createDirectories(output);
        for(int id:new int[]{773,788,1226,1227,1235}){
            byte[] sprite=io.ruin.Server.fileStore.get(8).getFile(id,0);assertNotNull("Native loot-window sprite "+id,sprite);
            if("1".equals(System.getenv("FLAVORSCAPE_RENDER_LOOT_KEYS")))java.nio.file.Files.write(output.resolve("sprite-"+id+".dat"),sprite);
        }
    }
    @Before public void values(){
        for(int id:new int[]{11802,4151,385}){
            ItemDef def=ItemDef.get(id);original.put(id,new int[]{def.value,def.protectValue,def.tradeable?1:0,def.consumable?1:0});
            def.value=def.protectValue=id==11802?1000:id==4151?500:100;def.tradeable=true;
        }
        ItemDef.get(385).consumable=true;
    }
    @After public void restore(){original.forEach((id,values)->{ItemDef def=ItemDef.get(id);def.value=values[0];def.protectValue=values[1];def.tradeable=values[2]==1;def.consumable=values[3]==1;});}
    private static Item key(Hunter p,Item... loot){p.inventory.add(LootKeys.create(Arrays.asList(loot)));return p.inventory.findFirst(LootKeys.KEY_ID);}

    @Test public void definitionsExposeBothSkullyActionsAndSeparateKeyAndChest(){
        assertEquals("Skully",NPCDef.get(LootKeys.NPC_ID).name);
        assertEquals("Talk-to",NPCDef.get(LootKeys.NPC_ID).options[0]);assertEquals("Value",NPCDef.get(LootKeys.NPC_ID).options[1]);
        assertNotNull(NPCDef.get(LootKeys.NPC_ID).defaultActions[0]);assertNotNull(NPCDef.get(LootKeys.NPC_ID).defaultActions[1]);
        assertEquals("Loot chest",ObjectDef.get(LootKeys.CHEST_ID).name);assertNotNull(ObjectDef.get(LootKeys.CHEST_ID).defaultActions[0]);
        ItemDef def=ItemDef.get(LootKeys.KEY_ID);assertFalse(def.stackable);assertFalse(def.tradeable);assertTrue(def.neverProtect);assertFalse(def.free);
        assertEquals("Check-value",def.inventoryOptions[0]);assertEquals("Destroy",def.inventoryOptions[4]);
    }
    @Test public void disabledKeysLeaveOrdinaryDropsAlone(){
        Hunter p=new Hunter();List<Item> ground=new ArrayList<>();LootKeys.Capture capture=new LootKeys.Capture(p,ground::add);
        assertFalse(capture.collect(new Item(11802)));capture.finish();assertEquals(0,LootKeys.count(p));assertTrue(ground.isEmpty());
    }
    @Test public void capturePersistsItemAmountsAttributesAndValue(){
        Hunter p=new Hunter();LootKeys.settings(p).active=true;LootKeys.Capture capture=new LootKeys.Capture(p,item->fail("Unexpected ground drop"));
        assertTrue(capture.collect(new Item(11802,1,Map.of("charges","123"))));assertTrue(capture.collect(new Item(4151,2)));capture.finish();
        Item key=p.inventory.findFirst(LootKeys.KEY_ID);assertEquals(2000,LootKeys.value(key));
        var json=new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();Item reloaded=json.fromJson(json.toJson(key),Item.class);
        assertEquals(2000,LootKeys.value(reloaded));assertEquals(2,LootKeys.read(reloaded).rows.size());
        assertTrue(LootKeys.read(reloaded).rows.stream().anyMatch(row->row.item.getAttributeString("charges","").equals("123")));
    }
    @Test public void settingsKeepConsumablesAndExcludedValuesOnTheGround(){
        Hunter p=new Hunter();var settings=LootKeys.settings(p);settings.active=true;settings.dropConsumables=true;settings.dropAbove=500;settings.dropBelow=100;
        LootKeys.Capture capture=new LootKeys.Capture(p,item->fail("Unexpected drop"));
        assertFalse(capture.collect(new Item(385)));assertFalse(capture.collect(new Item(11802)));
        assertFalse(capture.collect(new Item(13307,50)));assertTrue(capture.collect(new Item(4151)));
        capture.finish();assertEquals(500,LootKeys.value(p.inventory.findFirst(LootKeys.KEY_ID)));
    }
    @Test public void fiveKeyLimitFallsBackToOrdinaryLoot(){
        Hunter p=new Hunter();LootKeys.settings(p).active=true;for(int i=0;i<5;i++)key(p,new Item(11802));
        assertEquals(5,LootKeys.count(p));LootKeys.Capture capture=new LootKeys.Capture(p,item->fail("Unexpected drop"));
        assertFalse(capture.collect(new Item(11802)));capture.finish();assertEquals(5,LootKeys.count(p));
    }
    @Test public void fullInventoryDropsTheGeneratedKeyWithItsContents(){
        Hunter p=new Hunter();p.inventory.add(4151,28);LootKeys.settings(p).active=true;List<Item> ground=new ArrayList<>();
        LootKeys.Capture capture=new LootKeys.Capture(p,ground::add);assertTrue(capture.collect(new Item(11802)));capture.finish();
        assertEquals(1,ground.size());assertTrue(LootKeys.isKey(ground.get(0)));assertEquals(1000,LootKeys.value(ground.get(0)));assertEquals(0,LootKeys.count(p));
    }
    @Test public void keyKeepsTheTwentyEightMostValuableEntriesAndDropsOverflow(){
        Hunter p=new Hunter();LootKeys.settings(p).active=true;List<Item> ground=new ArrayList<>();LootKeys.Capture capture=new LootKeys.Capture(p,ground::add);
        for(int amount=1;amount<=30;amount++)assertTrue(capture.collect(new Item(11802,amount)));capture.finish();
        assertEquals(28,LootKeys.read(p.inventory.findFirst(LootKeys.KEY_ID)).rows.size());assertEquals(2,ground.size());
        assertEquals(3,ground.stream().mapToInt(Item::getAmount).sum());
    }
    @Test public void completedClaimConsumesTheKeyAndCountsValueExactlyOnce(){
        Hunter p=new Hunter();Item key=key(p,new Item(11802,1,Map.of("charges","123")),new Item(4151,2));
        LootKeys.claim(p,key);assertEquals(0,LootKeys.count(p));assertEquals(1,p.inventory.findFirst(11802).getAmount());assertEquals(2,p.inventory.count(4151));
        assertEquals("123",p.inventory.findFirst(11802).getAttributeString("charges",""));
        assertEquals(1,p.lootKeysClaimed);assertEquals(2000,p.lootKeyValueClaimed);
        LootKeys.claim(p,key);assertEquals(1,p.lootKeysClaimed);assertEquals(2000,p.lootKeyValueClaimed);
        assertTrue(LootKeys.statistics(p).contains("2,000"));
    }
    @Test public void claimingCanUseTheSlotFreedByTheConsumedKey(){
        Hunter p=new Hunter();Item key=key(p,new Item(11802));p.inventory.add(4151,27);assertEquals(0,p.inventory.getFreeSlots());
        LootKeys.claim(p,key);assertEquals(1,p.inventory.count(11802));assertEquals(0,LootKeys.count(p));assertEquals(1,p.lootKeysClaimed);
    }
    @Test public void partialClaimsKeepRemainderAndNeverDuplicateOrLoseValue(){
        Hunter p=new Hunter();Item key=key(p,new Item(11802,2),new Item(13307,100));p.inventory.add(4151,26);
        LootKeys.claim(p,key);assertEquals(1,LootKeys.count(p));assertEquals(1,p.inventory.count(11802));assertEquals(0,p.lootKeysClaimed);assertEquals(1000,p.lootKeyValueClaimed);
        assertEquals(1100,LootKeys.value(key));p.inventory.remove(4151,2);LootKeys.claim(p,key);
        assertEquals(0,LootKeys.count(p));assertEquals(2,p.inventory.count(11802));assertEquals(100,p.inventory.count(13307));
        assertEquals(1,p.lootKeysClaimed);assertEquals(2100,p.lootKeyValueClaimed);
    }
    @Test public void staleRemoteAndCombatRequestsCannotClaim(){
        Hunter p=new Hunter();Item key=key(p,new Item(11802));p.position.set(3200,3200,0);LootKeys.claim(p,key);assertEquals(0,p.inventory.count(11802));
        p.position.set(LootKeys.CHEST_X,LootKeys.CHEST_Y,0);p.defending=true;LootKeys.claim(p,key);assertEquals(0,p.inventory.count(11802));
        p.defending=false;Item replacement=key.copy();key.remove();p.inventory.add(replacement);LootKeys.claim(p,key);assertEquals(0,p.inventory.count(11802));
    }
    @Test public void destroyTracksRemainingValueOnceAndIronmenCannotClaimLoot(){
        Hunter p=new Hunter();Item key=key(p,new Item(11802));p.mode=GameMode.IRONMAN;
        LootKeys.claim(p,key);assertEquals(0,p.inventory.count(11802));assertEquals(1,LootKeys.count(p));
        LootKeys.destroy(p,key);LootKeys.destroy(p,key);assertEquals(1000,p.lootKeyValueDestroyed);assertEquals(0,p.lootKeysClaimed);assertEquals(0,LootKeys.count(p));
    }
    @Test public void aStaleDestroyConfirmationCannotDestroyAChangedOrReplacedKey()throws Exception{
        Hunter p=new Hunter();Item key=key(p,new Item(11802));LootKeys.requestDestroy(p,key);
        var field=io.ruin.model.inter.dialogue.YesNoDialogue.class.getDeclaredField("yesAction");field.setAccessible(true);Runnable confirm=(Runnable)field.get(p.dialogues[0]);
        key.remove();key(p,new Item(4151));confirm.run();assertEquals(1,LootKeys.count(p));assertEquals(0,p.lootKeyValueDestroyed);
    }
    @Test public void keysTransferOnPvpDeathAndAreLostOnPvmDeath(){
        Hunter victim=new Hunter(),killer=new Hunter();LootKeys.settings(killer).active=true;key(victim,new Item(11802));
        LootKeys.onDeath(victim,killer);assertEquals(0,LootKeys.count(victim));assertEquals(1,LootKeys.count(killer));assertEquals(1000,LootKeys.value(killer.inventory.findFirst(LootKeys.KEY_ID)));
        LootKeys.onDeath(killer,null);assertEquals(0,LootKeys.count(killer));
    }
    @Test public void keysCannotBeBankedAndPreferencesSurviveSaving(){
        Hunter p=new Hunter();Item key=key(p,new Item(11802));
        assertEquals(0,new io.ruin.model.item.containers.bank.Bank().add(key.getId(),1,key.copyOfAttributes()));assertEquals(1,LootKeys.count(p));
        var settings=LootKeys.settings(p);settings.active=true;settings.dropConsumables=true;settings.dropAbove=5000;settings.dropBelow=100;
        var json=new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();var restored=json.fromJson(json.toJson(settings),LootKeys.Settings.class);
        assertTrue(restored.active);assertTrue(restored.dropConsumables);assertEquals(5000,restored.dropAbove);assertEquals(100,restored.dropBelow);
        p.lootKeysClaimed=758;p.lootKeyValueClaimed=17_156_065_968L;
        assertTrue(LootKeys.statistics(p).contains("758"));assertTrue(LootKeys.statistics(p).contains("17,156,065,968"));
    }
}
