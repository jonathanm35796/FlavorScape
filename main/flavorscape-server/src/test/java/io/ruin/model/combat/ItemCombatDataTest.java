package io.ruin.model.combat;

import io.ruin.cache.ItemDef;
import io.ruin.data.impl.items.*;
import java.nio.file.*;
import org.junit.Test;
import static org.junit.Assert.*;

public class ItemCombatDataTest {
    @Test public void productionItemLoaderReadsCorrectedBonusesAndDecimalMagicDamage() throws Exception {
        io.ruin.tools.ItemCacheFixture.load();
        new weapon_types().fromJson("weapon_types.json", Files.readString(Path.of("data/items/weapon_types.json")));
        new shield_types().fromJson("shield_types.json", Files.readString(Path.of("data/items/shield_types.json")));
        new item_info().fromJson("item_info.json", Files.readString(Path.of("data/items/item_info.json")));
        assertEquals(30, ItemDef.get(12926).equipBonuses[4]);
        assertEquals(20, ItemDef.get(12926).equipBonuses[11]);
        assertEquals(35, ItemDef.get(11230).equipBonuses[11]);
        assertEquals(5, ItemDef.get(12002).magicDamageBonus, 0);
        assertEquals(.2, ItemDef.get(6731).magicDamageBonus, .000001);
        assertEquals(.5, ItemDef.get(11770).magicDamageBonus, .000001);
        assertEquals(3, ItemDef.get(21298).equipBonuses[10]);
    }
}
