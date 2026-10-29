package io.ruin.model.inter.handlers;

import io.ruin.api.utils.NumberUtils;
import io.ruin.cache.Color;
import io.ruin.cache.ItemDef;
import io.ruin.model.entity.player.Player;
import io.ruin.model.entity.player.PlayerAction;
import io.ruin.model.inter.Interface;
import io.ruin.model.inter.InterfaceHandler;
import io.ruin.model.inter.InterfaceType;
import io.ruin.model.inter.actions.OptionAction;
import io.ruin.model.inter.actions.SimpleAction;
import io.ruin.model.inter.dialogue.MessageDialogue;
import io.ruin.model.inter.utils.Config;
import io.ruin.model.item.Item;
import io.ruin.model.map.Bounds;
import io.ruin.model.stat.Stat;
import io.ruin.model.stat.StatType;

public class TabStats {

    static {
        InterfaceHandler.register(Interface.SKILLS, h -> {
            h.actions[1] = (OptionAction) (player, option) -> handleStat(player, StatType.Attack, option);
            h.actions[2] = (OptionAction) (player, option) -> handleStat(player, StatType.Strength, option);
            h.actions[3] = (OptionAction) (player, option) -> handleStat(player, StatType.Defence, option);
            h.actions[4] = (OptionAction) (player, option) -> handleStat(player, StatType.Ranged, option);
            h.actions[5] = (OptionAction) (player, option) -> handleStat(player, StatType.Prayer, option);
            h.actions[6] = (OptionAction) (player, option) -> handleStat(player, StatType.Magic, option);
            h.actions[7] = (OptionAction) (player, option) -> handleStat(player, StatType.Runecrafting, option);
            h.actions[8] = (OptionAction) (player, option) -> handleStat(player, StatType.Construction, option);
            h.actions[9] = (OptionAction) (player, option) -> handleStat(player, StatType.Hitpoints, option);
            h.actions[10] = (OptionAction) (player, option) -> handleStat(player, StatType.Agility, option);
            h.actions[11] = (OptionAction) (player, option) -> handleStat(player, StatType.Herblore, option);
            h.actions[12] = (OptionAction) (player, option) -> handleStat(player, StatType.Thieving, option);
            h.actions[13] = (OptionAction) (player, option) -> handleStat(player, StatType.Crafting, option);
            h.actions[14] = (OptionAction) (player, option) -> handleStat(player, StatType.Fletching, option);
            h.actions[15] = (OptionAction) (player, option) -> handleStat(player, StatType.Slayer, option);
            h.actions[16] = (OptionAction) (player, option) -> handleStat(player, StatType.Hunter, option);
            h.actions[17] = (OptionAction) (player, option) -> handleStat(player, StatType.Mining, option);
            h.actions[18] = (OptionAction) (player, option) -> handleStat(player, StatType.Smithing, option);
            h.actions[19] = (OptionAction) (player, option) -> handleStat(player, StatType.Fishing, option);
            h.actions[20] = (OptionAction) (player, option) -> handleStat(player, StatType.Cooking, option);
            h.actions[21] = (OptionAction) (player, option) -> handleStat(player, StatType.Firemaking, option);
            h.actions[22] = (OptionAction) (player, option) -> handleStat(player, StatType.Woodcutting, option);
            h.actions[23] = (OptionAction) (player, option) -> handleStat(player, StatType.Farming, option);
        });

        InterfaceHandler.register(Interface.SKILL_GUIDE, h -> {
            h.actions[11] = (SimpleAction) p -> selectCategory(p, 0);
            h.actions[12] = (SimpleAction) p -> selectCategory(p, 1);
            h.actions[13] = (SimpleAction) p -> selectCategory(p, 2);
            h.actions[14] = (SimpleAction) p -> selectCategory(p, 3);
            h.actions[15] = (SimpleAction) p -> selectCategory(p, 4);
            h.actions[16] = (SimpleAction) p -> selectCategory(p, 5);
            h.actions[17] = (SimpleAction) p -> selectCategory(p, 6);
            h.actions[18] = (SimpleAction) p -> selectCategory(p, 7);
            h.actions[19] = (SimpleAction) p -> selectCategory(p, 8);
            h.actions[20] = (SimpleAction) p -> selectCategory(p, 9);
            h.actions[21] = (SimpleAction) p -> selectCategory(p, 10);
            h.actions[22] = (SimpleAction) p -> selectCategory(p, 11);
            h.actions[23] = (SimpleAction) p -> selectCategory(p, 12);
            h.actions[24] = (SimpleAction) p -> selectCategory(p, 13);
        });
    }

    private static void handleStat(Player player, StatType statType, int option) {
        if(statType.isCombat()&&option==1)setLevel(player,statType);
        else openGuide(player,statType,0);
    }

    private static void handleTotal(Player player, int option) {
        if(option == 1)
            player.forceText("!My " + Color.ORANGE.wrap("Total Level") + " is " + NumberUtils.formatNumber(player.getStats().totalLevel) + ".");
        else
            player.forceText("!My " + Color.ORANGE.wrap("Total XP") + " is " + NumberUtils.formatNumber(player.getStats().totalXp) + ".");
    }

    public static void openGuide(Player player, StatType statType, int category) {
        Config.SKILL_GUIDE_STAT.set(player, statType.clientId);
        Config.SKILL_GUIDE_CAT.set(player, category);
        player.getPacketSender().sendClientScript(917, "ii", 4600861, 80);
        player.openInterface(InterfaceType.MAIN, Interface.SKILL_GUIDE);
    }

    private static void selectCategory(Player player, int category) {
        Config.SKILL_GUIDE_CAT.set(player, category);
    }

    public static void setLevel(Player player, StatType statType) {
        io.ruin.model.stat.CombatLevelEditor.open(player,statType);
    }
}
