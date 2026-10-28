package io.ruin.model.shop;

import com.google.gson.Gson;
import io.ruin.cache.ItemDef;
import io.ruin.cache.ItemID;
import io.ruin.model.entity.player.Player;
import io.ruin.model.inter.InterfaceType;
import io.ruin.model.item.Item;
import io.ruin.model.item.containers.Inventory;
import io.ruin.model.activities.wilderness.BountyHunterPoints;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** A fixed-price market. All prices, inventory changes and buybacks are server authoritative. */
public final class PvPMarket {
    public static final int NPC_ID = 9001, X = 3098, Y = 3506;
    public static final long BLOOD_MONEY_PER_PLATINUM_TOKEN = 1000L;
    public static final String[] CATEGORIES = {"Melee","Ranged","Magic","Miscellaneous","Untradeables","Barrows","Cosmetics","Resources","Tools"};
    private static final Gson JSON = new Gson();
    private PvPMarket() {}

    public static final class Entry {
        public final int id, category, buyback;
        public final long price;
        public final boolean bounty;
        public final String name;
        Entry(int id, long price, int category) {
            this(id,price,category,false);
        }
        Entry(int id, long price, int category, boolean bounty) {
            if(price<0||price>5_000_000_000L)throw new IllegalArgumentException("Unsupported market price: "+price);
            this.id=id; this.price=price; this.category=category; this.bounty=bounty;
            this.buyback=bounty?0:buybackPrice(price); this.name=ItemDef.get(id).name;
        }
    }
    public static int buybackPrice(long price) { return (int)(Math.max(0,price)*40/100); }

    public static List<Entry> catalogue() {
        return PvPEquipmentCatalogue.loadGeneralMarket();
    }

    static MarketShop createShop() {
        return new MarketShop(catalogue(),PvPEquipmentCatalogue.load());
    }

    public static boolean nearby(Player p) {
        return p.getHeight()==0&&Math.abs(p.getAbsX()-X)<=8&&Math.abs(p.getAbsY()-Y)<=10;
    }
    public static void open(Player p) {
        if(!nearby(p)||p.getCombat().isDefending(16)||p.isLocked())return;
        createShop().open(p);
    }
    public static void handle(Player p,String[] args) {
        if(!(p.viewingShop instanceof MarketShop)||!nearby(p))return;
        MarketShop market=(MarketShop)p.viewingShop;
        if(args.length<2||!market.token.equals(args[1]))return;
        try {
            switch(args[0]) {
                case "filter":
                    if(args.length!=5)return;
                    market.selected=Math.max(0,Math.min(CATEGORIES.length-1,Integer.parseInt(args[2])));
                    market.pvpOnly=!market.bountyEntries.isEmpty()
                            && (market.entries.isEmpty()||"1".equals(args[3]));
                    market.refreshStock();
                    market.replaceListeners();
                    market.query="~".equals(args[4])?"":new String(Base64.getUrlDecoder().decode(args[4]),StandardCharsets.UTF_8);
                    if(market.query.length()>60)market.query=market.query.substring(0,60);
                    market.snapshot(p);
                    break;
                case "value": if(args.length==3)market.showPrice(p,Integer.parseInt(args[2]));break;
                case "buyx": if(args.length==3)market.buyX(p,Integer.parseInt(args[2]));break;
                case "buy":
                    if(args.length==4){int amount=quantity(args[3]);if(amount==1||amount==5||amount==10)market.buyFromShop(p,new Item(Integer.parseInt(args[2]),amount));}
                    break;
                case "sell": if(args.length==4)market.sellToShop(p,new Item(Integer.parseInt(args[2]),quantity(args[3])));break;
                case "close":p.closeInterface(InterfaceType.MAIN);p.closeInterface(InterfaceType.INVENTORY);break;
            }
        } catch(IllegalArgumentException ignored) { /* Reject malformed UI requests. */ }
    }
    private static int quantity(String value) {int n=Integer.parseInt(value);return n>0?n:0;}

    public static final class MarketShop extends Shop {
        final String token=UUID.randomUUID().toString();
        final Map<Integer,Entry> entries=new LinkedHashMap<>();
        final Map<Integer,Entry> bountyEntries=new LinkedHashMap<>();
        int selected; boolean pvpOnly; String query="";
        public MarketShop(List<Entry> catalogue) {
            this(catalogue,Collections.emptyList());
        }
        public MarketShop(List<Entry> catalogue,List<Entry> equipment) {
            identifier="pvp-market";title="PvP Market";currency=Currency.BLOOD_MONEY;
            accessibleByIronMan=true;canSellToStore=true;defaultStock=new ArrayList<>();
            for(Entry entry:catalogue)(entry.bounty?bountyEntries:entries).put(entry.id,entry);
            for(Entry entry:equipment){
                if(!entry.bounty)throw new IllegalArgumentException("Equipment catalogue must use BH points");
                bountyEntries.put(entry.id,entry);
            }
            pvpOnly=entries.isEmpty()&&!bountyEntries.isEmpty();
            // The ordinary shop stays usable as a fallback when the client overlay is disabled.
            init();refreshStock();
        }
        Map<Integer,Entry> activeEntries(){return pvpOnly?bountyEntries:entries;}
        void refreshStock() {
            currency=pvpOnly?Currency.BOUNTY_HUNTER_POINTS:Currency.BLOOD_MONEY;
            currencyHandler=currency.getCurrencyHandler();
            title=pvpOnly?"PvP Equipment - Bounty Hunter points":"PvP Market";
            canSellToStore=!pvpOnly;
            defaultStock.clear();
            activeEntries().values().stream()
                    .filter(e->e.price<=Integer.MAX_VALUE)
                    .limit(ShopManager.SHOP_MAX_CAPACITY).forEach(e->defaultStock.add(new ShopItem(e.id,1000000,(int)e.price)));
            populate();
        }
        @Override public void open(Player p) {
            // Finish any previous shop's CLOSE before publishing this shop's data.
            p.closeInterface(InterfaceType.MAIN);
            p.closeInterface(InterfaceType.INVENTORY);
            // Publish the complete overlay before opening the native shop widgets. Otherwise
            // a frame can show the fallback shop while the catalogue packets are still arriving.
            snapshot(p);
            super.open(p);
        }
        @Override public void close(Player p) {super.close(p);p.getPacketSender().sendString(32765,3,"CLOSE");}
        boolean valid(Player p){return p.viewingShop==this&&nearby(p)&&!p.getCombat().isDefending(16)&&!p.isLocked();}
        long currencyBalance(Inventory inventory){
            return inventory.count(ItemID.BLOOD_MONEY)+(long)inventory.count(ItemID.PLATINUM_TOKEN)*BLOOD_MONEY_PER_PLATINUM_TOKEN;
        }
        Entry entry(int id){ItemDef def=ItemDef.get(id);return def==null?null:activeEntries().get(def.isNote()?def.fromNote().id:id);}
        void showPrice(Player p,int id){
            Entry e=activeEntries().get(id);if(!valid(p)||e==null)return;
            if(pvpOnly) {
                p.sendMessage(e.name+" costs "+e.price+" Bounty Hunter points each in PvP Equipment.");return;
            }
            p.sendMessage(e.name+" costs "+String.format(Locale.ROOT,"%,d",e.price)+" Blood Money each; platinum tokens are accepted at 1,000 Blood Money each.");
        }
        void buyX(Player p,int id){
            Entry e=activeEntries().get(id);if(!valid(p)||e==null)return;
            p.integerInput("How many "+e.name+" would you like to buy?",amount->{
                if(amount>0)buyFromShop(p,new Item(id,amount));
            });
        }
        @Override public int getBuyPrice(Item item){Entry e=entry(item.getId());return pvpOnly||e==null||e.buyback<=0?-1:e.buyback;}
        @Override public int getSellPrice(ShopItem item){Entry e=item==null?null:activeEntries().get(item.getId());return e==null||e.price>Integer.MAX_VALUE?Integer.MAX_VALUE:(int)e.price;}
        @Override public void buyFromShop(Player p,Item request) {
            if(!valid(p)||request.getAmount()<=0)return;
            Entry e=activeEntries().get(request.getId());if(e==null)return;
            if(pvpOnly) {
                long unitCost=(long)e.price*BountyHunterPoints.UNITS_PER_POINT;
                int amount=(int)Math.min(Math.min((long)request.getAmount(),unitCost==0?request.getAmount():p.bountyHunterPointUnits/unitCost),p.getInventory().getCapacityFor(e.id));
                if(amount<=0){p.sendMessage("You need enough Bounty Hunter points and inventory space to buy that.");return;}
                int added=p.getInventory().add(e.id,amount);
                p.bountyHunterPointUnits-=(long)added*unitCost;
                p.sendMessage("Bought "+added+" x "+e.name+" for "+((long)added*e.price)+" Bounty Hunter points.");
                snapshot(p);return;
            }
            Inventory inv=p.getInventory();
            int amount=(int)Math.min((long)request.getAmount(),e.price==0?request.getAmount():currencyBalance(inv)/e.price);
            int capacity=inv.getCapacityFor(e.id);
            if(capacity==0&&amount>0){
                long candidateCost=(long)amount*e.price;
                long blood=(long)inv.count(ItemID.BLOOD_MONEY);
                long tokens=inv.count(ItemID.PLATINUM_TOKEN);
                long shortfall=candidateCost-Math.min(blood,candidateCost);
                long tokenCount=(shortfall+BLOOD_MONEY_PER_PLATINUM_TOKEN-1)/BLOOD_MONEY_PER_PLATINUM_TOKEN;
                long change=tokenCount*BLOOD_MONEY_PER_PLATINUM_TOKEN-shortfall;
                boolean releasesCurrencySlot=(blood>0&&candidateCost>=blood)||(tokenCount>0&&tokenCount==tokens);
                if(change==0&&releasesCurrencySlot)
                    capacity=ItemDef.get(e.id).stackable?Integer.MAX_VALUE:1;
            }
            amount=Math.min(amount,capacity);
            if(amount<=0){p.sendMessage("You need enough Blood Money or platinum tokens and inventory space to buy that.");return;}
            long cost=(long)amount*e.price;
            int bloodMoneySpent=(int)Math.min((long)inv.count(ItemID.BLOOD_MONEY),cost);
            long tokenShortfall=cost-bloodMoneySpent;
            int tokensSpent=(int)((tokenShortfall+BLOOD_MONEY_PER_PLATINUM_TOKEN-1)/BLOOD_MONEY_PER_PLATINUM_TOKEN);
            long change=(long)tokensSpent*BLOOD_MONEY_PER_PLATINUM_TOKEN-tokenShortfall;
            if(inv.remove(ItemID.BLOOD_MONEY,bloodMoneySpent)!=bloodMoneySpent
                    ||inv.remove(ItemID.PLATINUM_TOKEN,tokensSpent)!=tokensSpent)
                throw new IllegalStateException("Market currency removal failed");
            if(change>0&&inv.add(ItemID.BLOOD_MONEY,(int)change)!=(int)change)
                throw new IllegalStateException("Market token change failed");
            int added=inv.add(e.id,amount);
            if(added<amount){
                long refund=(long)(amount-added)*e.price;
                int refundTokens=refund>Integer.MAX_VALUE?(int)(refund/BLOOD_MONEY_PER_PLATINUM_TOKEN):0;
                int refundBlood=(int)(refund-(long)refundTokens*BLOOD_MONEY_PER_PLATINUM_TOKEN);
                if(inv.add(ItemID.PLATINUM_TOKEN,refundTokens)!=refundTokens
                        ||inv.add(ItemID.BLOOD_MONEY,refundBlood)!=refundBlood)throw new IllegalStateException("Market purchase refund failed");
            }
            p.sendMessage("Bought "+added+" x "+e.name+" for "+((long)added*e.price)+" Blood Money equivalent.");
            snapshot(p);
        }
        @Override public void sellToShop(Player p,Item request) {
            if(!valid(p)||request.getAmount()<=0)return;
            Entry e=entry(request.getId());Inventory inv=p.getInventory();
            if(pvpOnly){p.sendMessage("PvP Equipment does not buy back items.");return;}
            if(e==null||e.buyback<=0){p.sendMessage("This market only buys back items in its catalogue.");return;}
            List<Item> owned=new ArrayList<>();long countLong=0;
            for(Item item:inv.getItems())if(item!=null&&item.getId()==request.getId()){owned.add(item);countLong+=item.getAmount();}
            int count=(int)Math.min(Integer.MAX_VALUE,countLong);
            int amount=Math.min(request.getAmount(),count);
            amount=(int)Math.min(amount,(Integer.MAX_VALUE-(long)inv.count(ItemID.BLOOD_MONEY))/e.buyback);
            if(amount<=0){p.sendMessage("There is no eligible item or room for the Blood Money payment.");return;}
            int planned=amount;boolean freesSlot=false;
            for(Item item:owned){int take=Math.min(planned,item.getAmount());if(take>0&&take==item.getAmount())freesSlot=true;planned-=take;}
            if(!inv.hasId(ItemID.BLOOD_MONEY)&&inv.getFreeSlots()==0&&!freesSlot){
                p.sendMessage("Make room for your Blood Money payment first.");return;
            }
            int removed=0;
            for(Item item:owned){int take=Math.min(amount-removed,item.getAmount());if(take>0){item.incrementAmount(-take);removed+=take;}}
            int payment=(int)((long)removed*e.buyback);
            if(inv.add(ItemID.BLOOD_MONEY,payment)!=payment)throw new IllegalStateException("Market buyback payment failed");
            p.sendMessage("Sold "+removed+" x "+e.name+" for "+payment+" Blood Money (40% buyback).");
            snapshot(p);
        }
        void snapshot(Player p) {
            Map<String,Object> header=new LinkedHashMap<>();header.put("token",token);header.put("category",selected);
            header.put("pvpOnly",pvpOnly);header.put("query",query);header.put("balance",currencyBalance(p.getInventory()));
            header.put("hasMarketSelector",!entries.isEmpty()&&!bountyEntries.isEmpty());
            header.put("bountyBalance",BountyHunterPoints.balance(p));
            p.getPacketSender().sendString(32765,0,JSON.toJson(header));
            String term=query.toLowerCase(Locale.ROOT);
            activeEntries().values().stream().filter(e->query.isEmpty()||e.name.toLowerCase(Locale.ROOT).contains(term))
                    .sorted(Comparator.comparingInt((Entry e)->e.category).thenComparing(e->e.name,String.CASE_INSENSITIVE_ORDER))
                    .forEach(e->p.getPacketSender().sendString(32765,1,JSON.toJson(e)));
            p.getPacketSender().sendString(32765,2,"END");
        }
    }
}
