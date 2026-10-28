package io.ruin.model.inter.journal.presets;

import org.junit.Test;
import static org.junit.Assert.*;

public class CombatPresetsTest {
    private static class Account extends io.ruin.model.combat.CombatCalculationTest.Fighter {
        final io.ruin.model.item.containers.Inventory inventory = new io.ruin.model.item.containers.Inventory();
        final io.ruin.model.item.actions.impl.storage.RunePouch pouch = new io.ruin.model.item.actions.impl.storage.RunePouch();
        final io.ruin.model.item.containers.bank.Bank bank = new io.ruin.model.item.containers.bank.Bank();
        Account(){inventory.init(this,28,-1,-1,-1,false);pouch.init(this,3,-1,-1,-1,false);bank.init(this,800,-1,-1,-1,true);varps=new int[5000];}
        final java.util.Map<Integer,String> packets = new java.util.LinkedHashMap<>();
        final java.util.List<String> messages = new java.util.ArrayList<>();
        final io.ruin.network.PacketSender sender = new io.ruin.network.PacketSender(this,null) {
            @Override public void sendString(int group,int row,String value){if(group==32766)packets.put(row,value);}
        };
        @Override public io.ruin.network.PacketSender getPacketSender(){return sender;}
        @Override public void sendMessage(String message){messages.add(message);}
        @Override public io.ruin.model.item.containers.Inventory getInventory(){return inventory;}
        @Override public io.ruin.model.item.actions.impl.storage.RunePouch getRunePouch(){return pouch;}
        @Override public io.ruin.model.item.containers.bank.Bank getBank(){return bank;}
        @Override public io.ruin.model.entity.player.GameMode getGameMode(){return io.ruin.model.entity.player.GameMode.STANDARD;}
    }
    @org.junit.BeforeClass public static void cache() throws Exception { io.ruin.tools.ItemCacheFixture.load(); }
    @Test public void lootKeysCannotBeBankedOrClonedBySavedGear() throws Exception {
        Account account=new Account();
        io.ruin.model.item.Item key=io.ruin.model.activities.wilderness.LootKeys.create(java.util.List.of(new io.ruin.model.item.Item(11802)));
        account.inventory.add(key);assertFalse(new Preset().bankCurrentSetup(account));assertEquals(1,io.ruin.model.activities.wilderness.LootKeys.count(account));
        Preset preset=new Preset();preset.free=true;preset.invItems[0]=key.copy();account.inventory.clear();
        java.lang.reflect.Method withdraw=Preset.class.getDeclaredMethod("withdrawItems",io.ruin.model.entity.player.Player.class,boolean.class);withdraw.setAccessible(true);withdraw.invoke(preset,account,true);
        assertEquals(0,io.ruin.model.activities.wilderness.LootKeys.count(account));
    }
    @Test public void loadingGearBanksItemsOutsidePresetIncludingAttributesAndPouch() throws Exception {
        Account account = new Account();
        account.inventory.set(0, new io.ruin.model.item.Item(4151));
        account.inventory.set(1, new io.ruin.model.item.Item(565, 250,
                new java.util.HashMap<>(java.util.Map.of("CHARGES", "12"))));
        account.getEquipment().set(3, new io.ruin.model.item.Item(11802));
        account.pouch.set(0, new io.ruin.model.item.Item(560, 1200));
        Preset preset = new Preset();
        preset.free = true;
        preset.invItems[0] = new io.ruin.model.item.Item(385);
        assertTrue(preset.bankCurrentSetup(account));
        java.lang.reflect.Method withdraw = Preset.class.getDeclaredMethod("withdrawItems", io.ruin.model.entity.player.Player.class, boolean.class);
        withdraw.setAccessible(true);
        withdraw.invoke(preset, account, true);
        withdraw.invoke(preset, account, false);
        assertEquals(385, account.inventory.get(0).getId());
        assertEquals(1, account.bank.count(4151));
        assertEquals(1, account.bank.count(11802));
        assertEquals(1200, account.bank.count(560));
        io.ruin.model.item.Item attributed = account.bank.findItem(565, java.util.Map.of("CHARGES", "12"));
        assertEquals(250, attributed.getAmount());
        assertEquals(12, attributed.getAttributeInt("CHARGES", 0));
        assertTrue(account.getEquipment().isEmpty());
        assertTrue(account.pouch.isEmpty());
    }
    @Test public void fullBankStopsPresetBeforeMovingAnyCurrentItems() {
        Account account = new Account();
        account.bank.init(account, 1, -1, -1, -1, true);
        account.bank.add(385, 1);
        account.inventory.set(0, new io.ruin.model.item.Item(4151));
        account.getEquipment().set(3, new io.ruin.model.item.Item(11802));
        account.pouch.set(0, new io.ruin.model.item.Item(560, 1200));
        assertFalse(new Preset().bankCurrentSetup(account));
        assertEquals(4151, account.inventory.get(0).getId());
        assertEquals(11802, account.getEquipment().get(3).getId());
        assertEquals(1200, account.pouch.get(0).getAmount());
        assertEquals(1, account.bank.count(385));
    }
    @Test public void overflowingBankStackStopsPresetWithoutLosingRunes() {
        Account account = new Account();
        account.bank.add(560, Integer.MAX_VALUE);
        account.inventory.set(0, new io.ruin.model.item.Item(560, 20));
        assertFalse(new Preset().bankCurrentSetup(account));
        assertEquals(20, account.inventory.get(0).getAmount());
        assertEquals(Integer.MAX_VALUE, account.bank.count(560));
    }
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
