package io.ruin.tools;

import com.google.gson.*;
import io.ruin.cache.ItemDef;
import java.nio.file.*;
import java.util.*;

/** Read-only, exhaustive comparison of the server equipment overlay with Wiki data. */
public final class CombatItemAudit {
    private static final String[] FIELDS = {"stab_attack_bonus", "slash_attack_bonus", "crush_attack_bonus",
            "magic_attack_bonus", "range_attack_bonus", "stab_defence_bonus", "slash_defence_bonus",
            "crush_defence_bonus", "magic_defence_bonus", "range_defence_bonus", "melee_strength_bonus",
            "ranged_strength_bonus", "magic_damage_bonus", "prayer_bonus"};
    private static final String[] KEYS = {"stab", "slash", "crush", "magic", "ranged"};

    public static void main(String[] args) throws Exception {
        ItemCacheFixture.load();
        io.ruin.model.combat.special.Special.load();
        Path directory = Path.of(args[0]);
        JsonParser parser = new JsonParser();
        JsonArray wiki = parser.parse(Files.readString(directory.resolve("wiki-equipment.json"))).getAsJsonArray();
        Map<Integer, List<JsonObject>> byId = new HashMap<>();
        for(JsonElement row : wiki) {
            JsonObject item = row.getAsJsonObject();
            byId.computeIfAbsent(item.get("id").getAsInt(), k -> new ArrayList<>()).add(item);
        }
        JsonArray local = parser.parse(Files.readString(Path.of("data/items/item_info.json"))).getAsJsonArray();
        Map<Integer, JsonObject> effective = new LinkedHashMap<>();
        List<String> duplicates = new ArrayList<>();
        duplicates.add("id\tnote");
        for(JsonElement row : local) {
            JsonObject item = row.getAsJsonObject();
            int id = item.get("id").getAsInt();
            if(effective.put(id, item) != null)
                duplicates.add(id + "\tLater overlay entry replaces earlier definition at runtime");
        }
        List<String> rows = new ArrayList<>();
        rows.add("id\tname\tstatus\tdifferences\tspecial_handler\tspecial_energy\twiki_url");
        List<String> changes = new ArrayList<>();
        changes.add("id\tname\tfield\tserver\twiki");
        Map<String, Integer> totals = new TreeMap<>();
        for(JsonObject item : effective.values()) {
            if(!item.has("equip_slot") || item.get("equip_slot").getAsInt() < 0) continue;
            int id = item.get("id").getAsInt();
            ItemDef def = ItemDef.get(id);
            String name = def == null ? "MISSING CACHE DEFINITION" : def.name;
            List<JsonObject> candidates = byId.getOrDefault(id, Collections.emptyList());
            JsonObject match = null;
            for(JsonObject candidate : candidates) {
                if(sameIdentity(name, candidate) && sameSlot(item.get("equip_slot").getAsInt(), candidate)) {
                    if(match != null && !Arrays.equals(bonuses(match), bonuses(candidate))) {
                        match = null;
                        break;
                    }
                    match = candidate;
                }
            }
            String status;
            List<String> differences = new ArrayList<>();
            if(match == null) {
                status = candidates.isEmpty() ? "NO_WIKI_ID" : "IDENTITY_OR_VARIANT_REVIEW";
                for(JsonObject candidate : candidates)
                    differences.add("Wiki identity: " + candidate.get("name").getAsString() + " / " + candidate.get("version").getAsString());
            } else {
                double[] reference = bonuses(match);
                for(int i = 0; i < FIELDS.length; i++) {
                    double value = item.has(FIELDS[i]) ? item.get(FIELDS[i]).getAsDouble() : 0;
                    if(value != reference[i]) {
                        differences.add(FIELDS[i] + ": " + value + " -> " + reference[i]);
                        changes.add(id + "\t" + name + "\t" + FIELDS[i] + "\t" + value + "\t" + reference[i]);
                    }
                }
                status = differences.isEmpty() ? "BONUSES_MATCH" : "BONUSES_DIFFER";
            }
            totals.merge(status, 1, Integer::sum);
            rows.add(id + "\t" + name.replace('\t', ' ') + "\t" + status + "\t" + String.join("; ", differences)
                    + "\t" + (def.special == null ? "" : def.special.getClass().getSimpleName())
                    + "\t" + (def.special == null ? "" : def.special.getDrainAmount())
                    + "\thttps://oldschool.runescape.wiki/w/" + java.net.URLEncoder.encode(name.replace(' ', '_'), "UTF-8"));
        }
        Files.write(directory.resolve("equipment-audit.tsv"), rows);
        Files.write(directory.resolve("bonus-differences.tsv"), changes);
        Files.write(directory.resolve("duplicate-item-definitions.tsv"), duplicates);
        String summary = "Equipment rows: " + (rows.size() - 1) + "\n" + totals + "\n"
                + "Compared 14 static bonuses, exact ID + normalized cache name. Passive effects, special attacks, requirements, speed and NPC stats are separate checks.\n";
        Files.writeString(directory.resolve("equipment-summary.txt"), summary);
        System.out.println(summary);
        // Cache loading starts shared background executors in this standalone audit JVM.
        System.exit(0);
    }

    private static String normalize(String name) {
        return name.toLowerCase(Locale.ROOT).replace(" ", "").replace("’", "'").replace("vambraces", "vamb");
    }

    private static boolean sameIdentity(String name, JsonObject reference) {
        String local = normalize(name), canonical = normalize(reference.get("name").getAsString());
        if(local.equals(canonical)) return true;
        String withoutCharges = local.replaceAll("\\([0-9]+\\)", "").replaceAll("[0-9]+$", "");
        if(withoutCharges.equals(canonical)) return true;
        String version = normalize(reference.get("version").getAsString());
        if(version.startsWith("poison"))
            return local.equals(canonical + "(" + version.replace("poison", "p") + ")");
        if(version.matches("[0-9]+"))
            return local.equals(canonical + version) || local.equals(canonical + "(" + version + ")");
        if(version.equals("lit") && local.equals(canonical + "(lit)")) return true;
        if(version.equals("karambwanpoison") && local.equals(canonical + "(kp)")) return true;
        // Wiki disambiguates identically named cache items with descriptive suffixes.
        String described = canonical.replaceAll("\\((?!(?:i|e|ei|or|t|l|cr|bh)\\))[^)]*\\)", "");
        if(local.equals(described)) return true;
        return (canonical.equals("priestgown(top)") || canonical.equals("priestgown(bottom)"))
                && local.equals("priestgown");
    }

    private static boolean sameSlot(int slot, JsonObject reference) {
        String[] names = {"head", "cape", "neck", "weapon", "body", "shield", "", "legs", "", "hands", "feet", "", "ring", "ammo"};
        return slot >= 0 && slot < names.length && names[slot].equals(reference.get("slot").getAsString());
    }

    private static double[] bonuses(JsonObject item) {
        double[] values = new double[14];
        for(int i = 0; i < 5; i++) {
            values[i] = item.getAsJsonObject("offensive").get(KEYS[i]).getAsDouble();
            values[5 + i] = item.getAsJsonObject("defensive").get(KEYS[i]).getAsDouble();
        }
        String[] other = {"str", "ranged_str", "magic_str", "prayer"};
        for(int i = 0; i < 4; i++) values[10 + i] = item.getAsJsonObject("bonuses").get(other[i]).getAsDouble();
        // Wiki calculator stores magic strength in tenths of one percent.
        values[12] /= 10;
        return values;
    }
}
