package io.ruin.model.inter.journal.presets;

import io.ruin.cache.Color;
import io.ruin.model.entity.player.Player;
import io.ruin.model.inter.dialogue.MessageDialogue;
import io.ruin.model.inter.dialogue.OptionsDialogue;
import io.ruin.model.inter.utils.Config;
import io.ruin.model.inter.utils.Option;
import io.ruin.model.item.Item;
import io.ruin.model.skills.magic.SpellBook;
import io.ruin.model.stat.StatType;

public class PresetCustom extends Preset {

    public static final PresetCustom[] ENTRIES = java.util.stream.IntStream.rangeClosed(1, CombatPresets.MAX_PRESETS)
            .mapToObj(PresetCustom::new).toArray(PresetCustom[]::new);

    public static PresetCustom[] getEntries() {
        return ENTRIES;
    }

    private int id;

    public int slotIndex() { return id - 1; }

    public static void saveCurrent(Player player, int slot) {
        check(player);
        PresetCustom entry = java.util.Arrays.stream(getEntries()).filter(p -> p.slotIndex() == slot).findFirst().orElse(null);
        if (entry == null || slot >= player.customPresets.length) { player.sendMessage("Invalid saved gear slot."); return; }
        if (!entry.allowSelect(player)) return;
        Runnable save = () -> {
            if (!entry.allowSelect(player)) return;
            PresetCustom preset = player.customPresets[slot];
            if (preset == null) { preset = new PresetCustom(slot + 1); preset.name = "Custom preset " + (slot + 1); }
            preset.overwrite(player);
            player.customPresets[slot] = preset;
            CombatPresets.sync(player);
            player.closeDialogue();
            player.sendMessage("Saved inventory, equipment, spellbook and prayers to preset " + (slot + 1) + ".");
        };
        if (player.customPresets[slot] == null) save.run();
        else player.dialogue(new OptionsDialogue("Replace preset " + (slot + 1) + " with your current setup?",
                new Option("Save current setup", save), new Option("Cancel", player::closeDialogue)));
    }

    public static boolean canLoad(Player player, Preset preset) {
        for (int i = 0; i < Math.min(player.customPresets.length, CombatPresets.MAX_PRESETS); i++)
            if (player.customPresets[i] == preset) return true;
        return false;
    }

    public PresetCustom(int id) {
        this.id = id;
    }

    public static void check(Player player) {
        if (player.customPresets == null) player.customPresets = new PresetCustom[CombatPresets.MAX_PRESETS];
        else if (player.customPresets.length != CombatPresets.MAX_PRESETS)
            player.customPresets = java.util.Arrays.copyOf(player.customPresets, CombatPresets.MAX_PRESETS);
        if (!player.editablePresetsInitialized) {
            for (int i = 0; i < CombatPresets.BUILT_INS.length; i++) {
                if (player.customPresets[i] == null)
                    player.customPresets[i] = fromDefault(i + 1, CombatPresets.BUILT_INS[i]);
            }
            player.editablePresetsInitialized = true;
        }
    }

    private static PresetCustom fromDefault(int id, Preset source) {
        PresetCustom preset = new PresetCustom(id);
        preset.name = source.name;
        preset.free = source.free;
        preset.attack = source.attack.clone(); preset.strength = source.strength.clone();
        preset.defence = source.defence.clone(); preset.ranged = source.ranged.clone();
        preset.prayer = source.prayer.clone(); preset.magic = source.magic.clone();
        preset.hitpoints = source.hitpoints.clone(); preset.spellBook = source.spellBook;
        for (int i = 0; i < source.invItems.length; i++)
            preset.invItems[i] = source.invItems[i] == null ? null : source.invItems[i].copy();
        for (int i = 0; i < source.equipItems.length; i++)
            preset.equipItems[i] = source.equipItems[i] == null ? null : source.equipItems[i].copy();
        return preset;
    }

    @Override
    public void send(Player player) {
        PresetCustom preset = player.customPresets[id - 1];
        if(preset == null)
            sendEmpty(player);
        else
            sendCustom(player, preset);
    }

    private void sendEmpty(Player player) {
        CombatPresets.sync(player);
        send(player, "<img=" + (getImageId()) + "> Empty", Color.BRONZE);
    }

    private void sendCustom(Player player, PresetCustom preset) {
        CombatPresets.sync(player);
        if(preset.equipItems.length != 14) { //todo remove
            Item[] newItems = new Item[14];
            System.arraycopy(preset.equipItems, 0, newItems, 0, Math.min(14, preset.equipItems.length));
            preset.equipItems = newItems;
        }
        send(player, "<img=" + getImageId() + "> " + preset.name, Color.BRONZE);
    }

    private int getImageId() {
        return id + 77;
    }

    @Override
    public void selectStart(Player player) {
        check(player);
        PresetCustom preset = player.customPresets[id - 1];
        if(preset == null) {
            player.dialogue(
                    new MessageDialogue("Would you like to create a custom preset?"),
                    new OptionsDialogue(
                            new Option("Yes", () -> player.nameInput("Enter desired preset name:", name -> {
                                PresetCustom newPreset = new PresetCustom(id);
                                newPreset.name = name;
                                newPreset.overwrite(player);
                                player.customPresets[id - 1] = newPreset;
                                CombatPresets.sync(player);
                                player.dialogue(new MessageDialogue("Your preset has been created."));
                                sendCustom(player, newPreset);
                            })),
                            new Option("No")
                    )
            );
        } else {
            player.dialogue(
                    new OptionsDialogue(
                            "Preset " + id + ": " + preset.name,
                            new Option("Select Preset", () -> preset.selectFinish(player)),
                            new Option("Rename Preset", () -> player.nameInput("Enter desired preset name:", name -> {
                                preset.name = name;
                                player.dialogue(new MessageDialogue("Your preset name has been changed."));
                                sendCustom(player, preset);
                            })),
                            new Option("Overwrite Preset", () -> player.dialogue(
                                    new MessageDialogue("Are you sure you want to overwrite this preset?"),
                                    new OptionsDialogue(
                                            new Option("Yes", () -> {
                                                preset.overwrite(player);
                                                CombatPresets.sync(player);
                                                player.dialogue(new MessageDialogue("Your preset items, stats, and spellbook have been updated."));
                                            }),
                                            new Option("No")
                                    )
                            )),
                            new Option("More Options", () -> {
                                player.dialogue(
                                        new OptionsDialogue(
                                                new Option("Delete Preset", () -> player.dialogue(
                                                        new MessageDialogue("Are you sure you want to delete this preset?"),
                                                        new OptionsDialogue(
                                                                new Option("Yes", () -> {
                                                                    player.customPresets[id - 1] = null;
                                                                    player.dialogue(new MessageDialogue("Your preset has been deleted."));
                                                                    sendEmpty(player);
                                                                }),
                                                                new Option("No")
                                                        )
                                                )),
                                                new Option("Swap Preset", () -> player.integerInput("Enter the preset number to swap with:", num -> {
                                                    if(num < 1 || num > getEntries().length) {
                                                        player.dialogue(new MessageDialogue("Preset " + num + " does not exist."));
                                                        return;
                                                    }
                                                    PresetCustom swap = player.customPresets[num - 1];
                                                    player.customPresets[id - 1] = swap;
                                                    if(swap == null)
                                                        getEntries()[id - 1].sendEmpty(player);
                                                    else
                                                        getEntries()[id - 1].sendCustom(player, swap);
                                                    player.customPresets[num - 1] = preset;
                                                    getEntries()[num - 1].sendCustom(player, preset);
                                                    player.dialogue(new MessageDialogue("Presets " + id + " has been swapped with preset " + num + "."));
                                                }))
                                        )
                                );
                            })
                    )
            );
        }
    }

    private void overwrite(Player player) {
        // An edited default follows normal saved-gear ownership rules.
        free = false;
        if (!player.getGameMode().isIronMan()) {
            attack = level(player.getStats().get(StatType.Attack));
            strength = level(player.getStats().get(StatType.Strength));
            defence = level(player.getStats().get(StatType.Defence));
            ranged = level(player.getStats().get(StatType.Ranged));
            prayer = level(player.getStats().get(StatType.Prayer));
            magic = level(player.getStats().get(StatType.Magic));
            hitpoints = level(player.getStats().get(StatType.Hitpoints));
        } else {
            attack = strength = defence = ranged = prayer = magic = hitpoints = null;
        }
        spellBook = SpellBook.values()[Config.MAGIC_BOOK.get(player)];
        activePrayers = java.util.Arrays.stream(io.ruin.model.skills.prayer.Prayer.values())
                .filter(p -> player.getPrayer().isActive(p)).map(Enum::name).toArray(String[]::new);
        quickPrayers = player.getPrayer().copyQuickPrayers();
        copy(player.getInventory(), invItems);
        copy(player.getEquipment(), equipItems);
        runePouchItems = new Item[3];
        copy(player.getRunePouch(), runePouchItems);
        CombatPresets.sync(player);
    }

}
