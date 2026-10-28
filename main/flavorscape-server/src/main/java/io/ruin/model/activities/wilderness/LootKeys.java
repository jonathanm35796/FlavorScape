package io.ruin.model.activities.wilderness;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import io.ruin.cache.ItemDef;
import io.ruin.model.entity.npc.NPCAction;
import io.ruin.model.entity.player.Player;
import io.ruin.model.inter.dialogue.NPCDialogue;
import io.ruin.model.inter.dialogue.OptionsDialogue;
import io.ruin.model.inter.dialogue.YesNoDialogue;
import io.ruin.model.inter.utils.Option;
import io.ruin.model.item.Item;
import io.ruin.model.item.actions.ItemAction;
import io.ruin.model.item.actions.ItemObjectAction;
import io.ruin.model.item.containers.Inventory;
import io.ruin.model.map.ground.GroundItem;
import io.ruin.model.map.object.actions.ObjectAction;
import java.util.*;
import java.util.function.Consumer;

/** Loot is carried in each key's saved attributes, including its original item attributes. */
public final class LootKeys {
    public static final int NPC_ID=15000, CHEST_ID=50005, KEY_ID=30330, MAX_KEYS=5;
    public static final int NPC_X=3099,NPC_Y=3510,CHEST_X=3095,CHEST_Y=3506;
    private static final String CONTENTS="loot_key_contents";
    private static final Gson JSON=new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
    private LootKeys() {}

    public static final class Settings {
        @Expose public boolean active,dropConsumables;
        @Expose public long dropAbove,dropBelow;
    }
    public static final class Row {
        @Expose public Item item;
        @Expose public long unitValue;
        Row(Item item){this.item=item.copy();unitValue=unitValue(item);}
    }
    public static final class Contents {
        @Expose public String token=UUID.randomUUID().toString();
        @Expose public List<Row> rows=new ArrayList<>();
    }

    public static Settings settings(Player p){if(p.lootKeySettings==null)p.lootKeySettings=new Settings();return p.lootKeySettings;}
    public static boolean isKey(Item item){return item!=null&&item.getId()==KEY_ID;}
    public static int count(Player p){int count=0;for(Item item:p.getInventory().getItems())if(isKey(item))count++;return count;}
    private static boolean near(Player p,int x,int y){return p.getHeight()==0&&Math.abs(p.getAbsX()-x)<=2&&Math.abs(p.getAbsY()-y)<=2;}
    static boolean idle(Player p){return !p.isLocked()&&!p.getCombat().isDead()&&!p.getCombat().isDefending(17)&&!p.getCombat().isAttacking(17);}
    static boolean owned(Player p,Item key){return isKey(key)&&key.getAmount()==1&&key.getContainer()==p.getInventory()&&p.getInventory().getSafe(key.getSlot())==key;}
    static boolean atChest(Player p){return near(p,CHEST_X,CHEST_Y)&&idle(p);}
    static String contentsStamp(Item key){return key.getAttributeString(CONTENTS,"");}
    private static long addValue(long first,long second){return first>Long.MAX_VALUE-second?Long.MAX_VALUE:first+second;}
    public static long unitValue(Item item){
        ItemDef def=item.getDef();if(def.isNote())def=def.fromNote();
        return def.id==13307?1L:Math.max(0,Math.max(def.protectValue,def.value));
    }
    private static long rowValue(Row row){return row.unitValue>Long.MAX_VALUE/row.item.getAmount()?Long.MAX_VALUE:row.unitValue*row.item.getAmount();}
    private static long value(Contents contents){long sum=0;for(Row row:contents.rows)sum=addValue(sum,rowValue(row));return sum;}
    public static long value(Item key){Contents contents=read(key);return contents==null?0:value(contents);}
    public static Contents read(Item key){
        if(!isKey(key))return null;
        try{
            Contents contents=JSON.fromJson(key.getAttributeString(CONTENTS,""),Contents.class);
            if(contents==null||contents.token==null||contents.rows==null||contents.rows.isEmpty()||contents.rows.size()>28)return null;
            for(Row row:contents.rows)if(row==null||row.item==null||row.item.getAmount()<=0||row.item.getDef()==null||isKey(row.item)||row.unitValue<0||row.unitValue>Integer.MAX_VALUE)return null;
            return contents;
        }catch(RuntimeException invalid){return null;}
    }
    private static void write(Item key,Contents contents){key.putAttribute(CONTENTS,JSON.toJson(contents));}
    public static Item create(List<Item> items){
        Contents contents=new Contents();
        for(Item item:items){if(item==null||item.getAmount()<=0||isKey(item))throw new IllegalArgumentException("Invalid key content");contents.rows.add(new Row(item));}
        if(contents.rows.isEmpty()||contents.rows.size()>28)throw new IllegalArgumentException("A key holds 1-28 item entries");
        Item key=new Item(KEY_ID);write(key,contents);return key;
    }

    public static final class Capture {
        private final Player killer;
        private final Consumer<Item> ground;
        private final List<Item> items=new ArrayList<>();
        public Capture(Player killer,Consumer<Item> ground){this.killer=killer;this.ground=ground;}
        public boolean collect(Item item){
            if(killer==null||!settings(killer).active||killer.getCombat().isDead()||count(killer)>=MAX_KEYS||!item.getDef().tradeable||isKey(item))return false;
            Settings settings=settings(killer);long value=rowValue(new Row(item));
            if(settings.dropConsumables&&(item.getDef().consumable||item.getDef().potion!=null))return false;
            if(settings.dropAbove>0&&value>settings.dropAbove||settings.dropBelow>0&&value<settings.dropBelow)return false;
            items.add(item.copy());return true;
        }
        public void finish(){
            if(items.isEmpty())return;
            items.sort(Comparator.comparingLong((Item item)->rowValue(new Row(item))).reversed());
            if(killer.getCombat().isDead()||count(killer)>=MAX_KEYS){items.forEach(ground);items.clear();return;}
            Item key=create(items.subList(0,Math.min(28,items.size())));
            if(killer.getInventory().add(key)!=1)ground.accept(key);
            for(int i=28;i<items.size();i++)ground.accept(items.get(i));
            items.clear();killer.sendMessage("Your opponent's loot has been stored in a loot key.");
        }
    }

    /** Keys cannot be protected or recovered through safe death storage. */
    public static void onDeath(Player victim,Player killer){
        for(Item item:victim.getInventory().getItems().clone())if(isKey(item)){
            Item copy=item.copy();item.remove();
            if(killer!=null&&killer!=victim&&settings(killer).active&&!killer.getCombat().isDead()&&count(killer)<MAX_KEYS&&read(copy)!=null){
                if(killer.getInventory().add(copy)!=1)new GroundItem(copy).owner(killer).position(victim.getPosition()).spawnPrivate();
            }
        }
    }

    public static void openChest(Player p){
        if(!near(p,CHEST_X,CHEST_Y)||!idle(p))return;
        for(Item key:p.getInventory().getItems())if(isKey(key)&&read(key)!=null){openKey(p,key);return;}
        p.dialogue(new NPCDialogue(NPC_ID,"You don't have any loot keys to open."));
    }
    public static void openKey(Player p,Item key){
        if(!near(p,CHEST_X,CHEST_Y)||!idle(p)||!owned(p,key))return;
        Contents contents=read(key);
        if(contents==null){p.sendMessage("This key contains no valid loot.");return;}
        LootKeyWindow.open(p,key);
    }

    private static void viewContents(Player p,Item key,int page){
        if(!near(p,CHEST_X,CHEST_Y)||!idle(p)||!owned(p,key))return;
        Contents contents=read(key);if(contents==null)return;
        int pages=(contents.rows.size()+4)/5;if(page<0||page>=pages)return;
        StringJoiner names=new StringJoiner("<br>");
        for(int i=page*5;i<Math.min(contents.rows.size(),page*5+5);i++){
            Row row=contents.rows.get(i);names.add(format(row.item.getAmount())+" x "+row.item.getDef().name);
        }
        List<Option> options=new ArrayList<>();
        if(page>0)options.add(new Option("Previous page",()->viewContents(p,key,page-1)));
        if(page+1<pages)options.add(new Option("Next page",()->viewContents(p,key,page+1)));
        options.add(new Option("Back to key",()->openKey(p,key)));
        p.dialogue(new io.ruin.model.inter.dialogue.MessageDialogue(names.toString()),new OptionsDialogue("Contents: page "+(page+1)+"/"+pages,options));
    }

    public static void claim(Player p,Item key){
        withdraw(p,key,false,false,-1,Integer.MAX_VALUE);
    }
    private static Item output(Row row,boolean bank,boolean noted,int amount){
        ItemDef base=row.item.getDef();if(base.isNote())base=base.fromNote();
        int id=!bank&&noted&&!row.item.hasAttributes()&&base.notedId!=-1?base.notedId:base.id;
        return new Item(id,amount,row.item.copyOfAttributes());
    }
    private static int addInventory(Inventory inventory,Item item){
        if(item.getDef().stackable&&!item.hasAttributes()){
            Item existing=inventory.findItem(item.getId());
            if(existing!=null)item.setAmount((int)Math.min(item.getAmount(),Integer.MAX_VALUE-(long)existing.getAmount()));
        }
        return item.getAmount()<=0?0:inventory.add(item);
    }
    public static void withdraw(Player p,Item key,boolean bank,boolean noted,int selectedRow,int requested){
        if(!atChest(p)||!owned(p,key)||p.getGameMode().isIronMan()||requested<=0)return;
        Contents contents=read(key);if(contents==null)return;
        if(selectedRow>=contents.rows.size())return;
        boolean freesKey=!bank&&(selectedRow<0||contents.rows.size()==1&&requested>=contents.rows.get(0).item.getAmount());
        if(freesKey){
            Inventory preview=new Inventory();preview.init(p,28,-1,-1,-1,false);
            for(Item item:p.getInventory().getItems())if(item!=null&&item!=key)preview.add(item);
            for(Row row:contents.rows){Item item=output(row,false,noted,row.item.getAmount());int requestedAmount=item.getAmount();if(addInventory(preview,item)!=requestedAmount){freesKey=false;break;}}
        }
        if(freesKey)key.remove();
        long claimed=0;int index=0;
        for(Iterator<Row> it=contents.rows.iterator();it.hasNext();){
            Row row=it.next();if(selectedRow>=0&&selectedRow!=index++){continue;}
            Item item=output(row,bank,noted,Math.min(requested,row.item.getAmount()));
            int moved;
            if(bank){
                if(!item.hasAttributes()){
                    Item existing=p.getBank().findItem(item.getId());
                    if(existing!=null)item.setAmount((int)Math.min(item.getAmount(),Integer.MAX_VALUE-(long)existing.getAmount()));
                }
                moved=item.getAmount()<=0?0:Math.max(0,p.getBank().add(item.getId(),item.getAmount(),item.copyOfAttributes()));
            }else moved=addInventory(p.getInventory(),item);
            if(freesKey&&moved!=row.item.getAmount())throw new IllegalStateException("Loot-key claim capacity changed");
            if(moved>0){claimed=addValue(claimed,row.unitValue*moved);row.item.setAmount(row.item.getAmount()-moved);}
            if(row.item.getAmount()==0)it.remove();
        }
        p.lootKeyValueClaimed=addValue(p.lootKeyValueClaimed,claimed);
        if(contents.rows.isEmpty()){
            if(!freesKey)key.remove();p.lootKeysClaimed=addValue(p.lootKeysClaimed,1);p.sendMessage("You have claimed all the loot in this key.");
        }else{write(key,contents);if(selectedRow<0)p.sendMessage("Make more "+(bank?"bank":"inventory")+" space to claim the remaining loot. Your key has been kept.");}
        p.closeDialogue();
    }

    public static void requestDestroy(Player p,Item key){
        if(!owned(p,key)||!idle(p)||p.wildernessLevel>0)return;
        String original=key.getAttributeString(CONTENTS,"");
        p.dialogue(new YesNoDialogue("Destroy loot key?","Permanently destroy the remaining loot worth "+format(value(key))+"?",key,()->{
            if(original.equals(key.getAttributeString(CONTENTS,"")))destroy(p,key);
        }));
    }
    public static void destroy(Player p,Item key){
        if(!owned(p,key)||!idle(p)||p.wildernessLevel>0)return;
        Contents contents=read(key);
        if(contents!=null)p.lootKeyValueDestroyed=addValue(p.lootKeyValueDestroyed,value(contents));
        key.remove();p.closeDialogue();
        p.sendMessage("You destroy the loot key and its remaining contents.");
    }
    private static String format(long value){return String.format(Locale.ROOT,"%,d",value);}
    public static String statistics(Player p){return "You've claimed <col=0000ff>"+format(p.lootKeysClaimed)+"</col> keys, containing about<br><col=0000ff>"
            +format(p.lootKeyValueClaimed)+"</col> Blood Money in value, and you've destroyed<br><col=0000ff>"+format(p.lootKeyValueDestroyed)+"</col> in value.";}
    private static String state(boolean active){return active?"<col=00ff00>Active</col>":"<col=ff0000>Inactive</col>";}
    public static void configure(Player p){
        if(!near(p,NPC_X,NPC_Y)||!idle(p))return;
        Settings settings=settings(p);
        p.dialogue(new OptionsDialogue("Adjust lootkey settings",
                new Option("Loot keys: "+state(settings.active),()->{if(near(p,NPC_X,NPC_Y)&&idle(p))settings.active=!settings.active;configure(p);}),
                new Option("Drop consumables on ground: "+state(settings.dropConsumables),()->{if(near(p,NPC_X,NPC_Y)&&idle(p))settings.dropConsumables=!settings.dropConsumables;configure(p);}),
                new Option("Drop item if value exceeds: "+(settings.dropAbove==0?state(false):format(settings.dropAbove)),()->threshold(p,true)),
                new Option("Drop item when value is under: "+(settings.dropBelow==0?state(false):format(settings.dropBelow)),()->threshold(p,false))));
    }
    private static void threshold(Player p,boolean above){
        if(!near(p,NPC_X,NPC_Y)||!idle(p))return;
        p.integerInput("Enter value threshold (0 disables this setting)",amount->{
            if(amount<0||!near(p,NPC_X,NPC_Y)||!idle(p))return;
            if(above)settings(p).dropAbove=amount;else settings(p).dropBelow=amount;configure(p);
        });
    }
    static {
        NPCAction.register(NPC_ID,"Talk-to",(p,npc)->configure(p));
        NPCAction.register(NPC_ID,"Value",(p,npc)->p.dialogue(new NPCDialogue(NPC_ID,statistics(p))));
        ObjectAction.register(CHEST_ID,"Loot",(p,obj)->openChest(p));
        ItemObjectAction.register(KEY_ID,CHEST_ID,(p,key,obj)->openKey(p,key));
        ItemAction.registerInventory(KEY_ID,"Check-value",(p,key)->p.sendMessage("This loot key contains "+format(value(key))+" Blood Money in loot value."));
        ItemAction.registerInventory(KEY_ID,"Destroy",LootKeys::requestDestroy);
    }
}
