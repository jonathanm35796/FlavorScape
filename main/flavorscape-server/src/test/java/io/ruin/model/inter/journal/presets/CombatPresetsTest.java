package io.ruin.model.inter.journal.presets;

import org.junit.Test;
import static org.junit.Assert.*;

public class CombatPresetsTest {
    private static class Account extends io.ruin.model.combat.CombatCalculationTest.Fighter {
        final io.ruin.model.item.containers.Inventory inventory = new io.ruin.model.item.containers.Inventory();
        final io.ruin.model.item.actions.impl.storage.RunePouch pouch = new io.ruin.model.item.actions.impl.storage.RunePouch();
        Account(){inventory.init(this,28,-1,-1,-1,false);pouch.init(this,3,-1,-1,-1,false);varps=new int[5000];}
        final java.util.Map<Integer,String> packets = new java.util.LinkedHashMap<>();
        final java.util.List<String> messages = new java.util.ArrayList<>();
        final io.ruin.network.PacketSender sender = new io.ruin.network.PacketSender(this,null) {
            @Override public void sendString(int group,int row,String value){if(group==32766)packets.put(row,value);}
        };
        @Override public io.ruin.network.PacketSender getPacketSender(){return sender;}
        @Override public void sendMessage(String message){messages.add(message);}
        @Override public io.ruin.model.item.containers.Inventory getInventory(){return inventory;}
        @Override public io.ruin.model.item.actions.impl.storage.RunePouch getRunePouch(){return pouch;}
        @Override public io.ruin.model.entity.player.GameMode getGameMode(){return io.ruin.model.entity.player.GameMode.STANDARD;}
    }
    @org.junit.BeforeClass public static void cache() throws Exception { io.ruin.tools.ItemCacheFixture.load(); }
    @Test public void builtInsHaveCompleteIndependentGearAndExpectedLevels() {
        assertEquals(6, CombatPresets.BUILT_INS.length);
        for (int i = 0; i < 6; i++) {
            Preset preset = CombatPresets.BUILT_INS[i];
            assertEquals(28, preset.invItems.length);
            assertEquals(14, preset.equipItems.length);
            for (io.ruin.model.item.Item item : preset.invItems) {
                assertNotNull(item);
                assertNotNull("Missing inventory item " + item.getId(), item.getDef());
            }
            for (io.ruin.model.item.Item item : preset.equipItems)
                if (item != null) assertNotNull("Missing equipment item " + item.getId(), item.getDef());
            assertEquals(i == 2 || i == 3 ? 1 : 99, preset.defence[0]);
            assertEquals(i == 2 || i == 3 ? 52 : 99, preset.prayer[0]);
            assertEquals(4, preset.preview().split("\\|", -1).length);
            assertNotNull(preset.spellBook);
            assertTrue(preset.free);
        }
        assertNotSame(CombatPresets.BUILT_INS[0].invItems, CombatPresets.BUILT_INS[1].invItems);
    }
    @Test public void everyAccountHasTwelveSlotsWithIndependentEditableDefaults() {
        Account first = new Account(), second = new Account();
        PresetCustom.check(first); PresetCustom.check(second);
        assertEquals(12, first.customPresets.length);
        assertEquals(12, PresetCustom.getEntries().length);
        for(int i=0;i<6;i++) {
            assertEquals(CombatPresets.BUILT_INS[i].preview(),first.customPresets[i].preview());
            assertTrue(PresetCustom.canLoad(first,first.customPresets[i]));
            assertNotSame(first.customPresets[i],second.customPresets[i]);
            assertNotSame(first.customPresets[i].invItems[0],second.customPresets[i].invItems[0]);
        }
        first.customPresets[0].invItems[0].setId(385);
        first.customPresets[0].defence[0]=42;
        assertEquals(12695,second.customPresets[0].invItems[0].getId());
        assertEquals(99,second.customPresets[0].defence[0]);
        for(int i=6;i<12;i++)assertNull(first.customPresets[i]);
    }
    @Test public void migrationPreservesSavedSlotsAndDoesNotRestoreDeletedDefaults() {
        Account account = new Account();
        PresetCustom saved = new PresetCustom(1); saved.name="My setup";
        account.customPresets = new PresetCustom[9]; account.customPresets[0]=saved;
        PresetCustom.check(account);
        assertSame(saved,account.customPresets[0]);
        assertNotNull(account.customPresets[1]);
        account.customPresets[1]=null;
        PresetCustom.check(account);
        assertNull(account.customPresets[1]);
        assertTrue(account.editablePresetsInitialized);
    }
    @Test public void twelfthSlotIsAvailableWithoutRankAndStalePresetsCannotLoad() {
        Account account = new Account(); PresetCustom.check(account);
        PresetCustom saved = new PresetCustom(12); account.customPresets[11]=saved;
        assertTrue(PresetCustom.canLoad(account,saved));
        account.customPresets[11]=null;
        assertFalse(PresetCustom.canLoad(account,saved));
        CombatPresets.load(account,11);
        assertTrue(account.messages.get(0).contains("12 is empty"));
        CombatPresets.load(account,12);
        assertTrue(account.messages.get(1).contains("1 to 12"));
    }
    @Test public void syncPublishesExactlyTwelveUnifiedRows() {
        Account account = new Account(); CombatPresets.sync(account);
        assertEquals(14,account.packets.size());
        for(int row=1;row<=12;row++)assertNotNull(account.packets.get(row));
        assertTrue(account.packets.get(6).startsWith("Max range|"));
        assertTrue(account.packets.get(7).startsWith("Empty slot|"));
        assertEquals("FS_GEARS_END",account.packets.get(18));
    }
    @Test public void accountRoundTripRetainsDefaultBehaviorAndDeletedSlots() {
        Account account = new Account(); PresetCustom.check(account);
        account.customPresets[1]=null;
        com.google.gson.Gson gson=new com.google.gson.GsonBuilder().excludeFieldsWithoutExposeAnnotation()
            .setExclusionStrategies(new com.google.gson.ExclusionStrategy(){
                public boolean shouldSkipClass(Class<?> type){return false;}
                public boolean shouldSkipField(com.google.gson.FieldAttributes field){
                    return field.getDeclaringClass()==io.ruin.model.entity.player.PlayerAttributes.class
                        && !field.getName().equals("customPresets") && !field.getName().equals("editablePresetsInitialized");
                }
            }).create();
        io.ruin.model.entity.player.Player saved=gson.fromJson(gson.toJson(account),io.ruin.model.entity.player.Player.class);
        PresetCustom.check(saved);
        assertTrue(saved.editablePresetsInitialized);
        assertTrue(saved.customPresets[0].free);
        assertNull(saved.customPresets[1]);
        assertEquals(account.customPresets[0].preview(),saved.customPresets[0].preview());
    }
    @Test public void overwritingDefaultCapturesSetupWithoutKeepingFreeItemPrivileges() throws Exception {
        Account account=new Account(); PresetCustom.check(account);
        account.inventory.set(0,new io.ruin.model.item.Item(4151));
        PresetCustom preset=account.customPresets[0];
        java.lang.reflect.Method overwrite=PresetCustom.class.getDeclaredMethod("overwrite",io.ruin.model.entity.player.Player.class);
        overwrite.setAccessible(true);overwrite.invoke(preset,account);
        assertFalse(preset.free);
        assertEquals(4151,preset.invItems[0].getId());
        assertNotSame(account.inventory.get(0),preset.invItems[0]);
        assertEquals(99,preset.defence[0]);
        assertNotNull(preset.activePrayers);assertNotNull(preset.quickPrayers);
        assertEquals(12695,CombatPresets.BUILT_INS[0].invItems[0].getId());
    }
}
