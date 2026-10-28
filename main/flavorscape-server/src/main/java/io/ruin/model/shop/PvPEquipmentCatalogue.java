package io.ruin.model.shop;

import com.google.gson.*;
import io.ruin.Server;
import io.ruin.cache.ItemDef;
import io.ruin.model.activities.wilderness.BountyHunterEquipment;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/** Explicit market stock, with PvP equipment kept separate from ordinary items. */
public final class PvPEquipmentCatalogue {
    private PvPEquipmentCatalogue() {}

    public static List<PvPMarket.Entry> load() {
        return load("pvp-equipment.json",true);
    }

    public static List<PvPMarket.Entry> loadGeneralMarket() {
        return load("general-market.json",false);
    }

    private static List<PvPMarket.Entry> load(String filename,boolean bounty) {
        File directory=Server.dataFolder==null?new File("data"):Server.dataFolder;
        File file=new File(directory,"market/"+filename);
        try(Reader reader=Files.newBufferedReader(file.toPath(),StandardCharsets.UTF_8)) {
            JsonArray rows=new JsonParser().parse(reader).getAsJsonArray();
            Map<Integer,PvPMarket.Entry> result=new LinkedHashMap<>();
            for(JsonElement element:rows) {
                JsonObject row=element.getAsJsonObject();
                int id=row.get("id").getAsInt(), category=row.get("category").getAsInt();
                ItemDef def=ItemDef.get(id);
                String name=row.get("name").getAsString();
                if(def==null||def.isNote()||def.isPlaceholder()||!def.name.equalsIgnoreCase(name)
                        ||category<0||category>=PvPMarket.CATEGORIES.length)
                    throw new IllegalArgumentException("Invalid PvP Equipment identity: "+id+" / "+name);
                if(BountyHunterEquipment.isRestricted(id)!=bounty)
                    throw new IllegalArgumentException("Item in wrong market catalogue: "+id+" / "+filename);
                PvPMarket.Entry entry=new PvPMarket.Entry(id,row.get("price").getAsLong(),category,bounty);
                if(result.put(id,entry)!=null)throw new IllegalArgumentException("Duplicate PvP Equipment item: "+id);
            }
            if(result.isEmpty())throw new IllegalArgumentException("PvP Equipment catalogue is empty");
            return new ArrayList<>(result.values());
        } catch(IOException error) {
            throw new IllegalStateException("Cannot load PvP Equipment catalogue: "+file,error);
        }
    }
}
