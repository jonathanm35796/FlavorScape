package io.ruin.tools;

import io.ruin.Server;
import io.ruin.api.filestore.FileStore;
import io.ruin.cache.ItemDef;

public final class ItemCacheFixture {
    private ItemCacheFixture() {}
    public static synchronized void load() throws Exception {
        if (ItemDef.cached.isEmpty()) {
            Server.dataOnlyMode = true;
            io.ruin.model.World.type = io.ruin.api.protocol.world.WorldType.PVP;
            io.ruin.model.World.stage = io.ruin.api.protocol.world.WorldStage.DEV;
            Server.fileStore = new FileStore("../Cache");
            io.ruin.cache.Varpbit.load();
            io.ruin.cache.IdentityKit.load();
            io.ruin.cache.AnimDef.load();
            io.ruin.cache.GfxDef.load();
            io.ruin.cache.ScriptDef.load();
            io.ruin.cache.InterfaceDef.load();
            ItemDef.load();
            io.ruin.cache.NPCDef.load();
            io.ruin.cache.ObjectDef.load();
            io.ruin.model.map.MapListener.LISTENERS = new io.ruin.model.map.MapListener[0];
            // Equipment slots live in the server's data overlay, not the cache.
            String info = java.nio.file.Files.readString(java.nio.file.Path.of("Data/items/item_info.json"));
            for (com.google.gson.JsonElement entry : new com.google.gson.JsonParser().parse(info).getAsJsonArray()) {
                com.google.gson.JsonObject item = entry.getAsJsonObject();
                if (item.has("equip_slot")) ItemDef.get(item.get("id").getAsInt()).equipSlot = item.get("equip_slot").getAsInt();
            }
        }
    }
}
