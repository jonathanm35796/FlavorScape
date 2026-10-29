package net.runelite.standalone;

import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.List;

public final class LletyaLootKeyState {
    public static final class Key {public String token;public long value;}
    public static final class Item {public int id,amount;public long value;public String name;}
    public static final class Snapshot {
        public String token,selected;
        public long revision,value;
        public boolean noted,canClaim;
        public int bankUsed,bankCapacity;
        public List<Key> keys=new ArrayList<>();
        public List<Item> items=new ArrayList<>();
    }
    private static final Gson JSON=new Gson();
    private static volatile Snapshot snapshot;
    private static String dismissed;
    private LletyaLootKeyState() {}
    public static boolean receive(int hash,String value){
        if((hash>>>16)!=32764)return false;
        try{
            if((hash&65535)==1){snapshot=null;return true;}
            if((hash&65535)==0){Snapshot next=JSON.fromJson(value,Snapshot.class);
                if(next!=null&&next.token!=null&&next.selected!=null&&!next.token.equals(dismissed)&&next.items!=null&&next.keys!=null
                        &&next.items.size()<=28&&!next.keys.isEmpty()&&next.keys.size()<=5&&next.revision>0
                        &&next.items.stream().allMatch(item->item!=null&&item.id>0&&item.amount>0&&item.name!=null)
                        &&next.keys.stream().allMatch(key->key!=null&&key.token!=null)){
                    if(snapshot==null||!snapshot.token.equals(next.token)||next.revision>snapshot.revision)snapshot=next;
                }
            }
        }catch(RuntimeException invalid){ }
        return true;
    }
    public static Snapshot get(){return snapshot;}
    public static void dismiss(String token){if(snapshot!=null&&snapshot.token.equals(token)){dismissed=token;snapshot=null;}}
    public static void clear(){snapshot=null;dismissed=null;}
}
