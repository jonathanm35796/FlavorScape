package net.runelite.standalone;

import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.List;

/** Server-provided market data; client controls never supply prices. */
public final class LletyaMarketState {
    private static final Gson GSON=new Gson();
    public static final class Item { public int id,buyback,category; public long price; public String name; }
    public static final class Snapshot {
        public String token,query; public int category; public long balance; public boolean pvpOnly;
        public double bountyBalance;
        public boolean hasMarketSelector=true;
        public List<Item> items=new ArrayList<>();
    }
    private static Snapshot pending;
    private static volatile Snapshot snapshot;
    private static String dismissedToken;
    private LletyaMarketState() {}
    public static boolean receive(int hash,String text) {
        if((hash>>>16)!=32765)return false;
        try {
            switch(hash&65535) {
                case 0:pending=GSON.fromJson(text,Snapshot.class);if(pending!=null&&pending.token!=null&&pending.token.equals(dismissedToken))pending=null;if(pending!=null)pending.items=new ArrayList<>();break;
                case 1:if(pending!=null)pending.items.add(GSON.fromJson(text,Item.class));break;
                case 2:if(pending!=null){snapshot=pending;pending=null;}break;
                case 3:clear();break;
            }
        } catch(RuntimeException ignored) { pending=null; }
        return true;
    }
    public static Snapshot get(){return snapshot;}
    public static void dismiss(String token){
        if(snapshot!=null&&snapshot.token.equals(token)){dismissedToken=token;pending=null;snapshot=null;}
    }
    public static void clear(){pending=null;snapshot=null;dismissedToken=null;}
}
