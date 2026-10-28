package io.ruin.model.activities.wilderness;

import com.google.gson.Gson;
import io.ruin.cache.ItemDef;
import io.ruin.model.entity.player.Player;
import io.ruin.model.inter.Interface;
import io.ruin.model.inter.InterfaceHandler;
import io.ruin.model.inter.InterfaceType;
import io.ruin.model.inter.dialogue.YesNoDialogue;
import io.ruin.model.item.Item;
import java.util.*;

/** Revision-checked UI requests never supply item IDs, amounts, prices or key contents. */
public final class LootKeyWindow {
    public static final int HOST=Interface.BARROWS_LOOT, PROTOCOL=32764;
    private static final Gson JSON=new Gson();
    private LootKeyWindow() {}
    public static final class Session {
        public final String token=UUID.randomUUID().toString();
        public long revision;
        public boolean noted;
        public Item selected;
        public String contents;
        Session(Item selected){this.selected=selected;}
    }
    public static void open(Player p,Item key){
        if(!LootKeys.atChest(p)||!LootKeys.owned(p,key)||LootKeys.read(key)==null)return;
        p.closeInterface(InterfaceType.MAIN);
        p.openInterface(InterfaceType.MAIN,HOST);
        p.getPacketSender().setHidden(HOST,0,true);
        p.lootKeySession=new Session(key);snapshot(p);
    }
    public static void closed(Player p){
        if(p.lootKeySession==null)return;
        p.lootKeySession=null;p.getPacketSender().sendString(PROTOCOL,1,"CLOSE");
        p.getPacketSender().setHidden(HOST,0,false);
    }
    public static void snapshot(Player p){
        Session session=p.lootKeySession;if(session==null)return;
        List<Item> keys=new ArrayList<>();for(Item item:p.getInventory().getItems())if(LootKeys.isKey(item)&&LootKeys.read(item)!=null)keys.add(item);
        if(!keys.contains(session.selected))session.selected=keys.isEmpty()?null:keys.get(0);
        if(session.selected==null){p.closeInterface(InterfaceType.MAIN);closed(p);return;}
        LootKeys.Contents contents=LootKeys.read(session.selected);session.contents=LootKeys.contentsStamp(session.selected);
        Map<String,Object> data=new LinkedHashMap<>();
        data.put("token",session.token);data.put("revision",++session.revision);data.put("noted",session.noted);
        data.put("selected",contents.token);data.put("value",LootKeys.value(session.selected));
        data.put("bankUsed",p.getBank().getCount());data.put("bankCapacity",p.getBank().getItems().length);data.put("canClaim",!p.getGameMode().isIronMan());
        List<Object> keyRows=new ArrayList<>();for(Item key:keys){Map<String,Object> row=new LinkedHashMap<>();row.put("token",LootKeys.read(key).token);row.put("value",LootKeys.value(key));keyRows.add(row);}
        data.put("keys",keyRows);List<Object> items=new ArrayList<>();
        for(LootKeys.Row row:contents.rows){ItemDef def=row.item.getDef();if(def.isNote())def=def.fromNote();Map<String,Object> item=new LinkedHashMap<>();
            item.put("id",def.id);item.put("name",def.name);item.put("amount",row.item.getAmount());item.put("value",row.unitValue*row.item.getAmount());items.add(item);}
        data.put("items",items);p.getPacketSender().sendString(PROTOCOL,0,JSON.toJson(data));
    }
    public static void handle(Player p,String[] args){
        Session session=p.lootKeySession;if(session==null||args==null||args.length<3||!session.token.equals(args[0]))return;
        if(args[2].equals("close")){p.closeInterface(InterfaceType.MAIN);closed(p);return;}
        try{
            if(session.revision!=Long.parseLong(args[1]))return;
            String action=args[2];
            if(!LootKeys.atChest(p)||!p.isVisibleInterface(HOST)){p.closeInterface(InterfaceType.MAIN);closed(p);return;}
            if(!LootKeys.owned(p,session.selected)||!session.contents.equals(LootKeys.contentsStamp(session.selected))){snapshot(p);return;}
            switch(action){
                case "key":
                    if(args.length!=4)return;
                    for(Item key:p.getInventory().getItems())if(LootKeys.isKey(key)&&LootKeys.read(key)!=null&&LootKeys.read(key).token.equals(args[3])){session.selected=key;break;}
                    break;
                case "item":session.noted=false;break;
                case "note":session.noted=true;break;
                case "inventory":LootKeys.withdraw(p,session.selected,false,session.noted,-1,Integer.MAX_VALUE);break;
                case "bank":LootKeys.withdraw(p,session.selected,true,false,-1,Integer.MAX_VALUE);break;
                case "take":
                    if(args.length!=4)return;
                    int row=Integer.parseInt(args[3]);if(row<0)return;LootKeys.withdraw(p,session.selected,false,session.noted,row,1);break;
                case "trash":
                    Item selected=session.selected;String stamp=session.contents;long revision=session.revision;
                    p.dialogue(new YesNoDialogue("Destroy loot key?","Delete this key and all of its remaining loot permanently?",selected,()->{
                        if(p.lootKeySession==session&&session.revision==revision&&LootKeys.atChest(p)&&LootKeys.owned(p,selected)&&stamp.equals(LootKeys.contentsStamp(selected))){LootKeys.destroy(p,selected);snapshot(p);}
                    }));
                    return;
                default:return;
            }
            snapshot(p);
        }catch(NumberFormatException ignored){ }
    }
    static {InterfaceHandler.register(HOST,h->h.closedAction=(p,next)->closed(p));}
}
