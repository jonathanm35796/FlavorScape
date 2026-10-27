package io.ruin.tools;

import com.google.gson.*;
import io.ruin.Server;
import io.ruin.api.filestore.FileStore;
import io.ruin.cache.ObjectDef;
import io.ruin.model.World;
import io.ruin.model.map.Region;
import io.ruin.model.map.Tile;
import io.ruin.model.map.object.GameObject;
import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Offline map/collision inventory: no listener, player loading, or running-server mutation. */
public final class HomeMapAudit {
    public static void main(String[] args) {
        try {
            initialize(args[0], args[1]);
            Path output = Path.of(args[2]); Files.createDirectories(output);
            dump(output, "old-home", 1984, 3520, 2111, 3647);
            dump(output, "edgeville", 3040, 3430, 3160, 3519);
            try (PrintWriter defs = new PrintWriter(Files.newBufferedWriter(output.resolve("object-definitions.csv")))) {
                defs.println("id,name,width,height,clipType,options");
                for (ObjectDef def : ObjectDef.LOADED.values()) defs.println(def.id + "," + csv(def.name) + "," + def.xLength + "," + def.yLength + "," + def.clipType + "," + csv(Arrays.toString(def.options)));
            }
            System.out.println("HOME_MAP_AUDIT exported " + output);
            System.exit(0);
        } catch (Throwable error) { error.printStackTrace(); System.exit(1); }
    }
    public static void initialize(String cache, String data) throws Exception {
            Server.dataOnlyMode = true;
            Properties settings = new Properties();
            try (InputStream in = Files.newInputStream(Path.of("server.properties"))) { settings.load(in); }
            World.parse(settings);
            Server.fileStore = new FileStore(cache);
            Server.dataFolder = new File(data);
            ObjectDef.load();
            JsonObject keys = new JsonParser().parse(Files.readString(Path.of(data, "region_keys.json"))).getAsJsonObject();
            for (int id = 0; id < Region.LOADED.length; id++) {
                Region region = new Region(id);
                if (keys.has(Integer.toString(id))) region.keys = new Gson().fromJson(keys.get(Integer.toString(id)), int[].class);
                Region.LOADED[id] = region;
            }
            Set<Integer> regions = new TreeSet<>();
            addRegions(regions, 1984, 3520, 2111, 3647);
            addRegions(regions, 3040, 3430, 3160, 3519);
            for (int id : regions) Region.LOADED[id].init();
    }
    private static void addRegions(Set<Integer> ids, int x1, int y1, int x2, int y2) {
        for (int x = x1 >> 6; x <= x2 >> 6; x++) for (int y = y1 >> 6; y <= y2 >> 6; y++) ids.add((x << 8) | y);
    }
    private static String csv(String value) { return "\"" + value.replace("\"", "\"\"") + "\""; }
    private static void dump(Path output, String name, int x1, int y1, int x2, int y2) throws Exception {
        try (PrintWriter objects = new PrintWriter(Files.newBufferedWriter(output.resolve(name + "-objects.csv")));
             PrintWriter tiles = new PrintWriter(Files.newBufferedWriter(output.resolve(name + "-tiles.csv")))) {
            objects.println("id,name,x,y,z,type,direction,options");
            tiles.println("x,y,z,clipping,free");
            for (int z = 0; z < 4; z++) for (int x = x1; x <= x2; x++) for (int y = y1; y <= y2; y++) {
                Tile tile = Tile.get(x, y, z);
                tiles.println(x + "," + y + "," + z + "," + (tile == null ? 0 : tile.clipping) + "," + (tile == null || tile.isFloorFree()));
                if (tile == null || tile.gameObjects == null) continue;
                for (GameObject object : tile.gameObjects) {
                    ObjectDef def = ObjectDef.get(object.id);
                    if (def != null) objects.println(object.id + "," + csv(def.name) + "," + object.x + "," + object.y + "," + object.z + "," + object.type + "," + object.direction + "," + csv(Arrays.toString(def.options)));
                }
            }
        }
    }
}
