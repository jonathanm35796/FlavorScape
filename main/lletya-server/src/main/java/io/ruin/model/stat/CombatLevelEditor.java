package io.ruin.model.stat;

import io.ruin.cache.ItemDef;
import io.ruin.model.entity.player.Player;
import io.ruin.model.entity.player.PlayerAction;
import io.ruin.model.inter.dialogue.OptionsDialogue;
import io.ruin.model.inter.utils.Option;
import io.ruin.model.item.Item;

/** Free player build selection, shared by skill buttons, commands and the reset NPC. */
public final class CombatLevelEditor {
    private CombatLevelEditor(){}
    public static void open(Player player,StatType type){
        if(type==null||!type.isCombat()){player.sendMessage("Only combat skills can be set freely.");return;}
        if(!allowed(player))return;
        player.integerInput("Set "+type.name()+" level ("+(type==StatType.Hitpoints?10:1)+"-99):",
                level->set(player,type,level));
    }
    public static void menu(Player player){
        if(!allowed(player))return;
        player.dialogue(new OptionsDialogue("Set combat levels - free",
                new Option("Attack",()->open(player,StatType.Attack)),
                new Option("Strength",()->open(player,StatType.Strength)),
                new Option("Defence",()->open(player,StatType.Defence)),
                new Option("Ranged",()->open(player,StatType.Ranged)),
                new Option("More...",()->player.dialogue(new OptionsDialogue("Set combat levels - free",
                        new Option("Prayer",()->open(player,StatType.Prayer)),
                        new Option("Magic",()->open(player,StatType.Magic)),
                        new Option("Hitpoints",()->open(player,StatType.Hitpoints)),
                        new Option("Back",()->menu(player)))))));
    }
    public static boolean set(Player player,StatType type,int level){
        if(type==null||!type.isCombat()){player.sendMessage("Only combat skills can be set freely.");return false;}
        int minimum=type==StatType.Hitpoints?10:1;
        if(level<minimum||level>99){player.sendMessage("Choose a "+type.name()+" level from "+minimum+" to 99.");return false;}
        // Recheck on submission: opening a dialogue does not authorise a later change in combat.
        if(!allowed(player))return false;
        for(Item item:player.getEquipment().getItems()){
            if(item==null)continue;
            ItemDef definition=item.getDef();
            if(definition.equipReqs==null)continue;
            for(int requirement:definition.equipReqs)
                if((requirement>>>8)==type.ordinal()&&level<(requirement&255)){
                    player.sendMessage("Unequip your "+definition.name+" before lowering "+type.name()+" to "+level+".");return false;
                }
        }
        boolean[] quick=player.getPrayer().copyQuickPrayers();
        player.getPrayer().deactivateAll();
        player.getStats().get(type).set(level);
        player.getPrayer().restoreQuickPrayers(quick);
        player.getEquipment().recalculateBonuses();
        player.getCombat().updateLevel();
        player.getCombat().checkLevel();
        player.sendMessage("Your "+type.name()+" level is now "+level+". You can change combat levels for free from the Skills tab.");
        return true;
    }
    private static boolean allowed(Player player){
        if(player.isLocked()||player.getCombat().isDead()||player.getCombat().isAttacking(16)||player.getCombat().isDefending(16)){
            player.sendMessage("Finish combat and wait 10 seconds before changing combat levels.");return false;
        }
        PlayerAction action=player.getAction(1);
        if(player.wildernessLevel>0||player.pvpAttackZone||action==PlayerAction.ATTACK||action==PlayerAction.FIGHT
                ||(player.getBountyHunter()!=null&&player.getBountyHunter().target!=null)){
            player.sendMessage("Leave the PvP area and finish your Bounty Hunter target before changing combat levels.");return false;
        }
        if(player.joinedTournament||player.tournament!=null||player.tournamentFight!=null
                ||player.lmsSession!=null||player.lmsQueue!=null||player.pestGame!=null
                ||player.currentInstance!=null||player.raidsParty!=null||player.fightCaves!=null||player.inferno!=null
                ||player.pyramidPlunderGame!=null||(player.getDuel()!=null&&player.getDuel().stage>0)
                ||player.getHeight()>3){
            player.sendMessage("Leave your current minigame, instance or duel before changing combat levels.");return false;
        }
        return true;
    }
}
