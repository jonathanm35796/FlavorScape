package io.ruin.model.inter;

import io.ruin.model.entity.player.Player;
import io.ruin.model.inter.utils.Config;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

public class ClassicSpellbookTest {
    @BeforeClass public static void cache() throws Exception {io.ruin.tools.ItemCacheFixture.load();}
    @Test public void newAccountsUseTheClassicGridWithoutHidingUnavailableSpells(){
        Player player=new Player();Config.load(player);
        assertEquals(1,Config.DISABLE_SPELL_FILTERING.get(player));
        for(int book=0;book<3;book++){
            Config.MAGIC_BOOK.set(player,book);
            assertEquals(1,Config.DISABLE_SPELL_FILTERING.get(player));
        }
    }
    @Test public void oldFilteredSettingsCannotRestoreTheRedistributedLayoutAtLogin(){
        Player player=new Player();
        player.savedConfigs.put((6718<<16)|1,0);
        Config.load(player);assertEquals(1,Config.DISABLE_SPELL_FILTERING.get(player));
        Config.DISABLE_SPELL_FILTERING.toggle(player);
        assertEquals(0,Config.DISABLE_SPELL_FILTERING.get(player));
        Config.save(player);Config.load(player);
        assertEquals(1,Config.DISABLE_SPELL_FILTERING.get(player));
    }
}
