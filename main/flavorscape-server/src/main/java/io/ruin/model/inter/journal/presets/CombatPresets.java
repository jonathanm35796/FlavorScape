package io.ruin.model.inter.journal.presets;

import io.ruin.model.entity.player.Player;
import io.ruin.model.entity.shared.listeners.LoginListener;
import io.ruin.model.item.Item;
import io.ruin.model.item.containers.Equipment;
import io.ruin.model.skills.magic.SpellBook;

/** Server-owned loadouts; preview messages use a reserved string-packet namespace. */
public final class CombatPresets extends Preset {
    public static final int MAX_PRESETS = 12;
    public static final Preset[] BUILT_INS = {
        new CombatPresets("Hybrid (max)", 0), new CombatPresets("Tribrid (max)", 1),
        new CombatPresets("Pure (52 Prayer)", 2), new CombatPresets("Pure hybrid (52 Prayer)", 3),
        new CombatPresets("Max melee", 4), new CombatPresets("Max range", 5)
    };

    private CombatPresets(String title, int style) {
        free = true;
        name = title;
        boolean pure = style == 2 || style == 3;
        attack = level(pure ? 75 : 99);
        strength = ranged = magic = hitpoints = level(99);
        defence = level(pure ? 1 : 99);
        prayer = level(pure ? 52 : 99);
        spellBook = style < 2 || style == 3 ? SpellBook.ANCIENT : style == 4 ? SpellBook.LUNAR : SpellBook.MODERN;
        java.util.Arrays.setAll(invItems, i -> new Item(SHARK));
        int[] supplies = {12695, 2444, 6685, 6685, 3024, 3024, 3144, 3144};
        for (int i = 0; i < supplies.length; i++) invItems[i] = new Item(supplies[i]);
        invItems[27] = new Item(TELEPORT_HOME, 10);
        equipItems[Equipment.SLOT_AMULET] = new Item(pure ? 6585 : 19553);
        equipItems[Equipment.SLOT_RING] = new Item(pure ? 2550 : 11773);
        equipItems[Equipment.SLOT_HANDS] = new Item(pure ? 7458 : 7462);
        equipItems[Equipment.SLOT_FEET] = new Item(pure ? 3105 : 13239);
        equipItems[Equipment.SLOT_CAPE] = new Item(pure ? 6570 : 21295);
        equipItems[Equipment.SLOT_HAT] = new Item(pure ? 6109 : 10828);
        equipItems[Equipment.SLOT_CHEST] = new Item(pure ? 6107 : 11832);
        equipItems[Equipment.SLOT_LEGS] = new Item(pure ? 6108 : 11834);
        equipItems[Equipment.SLOT_WEAPON] = new Item(pure ? 4587 : 12006);
        equipItems[Equipment.SLOT_SHIELD] = new Item(pure ? 3842 : 12954);
        invItems[8] = new Item(pure ? 5698 : 11802);
        if (style < 2 || style == 3) {
            equipItems[Equipment.SLOT_WEAPON] = new Item(4675);
            equipItems[Equipment.SLOT_CHEST] = new Item(pure ? 6107 : 4712);
            equipItems[Equipment.SLOT_LEGS] = new Item(pure ? 6108 : 4714);
            equipItems[Equipment.SLOT_CAPE] = new Item(pure ? 2414 : 21795);
            equipItems[Equipment.SLOT_AMULET] = new Item(pure ? 6585 : 12002);
            equipItems[Equipment.SLOT_FEET] = new Item(pure ? 3105 : 13235);
            invItems[9] = new Item(pure ? 4587 : 12006);
            invItems[10] = new Item(pure ? 2497 : 11834);
            invItems[11] = new Item(pure ? 1129 : 11832);
            invItems[24] = new Item(565, 1200);
            invItems[25] = new Item(560, 2400);
            invItems[26] = new Item(555, 3600);
        }
        if (style == 1 || style == 3) {
            invItems[12] = new Item(pure ? 9185 : 11785);
            invItems[13] = new Item(pure ? 2497 : 11830);
            invItems[14] = new Item(pure ? 1129 : 11828);
            invItems[15] = new Item(pure ? 10499 : 22109);
            equipItems[Equipment.SLOT_AMMO] = new Item(9244, 100);
        }
        if (style == 4) {
            invItems[24] = new Item(9075, 1000);
            invItems[25] = new Item(557, 5000);
            invItems[26] = new Item(560, 2000);
        }
        if (style == 5) {
            equipItems[Equipment.SLOT_HAT] = new Item(11826);
            equipItems[Equipment.SLOT_CHEST] = new Item(11828);
            equipItems[Equipment.SLOT_LEGS] = new Item(11830);
            equipItems[Equipment.SLOT_WEAPON] = new Item(11785);
            equipItems[Equipment.SLOT_SHIELD] = new Item(12817);
            equipItems[Equipment.SLOT_AMULET] = new Item(19547);
            equipItems[Equipment.SLOT_FEET] = new Item(13237);
            equipItems[Equipment.SLOT_CAPE] = new Item(22109);
            equipItems[Equipment.SLOT_RING] = new Item(11771);
            equipItems[Equipment.SLOT_AMMO] = new Item(9244, 100);
        }
    }

    public static void load(Player player, int index) {
        PresetCustom.check(player);
        if (index < 0 || index >= MAX_PRESETS) {
            player.sendMessage("Choose a gear preset from 1 to 12.");
            return;
        }
        Preset preset = player.customPresets[index];
        if (preset == null) player.sendMessage("Preset " + (index + 1) + " is empty. Save your setup in that slot first.");
        else preset.selectFinish(player);
    }

    public static void sync(Player player) {
        PresetCustom.check(player);
        player.getPacketSender().sendString(32766, 0, "FS_GEARS_BEGIN");
        int row = 1;
        for (int i = 0; i < MAX_PRESETS; i++) {
            Preset preset = player.customPresets[i];
            player.getPacketSender().sendString(32766, row++, preset == null
                    ? "Empty slot|-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1|Save your current setup|" : preset.preview());
        }
        player.getPacketSender().sendString(32766, 18, "FS_GEARS_END");
    }

    static { LoginListener.register(CombatPresets::sync); }
}
